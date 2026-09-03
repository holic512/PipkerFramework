/**
 * 文件：health.ts
 * 项目：Pipker Framework
 * 模块：概览模块 API
 * 说明：定义由概览模块负责的后端健康检查请求。
 * 处理逻辑：通过共享请求层调用统一 code/data/message 封装的 GET /api/ping 接口，并只返回已解包的健康文本。
 * 依赖：共享 HTTP 请求层
 * 检索关键词：前端、模块、概览、API、健康检查
 * 作者：holic512
 */
import { requestApi } from '../../../core/http/client'

export async function getBackendHealth(): Promise<string> {
  return requestApi<string>({
    method: 'GET',
    url: '/ping',
  })
}
