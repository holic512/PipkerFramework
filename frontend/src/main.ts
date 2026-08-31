/**
 * @file main.ts
 * @project Pipker Framework
 * @module Frontend bootstrap
 * @description Creates the Vue application and registers platform-wide plugins before mounting.
 * @logic Loads global styles, then installs Pinia and the router before mounting; Element Plus components are registered on demand at build time.
 * @dependencies Vue, Vue Router, Pinia
 * @index_tags frontend,bootstrap,vue,router,pinia
 * @author holic512
 */
import { createApp } from 'vue'
import App from './App.vue'
import { router } from './router'
import { pinia } from './stores'
import './styles/base.css'

createApp(App).use(pinia).use(router).mount('#app')
