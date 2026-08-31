/**
 * @file LoginIdentity.java
 * @project Pipker Framework
 * @module Pipker Business Common
 * @description Represents the minimal public identity required to create and resolve an authenticated application session.
 * @logic Keeps the login domain and domain-local user identifier together while avoiding a dependency on account persistence, roles, or Sa-Token APIs.
 * @dependencies LoginType, Java standard library
 * @index_tags business,common,authentication,identity,login
 * @author holic512
 */
package com.pipker.business.common.auth;

import java.util.Objects;

public record LoginIdentity(LoginType loginType, String userId) {

    public LoginIdentity {
        Objects.requireNonNull(loginType, "loginType must not be null");
        Objects.requireNonNull(userId, "userId must not be null");
        userId = userId.trim();
        if (userId.isEmpty() || userId.indexOf('\0') >= 0) {
            throw new IllegalArgumentException("userId must not be blank");
        }
    }
}
