/**
 * @file AuthToken.java
 * @project Pipker Framework
 * @module Pipker Spring Boot Starter Sa-Token
 * @description Carries the raw Sa-Token value created for an authenticated Pipker identity.
 * @logic Keeps token transport formatting outside business services; HTTP adapters prepend the configured Bearer scheme when writing a response.
 * @dependencies LoginIdentity
 * @index_tags starter,sa-token,authentication,token
 * @author holic512
 */
package com.pipker.starter.satoken.service;

import com.pipker.business.common.auth.LoginIdentity;

public record AuthToken(String value, LoginIdentity identity) {
}
