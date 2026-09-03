/**
 * @file FileStorageUtils.java
 * @project Pipker Framework
 * @module Pipker Spring Boot Starter File
 * @description 提供存储键、文件名扩展和根相对访问路径的无状态规范化工具。
 * @logic 将调用方输入收敛到 URL 与文件系统都安全的逻辑键格式；工具只生成逻辑访问路径，绝不读取主机或代理信息拼接完整 URL。
 * @dependencies Java 标准库
 * @index_tags starter、file-storage、utility、path-validation、relative-url
 * @author holic512
 */
package com.pipker.starter.file.util;

import java.util.StringJoiner;
import java.util.regex.Pattern;

/**
 * 文件存储路径和文件名工具。
 */
public final class FileStorageUtils {

    private static final Pattern SAFE_PATH_SEGMENT = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._-]*");
    private static final Pattern SAFE_EXTENSION = Pattern.compile("[a-z0-9]{1,16}");

    private FileStorageUtils() {
    }

    /**
     * 规范化存储根目录下的逻辑键。
     *
     * @param storageKey 调用方提供的相对键
     * @return 使用 {@code /} 分隔的安全逻辑键
     * @throws IllegalArgumentException 当键为空、不是安全相对路径或包含路径穿越成分时抛出
     */
    public static String normalizeStorageKey(String storageKey) {
        if (storageKey == null) {
            throw new IllegalArgumentException("storageKey must not be null");
        }

        String normalized = storageKey.trim();
        if (normalized.isEmpty() || normalized.startsWith("/") || normalized.contains("\\") || normalized.indexOf('\u0000') >= 0) {
            throw new IllegalArgumentException("storageKey must be a safe relative path");
        }

        String[] segments = normalized.split("/", -1);
        StringJoiner joiner = new StringJoiner("/");
        for (String segment : segments) {
            if (!SAFE_PATH_SEGMENT.matcher(segment).matches()) {
                throw new IllegalArgumentException("storageKey contains an unsafe path segment");
            }
            joiner.add(segment);
        }
        return joiner.toString();
    }

    /**
     * 规范化公开资源访问路径前缀。
     *
     * @param accessPath 以 {@code /} 开头的访问路径前缀
     * @return 不含尾部斜杠的安全绝对 URL 路径
     * @throws IllegalArgumentException 当路径不安全或不以 {@code /} 开头时抛出
     */
    public static String normalizeAccessPath(String accessPath) {
        if (accessPath == null) {
            throw new IllegalArgumentException("accessPath must not be null");
        }

        String normalized = accessPath.trim();
        if (!normalized.startsWith("/") || normalized.contains("\\") || normalized.indexOf('\u0000') >= 0) {
            throw new IllegalArgumentException("accessPath must be a safe absolute URL path");
        }
        while (normalized.length() > 1 && normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        if (normalized.length() == 1) {
            throw new IllegalArgumentException("accessPath must contain at least one path segment");
        }

        String[] segments = normalized.substring(1).split("/", -1);
        StringJoiner joiner = new StringJoiner("/", "/", "");
        for (String segment : segments) {
            if (!SAFE_PATH_SEGMENT.matcher(segment).matches()) {
                throw new IllegalArgumentException("accessPath contains an unsafe path segment");
            }
            joiner.add(segment);
        }
        return joiner.toString();
    }

    /**
     * 判断访问前缀是否可安全规范化。
     *
     * @param accessPath 待校验访问前缀
     * @return 可安全规范化时为 {@code true}
     */
    public static boolean isValidAccessPath(String accessPath) {
        try {
            normalizeAccessPath(accessPath);
            return true;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    /**
     * 拼接不含域名的根相对访问路径。
     *
     * @param accessPath 公开访问路径前缀
     * @param storageKey 存储根目录下的逻辑键
     * @return 例如 {@code /files/2026/09/04/example.txt} 的访问路径
     */
    public static String toAccessPath(String accessPath, String storageKey) {
        return normalizeAccessPath(accessPath) + "/" + normalizeStorageKey(storageKey);
    }

    /**
     * 将调用方提供的原始文件名收敛为不含路径的展示名称。
     *
     * @param originalFilename 原始文件名，可为空
     * @return 不包含目录或控制字符的文件名；未提供时返回空字符串
     */
    public static String normalizeOriginalFilename(String originalFilename) {
        if (originalFilename == null) {
            return "";
        }

        String normalized = originalFilename.trim().replace('\\', '/');
        int separatorIndex = normalized.lastIndexOf('/');
        String leafName = separatorIndex >= 0 ? normalized.substring(separatorIndex + 1) : normalized;
        StringBuilder cleaned = new StringBuilder(leafName.length());
        for (int index = 0; index < leafName.length(); index++) {
            char character = leafName.charAt(index);
            if (!Character.isISOControl(character)) {
                cleaned.append(character);
            }
        }
        return cleaned.toString();
    }

    /**
     * 从规范化原始名称中提取安全、可用于生成文件名的扩展名。
     *
     * @param originalFilename 原始文件名，可为空
     * @return 带 {@code .} 的小写安全扩展名；不存在或不安全时返回空字符串
     */
    public static String safeExtension(String originalFilename) {
        String normalized = normalizeOriginalFilename(originalFilename);
        int dotIndex = normalized.lastIndexOf('.');
        if (dotIndex <= 0 || dotIndex == normalized.length() - 1) {
            return "";
        }

        String extension = normalized.substring(dotIndex + 1).toLowerCase(java.util.Locale.ROOT);
        return SAFE_EXTENSION.matcher(extension).matches() ? "." + extension : "";
    }
}
