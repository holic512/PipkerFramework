/**
 * @file PipkerSaTokenAutoConfiguration.java
 * @project Pipker Framework
 * @module Pipker Spring Boot Starter Sa-Token
 * @description Auto-configures Pipker's Sa-Token session facade, conditional DAO, and API authentication filter.
 * @logic Binds Sa-Token and Pipker settings, installs one selected persistence implementation into SaManager, protects configured API routes, and renders unauthenticated requests as RFC 9457 problem details.
 * @dependencies Sa-Token Spring Boot 4, Spring Boot Auto Configuration, Spring Web, Spring Data Redis
 * @index_tags starter,sa-token,auto-configuration,authentication,filter,redis
 * @author holic512
 */
package com.pipker.starter.satoken.config;

import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.config.SaTokenConfig;
import cn.dev33.satoken.context.SaHolder;
import cn.dev33.satoken.dao.SaTokenDao;
import cn.dev33.satoken.dao.SaTokenDaoDefaultImpl;
import cn.dev33.satoken.filter.SaServletFilter;
import cn.dev33.satoken.stp.StpUtil;
import com.pipker.starter.satoken.dao.PipkerRedisSaTokenDao;
import com.pipker.starter.satoken.service.AuthSessionService;
import com.pipker.starter.satoken.service.SaTokenAuthSessionService;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;

@AutoConfiguration
@EnableConfigurationProperties(PipkerAuthProperties.class)
public class PipkerSaTokenAutoConfiguration {

    private static final String UNAUTHORIZED_JSON = """
            {"type":"about:blank","title":"Unauthorized","status":401,"detail":"Authentication is required."}
            """.trim();

    @Bean
    @ConditionalOnMissingBean
    @ConfigurationProperties(prefix = "sa-token")
    public SaTokenConfig pipkerSaTokenConfig() {
        return new SaTokenConfig();
    }

    @Bean
    @ConditionalOnProperty(prefix = "pipker.security.auth", name = "session-store", havingValue = "memory", matchIfMissing = true)
    @ConditionalOnMissingBean(SaTokenDao.class)
    public SaTokenDao pipkerMemorySaTokenDao() {
        return new SaTokenDaoDefaultImpl();
    }

    @Bean
    @ConditionalOnProperty(prefix = "pipker.security.auth", name = "session-store", havingValue = "redis")
    @ConditionalOnMissingBean(SaTokenDao.class)
    public SaTokenDao pipkerRedisSaTokenDao(StringRedisTemplate redisTemplate) {
        return new PipkerRedisSaTokenDao(redisTemplate);
    }

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

    @Bean
    @ConditionalOnMissingBean
    public AuthSessionService authSessionService() {
        return new SaTokenAuthSessionService();
    }

    @Bean
    public SaServletFilter pipkerSaTokenFilter(PipkerAuthProperties authProperties) {
        return new SaServletFilter()
                .setIncludeList(new ArrayList<>(authProperties.getProtectedPaths()))
                .setAuth(ignored -> {
                    if (!authProperties.isPublicRoute(
                            SaHolder.getRequest().getMethod(),
                            SaHolder.getRequest().getRequestPath())) {
                        StpUtil.checkLogin();
                    }
                })
                .setError(ignored -> unauthorizedProblemDetail());
    }

    private String unauthorizedProblemDetail() {
        SaHolder.getResponse()
                .setStatus(HttpStatus.UNAUTHORIZED.value())
                .setHeader("Content-Type", "application/problem+json;charset=UTF-8");

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNAUTHORIZED,
                "Authentication is required."
        );
        problemDetail.setTitle("Unauthorized");
        try {
            return JsonMapper.builder().build().writeValueAsString(problemDetail);
        } catch (JacksonException exception) {
            return UNAUTHORIZED_JSON;
        }
    }
}
