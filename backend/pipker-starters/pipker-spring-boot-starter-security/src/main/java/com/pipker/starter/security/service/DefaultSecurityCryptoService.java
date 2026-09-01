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

public class DefaultSecurityCryptoService implements SecurityCryptoService {

    private static final String BCRYPT_PREFIX = "{bcrypt}";
    private static final String PBKDF2_PREFIX = "{pbkdf2-sha256}";
    private static final String AES_ENVELOPE_PREFIX = "v1:aes-gcm:";
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

    @Override
    public String hashPassword(CharSequence rawPassword) {
        String password = requirePassword(rawPassword);
        return switch (properties.getPassword().getAlgorithm()) {
            case BCRYPT -> BCRYPT_PREFIX + bcryptPasswordEncoder.encode(password);
            case PBKDF2_SHA256 -> hashPbkdf2(password);
        };
    }

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

    @Override
    public String encrypt(String plainText) {
        Objects.requireNonNull(plainText, "plainText must not be null");
        return switch (properties.getEncryption().getAlgorithm()) {
            case AES_GCM -> encryptAes(plainText);
            case RSA_OAEP_SHA256 -> encryptRsa(plainText);
        };
    }

    @Override
    public String decrypt(String cipherText) {
        Objects.requireNonNull(cipherText, "cipherText must not be null");
        return switch (properties.getEncryption().getAlgorithm()) {
            case AES_GCM -> decryptAes(cipherText);
            case RSA_OAEP_SHA256 -> decryptRsa(cipherText);
        };
    }

    private String hashPbkdf2(String password) {
        byte[] salt = new byte[properties.getPassword().getPbkdf2SaltLength()];
        secureRandom.nextBytes(salt);
        byte[] hash = derivePbkdf2(password, salt, properties.getPassword().getPbkdf2Iterations(), properties.getPassword().getPbkdf2HashWidth());
        return PBKDF2_PREFIX
                + properties.getPassword().getPbkdf2Iterations() + '$'
                + Base64.getEncoder().encodeToString(salt) + '$'
                + Base64.getEncoder().encodeToString(hash);
    }

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

    private SecretKeySpec loadAesKey(String encodedKey) {
        byte[] key = decodeRequiredBase64(encodedKey, "AES-GCM key");
        if (key.length != AES_KEY_BYTES) {
            throw new IllegalStateException("AES-GCM key must decode to exactly 32 bytes");
        }
        return new SecretKeySpec(key, "AES");
    }

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

    private Cipher rsaOaepCipher() throws GeneralSecurityException {
        return Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
    }

    private OAEPParameterSpec oaepParameterSpec() {
        return new OAEPParameterSpec(
                "SHA-256",
                "MGF1",
                MGF1ParameterSpec.SHA256,
                PSource.PSpecified.DEFAULT
        );
    }

    private String requirePassword(CharSequence rawPassword) {
        Objects.requireNonNull(rawPassword, "rawPassword must not be null");
        return rawPassword.toString();
    }
}
