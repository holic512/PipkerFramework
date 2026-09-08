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

import com.pipker.server.config.LocalDataDirectoryInitializer;
import com.pipker.server.config.SqliteDatabaseDirectoryInitializer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
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

@SpringBootTest(properties = "spring.main.web-application-type=none")
@ContextConfiguration(initializers = {
        LocalDataDirectoryInitializer.class,
        SqliteDatabaseDirectoryInitializer.class
})
class DefaultProfileConfigurationTests {

    private static final Path DATA_ROOT = createTemporaryDirectory();
    private static final Path SQLITE_DATABASE = DATA_ROOT
            .resolve("base")
            .resolve("pipker.db");

    @Value("${spring.profiles.default}")
    private String defaultProfiles;

    @Value("${spring.datasource.driver-class-name}")
    private String driverClassName;

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
    void usesDevAndSqliteWhenNoProfileIsExplicitlySelected() {
        assertThat(defaultProfiles).isEqualTo("dev,sqlite");
        assertThat(driverClassName).isEqualTo("org.sqlite.JDBC");
        assertThat(changeLog).isEqualTo("classpath:db/changelog/sqlite/db.changelog-master.yaml");
        assertThat(Files.isRegularFile(SQLITE_DATABASE)).isTrue();
        assertThat(Files.isDirectory(DATA_ROOT.resolve("file"))).isTrue();
        assertThat(Files.isDirectory(DATA_ROOT.resolve("log"))).isTrue();
    }

    private static Path createTemporaryDirectory() {
        try {
            return Files.createTempDirectory("pipker-default-profile-");
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
