/**
 * 文件：PasswordAlgorithm.java
 * 项目：Pipker Framework
 * 模块：Pipker Spring Boot Starter Security
 * 说明：列出支持的单向密码哈希算法。
 * 处理逻辑：配置为新存储的密码选择一种算法，同时服务可以校验已识别的历史格式。
 * 依赖：Java 标准库
 * 检索关键词：starter、安全、密码、哈希、配置
 * 作者：holic512
 */
package com.pipker.starter.security.config;

/**
 * 支持的单向密码哈希算法。
 */
public enum PasswordAlgorithm {

    /**
     * 使用 BCrypt 哈希密码。
     */
    BCRYPT,

    /**
     * 使用 PBKDF2-HMAC-SHA256 哈希密码。
     */
    PBKDF2_SHA256
}
