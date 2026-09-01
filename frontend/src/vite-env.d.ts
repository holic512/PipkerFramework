/**
 * 文件：vite-env.d.ts
 * 项目：Pipker Framework
 * 模块：运行时环境类型
 * 说明：声明前端传输边界使用的公开 Vite 变量。
 * 处理逻辑：将浏览器暴露的配置限制为 .env.example 中说明的 VITE_* 值。
 * 依赖：Vite 客户端类型
 * 检索关键词：前端、Vite、环境、类型
 * 作者：holic512
 */
/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_API_BASE_URL?: string
  readonly VITE_HTTP_TIMEOUT_MS?: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}
