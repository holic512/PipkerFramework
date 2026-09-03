/**
 * @file LocalFileStorageService.java
 * @project Pipker Framework
 * @module Pipker Spring Boot Starter File
 * @description 使用本地文件系统实现安全文件保存、读取、查询、删除和相对访问路径生成。
 * @logic 以日期目录和 Base62 随机键写入临时文件后移动到最终位置；所有调用方键均经过规范化校验并限制在配置根目录内。
 * @dependencies Java NIO、Spring Core Resource、Pipker RandomIdentifierUtils、FileStorageUtils
 * @index_tags starter、file-storage、local、nio、atomic-write、path-security
 * @author holic512
 */
package com.pipker.starter.file.service;

import com.pipker.starter.common.util.RandomIdentifierUtils;
import com.pipker.starter.file.util.FileStorageUtils;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/**
 * 默认的本地文件系统存储服务。
 */
public final class LocalFileStorageService implements FileStorageService {

    private static final DateTimeFormatter DATE_DIRECTORY_FORMAT = DateTimeFormatter.ofPattern("yyyy/MM/dd");
    private static final int KEY_GENERATION_ATTEMPTS = 16;

    private final Path storageRoot;
    private final String accessPath;

    /**
     * 创建本地文件存储服务。
     *
     * @param storageRoot 文件根目录
     * @param accessPath 不含域名的公开访问前缀
     */
    public LocalFileStorageService(Path storageRoot, String accessPath) {
        this.storageRoot = Objects.requireNonNull(storageRoot, "storageRoot must not be null")
                .toAbsolutePath()
                .normalize();
        this.accessPath = FileStorageUtils.normalizeAccessPath(accessPath);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public StoredFile store(InputStream content, String originalFilename) {
        Objects.requireNonNull(content, "content must not be null");
        ensureStorageRoot();

        String normalizedOriginalFilename = FileStorageUtils.normalizeOriginalFilename(originalFilename);
        String extension = FileStorageUtils.safeExtension(normalizedOriginalFilename);
        for (int attempt = 0; attempt < KEY_GENERATION_ATTEMPTS; attempt++) {
            String storageKey = nextStorageKey(extension);
            Path target = resolveStoragePath(storageKey);
            if (Files.exists(target, LinkOption.NOFOLLOW_LINKS)) {
                continue;
            }
            return writeNewFile(content, storageKey, target, normalizedOriginalFilename);
        }

        throw new FileStorageException("Could not allocate a unique file storage key.");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Resource load(String storageKey) {
        Path file = resolveStoragePath(storageKey);
        if (!Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)) {
            throw new FileStorageException("File does not exist.");
        }
        return new FileSystemResource(file);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean exists(String storageKey) {
        return Files.isRegularFile(resolveStoragePath(storageKey), LinkOption.NOFOLLOW_LINKS);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean delete(String storageKey) {
        Path file = resolveStoragePath(storageKey);
        if (!Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)) {
            return false;
        }

        try {
            return Files.deleteIfExists(file);
        } catch (IOException exception) {
            throw new FileStorageException("Failed to delete file.", exception);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String accessPath(String storageKey) {
        return FileStorageUtils.toAccessPath(accessPath, storageKey);
    }

    /**
     * 确保根目录只在真正写入时被创建，且它是目录。
     */
    private void ensureStorageRoot() {
        try {
            Files.createDirectories(storageRoot);
            if (!Files.isDirectory(storageRoot)) {
                throw new FileStorageException("File storage root is not a directory.");
            }
        } catch (IOException exception) {
            throw new FileStorageException("Failed to initialize file storage.", exception);
        }
    }

    /**
     * 将一次写入落盘到临时文件后再移动到最终位置。
     */
    private StoredFile writeNewFile(
            InputStream content,
            String storageKey,
            Path target,
            String originalFilename
    ) {
        Path temporaryFile = null;
        try {
            Files.createDirectories(target.getParent());
            temporaryFile = Files.createTempFile(target.getParent(), ".pipker-", ".tmp");
            long size = Files.copy(content, temporaryFile, StandardCopyOption.REPLACE_EXISTING);
            moveWithoutReplacing(temporaryFile, target);
            return new StoredFile(storageKey, accessPath(storageKey), originalFilename, size);
        } catch (FileAlreadyExistsException exception) {
            throw new FileStorageException("Generated file storage key is already in use.", exception);
        } catch (IOException exception) {
            throw new FileStorageException("Failed to store file.", exception);
        } finally {
            if (temporaryFile != null) {
                try {
                    Files.deleteIfExists(temporaryFile);
                } catch (IOException ignored) {
                    // 临时文件清理失败不覆盖原始读写失败，也不暴露本地路径。
                }
            }
        }
    }

    /**
     * 原子发布已完整写入的临时文件，并且绝不替换同名目标。
     */
    private void moveWithoutReplacing(Path temporaryFile, Path target) throws IOException {
        try {
            Files.createLink(target, temporaryFile);
        } catch (FileAlreadyExistsException exception) {
            throw exception;
        } catch (UnsupportedOperationException | SecurityException exception) {
            Files.move(temporaryFile, target);
            return;
        } catch (IOException exception) {
            Files.move(temporaryFile, target);
            return;
        }

        try {
            Files.deleteIfExists(temporaryFile);
        } catch (IOException ignored) {
            // 已成功发布目标文件；finally 会继续尽力清理遗留临时链接。
        }
    }

    /**
     * 生成日期分区的随机存储键。
     */
    private String nextStorageKey(String extension) {
        return DATE_DIRECTORY_FORMAT.format(LocalDate.now())
                + "/"
                + RandomIdentifierUtils.randomAlphanumeric()
                + extension;
    }

    /**
     * 解析并再次确认调用方键始终位于配置根目录中。
     */
    private Path resolveStoragePath(String storageKey) {
        String normalizedKey = FileStorageUtils.normalizeStorageKey(storageKey);
        Path resolved = storageRoot.resolve(normalizedKey).normalize();
        if (!resolved.startsWith(storageRoot)) {
            throw new IllegalArgumentException("storageKey must remain inside the configured file storage root");
        }
        return resolved;
    }
}
