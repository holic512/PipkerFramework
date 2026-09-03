/**
 * @file StoredFile.java
 * @project Pipker Framework
 * @module Pipker Spring Boot Starter File
 * @description 描述一次成功本地文件保存后的稳定存储键、相对访问路径和基础元数据。
 * @logic 仅传递调用方可安全持久化或返回的逻辑文件信息，绝不携带物理路径、协议、域名或端口。
 * @dependencies Java 标准库、FileStorageUtils
 * @index_tags starter、file-storage、contract、storage-key、relative-url
 * @author holic512
 */
package com.pipker.starter.file.service;

import com.pipker.starter.file.util.FileStorageUtils;

/**
 * 已保存文件的逻辑描述。
 *
 * @param storageKey 存储根目录下的安全相对键
 * @param accessPath 不含协议、域名和端口的公开访问路径
 * @param originalFilename 调用方提供的规范化原始文件名
 * @param size 已写入字节数
 */
public record StoredFile(
        String storageKey,
        String accessPath,
        String originalFilename,
        long size
) {

    /**
     * 验证返回的持久化元数据保持有效。
     */
    public StoredFile {
        storageKey = FileStorageUtils.normalizeStorageKey(storageKey);
        accessPath = FileStorageUtils.normalizeAccessPath(accessPath);
        if (!accessPath.endsWith("/" + storageKey)) {
            throw new IllegalArgumentException("accessPath must end with storageKey");
        }
        if (originalFilename == null) {
            originalFilename = "";
        }
        if (size < 0) {
            throw new IllegalArgumentException("size must not be negative");
        }
    }
}
