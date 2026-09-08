<!--
  @file HomePage.vue
  @project Pipker Framework
  @module Public Home
  @description Renders the public system-introduction Hero page at the application root.
  @logic Presents verified platform capabilities and sends visitors to login or their first authorized console page according to the restored session.
  @dependencies Vue Composition API, Vue Router, Pinia session store, frontend router
  @index_tags homepage, hero, public-route, system-introduction
  @author holic512
-->
<script setup lang="ts">
import { computed } from 'vue'
import { getDefaultAuthorizedPath } from '../../../router'
import { useSessionStore } from '../../../stores/session'

const sessionStore = useSessionStore()

const technologyTags = [
  'Spring Boot',
  'Vue 3',
  '数据库驱动 RBAC',
  '动态菜单路由',
] as const

const platformCapabilities = [
  {
    code: '01',
    title: '快速响应',
    detail: '页面按已授权菜单动态装载，让入口保持轻量、清晰。',
  },
  {
    code: '02',
    title: '授权即路由',
    detail: '服务端返回角色、权限与菜单，前端只呈现允许访问的页面。',
  },
  {
    code: '03',
    title: '统一边界',
    detail: '认证、请求响应和数据库演进各有明确职责，便于持续交付。',
  },
] as const

const consoleDestination = computed(() => {
  if (!sessionStore.isAuthenticated) {
    return { name: 'login' }
  }

  return getDefaultAuthorizedPath() ?? { name: 'app-layout' }
})

const consoleActionCopy = computed(() => (
  sessionStore.isAuthenticated ? '进入我的控制台' : '进入控制台'
))
</script>

<template>
  <main class="home-page">
    <div class="home-page__halo home-page__halo--upper" aria-hidden="true"></div>
    <div class="home-page__halo home-page__halo--lower" aria-hidden="true"></div>
    <div class="home-page__grid" aria-hidden="true"></div>

    <header class="home-page__header">
      <RouterLink class="home-brand" to="/" aria-label="Pipker Framework 首页">
        <span class="home-brand__mark">P</span>
        <span class="home-brand__copy">
          <strong>PIPKER</strong>
          <small>FRAMEWORK</small>
        </span>
      </RouterLink>

      <p class="home-page__status">
        <span></span>
        PUBLIC ENTRY / READY
      </p>
    </header>

    <section class="home-hero" aria-labelledby="home-heading">
      <p class="home-hero__eyebrow">PIPKER FRAMEWORK / START HERE</p>
      <h1 id="home-heading">
        让系统更快响应，<br />
        <em>让业务更早落地。</em>
      </h1>
      <p class="home-hero__intro">
        面向后台系统的工程起点。Pipker 将 SYSTEM 登录、数据库驱动 RBAC、动态菜单路由与统一 API
        契约组织为一条清晰的交付链路。
      </p>

      <div class="home-hero__tags" aria-label="技术与能力标签">
        <span v-for="tag in technologyTags" :key="tag">{{ tag }}</span>
      </div>

      <RouterLink class="home-hero__action" :to="consoleDestination">
        <span>{{ consoleActionCopy }}</span>
        <i aria-hidden="true">↗</i>
      </RouterLink>
    </section>

    <section class="system-preview" aria-label="Pipker 系统能力结构预览">
      <div class="system-preview__topbar">
        <span class="system-preview__signal"></span>
        <p>SYSTEM / DELIVERY FLOW</p>
        <span>ONLINE</span>
      </div>

      <div class="system-preview__body">
        <div class="system-preview__copy">
          <p>从身份识别到业务页面，</p>
          <h2>每一步都有清晰的边界。</h2>
          <small>Built for controlled systems.</small>
        </div>

        <ol class="system-flow" aria-label="系统处理流程">
          <li>
            <span>01</span>
            <strong>AUTH</strong>
            <small>识别会话</small>
          </li>
          <li>
            <span>02</span>
            <strong>RBAC</strong>
            <small>裁决授权</small>
          </li>
          <li>
            <span>03</span>
            <strong>ROUTES</strong>
            <small>装载页面</small>
          </li>
          <li>
            <span>04</span>
            <strong>API</strong>
            <small>交付数据</small>
          </li>
        </ol>

        <div class="system-preview__monitor" aria-hidden="true">
          <div class="system-preview__monitor-line system-preview__monitor-line--top"></div>
          <div class="system-preview__monitor-line system-preview__monitor-line--middle"></div>
          <div class="system-preview__monitor-line system-preview__monitor-line--bottom"></div>
          <span>ROUTE READY</span>
        </div>
      </div>
    </section>

    <section class="capability-list" aria-label="平台优势">
      <article v-for="capability in platformCapabilities" :key="capability.code" class="capability-list__item">
        <span>{{ capability.code }}</span>
        <div>
          <h2>{{ capability.title }}</h2>
          <p>{{ capability.detail }}</p>
        </div>
      </article>
    </section>

    <aside class="home-maintenance-note" aria-label="首页维护位置">
      <span>EDIT THIS PAGE</span>
      <div>
        <p>本页内容与样式</p>
        <code>src/modules/home/pages/HomePage.vue</code>
      </div>
      <div>
        <p>首页路由配置</p>
        <code>src/router/index.ts</code>
      </div>
    </aside>
  </main>
