/**
 * @file SqliteDatabaseConfigurationTests.java
 * @project Pipker Framework
 * @module Pipker Server
 * @description 验证 SQLite Profile、SQLite 专用 Liquibase changelog 和系统表初始化。
 * @logic 使用随机临时 SQLite 文件启动完整应用上下文，检查表结构、约束、种子数据和 Liquibase 幂等性。
 * @dependencies Spring Boot Test、SQLite JDBC、Liquibase、JdbcTemplate
 * @index_tags server、test、sqlite、liquibase、configuration
 * @author holic512
 */
package com.pipker.server;

import liquibase.integration.spring.SpringLiquibase;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles({"dev", "sqlite"})
class SqliteDatabaseConfigurationTests {

    private static final Path SQLITE_DATABASE = Path.of(
            "target", "pipker-sqlite-integration-" + UUID.randomUUID() + ".db"
    ).toAbsolutePath();

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private SpringLiquibase springLiquibase;

    @Value("${spring.datasource.driver-class-name}")
    private String driverClassName;

    @Value("${spring.profiles.default}")
    private String defaultProfiles;

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
    void sqliteProfileUsesDedicatedChangelogAndCreatesSystemSchema() {
        assertThat(driverClassName).isEqualTo("org.sqlite.JDBC");
        assertThat(defaultProfiles).isEqualTo("dev,sqlite");
        assertThat(changeLog).isEqualTo("classpath:db/changelog/sqlite/db.changelog-master.yaml");
        assertThat(springLiquibase.getChangeLog()).isEqualTo(changeLog);

        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sqlite_master WHERE type = 'table' AND name LIKE 'system_%'",
                Integer.class
        )).isEqualTo(7);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM databasechangelog",
                Integer.class
        )).isEqualTo(8);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM system_user WHERE username = 'admin'",
                Integer.class
        )).isEqualTo(1);

        String userRoleDefinition = jdbcTemplate.queryForObject(
                "SELECT sql FROM sqlite_master WHERE type = 'table' AND name = 'system_user_role'",
                String.class
        );
        assertThat(userRoleDefinition)
                .contains("PRIMARY KEY (user_id, role_id)")
                .contains("FOREIGN KEY (user_id)")
                .contains("FOREIGN KEY (role_id)");
    }

    @Test
    void rerunningLiquibaseDoesNotDuplicateSeedData() throws Exception {
        springLiquibase.afterPropertiesSet();

        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM databasechangelog",
                Integer.class
        )).isEqualTo(8);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM system_user WHERE username = 'admin'",
                Integer.class
        )).isEqualTo(1);
    }
}
