/**
 * 文件：PipkerSecurityProperties.java
 * 项目：Pipker Framework
 * 模块：Pipker Spring Boot Starter Security
 * 说明：绑定当前密码哈希和字段加密算法，以及所需的成本参数和密钥设置。
 * 处理逻辑：为每项安全能力限制一个配置的写入算法，同时保留足够的密码参数，用于确定性校验和升级判断。
 * 依赖：Spring Boot Configuration Properties、Jakarta Validation
 * 检索关键词：starter、安全、配置、加密、密码
 * 作者：holic512
 */
package com.pipker.starter.security.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * 绑定 {@code pipker.security.crypto} 下的密码哈希和字段加密配置。
 */
@Validated
@ConfigurationProperties("pipker.security.crypto")
public class PipkerSecurityProperties {

    /**
     * 密码哈希配置。
     */
    @Valid
    @NotNull
    private Password password = new Password();

    /**
     * 可逆字段加密配置。
     */
    @Valid
    @NotNull
    private Encryption encryption = new Encryption();

    /**
     * 返回密码哈希配置。
     *
     * @return 密码哈希配置
     */
    public Password getPassword() {
        return password;
    }

    /**
     * 设置密码哈希配置。
     *
     * @param password 密码哈希配置
     */
    public void setPassword(Password password) {
        this.password = password;
    }

    /**
     * 返回字段加密配置。
     *
     * @return 字段加密配置
     */
    public Encryption getEncryption() {
        return encryption;
    }

    /**
     * 设置字段加密配置。
     *
     * @param encryption 字段加密配置
     */
    public void setEncryption(Encryption encryption) {
        this.encryption = encryption;
    }

    /**
     * 密码哈希算法和成本参数配置。
     */
    public static class Password {

        /**
         * 当前新密码使用的哈希算法，默认为 BCrypt。
         */
        @NotNull
        private PasswordAlgorithm algorithm = PasswordAlgorithm.BCRYPT;

        /**
         * BCrypt 工作因子，默认为 12。
         */
        @Min(4)
        @Max(31)
        private int bcryptStrength = 12;

        /**
         * PBKDF2 迭代次数，默认为 600000。
         */
        @Min(10_000)
        @Max(5_000_000)
        private int pbkdf2Iterations = 600_000;

        /**
         * PBKDF2 盐长度，单位为字节，默认为 16。
         */
        @Min(16)
        @Max(64)
        private int pbkdf2SaltLength = 16;

        /**
         * PBKDF2 输出宽度，单位为 bit，默认为 256。
         */
        @Min(128)
        @Max(1_024)
        private int pbkdf2HashWidth = 256;

        /**
         * 返回当前密码哈希算法。
         *
         * @return 密码哈希算法
         */
        public PasswordAlgorithm getAlgorithm() {
            return algorithm;
        }

        /**
         * 设置当前密码哈希算法。
         *
         * @param algorithm 密码哈希算法
         */
        public void setAlgorithm(PasswordAlgorithm algorithm) {
            this.algorithm = algorithm;
        }

        /**
         * 返回 BCrypt 工作因子。
         *
         * @return BCrypt 工作因子
         */
        public int getBcryptStrength() {
            return bcryptStrength;
        }

        /**
         * 设置 BCrypt 工作因子。
         *
         * @param bcryptStrength BCrypt 工作因子
         */
        public void setBcryptStrength(int bcryptStrength) {
            this.bcryptStrength = bcryptStrength;
        }

        /**
         * 返回 PBKDF2 迭代次数。
         *
         * @return 迭代次数
         */
        public int getPbkdf2Iterations() {
            return pbkdf2Iterations;
        }

        /**
         * 设置 PBKDF2 迭代次数。
         *
         * @param pbkdf2Iterations 迭代次数
         */
        public void setPbkdf2Iterations(int pbkdf2Iterations) {
            this.pbkdf2Iterations = pbkdf2Iterations;
        }

        /**
         * 返回 PBKDF2 盐长度。
         *
         * @return 盐长度，单位为字节
         */
        public int getPbkdf2SaltLength() {
            return pbkdf2SaltLength;
        }

        /**
         * 设置 PBKDF2 盐长度。
         *
         * @param pbkdf2SaltLength 盐长度，单位为字节
         */
        public void setPbkdf2SaltLength(int pbkdf2SaltLength) {
            this.pbkdf2SaltLength = pbkdf2SaltLength;
        }

        /**
         * 返回 PBKDF2 哈希宽度。
         *
         * @return 哈希宽度，单位为 bit
         */
        public int getPbkdf2HashWidth() {
            return pbkdf2HashWidth;
        }

        /**
         * 设置 PBKDF2 哈希宽度。
         *
         * @param pbkdf2HashWidth 哈希宽度，单位为 bit
         */
        public void setPbkdf2HashWidth(int pbkdf2HashWidth) {
            this.pbkdf2HashWidth = pbkdf2HashWidth;
        }
    }

    /**
     * 可逆字段加密算法和密钥材料配置。
     */
    public static class Encryption {

        /**
         * 当前字段加密算法，默认为 AES-GCM。
         */
        @NotNull
        private EncryptionAlgorithm algorithm = EncryptionAlgorithm.AES_GCM;

        /**
         * Base64 编码的 AES-GCM 密钥，解码后必须为 32 字节。
         */
        private String aesGcmKey;

        /**
         * Base64 编码的 RSA X.509 DER 公钥。
         */
        private String rsaOaepPublicKey;

        /**
         * Base64 编码的 RSA PKCS#8 DER 私钥。
         */
        private String rsaOaepPrivateKey;

        /**
         * 返回当前字段加密算法。
         *
         * @return 字段加密算法
         */
        public EncryptionAlgorithm getAlgorithm() {
            return algorithm;
        }

        /**
         * 设置当前字段加密算法。
         *
         * @param algorithm 字段加密算法
         */
        public void setAlgorithm(EncryptionAlgorithm algorithm) {
            this.algorithm = algorithm;
        }

        /**
         * 返回 Base64 编码的 AES-GCM 密钥。
         *
         * @return AES-GCM 密钥
         */
        public String getAesGcmKey() {
            return aesGcmKey;
        }

        /**
         * 设置 Base64 编码的 AES-GCM 密钥。
         *
         * @param aesGcmKey AES-GCM 密钥
         */
        public void setAesGcmKey(String aesGcmKey) {
            this.aesGcmKey = aesGcmKey;
        }

        /**
         * 返回 Base64 编码的 RSA-OAEP 公钥。
         *
         * @return RSA-OAEP 公钥
         */
        public String getRsaOaepPublicKey() {
            return rsaOaepPublicKey;
        }

        /**
         * 设置 Base64 编码的 RSA-OAEP 公钥。
         *
         * @param rsaOaepPublicKey RSA-OAEP 公钥
         */
        public void setRsaOaepPublicKey(String rsaOaepPublicKey) {
            this.rsaOaepPublicKey = rsaOaepPublicKey;
        }

        /**
         * 返回 Base64 编码的 RSA-OAEP 私钥。
         *
         * @return RSA-OAEP 私钥
         */
        public String getRsaOaepPrivateKey() {
            return rsaOaepPrivateKey;
        }

        /**
         * 设置 Base64 编码的 RSA-OAEP 私钥。
         *
         * @param rsaOaepPrivateKey RSA-OAEP 私钥
         */
        public void setRsaOaepPrivateKey(String rsaOaepPrivateKey) {
            this.rsaOaepPrivateKey = rsaOaepPrivateKey;
        }
    }
}
