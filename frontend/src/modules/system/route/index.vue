<!--
  @file index.vue
  @project Pipker Framework
  @module Frontend Route Management
  @description 提供系统已落库路由与目录定义的只读筛选、分页和详情查看工作台。
  @logic 以 system_menu 的页面路径、路由名、组件索引、展示状态和类型为单一视图；目录明确不要求真实页面文件或组件索引，页面菜单则显示其索引状态。
  @dependencies Vue、Element Plus、路由管理 API、frontend API contracts
  @index_tags page、route、system-menu、pagination、read-only、administration
  @author holic512
-->
<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { ApiBusinessError } from '../../../core/api/contracts'
import type {
  PageResult,
  SystemRouteDetail,
  SystemRouteMenuType,
  SystemRouteStatus,
  SystemRouteSummary,
} from '../../../core/api/contracts'
import { getRouteDetail, getRoutePage } from './api/routeManagement'

const EMPTY_ROUTE_PAGE: PageResult<SystemRouteSummary> = {
  page: 1,
  pageSize: 10,
  total: 0,
  records: [],
}

const filters = reactive<{
  keyword: string
  menuType: SystemRouteMenuType | undefined
  visible: boolean | undefined
  status: SystemRouteStatus | undefined
}>({
  keyword: '',
  menuType: undefined,
  visible: undefined,
  status: undefined,
})
const routePage = ref<PageResult<SystemRouteSummary>>(EMPTY_ROUTE_PAGE)
const loading = ref(false)
const errorMessage = ref<string | null>(null)
const detailDrawerVisible = ref(false)
const detailLoading = ref(false)
const routeDetail = ref<SystemRouteDetail | null>(null)

onMounted(() => {
  void loadRoutes()
})

async function loadRoutes(page = routePage.value.page): Promise<void> {
  loading.value = true
  errorMessage.value = null
  try {
    routePage.value = await getRoutePage({
      page,
      pageSize: routePage.value.pageSize,
      keyword: filters.keyword.trim() || undefined,
      menuType: filters.menuType,
      visible: filters.visible,
      status: filters.status,
    })
  } catch (error) {
    errorMessage.value = readableError(error, '无法读取已落库的路由定义。')
  } finally {
    loading.value = false
  }
}

function applyFilters(): void {
  void loadRoutes(1)
}

function clearFilters(): void {
  filters.keyword = ''
  filters.menuType = undefined
  filters.visible = undefined
  filters.status = undefined
  void loadRoutes(1)
}

function changePage(page: number): void {
  void loadRoutes(page)
}

function changePageSize(pageSize: number): void {
  routePage.value = { ...routePage.value, page: 1, pageSize }
  void loadRoutes(1)
}

async function openDetail(route: SystemRouteSummary): Promise<void> {
  detailDrawerVisible.value = true
  routeDetail.value = null
  detailLoading.value = true
  try {
    routeDetail.value = await getRouteDetail(route.id)
  } catch (error) {
    ElMessage.error(readableError(error, '无法读取路由详情。'))
    detailDrawerVisible.value = false
  } finally {
    detailLoading.value = false
  }
}

function menuTypeLabel(menuType: SystemRouteMenuType): string {
  return menuType === 'DIRECTORY' ? '分类目录' : '页面菜单'
}

function menuTypeTagType(menuType: SystemRouteMenuType): 'info' | 'primary' {
  return menuType === 'DIRECTORY' ? 'info' : 'primary'
}

function statusLabel(status: SystemRouteStatus): string {
  return status === 'ENABLED' ? '启用' : '停用'
}

function statusTagType(status: SystemRouteStatus): 'success' | 'info' {
  return status === 'ENABLED' ? 'success' : 'info'
}

function visibilityLabel(visible: boolean): string {
  return visible ? '展示菜单' : '隐藏菜单'
}

function visibilityTagType(visible: boolean): 'success' | 'warning' {
  return visible ? 'success' : 'warning'
}

function pageIndexLabel(route: Pick<SystemRouteSummary, 'menuType' | 'componentKey'>): string {
  if (route.menuType === 'DIRECTORY') {
    return '分类无需页面索引'
  }
  return route.componentKey || '未配置页面索引'
}

function pageIndexState(route: Pick<SystemRouteSummary, 'menuType' | 'componentKey'>): 'category' | 'ready' | 'missing' {
  if (route.menuType === 'DIRECTORY') {
    return 'category'
  }
  return route.componentKey ? 'ready' : 'missing'
}

