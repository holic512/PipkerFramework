<!--
  @file index.vue
  @project Pipker Framework
  @module 系统登录日志
  @description 提供登录、登出和当前会话鉴权记录的只读筛选分页工作区。
  @logic 按账号/IP/TraceId、事件、结果和本地时间范围请求服务端分页数据，以稳定中文标签展示结果，不提供删除、导出或清理操作。
  @dependencies Vue、Element Plus、登录日志 API、frontend API contracts
  @index_tags authentication、audit、login-log、pagination、read-only、administration
  @author holic512
-->
<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ApiBusinessError } from '../../../core/api/contracts'
import type {
  AuthenticationEventResult,
  AuthenticationEventType,
  AuthenticationFailureReason,
  PageResult,
  SystemLoginLogSummary,
} from '../../../core/api/contracts'
import { getLoginLogPage } from './api/loginLogManagement'

interface LoginLogFilters {
  keyword: string
  eventType: AuthenticationEventType | undefined
  result: AuthenticationEventResult | undefined
  timeRange: [string, string] | null
}

const EMPTY_PAGE: PageResult<SystemLoginLogSummary> = {
  page: 1,
  pageSize: 10,
  total: 0,
  records: [],
}

const filters = reactive<LoginLogFilters>({
  keyword: '',
  eventType: undefined,
  result: undefined,
  timeRange: null,
})
const loginLogPage = ref<PageResult<SystemLoginLogSummary>>(EMPTY_PAGE)
const loading = ref(false)
const errorMessage = ref<string | null>(null)

const eventLabels: Record<AuthenticationEventType, string> = {
  LOGIN: '登录',
  LOGOUT: '登出',
  AUTH_CHECK: '会话鉴权',
}

const failureLabels: Record<AuthenticationFailureReason, string> = {
  INVALID_CREDENTIALS: '账号或密码错误',
  ACCOUNT_DISABLED: '账户已停用',
  AUTH_REQUIRED: '未登录或会话失效',
  AUTH_FORBIDDEN: '权限不足',
  VALIDATION_FAILED: '请求格式错误',
  INTERNAL_ERROR: '内部错误',
}

onMounted(() => {
  void loadLoginLogs()
})

async function loadLoginLogs(page = loginLogPage.value.page): Promise<void> {
  loading.value = true
  errorMessage.value = null
  try {
    loginLogPage.value = await getLoginLogPage({
      page,
      pageSize: loginLogPage.value.pageSize,
      keyword: filters.keyword.trim() || undefined,
      eventType: filters.eventType,
      result: filters.result,
      startTime: filters.timeRange?.[0],
      endTime: filters.timeRange?.[1],
    })
  } catch (error) {
    errorMessage.value = readableError(error, '无法读取登录日志。')
  } finally {
    loading.value = false
  }
}

function applyFilters(): void {
  void loadLoginLogs(1)
}

function clearFilters(): void {
  filters.keyword = ''
  filters.eventType = undefined
  filters.result = undefined
  filters.timeRange = null
  void loadLoginLogs(1)
}

function changePage(page: number): void {
  void loadLoginLogs(page)
}

function changePageSize(pageSize: number): void {
  loginLogPage.value = { ...loginLogPage.value, page: 1, pageSize }
  void loadLoginLogs(1)
}

function eventTagType(eventType: AuthenticationEventType): 'primary' | 'warning' | 'info' {
  if (eventType === 'LOGIN') return 'primary'
  if (eventType === 'LOGOUT') return 'warning'
  return 'info'
}

function resultLabel(result: AuthenticationEventResult): string {
  return result === 'SUCCESS' ? '成功' : '失败'
}

