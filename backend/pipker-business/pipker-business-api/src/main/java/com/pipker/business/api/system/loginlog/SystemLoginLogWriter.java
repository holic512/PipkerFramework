/**
 * @file SystemLoginLogWriter.java
 * @project Pipker Framework
 * @module Pipker Business API
 * @description 在独立事务中追加一条认证生命周期日志。
 * @logic 每次写入使用 REQUIRES_NEW，与调用方认证事务隔离；异常交由外层 Recorder 捕获并告警。
 * @dependencies SystemLoginLogMapper、SystemLoginLog、Spring Transaction
 * @index_tags authentication、audit、login-log、transaction、persistence
 * @author holic512
 */
package com.pipker.business.api.system.loginlog;

import com.pipker.business.api.common.mapper.SystemLoginLogMapper;
import com.pipker.business.api.common.model.SystemLoginLog;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** 独立事务日志写入器。 */
@Service
public class SystemLoginLogWriter {

    private final SystemLoginLogMapper systemLoginLogMapper;

    /** 创建日志写入器。 */
    public SystemLoginLogWriter(SystemLoginLogMapper systemLoginLogMapper) {
        this.systemLoginLogMapper = systemLoginLogMapper;
    }

    /** 追加一条不可变日志。 */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void write(SystemLoginLog log) {
        systemLoginLogMapper.insert(log);
    }
}
