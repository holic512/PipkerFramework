/**
 * @file PipkerApplication.java
 * @project Pipker Framework
 * @module Pipker Server
 * @description 提供 Pipker 可执行 HTTP Server 的唯一启动入口并启用应用级定时任务。
 * @logic 在 Spring 上下文创建基础设施前初始化独立配置的文件、日志目录，并在 sqlite Profile 下校验 SQLite 数据库目录；随后扫描公共 Starter、业务 API 组件和集中式 MyBatis-Plus Mapper，并启用本地缓存等固定周期维护任务。
 * @dependencies Spring Boot、Spring Scheduling、MyBatis-Plus、Pipker Business API、LocalDataDirectoryInitializer、SQLiteDatabaseDirectoryInitializer
 * @index_tags server、bootstrap、spring-boot、scheduling、mybatis-plus、sqlite、data-root
 * @author holic512
 */
package com.pipker.server;

import com.pipker.server.config.LocalDataDirectoryInitializer;
import com.pipker.server.config.SqliteDatabaseDirectoryInitializer;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Pipker HTTP Server 的 Spring Boot 启动入口。
 */
@SpringBootApplication(scanBasePackages = "com.pipker")
@MapperScan(basePackages = "com.pipker.business.api.common.mapper")
@EnableScheduling
public class PipkerApplication {

    /**
     * 启动 Pipker Server 应用上下文。
     */
    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(PipkerApplication.class);
        application.addInitializers(
                new LocalDataDirectoryInitializer(),
                new SqliteDatabaseDirectoryInitializer()
        );
        application.run(args);
    }
}
