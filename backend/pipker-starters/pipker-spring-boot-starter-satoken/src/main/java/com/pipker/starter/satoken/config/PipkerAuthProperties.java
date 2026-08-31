/**
 * @file PipkerAuthProperties.java
 * @project Pipker Framework
 * @module Pipker Spring Boot Starter Sa-Token
 * @description Binds Pipker's authentication routing and session-store settings from external configuration.
 * @logic Selects a single session backend and defines the protected path set plus method-aware anonymous API routes.
 * @dependencies Spring Boot Configuration Properties, Jakarta Validation
 * @index_tags starter,sa-token,configuration,authentication,routing
 * @author holic512
 */
package com.pipker.starter.satoken.config;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Validated
@ConfigurationProperties("pipker.security.auth")
public class PipkerAuthProperties {

    @NotNull
    private SessionStore sessionStore = SessionStore.MEMORY;

    @NotEmpty
    private List<String> protectedPaths = new ArrayList<>(List.of("/api/**"));

    private List<String> publicRoutes = new ArrayList<>(List.of("GET /api/ping"));

    public SessionStore getSessionStore() {
        return sessionStore;
    }

    public void setSessionStore(SessionStore sessionStore) {
        this.sessionStore = sessionStore;
    }

    public List<String> getProtectedPaths() {
        return protectedPaths;
    }

    public void setProtectedPaths(List<String> protectedPaths) {
        this.protectedPaths = protectedPaths;
    }

    public List<String> getPublicRoutes() {
        return publicRoutes;
    }

    public void setPublicRoutes(List<String> publicRoutes) {
        this.publicRoutes = publicRoutes;
    }

    public boolean isPublicRoute(String method, String path) {
        String requestRoute = method.toUpperCase(Locale.ROOT) + " " + path;
        return publicRoutes.stream().anyMatch(requestRoute::equals);
    }
}
