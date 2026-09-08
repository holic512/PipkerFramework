/**
 * @file vite.config.ts
 * @project Pipker Framework
 * @module Frontend Build Configuration
 * @description 为 Vite 构建及开发环境配置 Vue 编译、Sass 公共抽象、Element Plus 组件级导入与后端路径代理。
 * @logic 注入无输出 Sass mixin 和 @ 路径别名，按需解析 Element Plus 组件，并在开发期间将 /api 与公开 /files 前缀转发到已配置后端。
 * @dependencies Vite、Sass、@vitejs/plugin-vue、unplugin-vue-components、Element Plus
 * @index_tags frontend、vite、sass、theme、proxy、api、file-storage、vue、element-plus
 * @author holic512
 */
import { fileURLToPath, URL } from 'node:url'
import vue from '@vitejs/plugin-vue'
import { defineConfig, loadEnv } from 'vite'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'

// Vite 配置参考：https://vite.dev/config/
export default defineConfig(({ mode }) => {
  const environment = loadEnv(mode, process.cwd(), 'PIPKER_')
  const backendOrigin = environment.PIPKER_DEV_BACKEND_ORIGIN || 'http://localhost:8080'

  return {
    resolve: {
      alias: {
        '@': fileURLToPath(new URL('./src', import.meta.url)),
      },
    },
    css: {
      preprocessorOptions: {
        scss: {
          additionalData: '@use "@/styles/abstracts" as *;',
        },
      },
    },
    plugins: [
      vue(),
      Components({
        resolvers: [ElementPlusResolver()],
      }),
    ],
    server: {
      proxy: {
        '/api': {
          target: backendOrigin,
          changeOrigin: true,
        },
        '/files': {
          target: backendOrigin,
          changeOrigin: true,
        },
      },
    },
  }
})
