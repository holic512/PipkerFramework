/**
 * @file SecurityCryptoService.java
 * @project Pipker Framework
 * @module Pipker Spring Boot Starter Security
 * @description Defines the single application-facing entry point for password hashing and selected field cryptography.
 * @logic Hides algorithm selection from business callers; configuration determines the active password and reversible-encryption implementations.
 * @dependencies Java standard library
 * @index_tags starter,security,cryptography,password,public-api
 * @author holic512
 */
package com.pipker.starter.security.service;

public interface SecurityCryptoService {

    String hashPassword(CharSequence rawPassword);

    boolean matchesPassword(CharSequence rawPassword, String storedHash);

    boolean needsPasswordUpgrade(String storedHash);

    String encrypt(String plainText);

    String decrypt(String cipherText);
}
