/**
 * @file LocalDataDirectoryInitializer.java
 * @project Pipker Framework
 * @module Pipker Server
 * @description 在应用基础设施启动前准备并约束本地 data 根目录及其固定子目录。
 * @logic 解析 pipker.data.root，创建 base、file、log 目录；校验文件存储与 SQLite 的最终路径只能位于约定位置，避免高优先级属性绕过本地目录边界。
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

    static final String DATA_ROOT_PROPERTY = "pipker.data.root";
    static final String FILE_STORAGE_ROOT_PROPERTY = "pipker.file.local.root";
    private static final String DATASOURCE_URL_PROPERTY = "spring.datasource.url";
    private static final String SQLITE_PROFILE = "sqlite";
    static final String BASE_DIRECTORY = "base";
    static final String FILE_DIRECTORY = "file";
    static final String LOG_DIRECTORY = "log";
    static final String SQLITE_DATABASE_FILENAME = "pipker.db";

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        Environment environment = applicationContext.getEnvironment();
        Path dataRoot = initializeDirectories(environment.getProperty(DATA_ROOT_PROPERTY));
        validateFileStorageRoot(dataRoot, environment.getProperty(FILE_STORAGE_ROOT_PROPERTY));
        if (isSqliteProfileSelected(environment)) {
            validateSqliteDatasource(dataRoot, environment.getProperty(DATASOURCE_URL_PROPERTY));
        }
    }

    /**
     * 创建数据根目录及其固定子目录。
     *
     * @param configuredDataRoot 已配置的数据根目录
     * @return 规范绝对数据根目录
     */
    static Path initializeDirectories(String configuredDataRoot) {
        Path dataRoot = resolveLocalPath(configuredDataRoot, DATA_ROOT_PROPERTY);
        ensureWritableDirectory(dataRoot, "data root");
        ensureWritableDirectory(dataRoot.resolve(BASE_DIRECTORY), "base directory");
        ensureWritableDirectory(dataRoot.resolve(FILE_DIRECTORY), "file directory");
        ensureWritableDirectory(dataRoot.resolve(LOG_DIRECTORY), "log directory");
        return dataRoot;
    }

    /**
     * 校验文件存储根目录固定为 data/file。
     *
     * @param dataRoot 规范绝对数据根目录
     * @param configuredFileStorageRoot 已解析的文件根目录
     */
    static void validateFileStorageRoot(Path dataRoot, String configuredFileStorageRoot) {
        Path expected = dataRoot.resolve(FILE_DIRECTORY).normalize();
        Path actual = resolveLocalPath(configuredFileStorageRoot, FILE_STORAGE_ROOT_PROPERTY);
        if (!expected.equals(actual)) {
            throw new IllegalStateException(
                    FILE_STORAGE_ROOT_PROPERTY + " must resolve to " + expected + " under " + DATA_ROOT_PROPERTY
            );
        }
    }

    /**
     * 校验 SQLite 数据源固定为 data/base/pipker.db。
     *
     * @param dataRoot 规范绝对数据根目录
     * @param datasourceUrl 已解析的 JDBC 数据源 URL
     */
    static void validateSqliteDatasource(Path dataRoot, String datasourceUrl) {
        Path expected = dataRoot.resolve(BASE_DIRECTORY).resolve(SQLITE_DATABASE_FILENAME).normalize();
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
