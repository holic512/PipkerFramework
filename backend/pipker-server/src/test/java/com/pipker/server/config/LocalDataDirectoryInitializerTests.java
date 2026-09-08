/**
 * @file LocalDataDirectoryInitializerTests.java
 * @project Pipker Framework
 * @module Pipker Server
 * @description 验证受控本地 data 目录的初始化与路径边界校验。
 * @logic 在隔离临时目录中验证 base、file、log 创建、普通文件冲突及 SQLite 和文件根目录越界拒绝。
 * @dependencies JUnit Jupiter、AssertJ、Java NIO Files、LocalDataDirectoryInitializer
 * @index_tags server、test、configuration、data-root、sqlite、file-storage
 * @author holic512
 */
package com.pipker.server.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

class LocalDataDirectoryInitializerTests {

    @TempDir
    private Path temporaryDirectory;

    @Test
    void createsManagedDirectoriesWithoutCreatingTheSqliteDatabaseFile() {
        Path configuredRoot = temporaryDirectory.resolve("runtime-data");

        Path dataRoot = LocalDataDirectoryInitializer.initializeDirectories(configuredRoot.toString());

        assertThat(dataRoot).isEqualTo(configuredRoot.toAbsolutePath());
        assertThat(Files.isDirectory(dataRoot.resolve(LocalDataDirectoryInitializer.BASE_DIRECTORY))).isTrue();
        assertThat(Files.isDirectory(dataRoot.resolve(LocalDataDirectoryInitializer.FILE_DIRECTORY))).isTrue();
        assertThat(Files.isDirectory(dataRoot.resolve(LocalDataDirectoryInitializer.LOG_DIRECTORY))).isTrue();
        assertThat(Files.exists(dataRoot.resolve(LocalDataDirectoryInitializer.BASE_DIRECTORY)
                .resolve(LocalDataDirectoryInitializer.SQLITE_DATABASE_FILENAME))).isFalse();
    }

    @Test
    void rejectsAManagedDirectoryThatIsAlreadyARegularFile() throws IOException {
        Path configuredRoot = temporaryDirectory.resolve("runtime-data");
        Files.createDirectories(configuredRoot);
        Files.createFile(configuredRoot.resolve(LocalDataDirectoryInitializer.FILE_DIRECTORY));

        assertThatIllegalStateException()
                .isThrownBy(() -> LocalDataDirectoryInitializer.initializeDirectories(configuredRoot.toString()))
                .withMessageContaining("file directory")
                .withMessageContaining("not a directory");
    }

    @Test
    void rejectsFileStorageRootsOutsideTheManagedFileDirectory() {
        Path dataRoot = LocalDataDirectoryInitializer.initializeDirectories(
                temporaryDirectory.resolve("runtime-data").toString()
        );

        assertThatIllegalStateException()
                .isThrownBy(() -> LocalDataDirectoryInitializer.validateFileStorageRoot(
                        dataRoot,
                        temporaryDirectory.resolve("outside-file-root").toString()
                ))
                .withMessageContaining("pipker.file.local.root")
                .withMessageContaining("pipker.data.root");
    }

    @Test
    void rejectsSqliteDatasourcePathsOutsideTheManagedBaseDirectory() {
        Path dataRoot = LocalDataDirectoryInitializer.initializeDirectories(
                temporaryDirectory.resolve("runtime-data").toString()
        );

        assertThatIllegalStateException()
                .isThrownBy(() -> LocalDataDirectoryInitializer.validateSqliteDatasource(
                        dataRoot,
                        "jdbc:sqlite:" + temporaryDirectory.resolve("outside.db")
                ))
                .withMessageContaining("spring.datasource.url")
                .withMessageContaining("managed SQLite database");
    }
}
