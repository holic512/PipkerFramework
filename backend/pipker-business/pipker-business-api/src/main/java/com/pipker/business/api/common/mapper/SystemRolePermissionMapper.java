/**
 * @file SystemRolePermissionMapper.java
 * @project Pipker Framework
 * @module Pipker Business API
 * @description 提供 system_role_permission 的 MyBatis-Plus 数据访问、单角色权限读取和启用角色权限全量投影。
 * @logic 单表关联写入通过 BaseMapper 完成；单角色查询保留历史编码，全量查询使用角色左连接保留无权限角色，权限有效性由 Java 权限枚举目录在业务层决定。
 * @dependencies MyBatis-Plus、SystemRolePermission、SystemRolePermissionMapping
 * @index_tags mybatis-plus、system-role-permission、permission、rbac、cache
 * @author holic512
 */
package com.pipker.business.api.common.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pipker.business.api.common.model.SystemRolePermissionMapping;
import com.pipker.business.api.common.model.SystemRolePermission;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/** 角色权限关联 Mapper。 */
public interface SystemRolePermissionMapper extends BaseMapper<SystemRolePermission> {

    /**
     * 一次读取全部启用角色及其可选权限编码，供进程内角色权限映射原子重建。
     *
     * @return 按角色排序的启用角色权限投影；无显式权限的角色仍返回一行
     */
    @Select("""
            SELECT r.role_code, rp.permission_code
            FROM system_role r
            LEFT JOIN system_role_permission rp ON rp.role_id = r.id
            WHERE r.status = 'ENABLED'
            ORDER BY r.sort, r.role_code, rp.permission_code
            """)
    List<SystemRolePermissionMapping> findEnabledRolePermissionMappings();

    /**
     * 读取一个角色保存过的全部权限编码，包括已从当前 Java 枚举删除的历史编码。
     *
     * @param roleId 角色主键
     * @return 权限编码列表
     */
    @Select("""
            SELECT permission_code
            FROM system_role_permission
            WHERE role_id = #{roleId}
            ORDER BY permission_code
            """)
    List<String> findPermissionCodesByRoleId(@Param("roleId") long roleId);
}
