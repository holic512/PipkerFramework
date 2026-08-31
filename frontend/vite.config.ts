/**
 * @file vite.config.ts
 * @project Pipker Framework
 * @module Frontend build configuration
 * @description Configures Vue compilation and Element Plus component-level imports for development and production builds.
 * @logic Uses the Element Plus resolver to include only rendered components and proxies the confirmed local /api backend prefix during development.
 * @dependencies Vite, @vitejs/plugin-vue, unplugin-vue-components, Element Plus
 * @index_tags frontend,vite,build,element-plus,on-demand-import
 * @author holic512
 */
import vue from '@vitejs/plugin-vue'
import { defineConfig, loadEnv } from 'vite'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'

// https://vite.dev/config/
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