function routeDefinitionLabel(route: Pick<SystemRouteSummary, 'menuType' | 'routePath' | 'routeName'>): string {
  if (route.menuType === 'DIRECTORY') {
    return '目录仅承担分类和导航层级'
  }
  if (!route.routePath || !route.routeName) {
    return '页面路由信息不完整'
  }
  return '页面路由定义完整'
}

function formatTime(value: string | null): string {
  if (!value) {
    return '—'
  }
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return value
  }
  return new Intl.DateTimeFormat('zh-CN', {
    dateStyle: 'medium',
    timeStyle: 'short',
    hour12: false,
  }).format(date)
}

function readableError(error: unknown, fallback: string): string {
  return error instanceof ApiBusinessError ? error.message : fallback
}
</script>

<template>
  <main class="route-management-page ui-page-frame">
    <header class="route-management-page__header">
      <div class="route-management-page__heading">
        <p class="ui-eyebrow">SYSTEM / ROUTE LEDGER</p>
        <h1>路由管理</h1>
        <p>查看已持久化的页面路径、路由标识、页面索引和菜单展示策略。这里是只读索引，不提供新增、编辑或删除入口。</p>
      </div>
      <div class="route-management-page__count">
        <strong>{{ routePage.total }}</strong>
        <span>条路由定义</span>
      </div>
    </header>

    <section class="route-management-page__model ui-panel" aria-label="路由存储规则">
      <div class="route-management-page__model-title">
        <p class="ui-eyebrow">PERSISTENCE MODEL</p>
        <strong>system_menu 是路由与菜单的唯一数据源</strong>
      </div>
      <dl class="route-management-page__model-rules">
        <div><dt>页面菜单</dt><dd>路径 + 路由名 + 页面索引</dd></div>
        <div><dt>菜单展示</dt><dd>由 <code>visible</code> 决定</dd></div>
        <div><dt>分类目录</dt><dd>不需要真实页面文件或索引</dd></div>
      </dl>
    </section>

    <section class="route-management-page__filter ui-panel" aria-label="路由筛选">
      <div class="route-management-page__filter-fields">
        <el-input
          v-model="filters.keyword"
          clearable
          placeholder="路径、路由名、页面索引或菜单名称"
          @keyup.enter="applyFilters"
        />
        <el-select v-model="filters.menuType" clearable placeholder="全部类型">
          <el-option label="分类目录" value="DIRECTORY" />
          <el-option label="页面菜单" value="MENU" />
        </el-select>
        <el-select v-model="filters.visible" clearable placeholder="菜单展示">
          <el-option label="展示菜单" :value="true" />
          <el-option label="隐藏菜单" :value="false" />
        </el-select>
        <el-select v-model="filters.status" clearable placeholder="全部状态">
          <el-option label="启用" value="ENABLED" />
          <el-option label="停用" value="DISABLED" />
        </el-select>
      </div>
      <div class="route-management-page__filter-actions">
        <el-button :loading="loading" type="primary" @click="applyFilters">查询</el-button>
        <el-button :disabled="loading" @click="clearFilters">重置</el-button>
      </div>
    </section>

    <p v-if="errorMessage" class="route-management-page__notice" role="alert">{{ errorMessage }}</p>

    <section class="route-management-page__workspace ui-panel">
      <div class="route-management-page__toolbar">
        <span>只读配置清单 · 目录分类无需页面文件</span>
        <span>每页最多 100 条</span>
      </div>
      <el-table
        v-loading="loading"
        :data="routePage.records"
        class="route-management-page__table"
        empty-text="没有符合当前筛选条件的路由定义"
      >
        <el-table-column label="路由定义" min-width="190">
          <template #default="{ row }: { row: SystemRouteSummary }">
            <div class="route-identity">
              <strong>{{ row.menuName }}</strong>
              <code>{{ row.routeName || '—' }}</code>
              <small>{{ routeDefinitionLabel(row) }}</small>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="页面路径" min-width="190" show-overflow-tooltip>
          <template #default="{ row }: { row: SystemRouteSummary }">
            <code class="route-code">{{ row.routePath || '—' }}</code>
          </template>
        </el-table-column>
        <el-table-column label="页面索引" min-width="185" show-overflow-tooltip>
          <template #default="{ row }: { row: SystemRouteSummary }">
            <span :class="['route-index', `route-index--${pageIndexState(row)}`]">
              {{ pageIndexLabel(row) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="类型" width="106">
          <template #default="{ row }: { row: SystemRouteSummary }">
            <el-tag :type="menuTypeTagType(row.menuType)" effect="plain">{{ menuTypeLabel(row.menuType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="展示" width="104">
          <template #default="{ row }: { row: SystemRouteSummary }">
            <el-tag :type="visibilityTagType(row.visible)" effect="plain">{{ visibilityLabel(row.visible) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="88">
          <template #default="{ row }: { row: SystemRouteSummary }">
            <el-tag :type="statusTagType(row.status)" effect="plain">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="排序" width="76" prop="sort" />
        <el-table-column label="操作" fixed="right" width="78">
          <template #default="{ row }: { row: SystemRouteSummary }">
            <el-button link type="primary" @click="openDetail(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>
      <footer class="route-management-page__pagination">
        <span>当前第 {{ routePage.page }} 页</span>
        <el-pagination
          background
          :current-page="routePage.page"
          :page-size="routePage.pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :total="routePage.total"
          layout="sizes, prev, pager, next"
          @current-change="changePage"
          @size-change="changePageSize"
        />
      </footer>
    </section>

    <el-drawer v-model="detailDrawerVisible" :size="'min(46rem, 100vw)'" title="路由定义详情">
      <section v-if="routeDetail" v-loading="detailLoading" class="route-detail">
        <div class="route-detail__hero">
          <div>
            <p class="ui-eyebrow">DATABASE ROUTE RECORD</p>
            <h2>{{ routeDetail.menuName }}</h2>
            <code>{{ routeDetail.routeName || '分类目录' }}</code>
          </div>
          <div class="route-detail__tags">
            <el-tag :type="menuTypeTagType(routeDetail.menuType)" effect="plain">{{ menuTypeLabel(routeDetail.menuType) }}</el-tag>
            <el-tag :type="statusTagType(routeDetail.status)" effect="plain">{{ statusLabel(routeDetail.status) }}</el-tag>
          </div>
        </div>

        <p class="route-detail__explanation">
          <template v-if="routeDetail.componentIndexRequired">
            这是页面菜单：前端通过页面路径、路由名和页面索引解析对应模块。
          </template>
          <template v-else>
            这是分类目录：它只组织导航层级，不需要真实页面文件或页面索引。
          </template>
        </p>

        <dl class="route-detail__facts">
          <div><dt>页面路径</dt><dd><code>{{ routeDetail.routePath || '—' }}</code></dd></div>
          <div><dt>路由标识</dt><dd><code>{{ routeDetail.routeName || '—' }}</code></dd></div>
          <div><dt>页面索引</dt><dd><code>{{ pageIndexLabel(routeDetail) }}</code></dd></div>
          <div><dt>菜单展示</dt><dd>{{ visibilityLabel(routeDetail.visible) }}</dd></div>
          <div><dt>父级分类</dt><dd>{{ routeDetail.parentName || '顶级节点' }}</dd></div>
          <div><dt>图标 / 排序</dt><dd>{{ routeDetail.icon || '默认图标' }} · {{ routeDetail.sort }}</dd></div>
          <div><dt>记录主键</dt><dd><code>{{ routeDetail.id }}</code></dd></div>
          <div><dt>最后更新</dt><dd>{{ formatTime(routeDetail.updatedAt) }}</dd></div>
        </dl>

        <footer class="route-detail__audit">
          <span>创建于 {{ formatTime(routeDetail.createdAt) }}</span>
          <span>此页面为只读，路由定义不在此处修改。</span>
        </footer>
      </section>
      <div v-else v-loading="detailLoading" class="route-detail__loading">正在读取路由详情…</div>
    </el-drawer>
  </main>
</template>

<style scoped lang="scss">
.route-management-page {
  padding: clamp(1.25rem, 3.5vw, 3.25rem);
}

.route-management-page__header,
.route-management-page__filter,
.route-management-page__filter-fields,
.route-management-page__filter-actions,
.route-management-page__toolbar,
.route-management-page__pagination,
.route-detail__hero,
.route-detail__tags,
.route-detail__audit {
  display: flex;
  align-items: center;
}

.route-management-page__header {
  justify-content: space-between;
  gap: 2rem;
  padding-bottom: 1.6rem;
  border-bottom: 1px solid var(--color-line-subtle);
}

.route-management-page__heading h1,
.route-detail h2 {
  color: var(--color-ink-strong);
  font-family: var(--font-display);
}

.route-management-page__heading h1 {
  margin: 0.45rem 0;
  font-size: clamp(2rem, 4vw, 3rem);
  letter-spacing: -0.055em;
}

.route-management-page__heading > p:last-child {
  max-width: 46rem;
  margin: 0;
  color: var(--color-ink-muted);
  font-size: 0.9rem;
  line-height: 1.75;
}

.route-management-page__count {
  display: grid;
  min-width: 8rem;
  padding-left: 1.75rem;
  color: var(--color-ink-soft);
  border-left: 1px solid var(--color-line-subtle);
  text-align: right;
}

.route-management-page__count strong {
  color: var(--color-ink-strong);
  font-family: var(--font-mono);
  font-size: 2.1rem;
  font-weight: 650;
  letter-spacing: -0.07em;
}

.route-management-page__count span {
  font-size: 0.72rem;
}

.route-management-page__model {
  display: grid;
  grid-template-columns: minmax(12rem, 0.75fr) minmax(0, 2fr);
  gap: 1rem;
  margin-top: 1.3rem;
  padding: 1rem;
  background:
    linear-gradient(95deg, color-mix(in srgb, var(--color-accent-primary) 8%, var(--color-surface-base)), transparent 62%),
    var(--color-surface-base);
}

.route-management-page__model-title strong {
  display: block;
  margin-top: 0.35rem;
  color: var(--color-ink-strong);
  font-size: 0.9rem;
  line-height: 1.5;
}

.route-management-page__model-rules {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  margin: 0;
  border-left: 1px solid var(--color-line-subtle);
}

.route-management-page__model-rules div {
  min-height: 3.5rem;
  padding: 0.25rem 0.9rem;
  border-right: 1px solid var(--color-line-subtle);
}

.route-management-page__model-rules dt {
  color: var(--color-ink-soft);
  font-size: 0.66rem;
  font-weight: 760;
  letter-spacing: 0.08em;
}

.route-management-page__model-rules dd {
  margin: 0.45rem 0 0;
  color: var(--color-ink-strong);
  font-size: 0.78rem;
  line-height: 1.45;
}

.route-management-page__model-rules code {
  font-family: var(--font-mono);
  font-size: 0.72rem;
}

.route-management-page__filter {
  justify-content: space-between;
  gap: 1rem;
  margin-top: 1.1rem;
  padding: 0.85rem;
}

.route-management-page__filter-fields {
  flex: 1;
  gap: 0.65rem;
}

.route-management-page__filter-fields :deep(.el-input) {
  max-width: 22rem;
}

.route-management-page__filter-fields :deep(.el-select) {
  width: 8rem;
}

.route-management-page__filter-actions {
  gap: 0.5rem;
}

.route-management-page__notice {
  margin: 1rem 0 0;
  padding: 0.75rem 0.9rem;
  color: var(--color-accent-danger);
  background: color-mix(in srgb, var(--color-accent-danger) 10%, var(--color-surface-base));
  border: 1px solid color-mix(in srgb, var(--color-accent-danger) 30%, var(--color-line-subtle));
  border-radius: var(--radius-control);
  font-size: 0.84rem;
}

.route-management-page__workspace {
  margin-top: 1.1rem;
  overflow: hidden;
}

.route-management-page__toolbar {
  justify-content: space-between;
  gap: 1rem;
  min-height: 3.5rem;
  padding: 0.75rem 1rem;
  color: var(--color-ink-soft);
  background: linear-gradient(100deg, var(--color-surface-muted), transparent 70%);
  border-bottom: 1px solid var(--color-line-subtle);
  font-family: var(--font-mono);
  font-size: 0.68rem;
}

.route-management-page__table {
  width: 100%;
}

.route-identity {
  display: grid;
  gap: 0.18rem;
}

.route-identity strong {
  color: var(--color-ink-strong);
  font-size: 0.83rem;
  font-weight: 760;
}

.route-identity code,
.route-code,
.route-detail code {
  color: var(--color-ink-soft);
  font-family: var(--font-mono);
  font-size: 0.69rem;
}

.route-identity small {
  color: var(--color-ink-muted);
  font-size: 0.68rem;
}

.route-index {
  display: inline-block;
  max-width: 100%;
  overflow: hidden;
  font-family: var(--font-mono);
  font-size: 0.7rem;
  text-overflow: ellipsis;
  vertical-align: middle;
  white-space: nowrap;
}

.route-index--category {
  color: var(--color-ink-soft);
}

.route-index--ready {
  color: var(--color-accent-primary);
}

.route-index--missing {
  color: var(--color-accent-danger);
}

.route-management-page__pagination {
  justify-content: space-between;
  gap: 1rem;
  min-height: 4.25rem;
  padding: 0.75rem 1rem;
  color: var(--color-ink-soft);
  border-top: 1px solid var(--color-line-subtle);
  font-size: 0.72rem;
}

.route-detail {
  padding: 0 0.35rem 1.25rem;
}

.route-detail__hero {
  justify-content: space-between;
  gap: 1rem;
  padding-bottom: 1.35rem;
  border-bottom: 1px solid var(--color-line-subtle);
}

.route-detail__hero h2 {
  margin: 0.4rem 0 0.25rem;
  font-size: 1.65rem;
}

.route-detail__tags {
  gap: 0.5rem;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.route-detail__explanation {
  margin: 1rem 0;
  padding: 0.85rem 0.9rem;
  color: var(--color-ink-muted);
  background: var(--color-surface-muted);
  border-left: 3px solid var(--color-accent-primary);
  font-size: 0.84rem;
  line-height: 1.75;
}

.route-detail__facts {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  margin: 0;
  border: 1px solid var(--color-line-subtle);
  border-radius: var(--radius-control);
  overflow: hidden;
}

.route-detail__facts div {
  min-height: 5rem;
  padding: 0.85rem;
  background: var(--color-surface-muted);
  border-bottom: 1px solid var(--color-line-subtle);
}

.route-detail__facts div:nth-child(odd) {
  border-right: 1px solid var(--color-line-subtle);
}

.route-detail__facts div:nth-last-child(-n + 2) {
  border-bottom: 0;
}

.route-detail__facts dt {
  color: var(--color-ink-soft);
  font-size: 0.67rem;
  font-weight: 760;
  letter-spacing: 0.08em;
}

.route-detail__facts dd {
  margin: 0.42rem 0 0;
  color: var(--color-ink-strong);
  font-size: 0.82rem;
  line-height: 1.45;
  overflow-wrap: anywhere;
}

.route-detail__audit {
  justify-content: space-between;
  gap: 1rem;
  margin-top: 1rem;
  color: var(--color-ink-soft);
  font-size: 0.72rem;
  line-height: 1.55;
}

.route-detail__loading {
  display: grid;
  min-height: 15rem;
  place-items: center;
  color: var(--color-ink-muted);
}

@include at-most('tablet') {
  .route-management-page__header,
  .route-management-page__filter,
  .route-management-page__toolbar,
  .route-detail__hero,
  .route-detail__audit {
    align-items: stretch;
    flex-direction: column;
  }

  .route-management-page__count {
    min-width: 0;
    padding: 1rem 0 0;
    border-top: 1px solid var(--color-line-subtle);
    border-left: 0;
    text-align: left;
  }

  .route-management-page__model {
    grid-template-columns: 1fr;
  }

  .route-management-page__model-rules {
    border-top: 1px solid var(--color-line-subtle);
    border-left: 0;
  }

  .route-management-page__filter-fields {
    align-items: stretch;
    flex-direction: column;
  }

  .route-management-page__filter-fields :deep(.el-input),
  .route-management-page__filter-fields :deep(.el-select) {
    max-width: none;
    width: 100%;
  }

  .route-management-page__pagination {
    align-items: flex-start;
    flex-direction: column;
  }

  .route-management-page__pagination :deep(.el-pagination) {
    max-width: 100%;
    overflow-x: auto;
  }
}

@include at-most('phone') {
  .route-management-page {
    padding: 1rem;
  }

  .route-management-page__model-rules,
  .route-detail__facts {
    grid-template-columns: 1fr;
  }

  .route-management-page__model-rules div {
    border-right: 0;
    border-bottom: 1px solid var(--color-line-subtle);
  }

  .route-management-page__model-rules div:last-child {
    border-bottom: 0;
  }

  .route-detail__facts div,
  .route-detail__facts div:nth-child(odd),
  .route-detail__facts div:nth-last-child(-n + 2) {
    border-right: 0;
    border-bottom: 1px solid var(--color-line-subtle);
  }

  .route-detail__facts div:last-child {
    border-bottom: 0;
  }
}
</style>
