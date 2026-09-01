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

/**
 * 与具体认证框架解耦的会话操作门面。
 */
public interface AuthSessionService {

    /**
     * 为已完成身份校验的用户创建登录会话。
     *
     * @param identity 已认证的登录身份
     * @return 新创建的认证令牌
     */
    AuthToken login(LoginIdentity identity);

    /**
     * 注销当前请求关联的登录会话。
     */
    void logoutCurrent();

    /**
     * 读取当前请求对应的登录身份。
     *
     * @return 已登录身份；未登录时为空
     */
    Optional<LoginIdentity> currentIdentity();

    /**
     * 检查当前请求是否已经登录。
     *
     * @throws RuntimeException 当前请求未登录或会话无效时由底层认证实现抛出
     */
    void checkLogin();
}
