/**
 * 文件：EncryptionAlgorithm.java
 * 项目：Pipker Framework
 * 模块：Pipker Spring Boot Starter Security
 * 说明：列出支持的可逆字段加密算法。
 * 处理逻辑：由配置选择一个当前算法，并在应用成功启动前强制校验对应的密钥材料。
 * 依赖：Java 标准库
 * 检索关键词：starter、安全、加密、配置
 * 作者：holic512
 */
package com.pipker.starter.security.config;

/**
 * 支持的可逆字段加密算法。
 */
public enum EncryptionAlgorithm {

    /**
     * 使用 AES-GCM 对称加密。
     */
    AES_GCM,

    /**
     * 使用 RSA-OAEP 与 SHA-256 参数的非对称加密。
     */
    RSA_OAEP_SHA256
}
