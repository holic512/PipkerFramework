/**
 * @file ConfigurationPropertiesLayoutTests.java
 * @project Pipker Framework
 * @module Pipker Server
 * @description 验证 Spring Properties 配置资源的目录布局、持久化路径职责和生产日志切片策略。
 * @logic 读取源码资源，确认 dev/prod 导入、独立数据目录、公共 HTTP 日志开关和仅 prod 启用的无自动清理按日文件策略。
 * @dependencies JUnit Jupiter、AssertJ、Java NIO、Java Properties、Logback XML
 * @index_tags server、test、configuration、properties、profile、sqlite、persistence-path、logback
 * @author holic512
 */
package com.pipker.server;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class ConfigurationPropertiesLayoutTests {

    private static final Path RESOURCE_DIRECTORY = Path.of("src/main/resources");

    @Test
    void containsOnlyPropertiesForSpringConfiguration() throws IOException {
        try (Stream<Path> resourceFiles = Files.walk(RESOURCE_DIRECTORY)) {
            assertThat(resourceFiles
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".yml") || path.toString().endsWith(".yaml"))
                    .toList())
                    .isEmpty();
        }
    }

    @Test
    void environmentEntrypointsImportTheirOwnProperties() throws IOException {
        assertThat(load("application-dev.properties"))
                .containsEntry("spring.config.import[0]", "classpath:/config/dev/file.properties")
                .containsEntry("spring.config.import[1]", "classpath:/config/dev/base/sqlite.properties")
                .containsEntry("spring.config.import[2]", "classpath:/config/dev/base/mysql.properties")
                .containsEntry("spring.config.import[3]", "classpath:/config/dev/base/pg.properties")
                .containsEntry("spring.config.import[4]", "classpath:/config/dev/redis.properties");
        assertThat(load("application-prod.properties"))
                .containsEntry("spring.config.import[0]", "classpath:/config/prod/file.properties")
                .containsEntry("spring.config.import[1]", "classpath:/config/prod/base/sqlite.properties")
                .containsEntry("spring.config.import[2]", "classpath:/config/prod/base/mysql.properties")
                .containsEntry("spring.config.import[3]", "classpath:/config/prod/base/pg.properties")
                .containsEntry("spring.config.import[4]", "classpath:/config/prod/redis.properties");
    }

    @Test
    void commonConfigurationEnablesSafeHttpCompletionLogs() throws IOException {
        assertThat(load("application.properties"))
                .containsEntry("pipker.log.request.enabled", "true")
                .containsEntry("pipker.log.request.include-parameters", "true")
                .containsEntry("pipker.log.request.include-headers", "false")
                .containsEntry("pipker.log.request.include-request-body", "false")
                .containsEntry("pipker.log.request.include-response-body", "false");
    }

    @Test
    void productionLogbackUsesPermanentDailyLogFiles() throws IOException {
        String configuration = Files.readString(RESOURCE_DIRECTORY.resolve("logback-spring.xml"));

        assertThat(configuration)
                .contains(
                        "<springProfile name=\"prod\">",
                        "<springProfile name=\"!prod\">",
                        "source=\"pipker.data.log-root\"",
                        "${PIPKER_LOG_ROOT}/pipker-server.log",
                        "ch.qos.logback.core.rolling.TimeBasedRollingPolicy",
                        "${PIPKER_LOG_ROOT}/archive/pipker-server.%d{yyyy-MM-dd}.log",
                        "<appender-ref ref=\"CONSOLE\"/>",
                        "<appender-ref ref=\"FILE\"/>"
                )
                .doesNotContain(
                        "SizeAndTimeBasedRollingPolicy",
                        "maxFileSize",
                        "maxHistory",
                        "totalSizeCap",
                        "cleanHistoryOnStart",
                        ".gz"
                );
    }

    @Test
    void fileConfigurationOwnsIndependentPersistentPaths() throws IOException {
        for (String configuration : List.of("config/dev/file.properties", "config/prod/file.properties")) {
            assertThat(load(configuration))
                    .containsEntry("pipker.data.log-root", "${PIPKER_LOG_ROOT:./data/log}")
                    .containsEntry("pipker.data.database-root", "${PIPKER_SQLITE_ROOT:./data/base}")
                    .containsEntry("pipker.file.local.root", "${PIPKER_FILE_ROOT:./data/file}")
                    .doesNotContainKey("pipker.data.file-root")
                    .doesNotContainKey("spring.config.activate.on-profile");
        }
    }

    @Test
    void sqliteConfigurationOnlyDefinesDatasourceAndLiquibase() throws IOException {
        for (String configuration : List.of("config/dev/base/sqlite.properties", "config/prod/base/sqlite.properties")) {
            assertThat(load(configuration))
                    .containsEntry("spring.config.activate.on-profile", "sqlite")
                    .containsEntry("spring.datasource.url", "jdbc:sqlite:${pipker.data.database-root}/pipker.db")
                    .containsEntry("spring.liquibase.change-log", "classpath:db/changelog/db.changelog-master.xml")
                    .doesNotContainKey("pipker.file.local.root");
        }
    }

    private Properties load(String relativePath) throws IOException {
        Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(RESOURCE_DIRECTORY.resolve(relativePath))) {
            properties.load(input);
        }
        return properties;
    }
}
