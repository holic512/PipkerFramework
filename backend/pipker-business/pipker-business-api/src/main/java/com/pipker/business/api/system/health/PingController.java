/**
 * @file PingController.java
 * @project Pipker Framework
 * @module Pipker Business API
 * @description 使用公共 API 响应契约提供匿名 HTTP 存活检测接口。
 * @logic 返回稳定的 code/data/message 健康文本，不依赖数据库或会话基础设施。
 * @dependencies Spring Web MVC、ApiResponse
 * @index_tags system、health、controller、api-response
 * @author holic512
 */
package com.pipker.business.api.system.health;

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