</template>

<style scoped>
.home-page {
  --home-ink: #172727;
  --home-muted: #667472;
  --home-line: #dbe4df;
  --home-green: #126c57;
  --home-mint: #c8f2d3;
  --home-blue: #1676eb;
  position: relative;
  min-height: 100svh;
  overflow: hidden;
  padding: clamp(1.1rem, 2.8vw, 2.65rem) clamp(1.1rem, 5vw, 5.25rem) 2.4rem;
  color: var(--home-ink);
  background: #fcfdfb;
  isolation: isolate;
}

.home-page__halo,
.home-page__grid {
  position: absolute;
  pointer-events: none;
}

.home-page__halo {
  z-index: -2;
  border-radius: 50%;
  filter: blur(1px);
}

.home-page__halo--upper {
  width: min(52rem, 74vw);
  aspect-ratio: 1;
  top: -38rem;
  left: 50%;
  background: radial-gradient(circle, rgba(190, 239, 203, 0.82) 0%, rgba(220, 246, 227, 0.42) 42%, transparent 71%);
  transform: translateX(-48%);
}

.home-page__halo--lower {
  width: 34rem;
  height: 26rem;
  right: -17rem;
  bottom: 6rem;
  background: radial-gradient(ellipse, rgba(177, 219, 255, 0.29), transparent 68%);
}

.home-page__grid {
  z-index: -1;
  inset: 0;
  opacity: 0.55;
  background-image:
    linear-gradient(rgba(79, 112, 103, 0.08) 1px, transparent 1px),
    linear-gradient(90deg, rgba(79, 112, 103, 0.08) 1px, transparent 1px);
  background-size: 4.6rem 4.6rem;
  mask-image: linear-gradient(to bottom, black 0%, transparent 82%);
}

.home-page__header,
.home-hero,
.system-preview,
.capability-list,
.home-maintenance-note {
  width: min(100%, 76rem);
  margin-right: auto;
  margin-left: auto;
}

.home-page__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  animation: reveal 520ms 40ms both cubic-bezier(0.16, 1, 0.3, 1);
}

.home-brand {
  display: inline-flex;
  align-items: center;
  gap: 0.62rem;
  color: var(--home-ink);
  text-decoration: none;
}

.home-brand:focus-visible,
.home-hero__action:focus-visible {
  outline: 3px solid rgba(22, 118, 235, 0.35);
  outline-offset: 0.3rem;
}

.home-brand__mark {
  width: 2.05rem;
  height: 2.05rem;
  display: grid;
  place-items: center;
  color: #f9fdf9;
  background: var(--home-ink);
  border-radius: 0.58rem 0.2rem 0.58rem 0.2rem;
  font-family: var(--font-display);
  font-size: 1.05rem;
  font-weight: 900;
}

.home-brand__copy {
  display: grid;
  gap: 0.05rem;
  line-height: 1;
}

.home-brand__copy strong {
  font-family: var(--font-display);
  font-size: 0.8rem;
  letter-spacing: 0.09em;
}

.home-brand__copy small,
.home-page__status,
.home-hero__eyebrow,
.system-preview__topbar,
.system-preview__copy small,
.system-flow span,
.system-flow strong,
.home-maintenance-note > span,
.home-maintenance-note p {
  font-family: var(--font-mono);
  font-weight: 700;
  letter-spacing: 0.1em;
}

