/**
 * @file SystemStpInterface.java
 * @project Pipker Framework
 * @module Pipker Business API
 * @description 将 Sa-Token 角色和权限请求接入 system_ 表的实时 RBAC 查询。
 * @logic 解码登录主体后仅处理 SYSTEM 域，调用授权服务返回最新角色和权限，不维护本地副本或缓存。
 * @dependencies Sa-Token、LoginIdentityCodec、SystemAuthorizationService
 * @index_tags satoken、rbac、permission
 * @author holic512
 */
package com.pipker.business.api.system.auth;

import cn.dev33.satoken.stp.StpInterface;
import com.pipker.business.api.common.model.SystemAuthorizationSnapshot;
import com.pipker.business.api.system.authorization.SystemAuthorizationService;
import com.pipker.business.common.auth.LoginIdentity;
import com.pipker.business.common.auth.LoginIdentityCodec;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 数据库驱动的 Sa-Token 授权接口实现。
 */
@Component
public class SystemStpInterface implements StpInterface {

    private final SystemAuthorizationService systemAuthorizationService;

    /**
     * 创建 Sa-Token 授权适配器。
     *
     * @param systemAuthorizationService 系统 RBAC 服务
     */
    public SystemStpInterface(SystemAuthorizationService systemAuthorizationService) {
        this.systemAuthorizationService = systemAuthorizationService;
    }

    /**
     * 返回指定登录主体的实时权限列表。
     */
    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        SystemAuthorizationSnapshot snapshot = resolveSnapshot(loginId);
        return snapshot == null ? List.of() : snapshot.permissions();
    }

    /**
     * 返回指定登录主体的实时角色列表。
     */
    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        SystemAuthorizationSnapshot snapshot = resolveSnapshot(loginId);
        return snapshot == null ? List.of() : snapshot.roles();
    }

    /**
     * 安全解析 Sa-Token 保存的 Pipker 身份。
     */
    private SystemAuthorizationSnapshot resolveSnapshot(Object loginId) {
        try {
            LoginIdentity identity = LoginIdentityCodec.decode(String.valueOf(loginId));
            if (!SystemLoginTypes.SYSTEM.equals(identity.loginType())) {
                return null;
            }
            return systemAuthorizationService.findSnapshot(Long.parseLong(identity.userId()));
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }
}
