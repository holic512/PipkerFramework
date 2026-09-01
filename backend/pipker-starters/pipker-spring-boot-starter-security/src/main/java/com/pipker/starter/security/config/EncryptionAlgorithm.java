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

public enum EncryptionAlgorithm {
    AES_GCM,
    RSA_OAEP_SHA256
}
