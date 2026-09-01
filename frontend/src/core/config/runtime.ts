/**
 * 文件：runtime.ts
 * 项目：Pipker Framework
 * 模块：运行时配置
 * 说明：将公开的 Vite 变量归一化为应用运行时配置。
 * 处理逻辑：在启动时一次性校验可选字符串，并向基础设施模块提供安全的回退值。
 * 依赖：Vite import.meta.env
 * 检索关键词：前端、配置、运行时、HTTP
 * 作者：holic512
 */
const DEFAULT_API_BASE_URL = '/api'
const DEFAULT_HTTP_TIMEOUT_MS = 10_000

function readNonEmpty(value: string | undefined, fallback: string): string {
  const normalizedValue = value?.trim()
  return normalizedValue ? normalizedValue : fallback
}

function readPositiveInteger(value: string | undefined, fallback: number): number {
  const parsedValue = Number(value)
  return Number.isInteger(parsedValue) && parsedValue > 0 ? parsedValue : fallback
}

export const runtimeConfig = {
  apiBaseUrl: readNonEmpty(import.meta.env.VITE_API_BASE_URL, DEFAULT_API_BASE_URL),
  httpTimeoutMs: readPositiveInteger(
    import.meta.env.VITE_HTTP_TIMEOUT_MS,
    DEFAULT_HTTP_TIMEOUT_MS,
  ),
} as const
