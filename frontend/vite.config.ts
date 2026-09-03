/**
 * @file vite.config.ts
 * @project Pipker Framework
 * @module Frontend Build Configuration
 * @description 为 Vite 构建及开发环境配置 Vue 编译、Element Plus 组件级导入与后端路径代理。
 * @logic 使用 Element Plus 解析器仅引入实际渲染的组件，并在开发期间将 /api 与公开 /files 前缀转发到已配置后端。
 * @dependencies Vite、@vitejs/plugin-vue、unplugin-vue-components、Element Plus
 * @index_tags frontend、vite、proxy、api、file-storage、vue、element-plus
 * @author holic512
 */
import vue from '@vitejs/plugin-vue'
import { defineConfig, loadEnv } from 'vite'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'

// Vite 配置参考：https://vite.dev/config/
export default defineConfig(({ mode }) => {
  const environment = loadEnv(mode, process.cwd(), 'PIPKER_')
  const backendOrigin = environment.PIPKER_DEV_BACKEND_ORIGIN || 'http://localhost:8080'

  return {
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
