/**
 * 文件：AuthSessionService.java
 * 项目：Pipker Framework
 * 模块：Pipker Spring Boot Starter Sa-Token
 * 说明：为后续业务登录流程定义与框架无关的认证会话操作。
 * 处理逻辑：接收已完成校验的身份信息，创建和解析 Sa-Token 会话，并提供当前请求的登录状态检查，不将 Sa-Token 工具泄漏到业务模块。
 * 依赖：LoginIdentity、Sa-Token
 * 检索关键词：starter、sa-token、认证、会话、公开接口
 * 作者：holic512
 */
package com.pipker.starter.satoken.service;

import com.pipker.business.common.auth.LoginIdentity;

import java.util.Optional;

public interface AuthSessionService {

    AuthToken login(LoginIdentity identity);

    void logoutCurrent();

    Optional<LoginIdentity> currentIdentity();

    void checkLogin();
}
