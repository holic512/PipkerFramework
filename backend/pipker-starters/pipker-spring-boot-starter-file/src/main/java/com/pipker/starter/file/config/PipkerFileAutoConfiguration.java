/**
 * @file PipkerFileAutoConfiguration.java
 * @project Pipker Framework
 * @module Pipker Spring Boot Starter File
 * @description 在文件功能开启时注册默认本地文件存储服务。
 * @logic 绑定 pipker.file 配置，并以条件化 Bean 形式创建 LocalFileStorageService；用户关闭功能或提供自定义服务时不创建默认实现。
 * @dependencies Spring Boot AutoConfiguration、PipkerFileProperties、LocalFileStorageService
 * @index_tags starter、file-storage、auto-configuration、local、conditional
 * @author holic512
 */
package com.pipker.starter.file.config;

import com.pipker.starter.file.service.FileStorageService;
import com.pipker.starter.file.service.LocalFileStorageService;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * 自动配置 Pipker 默认本地文件存储服务。
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "pipker.file", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(PipkerFileProperties.class)
public class PipkerFileAutoConfiguration {

    /**
     * 注册默认本地文件存储服务。
     *
     * @param properties 文件存储配置
     * @return 本地文件存储服务
     */
    @Bean
    @ConditionalOnMissingBean(FileStorageService.class)
    public FileStorageService fileStorageService(PipkerFileProperties properties) {
        return new LocalFileStorageService(properties.resolveLocalRoot(), properties.getAccessPath());
    }
}
