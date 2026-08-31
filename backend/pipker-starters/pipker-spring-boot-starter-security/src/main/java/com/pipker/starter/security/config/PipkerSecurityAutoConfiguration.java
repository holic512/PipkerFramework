/**
 * @file PipkerSecurityAutoConfiguration.java
 * @project Pipker Framework
 * @module Pipker Spring Boot Starter Security
 * @description Auto-configures Pipker's unified password and field-cryptography service.
 * @logic Binds security settings and constructs the configured crypto service during startup so invalid selected key material fails before application traffic is served.
 * @dependencies Spring Boot Auto Configuration, Pipker Security Crypto Service
 * @index_tags starter,security,auto-configuration,cryptography
 * @author holic512
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
