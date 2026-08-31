/**
 * @file EncryptionAlgorithm.java
 * @project Pipker Framework
 * @module Pipker Spring Boot Starter Security
 * @description Lists the supported reversible field-encryption algorithms.
 * @logic Configuration selects one active algorithm and forces the corresponding key material to be valid before application startup succeeds.
 * @dependencies Java standard library
 * @index_tags starter,security,encryption,configuration
 * @author holic512
 */
package com.pipker.starter.security.config;

public enum EncryptionAlgorithm {
    AES_GCM,
    RSA_OAEP_SHA256
}
