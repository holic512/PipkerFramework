/**
 * @file PipkerSaTokenAutoConfiguration.java
 * @project Pipker Framework
 * @module Pipker Sa-Token Starter
 * @description Configures the default Sa-Token session facade, selected DAO, and database-backed protected API filter.
 * @logic Installs configuration and storage in SaManager, exempts explicit anonymous routes, requires login, then delegates protected API authorization to an application service.
 * @dependencies Sa-Token Spring Boot 4, Spring Boot AutoConfiguration, Spring Web, Spring Data Redis, ApiAuthorizationService
 * @index_tags starter, sa-token, authentication, authorization, api-response, redis
 * @author holic512
 */
package com.pipker.starter.satoken.config;

import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.config.SaTokenConfig;
import cn.dev33.satoken.context.SaHolder;
import cn.dev33.satoken.dao.SaTokenDao;
import cn.dev33.satoken.dao.SaTokenDaoDefaultImpl;
import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.filter.SaServletFilter;
import cn.dev33.satoken.stp.StpUtil;
import com.pipker.business.common.api.ApiResponse;
import com.pipker.business.common.api.CommonApiCode;
import com.pipker.business.common.auth.LoginIdentity;
import com.pipker.business.common.auth.LoginIdentityCodec;
import com.pipker.starter.satoken.dao.PipkerRedisSaTokenDao;
import com.pipker.starter.satoken.service.ApiAuthorizationDeniedException;
import com.pipker.starter.satoken.service.ApiAuthorizationService;
import com.pipker.starter.satoken.service.AuthSessionService;
import com.pipker.starter.satoken.service.SaTokenAuthSessionService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.StringRedisTemplate;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;

/**
 * 自动配置 Sa-Token 会话、DAO 和受保护 API 的认证 Filter。
 */
@AutoConfiguration
@EnableConfigurationProperties(PipkerAuthProperties.class)
public class PipkerSaTokenAutoConfiguration {

    private static final String UNAUTHORIZED_JSON = """
            {"code":401,"data":null,"message":"Authentication is required."}
            """.trim();

    /**
     * 绑定并注册 Sa-Token 原生配置。
     *
     * @return Sa-Token 配置对象
     */
    @Bean
    @ConditionalOnMissingBean
    @ConfigurationProperties(prefix = "sa-token")
    public SaTokenConfig pipkerSaTokenConfig() {
        return new SaTokenConfig();
    }

    /**
     * 在会话存储选择为内存时注册默认 DAO。
     *
     * @return Sa-Token 内存 DAO
     */
    @Bean
    @ConditionalOnProperty(prefix = "pipker.security.auth", name = "session-store", havingValue = "memory", matchIfMissing = true)
    @ConditionalOnMissingBean(SaTokenDao.class)
    public SaTokenDao pipkerMemorySaTokenDao() {
        return new SaTokenDaoDefaultImpl();
    }

    /**
     * 在会话存储选择为 Redis 时注册 Redis DAO。
     *
     * @param redisTemplate 字符串 Redis 模板
     * @return Sa-Token Redis DAO
     */
    @Bean
    @ConditionalOnProperty(prefix = "pipker.security.auth", name = "session-store", havingValue = "redis")
    @ConditionalOnMissingBean(SaTokenDao.class)
    public SaTokenDao pipkerRedisSaTokenDao(StringRedisTemplate redisTemplate) {
        return new PipkerRedisSaTokenDao(redisTemplate);
    }

    /**
     * 在所有 Bean 初始化完成后将配置和 DAO 安装到 Sa-Token 管理器。
     *
     * @param pipkerSaTokenConfig Sa-Token 配置
     * @param saTokenDao 选定的会话 DAO
     * @return 管理器初始化回调
     */
    @Bean
    public SmartInitializingSingleton pipkerSaTokenManagerInitializer(
            SaTokenConfig pipkerSaTokenConfig,
            SaTokenDao saTokenDao
    ) {
        return () -> {
            SaManager.setConfig(pipkerSaTokenConfig);
            SaManager.setSaTokenDao(saTokenDao);
        };
    }

