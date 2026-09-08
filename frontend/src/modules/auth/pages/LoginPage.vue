<!--
  @file LoginPage.vue
  @project Pipker Framework
  @module Frontend Authentication
  @description Provides the SYSTEM-domain sign-in surface and shared visual-theme selection for all system_user accounts.
  @logic Submits credentials through the session store, refreshes database authorization, then redirects only after dynamic routes are registered; visual choices are persisted by ThemeSwitcher.
  @dependencies Vue, Vue Router, Pinia session store, ThemeSwitcher, Element Plus
  @index_tags login, authentication, rbac, session, theme
  @author holic512
-->
<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ApiBusinessError } from '../../../core/api/contracts'
import { getDefaultAuthorizedPath } from '../../../router'
import { useSessionStore } from '../../../stores/session'
import ThemeSwitcher from '../../../components/ThemeSwitcher.vue'

const router = useRouter()
const route = useRoute()
const sessionStore = useSessionStore()
const submitting = ref(false)
const errorMessage = ref<string | null>(null)
const credentials = reactive({
  username: '',
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
    await sessionStore.login(credentials)
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
    <section class="login-page__brief" aria-labelledby="login-heading">
      <p class="login-page__eyebrow ui-eyebrow">PIPKER / SYSTEM ACCESS</p>
      <h1 id="login-heading">让授权来自<br />正在运行的系统。</h1>
      <p class="login-page__copy">
        登录后，服务端会返回当前账户的角色、权限与可见菜单。前端只装载这些已授权的页面。
      </p>
      <dl class="login-page__principles">
        <div>
          <dt>01</dt>
          <dd>统一 SYSTEM 登录域</dd>
        </div>
        <div>
          <dt>02</dt>
          <dd>数据库驱动 RBAC</dd>
        </div>
        <div>
          <dt>03</dt>
          <dd>会话级 Bearer 令牌</dd>
        </div>
      </dl>
    </section>

    <section class="login-panel ui-panel" aria-label="系统登录表单">
      <div class="login-panel__header">
        <img class="login-panel__mark" src="/brand/pipker-logo.webp" alt="" />
        <div>
          <p>CONTROL ROOM</p>
          <h2>系统登录</h2>
        </div>
      </div>

      <form class="login-form" @submit.prevent="submit">
        <label>
          <span>用户名</span>
          <el-input v-model="credentials.username" autocomplete="username" placeholder="输入系统用户名" size="large" />
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
        <p v-if="errorMessage" class="login-form__error" role="alert">{{ errorMessage }}</p>
        <el-button class="login-form__submit" native-type="submit" :loading="submitting" size="large" type="primary">
          进入控制台
        </el-button>
      </form>

      <p class="login-panel__note">认证状态仅存储在当前浏览器会话中。</p>
    </section>
  </main>
</template>

<style scoped lang="scss">
.login-page {
  position: relative;
  min-height: 100svh;
  display: grid;
  grid-template-columns: minmax(0, 1.18fr) minmax(22rem, 0.82fr);
  gap: clamp(2.5rem, 8vw, 10rem);
  align-items: center;
  padding: clamp(1.5rem, 6vw, 6rem);
  overflow: hidden;
  background:
    radial-gradient(circle at 16% 18%, var(--color-glow-secondary), transparent 26rem),
    linear-gradient(115deg, var(--color-surface-contrast) 0%, var(--color-surface-contrast-raised) 58%, var(--color-surface-muted) 58%, var(--color-surface-page) 100%);
}

.login-page__theme-switcher {
  position: absolute;
  top: clamp(1rem, 2vw, 1.8rem);
  right: clamp(1rem, 2vw, 2rem);
}

.login-page__brief {
  max-width: 44rem;
  color: var(--color-ink-on-contrast);
  animation: rise-in 540ms both cubic-bezier(0.16, 1, 0.3, 1);
}

.login-page__eyebrow,
.login-panel__header p,
.login-panel__note {
  margin: 0;
  font-family: var(--font-mono);
  font-size: 0.67rem;
  font-weight: 700;
  letter-spacing: 0.13em;
}

.login-page__eyebrow {
  color: var(--color-accent-secondary);
}

.login-page h1 {
  margin: 1.25rem 0 0;
  font-family: var(--font-display);
  font-size: clamp(3rem, 6.3vw, 6.6rem);
  font-weight: 600;
  line-height: 1.02;
  letter-spacing: -0.075em;
}

.login-page__copy {
  max-width: 35rem;
  margin: 1.8rem 0 0;
  color: var(--color-ink-on-contrast-muted);
  font-size: 1rem;
  line-height: 1.85;
}

.login-page__principles {
  display: flex;
  flex-wrap: wrap;
  gap: 0.8rem;
  margin: 2.5rem 0 0;
}

.login-page__principles div {
  min-width: 10.5rem;
  padding: 0.85rem 1rem;
  border: 1px solid var(--color-line-contrast);
  background: color-mix(in srgb, var(--color-surface-contrast-raised) 62%, transparent);
}

.login-page__principles dt {
  color: var(--color-accent-secondary);
  font-family: var(--font-mono);
  font-size: 0.66rem;
}

.login-page__principles dd {
  margin: 0.45rem 0 0;
  font-size: 0.8rem;
}

.login-panel {
  width: min(100%, 28rem);
  justify-self: end;
  padding: clamp(1.5rem, 4vw, 3rem);
  color: var(--color-ink-strong);
  background: var(--color-surface-translucent);
  border-color: var(--color-line-subtle);
  border-radius: var(--radius-panel);
  box-shadow: var(--shadow-floating);
  backdrop-filter: blur(1rem);
  animation: rise-in 560ms 90ms both cubic-bezier(0.16, 1, 0.3, 1);
}

.login-panel__header {
  display: flex;
  align-items: center;
  gap: 0.85rem;
}

.login-panel__mark {
  width: 2.65rem;
  height: 2.65rem;
  display: block;
  flex: 0 0 auto;
  object-fit: cover;
  border-radius: var(--radius-small);
}

.login-panel__header p {
  color: var(--color-accent-primary);
}

.login-panel__header h2 {
  margin: 0.22rem 0 0;
  font-family: var(--font-display);
  font-size: 1.55rem;
  letter-spacing: -0.045em;
}

.login-form {
  display: grid;
  gap: 1.1rem;
  margin-top: 2.5rem;
}

.login-form label {
  display: grid;
  gap: 0.48rem;
  color: var(--color-ink-muted);
  font-size: 0.75rem;
  font-weight: 700;
}

.login-form :deep(.el-input__wrapper) {
  min-height: 2.8rem;
  box-shadow: 0 0 0 1px var(--color-line-strong) inset;
}

.login-form :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px var(--color-accent-primary) inset;
}

.login-form__error {
  margin: -0.25rem 0 0;
  color: var(--color-accent-danger);
  font-size: 0.8rem;
  line-height: 1.55;
}

.login-form__submit {
  width: 100%;
  margin-top: 0.4rem;
}

.login-panel__note {
  margin-top: 2rem;
  color: var(--ink-soft);
  line-height: 1.6;
}

@keyframes rise-in {
  from {
    opacity: 0;
    transform: translateY(1.1rem);
  }

  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@include at-most('tablet') {
  .login-page {
    grid-template-columns: 1fr;
    gap: 2.5rem;
    background: linear-gradient(155deg, var(--color-surface-contrast) 0%, var(--color-surface-contrast-raised) 48%, var(--color-surface-muted) 48%, var(--color-surface-page) 100%);
  }

  .login-page__brief {
    max-width: 36rem;
  }

  .login-panel {
    justify-self: stretch;
  }
}
</style>
