/**
 * @file PipkerSecurityProperties.java
 * @project Pipker Framework
 * @module Pipker Spring Boot Starter Security
 * @description Binds the active password-hash and field-encryption algorithms plus their required cost and key settings.
 * @logic Limits each security capability to one configured write algorithm while retaining sufficient password parameters for deterministic verification and upgrade decisions.
 * @dependencies Spring Boot Configuration Properties, Jakarta Validation
 * @index_tags starter,security,configuration,cryptography,password
 * @author holic512
 */
package com.pipker.starter.security.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("pipker.security.crypto")
public class PipkerSecurityProperties {

    @Valid
    @NotNull
    private Password password = new Password();

    @Valid
    @NotNull
    private Encryption encryption = new Encryption();

    public Password getPassword() {
        return password;
    }

    public void setPassword(Password password) {
        this.password = password;
    }

    public Encryption getEncryption() {
        return encryption;
    }

    public void setEncryption(Encryption encryption) {
        this.encryption = encryption;
    }

    public static class Password {

        @NotNull
        private PasswordAlgorithm algorithm = PasswordAlgorithm.BCRYPT;

        @Min(4)
        @Max(31)
        private int bcryptStrength = 12;

        @Min(10_000)
        @Max(5_000_000)
        private int pbkdf2Iterations = 600_000;

        @Min(16)
        @Max(64)
        private int pbkdf2SaltLength = 16;

        @Min(128)
        @Max(1_024)
        private int pbkdf2HashWidth = 256;

        public PasswordAlgorithm getAlgorithm() {
            return algorithm;
        }

        public void setAlgorithm(PasswordAlgorithm algorithm) {
            this.algorithm = algorithm;
        }

        public int getBcryptStrength() {
            return bcryptStrength;
        }

        public void setBcryptStrength(int bcryptStrength) {
            this.bcryptStrength = bcryptStrength;
        }

        public int getPbkdf2Iterations() {
            return pbkdf2Iterations;
        }

        public void setPbkdf2Iterations(int pbkdf2Iterations) {
            this.pbkdf2Iterations = pbkdf2Iterations;
        }

        public int getPbkdf2SaltLength() {
            return pbkdf2SaltLength;
        }

        public void setPbkdf2SaltLength(int pbkdf2SaltLength) {
            this.pbkdf2SaltLength = pbkdf2SaltLength;
        }

        public int getPbkdf2HashWidth() {
            return pbkdf2HashWidth;
        }

        public void setPbkdf2HashWidth(int pbkdf2HashWidth) {
            this.pbkdf2HashWidth = pbkdf2HashWidth;
        }
    }

    public static class Encryption {

        @NotNull
        private EncryptionAlgorithm algorithm = EncryptionAlgorithm.AES_GCM;

        private String aesGcmKey;

        private String rsaOaepPublicKey;

        private String rsaOaepPrivateKey;

        public EncryptionAlgorithm getAlgorithm() {
            return algorithm;
        }

        public void setAlgorithm(EncryptionAlgorithm algorithm) {
            this.algorithm = algorithm;
        }

        public String getAesGcmKey() {
            return aesGcmKey;
        }

        public void setAesGcmKey(String aesGcmKey) {
            this.aesGcmKey = aesGcmKey;
        }

        public String getRsaOaepPublicKey() {
            return rsaOaepPublicKey;
        }

        public void setRsaOaepPublicKey(String rsaOaepPublicKey) {
            this.rsaOaepPublicKey = rsaOaepPublicKey;
        }

        public String getRsaOaepPrivateKey() {
            return rsaOaepPrivateKey;
        }

        public void setRsaOaepPrivateKey(String rsaOaepPrivateKey) {
            this.rsaOaepPrivateKey = rsaOaepPrivateKey;
        }
    }
}
