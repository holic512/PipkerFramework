/**
 * 文件：client.ts
 * 项目：Pipker Framework
 * 模块：HTTP 基础设施
 * 说明：为所有业务 API 模块提供统一配置的 Axios 客户端。
 * 处理逻辑：应用运行时传输默认值；在后端契约明确认证、错误和响应封装规则前，保留 Axios 原始响应。
 * 依赖：Axios、运行时配置
 * 检索关键词：前端、HTTP、Axios、基础设施
 * 作者：holic512
 */
import axios from 'axios'
import { runtimeConfig } from '../config/runtime'

export const httpClient = axios.create({
  baseURL: runtimeConfig.apiBaseUrl,
  timeout: runtimeConfig.httpTimeoutMs,
})
