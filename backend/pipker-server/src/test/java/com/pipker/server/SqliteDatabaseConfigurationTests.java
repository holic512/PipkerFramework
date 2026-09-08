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

import com.pipker.server.config.LocalDataDirectoryInitializer;
import com.pipker.server.config.SqliteDatabaseDirectoryInitializer;
import liquibase.integration.spring.SpringLiquibase;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles({"dev", "sqlite"})
@ContextConfiguration(initializers = {
        LocalDataDirectoryInitializer.class,
        SqliteDatabaseDirectoryInitializer.class
})
class SqliteDatabaseConfigurationTests {

    private static final Path DATA_ROOT = createTemporaryDirectory();
    private static final Path SQLITE_DATABASE = DATA_ROOT
            .resolve("base")
            .resolve("pipker.db");

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
        registry.add("pipker.data.root", DATA_ROOT::toString);
    }

    @AfterAll
    static void removeTemporaryDatabase() throws IOException {
        deleteDirectory(DATA_ROOT);
    }

    @Test
    void sqliteProfileUsesDedicatedChangelogAndCreatesSystemSchema() {
        assertThat(driverClassName).isEqualTo("org.sqlite.JDBC");
        assertThat(defaultProfiles).isEqualTo("dev,sqlite");
        assertThat(changeLog).isEqualTo("classpath:db/changelog/sqlite/db.changelog-master.yaml");
        assertThat(springLiquibase.getChangeLog()).isEqualTo(changeLog);
        assertThat(Files.isRegularFile(SQLITE_DATABASE)).isTrue();
        assertThat(Files.isDirectory(DATA_ROOT.resolve("file"))).isTrue();
        assertThat(Files.isDirectory(DATA_ROOT.resolve("log"))).isTrue();

        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sqlite_master WHERE type = 'table' AND name LIKE 'system_%'",
                Integer.class
        )).isEqualTo(8);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM databasechangelog",
                Integer.class
        )).isEqualTo(10);
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
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sqlite_master WHERE type = 'table' AND name = 'system_role_menu'",
                Integer.class
        )).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sqlite_master WHERE type = 'table' AND name = 'system_api_resource'",
                Integer.class
        )).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM pragma_table_info('system_menu') WHERE name = 'permission_code'",
                Integer.class
        )).isZero();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM system_permission WHERE permission_type = 'PAGE'",
                Integer.class
        )).isZero();
    }

    @Test
    void rerunningLiquibaseDoesNotDuplicateSeedData() throws Exception {
        springLiquibase.afterPropertiesSet();

        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM databasechangelog",
                Integer.class
        )).isEqualTo(10);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM system_user WHERE username = 'admin'",
                Integer.class
        )).isEqualTo(1);
    }

    private static Path createTemporaryDirectory() {
        try {
            return Files.createTempDirectory("pipker-sqlite-integration-");
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot create temporary data root", exception);
        }
    }

    private static void deleteDirectory(Path directory) throws IOException {
        try (Stream<Path> pathStream = Files.walk(directory)) {
            List<Path> paths = pathStream.sorted(Comparator.reverseOrder()).toList();
            for (Path path : paths) {
                Files.deleteIfExists(path);
            }
        }
    }
}
