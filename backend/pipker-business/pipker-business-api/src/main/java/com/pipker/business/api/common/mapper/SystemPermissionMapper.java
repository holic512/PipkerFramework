/**
 * @file SystemPermissionMapper.java
 * @project Pipker Framework
 * @module Pipker Business API
 * @description 提供 system_permission 的 MyBatis-Plus 单表数据访问能力。
 * @logic 授权服务按权限状态与类型查询实体后投影权限编码。
 * @dependencies MyBatis-Plus、SystemPermission
 * @index_tags mybatis-plus、system-permission、rbac
 * @author holic512
 */
package com.pipker.business.api.common.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pipker.business.api.common.model.SystemPermission;

/** 系统权限 Mapper。 */
public interface SystemPermissionMapper extends BaseMapper<SystemPermission> {
}
