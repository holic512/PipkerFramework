/**
 * 文件：AuthToken.java
 * 项目：Pipker Framework
 * 模块：Pipker Spring Boot Starter Sa-Token
 * 说明：承载为已认证 Pipker 身份创建的原始 Sa-Token 值。
 * 处理逻辑：将令牌传输格式保留在业务服务之外；HTTP 适配器写入响应时再添加配置的 Bearer 方案。
 * 依赖：LoginIdentity
 * 检索关键词：starter、sa-token、认证、令牌
 * 作者：holic512
 */
package com.pipker.starter.satoken.service;

import com.pipker.business.common.auth.LoginIdentity;

public record AuthToken(String value, LoginIdentity identity) {
}
