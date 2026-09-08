/**
 * @file main.ts
 * @project Pipker Framework
 * @module Frontend Bootstrap
 * @description Creates the Vue application, restores the selected visual theme, and restores the SYSTEM session before initial router navigation.
 * @logic Installs Pinia first, applies persisted design and color tokens, fetches /auth/me when a session token exists, registers database menu routes, then mounts the router-backed application.
 * @dependencies Vue, Vue Router, Pinia, session store, theme store, Element Plus dark variables
 * @index_tags frontend, bootstrap, theme, authentication, dynamic-routing
 * @author holic512
 */
import { createApp } from 'vue'
import App from './App.vue'
import { router } from './router'
import { pinia } from './stores'
import { useSessionStore } from './stores/session'
import { useThemeStore } from './stores/theme'
import 'element-plus/theme-chalk/dark/css-vars.css'
import './styles/index.scss'

async function bootstrap(): Promise<void> {
  const application = createApp(App)
  application.use(pinia)
  const themeStore = useThemeStore(pinia)
  themeStore.initializeTheme()
  const sessionStore = useSessionStore(pinia)
  try {
    await sessionStore.restoreSession()
  } catch (error) {
    sessionStore.clearSession()
    if (import.meta.env.DEV) {
      console.warn('[pipker] Session restoration failed; the user must sign in again.', error)
    }
  }
  application.use(router)
  await router.isReady()
  application.mount('#app')
}

void bootstrap()
