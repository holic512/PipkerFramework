<!--
  @file index.vue
  @project Pipker Framework
  @module 系统仪表盘
  @description 基于当前登录会话的真实授权投影，展示菜单、路由、角色与接口权限的系统仪表盘。
  @logic 从会话 Store 派生统计和图表数据；ECharts 实例随容器、主题与授权数据更新，并在页面卸载时释放。
  @dependencies Vue、ECharts、Element Plus、Pinia 会话 Store、frontend API contracts
  @index_tags dashboard、overview、echarts、authorization、menu、route、rbac
  @author holic512
-->
<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { DataAnalysis, Document, Key, Menu, Refresh, UserFilled } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { init, use, type ECharts, type EChartsCoreOption } from 'echarts/core'
import { BarChart, PieChart } from 'echarts/charts'
import { GridComponent, TooltipComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import type { SystemMenuNode } from '../../../core/api/contracts'
import { useSessionStore } from '../../../stores/session'

use([BarChart, PieChart, GridComponent, TooltipComponent, CanvasRenderer])

interface DashboardMetric {
  label: string
  value: number
  description: string
  icon: typeof DataAnalysis
}

interface DashboardPalette {
  primary: string
  secondary: string
  muted: string
  soft: string
  strong: string
  surface: string
  line: string
}

interface WorkspaceDistribution {
  name: string
  value: number
}

const session = useSessionStore()
const workspaceChartElement = ref<HTMLElement | null>(null)
const resourceChartElement = ref<HTMLElement | null>(null)
const refreshing = ref(false)
let workspaceChart: ECharts | null = null
let resourceChart: ECharts | null = null
let resizeObserver: ResizeObserver | null = null
let themeObserver: MutationObserver | null = null

const menuNodeCount = computed(() => countMenuNodes(session.menus))
const directoryCount = computed(() => countMenusByType(session.menus, 'DIRECTORY'))
const metrics = computed<DashboardMetric[]>(() => [
  { label: '授权页面', value: session.routes.length, description: '当前会话可装载的页面路由', icon: Document },
  { label: '可见节点', value: menuNodeCount.value, description: '导航中可见的目录与页面', icon: Menu },
  { label: '接口权限', value: session.permissions.length, description: '当前角色可调用的接口能力', icon: Key },
  { label: '当前角色', value: session.roles.length, description: '正在生效的角色数量', icon: UserFilled },
])
const resourceDistribution = computed<WorkspaceDistribution[]>(() => [
  { name: '页面路由', value: session.routes.length },
  { name: '导航目录', value: directoryCount.value },
  { name: '接口权限', value: session.permissions.length },
])
const workspaceDistribution = computed<WorkspaceDistribution[]>(() => session.menus.map(menu => ({
  name: menu.name,
  value: countPagesInBranch(menu),
})).filter(item => item.value > 0))
const accessiblePages = computed(() => session.routes.slice(0, 8))
const userDisplayName = computed(() => session.user?.nickname?.trim() || session.user?.username || '当前账户')
const roleSummary = computed(() => session.roles.length > 0 ? session.roles.join('、') : '未分配角色')

watch(
  () => [resourceDistribution.value, workspaceDistribution.value],
  () => { void nextTick().then(renderCharts) },
  { deep: true },
)

onMounted(async () => {
  await nextTick()
  initialiseCharts()
  resizeObserver = new ResizeObserver(() => {
    workspaceChart?.resize()
    resourceChart?.resize()
  })
  if (workspaceChartElement.value) resizeObserver.observe(workspaceChartElement.value)
  if (resourceChartElement.value) resizeObserver.observe(resourceChartElement.value)

  themeObserver = new MutationObserver(() => renderCharts())
  themeObserver.observe(document.documentElement, {
    attributes: true,
    attributeFilter: ['data-design-theme', 'data-color-scheme'],
  })
})

onBeforeUnmount(() => {
  resizeObserver?.disconnect()
  themeObserver?.disconnect()
  workspaceChart?.dispose()
  resourceChart?.dispose()
  workspaceChart = null
  resourceChart = null
})

async function refreshDashboard(): Promise<void> {
  refreshing.value = true
  try {
    await session.refreshAuthorization()
    ElMessage.success('仪表盘已同步当前授权状态。')
  } catch {
    ElMessage.error('授权状态同步失败，请稍后重试。')
  } finally {
    refreshing.value = false
  }
}

function initialiseCharts(): void {
  if (workspaceChartElement.value) workspaceChart = init(workspaceChartElement.value)
  if (resourceChartElement.value) resourceChart = init(resourceChartElement.value)
  renderCharts()
}

function renderCharts(): void {
  const palette = readPalette()
  renderWorkspaceChart(palette)
  renderResourceChart(palette)
}

function renderWorkspaceChart(palette: DashboardPalette): void {
  if (!workspaceChart) return
  const items = workspaceDistribution.value
  const option: EChartsCoreOption = {
    animationDuration: 360,
    animationEasing: 'cubicOut',
    grid: { top: 10, right: 28, bottom: 8, left: 12, containLabel: true },
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      backgroundColor: palette.surface,
      borderColor: palette.line,
      textStyle: { color: palette.strong },
      formatter: (parameters: unknown) => {
        const item = (Array.isArray(parameters) ? parameters[0] : parameters) as {
          name?: string
          value?: string | number
        } | undefined
        return `${item?.name ?? '工作区'}<br/>授权页面：${item?.value ?? 0}`
      },
    },
    xAxis: {
      type: 'value',
      minInterval: 1,
      axisLabel: { color: palette.muted, fontSize: 11 },
      axisLine: { lineStyle: { color: palette.line } },
      splitLine: { lineStyle: { color: palette.line, type: 'dashed' } },
    },
    yAxis: {
      type: 'category',
      inverse: true,
      data: items.map(item => item.name),
      axisLabel: { color: palette.muted, fontSize: 11, width: 92, overflow: 'truncate' },
      axisTick: { show: false },
      axisLine: { show: false },
    },
    series: [{
      type: 'bar',
      data: items.map(item => item.value),
      barMaxWidth: 22,
      itemStyle: { color: palette.primary, borderRadius: [0, 4, 4, 0] },
      label: { show: true, position: 'right', color: palette.muted, fontSize: 11 },
      emphasis: { itemStyle: { color: palette.secondary } },
    }],
  }
  workspaceChart.setOption(option, true)
}

