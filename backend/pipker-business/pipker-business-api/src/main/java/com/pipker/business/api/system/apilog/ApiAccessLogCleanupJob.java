/**
 * @file ApiAccessLogCleanupJob.java
 * @project Pipker Framework
 * @module Pipker Business API
 * @description 按配置的保留期和 Cron 周期清理过期接口访问日志。
 * @logic 每轮固定截止时间并反复调用独立事务批次，失败时停止本轮并等待下次调度，不触碰认证审计日志。
 * @dependencies ApiAccessLogCleanupWriter、PipkerLogProperties、Spring Scheduling
 * @index_tags api-log、scheduled-cleanup、retention
 * @author holic512
 */
package com.pipker.business.api.system.apilog;

import com.pipker.starter.log.config.PipkerLogProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;

/** 接口访问日志保留期清理任务。 */
@Component
public class ApiAccessLogCleanupJob {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApiAccessLogCleanupJob.class);
    private final ApiAccessLogCleanupWriter writer;
    private final Duration retention;

    public ApiAccessLogCleanupJob(ApiAccessLogCleanupWriter writer, PipkerLogProperties properties) {
        this.writer = writer;
        this.retention = properties.getApiAccess().getRetention();
        if (retention == null || retention.isZero() || retention.isNegative()) {
            throw new IllegalArgumentException("pipker.log.api-access.retention must be positive");
        }
    }

    /** 每天按服务器本地时间执行分批清理。 */
    @Scheduled(cron = "${pipker.log.api-access.cleanup-cron:0 30 2 * * *}")
    public void cleanupExpired() {
        LocalDateTime cutoff = LocalDateTime.now().minus(retention);
        int deleted = 0;
        try {
            int batch;
            do {
                batch = writer.deleteBatchBefore(cutoff);
                deleted += batch;
            } while (batch > 0);
            if (deleted > 0) {
                LOGGER.info("Expired API access logs cleaned: deleted={} cutoff={}", deleted, cutoff);
            }
        } catch (RuntimeException exception) {
            LOGGER.error("Scheduled API access log cleanup failed: deleted={} cutoff={} failureType={}",
                    deleted, cutoff, exception.getClass().getSimpleName());
        }
    }
}
