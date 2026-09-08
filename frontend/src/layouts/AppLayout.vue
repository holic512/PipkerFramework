<!--
  @file AppLayout.vue
  @project Pipker Framework
  @module 应用布局入口
  @description 为受保护应用壳选择当前启用的布局页面，同时保持数据库动态路由的稳定父级不变。
  @logic 读取源码配置后只挂载一种布局实现，不重定向、不重建路由，也不改变会话与授权边界。
  @dependencies Vue、侧栏布局、页签布局、应用布局配置
  @index_tags 布局、入口、功能开关、动态路由、控制台
  @author holic512
-->
<script setup lang="ts">
import { defineAsyncComponent } from 'vue'
import { applicationLayoutVariant } from './api/config'

const SidebarLayout = defineAsyncComponent(() => import('./pages/sidebar/index.vue'))
const TabbedLayout = defineAsyncComponent(() => import('./pages/tabbed/index.vue'))
const activeLayout = applicationLayoutVariant === 'tabbed' ? TabbedLayout : SidebarLayout
</script>

<template>
  <component :is="activeLayout" />
</template>