function renderResourceChart(palette: DashboardPalette): void {
  if (!resourceChart) return
  const option: EChartsCoreOption = {
    animationDuration: 360,
    animationEasing: 'cubicOut',
    color: [palette.primary, palette.secondary, palette.soft],
    tooltip: {
      trigger: 'item',
      backgroundColor: palette.surface,
      borderColor: palette.line,
      textStyle: { color: palette.strong },
      formatter: '{b}<br/>{c} 项（{d}%）',
    },
    series: [{
      type: 'pie',
      radius: ['53%', '76%'],
      center: ['50%', '48%'],
      minAngle: 5,
      padAngle: 2,
      itemStyle: { borderColor: palette.surface, borderWidth: 2, borderRadius: 3 },
      label: { color: palette.muted, fontSize: 11, formatter: '{b}\n{c} 项' },
      labelLine: { lineStyle: { color: palette.line } },
      data: resourceDistribution.value,
    }],
  }
  resourceChart.setOption(option, true)
}

function readPalette(): DashboardPalette {
  const styles = getComputedStyle(document.documentElement)
  return {
    primary: readThemeValue(styles, '--color-accent-primary'),
    secondary: readThemeValue(styles, '--color-accent-secondary'),
    muted: readThemeValue(styles, '--color-ink-muted'),
    soft: readThemeValue(styles, '--color-ink-soft'),
    strong: readThemeValue(styles, '--color-ink-strong'),
    surface: readThemeValue(styles, '--color-surface-raised'),
    line: readThemeValue(styles, '--color-line-subtle'),
  }
}

function readThemeValue(styles: CSSStyleDeclaration, name: string): string {
  return styles.getPropertyValue(name).trim()
}

function countMenuNodes(nodes: SystemMenuNode[]): number {
  return nodes.reduce((total, node) => total + 1 + countMenuNodes(node.children), 0)
}

function countMenusByType(nodes: SystemMenuNode[], type: SystemMenuNode['type']): number {
  return nodes.reduce((total, node) => total + (node.type === type ? 1 : 0) + countMenusByType(node.children, type), 0)
}

function countPagesInBranch(node: SystemMenuNode): number {
  return (node.type === 'MENU' ? 1 : 0) + node.children.reduce((total, child) => total + countPagesInBranch(child), 0)
}
</script>

