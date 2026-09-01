/**
 * 文件：PipkerApplication.java
 * 项目：Pipker Framework
 * 模块：Pipker Server
 * 说明：Pipker HTTP Server 的应用启动入口。
 * 处理逻辑：启动 Spring，并从 Server 根包开始进行组件扫描。
 * 依赖：Spring Boot
 * 检索关键词：应用、启动、spring-boot
 * 作者：holic512
 */
package com.pipker.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Pipker HTTP Server 的 Spring Boot 启动入口。
 */
@SpringBootApplication
public class PipkerApplication {

    /**
     * 启动 Pipker Server 应用上下文。
     *
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        SpringApplication.run(PipkerApplication.class, args);
    }
}
