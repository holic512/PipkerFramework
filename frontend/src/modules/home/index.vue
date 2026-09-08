<!--
  @file index.vue
  @project Pipker Framework
  @module 公开首页入口
  @description 在保持根路由不变的前提下，选择渲染框架默认首页或项目专属首页，并统一提供控制台跳转结果。
  @logic 读取源码配置决定在 / 挂载哪一个页面，不执行重定向；随后把已恢复会话对应的控制台目标传递给当前页面。
  @dependencies Vue、Vue Router、Pinia 会话 Store、默认首页、定制首页、首页配置
  @index_tags 首页、入口、定制、功能开关、公开路由
  @author holic512
-->
<script setup lang="ts">
import { computed, defineAsyncComponent } from 'vue'
import type { RouteLocationRaw } from 'vue-router'
import { getDefaultAuthorizedPath } from '../../router'
import { useSessionStore } from '../../stores/session'
import { isCustomHomeEnabled } from './api/config'

const DefaultHome = defineAsyncComponent(() => import('./pages/defaults/index.vue'))
const CustomHome = defineAsyncComponent(() => import('./pages/custom/index.vue'))
const activeHome = isCustomHomeEnabled ? CustomHome : DefaultHome
const sessionStore = useSessionStore()
const consoleDestination = computed<RouteLocationRaw>(() => (
  sessionStore.isAuthenticated
    ? getDefaultAuthorizedPath() ?? { name: 'app-layout' }
    : { name: 'login' }
))
const consoleActionCopy = computed(() => (
  sessionStore.isAuthenticated ? '进入我的控制台' : '进入控制台'
))
</script>

<template>
  <component
    :is="activeHome"
    :console-action-copy="consoleActionCopy"
    :console-destination="consoleDestination"
  />
</template>
