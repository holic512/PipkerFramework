/**
 * @file AuthSessionService.java
 * @project Pipker Framework
 * @module Pipker Spring Boot Starter Sa-Token
 * @description Defines Pipker's framework-neutral authentication-session operations for future business login flows.
 * @logic Accepts already-verified identities, creates and resolves Sa-Token sessions, and exposes current-request login checks without leaking Sa-Token utilities into business modules.
 * @dependencies LoginIdentity, Sa-Token
 * @index_tags starter,sa-token,authentication,session,public-api
 * @author holic512
 */
package com.pipker.starter.satoken.service;

import com.pipker.business.common.auth.LoginIdentity;

import java.util.Optional;

public interface AuthSessionService {

    AuthToken login(LoginIdentity identity);

    void logoutCurrent();

    Optional<LoginIdentity> currentIdentity();

    void checkLogin();
}
