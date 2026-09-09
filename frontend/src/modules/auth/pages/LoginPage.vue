<!--
  @file LoginPage.vue
  @project Pipker Framework
  @module Frontend Authentication
  @description Provides the SYSTEM-domain sign-in surface, optional session persistence, remembered-username preference, and shared visual-theme selection.
  @logic Restores only a saved username, submits credentials with the selected token persistence, stores no password, refreshes database authorization, then redirects only after dynamic routes are registered.
  @dependencies Vue, Vue Router, Pinia session store, runtime configuration, authentication storage, LoginPulseVisual, ThemeSwitcher, Element Plus
  @index_tags login, authentication, rbac, session, remembered-username, theme, echarts
  @author holic512
-->
<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ApiBusinessError } from '../../../core/api/contracts'
import {
  clearRememberedUsername,
  readRememberedUsername,
  writeRememberedUsername,
} from '../../../core/auth/sessionStorage'
import { runtimeConfig } from '../../../core/config/runtime'
import { getDefaultAuthorizedPath } from '../../../router'
import { useSessionStore } from '../../../stores/session'
import ThemeSwitcher from '../../../components/ThemeSwitcher.vue'
import LoginPulseVisual from '../components/LoginPulseVisual.vue'

const router = useRouter()
const route = useRoute()
const sessionStore = useSessionStore()
const submitting = ref(false)
const errorMessage = ref<string | null>(null)
const rememberedUsername = readRememberedUsername()
const rememberUsername = ref(rememberedUsername !== null)
const persistSession = ref(false)
const credentials = reactive({
  username: rememberedUsername ?? '',
  password: '',
})

