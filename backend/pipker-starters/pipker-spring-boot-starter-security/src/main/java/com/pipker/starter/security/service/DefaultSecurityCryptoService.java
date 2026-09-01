/**
 * 文件：DefaultSecurityCryptoService.java
 * 项目：Pipker Framework
 * 模块：Pipker Spring Boot Starter Security
 * 说明：使用 Spring Security Crypto 和 JDK JCA/JCE 原语实现 Pipker 选择的密码与字段加密算法。
 * 处理逻辑：为密码哈希和密文封装添加算法标识，在构造时校验所选密钥材料，并拒绝算法与密钥不匹配的情况，避免静默降低安全性。
 * 依赖：Pipker Security Properties、Spring Security Crypto、Java Cryptography Architecture
 * 检索关键词：starter、安全、加密、密码、AES、RSA、PBKDF2、BCrypt
 * 作者：holic512
 */
package com.pipker.starter.security.service;

import com.pipker.starter.security.config.EncryptionAlgorithm;
import com.pipker.starter.security.config.PasswordAlgorithm;
import com.pipker.starter.security.config.PipkerSecurityProperties;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PSource;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Base64;
import java.util.Objects;

/**
 * 使用 Spring Security Crypto 与 JDK JCA/JCE 实现密码哈希和字段加密。
 */
public class DefaultSecurityCryptoService implements SecurityCryptoService {

    /**
     * BCrypt 哈希的存储前缀。
     */
    private static final String BCRYPT_PREFIX = "{bcrypt}";
    /**
     * PBKDF2 哈希的存储前缀。
     */
    private static final String PBKDF2_PREFIX = "{pbkdf2-sha256}";
    /**
     * AES-GCM 密文信封前缀。
     */
    private static final String AES_ENVELOPE_PREFIX = "v1:aes-gcm:";
    /**
     * RSA-OAEP 密文信封前缀。
     */
    private static final String RSA_ENVELOPE_PREFIX = "v1:rsa-oaep-sha256:";
    private static final int AES_KEY_BYTES = 32;
    private static final int AES_GCM_NONCE_BYTES = 12;
    private static final int AES_GCM_TAG_BITS = 128;
    private static final int MAX_STORED_PBKDF2_ITERATIONS = 5_000_000;

    private final PipkerSecurityProperties properties;
    private final BCryptPasswordEncoder bcryptPasswordEncoder;
    private final SecureRandom secureRandom = new SecureRandom();
    private final SecretKeySpec aesKey;
    private final RSAPublicKey rsaPublicKey;
    private final RSAPrivateKey rsaPrivateKey;

    /**
     * 创建安全密码学服务，并在初始化阶段校验当前算法所需的密钥材料。
     *
     * @param properties 安全密码学配置
     * @throws NullPointerException 配置为空时抛出
     * @throws IllegalStateException 密钥材料缺失、格式错误或算法不受支持时抛出
     */
    public DefaultSecurityCryptoService(PipkerSecurityProperties properties) {
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
        this.bcryptPasswordEncoder = new BCryptPasswordEncoder(properties.getPassword().getBcryptStrength());

        if (properties.getEncryption().getAlgorithm() == EncryptionAlgorithm.AES_GCM) {
            this.aesKey = loadAesKey(properties.getEncryption().getAesGcmKey());
            this.rsaPublicKey = null;
            this.rsaPrivateKey = null;
        } else if (properties.getEncryption().getAlgorithm() == EncryptionAlgorithm.RSA_OAEP_SHA256) {
            this.aesKey = null;
            this.rsaPublicKey = loadRsaPublicKey(properties.getEncryption().getRsaOaepPublicKey());
            this.rsaPrivateKey = loadRsaPrivateKey(properties.getEncryption().getRsaOaepPrivateKey());
        } else {
            throw new IllegalStateException("Unsupported encryption algorithm: " + properties.getEncryption().getAlgorithm());
        }
    }

