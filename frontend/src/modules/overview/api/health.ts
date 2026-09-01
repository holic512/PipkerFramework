/**
 * 文件：health.ts
 * 项目：Pipker Framework
 * 模块：概览模块 API
 * 说明：定义由概览模块负责的后端健康检查请求。
 * 处理逻辑：通过共享 HTTP 客户端调用纯文本 GET /api/ping 接口，并只返回契约中说明的响应体。
 * 依赖：Axios HTTP 客户端
 * 检索关键词：前端、模块、概览、API、健康检查
 * 作者：holic512
 */
import { httpClient } from '../../../core/http/client'

export async function getBackendHealth(): Promise<string> {
  const response = await httpClient.get<string>('/ping')
  return response.data
}