async function submit(): Promise<void> {
  if (!credentials.username.trim() || !credentials.password) {
    errorMessage.value = '请输入用户名和密码。'
    return
  }

  submitting.value = true
  errorMessage.value = null
  try {
    await sessionStore.login(
      credentials,
      persistSession.value ? 'persistent' : 'session',
    )
    if (rememberUsername.value) {
      writeRememberedUsername(credentials.username)
    } else {
      clearRememberedUsername()
    }

    const requestedPath = route.query.redirect
    const destination = typeof requestedPath === 'string' && requestedPath.startsWith('/')
      ? requestedPath
      : getDefaultAuthorizedPath() ?? '/'
    await router.replace(destination)
  } catch (error) {
    errorMessage.value = error instanceof ApiBusinessError
      ? error.message
      : '无法连接到服务，请确认后端已启动后重试。'
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <main class="login-page">
    <ThemeSwitcher class="login-page__theme-switcher" />

    <section class="login-page__visual" aria-hidden="true">
      <div class="login-page__brand-mark">
        <img src="/brand/pipker-logo.webp" alt="" />
        <span>{{ runtimeConfig.appName }}<small>WORKSPACE</small></span>
      </div>
      <LoginPulseVisual />
      <div class="login-page__visual-copy">
        <p>CONNECTED POSSIBILITIES</p>
        <h2>连接每一份可能</h2>
        <span>从这里，开启高效有序的工作。</span>
      </div>
      <span class="login-page__visual-footer">PIPKER FRAMEWORK · YOUR DIGITAL WORKSPACE</span>
    </section>

    <section class="login-panel" aria-labelledby="login-heading">
      <div class="login-panel__header">
        <p class="login-panel__eyebrow">WELCOME BACK</p>
        <h1 id="login-heading">欢迎回来</h1>
        <p class="login-panel__subtitle">登录 {{ runtimeConfig.appName }}，开始今天的工作</p>
      </div>

      <form class="login-form" @submit.prevent="submit">
        <label>
          <span>用户名</span>
          <el-input v-model="credentials.username" autocomplete="username" placeholder="输入用户名" size="large" />
        </label>
        <label>
          <span>密码</span>
          <el-input
            v-model="credentials.password"
            autocomplete="current-password"
            placeholder="输入密码"
            show-password
            size="large"
            type="password"
          />
        </label>

        <div class="login-form__preferences">
          <el-checkbox v-model="persistSession">下次自动登录</el-checkbox>
          <el-checkbox v-model="rememberUsername">记住账号</el-checkbox>
        </div>

        <p v-if="errorMessage" class="login-form__error" role="alert">{{ errorMessage }}</p>
        <el-button class="login-form__submit" native-type="submit" :loading="submitting" size="large" type="primary">
          登 录 <span class="login-form__arrow" aria-hidden="true">→</span>
        </el-button>
      </form>
      <p class="login-panel__footer">{{ runtimeConfig.appName }} · 让工作井然有序</p>
    </section>
  </main>
</template>

<style scoped lang="scss">
.login-page {
  position: fixed;
  inset: 0;
  height: 100dvh;
  display: grid;
  grid-template-columns: minmax(0, 1.15fr) minmax(390px, 1fr);
  overflow: hidden;
  color: var(--color-ink-strong);
  background: var(--color-surface-base);
}
.login-page, .login-page * { box-sizing: border-box; }
.login-page__theme-switcher { position: absolute; z-index: 4; top: 24px; right: 28px; }
.login-page__visual {
  position: relative;
  min-width: 0;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  background: radial-gradient(ellipse at 50% 42%, #153657 0%, #0b1b30 48%, #071322 100%);
  color: #edf7ff;
}
.login-page__visual::before {
  content: ''; position: absolute; inset: 0; opacity: .16;
  background-image: radial-gradient(#7eaac7 .8px, transparent .8px);
  background-size: 28px 28px;
  mask-image: linear-gradient(transparent, black, transparent);
}
.login-page__brand-mark {
  position: absolute; z-index: 2; top: 36px; left: 40px;
  display: flex; align-items: center; gap: 12px;
  font-size: 21px; font-weight: 650; letter-spacing: -.5px;
}
.login-page__brand-mark img { display: block; width: 38px; height: 38px; object-fit: cover; border-radius: 10px; }
.login-page__brand-mark small { display: block; margin-top: 3px; font-size: 9px; letter-spacing: 2.8px; color: #8fa9c2; font-weight: 500; }
.login-page__visual :deep(.login-pulse) { width: min(78%, 60vh, 620px); transform: translateY(-5vh); }
.login-page__visual-copy { position: absolute; bottom: 12%; left: 12%; right: 8%; }
.login-page__visual-copy p { margin: 0 0 14px; font-size: 10px; letter-spacing: 3px; color: #79c4e5; }
.login-page__visual-copy h2 { margin: 0 0 14px; font-size: clamp(24px, 2.6vw, 38px); font-weight: 500; letter-spacing: 3px; }
.login-page__visual-copy > span { color: #94acc2; font-size: 13px; letter-spacing: 1px; }
.login-page__visual-footer { position: absolute; bottom: 28px; left: 40px; font-size: 9px; letter-spacing: 1.6px; color: #68829b; }
.login-panel { width: min(100%, 460px); min-height: 0; align-self: center; justify-self: center; padding: 76px 36px 44px; }
.login-panel__header { display: block; }
.login-panel__eyebrow { margin: 0 0 16px; color: var(--color-accent-primary); font-size: 10px; font-weight: 650; letter-spacing: 2.5px; }
.login-panel__header h1 { margin: 0; font-size: 32px; font-weight: 600; line-height: 1.3; letter-spacing: 1px; }
.login-panel__subtitle { margin: 12px 0 0; color: var(--color-ink-muted); font-size: 13px; line-height: 1.7; overflow-wrap: anywhere; }
.login-form { display: grid; gap: 22px; margin-top: 36px; }
.login-form > label { display: grid; gap: 10px; color: var(--color-ink-muted); font-size: 13px; font-weight: 500; }
.login-form :deep(.el-input__wrapper) { min-height: 48px; padding: 0 15px; border-radius: 8px; background: var(--color-surface-page); box-shadow: 0 0 0 1px var(--color-line-strong) inset; }
.login-form :deep(.el-input__wrapper.is-focus) { box-shadow: 0 0 0 1px var(--color-accent-primary) inset, 0 0 0 3px var(--color-glow-primary); }
.login-form__preferences { display: flex; align-items: center; justify-content: space-between; gap: 12px; }
.login-form__preferences :deep(.el-checkbox) { display: inline-flex; height: 22px; margin: 0; font-weight: 400; }
.login-form__preferences :deep(.el-checkbox__label) { font-size: 12px; padding-left: 7px; }
.login-form__error { margin: 0; color: var(--color-accent-danger); font-size: 12px; line-height: 1.5; }
.login-form__submit { width: 100%; height: 48px; margin: 2px 0 0; border-radius: 8px; font-size: 14px; font-weight: 600; }
.login-form__arrow { position: absolute; right: 18px; font-size: 20px; font-weight: 400; }
.login-form__submit { position: relative; }
.login-panel__footer { margin: 36px 0 0; text-align: center; font-size: 11px; color: var(--color-ink-muted); letter-spacing: .6px; }
@media (max-width: 760px) {
  .login-page { grid-template-columns: 1fr; }
  .login-page__visual { display: none; }
  .login-page__theme-switcher { top: 18px; right: 18px; }
  .login-panel { padding: 70px 28px 32px; }
}
@media (max-height: 650px) {
  .login-panel { padding-top: 64px; padding-bottom: 16px; }
  .login-form { gap: 14px; margin-top: 22px; }
  .login-panel__footer { margin-top: 20px; }
  .login-page__visual-copy { bottom: 10%; }
  .login-page__visual-copy h2 { font-size: 25px; }
}
@media (max-height: 480px) {
  .login-panel { height: 100%; overflow-y: auto; }
}
</style>
