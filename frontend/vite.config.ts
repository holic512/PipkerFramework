/**
 * 文件：vite.config.ts
 * 项目：Pipker Framework
 * 模块：前端构建配置
 * 说明：为开发和生产构建配置 Vue 编译及 Element Plus 组件级导入。
 * 处理逻辑：使用 Element Plus 解析器仅引入实际渲染的组件，并在开发期间代理已确认的本地 /api 后端前缀。
 * 依赖：Vite、@vitejs/plugin-vue、unplugin-vue-components、Element Plus
 * 检索关键词：前端、Vite、构建、Element Plus、按需导入
 * 作者：holic512
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
      },
    },
  }
})
