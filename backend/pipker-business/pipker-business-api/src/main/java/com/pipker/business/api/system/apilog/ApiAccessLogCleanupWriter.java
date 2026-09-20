/**
 * @file ApiAccessLogCleanupWriter.java
 * @project Pipker Framework
 * @module Pipker Business API
 * @description 在独立事务中选择并删除一批超过保留期的接口访问日志。
 * @logic 每批按发生时间和主键稳定选取最多一千条，再按主键集合删除，兼容 SQLite、MySQL 和 PostgreSQL。
 * @dependencies SystemApiAccessLogMapper、MyBatis-Plus、Spring Transaction
 * @index_tags api-log、cleanup、retention、transaction
 * @author holic512
 */
package com.pipker.business.api.system.apilog;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pipker.business.api.common.mapper.SystemApiAccessLogMapper;
import com.pipker.business.api.common.model.SystemApiAccessLog;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/** 接口访问日志分批清理写入器。 */
@Service
public class ApiAccessLogCleanupWriter {

    private static final int BATCH_SIZE = 1_000;
    private final SystemApiAccessLogMapper mapper;

    public ApiAccessLogCleanupWriter(SystemApiAccessLogMapper mapper) {
        this.mapper = mapper;
    }

    /** 删除一批严格早于截止时间的记录并返回删除数量。 */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int deleteBatchBefore(LocalDateTime cutoff) {
        List<Long> ids = mapper.selectList(new LambdaQueryWrapper<SystemApiAccessLog>()
                        .select(SystemApiAccessLog::getId)
                        .lt(SystemApiAccessLog::getOccurredAt, cutoff)
                        .orderByAsc(SystemApiAccessLog::getOccurredAt)
                        .orderByAsc(SystemApiAccessLog::getId)
                        .last("LIMIT " + BATCH_SIZE))
                .stream()
                .map(SystemApiAccessLog::getId)
                .toList();
        return ids.isEmpty() ? 0 : mapper.deleteByIds(ids);
    }
}
