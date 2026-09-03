/**
 * @file DefaultProfileConfigurationTests.java
 * @project Pipker Framework
 * @module Pipker Server
 * @description 验证未显式指定 Profile 时的默认环境和数据库选择。
 * @logic 不设置 ActiveProfiles，使用临时 SQLite 文件启动应用上下文，确认 Spring Boot 采用 dev、sqlite 默认 Profile。
 * @dependencies Spring Boot Test、SQLite JDBC、JUnit Jupiter、AssertJ
 * @index_tags server、test、configuration、profile、sqlite、default
 * @author holic512
 */
package com.pipker.server;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "spring.main.web-application-type=none")
class DefaultProfileConfigurationTests {

    private static final Path SQLITE_DATABASE = Path.of(
            "target", "pipker-default-profile-" + UUID.randomUUID() + ".db"
    ).toAbsolutePath();

    @Value("${spring.profiles.default}")
    private String defaultProfiles;

    @Value("${spring.datasource.driver-class-name}")
    private String driverClassName;

    @Value("${spring.liquibase.change-log}")
    private String changeLog;

    @DynamicPropertySource
    static void sqliteProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:sqlite:" + SQLITE_DATABASE);
        registry.add("spring.datasource.username", () -> "");
        registry.add("spring.datasource.password", () -> "");
        registry.add("spring.datasource.driver-class-name", () -> "org.sqlite.JDBC");
    }

    @AfterAll
    static void removeTemporaryDatabase() throws IOException {
        Files.deleteIfExists(SQLITE_DATABASE);
    }

    @Test
    void usesDevAndSqliteWhenNoProfileIsExplicitlySelected() {
        assertThat(defaultProfiles).isEqualTo("dev,sqlite");
        assertThat(driverClassName).isEqualTo("org.sqlite.JDBC");
        assertThat(changeLog).isEqualTo("classpath:db/changelog/sqlite/db.changelog-master.yaml");
    }
}