    /**
     * 使用当前配置算法生成带算法前缀的密码哈希。
     *
     * @param rawPassword 原始密码
     * @return 可持久化的密码哈希
     * @throws NullPointerException 原始密码为空时抛出
     */
    @Override
    public String hashPassword(CharSequence rawPassword) {
        String password = requirePassword(rawPassword);
        return switch (properties.getPassword().getAlgorithm()) {
            case BCRYPT -> BCRYPT_PREFIX + bcryptPasswordEncoder.encode(password);
            case PBKDF2_SHA256 -> hashPbkdf2(password);
        };
    }

    /**
     * 根据哈希前缀选择兼容的算法校验密码。
     *
     * @param rawPassword 待校验的原始密码
     * @param storedHash 已存储的密码哈希
     * @return 密码匹配时返回 {@code true}
     */
    @Override
    public boolean matchesPassword(CharSequence rawPassword, String storedHash) {
        if (rawPassword == null || storedHash == null) {
            return false;
        }
        String password = rawPassword.toString();
        if (storedHash.startsWith(BCRYPT_PREFIX)) {
            return bcryptPasswordEncoder.matches(password, storedHash.substring(BCRYPT_PREFIX.length()));
        }
        if (storedHash.startsWith(PBKDF2_PREFIX)) {
            return matchesPbkdf2(password, storedHash.substring(PBKDF2_PREFIX.length()));
        }
        return false;
    }

    /**
     * 判断密码哈希是否不是当前配置格式或成本参数已过时。
     *
     * @param storedHash 已存储的密码哈希
     * @return 需要升级时返回 {@code true}
     */
    @Override
    public boolean needsPasswordUpgrade(String storedHash) {
        if (storedHash == null) {
            return true;
        }
        return switch (properties.getPassword().getAlgorithm()) {
            case BCRYPT -> !storedHash.startsWith(BCRYPT_PREFIX)
                    || bcryptPasswordEncoder.upgradeEncoding(storedHash.substring(BCRYPT_PREFIX.length()));
            case PBKDF2_SHA256 -> needsPbkdf2Upgrade(storedHash);
        };
    }

    /**
     * 使用当前配置的 AES-GCM 或 RSA-OAEP 算法加密明文。
     *
     * @param plainText 待加密的明文
     * @return 带版本和算法前缀的密文
     * @throws NullPointerException 明文为空时抛出
     * @throws IllegalStateException 底层加密操作失败时抛出
     */
    @Override
    public String encrypt(String plainText) {
        Objects.requireNonNull(plainText, "plainText must not be null");
        return switch (properties.getEncryption().getAlgorithm()) {
            case AES_GCM -> encryptAes(plainText);
            case RSA_OAEP_SHA256 -> encryptRsa(plainText);
        };
    }

    /**
     * 解密当前配置算法生成的密文信封。
     *
     * @param cipherText 带版本和算法前缀的密文
     * @return 解密后的明文
     * @throws NullPointerException 密文为空时抛出
     * @throws IllegalArgumentException 密文格式、算法前缀或认证校验无效时抛出
     */
    @Override
    public String decrypt(String cipherText) {
        Objects.requireNonNull(cipherText, "cipherText must not be null");
        return switch (properties.getEncryption().getAlgorithm()) {
            case AES_GCM -> decryptAes(cipherText);
            case RSA_OAEP_SHA256 -> decryptRsa(cipherText);
        };
    }

    /**
     * 生成包含迭代次数、Base64 盐和 Base64 哈希的 PBKDF2 存储值。
     */
    private String hashPbkdf2(String password) {
        byte[] salt = new byte[properties.getPassword().getPbkdf2SaltLength()];
        secureRandom.nextBytes(salt);
        byte[] hash = derivePbkdf2(password, salt, properties.getPassword().getPbkdf2Iterations(), properties.getPassword().getPbkdf2HashWidth());
        return PBKDF2_PREFIX
                + properties.getPassword().getPbkdf2Iterations() + '$'
                + Base64.getEncoder().encodeToString(salt) + '$'
                + Base64.getEncoder().encodeToString(hash);
    }

