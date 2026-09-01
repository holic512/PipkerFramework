/**
 * 文件：SecurityCryptoService.java
 * 项目：Pipker Framework
 * 模块：Pipker Spring Boot Starter Security
 * 说明：定义面向应用的统一入口，用于密码哈希和选定的字段加密。
 * 处理逻辑：对业务调用方隐藏算法选择，由配置决定当前密码和可逆加密的实现。
 * 依赖：Java 标准库
 * 检索关键词：starter、安全、加密、密码、公开接口
 * 作者：holic512
 */
package com.pipker.starter.security.service;

public interface SecurityCryptoService {

    String hashPassword(CharSequence rawPassword);

    boolean matchesPassword(CharSequence rawPassword, String storedHash);

    boolean needsPasswordUpgrade(String storedHash);

    String encrypt(String plainText);

    String decrypt(String cipherText);
}
