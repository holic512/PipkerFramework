/**
 * 文件：SessionStore.java
 * 项目：Pipker Framework
 * 模块：Pipker Spring Boot Starter Sa-Token
 * 说明：枚举 Pipker 认证 Starter 支持的 Sa-Token 持久化后端。
 * 处理逻辑：在应用启动时选择且仅选择一个 DAO 实现。
 * 依赖：Java 标准库
 * 检索关键词：starter、sa-token、会话、配置
 * 作者：holic512
 */
package com.pipker.starter.satoken.config;

public enum SessionStore {
    MEMORY,
    REDIS
}
