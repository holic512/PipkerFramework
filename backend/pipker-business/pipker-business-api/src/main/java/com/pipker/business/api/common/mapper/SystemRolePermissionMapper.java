/**
 * @file SystemRolePermissionMapper.java
 * @project Pipker Framework
 * @module Pipker Business API
 * @description 提供 system_role_permission 的 MyBatis-Plus 单表数据访问能力。
 * @logic 使用独立关联主键完成角色 API 权限关联的基础维护。
 * @dependencies MyBatis-Plus、SystemRolePermission
 * @index_tags mybatis-plus、system-role-permission、rbac
 * @author holic512
 */
package com.pipker.business.api.common.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pipker.business.api.common.model.SystemRolePermission;

/** 角色权限关联 Mapper。 */
public interface SystemRolePermissionMapper extends BaseMapper<SystemRolePermission> {
}
