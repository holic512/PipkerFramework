<!--
  @file index.vue
  @project Pipker Framework
  @module 系统接口监控
  @description 提供接口调用量、成功率、响应耗时、采集质量和慢请求的只读分析工作区。
  @logic 统一时间范围驱动概览、接口聚合和慢请求分页；趋势图随主题和容器更新，接口行可下钻到明细与单条安全元数据详情。
  @dependencies Vue、Element Plus、ECharts、接口监控 API、frontend API contracts
  @index_tags api-monitor、observability、performance、success-rate、slow-request、echarts
  @author holic512
-->
<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { DataAnalysis, Refresh, Search, WarningFilled } from '@element-plus/icons-vue'
import { init, use, type ECharts, type EChartsCoreOption } from 'echarts/core'
import { BarChart, LineChart } from 'echarts/charts'
import { GridComponent, LegendComponent, TooltipComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import { ApiBusinessError } from '../../../core/api/contracts'
import type {
  ApiAccessEndpointSummary,
  ApiAccessLogDetail,
  ApiAccessLogSummary,
  ApiAccessOverview,
  ApiAccessOverviewSummary,
  PageResult,
} from '../../../core/api/contracts'
import {
  getApiAccessLogDetail,
  getApiAccessLogPage,
  getApiEndpointPage,
  getApiLogOverview,
  type ApiEndpointSort,
  type SortDirection,
} from './api/apiLogManagement'

use([BarChart, LineChart, GridComponent, LegendComponent, TooltipComponent, CanvasRenderer])

type RangePreset = '1h' | '24h' | '7d' | '30d' | 'custom'

interface EndpointFilters {
  keyword: string
  sortBy: ApiEndpointSort
  sortDirection: SortDirection
}

interface LogFilters {
  keyword: string
  httpMethod: string | undefined
  success: boolean | undefined
}

interface MonitorPalette {
  primary: string
  secondary: string
  danger: string
  warning: string
  muted: string
  soft: string
  strong: string
  surface: string
  line: string
}

const EMPTY_SUMMARY: ApiAccessOverviewSummary = {
  totalCount: 0,
  successCount: 0,
  failureCount: 0,
  successRate: 0,
  avgDurationMs: 0,
  maxDurationMs: 0,
  slowCount: 0,
}
const EMPTY_ENDPOINT_PAGE: PageResult<ApiAccessEndpointSummary> = { page: 1, pageSize: 20, total: 0, records: [] }
const EMPTY_LOG_PAGE: PageResult<ApiAccessLogSummary> = { page: 1, pageSize: 20, total: 0, records: [] }
const rangePresets: Array<{ value: Exclude<RangePreset, 'custom'>; label: string; hours: number }> = [
  { value: '1h', label: '1 小时', hours: 1 },
  { value: '24h', label: '24 小时', hours: 24 },
  { value: '7d', label: '7 天', hours: 24 * 7 },
  { value: '30d', label: '30 天', hours: 24 * 30 },
]

const selectedPreset = ref<RangePreset>('24h')
const timeRange = ref<[string, string]>(createRange(24))
const slowThresholdMs = ref(1000)
const includeMonitorEndpoints = ref(false)
const overview = ref<ApiAccessOverview | null>(null)
const endpointPage = ref<PageResult<ApiAccessEndpointSummary>>({ ...EMPTY_ENDPOINT_PAGE })
const logPage = ref<PageResult<ApiAccessLogSummary>>({ ...EMPTY_LOG_PAGE })
const endpointFilters = reactive<EndpointFilters>({ keyword: '', sortBy: 'AVG_DURATION', sortDirection: 'DESC' })
const logFilters = reactive<LogFilters>({ keyword: '', httpMethod: undefined, success: undefined })
const selectedEndpoint = ref<ApiAccessEndpointSummary | null>(null)
const detail = ref<ApiAccessLogDetail | null>(null)
const detailVisible = ref(false)
const detailLoading = ref(false)
const overviewLoading = ref(false)
const endpointLoading = ref(false)
const logLoading = ref(false)
const overviewError = ref<string | null>(null)
const endpointError = ref<string | null>(null)
const logError = ref<string | null>(null)
const chartElement = ref<HTMLElement | null>(null)
let trendChart: ECharts | null = null
let resizeObserver: ResizeObserver | null = null
let themeObserver: MutationObserver | null = null
let motionQuery: MediaQueryList | null = null

const summary = computed(() => overview.value?.summary ?? EMPTY_SUMMARY)
const refreshing = computed(() => overviewLoading.value || endpointLoading.value || logLoading.value)
const collectorHasGap = computed(() => {
  const collector = overview.value?.collector
  return collector ? collector.droppedSinceStartup > 0 || collector.writeFailuresSinceStartup > 0 : false
})
const queueUsage = computed(() => {
  const collector = overview.value?.collector
  if (!collector || collector.queueCapacity <= 0) return 0
  return Math.min(100, collector.queueDepth * 100 / collector.queueCapacity)
})
const selectedEndpointLabel = computed(() => selectedEndpoint.value
  ? `${selectedEndpoint.value.httpMethod} ${selectedEndpoint.value.routePattern || '未解析请求'}`
  : '全部接口')

onMounted(async () => {
  await nextTick()
  initialiseChart()
  resizeObserver = new ResizeObserver(() => trendChart?.resize())
  if (chartElement.value) resizeObserver.observe(chartElement.value)
  themeObserver = new MutationObserver(() => renderTrendChart())
  themeObserver.observe(document.documentElement, {
    attributes: true,
    attributeFilter: ['data-design-theme', 'data-color-scheme'],
  })
  motionQuery = window.matchMedia('(prefers-reduced-motion: reduce)')
  motionQuery.addEventListener('change', renderTrendChart)
  await loadAll()
})

onBeforeUnmount(() => {
  resizeObserver?.disconnect()
  themeObserver?.disconnect()
  motionQuery?.removeEventListener('change', renderTrendChart)
  trendChart?.dispose()
  trendChart = null
})

function selectPreset(preset: Exclude<RangePreset, 'custom'>, hours: number): void {
  selectedPreset.value = preset
  timeRange.value = createRange(hours)
  void loadAll(1, 1)
}

function selectCustomRange(): void {
  selectedPreset.value = 'custom'
}

async function loadAll(
  endpointPageNumber = endpointPage.value.page,
  logPageNumber = logPage.value.page,
): Promise<void> {
  await Promise.all([
    loadOverview(),
    loadEndpoints(endpointPageNumber),
    loadLogs(logPageNumber),
  ])
}

async function loadOverview(): Promise<void> {
  overviewLoading.value = true
  overviewError.value = null
  try {
    overview.value = await getApiLogOverview({
      ...rangeQuery(),
      slowThresholdMs: slowThresholdMs.value,
      includeMonitorEndpoints: includeMonitorEndpoints.value,
    })
    await nextTick()
    renderTrendChart()
  } catch (error) {
    overviewError.value = readableError(error, '无法读取接口概览。')
  } finally {
    overviewLoading.value = false
  }
}

async function loadEndpoints(page = endpointPage.value.page): Promise<void> {
  endpointLoading.value = true
  endpointError.value = null
  try {
    endpointPage.value = await getApiEndpointPage({
      ...rangeQuery(),
      page,
      pageSize: endpointPage.value.pageSize,
      slowThresholdMs: slowThresholdMs.value,
      includeMonitorEndpoints: includeMonitorEndpoints.value,
      keyword: endpointFilters.keyword.trim() || undefined,
      sortBy: endpointFilters.sortBy,
      sortDirection: endpointFilters.sortDirection,
    })
  } catch (error) {
    endpointError.value = readableError(error, '无法读取接口分析。')
  } finally {
    endpointLoading.value = false
  }
}

async function loadLogs(page = logPage.value.page): Promise<void> {
  logLoading.value = true
  logError.value = null
  try {
    logPage.value = await getApiAccessLogPage({
      ...rangeQuery(),
      page,
      pageSize: logPage.value.pageSize,
      keyword: logFilters.keyword.trim() || undefined,
      httpMethod: logFilters.httpMethod,
      success: logFilters.success,
      minDurationMs: slowThresholdMs.value,
      routePattern: selectedEndpoint.value?.routePattern || undefined,
      unresolvedRoute: selectedEndpoint.value?.routePattern === null || undefined,
    })
  } catch (error) {
    logError.value = readableError(error, '无法读取慢请求明细。')
  } finally {
    logLoading.value = false
  }
}

function applyControls(): void {
  selectedEndpoint.value = null
  logFilters.keyword = ''
  logFilters.httpMethod = undefined
  void loadAll(1, 1)
}

function applyEndpointFilters(): void {
  void loadEndpoints(1)
}

function clearEndpointFilters(): void {
  endpointFilters.keyword = ''
  endpointFilters.sortBy = 'AVG_DURATION'
  endpointFilters.sortDirection = 'DESC'
  void loadEndpoints(1)
}

function handleEndpointSort(change: { prop: string; order: 'ascending' | 'descending' | null }): void {
  const sortMap: Record<string, ApiEndpointSort> = {
    totalCount: 'REQUEST_COUNT',
    successRate: 'SUCCESS_RATE',
    avgDurationMs: 'AVG_DURATION',
    maxDurationMs: 'MAX_DURATION',
    slowCount: 'SLOW_COUNT',
  }
  if (!change.order || !sortMap[change.prop]) return
  endpointFilters.sortBy = sortMap[change.prop]
  endpointFilters.sortDirection = change.order === 'ascending' ? 'ASC' : 'DESC'
  void loadEndpoints(1)
}

function inspectEndpoint(row: ApiAccessEndpointSummary): void {
  selectedEndpoint.value = row
  logFilters.keyword = ''
  logFilters.httpMethod = row.httpMethod
  void loadLogs(1)
}

function clearSelectedEndpoint(): void {
  selectedEndpoint.value = null
  logFilters.keyword = ''
  logFilters.httpMethod = undefined
  void loadLogs(1)
}

async function openDetail(logId: string): Promise<void> {
  detailVisible.value = true
  detailLoading.value = true
  detail.value = null
  try {
    detail.value = await getApiAccessLogDetail(logId)
  } catch (error) {
    logError.value = readableError(error, '无法读取日志详情。')
    detailVisible.value = false
  } finally {
    detailLoading.value = false
  }
}

function changeEndpointPage(page: number): void {
  void loadEndpoints(page)
}

function changeEndpointPageSize(pageSize: number): void {
  endpointPage.value = { ...endpointPage.value, page: 1, pageSize }
  void loadEndpoints(1)
}

function changeLogPage(page: number): void {
  void loadLogs(page)
}

function changeLogPageSize(pageSize: number): void {
  logPage.value = { ...logPage.value, page: 1, pageSize }
  void loadLogs(1)
}

function initialiseChart(): void {
  if (chartElement.value) trendChart = init(chartElement.value)
  renderTrendChart()
}

function renderTrendChart(): void {
  if (!trendChart) return
  const palette = readPalette()
  const trend = overview.value?.trend ?? []
  const bucket = overview.value?.bucket ?? 'HOUR'
  const option: EChartsCoreOption = {
    animation: !(motionQuery?.matches ?? window.matchMedia('(prefers-reduced-motion: reduce)').matches),
    animationDuration: 320,
    color: [palette.primary, palette.secondary, palette.danger],
    grid: { top: 48, right: 54, bottom: 36, left: 28, containLabel: true },
    legend: {
      top: 4,
      right: 0,
      itemWidth: 12,
      itemHeight: 7,
      textStyle: { color: palette.muted, fontSize: 11 },
      data: ['请求量', '平均耗时', '失败量'],
    },
    tooltip: {
      trigger: 'axis',
      backgroundColor: palette.surface,
      borderColor: palette.line,
      textStyle: { color: palette.strong },
      valueFormatter: (value: unknown) => typeof value === 'number' ? value.toLocaleString('zh-CN') : String(value),
    },
    xAxis: {
      type: 'category',
      boundaryGap: true,
      data: trend.map(item => formatBucket(item.bucketStart, bucket)),
      axisLabel: { color: palette.soft, fontSize: 10, hideOverlap: true },
      axisLine: { lineStyle: { color: palette.line } },
      axisTick: { show: false },
    },
    yAxis: [
      {
        type: 'value',
        name: '请求',
        minInterval: 1,
        nameTextStyle: { color: palette.soft, fontSize: 10 },
        axisLabel: { color: palette.soft, fontSize: 10 },
        splitLine: { lineStyle: { color: palette.line, type: 'dashed' } },
      },
      {
        type: 'value',
        name: '毫秒',
        nameTextStyle: { color: palette.soft, fontSize: 10 },
        axisLabel: { color: palette.soft, fontSize: 10 },
        splitLine: { show: false },
      },
    ],
    series: [
      {
        name: '请求量',
        type: 'bar',
        data: trend.map(item => item.totalCount),
        barMaxWidth: 24,
        itemStyle: { color: palette.primary, borderRadius: [3, 3, 0, 0] },
      },
      {
        name: '平均耗时',
        type: 'line',
        yAxisIndex: 1,
        data: trend.map(item => item.avgDurationMs),
        symbol: 'circle',
        symbolSize: 5,
        showSymbol: trend.length <= 36,
        smooth: 0.24,
        lineStyle: { width: 2, color: palette.secondary },
        itemStyle: { color: palette.secondary },
      },
      {
        name: '失败量',
        type: 'line',
        data: trend.map(item => item.failureCount),
        symbol: 'none',
        lineStyle: { width: 1.5, color: palette.danger, type: 'dashed' },
        itemStyle: { color: palette.danger },
      },
    ],
  }
  trendChart.setOption(option, true)
}

function rangeQuery(): { startTime: string; endTime: string } {
  return { startTime: timeRange.value[0], endTime: timeRange.value[1] }
}

function createRange(hours: number): [string, string] {
  const end = new Date()
  const start = new Date(end.getTime() - hours * 60 * 60 * 1000)
  return [formatLocalIso(start), formatLocalIso(end)]
}

function formatLocalIso(date: Date): string {
  const pad = (value: number) => String(value).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
}

function formatBucket(value: string, bucket: 'HOUR' | 'DAY'): string {
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return new Intl.DateTimeFormat('zh-CN', bucket === 'HOUR'
    ? { month: '2-digit', day: '2-digit', hour: '2-digit', hour12: false }
    : { month: '2-digit', day: '2-digit' }).format(date)
}

function formatTime(value: string | null): string {
  if (!value) return '—'
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? value : new Intl.DateTimeFormat('zh-CN', {
    dateStyle: 'medium',
    timeStyle: 'medium',
    hour12: false,
  }).format(date)
}

function formatNumber(value: number): string {
  return value.toLocaleString('zh-CN')
}

function formatDuration(value: number | null): string {
  return value === null ? '—' : `${value.toLocaleString('zh-CN', { maximumFractionDigits: 2 })} ms`
}

function readPalette(): MonitorPalette {
  const styles = getComputedStyle(document.documentElement)
  const read = (name: string) => styles.getPropertyValue(name).trim()
  return {
    primary: read('--color-accent-primary'),
    secondary: read('--color-accent-secondary'),
    danger: read('--color-accent-danger'),
    warning: read('--color-accent-warning'),
    muted: read('--color-ink-muted'),
    soft: read('--color-ink-soft'),
    strong: read('--color-ink-strong'),
    surface: read('--color-surface-raised'),
    line: read('--color-line-subtle'),
  }
}

function readableError(error: unknown, fallback: string): string {
  return error instanceof ApiBusinessError ? error.message : fallback
}
</script>

<template>
  <section class="api-monitor" aria-label="接口监控">
    <header class="api-monitor__toolbar ui-panel">
      <div class="api-monitor__presets" role="group" aria-label="快捷时间范围">
        <el-button
          v-for="preset in rangePresets"
          :key="preset.value"
          size="small"
          :type="selectedPreset === preset.value ? 'primary' : 'default'"
          :plain="selectedPreset !== preset.value"
          @click="selectPreset(preset.value, preset.hours)"
        >{{ preset.label }}</el-button>
      </div>
      <el-date-picker
        v-model="timeRange"
        class="api-monitor__date-range"
        type="datetimerange"
        value-format="YYYY-MM-DDTHH:mm:ss"
        format="YYYY-MM-DD HH:mm:ss"
        range-separator="至"
        start-placeholder="开始时间"
        end-placeholder="结束时间"
        :clearable="false"
        @change="selectCustomRange"
      />
      <label class="api-monitor__threshold">
        <span>慢接口阈值</span>
        <el-input-number v-model="slowThresholdMs" :min="0" :max="3600000" :step="100" controls-position="right" />
        <span>ms</span>
      </label>
      <el-tooltip content="概览和接口聚合默认排除监控接口，原始明细始终保留" placement="bottom">
        <el-checkbox v-model="includeMonitorEndpoints">纳入监控接口</el-checkbox>
      </el-tooltip>
      <el-button type="primary" :icon="Refresh" :loading="refreshing" @click="applyControls">刷新</el-button>
    </header>

    <div v-if="overviewError" class="api-monitor__notice" role="alert">
      <span>{{ overviewError }}</span>
      <el-button link type="primary" :loading="overviewLoading" @click="loadOverview">重试概览</el-button>
    </div>

    <section
      v-if="overview"
      class="api-monitor__collector"
      :class="{ 'api-monitor__collector--warning': collectorHasGap }"
      aria-label="异步采集器状态"
    >
      <span class="api-monitor__collector-state">
        <i aria-hidden="true"></i>
        {{ collectorHasGap ? '存在采集数据缺口' : '异步采集器运行正常' }}
      </span>
      <span>队列 {{ overview.collector.queueDepth }} / {{ overview.collector.queueCapacity }}（{{ queueUsage.toFixed(1) }}%）</span>
      <span>本实例启动以来丢弃 {{ formatNumber(overview.collector.droppedSinceStartup) }} 条</span>
      <span>写入失败 {{ formatNumber(overview.collector.writeFailuresSinceStartup) }} 条</span>
      <span v-if="collectorHasGap" class="api-monitor__collector-explanation">队列满或数据库写入失败会造成统计低估，但不会影响业务接口响应。</span>
    </section>

    <section class="api-monitor__metrics" aria-label="接口汇总">
      <article class="api-monitor__metric ui-panel">
        <span>请求总量</span><strong>{{ formatNumber(summary.totalCount) }}</strong><small>选定范围内的接口请求</small>
      </article>
      <article class="api-monitor__metric ui-panel">
        <span>成功率</span><strong>{{ summary.successRate.toFixed(2) }}%</strong><small>{{ formatNumber(summary.failureCount) }} 次失败</small>
      </article>
      <article class="api-monitor__metric ui-panel">
        <span>平均耗时</span><strong>{{ formatDuration(summary.avgDurationMs) }}</strong><small>最大 {{ formatDuration(summary.maxDurationMs) }}</small>
      </article>
      <article class="api-monitor__metric ui-panel">
        <span>慢请求</span><strong>{{ formatNumber(summary.slowCount) }}</strong><small>端到端耗时 ≥ {{ slowThresholdMs }} ms</small>
      </article>
    </section>

    <article class="api-monitor__trend ui-panel" v-loading="overviewLoading">
      <header>
        <div>
          <h1>请求量、耗时与失败趋势</h1>
          <p>按{{ overview?.bucket === 'DAY' ? '天' : '小时' }}补齐时间桶，监控接口{{ includeMonitorEndpoints ? '已纳入' : '默认排除' }}聚合。</p>
        </div>
        <el-icon aria-hidden="true"><DataAnalysis /></el-icon>
      </header>
      <div v-if="!overviewError && (overview?.trend.length ?? 0) === 0" class="api-monitor__empty">当前范围暂无趋势数据。</div>
      <div ref="chartElement" class="api-monitor__chart" role="img" aria-label="请求量、平均耗时和失败数量组合趋势图"></div>
    </article>

    <section class="api-monitor__analysis ui-panel" aria-label="接口分析">
      <header class="api-monitor__section-header">
        <div><h2>接口分析</h2><p>按 HTTP 方法和标准路由聚合；未解析请求统一归组。</p></div>
        <form class="api-monitor__inline-filter" @submit.prevent="applyEndpointFilters">
          <el-input v-model="endpointFilters.keyword" clearable :prefix-icon="Search" aria-label="用途或路径关键字" placeholder="用途或路径" />
          <el-button native-type="submit" :loading="endpointLoading">查询</el-button>
          <el-button :disabled="endpointLoading" @click="clearEndpointFilters">重置</el-button>
        </form>
      </header>
      <div v-if="endpointError" class="api-monitor__section-error" role="alert">
        <span>{{ endpointError }}</span><el-button link type="primary" @click="loadEndpoints()">重试</el-button>
      </div>
      <el-table
        v-loading="endpointLoading"
        :data="endpointError ? [] : endpointPage.records"
        empty-text="当前时间范围内没有接口聚合数据"
        @sort-change="handleEndpointSort"
      >
        <el-table-column label="用途" min-width="165" show-overflow-tooltip>
          <template #default="{ row }: { row: ApiAccessEndpointSummary }">{{ row.purpose || '未标注用途' }}</template>
        </el-table-column>
        <el-table-column label="方法与路由" min-width="245">
          <template #default="{ row }: { row: ApiAccessEndpointSummary }">
            <button class="api-monitor__endpoint-link ui-focusable" type="button" @click="inspectEndpoint(row)">
              <el-tag size="small" effect="plain">{{ row.httpMethod }}</el-tag>
              <code>{{ row.routePattern || '未解析请求' }}</code>
            </button>
          </template>
        </el-table-column>
        <el-table-column prop="totalCount" label="调用量" width="105" align="right" sortable="custom" />
        <el-table-column prop="successRate" label="成功率" width="112" align="right" sortable="custom">
          <template #default="{ row }: { row: ApiAccessEndpointSummary }">
            <span :class="{ 'api-monitor__danger': row.successRate < 100 }">{{ row.successRate.toFixed(2) }}%</span>
          </template>
        </el-table-column>
        <el-table-column prop="avgDurationMs" label="平均耗时" width="125" align="right" sortable="custom">
          <template #default="{ row }: { row: ApiAccessEndpointSummary }">{{ formatDuration(row.avgDurationMs) }}</template>
        </el-table-column>
        <el-table-column prop="maxDurationMs" label="最大耗时" width="125" align="right" sortable="custom">
          <template #default="{ row }: { row: ApiAccessEndpointSummary }">{{ formatDuration(row.maxDurationMs) }}</template>
        </el-table-column>
        <el-table-column prop="slowCount" label="慢请求" width="105" align="right" sortable="custom" />
      </el-table>
      <footer class="api-monitor__pagination">
        <span>共 {{ endpointError ? 0 : endpointPage.total }} 个接口分组</span>
        <el-pagination
          background
          :current-page="endpointPage.page"
          :page-size="endpointPage.pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :total="endpointPage.total"
          layout="sizes, prev, pager, next"
          @current-change="changeEndpointPage"
          @size-change="changeEndpointPageSize"
        />
      </footer>
    </section>

    <section class="api-monitor__slow ui-panel" aria-label="慢请求排查">
      <header class="api-monitor__section-header">
        <div>
          <h2>慢请求排查</h2>
          <p>{{ selectedEndpointLabel }} · 仅展示总耗时不低于 {{ slowThresholdMs }} ms 的明细。</p>
        </div>
        <form class="api-monitor__inline-filter api-monitor__inline-filter--logs" @submit.prevent="loadLogs(1)">
          <el-input v-model="logFilters.keyword" clearable aria-label="路径、用途或 TraceId" placeholder="路径、用途或 TraceId" />
          <el-select v-model="logFilters.httpMethod" clearable aria-label="HTTP 方法" placeholder="全部方法">
            <el-option v-for="method in ['GET', 'POST', 'PUT', 'PATCH', 'DELETE']" :key="method" :label="method" :value="method" />
          </el-select>
          <el-select v-model="logFilters.success" clearable aria-label="执行结果" placeholder="全部结果">
            <el-option label="成功" :value="true" /><el-option label="失败" :value="false" />
          </el-select>
          <el-button native-type="submit" :loading="logLoading">筛选</el-button>
          <el-button v-if="selectedEndpoint" @click="clearSelectedEndpoint">取消接口联动</el-button>
        </form>
      </header>
      <div v-if="logError" class="api-monitor__section-error" role="alert">
        <span>{{ logError }}</span><el-button link type="primary" @click="loadLogs()">重试</el-button>
      </div>
      <el-table v-loading="logLoading" :data="logError ? [] : logPage.records" empty-text="当前筛选条件下没有慢请求">
        <el-table-column label="时间" min-width="165">
          <template #default="{ row }: { row: ApiAccessLogSummary }">{{ formatTime(row.occurredAt) }}</template>
        </el-table-column>
        <el-table-column label="接口" min-width="235" show-overflow-tooltip>
          <template #default="{ row }: { row: ApiAccessLogSummary }"><code>{{ row.httpMethod }} {{ row.routePattern || row.requestPath }}</code></template>
        </el-table-column>
        <el-table-column label="用途" min-width="150" show-overflow-tooltip>
          <template #default="{ row }: { row: ApiAccessLogSummary }">{{ row.purpose || '未标注用途' }}</template>
        </el-table-column>
        <el-table-column label="结果" width="98">
          <template #default="{ row }: { row: ApiAccessLogSummary }">
            <el-tag size="small" effect="light" :type="row.success ? 'success' : 'danger'">{{ row.success ? '成功' : '失败' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="总耗时" width="118" align="right">
          <template #default="{ row }: { row: ApiAccessLogSummary }"><strong>{{ formatDuration(row.durationMs) }}</strong></template>
        </el-table-column>
        <el-table-column label="Controller" width="125" align="right">
          <template #default="{ row }: { row: ApiAccessLogSummary }">{{ formatDuration(row.handlerDurationMs) }}</template>
        </el-table-column>
        <el-table-column label="TraceId" min-width="185" show-overflow-tooltip>
          <template #default="{ row }: { row: ApiAccessLogSummary }"><code>{{ row.traceId || '—' }}</code></template>
        </el-table-column>
        <el-table-column label="" width="78" fixed="right">
          <template #default="{ row }: { row: ApiAccessLogSummary }"><el-button link type="primary" @click="openDetail(row.id)">详情</el-button></template>
        </el-table-column>
      </el-table>
      <footer class="api-monitor__pagination">
        <span>共 {{ logError ? 0 : logPage.total }} 条慢请求</span>
        <el-pagination
          background
          :current-page="logPage.page"
          :page-size="logPage.pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :total="logPage.total"
          layout="sizes, prev, pager, next"
          @current-change="changeLogPage"
          @size-change="changeLogPageSize"
        />
      </footer>
    </section>

    <el-drawer v-model="detailVisible" title="接口访问详情" size="min(36rem, 94vw)" destroy-on-close>
      <div v-loading="detailLoading" class="api-monitor__detail">
        <template v-if="detail">
          <div class="api-monitor__detail-result" :class="{ 'api-monitor__detail-result--failed': !detail.success }">
            <el-icon aria-hidden="true"><WarningFilled v-if="!detail.success" /><DataAnalysis v-else /></el-icon>
            <div><strong>{{ detail.success ? '请求成功' : '请求失败' }}</strong><span>{{ formatTime(detail.occurredAt) }}</span></div>
            <b>{{ formatDuration(detail.durationMs) }}</b>
          </div>
          <el-descriptions :column="1" border>
            <el-descriptions-item label="接口"><code>{{ detail.httpMethod }} {{ detail.routePattern || detail.requestPath }}</code></el-descriptions-item>
            <el-descriptions-item label="实际路径"><code>{{ detail.requestPath }}</code></el-descriptions-item>
            <el-descriptions-item label="用途">{{ detail.purpose || '—' }}</el-descriptions-item>
            <el-descriptions-item label="备注">{{ detail.remark || '—' }}</el-descriptions-item>
            <el-descriptions-item label="处理方法"><code>{{ detail.handlerMethod || '—' }}</code></el-descriptions-item>
            <el-descriptions-item label="耗时">端到端 {{ formatDuration(detail.durationMs) }} / Controller {{ formatDuration(detail.handlerDurationMs) }}</el-descriptions-item>
            <el-descriptions-item label="状态码">HTTP {{ detail.httpStatus }} / 业务 {{ detail.businessCode ?? '未识别' }}</el-descriptions-item>
            <el-descriptions-item label="TraceId"><code>{{ detail.traceId || '—' }}</code></el-descriptions-item>
            <el-descriptions-item label="RequestId"><code>{{ detail.requestId || '—' }}</code></el-descriptions-item>
            <el-descriptions-item label="客户端 IP"><code>{{ detail.clientIp || '—' }}</code></el-descriptions-item>
            <el-descriptions-item label="异常类型"><code>{{ detail.exceptionType || '—' }}</code></el-descriptions-item>
            <el-descriptions-item label="异常摘要">{{ detail.exceptionMessage || '—' }}</el-descriptions-item>
          </el-descriptions>
          <p class="api-monitor__detail-safety">详情仅包含诊断元数据，不保存请求参数、请求体、响应体、Cookie、Token 或 Authorization。</p>
        </template>
      </div>
    </el-drawer>
  </section>
</template>

<style scoped lang="scss">
.api-monitor { display: grid; gap: 0.7rem; width: min(100%, 92rem); margin: 0 auto; padding-bottom: 0.2rem; }
.api-monitor__toolbar { display: flex; align-items: center; flex-wrap: wrap; gap: 0.5rem; padding: 0.65rem; }
.api-monitor__presets { display: flex; gap: 0.3rem; }
.api-monitor__date-range { width: 22.5rem; }
.api-monitor__threshold { display: flex; align-items: center; gap: 0.35rem; color: var(--color-ink-muted); font-size: 0.7rem; }
.api-monitor__threshold :deep(.el-input-number) { width: 8rem; }
.api-monitor__toolbar > .el-button:last-child { margin-left: auto; }
.api-monitor__notice, .api-monitor__section-error { display: flex; align-items: center; justify-content: space-between; gap: 0.8rem; color: var(--color-accent-danger); background: color-mix(in srgb, var(--color-accent-danger) 7%, var(--color-surface-base)); font-size: 0.72rem; }
.api-monitor__notice { padding: 0.55rem 0.75rem; border-radius: var(--radius-control); }
.api-monitor__collector { display: flex; align-items: center; flex-wrap: wrap; gap: 0.45rem 1rem; padding: 0.48rem 0.7rem; color: var(--color-ink-muted); background: color-mix(in srgb, var(--color-accent-success) 7%, var(--color-surface-base)); border: 1px solid color-mix(in srgb, var(--color-accent-success) 25%, var(--color-line-subtle)); border-radius: var(--radius-control); font-size: 0.67rem; }
.api-monitor__collector--warning { background: color-mix(in srgb, var(--color-accent-warning) 8%, var(--color-surface-base)); border-color: color-mix(in srgb, var(--color-accent-warning) 32%, var(--color-line-subtle)); }
.api-monitor__collector-state { display: inline-flex; align-items: center; gap: 0.38rem; color: var(--color-ink-strong); font-weight: 700; }
.api-monitor__collector-state i { width: 0.42rem; height: 0.42rem; border-radius: 50%; background: var(--color-accent-success); box-shadow: 0 0 0 0.18rem color-mix(in srgb, var(--color-accent-success) 15%, transparent); }
.api-monitor__collector--warning .api-monitor__collector-state i { background: var(--color-accent-warning); box-shadow: 0 0 0 0.18rem color-mix(in srgb, var(--color-accent-warning) 15%, transparent); }
.api-monitor__collector-explanation { flex: 1 1 100%; color: var(--color-ink-soft); }
.api-monitor__metrics { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 0.7rem; }
.api-monitor__metric { display: grid; gap: 0.25rem; min-height: 6.6rem; padding: 0.8rem 0.9rem; }
.api-monitor__metric span { color: var(--color-ink-muted); font-size: 0.68rem; }
.api-monitor__metric strong { align-self: center; color: var(--color-ink-strong); font-family: var(--font-display); font-size: clamp(1.25rem, 2vw, 1.7rem); line-height: 1; }
.api-monitor__metric small { align-self: end; color: var(--color-ink-soft); font-size: 0.64rem; }
.api-monitor__trend { min-width: 0; padding: 1rem; overflow: hidden; }
.api-monitor__trend > header { display: flex; align-items: flex-start; justify-content: space-between; gap: 1rem; }
.api-monitor__trend h1 { margin: 0; color: var(--color-ink-strong); font-family: var(--font-display); font-size: 1rem; }
.api-monitor__trend header p { margin: 0.28rem 0 0; color: var(--color-ink-soft); font-size: 0.67rem; }
.api-monitor__trend > header > .el-icon { color: var(--color-accent-primary); font-size: 1.15rem; }
.api-monitor__chart { width: 100%; height: 20rem; margin-top: 0.25rem; }
.api-monitor__empty { display: grid; place-items: center; height: 4rem; color: var(--color-ink-soft); font-size: 0.72rem; }
.api-monitor__analysis, .api-monitor__slow { min-width: 0; overflow: hidden; }
.api-monitor__section-header { display: flex; align-items: center; justify-content: space-between; gap: 1rem; padding: 0.75rem; border-bottom: 1px solid var(--color-line-subtle); }
.api-monitor__section-header h2 { margin: 0; color: var(--color-ink-strong); font-family: var(--font-display); font-size: 0.88rem; }
.api-monitor__section-header p { margin: 0.22rem 0 0; color: var(--color-ink-soft); font-size: 0.65rem; }
.api-monitor__inline-filter { display: flex; align-items: center; gap: 0.4rem; }
.api-monitor__inline-filter .el-input { width: 14rem; }
.api-monitor__inline-filter--logs .el-input { width: 13rem; }
.api-monitor__inline-filter--logs .el-select { width: 7rem; }
.api-monitor__section-error { padding: 0.35rem 0.75rem; }
.api-monitor__analysis :deep(.el-table__header-wrapper th.el-table__cell), .api-monitor__slow :deep(.el-table__header-wrapper th.el-table__cell) { padding: 0.4rem 0; color: var(--color-ink-muted); font-size: 0.7rem; }
.api-monitor__analysis :deep(.el-table__body-wrapper td.el-table__cell), .api-monitor__slow :deep(.el-table__body-wrapper td.el-table__cell) { padding: 0.42rem 0; color: var(--color-ink-muted); font-size: 0.68rem; }
.api-monitor__analysis code, .api-monitor__slow code, .api-monitor__detail code { color: var(--color-ink-soft); font-family: var(--font-mono); font-size: 0.64rem; }
.api-monitor__endpoint-link { display: inline-flex; align-items: center; max-width: 100%; gap: 0.45rem; padding: 0; color: inherit; background: transparent; border: 0; cursor: pointer; }
.api-monitor__endpoint-link code { overflow: hidden; color: var(--color-accent-primary); text-overflow: ellipsis; white-space: nowrap; }
.api-monitor__endpoint-link:hover code { text-decoration: underline; }
.api-monitor__danger { color: var(--color-accent-danger); font-weight: 700; }
.api-monitor__pagination { display: flex; align-items: center; justify-content: space-between; gap: 0.8rem; min-height: 2.5rem; padding: 0.4rem 0.75rem; color: var(--color-ink-soft); border-top: 1px solid var(--color-line-subtle); font-size: 0.66rem; }
.api-monitor__detail { min-height: 10rem; }
.api-monitor__detail-result { display: flex; align-items: center; gap: 0.65rem; margin-bottom: 0.8rem; padding: 0.7rem; color: var(--color-accent-success); background: color-mix(in srgb, var(--color-accent-success) 8%, var(--color-surface-base)); border-radius: var(--radius-control); }
.api-monitor__detail-result--failed { color: var(--color-accent-danger); background: color-mix(in srgb, var(--color-accent-danger) 8%, var(--color-surface-base)); }
.api-monitor__detail-result .el-icon { font-size: 1.1rem; }
.api-monitor__detail-result div { display: grid; gap: 0.15rem; }
.api-monitor__detail-result div strong { color: var(--color-ink-strong); font-size: 0.76rem; }
.api-monitor__detail-result div span { color: var(--color-ink-soft); font-size: 0.63rem; }
.api-monitor__detail-result b { margin-left: auto; font-family: var(--font-mono); font-size: 0.82rem; }
.api-monitor__detail-safety { margin: 0.8rem 0 0; color: var(--color-ink-soft); font-size: 0.65rem; line-height: 1.6; }
@media (max-width: 1120px) {
  .api-monitor__toolbar > .el-button:last-child { margin-left: 0; }
  .api-monitor__date-range { flex: 1 1 20rem; }
  .api-monitor__section-header { align-items: stretch; flex-direction: column; }
  .api-monitor__inline-filter { flex-wrap: wrap; }
}
@media (max-width: 760px) {
  .api-monitor { gap: 0.55rem; }
  .api-monitor__toolbar { align-items: stretch; flex-direction: column; }
  .api-monitor__presets { display: grid; grid-template-columns: repeat(4, 1fr); }
  .api-monitor__date-range { width: 100%; }
  .api-monitor__threshold { justify-content: space-between; }
  .api-monitor__toolbar > .el-button:last-child { width: 100%; }
  .api-monitor__metrics { grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 0.55rem; }
  .api-monitor__metric { min-height: 5.8rem; }
  .api-monitor__trend { padding: 0.8rem; }
  .api-monitor__chart { height: 16rem; }
  .api-monitor__inline-filter .el-input, .api-monitor__inline-filter--logs .el-input { flex: 1 1 100%; width: 100%; }
  .api-monitor__inline-filter--logs .el-select { flex: 1; min-width: 7rem; }
  .api-monitor__pagination { align-items: flex-start; flex-direction: column; }
  .api-monitor__pagination :deep(.el-pagination) { max-width: 100%; overflow-x: auto; }
}
@media (max-width: 480px) {
  .api-monitor__presets, .api-monitor__metrics { grid-template-columns: 1fr 1fr; }
}
@media (prefers-reduced-motion: reduce) {
  .api-monitor *, .api-monitor *::before, .api-monitor *::after { scroll-behavior: auto !important; transition-duration: 0.01ms !important; animation-duration: 0.01ms !important; }
}
</style>
