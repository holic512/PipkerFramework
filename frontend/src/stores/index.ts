/**
 * 文件：index.ts
 * 项目：Pipker Framework
 * 模块：状态管理启动
 * 说明：导出应用范围的 Pinia 实例。
 * 处理逻辑：创建一个 Pinia 根实例，由 Vue 插件注册流程提供给各功能 Store。
 * 依赖：Pinia
 * 检索关键词：前端、Pinia、状态、启动
 * 作者：holic512
 */
import { createPinia } from 'pinia'

export const pinia = createPinia()
