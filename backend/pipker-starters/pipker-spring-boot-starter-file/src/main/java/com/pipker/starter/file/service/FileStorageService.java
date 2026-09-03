/**
 * @file FileStorageService.java
 * @project Pipker Framework
 * @module Pipker Spring Boot Starter File
 * @description 定义供其他后端模块调用的文件保存、读取、查询、删除和相对访问路径能力。
 * @logic 保存操作生成服务管理的存储键；调用方只能通过存储键或不含域名的 accessPath 引用文件，不直接访问物理路径。
 * @dependencies Spring Core Resource、Java 标准库、StoredFile
 * @index_tags starter、file-storage、service、contract、resource
 * @author holic512
 */
package com.pipker.starter.file.service;

import org.springframework.core.io.Resource;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Objects;

/**
 * 文件持久化服务门面。
 */
public interface FileStorageService {

    /**
     * 保存输入流内容并生成新的存储键。
     *
     * @param content 调用方提供的内容流，服务读取但不关闭该流
     * @param originalFilename 原始文件名，用于安全提取扩展名和返回元数据，可为空
     * @return 已保存文件的逻辑描述
     */
    StoredFile store(InputStream content, String originalFilename);

    /**
     * 保存字节数组内容的便捷重载。
     *
     * @param content 待保存的文件字节
     * @param originalFilename 原始文件名，用于安全提取扩展名和返回元数据，可为空
     * @return 已保存文件的逻辑描述
     */
    default StoredFile store(byte[] content, String originalFilename) {
        Objects.requireNonNull(content, "content must not be null");
        return store(new ByteArrayInputStream(content), originalFilename);
    }

    /**
     * 读取已保存文件的资源描述。
     *
     * @param storageKey 存储根目录下的安全相对键
     * @return 文件资源
     * @throws FileStorageException 当文件不存在或无法读取时抛出
     */
    Resource load(String storageKey);

    /**
     * 判断指定存储键是否对应常规文件。
     *
     * @param storageKey 存储根目录下的安全相对键
     * @return 文件存在时为 {@code true}
     */
    boolean exists(String storageKey);

    /**
     * 删除指定存储键对应的常规文件。
     *
     * @param storageKey 存储根目录下的安全相对键
     * @return 实际删除文件时为 {@code true}，不存在时为 {@code false}
     * @throws FileStorageException 当文件存在但无法删除时抛出
     */
    boolean delete(String storageKey);

    /**
     * 根据存储键生成不含协议、域名和端口的公开访问路径。
     *
     * @param storageKey 存储根目录下的安全相对键
     * @return 根相对公开访问路径
     */
    String accessPath(String storageKey);
}