<template>
  <section class="system-overview" aria-label="系统仪表盘">
    <header class="system-overview__signal ui-panel">
      <div class="system-overview__signal-copy">
        <span class="system-overview__signal-state"><i aria-hidden="true"></i>授权状态正常</span>
        <h1>你好，{{ userDisplayName }}</h1>
        <p>此处仅呈现当前账户已获授权的系统访问面，所有统计会随角色与菜单配置同步更新。</p>
      </div>
      <div class="system-overview__signal-meta">
        <span>生效角色</span>
        <strong>{{ roleSummary }}</strong>
        <el-button plain :icon="Refresh" :loading="refreshing" @click="refreshDashboard">同步授权</el-button>
      </div>
    </header>

    <section class="system-overview__metrics" aria-label="授权汇总">
      <article v-for="metric in metrics" :key="metric.label" class="system-overview__metric ui-panel">
        <el-icon aria-hidden="true"><component :is="metric.icon" /></el-icon>
        <div><strong>{{ metric.value }}</strong><span>{{ metric.label }}</span></div>
        <p>{{ metric.description }}</p>
      </article>
    </section>

    <section class="system-overview__charts" aria-label="授权图表">
      <article class="system-overview__chart ui-panel">
        <header><div><h2>工作区页面分布</h2><p>按顶级菜单归集可访问页面数量</p></div><DataAnalysis aria-hidden="true" /></header>
        <div ref="workspaceChartElement" class="system-overview__chart-canvas" role="img" aria-label="工作区页面分布柱状图"></div>
      </article>
      <article class="system-overview__chart ui-panel">
        <header><div><h2>授权资源构成</h2><p>页面、目录与接口权限的当前占比</p></div><Key aria-hidden="true" /></header>
        <div ref="resourceChartElement" class="system-overview__chart-canvas" role="img" aria-label="授权资源构成环形图"></div>
      </article>
    </section>

    <section class="system-overview__details" aria-label="授权明细">
      <article class="system-overview__pages ui-panel">
        <header><div><h2>可访问页面</h2><p>本次会话已注册的动态路由</p></div><span>{{ session.routes.length }} 项</span></header>
        <ul v-if="accessiblePages.length > 0">
          <li v-for="route in accessiblePages" :key="route.id"><strong>{{ route.title }}</strong><code>{{ route.path }}</code></li>
        </ul>
        <p v-else class="system-overview__empty">当前账户没有可装载的页面。</p>
      </article>
      <article class="system-overview__permissions ui-panel">
        <header><div><h2>接口权限</h2><p>当前角色集合中的有效接口能力</p></div><span>{{ session.permissions.length }} 项</span></header>
        <div v-if="session.permissions.length > 0" class="system-overview__permission-list">
          <code v-for="permission in session.permissions.slice(0, 12)" :key="permission">{{ permission }}</code>
        </div>
        <p v-else class="system-overview__empty">当前账户没有已登记的接口权限。</p>
        <p v-if="session.permissions.length > 12" class="system-overview__more">另有 {{ session.permissions.length - 12 }} 项权限未展开。</p>
      </article>
    </section>
  </section>
</template>