function failureLabel(reason: AuthenticationFailureReason | null): string {
  return reason ? failureLabels[reason] : '—'
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

function readableError(error: unknown, fallback: string): string {
  return error instanceof ApiBusinessError ? error.message : fallback
}
</script>

<template>
  <section class="login-log-page" aria-label="登录日志">
    <form class="login-log-filters" aria-label="登录日志筛选" @submit.prevent="applyFilters">
      <div class="login-log-filter-fields">
        <el-input
          v-model="filters.keyword"
          clearable
          aria-label="账号、IP 或 TraceId"
          class="login-log-search"
          placeholder="账号、客户端 IP 或 TraceId"
        />
        <el-select v-model="filters.eventType" clearable aria-label="事件类型" placeholder="全部事件">
          <el-option label="登录" value="LOGIN" />
          <el-option label="登出" value="LOGOUT" />
          <el-option label="会话鉴权" value="AUTH_CHECK" />
        </el-select>
        <el-select v-model="filters.result" clearable aria-label="执行结果" placeholder="全部结果">
          <el-option label="成功" value="SUCCESS" />
          <el-option label="失败" value="FAILURE" />
        </el-select>
        <el-date-picker
          v-model="filters.timeRange"
          class="login-log-time-range"
          type="datetimerange"
          value-format="YYYY-MM-DDTHH:mm:ss"
          format="YYYY-MM-DD HH:mm:ss"
          range-separator="至"
          start-placeholder="开始时间"
          end-placeholder="结束时间"
          :clearable="true"
        />
      </div>
      <el-divider direction="vertical" class="login-log-filter-divider" />
      <div class="login-log-filter-actions">
        <el-button native-type="submit" type="primary" :loading="loading">查询</el-button>
        <el-button :disabled="loading" @click="clearFilters">重置</el-button>
      </div>
    </form>

    <div v-if="errorMessage" class="login-log-notice" role="alert">
      <span>{{ errorMessage }}</span>
      <el-button link type="primary" :loading="loading" @click="loadLoginLogs()">重试</el-button>
    </div>

    <section class="login-log-workspace ui-panel">
      <div class="login-log-table-container">
        <el-table
          v-loading="loading"
          :data="errorMessage ? [] : loginLogPage.records"
          height="100%"
          empty-text="没有符合当前筛选条件的登录日志"
        >
          <el-table-column label="发生时间" min-width="168">
            <template #default="{ row }: { row: SystemLoginLogSummary }">{{ formatTime(row.occurredAt) }}</template>
          </el-table-column>
          <el-table-column label="账号 / 用户 ID" min-width="180">
            <template #default="{ row }: { row: SystemLoginLogSummary }">
              <div class="login-log-identity">
                <strong>{{ row.username || '未识别账号' }}</strong>
                <code>{{ row.userId || '—' }}</code>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="事件" width="112">
            <template #default="{ row }: { row: SystemLoginLogSummary }">
              <el-tooltip :content="row.eventType" placement="top">
                <el-tag size="small" effect="light" :type="eventTagType(row.eventType)">{{ eventLabels[row.eventType] }}</el-tag>
              </el-tooltip>
            </template>
          </el-table-column>
          <el-table-column label="结果" width="78">
            <template #default="{ row }: { row: SystemLoginLogSummary }">
              <el-tooltip :content="row.result" placement="top">
                <el-tag size="small" effect="light" :type="row.result === 'SUCCESS' ? 'success' : 'danger'">
                  {{ resultLabel(row.result) }}
                </el-tag>
              </el-tooltip>
            </template>
          </el-table-column>
          <el-table-column label="失败原因" min-width="154" show-overflow-tooltip>
            <template #default="{ row }: { row: SystemLoginLogSummary }">
              <el-tooltip v-if="row.failureReason" :content="row.failureReason" placement="top">
                <span>{{ failureLabel(row.failureReason) }}</span>
              </el-tooltip>
              <span v-else>—</span>
            </template>
          </el-table-column>
          <el-table-column prop="clientIp" label="客户端 IP" min-width="132" show-overflow-tooltip>
            <template #default="{ row }: { row: SystemLoginLogSummary }">{{ row.clientIp || '—' }}</template>
          </el-table-column>
          <el-table-column label="请求" min-width="205" show-overflow-tooltip>
            <template #default="{ row }: { row: SystemLoginLogSummary }">
              <code class="login-log-request">{{ row.httpMethod || '—' }} {{ row.requestPath || '—' }}</code>
            </template>
          </el-table-column>
          <el-table-column prop="traceId" label="TraceId" min-width="190" show-overflow-tooltip>
            <template #default="{ row }: { row: SystemLoginLogSummary }"><code>{{ row.traceId || '—' }}</code></template>
          </el-table-column>
          <el-table-column prop="userAgent" label="User-Agent" min-width="220" show-overflow-tooltip>
            <template #default="{ row }: { row: SystemLoginLogSummary }">{{ row.userAgent || '—' }}</template>
          </el-table-column>
        </el-table>
      </div>

      <footer class="login-log-page-summary">
        <span>{{ errorMessage ? '—' : `共 ${loginLogPage.total} 条认证记录` }}</span>
        <el-pagination
          background
          :current-page="loginLogPage.page"
          :page-size="loginLogPage.pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :total="loginLogPage.total"
          layout="sizes, prev, pager, next"
          @current-change="changePage"
          @size-change="changePageSize"
        />
      </footer>
    </section>
  </section>
</template>

<style scoped lang="scss">
.login-log-page { display: flex; flex: 1; flex-direction: column; width: 100%; min-height: 0; gap: 0.625rem; }
.login-log-filters, .login-log-filter-fields, .login-log-filter-actions { display: flex; align-items: center; }
.login-log-filters { flex-wrap: wrap; gap: 0.5rem; }
.login-log-filter-fields { flex: 1 1 0; min-width: 0; gap: 0.5rem; }
.login-log-search { flex: 1; min-width: 14rem; }
.login-log-filter-fields > .el-select { width: 8rem; }
.login-log-time-range { width: 23rem; }
.login-log-filter-actions { gap: 0.5rem; }
:deep(.login-log-filter-divider.el-divider--vertical) { height: 1.35rem; margin: 0 0.125rem; border-color: var(--color-line-subtle); }
.login-log-notice { display: flex; align-items: center; justify-content: space-between; gap: 0.75rem; padding: 0.55rem 0.75rem; color: var(--color-accent-danger); background: color-mix(in srgb, var(--color-accent-danger) 7%, var(--color-surface-base)); border-radius: var(--radius-control); font-size: 0.78rem; }
.login-log-workspace { display: flex; flex: 1; flex-direction: column; min-height: 0; overflow: hidden; }
.login-log-table-container { flex: 1; min-height: 0; }
.login-log-identity { display: grid; gap: 0.15rem; min-width: 0; }
.login-log-identity strong, .login-log-identity code { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.login-log-identity strong { color: var(--color-ink-strong); font-size: 0.72rem; font-weight: 650; }
.login-log-identity code, .login-log-request, .login-log-workspace code { color: var(--color-ink-soft); font-family: var(--font-mono); font-size: 0.64rem; }
.login-log-workspace :deep(.el-table__header-wrapper th.el-table__cell) { padding: 0.35rem 0; color: var(--color-ink-muted); font-size: 0.76rem; font-weight: 650; }
.login-log-workspace :deep(.el-table__body-wrapper td.el-table__cell) { padding: calc(0.25rem + 3px) 0; font-size: 0.7rem; }
.login-log-workspace :deep(.el-table .cell) { line-height: 1.2; }
.login-log-workspace :deep(.el-tag) { min-width: 3rem; height: 1.18rem; justify-content: center; padding-inline: 0.24rem; border-radius: var(--radius-small); font-size: 0.64rem; font-weight: 650; }
.login-log-page-summary { display: flex; align-items: center; justify-content: space-between; gap: 0.75rem; min-height: 2.25rem; padding: 0.4rem 0.75rem; color: var(--color-ink-soft); border-top: 1px solid var(--color-line-subtle); font-size: 0.68rem; }
@media (max-width: 1100px) {
  .login-log-filter-fields { flex-wrap: wrap; }
  .login-log-search { flex-basis: 100%; }
  .login-log-time-range { flex: 1 1 20rem; }
}
@media (max-width: 760px) {
  .login-log-filters { align-items: stretch; flex-direction: column; }
  .login-log-filter-fields { width: 100%; }
  .login-log-filter-fields > .el-select { flex: 1; min-width: 7rem; }
  .login-log-time-range { width: 100%; flex-basis: 100%; }
  :deep(.login-log-filter-divider.el-divider--vertical) { display: none; }
  .login-log-filter-actions { justify-content: flex-end; }
  .login-log-page-summary { align-items: flex-start; flex-direction: column; }
  .login-log-page-summary :deep(.el-pagination) { max-width: 100%; overflow-x: auto; }
}
</style>
