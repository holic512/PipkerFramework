/**
 * @file PipkerApplication.java
 * @project Pipker Framework
 * @module Pipker Server
 * @description Application entry point for the Pipker HTTP server.
 * @logic Boots Spring and enables component scanning from the server package root.
 * @dependencies Spring Boot
 * @index_tags application,bootstrap,spring-boot
 * @author holic512
 */
package com.pipker.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class PipkerApplication {

    public static void main(String[] args) {
        SpringApplication.run(PipkerApplication.class, args);
    }
}
