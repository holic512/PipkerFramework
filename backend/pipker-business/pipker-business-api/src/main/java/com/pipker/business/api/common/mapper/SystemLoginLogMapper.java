/**
 * @file SystemLoginLogMapper.java
 * @project Pipker Framework
 * @module Pipker Business API
 * @description 提供认证生命周期日志的 MyBatis-Plus 插入与只读分页查询能力。
 * @logic 由认证审计 Recorder 追加记录、由管理员查询服务筛选读取，不暴露更新或删除业务。
 * @dependencies MyBatis-Plus、SystemLoginLog
 * @index_tags authentication、audit、login-log、mapper、pagination
 * @author holic512
 */
package com.pipker.business.api.common.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pipker.business.api.common.model.SystemLoginLog;

/** 登录日志 Mapper。 */
public interface SystemLoginLogMapper extends BaseMapper<SystemLoginLog> {
}
