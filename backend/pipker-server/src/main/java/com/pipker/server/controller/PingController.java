/**
 * @file PingController.java
 * @project Pipker Framework
 * @module Pipker Server
 * @description Provides the anonymous HTTP liveness endpoint using the shared API response envelope.
 * @logic Returns a stable health text in code/data/message form without depending on database or session infrastructure.
 * @dependencies Spring Web MVC, ApiResponse
 * @index_tags server, controller, ping, api-response
 * @author holic512
 */
package com.pipker.server.controller;

import com.pipker.business.common.api.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 提供 Server 最小运行状态检查接口。
 */
@RestController
@RequestMapping("/api")
public class PingController {

    /**
     * 返回 Server 正常运行的固定文本。
     *
     * @return Server 运行状态文本
     */
    @GetMapping("/ping")
    public ApiResponse<String> ping() {
        return ApiResponse.success("Pipker Server is running.");
    }
}
