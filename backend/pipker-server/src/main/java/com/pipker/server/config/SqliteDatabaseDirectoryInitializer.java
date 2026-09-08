/**
 * @file SqliteDatabaseDirectoryInitializer.java
 * @project Pipker Framework
 * @module Pipker Server
 * @description 在创建 SQLite 数据源前确保数据库文件的父目录存在。
 * @logic 读取配置解析后的 JDBC URL；仅对本地文件 SQLite URL 创建父目录，内存数据库和非 SQLite 数据源保持不变。
 * @dependencies Spring Context、Spring Core Environment、Java NIO Files
 * @index_tags server、configuration、sqlite、datasource、bootstrap
 * @author holic512
 */
package com.pipker.server.config;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;

/**
 * 让文件型 SQLite 数据库可以使用尚不存在的嵌套目录。
 */
public final class SqliteDatabaseDirectoryInitializer
        implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    private static final String SQLITE_JDBC_URL_PREFIX = "jdbc:sqlite:";

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        createParentDirectory(applicationContext.getEnvironment().getProperty("spring.datasource.url"));
    }

    /**
     * 为本地文件 SQLite JDBC URL 创建数据库文件的父目录。
     *
     * @param datasourceUrl 已解析的 JDBC 数据源 URL
     */
    static void createParentDirectory(String datasourceUrl) {
        if (datasourceUrl == null || !datasourceUrl.startsWith(SQLITE_JDBC_URL_PREFIX)) {
            return;
        }

        String databaseLocation = datasourceUrl.substring(SQLITE_JDBC_URL_PREFIX.length());
        if (databaseLocation.isBlank()
                || ":memory:".equals(databaseLocation)
                || databaseLocation.startsWith("file::memory:")
                || databaseLocation.contains("mode=memory")) {
            return;
        }

        Path databaseFile;
        try {
            databaseFile = Path.of(removeFileUrlPrefixAndQuery(databaseLocation));
        } catch (InvalidPathException exception) {
            throw new IllegalStateException(
                    "SQLite datasource URL must reference a valid local database file: " + datasourceUrl,
                    exception
            );
        }

        Path parentDirectory = databaseFile.toAbsolutePath().getParent();
        if (parentDirectory == null) {
            return;
        }

        try {
            Files.createDirectories(parentDirectory);
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Cannot create the SQLite database directory: " + parentDirectory,
                    exception
            );
        }
    }

    private static String removeFileUrlPrefixAndQuery(String databaseLocation) {
        String localLocation = databaseLocation.startsWith("file:")
                ? databaseLocation.substring("file:".length())
                : databaseLocation;
        int queryStart = localLocation.indexOf('?');
        return queryStart < 0 ? localLocation : localLocation.substring(0, queryStart);
    }
}
