/**
 * @file LocalDataDirectoryInitializer.java
 * @project Pipker Framework
 * @module Pipker Server
 * @description 在应用基础设施启动前准备并约束独立配置的本地持久化目录。
 * @logic 所有 Profile 均创建文件、日志目录；仅在 sqlite Profile 激活时创建数据库目录，并校验 SQLite URL 固定指向该目录下的 pipker.db。
 * @dependencies Spring Context、Spring Core Environment、Java NIO Files、SQLiteDatabaseDirectoryInitializer
 * @index_tags server、configuration、bootstrap、data-root、local-storage、sqlite、file-storage
 * @author holic512
 */
package com.pipker.server.config;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Arrays;

/**
 * 初始化受控的本地运行时数据目录，并阻止 SQLite 和文件存储逃离该目录。
 */
public final class LocalDataDirectoryInitializer
        implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    static final String LOG_ROOT_PROPERTY = "pipker.data.log-root";
    static final String DATABASE_ROOT_PROPERTY = "pipker.data.database-root";
    static final String FILE_STORAGE_ROOT_PROPERTY = "pipker.file.local.root";
    private static final String DATASOURCE_URL_PROPERTY = "spring.datasource.url";
    private static final String SQLITE_PROFILE = "sqlite";
    static final String SQLITE_DATABASE_FILENAME = "pipker.db";

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        Environment environment = applicationContext.getEnvironment();
        initializeDirectory(
                environment.getProperty(FILE_STORAGE_ROOT_PROPERTY),
                FILE_STORAGE_ROOT_PROPERTY,
                "file directory"
        );
        initializeDirectory(
                environment.getProperty(LOG_ROOT_PROPERTY),
                LOG_ROOT_PROPERTY,
                "log directory"
        );
        if (isSqliteProfileSelected(environment)) {
            Path databaseRoot = initializeDirectory(
                    environment.getProperty(DATABASE_ROOT_PROPERTY),
                    DATABASE_ROOT_PROPERTY,
                    "SQLite database directory"
            );
            validateSqliteDatasource(databaseRoot, environment.getProperty(DATASOURCE_URL_PROPERTY));
        }
    }

    /**
     * 创建单个受控持久化目录。
     *
     * @param configuredDirectory 已配置的目录路径
     * @param propertyName 配置属性名称
     * @param description 目录职责说明
     * @return 规范绝对目录
     */
    static Path initializeDirectory(String configuredDirectory, String propertyName, String description) {
        Path directory = resolveLocalPath(configuredDirectory, propertyName);
        ensureWritableDirectory(directory, description);
        return directory;
    }

    /**
     * 校验 SQLite 数据源固定为 database-root/pipker.db。
     *
     * @param databaseRoot 规范绝对 SQLite 数据库目录
     * @param datasourceUrl 已解析的 JDBC 数据源 URL
     */
    static void validateSqliteDatasource(Path databaseRoot, String datasourceUrl) {
        Path expected = databaseRoot.resolve(SQLITE_DATABASE_FILENAME).normalize();
        Path actual = SqliteDatabaseDirectoryInitializer.resolveLocalDatabaseFile(datasourceUrl);
        if (!expected.equals(actual)) {
            throw new IllegalStateException(
                    DATASOURCE_URL_PROPERTY + " must resolve to the managed SQLite database " + expected
            );
        }
    }

    private static Path resolveLocalPath(String configuredPath, String propertyName) {
        if (configuredPath == null || configuredPath.isBlank()) {
            throw new IllegalStateException(propertyName + " must be a non-blank local filesystem path");
        }
        try {
            return Path.of(configuredPath).toAbsolutePath().normalize();
        } catch (InvalidPathException exception) {
            throw new IllegalStateException(propertyName + " must be a valid local filesystem path", exception);
        }
    }

    private static void ensureWritableDirectory(Path directory, String description) {
        if (Files.exists(directory) && !Files.isDirectory(directory)) {
            throw new IllegalStateException("Managed " + description + " is not a directory: " + directory);
        }
        try {
            Files.createDirectories(directory);
        } catch (IOException | SecurityException exception) {
            throw new IllegalStateException("Cannot create managed " + description + ": " + directory, exception);
        }
        if (!Files.isDirectory(directory)) {
            throw new IllegalStateException("Managed " + description + " is not a directory: " + directory);
        }
        if (!Files.isWritable(directory)) {
            throw new IllegalStateException("Managed " + description + " is not writable: " + directory);
        }
    }

    private static boolean isSqliteProfileSelected(Environment environment) {
        String[] activeProfiles = environment.getActiveProfiles();
        String[] selectedProfiles = activeProfiles.length == 0
                ? environment.getDefaultProfiles()
                : activeProfiles;
        return Arrays.asList(selectedProfiles).contains(SQLITE_PROFILE);
    }
}
