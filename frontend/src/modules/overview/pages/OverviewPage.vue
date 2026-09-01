<!--
  文件：OverviewPage.vue
  项目：Pipker Framework
  模块：概览模块页面
  说明：展示已确定的前端基线、模块边界和后端连接状态。
  处理逻辑：渲染架构信息，并通过概览模块 API 调用已确认的健康检查接口，不假设尚未确认的响应封装。
  依赖：Vue Composition API、Element Plus、概览健康检查 API
  检索关键词：前端、模块、概览、页面、Element Plus
  作者：holic512
-->
<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { getBackendHealth } from '../api/health'

const platformCapabilities = [
  {
    id: '01',
    label: 'ROUTER',
    title: '路由组装',
    detail: '浏览器 History、模块路由导出与 404 回退。',
  },
  {
    id: '02',
    label: 'STATE',
    title: '状态边界',
    detail: 'Pinia 仅承载跨页面共享状态，业务状态归属领域模块。',
  },
  {
    id: '03',
    label: 'TRANSPORT',
    title: '请求基础设施',
    detail: '单一 Axios 实例、运行时前缀与可扩展的拦截器。',
  },
  {
    id: '04',
    label: 'SURFACE',
    title: '界面系统',
    detail: 'Element Plus 统一基础交互，模块样式保持局部封装。',
  },
] as const

const moduleFlow = ['入口装配', '平台核心', '业务模块', '页面与复用组件'] as const

type BackendHealthState = 'checking' | 'available' | 'unavailable'

const backendHealthState = ref<BackendHealthState>('checking')
const backendMessage = ref('正在请求 GET /api/ping…')

async function checkBackendHealth(): Promise<void> {
  backendHealthState.value = 'checking'
  backendMessage.value = '正在请求 GET /api/ping…'

  try {
    backendMessage.value = await getBackendHealth()
    backendHealthState.value = 'available'
  } catch {
    backendMessage.value = '暂未连接到 http://localhost:8080；启动后端后可再次检查。'
    backendHealthState.value = 'unavailable'
  }
}

onMounted(() => {
  void checkBackendHealth()
})
</script>

<template>
  <div class="overview-page">
    <section class="intro-section">
      <p class="eyebrow">PIPKER / FOUNDATION 01</p>
      <h1>前端不是一组页面，<br />而是一条清晰的交付链路。</h1>
      <p class="intro-section__copy">
        Vue、路由、状态和请求层已经建立独立边界。第一个业务模块可以从
        <code>src/modules</code> 开始，而不需要改动平台基础设施。
      </p>
      <div class="intro-section__tags" aria-label="已启用的技术">
        <el-tag v-for="capability in platformCapabilities" :key="capability.label" effect="plain">
          {{ capability.label }}
        </el-tag>
      </div>
    </section>

    <section class="flow-strip" aria-label="前端分层流向">
      <template v-for="(step, index) in moduleFlow" :key="step">
        <div class="flow-strip__step">
          <span>0{{ index + 1 }}</span>
          <strong>{{ step }}</strong>
        </div>
        <i v-if="index < moduleFlow.length - 1" aria-hidden="true"></i>
      </template>
    </section>

    <section class="capability-grid" aria-label="平台能力">
      <article v-for="capability in platformCapabilities" :key="capability.id" class="capability-card">
        <span>{{ capability.id }}</span>
        <p>{{ capability.label }}</p>
        <h2>{{ capability.title }}</h2>
        <div></div>
        <small>{{ capability.detail }}</small>
      </article>
    </section>

    <section class="health-check" aria-label="后端连通性检查">
      <div>
        <p>BACKEND HEALTH / CONFIRMED CONTRACT</p>
        <h2>GET <code>/api/ping</code></h2>
        <small>{{ backendMessage }}</small>
      </div>
      <div class="health-check__action">
        <el-tag
          :type="backendHealthState === 'available' ? 'success' : backendHealthState === 'unavailable' ? 'warning' : 'info'"
          effect="plain"
        >
          {{ backendHealthState === 'available' ? '已连通' : backendHealthState === 'unavailable' ? '等待后端' : '检查中' }}
        </el-tag>
        <el-button plain :loading="backendHealthState === 'checking'" @click="checkBackendHealth">
          重新检查
        </el-button>
      </div>
    </section>
  </div>
</template>

<style scoped>
.overview-page {
  display: grid;
  gap: 1.5rem;
  animation: enter-page 420ms both cubic-bezier(0.16, 1, 0.3, 1);
}

.intro-section {
  max-width: 54rem;
  padding: 2rem 0 2.75rem;
}

.eyebrow {
  margin: 0 0 1.2rem;
  color: var(--accent-strong);
  font-family: var(--font-mono);
  font-size: 0.68rem;
  font-weight: 700;
  letter-spacing: 0.13em;
}

h1 {
  max-width: 46rem;
  margin: 0;
  color: var(--ink-strong);
  font-family: var(--font-display);
  font-size: clamp(2.25rem, 5vw, 4.75rem);
  font-weight: 600;
  line-height: 1.06;
  letter-spacing: -0.065em;
}