    /**
     * 解析并恒定时间比较 PBKDF2 存储值。
     */
    private boolean matchesPbkdf2(String password, String encodedHash) {
        String[] segments = encodedHash.split("\\$", -1);
        if (segments.length != 3) {
            return false;
        }
        try {
            int iterations = Integer.parseInt(segments[0]);
            if (iterations < 10_000 || iterations > MAX_STORED_PBKDF2_ITERATIONS) {
                return false;
            }
            byte[] salt = Base64.getDecoder().decode(segments[1]);
            byte[] expectedHash = Base64.getDecoder().decode(segments[2]);
            byte[] actualHash = derivePbkdf2(password, salt, iterations, expectedHash.length * Byte.SIZE);
            return MessageDigest.isEqual(expectedHash, actualHash);
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    /**
     * 判断 PBKDF2 存储值的迭代次数和输出宽度是否需要升级。
     */
    private boolean needsPbkdf2Upgrade(String storedHash) {
        if (!storedHash.startsWith(PBKDF2_PREFIX)) {
            return true;
        }
        String[] segments = storedHash.substring(PBKDF2_PREFIX.length()).split("\\$", -1);
        if (segments.length != 3) {
            return true;
        }
        try {
            int iterations = Integer.parseInt(segments[0]);
            int hashWidth = Base64.getDecoder().decode(segments[2]).length * Byte.SIZE;
            return iterations != properties.getPassword().getPbkdf2Iterations()
                    || hashWidth != properties.getPassword().getPbkdf2HashWidth();
        } catch (IllegalArgumentException exception) {
            return true;
        }
    }

    /**
     * 派生 PBKDF2-HMAC-SHA256 密钥，并在完成后清理密码字符数组。
     */
    private byte[] derivePbkdf2(String password, byte[] salt, int iterations, int hashWidth) {
        char[] passwordCharacters = password.toCharArray();
        PBEKeySpec specification = new PBEKeySpec(passwordCharacters, salt, iterations, hashWidth);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                    .generateSecret(specification)
                    .getEncoded();
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("PBKDF2-HMAC-SHA256 is unavailable", exception);
        } finally {
            specification.clearPassword();
            Arrays.fill(passwordCharacters, '\0');
        }
    }

    /**
     * 使用随机 GCM nonce 加密明文并组装 AES-GCM 信封。
     */
    private String encryptAes(String plainText) {
        byte[] nonce = new byte[AES_GCM_NONCE_BYTES];
        secureRandom.nextBytes(nonce);
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, aesKey, new GCMParameterSpec(AES_GCM_TAG_BITS, nonce));
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            return AES_ENVELOPE_PREFIX
                    + Base64.getUrlEncoder().withoutPadding().encodeToString(nonce) + ':'
                    + Base64.getUrlEncoder().withoutPadding().encodeToString(encrypted);
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("AES-GCM encryption failed", exception);
        }
    }

    /**
     * 校验 AES-GCM 信封、恢复 nonce 并验证认证标签后解密。
     */
    private String decryptAes(String cipherText) {
        if (!cipherText.startsWith(AES_ENVELOPE_PREFIX)) {
            throw new IllegalArgumentException("Ciphertext was not produced by the active AES-GCM algorithm");
        }
        String[] segments = cipherText.substring(AES_ENVELOPE_PREFIX.length()).split(":", -1);
        if (segments.length != 2) {
            throw new IllegalArgumentException("AES-GCM ciphertext envelope is invalid");
        }
        try {
            byte[] nonce = Base64.getUrlDecoder().decode(segments[0]);
            if (nonce.length != AES_GCM_NONCE_BYTES) {
                throw new IllegalArgumentException("AES-GCM ciphertext nonce is invalid");
            }
            byte[] encrypted = Base64.getUrlDecoder().decode(segments[1]);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, aesKey, new GCMParameterSpec(AES_GCM_TAG_BITS, nonce));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException exception) {
            throw new IllegalArgumentException("AES-GCM ciphertext cannot be decrypted", exception);
        }
    }

    /**
     * 使用 RSA 公钥和固定 OAEP 参数加密明文。
     */
    private String encryptRsa(String plainText) {
        try {
            Cipher cipher = rsaOaepCipher();
            cipher.init(Cipher.ENCRYPT_MODE, rsaPublicKey, oaepParameterSpec());
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            return RSA_ENVELOPE_PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(encrypted);
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("RSA-OAEP-SHA-256 encryption failed", exception);
        }
    }

    /**
     * 使用 RSA 私钥和固定 OAEP 参数解密密文信封。
     */
    private String decryptRsa(String cipherText) {
        if (!cipherText.startsWith(RSA_ENVELOPE_PREFIX)) {
            throw new IllegalArgumentException("Ciphertext was not produced by the active RSA-OAEP-SHA-256 algorithm");
        }
        try {
            byte[] encrypted = Base64.getUrlDecoder().decode(cipherText.substring(RSA_ENVELOPE_PREFIX.length()));
            Cipher cipher = rsaOaepCipher();
            cipher.init(Cipher.DECRYPT_MODE, rsaPrivateKey, oaepParameterSpec());
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException exception) {
            throw new IllegalArgumentException("RSA-OAEP-SHA-256 ciphertext cannot be decrypted", exception);
        }
    }

    /**
     * 解码并校验 256 位 AES-GCM 密钥。
     */
    private SecretKeySpec loadAesKey(String encodedKey) {
        byte[] key = decodeRequiredBase64(encodedKey, "AES-GCM key");
        if (key.length != AES_KEY_BYTES) {
            throw new IllegalStateException("AES-GCM key must decode to exactly 32 bytes");
        }
        return new SecretKeySpec(key, "AES");
    }

    /**
     * 解码并加载 X.509 DER 格式的 RSA 公钥。
     */
    private RSAPublicKey loadRsaPublicKey(String encodedKey) {
        try {
            PublicKey key = KeyFactory.getInstance("RSA")
                    .generatePublic(new X509EncodedKeySpec(decodeRequiredBase64(encodedKey, "RSA-OAEP public key")));
            if (!(key instanceof RSAPublicKey rsaKey)) {
                throw new IllegalStateException("RSA-OAEP public key is not an RSA key");
            }
            return rsaKey;
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("RSA-OAEP public key is invalid", exception);
        }
    }

    /**
     * 解码并加载 PKCS#8 DER 格式的 RSA 私钥。
     */
    private RSAPrivateKey loadRsaPrivateKey(String encodedKey) {
        try {
            PrivateKey key = KeyFactory.getInstance("RSA")
                    .generatePrivate(new PKCS8EncodedKeySpec(decodeRequiredBase64(encodedKey, "RSA-OAEP private key")));
            if (!(key instanceof RSAPrivateKey rsaKey)) {
                throw new IllegalStateException("RSA-OAEP private key is not an RSA key");
            }
            return rsaKey;
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("RSA-OAEP private key is invalid", exception);
        }
    }

    /**
     * 解码必填的 Base64 配置值，并将配置错误转换为启动失败。
     */
    private byte[] decodeRequiredBase64(String encodedValue, String description) {
        if (encodedValue == null || encodedValue.isBlank()) {
            throw new IllegalStateException(description + " must be configured");
        }
        try {
            return Base64.getDecoder().decode(encodedValue.trim());
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException(description + " must be Base64 encoded", exception);
        }
    }

    /**
     * 创建 RSA-OAEP-SHA-256 Cipher 实例。
     */
    private Cipher rsaOaepCipher() throws GeneralSecurityException {
        return Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
    }

    /**
     * 返回 RSA-OAEP 使用的 SHA-256 和 MGF1 参数。
     */
    private OAEPParameterSpec oaepParameterSpec() {
        return new OAEPParameterSpec(
                "SHA-256",
                "MGF1",
                MGF1ParameterSpec.SHA256,
                PSource.PSpecified.DEFAULT
        );
    }

    /**
     * 校验并转换原始密码，保持调用方传入的密码语义不变。
     */
    private String requirePassword(CharSequence rawPassword) {
        Objects.requireNonNull(rawPassword, "rawPassword must not be null");
        return rawPassword.toString();
    }
}
