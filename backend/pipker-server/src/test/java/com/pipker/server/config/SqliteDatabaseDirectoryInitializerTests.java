/**
 * @file SqliteDatabaseDirectoryInitializerTests.java
 * @project Pipker Framework
 * @module Pipker Server
 * @description 验证启动阶段为本地文件 SQLite 数据库创建父目录的行为。
 * @logic 对不存在的嵌套 SQLite 路径断言父目录会被创建，并确保内存数据库和其他 JDBC URL 不产生目录写入。
 * @dependencies JUnit Jupiter、AssertJ、Java NIO Files
 * @index_tags server、test、sqlite、datasource、bootstrap
 * @author holic512
 */
package com.pipker.server.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class SqliteDatabaseDirectoryInitializerTests {

    @TempDir
    private Path temporaryDirectory;

    @Test
    void createsMissingParentDirectoriesForFileBasedSqliteDatabase() {
        Path databaseFile = temporaryDirectory.resolve("data").resolve("local").resolve("pipker.db");

        SqliteDatabaseDirectoryInitializer.createParentDirectory("jdbc:sqlite:" + databaseFile);

        assertThat(Files.isDirectory(databaseFile.getParent())).isTrue();
        assertThat(Files.exists(databaseFile)).isFalse();
    }

    @Test
    void ignoresInMemoryAndNonSqliteJdbcUrls() {
        Path databaseFile = temporaryDirectory.resolve("untouched").resolve("pipker.db");

        SqliteDatabaseDirectoryInitializer.createParentDirectory("jdbc:sqlite::memory:");
        SqliteDatabaseDirectoryInitializer.createParentDirectory("jdbc:postgresql://localhost/pipker");

        assertThat(Files.exists(databaseFile.getParent())).isFalse();
    }
}
