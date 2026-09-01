/**
 * 文件：app.ts
 * 项目：Pipker Framework
 * 模块：共享应用状态
 * 说明：管理布局和独立功能页面有意共享的界面状态。
 * 处理逻辑：将外壳展示状态置于独立业务模块之外，并暴露范围明确的变更操作。
 * 依赖：Pinia、Vue Composition API
 * 检索关键词：前端、Pinia、状态、布局
 * 作者：holic512
 */
import { ref } from 'vue'
import { defineStore } from 'pinia'

export const useAppStore = defineStore('app', () => {
  const sidebarCollapsed = ref(false)

  function toggleSidebar(): void {
    sidebarCollapsed.value = !sidebarCollapsed.value
  }

  return {
    sidebarCollapsed,
    toggleSidebar,
  }
})
