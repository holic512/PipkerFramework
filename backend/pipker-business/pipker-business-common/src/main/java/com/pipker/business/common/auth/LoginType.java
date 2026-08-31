/**
 * @file LoginType.java
 * @project Pipker Framework
 * @module Pipker Business Common
 * @description Defines an extensible, validated identifier for an account domain without imposing application roles.
 * @logic Rejects blank login-domain values so authentication infrastructure can safely distinguish account types while business modules retain ownership of their semantics.
 * @dependencies Java standard library
 * @index_tags business,common,authentication,identity,login-type
 * @author holic512
 */
package com.pipker.business.common.auth;

import java.util.Objects;

public record LoginType(String value) {

    public LoginType {
        Objects.requireNonNull(value, "loginType must not be null");
        value = value.trim();
        if (value.isEmpty() || value.indexOf('\0') >= 0) {
            throw new IllegalArgumentException("loginType must not be blank");
        }
    }
}
