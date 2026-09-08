/**
 * @file LocalDataDirectoryInitializerTests.java
 * @project Pipker Framework
 * @module Pipker Server
 * @description 验证独立配置的文件、日志和 SQLite 数据库目录初始化与边界校验。
 * @logic 在隔离临时目录中验证所有 Profile 初始化文件和日志目录，SQLite Profile 额外初始化数据库目录并拒绝目录或数据库 URL 越界。
 * @dependencies JUnit Jupiter、AssertJ、Spring Context、Java NIO Files、LocalDataDirectoryInitializer
 * @index_tags server、test、configuration、persistence-path、sqlite、file-storage、log
 * @author holic512
 */
package com.pipker.server.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.env.MapPropertySource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

class LocalDataDirectoryInitializerTests {

    @TempDir
    private Path temporaryDirectory;

    @Test
    void initializesFileAndLogDirectoriesWithoutRequiringDatabaseDirectoryForNonSqliteProfile() {
        Path fileRoot = temporaryDirectory.resolve("file-storage");
        Path logRoot = temporaryDirectory.resolve("application-log");

        try (GenericApplicationContext applicationContext = applicationContext("mysql", Map.of(
                LocalDataDirectoryInitializer.LOG_ROOT_PROPERTY, logRoot.toString(),
                LocalDataDirectoryInitializer.FILE_STORAGE_ROOT_PROPERTY, fileRoot.toString()
        ))) {
            new LocalDataDirectoryInitializer().initialize(applicationContext);
        }

        assertThat(Files.isDirectory(fileRoot)).isTrue();
        assertThat(Files.isDirectory(logRoot)).isTrue();
        assertThat(Files.exists(temporaryDirectory.resolve("sqlite-data"))).isFalse();
    }

    @Test
    void initializesIndependentDirectoriesForSqliteProfile() {
        Path fileRoot = temporaryDirectory.resolve("file-storage");
        Path logRoot = temporaryDirectory.resolve("application-log");
        Path databaseRoot = temporaryDirectory.resolve("sqlite-data");

        try (GenericApplicationContext applicationContext = applicationContext("sqlite", Map.of(
                LocalDataDirectoryInitializer.LOG_ROOT_PROPERTY, logRoot.toString(),
                LocalDataDirectoryInitializer.DATABASE_ROOT_PROPERTY, databaseRoot.toString(),
                LocalDataDirectoryInitializer.FILE_STORAGE_ROOT_PROPERTY, fileRoot.toString(),
                "spring.datasource.url", "jdbc:sqlite:" + databaseRoot.resolve("pipker.db")
        ))) {
            new LocalDataDirectoryInitializer().initialize(applicationContext);
        }

        assertThat(Files.isDirectory(fileRoot)).isTrue();
        assertThat(Files.isDirectory(logRoot)).isTrue();
        assertThat(Files.isDirectory(databaseRoot)).isTrue();
        assertThat(Files.exists(databaseRoot.resolve("pipker.db"))).isFalse();
    }

    @Test
    void rejectsAConfiguredPersistentDirectoryThatIsAlreadyARegularFile() throws IOException {
        Path fileRoot = temporaryDirectory.resolve("file-storage");
        Files.createFile(fileRoot);

        try (GenericApplicationContext applicationContext = applicationContext("pg", Map.of(
                LocalDataDirectoryInitializer.LOG_ROOT_PROPERTY, temporaryDirectory.resolve("application-log").toString(),
                LocalDataDirectoryInitializer.FILE_STORAGE_ROOT_PROPERTY, fileRoot.toString()
        ))) {
            assertThatIllegalStateException()
                    .isThrownBy(() -> new LocalDataDirectoryInitializer().initialize(applicationContext))
                    .withMessageContaining("file directory")
                    .withMessageContaining("not a directory");
        }
    }

    @Test
    void rejectsSqliteDatasourcePathsOutsideTheConfiguredDatabaseDirectory() {
        Path fileRoot = temporaryDirectory.resolve("file-storage");
        Path databaseRoot = temporaryDirectory.resolve("sqlite-data");

        try (GenericApplicationContext applicationContext = applicationContext("sqlite", Map.of(
                LocalDataDirectoryInitializer.LOG_ROOT_PROPERTY, temporaryDirectory.resolve("application-log").toString(),
                LocalDataDirectoryInitializer.DATABASE_ROOT_PROPERTY, databaseRoot.toString(),
                LocalDataDirectoryInitializer.FILE_STORAGE_ROOT_PROPERTY, fileRoot.toString(),
                "spring.datasource.url", "jdbc:sqlite:" + temporaryDirectory.resolve("outside.db")
        ))) {
            assertThatIllegalStateException()
                    .isThrownBy(() -> new LocalDataDirectoryInitializer().initialize(applicationContext))
                    .withMessageContaining("spring.datasource.url")
                    .withMessageContaining("managed SQLite database");
        }
    }

    @Test
    void validatesConfiguredDirectoriesWithoutCreatingTheSqliteDatabaseFile() {
        Path directory = temporaryDirectory.resolve("custom-directory");

        assertThatCode(() -> LocalDataDirectoryInitializer.initializeDirectory(
                directory.toString(),
                "test.directory",
                "test directory"
        )).doesNotThrowAnyException();
        assertThat(Files.isDirectory(directory)).isTrue();
    }

    private GenericApplicationContext applicationContext(String profile, Map<String, Object> properties) {
        GenericApplicationContext applicationContext = new GenericApplicationContext();
        applicationContext.getEnvironment().setActiveProfiles(profile);
        applicationContext.getEnvironment().getPropertySources()
                .addFirst(new MapPropertySource("test-properties", properties));
        return applicationContext;
    }
}