.home-brand__copy small {
  color: var(--home-muted);
  font-size: 0.52rem;
}

.home-page__status {
  display: inline-flex;
  align-items: center;
  gap: 0.42rem;
  margin: 0;
  color: var(--home-green);
  font-size: 0.61rem;
}

.home-page__status span,
.system-preview__signal {
  width: 0.42rem;
  height: 0.42rem;
  display: inline-block;
  background: #2bd27d;
  border-radius: 50%;
  box-shadow: 0 0 0 0.25rem rgba(43, 210, 125, 0.13);
}

.home-hero {
  max-width: 59rem;
  padding: clamp(5.75rem, 11vw, 9.1rem) 0 clamp(3.3rem, 6vw, 5.4rem);
  text-align: center;
}

.home-hero__eyebrow {
  margin: 0;
  color: var(--home-blue);
  font-size: 0.66rem;
  animation: reveal 560ms 120ms both cubic-bezier(0.16, 1, 0.3, 1);
}

.home-hero h1 {
  margin: 1.25rem 0 0;
  color: #142321;
  font-family: var(--font-display);
  font-size: clamp(3rem, 7.1vw, 6.8rem);
  font-weight: 700;
  line-height: 1.06;
  letter-spacing: -0.085em;
  animation: reveal 620ms 170ms both cubic-bezier(0.16, 1, 0.3, 1);
}

.home-hero h1 em {
  color: var(--home-blue);
  font-style: normal;
}

.home-hero__intro {
  max-width: 45rem;
  margin: 1.45rem auto 0;
  color: var(--home-muted);
  font-size: clamp(0.92rem, 1.5vw, 1.05rem);
  line-height: 1.9;
  animation: reveal 580ms 250ms both cubic-bezier(0.16, 1, 0.3, 1);
}

.home-hero__tags {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 0.48rem;
  margin-top: 1.65rem;
  animation: reveal 580ms 310ms both cubic-bezier(0.16, 1, 0.3, 1);
}

.home-hero__tags span {
  padding: 0.42rem 0.72rem;
  color: #37655a;
  background: rgba(245, 250, 246, 0.82);
  border: 1px solid #cbded4;
  border-radius: 999px;
  font-family: var(--font-mono);
  font-size: 0.64rem;
  font-weight: 600;
  letter-spacing: 0.025em;
}

.home-hero__action {
  min-height: 3.15rem;
  display: inline-flex;
  align-items: center;
  gap: 1.2rem;
  margin-top: 1.95rem;
  padding: 0.45rem 0.55rem 0.45rem 1.2rem;
  color: #f8fcf9;
  background: var(--home-blue);
  border: 1px solid var(--home-blue);
  border-radius: 0.78rem;
  box-shadow: 0 0.8rem 1.8rem rgba(22, 118, 235, 0.2);
  font-size: 0.84rem;
  font-weight: 750;
  text-decoration: none;
  transition: transform 180ms ease, background 180ms ease, box-shadow 180ms ease;
  animation: reveal 580ms 360ms both cubic-bezier(0.16, 1, 0.3, 1);
}

.home-hero__action i {
  width: 2.1rem;
  height: 2.1rem;
  display: grid;
  place-items: center;
  color: var(--home-blue);
  background: #f6fcff;
  border-radius: 0.53rem;
  font-size: 1.1rem;
  font-style: normal;
  transition: transform 180ms ease;
}

.home-hero__action:hover {
  background: #0868d8;
  box-shadow: 0 1.1rem 2rem rgba(22, 118, 235, 0.29);
  transform: translateY(-0.18rem);
}

.home-hero__action:hover i {
  transform: rotate(12deg);
}

.system-preview {
  overflow: hidden;
  background: #152a27;
  border: 1px solid rgba(129, 187, 164, 0.38);
  border-radius: 1.05rem;
  box-shadow: 0 2.2rem 5rem rgba(23, 64, 57, 0.14);
  animation: rise-preview 760ms 390ms both cubic-bezier(0.16, 1, 0.3, 1);
}

