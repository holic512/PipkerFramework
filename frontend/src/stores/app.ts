/**
 * @file app.ts
 * @project Pipker Framework
 * @module Shared application state
 * @description Manages UI state that is intentionally shared by layouts and independent feature pages.
 * @logic Keeps shell presentation state out of individual business modules and exposes narrow mutation actions.
 * @dependencies Pinia, Vue Composition API
 * @index_tags frontend,pinia,store,layout
 * @author holic512
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
