<!--
  @file LoginPage.vue
  @project Pipker Framework
  @module Frontend Authentication
  @description Provides the SYSTEM-domain sign-in surface for all system_user accounts.
  @logic Submits credentials through the session store, refreshes database authorization, then redirects only after dynamic routes are registered.
  @dependencies Vue, Vue Router, Pinia session store, Element Plus
  @index_tags login, authentication, rbac, session
  @author holic512
-->
<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ApiBusinessError } from '../../../core/api/contracts'
import { getDefaultAuthorizedPath } from '../../../router'
import { useSessionStore } from '../../../stores/session'

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
    <section class="login-page__brief" aria-labelledby="login-heading">
      <p class="login-page__eyebrow">PIPKER / SYSTEM ACCESS</p>
      <h1 id="login-heading">让授权来自<br />正在运行的系统。</h1>
      <p class="login-page__copy">
        登录后，服务端会实时返回当前账户的角色、权限与可见菜单。前端只装载这些已授权的页面。
      </p>
      <dl class="login-page__principles">
        <div>
          <dt>01</dt>
          <dd>统一 SYSTEM 登录域</dd>
        </div>
        <div>
          <dt>02</dt>
          <dd>数据库实时 RBAC</dd>
        </div>
        <div>
          <dt>03</dt>
          <dd>会话级 Bearer 令牌</dd>
        </div>
      </dl>
    </section>

    <section class="login-panel" aria-label="系统登录表单">
      <div class="login-panel__header">
        <span class="login-panel__mark">P</span>
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

<style scoped>
.login-page {
  min-height: 100svh;
  display: grid;
  grid-template-columns: minmax(0, 1.18fr) minmax(22rem, 0.82fr);
  gap: clamp(2.5rem, 8vw, 10rem);
  align-items: center;
  padding: clamp(1.5rem, 6vw, 6rem);
  overflow: hidden;
  background:
    radial-gradient(circle at 16% 18%, rgba(180, 215, 156, 0.36), transparent 26rem),
    linear-gradient(115deg, #15231f 0%, #10201b 58%, #e9f0e5 58%, #f8faf6 100%);
}

.login-page__brief {
  max-width: 44rem;
  color: #eff7eb;
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
  color: #b6d69c;
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
  color: rgba(239, 247, 235, 0.72);
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
  border: 1px solid rgba(215, 239, 196, 0.18);
  background: rgba(6, 22, 17, 0.15);
}

.login-page__principles dt {
  color: #b6d69c;
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
  color: var(--ink-strong);
  background: rgba(253, 255, 251, 0.82);
  border: 1px solid rgba(255, 255, 255, 0.75);
  border-radius: 0.65rem;
  box-shadow: 0 2.5rem 6rem rgba(8, 24, 18, 0.2);
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
  display: grid;
  place-items: center;
  color: #f2faee;
  background: #1e3830;
  font-family: var(--font-display);
  font-size: 1.45rem;
  font-weight: 800;
  border-radius: 0.32rem;
}

.login-panel__header p {
  color: var(--accent-strong);
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
  color: var(--ink-muted);
  font-size: 0.75rem;
  font-weight: 700;
}

.login-form :deep(.el-input__wrapper) {
  min-height: 2.8rem;
  box-shadow: 0 0 0 1px var(--line-strong) inset;
}

.login-form :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px var(--accent-strong) inset;
}

.login-form__error {
  margin: -0.25rem 0 0;
  color: #a73a34;
  font-size: 0.8rem;
  line-height: 1.55;
}

.login-form__submit {
  width: 100%;
  margin-top: 0.4rem;
  --el-button-bg-color: #1f4637;
  --el-button-border-color: #1f4637;
  --el-button-hover-bg-color: #2f614d;
  --el-button-hover-border-color: #2f614d;
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

@media (max-width: 52rem) {
  .login-page {
    grid-template-columns: 1fr;
    gap: 2.5rem;
    background: linear-gradient(155deg, #14251f 0%, #1d3930 48%, #edf4e9 48%, #f8faf6 100%);
  }

  .login-page__brief {
    max-width: 36rem;
  }

  .login-panel {
    justify-self: stretch;
  }
}
</style>
