/**
 * @file main.ts
 * @project Pipker Framework
 * @module Frontend Bootstrap
 * @description Creates the Vue application and restores the SYSTEM session before the initial router navigation.
 * @logic Installs Pinia first, fetches /auth/me when a session token exists, registers database menu routes, then mounts the router-backed application.
 * @dependencies Vue, Vue Router, Pinia, session store
 * @index_tags frontend, bootstrap, authentication, dynamic-routing
 * @author holic512
 */
import { createApp } from 'vue'
import App from './App.vue'
import { router } from './router'
import { pinia } from './stores'
import { useSessionStore } from './stores/session'
import './styles/base.css'

async function bootstrap(): Promise<void> {
  const application = createApp(App)
  application.use(pinia)
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