<style scoped lang="scss">
.system-overview { display: grid; gap: 0.75rem; width: min(100%, 78rem); min-height: 0; margin: 0 auto; padding-bottom: 0.15rem; }
.system-overview__signal { display: flex; align-items: stretch; justify-content: space-between; gap: 1.5rem; overflow: hidden; color: var(--color-ink-on-contrast); background: var(--color-surface-contrast); border-color: var(--color-line-contrast); }
.system-overview__signal-copy { min-width: 0; padding: 1.1rem 1.25rem; }
.system-overview__signal-state { display: inline-flex; align-items: center; gap: 0.4rem; color: var(--color-ink-on-contrast-muted); font-size: 0.7rem; font-weight: 650; }
.system-overview__signal-state i { width: 0.42rem; height: 0.42rem; border-radius: 50%; background: var(--color-accent-success); box-shadow: 0 0 0 0.2rem color-mix(in srgb, var(--color-accent-success) 16%, transparent); }
.system-overview__signal h1 { margin: 0.5rem 0 0; color: var(--color-ink-on-contrast); font-family: var(--font-display); font-size: clamp(1.35rem, 2.2vw, 1.85rem); line-height: 1.15; }
.system-overview__signal p { max-width: 43rem; margin: 0.5rem 0 0; color: var(--color-ink-on-contrast-muted); font-size: 0.76rem; line-height: 1.65; }
.system-overview__signal-meta { display: grid; align-content: center; gap: 0.32rem; min-width: min(16rem, 32%); padding: 1.1rem 1.25rem; border-left: 1px solid var(--color-line-contrast); }
.system-overview__signal-meta span { color: var(--color-ink-on-contrast-muted); font-size: 0.65rem; }
.system-overview__signal-meta strong { overflow-wrap: anywhere; color: var(--color-ink-on-contrast); font-family: var(--font-mono); font-size: 0.78rem; font-weight: 650; line-height: 1.45; }
.system-overview__signal-meta :deep(.el-button) { width: fit-content; margin-top: 0.45rem; color: var(--color-ink-on-contrast); background: var(--color-panel-overlay); border-color: var(--color-line-contrast); }
.system-overview__signal-meta :deep(.el-button:hover) { color: var(--color-ink-on-contrast); background: var(--color-panel-overlay-hover); }
.system-overview__metrics { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 0.75rem; }
.system-overview__metric { display: grid; grid-template-columns: auto minmax(0, 1fr); column-gap: 0.65rem; min-height: 7.2rem; padding: 0.9rem; }
.system-overview__metric > .el-icon { width: 1.9rem; height: 1.9rem; display: grid; place-items: center; color: var(--color-accent-on-primary); background: var(--color-accent-primary); border-radius: var(--radius-small); font-size: 1rem; }
.system-overview__metric div { display: grid; align-content: center; gap: 0.1rem; min-width: 0; }
.system-overview__metric strong { color: var(--color-ink-strong); font-family: var(--font-display); font-size: 1.55rem; line-height: 1; }
.system-overview__metric span { color: var(--color-ink-muted); font-size: 0.7rem; }
.system-overview__metric p { grid-column: 1 / -1; align-self: end; margin: 0.6rem 0 0; color: var(--color-ink-soft); font-size: 0.68rem; line-height: 1.45; }
.system-overview__charts, .system-overview__details { display: grid; grid-template-columns: minmax(0, 1fr) minmax(0, 1fr); gap: 0.75rem; }
.system-overview__chart, .system-overview__pages, .system-overview__permissions { min-width: 0; padding: 1rem; }
.system-overview__chart > header, .system-overview__pages > header, .system-overview__permissions > header { display: flex; align-items: flex-start; justify-content: space-between; gap: 1rem; }
.system-overview__chart h2, .system-overview__pages h2, .system-overview__permissions h2 { margin: 0; color: var(--color-ink-strong); font-family: var(--font-display); font-size: 0.9rem; line-height: 1.2; }
.system-overview__chart header p, .system-overview__pages header p, .system-overview__permissions header p { margin: 0.25rem 0 0; color: var(--color-ink-soft); font-size: 0.67rem; line-height: 1.45; }
.system-overview__chart > header > svg { flex: 0 0 auto; color: var(--color-ink-soft); font-size: 1rem; }
.system-overview__chart-canvas { width: 100%; height: 13.5rem; margin-top: 0.45rem; }
.system-overview__pages > header > span, .system-overview__permissions > header > span { flex: 0 0 auto; padding: 0.2rem 0.42rem; color: var(--color-ink-muted); background: var(--color-surface-muted); border-radius: var(--radius-small); font-family: var(--font-mono); font-size: 0.66rem; }
.system-overview__pages ul { display: grid; gap: 0; margin: 0.8rem 0 0; padding: 0; list-style: none; border-top: 1px solid var(--color-line-subtle); }
.system-overview__pages li { display: flex; align-items: baseline; justify-content: space-between; gap: 1rem; min-width: 0; padding: 0.55rem 0; border-bottom: 1px solid var(--color-line-subtle); }
.system-overview__pages li strong { overflow: hidden; color: var(--color-ink-strong); font-size: 0.72rem; font-weight: 650; text-overflow: ellipsis; white-space: nowrap; }
.system-overview__pages li code, .system-overview__permission-list code { color: var(--color-ink-soft); font-family: var(--font-mono); font-size: 0.65rem; }
.system-overview__pages li code { flex: 0 1 auto; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.system-overview__permission-list { display: flex; flex-wrap: wrap; gap: 0.35rem; margin-top: 0.85rem; }
.system-overview__permission-list code { padding: 0.25rem 0.4rem; color: var(--color-ink-muted); background: var(--color-surface-muted); border: 1px solid var(--color-line-subtle); border-radius: var(--radius-small); line-height: 1.2; }
.system-overview__empty, .system-overview__more { margin: 0.85rem 0 0; color: var(--color-ink-soft); font-size: 0.7rem; line-height: 1.5; }
.system-overview__more { padding-top: 0.65rem; border-top: 1px solid var(--color-line-subtle); }
@include at-most('tablet') { .system-overview__metrics { grid-template-columns: repeat(2, minmax(0, 1fr)); } .system-overview__signal { flex-direction: column; gap: 0; } .system-overview__signal-meta { min-width: 0; border-top: 1px solid var(--color-line-contrast); border-left: 0; } }
@include at-most('phone') { .system-overview { gap: 0.6rem; } .system-overview__signal-copy, .system-overview__signal-meta, .system-overview__chart, .system-overview__pages, .system-overview__permissions { padding: 0.85rem; } .system-overview__metrics, .system-overview__charts, .system-overview__details { grid-template-columns: 1fr; gap: 0.6rem; } .system-overview__metric { min-height: 6.5rem; } .system-overview__chart-canvas { height: 12rem; } }
</style>
