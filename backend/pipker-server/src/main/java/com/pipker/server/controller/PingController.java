/**
 * 文件：PingController.java
 * 项目：Pipker Framework
 * 模块：Pipker Server
 * 说明：提供用于确认 Server 正常运行的最小健康检查接口。
 * 处理逻辑：将 GET /api/ping 映射为稳定的纯文本响应，不引入额外依赖。
 * 依赖：Spring Web MVC
 * 检索关键词：api、ping、控制器、server
 * 作者：holic512
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
