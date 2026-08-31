/**
 * @file PingController.java
 * @project Pipker Framework
 * @module Pipker Server
 * @description Exposes the minimal health-style endpoint for verifying the running server.
 * @logic Maps GET /api/ping to a stable plain-text response without external dependencies.
 * @dependencies Spring Web MVC
 * @index_tags api,ping,controller,server
 * @author holic512
 */
package com.pipker.server.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class PingController {

    @GetMapping("/ping")
    public String ping() {
        return "Pipker Server is running.";
    }
}
