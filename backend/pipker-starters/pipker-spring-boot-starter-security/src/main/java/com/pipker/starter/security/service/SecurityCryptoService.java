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

/**
 * 面向业务模块的统一密码哈希与字段加解密接口。
 */
public interface SecurityCryptoService {

    /**
     * 使用当前配置的密码算法生成带算法标识的密码哈希。
     *
     * @param rawPassword 原始密码
     * @return 可持久化的密码哈希
     */
    String hashPassword(CharSequence rawPassword);

    /**
     * 校验原始密码是否匹配已存储的、可识别的密码哈希。
     *
     * @param rawPassword 待校验的原始密码
     * @param storedHash 已存储的密码哈希
     * @return 密码匹配时返回 {@code true}
     */
    boolean matchesPassword(CharSequence rawPassword, String storedHash);

    /**
     * 判断已存储的密码哈希是否需要按当前配置重新生成。
     *
     * @param storedHash 已存储的密码哈希
     * @return 需要升级或格式不可识别时返回 {@code true}
     */
    boolean needsPasswordUpgrade(String storedHash);

    /**
     * 使用当前配置的可逆加密算法加密明文。
     *
     * @param plainText 待加密的明文
     * @return 带算法和版本标识的密文
     */
    String encrypt(String plainText);

    /**
     * 解密当前配置算法生成的密文。
     *
     * @param cipherText 带算法和版本标识的密文
     * @return 解密后的明文
     */
    String decrypt(String cipherText);
}
