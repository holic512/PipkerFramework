/**
 * 文件：PipkerSecurityAutoConfiguration.java
 * 项目：Pipker Framework
 * 模块：Pipker Spring Boot Starter Security
 * 说明：自动配置 Pipker 统一的密码和字段加密服务。
 * 处理逻辑：绑定安全配置，并在启动期间构造已配置的加密服务，使无效的密钥材料在应用接收流量前失败。
 * 依赖：Spring Boot 自动配置、Pipker Security Crypto Service
 * 检索关键词：starter、安全、自动配置、加密
 * 作者：holic512
 */
package com.pipker.starter.security.config;

import com.pipker.starter.security.service.DefaultSecurityCryptoService;
import com.pipker.starter.security.service.SecurityCryptoService;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@EnableConfigurationProperties(PipkerSecurityProperties.class)
public class PipkerSecurityAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public SecurityCryptoService securityCryptoService(PipkerSecurityProperties properties) {
        return new DefaultSecurityCryptoService(properties);
    }
}
