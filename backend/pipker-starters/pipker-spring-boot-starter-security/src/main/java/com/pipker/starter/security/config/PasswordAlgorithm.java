/**
 * @file PasswordAlgorithm.java
 * @project Pipker Framework
 * @module Pipker Spring Boot Starter Security
 * @description Lists the supported one-way password hashing algorithms.
 * @logic Configuration selects one algorithm for newly stored passwords while the service can verify recognized historical formats.
 * @dependencies Java standard library
 * @index_tags starter,security,password,hashing,configuration
 * @author holic512
 */
package com.pipker.starter.security.config;

public enum PasswordAlgorithm {
    BCRYPT,
    PBKDF2_SHA256
}
