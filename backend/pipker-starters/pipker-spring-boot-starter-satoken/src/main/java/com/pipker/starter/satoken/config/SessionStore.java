/**
 * @file SessionStore.java
 * @project Pipker Framework
 * @module Pipker Spring Boot Starter Sa-Token
 * @description Enumerates the Sa-Token persistence backends available through Pipker's authentication starter.
 * @logic Selects exactly one DAO implementation at application startup.
 * @dependencies Java standard library
 * @index_tags starter,sa-token,session,configuration
 * @author holic512
 */
package com.pipker.starter.satoken.config;

public enum SessionStore {
    MEMORY,
    REDIS
}