    /**
     * 在应用显式启用数据库授权时，拒绝缺少业务授权实现的错误启动。
     *
     * @param authProperties 认证授权配置
     * @param apiAuthorizationServiceProvider 数据库 API 授权服务提供者
     * @return 初始化验证回调
     */
    @Bean
    public SmartInitializingSingleton pipkerDatabaseAuthorizationInitializer(
            PipkerAuthProperties authProperties,
            ObjectProvider<ApiAuthorizationService> apiAuthorizationServiceProvider
    ) {
        return () -> {
            if (authProperties.isDatabaseAuthorizationRequired()
                    && apiAuthorizationServiceProvider.getIfAvailable() == null) {
                throw new IllegalStateException(
                        "pipker.security.auth.database-authorization-required requires an ApiAuthorizationService bean"
                );
            }
        };
    }

    /**
     * 注册业务层使用的认证会话门面。
     *
     * @return Sa-Token 会话服务
     */
    @Bean
    @ConditionalOnMissingBean
    public AuthSessionService authSessionService() {
        return new SaTokenAuthSessionService();
    }

    /**
     * 创建保护配置路径并放行显式声明匿名路由的 Sa-Token Filter。
     *
     * @param authProperties 认证路由配置
     * @return Sa-Token Servlet Filter
     */
    @Bean
    public SaServletFilter pipkerSaTokenFilter(
            PipkerAuthProperties authProperties,
            ObjectProvider<ApiAuthorizationService> apiAuthorizationServiceProvider
    ) {
        return new SaServletFilter()
                .setIncludeList(new ArrayList<>(authProperties.getProtectedPaths()))
                .setAuth(ignored -> {
                    if (!authProperties.isPublicRoute(
                            SaHolder.getRequest().getMethod(),
                            SaHolder.getRequest().getRequestPath())) {
                        StpUtil.checkLogin();
                        if (authProperties.isDatabaseAuthorizationRequired()) {
                            authorizeApiRequest(apiAuthorizationServiceProvider.getObject());
                        }
                    }
                })
                .setError(this::authorizationErrorResponse);
    }

    /**
     * 解析当前登录主体并委托业务层判断本次 API 请求是否被授权。
     *
     * @param apiAuthorizationService 数据库 API 授权服务
     */
    private void authorizeApiRequest(ApiAuthorizationService apiAuthorizationService) {
        LoginIdentity identity;
        try {
            identity = LoginIdentityCodec.decode(String.valueOf(StpUtil.getLoginId()));
        } catch (IllegalArgumentException exception) {
            throw new ApiAuthorizationDeniedException("Current login identity is invalid", exception);
        }
        boolean authorized = apiAuthorizationService.isAuthorized(
                identity,
                SaHolder.getRequest().getMethod(),
                SaHolder.getRequest().getRequestPath()
        );
        if (!authorized) {
            throw new ApiAuthorizationDeniedException("No database API permission grants this request");
        }
    }

    /**
     * 设置统一授权失败响应内容类型，并按失败原因使用稳定业务编码。
     *
     * @param throwable Sa-Token Filter 捕获的失败
     * @return 序列化后的 API 响应
     */
    private String authorizationErrorResponse(Throwable throwable) {
        SaHolder.getResponse()
                .setStatus(200)
                .setHeader("Content-Type", "application/json;charset=UTF-8");
        try {
            return JsonMapper.builder().build().writeValueAsString(
                    ApiResponse.failure(resolveAuthorizationErrorCode(throwable))
            );
        } catch (JacksonException exception) {
            return UNAUTHORIZED_JSON;
        }
    }

    /**
     * 将登录失败、明确拒绝和基础设施异常区分为统一 API 响应码。
     *
     * @param throwable Filter 捕获的异常
     * @return 稳定响应码
     */
    private CommonApiCode resolveAuthorizationErrorCode(Throwable throwable) {
        if (throwable instanceof NotLoginException) {
            return CommonApiCode.AUTH_REQUIRED;
        }
        if (throwable instanceof ApiAuthorizationDeniedException) {
            return CommonApiCode.AUTH_FORBIDDEN;
        }
        return CommonApiCode.INTERNAL_ERROR;
    }
}
