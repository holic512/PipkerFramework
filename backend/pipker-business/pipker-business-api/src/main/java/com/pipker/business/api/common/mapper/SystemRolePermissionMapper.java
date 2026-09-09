/**
 * @file SystemRolePermissionMapper.java
 * @project Pipker Framework
 * @module Pipker Business API
 * @description 提供 system_role_permission 的 MyBatis-Plus 数据访问和角色权限编码读取。
 * @logic 单表关联写入通过 BaseMapper 完成；角色当前及历史权限编码以稳定排序读取，是否有效由 Java 权限枚举目录在业务层决定。
 * @dependencies MyBatis-Plus、SystemRolePermission
 * @index_tags mybatis-plus、system-role-permission、permission、rbac
 * @author holic512
 */
package com.pipker.business.api.common.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pipker.business.api.common.model.SystemRolePermission;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/** 角色权限关联 Mapper。 */
public interface SystemRolePermissionMapper extends BaseMapper<SystemRolePermission> {

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
