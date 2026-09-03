/**
 * @file PipkerFileWebMvcAutoConfiguration.java
 * @project Pipker Framework
 * @module Pipker Spring Boot Starter File
 * @description 在 Servlet Web 应用中为本地文件根目录注册公开只读资源映射。
 * @logic 当文件功能开启且 Spring MVC 可用时，将 access-path/** 映射到 file: 根目录；不创建上传、删除、目录枚举或完整 URL 接口。
 * @dependencies Spring Boot AutoConfiguration、Spring Web MVC、PipkerFileProperties
 * @index_tags starter、file-storage、webmvc、resource-handler、public-read
 * @author holic512
 */
package com.pipker.starter.file.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 为已启用的本地文件存储注册 Spring MVC 静态资源访问映射。
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass({WebMvcConfigurer.class, ResourceHandlerRegistry.class})
@ConditionalOnProperty(prefix = "pipker.file", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(PipkerFileProperties.class)
public class PipkerFileWebMvcAutoConfiguration {

    /**
     * 注册独立于应用其他 MVC 配置的文件资源映射。
     *
     * @param properties 文件存储配置
     * @return 文件资源 Web MVC 配置器
     */
    @Bean(name = "pipkerFileWebMvcConfigurer")
    public WebMvcConfigurer pipkerFileWebMvcConfigurer(PipkerFileProperties properties) {
        String accessPattern = properties.getAccessPath() + "/**";
        String resourceLocation = fileResourceLocation(properties);
        return new WebMvcConfigurer() {

            @Override
            public void addResourceHandlers(ResourceHandlerRegistry registry) {
                registry.addResourceHandler(accessPattern)
                        .addResourceLocations(resourceLocation);
            }
        };
    }

    /**
     * 将本地目录转换为带尾部斜杠的 {@code file:} 资源位置。
     *
     * @param properties 文件存储配置
     * @return Spring MVC 可解析的文件资源位置
     */
    private String fileResourceLocation(PipkerFileProperties properties) {
        String location = properties.resolveLocalRoot().toUri().toString();
        return location.endsWith("/") ? location : location + "/";
    }
}
