/**
 * @file AsyncApiAccessLogPublisher.java
 * @project Pipker Framework
 * @module Pipker Spring Boot Starter Log
 * @description 通过单消费者有界队列异步批量分发接口访问日志并暴露进程内诊断状态。
 * @logic 请求线程仅非阻塞入队；后台线程按批量上限或刷新间隔消费，关闭时限时排空，队列满和处理失败均旁路计数与限频告警。
 * @dependencies PipkerLogProperties、ApiAccessLogHandler、Java Concurrency、SLF4J
 * @index_tags log、api、async、queue、batch、lifecycle
 * @author holic512
 */
package com.pipker.starter.log.api;

import com.pipker.starter.log.config.PipkerLogProperties;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/** 有界、尽力而为的接口访问日志异步发布器。 */
public class AsyncApiAccessLogPublisher implements ApiAccessLogDiagnostics {

    private static final Logger LOGGER = LoggerFactory.getLogger(AsyncApiAccessLogPublisher.class);
    private static final long OVERFLOW_WARNING_INTERVAL_NANOS = TimeUnit.MINUTES.toNanos(1);

    private final BlockingQueue<ApiAccessLogRecord> queue;
    private final List<ApiAccessLogHandler> handlers;
    private final int batchSize;
    private final long flushIntervalMillis;
    private final long shutdownDrainTimeoutMillis;
    private final AtomicLong dropped = new AtomicLong();
    private final AtomicLong writeFailures = new AtomicLong();
    private final AtomicLong lastOverflowWarning = new AtomicLong();

    private volatile boolean running;
    private Thread worker;

    /** 创建异步发布器并校验所有有界参数。 */
    public AsyncApiAccessLogPublisher(PipkerLogProperties properties, List<ApiAccessLogHandler> handlers) {
        PipkerLogProperties.ApiAccess config = properties.getApiAccess();
        int queueCapacity = requirePositive(config.getQueueCapacity(), "queue-capacity");
        this.batchSize = requirePositive(config.getBatchSize(), "batch-size");
        this.flushIntervalMillis = requirePositive(config.getFlushInterval(), "flush-interval");
        this.shutdownDrainTimeoutMillis = requirePositive(config.getShutdownDrainTimeout(), "shutdown-drain-timeout");
        this.queue = new ArrayBlockingQueue<>(queueCapacity);
        this.handlers = List.copyOf(handlers);
    }

    /** 启动独立守护消费线程。 */
    @PostConstruct
    public void start() {
        if (running || handlers.isEmpty()) {
            return;
        }
        running = true;
        worker = Thread.ofPlatform()
                .daemon(true)
                .name("pipker-api-access-log-writer")
                .start(this::consumeLoop);
    }

    /** 非阻塞发布；队列满时丢弃并记录诊断信息。 */
    public void publish(ApiAccessLogRecord record) {
        if (record == null || handlers.isEmpty() || !running) {
            return;
        }
        if (!queue.offer(record)) {
            dropped.incrementAndGet();
            warnOverflowAtMostOncePerMinute();
        }
    }

    /** 限时排空队列后停止后台线程。 */
    @PreDestroy
    public void stop() {
        running = false;
        Thread currentWorker = worker;
        if (currentWorker == null) {
            return;
        }
        currentWorker.interrupt();
        try {
            currentWorker.join(shutdownDrainTimeoutMillis);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
        int abandoned = queue.size();
        if (abandoned > 0) {
            dropped.addAndGet(abandoned);
            queue.clear();
            LOGGER.warn("API access log shutdown timeout discarded {} queued records", abandoned);
        }
    }

    @Override
    public Snapshot snapshot() {
        return new Snapshot(queue.size(), queue.size() + queue.remainingCapacity(), dropped.get(), writeFailures.get());
    }

    private void consumeLoop() {
        List<ApiAccessLogRecord> batch = new ArrayList<>(batchSize);
        while (running || !queue.isEmpty()) {
            try {
                ApiAccessLogRecord first = queue.poll(flushIntervalMillis, TimeUnit.MILLISECONDS);
                if (first == null) {
                    continue;
                }
                batch.add(first);
                queue.drainTo(batch, batchSize - 1);
                dispatch(batch);
                batch.clear();
            } catch (InterruptedException exception) {
                if (running) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        drainRemaining(batch);
    }

    private void drainRemaining(List<ApiAccessLogRecord> batch) {
        while (!queue.isEmpty()) {
            queue.drainTo(batch, batchSize);
            if (batch.isEmpty()) {
                break;
            }
            dispatch(batch);
            batch.clear();
        }
    }

    private void dispatch(List<ApiAccessLogRecord> batch) {
        List<ApiAccessLogRecord> immutableBatch = List.copyOf(batch);
        for (ApiAccessLogHandler handler : handlers) {
            try {
                handler.handle(immutableBatch);
            } catch (RuntimeException exception) {
                writeFailures.addAndGet(immutableBatch.size());
                LOGGER.error("API access log batch persistence failed: batchSize={} failureType={}",
                        immutableBatch.size(), exception.getClass().getSimpleName());
            }
        }
    }

    private void warnOverflowAtMostOncePerMinute() {
        long now = System.nanoTime();
        long previous = lastOverflowWarning.get();
        if ((previous == 0 || now - previous >= OVERFLOW_WARNING_INTERVAL_NANOS)
                && lastOverflowWarning.compareAndSet(previous, now)) {
            LOGGER.warn("API access log queue is full; new records are being discarded: capacity={} droppedSinceStartup={}",
                    queue.size() + queue.remainingCapacity(), dropped.get());
        }
    }

    private int requirePositive(int value, String name) {
        if (value <= 0) {
            throw new IllegalArgumentException("pipker.log.api-access." + name + " must be positive");
        }
        return value;
    }

    private long requirePositive(Duration value, String name) {
        if (value == null || value.isZero() || value.isNegative()) {
            throw new IllegalArgumentException("pipker.log.api-access." + name + " must be positive");
        }
        return Math.max(1, value.toMillis());
    }
}