.system-preview__topbar {
  min-height: 2.75rem;
  display: flex;
  align-items: center;
  gap: 0.55rem;
  padding: 0 1rem;
  color: rgba(228, 250, 237, 0.68);
  background: #10231f;
  border-bottom: 1px solid rgba(154, 224, 195, 0.13);
  font-size: 0.58rem;
}

.system-preview__topbar p {
  margin: 0;
}

.system-preview__topbar > span:last-child {
  margin-left: auto;
  color: #8fe1af;
}

.system-preview__body {
  min-height: 17rem;
  position: relative;
  display: grid;
  grid-template-columns: minmax(14rem, 0.88fr) minmax(28rem, 1.45fr);
  gap: 1.5rem;
  align-items: center;
  padding: clamp(1.45rem, 3vw, 2.8rem);
  background:
    linear-gradient(rgba(164, 231, 199, 0.055) 1px, transparent 1px),
    linear-gradient(90deg, rgba(164, 231, 199, 0.055) 1px, transparent 1px),
    radial-gradient(circle at 75% 50%, rgba(39, 158, 123, 0.18), transparent 25rem),
    #152a27;
  background-size: 2.7rem 2.7rem, 2.7rem 2.7rem, auto, auto;
}

.system-preview__copy {
  position: relative;
  z-index: 1;
}

.system-preview__copy p {
  margin: 0;
  color: #a6dcb5;
  font-size: 0.78rem;
}

.system-preview__copy h2 {
  max-width: 16rem;
  margin: 0.55rem 0 0;
  color: #f0faed;
  font-family: var(--font-display);
  font-size: clamp(1.6rem, 2.65vw, 2.45rem);
  font-weight: 650;
  line-height: 1.25;
  letter-spacing: -0.055em;
}

.system-preview__copy small {
  display: inline-block;
  margin-top: 1.25rem;
  color: rgba(199, 237, 212, 0.58);
  font-size: 0.58rem;
}

.system-flow {
  position: relative;
  z-index: 1;
  display: grid;
  grid-template-columns: repeat(4, minmax(6.25rem, 1fr));
  gap: 0.7rem;
  margin: 0;
  padding: 0;
  list-style: none;
}

.system-flow::before {
  width: calc(100% - 8rem);
  height: 1px;
  position: absolute;
  top: 2.35rem;
  right: 4rem;
  z-index: -1;
  background: linear-gradient(90deg, rgba(130, 232, 186, 0.12), rgba(130, 232, 186, 0.72), rgba(130, 232, 186, 0.12));
  content: '';
}

.system-flow li {
  min-height: 7.2rem;
  padding: 1rem 0.88rem;
  color: #e8faea;
  background: rgba(23, 55, 48, 0.8);
  border: 1px solid rgba(146, 215, 181, 0.23);
  border-radius: 0.72rem;
  box-shadow: 0 0.8rem 1.8rem rgba(3, 20, 16, 0.15);
  transition: border-color 180ms ease, transform 180ms ease, background 180ms ease;
}

.system-flow li:hover {
  background: rgba(37, 77, 67, 0.94);
  border-color: rgba(143, 235, 184, 0.65);
  transform: translateY(-0.3rem);
}

.system-flow span {
  color: #8ce0ae;
  font-size: 0.58rem;
}

.system-flow strong {
  display: block;
  margin-top: 1.5rem;
  color: #f2fff2;
  font-size: 0.75rem;
}

.system-flow small {
  display: block;
  margin-top: 0.32rem;
  color: rgba(220, 249, 226, 0.62);
  font-size: 0.7rem;
}

.system-preview__monitor {
  width: 11rem;
  height: 11rem;
  position: absolute;
  right: -3.5rem;
  bottom: -4.5rem;
  display: grid;
  place-items: center;
  color: rgba(213, 255, 224, 0.68);
  border: 1px solid rgba(162, 242, 194, 0.22);
  border-radius: 50%;
  font-family: var(--font-mono);
  font-size: 0.55rem;
  letter-spacing: 0.08em;
  transform: rotate(-18deg);
}

.system-preview__monitor::before,
.system-preview__monitor::after,
.system-preview__monitor-line {
  position: absolute;
  background: rgba(159, 239, 190, 0.26);
  content: '';
}

.system-preview__monitor::before {
  width: 1px;
  height: 100%;
}

.system-preview__monitor::after {
  width: 100%;
  height: 1px;
}

