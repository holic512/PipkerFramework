/**
 * @file SystemApiAccessLogMapper.java
 * @project Pipker Framework
 * @module Pipker Business API
 * @description 提供接口访问日志的追加、分页与跨数据库聚合查询。
 * @logic 聚合 SQL 只使用 SQLite、MySQL 和 PostgreSQL 共同支持的 CASE、COALESCE、GROUP BY；时间桶由写入端预计算。
 * @dependencies MyBatis-Plus、SystemApiAccessLog、ApiAccessLogAggregate
 * @index_tags api-log、mapper、aggregate、pagination、multi-database
 * @author holic512
 */
package com.pipker.business.api.common.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pipker.business.api.common.model.ApiAccessLogAggregate;
import com.pipker.business.api.common.model.SystemApiAccessLog;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

/** 接口访问日志 Mapper。 */
public interface SystemApiAccessLogMapper extends BaseMapper<SystemApiAccessLog> {

    @Select("""
            <script>
            SELECT COUNT(*) AS total_count,
                   COALESCE(SUM(CASE WHEN success = TRUE THEN 1 ELSE 0 END), 0) AS success_count,
                   COALESCE(SUM(CASE WHEN duration_ms &gt;= #{slowThresholdMs} THEN 1 ELSE 0 END), 0) AS slow_count,
                   COALESCE(AVG(duration_ms), 0) AS avg_duration_ms,
                   COALESCE(MAX(duration_ms), 0) AS max_duration_ms
              FROM system_api_access_log
             WHERE occurred_at &gt;= #{startTime} AND occurred_at &lt;= #{endTime}
             <if test="!includeMonitorEndpoints">
               AND COALESCE(route_pattern, request_path) NOT LIKE '/api/admin/api-logs%'
             </if>
            </script>
            """)
    ApiAccessLogAggregate.Summary selectSummary(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("slowThresholdMs") long slowThresholdMs,
            @Param("includeMonitorEndpoints") boolean includeMonitorEndpoints
    );

    @Select("""
            <script>
            SELECT occurred_hour AS bucket_start,
                   COUNT(*) AS total_count,
                   COALESCE(SUM(CASE WHEN success = TRUE THEN 1 ELSE 0 END), 0) AS success_count,
                   COALESCE(SUM(CASE WHEN duration_ms &gt;= #{slowThresholdMs} THEN 1 ELSE 0 END), 0) AS slow_count,
                   COALESCE(AVG(duration_ms), 0) AS avg_duration_ms
              FROM system_api_access_log
             WHERE occurred_at &gt;= #{startTime} AND occurred_at &lt;= #{endTime}
             <if test="!includeMonitorEndpoints">
               AND COALESCE(route_pattern, request_path) NOT LIKE '/api/admin/api-logs%'
             </if>
             GROUP BY occurred_hour
             ORDER BY occurred_hour
            </script>
            """)
    List<ApiAccessLogAggregate.HourlyTrend> selectHourlyTrend(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("slowThresholdMs") long slowThresholdMs,
            @Param("includeMonitorEndpoints") boolean includeMonitorEndpoints
    );

    @Select("""
            <script>
            SELECT occurred_date AS bucket_date,
                   COUNT(*) AS total_count,
                   COALESCE(SUM(CASE WHEN success = TRUE THEN 1 ELSE 0 END), 0) AS success_count,
                   COALESCE(SUM(CASE WHEN duration_ms &gt;= #{slowThresholdMs} THEN 1 ELSE 0 END), 0) AS slow_count,
                   COALESCE(AVG(duration_ms), 0) AS avg_duration_ms
              FROM system_api_access_log
             WHERE occurred_at &gt;= #{startTime} AND occurred_at &lt;= #{endTime}
             <if test="!includeMonitorEndpoints">
               AND COALESCE(route_pattern, request_path) NOT LIKE '/api/admin/api-logs%'
             </if>
             GROUP BY occurred_date
             ORDER BY occurred_date
            </script>
            """)
    List<ApiAccessLogAggregate.DailyTrend> selectDailyTrend(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("slowThresholdMs") long slowThresholdMs,
            @Param("includeMonitorEndpoints") boolean includeMonitorEndpoints
    );

    @Select("""
            <script>
            SELECT http_method,
                   COALESCE(route_pattern, '&lt;unresolved&gt;') AS route_pattern,
                   MAX(purpose) AS purpose,
                   COUNT(*) AS total_count,
                   COALESCE(SUM(CASE WHEN success = TRUE THEN 1 ELSE 0 END), 0) AS success_count,
                   COALESCE(SUM(CASE WHEN duration_ms &gt;= #{slowThresholdMs} THEN 1 ELSE 0 END), 0) AS slow_count,
                   COALESCE(AVG(duration_ms), 0) AS avg_duration_ms,
                   COALESCE(MAX(duration_ms), 0) AS max_duration_ms
              FROM system_api_access_log
             WHERE occurred_at &gt;= #{startTime} AND occurred_at &lt;= #{endTime}
             <if test="keyword != null">
               AND (route_pattern LIKE #{likeKeyword} OR request_path LIKE #{likeKeyword}
                    OR purpose LIKE #{likeKeyword} OR handler_method LIKE #{likeKeyword})
             </if>
             <if test="!includeMonitorEndpoints">
               AND COALESCE(route_pattern, request_path) NOT LIKE '/api/admin/api-logs%'
             </if>
             GROUP BY http_method, route_pattern
             ORDER BY
             <choose>
               <when test="sortBy == 'REQUEST_COUNT'">total_count</when>
               <when test="sortBy == 'SUCCESS_RATE'">(1.0 * COALESCE(SUM(CASE WHEN success = TRUE THEN 1 ELSE 0 END), 0) / COUNT(*))</when>
               <when test="sortBy == 'MAX_DURATION'">max_duration_ms</when>
               <when test="sortBy == 'SLOW_COUNT'">slow_count</when>
               <otherwise>avg_duration_ms</otherwise>
             </choose>
             <choose><when test="sortDirection == 'ASC'">ASC</when><otherwise>DESC</otherwise></choose>,
             route_pattern ASC, http_method ASC
            </script>
            """)
    IPage<ApiAccessLogAggregate.Endpoint> selectEndpointPage(
            Page<ApiAccessLogAggregate.Endpoint> page,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("slowThresholdMs") long slowThresholdMs,
            @Param("keyword") String keyword,
            @Param("likeKeyword") String likeKeyword,
            @Param("sortBy") String sortBy,
            @Param("sortDirection") String sortDirection,
            @Param("includeMonitorEndpoints") boolean includeMonitorEndpoints
    );
}
