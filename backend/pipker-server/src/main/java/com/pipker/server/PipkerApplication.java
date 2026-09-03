package com.pipker.server;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Pipker HTTP Server 的 Spring Boot 启动入口。
 */
@SpringBootApplication(scanBasePackages = "com.pipker")
@MapperScan("com.pipker.business.api.system.mapper")
public class PipkerApplication {

    /**
     * 启动 Pipker Server 应用上下文。
     */
    public static void main(String[] args) {
        SpringApplication.run(PipkerApplication.class, args);
    }
}
