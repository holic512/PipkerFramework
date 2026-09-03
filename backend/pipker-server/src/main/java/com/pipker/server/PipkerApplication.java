/**
 * @file PipkerApplication.java
 * @project Pipker Framework
 * @module Pipker Server
 * @description 提供 Pipker 可执行 HTTP Server 的唯一启动入口。
 * @logic 扫描公共 Starter 与业务 API 组件，并扫描 API 下按功能分布的 MyBatis Mapper。
 * @dependencies Spring Boot、MyBatis Spring、Pipker Business API
 * @index_tags server、bootstrap、spring-boot、mybatis
 * @author holic512
 */
package com.pipker.server;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Pipker HTTP Server 的 Spring Boot 启动入口。
 */
@SpringBootApplication(scanBasePackages = "com.pipker")
@MapperScan(basePackages = "com.pipker.business.api")
public class PipkerApplication {

    /**
     * 启动 Pipker Server 应用上下文。
     */
    public static void main(String[] args) {
        SpringApplication.run(PipkerApplication.class, args);
    }
}
