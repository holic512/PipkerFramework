/**
 * 文件：main.ts
 * 项目：Pipker Framework
 * 模块：前端启动
 * 说明：创建 Vue 应用，并在挂载前注册平台级插件。
 * 处理逻辑：加载全局样式，再安装 Pinia 和路由后挂载应用；Element Plus 组件在构建时按需注册。
 * 依赖：Vue、Vue Router、Pinia
 * 检索关键词：前端、启动、Vue、路由、Pinia
 * 作者：holic512
 */
import { createApp } from 'vue'
import App from './App.vue'
import { router } from './router'
import { pinia } from './stores'
import './styles/base.css'

createApp(App).use(pinia).use(router).mount('#app')
