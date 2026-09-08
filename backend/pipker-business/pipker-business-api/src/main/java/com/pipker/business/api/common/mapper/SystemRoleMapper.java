/**
 * @file SystemRoleMapper.java
 * @project Pipker Framework
 * @module Pipker Business API
 * @description 提供 system_role 的 MyBatis-Plus 单表数据访问能力。
 * @logic 角色配置服务使用标准 BaseMapper CRUD 和 Lambda 条件读取角色。
 * @dependencies MyBatis-Plus、SystemRole
 * @index_tags mybatis-plus、system-role、rbac
 * @author holic512
 */
package com.pipker.business.api.common.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pipker.business.api.common.model.SystemRole;

/** 系统角色 Mapper。 */
public interface SystemRoleMapper extends BaseMapper<SystemRole> {
}
