/**
 * @file ApiAccessLogAggregate.java
 * @project Pipker Framework
 * @module Pipker Business API
 * @description 承载数据库接口日志概览、趋势和接口维度的聚合结果。
 * @logic Mapper 写入可变投影，管理服务完成百分比、舍入、时间桶补齐和安全响应转换。
 * @dependencies Lombok、Java Time
 * @index_tags api-log、aggregate、trend、endpoint
 * @author holic512
 */
package com.pipker.business.api.common.model;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** 数据库聚合投影集合。 */
public final class ApiAccessLogAggregate {

    private ApiAccessLogAggregate() {
    }

    @Getter
    @Setter
    public static class Summary {
        private long totalCount;
        private long successCount;
        private long slowCount;
        private double avgDurationMs;
        private long maxDurationMs;
    }

    @Getter
    @Setter
    public static class HourlyTrend {
        private LocalDateTime bucketStart;
        private long totalCount;
        private long successCount;
        private long slowCount;
        private double avgDurationMs;
    }

    @Getter
    @Setter
    public static class DailyTrend {
        private LocalDate bucketDate;
        private long totalCount;
        private long successCount;
        private long slowCount;
        private double avgDurationMs;
    }

    @Getter
    @Setter
    public static class Endpoint {
        private String httpMethod;
        private String routePattern;
        private String purpose;
        private long totalCount;
        private long successCount;
        private long slowCount;
        private double avgDurationMs;
        private long maxDurationMs;
    }
}
