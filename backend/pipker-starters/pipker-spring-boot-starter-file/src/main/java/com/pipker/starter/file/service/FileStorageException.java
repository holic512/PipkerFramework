/**
 * @file FileStorageException.java
 * @project Pipker Framework
 * @module Pipker Spring Boot Starter File
 * @description 表示本地文件存储读写过程中的可预期运行时失败。
 * @logic 将底层 I/O 失败转换为稳定异常类型，消息不包含文件系统绝对路径。
 * @dependencies Java 标准库
 * @index_tags starter、file-storage、exception、io
 * @author holic512
 */
package com.pipker.starter.file.service;

/**
 * 文件存储执行失败异常。
 */
public class FileStorageException extends RuntimeException {

    /**
     * 使用说明文本创建异常。
     *
     * @param message 不包含物理路径的失败说明
     */
    public FileStorageException(String message) {
        super(message);
    }

    /**
     * 使用说明文本和根因创建异常。
     *
     * @param message 不包含物理路径的失败说明
     * @param cause 底层 I/O 根因
     */
    public FileStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
