/**
 * @file CurrentSystemAuthorizationService.java
 * @project Pipker Framework
 * @module Pipker Server
 * @description 将当前 Sa-Token 会话身份解析为 system_user 的授权快照。
 * @logic 仅接受 SYSTEM 登录域和数字用户 ID，再委托实时授权服务获取角色、权限和菜单。
 * @dependencies AuthSessionService、SystemAuthorizationService、ApiBusinessException
 * @index_tags auth、rbac、current-user
 * @author holic512
 */
package com.pipker.server.auth;

import com.pipker.business.api.system.model.SystemAuthorizationSnapshot;
import com.pipker.business.api.system.service.SystemAuthorizationService;
import com.pipker.business.common.api.CommonApiCode;
import com.pipker.business.common.auth.LoginIdentity;
import com.pipker.business.common.auth.SystemLoginTypes;
import com.pipker.server.error.ApiBusinessException;
import com.pipker.starter.satoken.service.AuthSessionService;
import org.springframework.stereotype.Service;

/**
 * 当前系统用户授权服务。
 */
@Service
public class CurrentSystemAuthorizationService {

    private final AuthSessionService authSessionService;
    private final SystemAuthorizationService systemAuthorizationService;

    /**
     * 创建当前授权服务。
     *
     * @param authSessionService 会话服务
     * @param systemAuthorizationService 系统 RBAC 服务
     */
    public CurrentSystemAuthorizationService(
            AuthSessionService authSessionService,
            SystemAuthorizationService systemAuthorizationService
    ) {
        this.authSessionService = authSessionService;
        this.systemAuthorizationService = systemAuthorizationService;
    }

    /**
     * 获取当前已登录系统用户的授权快照。
     *
     * @return 授权快照
     */
    public SystemAuthorizationSnapshot currentSnapshot() {
        LoginIdentity identity = authSessionService.currentIdentity()
                .orElseThrow(() -> new ApiBusinessException(CommonApiCode.AUTH_REQUIRED));
        if (!SystemLoginTypes.SYSTEM.equals(identity.loginType())) {
            throw new ApiBusinessException(CommonApiCode.AUTH_REQUIRED);
        }
        try {
            long userId = Long.parseLong(identity.userId());
            SystemAuthorizationSnapshot snapshot = systemAuthorizationService.findSnapshot(userId);
            if (snapshot == null) {
                throw new ApiBusinessException(CommonApiCode.AUTH_ACCOUNT_DISABLED);
            }
            return snapshot;
        } catch (NumberFormatException exception) {
            throw new ApiBusinessException(CommonApiCode.AUTH_REQUIRED);
        }
    }
}
