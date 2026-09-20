/**
 * @file SystemRolePermissionMapping.java
 * @project Pipker Framework
 * @module Pipker Business API
 * @description 承载启用角色与其接口权限编码的只读查询投影。
 * @logic 允许权限编码为空以保留没有任何显式接口权限的启用角色，供全量一级缓存构建完整角色映射。
 * @dependencies Java 标准库
 * @index_tags rbac、role、permission、cache、projection
 * @author holic512
 */
package com.pipker.business.api.common.model;

/** 启用角色与可选权限编码的查询行。 */
public class SystemRolePermissionMapping {

    private String roleCode;
    private String permissionCode;

    /** 返回角色编码。 */
    public String getRoleCode() {
        return roleCode;
    }

    /** 设置角色编码。 */
    public void setRoleCode(String roleCode) {
        this.roleCode = roleCode;
    }

    /** 返回权限编码；没有显式权限时为 {@code null}。 */
    public String getPermissionCode() {
        return permissionCode;
    }

    /** 设置权限编码。 */
    public void setPermissionCode(String permissionCode) {
        this.permissionCode = permissionCode;
    }
}
