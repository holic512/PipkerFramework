package com.pipker.starter.security;

import com.pipker.starter.security.config.EncryptionAlgorithm;
import com.pipker.starter.security.config.PasswordAlgorithm;
import com.pipker.starter.security.config.PipkerSecurityProperties;
import com.pipker.starter.security.service.DefaultSecurityCryptoService;
import com.pipker.starter.security.service.SecurityCryptoService;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

class DefaultSecurityCryptoServiceTests {

    @Test
    void bcryptHashesPasswordsAndMarksPbkdf2HashesForUpgrade() {
        SecurityCryptoService bcryptService = new DefaultSecurityCryptoService(aesProperties(PasswordAlgorithm.BCRYPT));
        String bcryptHash = bcryptService.hashPassword("password-for-test");

        assertThat(bcryptHash).startsWith("{bcrypt}");
        assertThat(bcryptService.matchesPassword("password-for-test", bcryptHash)).isTrue();
        assertThat(bcryptService.needsPasswordUpgrade(bcryptHash)).isFalse();

        SecurityCryptoService pbkdf2Service = new DefaultSecurityCryptoService(aesProperties(PasswordAlgorithm.PBKDF2_SHA256));
        String pbkdf2Hash = pbkdf2Service.hashPassword("password-for-test");

        assertThat(pbkdf2Hash).startsWith("{pbkdf2-sha256}");
        assertThat(bcryptService.matchesPassword("password-for-test", pbkdf2Hash)).isTrue();
        assertThat(bcryptService.needsPasswordUpgrade(pbkdf2Hash)).isTrue();
    }

    @Test
    void pbkdf2HashesPasswordsAndRecognizesTheConfiguredParameters() {
        SecurityCryptoService service = new DefaultSecurityCryptoService(aesProperties(PasswordAlgorithm.PBKDF2_SHA256));
        String hash = service.hashPassword("password-for-test");

        assertThat(service.matchesPassword("password-for-test", hash)).isTrue();
        assertThat(service.matchesPassword("wrong-password", hash)).isFalse();
        assertThat(service.needsPasswordUpgrade(hash)).isFalse();
    }

    @Test
    void aesGcmEncryptsAndDecryptsWithAnAlgorithmEnvelope() {
        SecurityCryptoService service = new DefaultSecurityCryptoService(aesProperties(PasswordAlgorithm.BCRYPT));
        String encrypted = service.encrypt("sensitive-field-value");

        assertThat(encrypted).startsWith("v1:aes-gcm:");
        assertThat(service.decrypt(encrypted)).isEqualTo("sensitive-field-value");
        assertThatIllegalArgumentException().isThrownBy(() -> service.decrypt("v1:rsa-oaep-sha256:value"));
    }

    @Test
    void rsaOaepEncryptsAndDecryptsWithRuntimeGeneratedKeys() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair keyPair = generator.generateKeyPair();

        PipkerSecurityProperties properties = new PipkerSecurityProperties();
        properties.getEncryption().setAlgorithm(EncryptionAlgorithm.RSA_OAEP_SHA256);
        properties.getEncryption().setRsaOaepPublicKey(Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded()));
        properties.getEncryption().setRsaOaepPrivateKey(Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded()));
        properties.getPassword().setBcryptStrength(4);

        SecurityCryptoService service = new DefaultSecurityCryptoService(properties);
        String encrypted = service.encrypt("rsa-field-value");

        assertThat(encrypted).startsWith("v1:rsa-oaep-sha256:");
        assertThat(service.decrypt(encrypted)).isEqualTo("rsa-field-value");
    }

    @Test
    void selectedAesAlgorithmRejectsMissingOrInvalidKeysImmediately() {
        PipkerSecurityProperties missingKey = new PipkerSecurityProperties();
        missingKey.getEncryption().setAesGcmKey(null);

        assertThatIllegalStateException().isThrownBy(() -> new DefaultSecurityCryptoService(missingKey))
                .withMessageContaining("AES-GCM key must be configured");

        PipkerSecurityProperties shortKey = new PipkerSecurityProperties();
        shortKey.getEncryption().setAesGcmKey(Base64.getEncoder().encodeToString(new byte[16]));

        assertThatIllegalStateException().isThrownBy(() -> new DefaultSecurityCryptoService(shortKey))
                .withMessageContaining("exactly 32 bytes");
    }

    private PipkerSecurityProperties aesProperties(PasswordAlgorithm passwordAlgorithm) {
        byte[] aesKey = new byte[32];
        new SecureRandom().nextBytes(aesKey);

        PipkerSecurityProperties properties = new PipkerSecurityProperties();
        properties.getPassword().setAlgorithm(passwordAlgorithm);
        properties.getPassword().setBcryptStrength(4);
        properties.getPassword().setPbkdf2Iterations(10_000);
        properties.getEncryption().setAlgorithm(EncryptionAlgorithm.AES_GCM);
        properties.getEncryption().setAesGcmKey(Base64.getEncoder().encodeToString(aesKey));
        return properties;
    }
}