.system-preview__monitor-line--top,
.system-preview__monitor-line--bottom {
  width: 82%;
  height: 1px;
}

.system-preview__monitor-line--top {
  transform: rotate(45deg);
}

.system-preview__monitor-line--middle {
  width: 1px;
  height: 82%;
  transform: rotate(45deg);
}

.system-preview__monitor-line--bottom {
  transform: rotate(-45deg);
}

.system-preview__monitor span {
  position: relative;
  z-index: 1;
}

.capability-list {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  margin-top: 1.35rem;
  background: rgba(255, 255, 255, 0.68);
  border: 1px solid var(--home-line);
  border-radius: 0.88rem;
  animation: reveal 640ms 560ms both cubic-bezier(0.16, 1, 0.3, 1);
}

.capability-list__item {
  min-width: 0;
  display: grid;
  grid-template-columns: auto 1fr;
  gap: 0.82rem;
  padding: 1.25rem 1.35rem;
}

.capability-list__item + .capability-list__item {
  border-left: 1px solid var(--home-line);
}

.capability-list__item > span {
  color: var(--home-blue);
  font-family: var(--font-mono);
  font-size: 0.62rem;
  font-weight: 700;
}

.capability-list h2,
.capability-list p {
  margin: 0;
}

.capability-list h2 {
  color: #26433d;
  font-family: var(--font-display);
  font-size: 1rem;
  font-weight: 700;
  letter-spacing: -0.04em;
}

.capability-list p {
  margin-top: 0.32rem;
  color: var(--home-muted);
  font-size: 0.72rem;
  line-height: 1.65;
}

.home-maintenance-note {
  display: grid;
  grid-template-columns: minmax(9rem, 0.65fr) repeat(2, 1fr);
  gap: 1rem;
  align-items: center;
  margin-top: 1.35rem;
  padding: 1rem 1.2rem;
  color: #557068;
  background: rgba(240, 247, 242, 0.76);
  border: 1px dashed #bcd0c4;
  border-radius: 0.7rem;
  animation: reveal 620ms 630ms both cubic-bezier(0.16, 1, 0.3, 1);
}

.home-maintenance-note > span {
  color: var(--home-green);
  font-size: 0.6rem;
}

.home-maintenance-note p {
  margin: 0 0 0.25rem;
  color: #7a8e86;
  font-size: 0.54rem;
}

.home-maintenance-note code {
  color: #24463c;
  font-family: var(--font-mono);
  font-size: clamp(0.58rem, 1.2vw, 0.69rem);
  overflow-wrap: anywhere;
}

@keyframes reveal {
  from {
    opacity: 0;
    transform: translateY(0.75rem);
  }

  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes rise-preview {
  from {
    opacity: 0;
    transform: translateY(1.5rem) scale(0.985);
  }

  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

@media (max-width: 56rem) {
  .system-preview__body {
    grid-template-columns: 1fr;
  }

  .system-preview__copy h2 {
    max-width: 25rem;
  }

  .system-flow {
    max-width: 40rem;
  }

  .capability-list {
    grid-template-columns: 1fr;
  }

  .capability-list__item + .capability-list__item {
    border-top: 1px solid var(--home-line);
    border-left: 0;
  }
}

@media (max-width: 40rem) {
  .home-page {
    padding-right: 1rem;
    padding-left: 1rem;
  }

  .home-page__status {
    font-size: 0.53rem;
  }

  .home-hero {
    padding-top: 4.9rem;
  }

  .home-hero h1 {
    font-size: clamp(2.65rem, 13vw, 4rem);
  }

  .home-hero__intro {
    font-size: 0.88rem;
  }

  .system-preview__body {
    min-height: 0;
    padding: 1.35rem;
  }

  .system-flow {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .system-flow::before {
    display: none;
  }

  .system-flow li {
    min-height: 6.45rem;
  }

  .home-maintenance-note {
    grid-template-columns: 1fr;
    gap: 0.72rem;
  }
}

@media (prefers-reduced-motion: reduce) {
  *,
  *::before,
  *::after {
    scroll-behavior: auto !important;
    animation-duration: 0.01ms !important;
    animation-iteration-count: 1 !important;
    transition-duration: 0.01ms !important;
  }
}
</style>
