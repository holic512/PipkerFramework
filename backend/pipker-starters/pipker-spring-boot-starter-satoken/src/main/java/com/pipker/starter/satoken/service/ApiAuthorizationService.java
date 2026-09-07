/**
 * @file ApiAuthorizationService.java
 * @project Pipker Framework
 * @module Pipker Sa-Token Starter
 * @description Defines the application callback used by the Sa-Token filter to authorize protected API requests.
 * @logic Receives an already-authenticated Pipker identity and request route; implementations decide from application-owned authorization data without exposing persistence to the starter.
 * @dependencies LoginIdentity
 * @index_tags starter, authorization, api, spi
 * @author holic512
 */
package com.pipker.starter.satoken.service;

import com.pipker.business.common.auth.LoginIdentity;

/**
 * 已认证 API 请求的应用级授权回调。
 */
public interface ApiAuthorizationService {

    /**
     * 判断身份是否可以访问一个受保护 API 请求。
     *
     * @param identity 已通过登录验证的身份
     * @param httpMethod 大写或原始 HTTP 方法
     * @param requestPath 不含查询参数的请求路径
     * @return 只有数据库规则唯一匹配且身份持有所需权限时返回 {@code true}
     */
    boolean isAuthorized(LoginIdentity identity, String httpMethod, String requestPath);
}