.intro-section__copy {
  max-width: 43rem;
  margin: 1.5rem 0 0;
  color: var(--ink-muted);
  font-size: 1rem;
  line-height: 1.8;
}

code {
  padding: 0.12rem 0.35rem;
  color: var(--ink-strong);
  background: var(--surface-tinted);
  border-radius: 0.18rem;
  font-family: var(--font-mono);
  font-size: 0.88em;
}

.intro-section__tags {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
  margin-top: 1.8rem;
}

.intro-section__tags :deep(.el-tag) {
  color: var(--ink-muted);
  border-color: var(--line-strong);
  border-radius: 999px;
  font-family: var(--font-mono);
  font-size: 0.66rem;
  letter-spacing: 0.08em;
}

.flow-strip {
  min-height: 5.35rem;
  display: flex;
  align-items: center;
  padding: 0 1.35rem;
  overflow-x: auto;
  background: #182825;
  border-radius: 0.55rem;
  box-shadow: inset 0 1px rgba(226, 242, 213, 0.09), 0 1.25rem 2.5rem rgba(21, 36, 33, 0.08);
}

.flow-strip__step {
  flex: 0 0 auto;
  min-width: 8.2rem;
  display: grid;
  gap: 0.28rem;
}

.flow-strip__step span {
  color: #9ac784;
  font-family: var(--font-mono);
  font-size: 0.62rem;
  font-weight: 700;
  letter-spacing: 0.1em;
}

.flow-strip__step strong {
  color: #edf5e8;
  font-family: var(--font-display);
  font-size: 1rem;
  font-weight: 600;
}

.flow-strip i {
  flex: 0 0 3.25rem;
  height: 1px;
  margin-right: 1rem;
  background: linear-gradient(90deg, rgba(188, 222, 164, 0.62), rgba(188, 222, 164, 0.08));
}

.capability-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  border-top: 1px solid var(--line-subtle);
  border-left: 1px solid var(--line-subtle);
}

.capability-card {
  min-height: 15rem;
  display: flex;
  flex-direction: column;
  padding: 1.35rem;
  background: var(--surface-base);
  border-right: 1px solid var(--line-subtle);
  border-bottom: 1px solid var(--line-subtle);
  transition: background 180ms ease, transform 180ms ease;
}

.capability-card:hover {
  background: var(--surface-tinted);
  transform: translateY(-0.22rem);
}

.capability-card > span {
  color: var(--accent-strong);
  font-family: var(--font-mono);
  font-size: 0.7rem;
  font-weight: 700;
}

.capability-card p {
  margin: 2.4rem 0 0;
  color: var(--ink-soft);
  font-family: var(--font-mono);
  font-size: 0.64rem;
  letter-spacing: 0.08em;
}

.capability-card h2 {
  margin: 0.38rem 0 0;
  color: var(--ink-strong);
  font-family: var(--font-display);
  font-size: 1.35rem;
  font-weight: 600;
  letter-spacing: -0.035em;
}

.capability-card div {
  width: 1.8rem;
  height: 1px;
  margin: 1.1rem 0;
  background: var(--accent-strong);
}

.capability-card small {
  color: var(--ink-muted);
  font-size: 0.82rem;
  line-height: 1.65;
}

.health-check {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1.25rem;
  margin-top: 0.8rem;
  padding: 1.25rem 1.35rem;
  border: 1px solid #bfd5cc;
  border-radius: 0.38rem;
  background: #f1f7f2;
}

.health-check p {
  margin: 0;
  color: #67826f;
  font-family: var(--font-mono);
  font-size: 0.62rem;
  font-weight: 700;
  letter-spacing: 0.09em;
}

.health-check h2 {
  margin: 0.45rem 0 0;
  color: #24423a;
  font-family: var(--font-display);
  font-size: 1.2rem;
  font-weight: 700;
}

.health-check small {
  display: block;
  max-width: 38rem;
  margin-top: 0.52rem;
  color: #5c7067;
  font-size: 0.78rem;
  line-height: 1.65;
}

.health-check__action {
  flex: 0 0 auto;
  display: flex;
  align-items: center;
  gap: 0.65rem;
}

.health-check__action :deep(.el-tag) {
  font-family: var(--font-mono);
  font-size: 0.66rem;
}

.health-check__action :deep(.el-button) {
  color: #355940;
  border-color: #9db8a3;
  background: transparent;
}

@keyframes enter-page {
  from {
    opacity: 0;
    transform: translateY(0.75rem);
  }

  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@media (max-width: 70rem) {
  .capability-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 38rem) {
  .intro-section {
    padding-top: 0.75rem;
  }

  h1 {
    font-size: clamp(2.15rem, 12vw, 3.2rem);
  }

  .flow-strip {
    padding: 0 1rem;
  }

  .flow-strip__step {
    min-width: 7.35rem;
  }

  .flow-strip i {
    flex-basis: 1.4rem;
    margin-right: 0.65rem;
  }

  .capability-grid {
    grid-template-columns: 1fr;
  }

  .health-check {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
