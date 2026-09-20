<!--
  @file index.vue
  @project Pipker Framework
  @module 系统路由管理
  @description 全宽树形路由查询与展示配置工作区。
  @logic 加载完整目录—页面树并在前端保留筛选命中节点的祖先上下文；受控表单仅维护展示配置，路由结构字段只读。
  @dependencies Vue、Element Plus、路由管理 API、会话 Store、共享图标目录
  @index_tags route、tree、configuration、authorization
  @author holic512
-->
<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { isAxiosError } from 'axios'
import { useRoute } from 'vue-router'
import { ApiBusinessError } from '../../../core/api/contracts'
import type { SystemRouteDetail, SystemRouteMenuType, SystemRouteStatus, SystemRouteTreeNode } from '../../../core/api/contracts'
import { notification } from '../../../core/notification'
import { useSessionStore } from '../../../stores/session'
import { getDefaultAuthorizedPath, router } from '../../../router'
import { runtimeConfig } from '../../../core/config/runtime'
import { menuIconNames, resolveMenuIcon } from '../../../layouts/model/menuIcons'
import { getRouteDetail, getRouteTree, updateRouteConfiguration, type RouteConfiguration, type RouteTreeQuery } from './api/routeManagement'

const session = useSessionStore()
const currentRoute = useRoute()
const canManage = computed(() => session.permissions.includes('system:route:manage'))
const filters = reactive({ keyword: '', menuType: undefined as SystemRouteMenuType | undefined, visible: undefined as boolean | undefined, status: undefined as SystemRouteStatus | undefined })
const appliedFilters = ref<RouteTreeQuery>({})
const routeTree = ref<SystemRouteTreeNode[]>([])
const loading = ref(false)
const loaded = ref(false)
const errorMessage = ref('')
const authorizationError = ref('')
const refreshingAuthorization = ref(false)
const drawerVisible = ref(false)
const detailLoading = ref(false)
const detailError = ref('')
const saveError = ref('')
const editing = ref(false)
const saving = ref(false)
const detail = ref<SystemRouteDetail | null>(null)
const selectedId = ref('')
const configuration = reactive<RouteConfiguration>({ menuName: '', icon: null, sort: 0, visible: true, status: 'ENABLED' })
let querySequence = 0
let detailSequence = 0
const hasFilters = computed(() => Object.values(appliedFilters.value).some(value => value !== undefined))
const emptyText = computed(() => errorMessage.value ? '加载失败，请重试' : !loaded.value ? '正在加载…' : hasFilters.value ? '没有符合筛选条件的路由' : '暂无路由记录')
const iconOptions = computed(() => configuration.icon && !menuIconNames.includes(configuration.icon) ? [configuration.icon, ...menuIconNames] : menuIconNames)
const filteredRouteTree = computed(() => filterRouteTree(routeTree.value, appliedFilters.value))
const visibleRouteCount = computed(() => countTreeNodes(filteredRouteTree.value))

onMounted(() => { void loadRoutes() })

async function loadRoutes(): Promise<void> {
  const sequence = ++querySequence
  loading.value = true
  errorMessage.value = ''
  try {
    const result = await getRouteTree()
    if (sequence !== querySequence) return
    routeTree.value = result
    loaded.value = true
  } catch (error) {
    if (sequence === querySequence) errorMessage.value = readableError(error, '无法读取路由，请重试。')
  } finally {
    if (sequence === querySequence) loading.value = false
  }
}

function search(): void {
  appliedFilters.value = { keyword: filters.keyword.trim() || undefined, menuType: filters.menuType || undefined, visible: typeof filters.visible === 'boolean' ? filters.visible : undefined, status: filters.status || undefined }
}
function reset(): void {
  Object.assign(filters, { keyword: '', menuType: undefined, visible: undefined, status: undefined })
  search()
}
async function openDetail(row: SystemRouteTreeNode, edit = false): Promise<void> {
  selectedId.value = row.id
  editing.value = edit && canManage.value
  drawerVisible.value = true
  await loadDetail()
}
async function loadDetail(): Promise<void> {
  const sequence = ++detailSequence
  detailLoading.value = true
  detail.value = null
  detailError.value = ''
  saveError.value = ''
  try {
    const result = await getRouteDetail(selectedId.value)
    if (sequence !== detailSequence) return
    detail.value = result
    Object.assign(configuration, { menuName: result.menuName, icon: result.icon, sort: result.sort ?? 0, visible: result.visible, status: result.status })
  } catch (error) {
    if (sequence === detailSequence) detailError.value = readableError(error, '无法读取路由详情。')
  } finally {
    if (sequence === detailSequence) detailLoading.value = false
  }
}
async function refreshAuthorization(): Promise<void> {
  refreshingAuthorization.value = true
  try {
    const activePath = currentRoute.path
    const activeId = currentRoute.meta.routeId
    await session.refreshAuthorization()
    authorizationError.value = ''
    if (typeof activeId === 'string' && !session.routes.some(route => route.id === activeId && router.hasRoute(route.routeName))) {
      await router.replace(getDefaultAuthorizedPath() ?? '/')
    } else {
      await router.replace(currentRoute.fullPath)
      const active = session.routes.find(route => route.path === activePath)
      if (active) document.title = `${active.title} · ${runtimeConfig.appName}`
    }
  } catch {
    authorizationError.value = '配置已保存，导航刷新失败。请重试刷新。'
  } finally {
    refreshingAuthorization.value = false
  }
}
async function save(): Promise<void> {
  if (!detail.value || saving.value || !canManage.value) return
  saveError.value = ''
  const menuName = configuration.menuName.trim()
  if (!menuName || menuName.length > 100 || !Number.isInteger(configuration.sort) || configuration.sort < 0 || configuration.sort > 2147483647) {
    saveError.value = '请输入 1–100 字符的菜单名称和有效的非负整数排序。'
    return
  }
  saving.value = true
  try {
    detail.value = await updateRouteConfiguration(detail.value.id, { ...configuration, menuName, icon: configuration.icon?.trim() || null })
  } catch (error) {
    saveError.value = readableError(error, '保存失败，请重试。')
    saving.value = false
    return
  }
  notification.success('配置已保存')
  drawerVisible.value = false
  await refreshAuthorization()
  await loadRoutes()
  saving.value = false
}
function readableError(error: unknown, fallback: string): string {
  if (error instanceof ApiBusinessError) return error.message
  if (isAxiosError(error)) {
    if (error.response?.status === 403) return '没有此操作的权限。'
    if (error.response?.status === 401) return '登录已失效，请重新登录。'
    if (error.response?.status === 404) return '接口不存在，请检查服务版本和请求地址。'
    if (!error.response) return '服务连接失败或请求超时，请重试。'
  }
  return fallback
}
function formatTime(value: string | null): string {
  if (!value) return '—'
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? value : date.toLocaleString('zh-CN', { hour12: false })
}
function filterRouteTree(nodes: SystemRouteTreeNode[], query: RouteTreeQuery): SystemRouteTreeNode[] {
  return nodes.flatMap(node => {
    const children = filterRouteTree(node.children, query)
    return matchesRouteQuery(node, query) || children.length > 0 ? [{ ...node, children }] : []
  })
}
function matchesRouteQuery(node: SystemRouteTreeNode, query: RouteTreeQuery): boolean {
  const keyword = query.keyword?.toLocaleLowerCase()
  if (keyword && ![node.menuName, node.routePath, node.routeName, node.componentKey]
    .some(value => value?.toLocaleLowerCase().includes(keyword))) return false
  return (query.menuType === undefined || node.menuType === query.menuType)
    && (query.visible === undefined || node.visible === query.visible)
    && (query.status === undefined || node.status === query.status)
}
function countTreeNodes(nodes: SystemRouteTreeNode[]): number {
  return nodes.reduce((count, node) => count + 1 + countTreeNodes(node.children), 0)
}
</script>

<template>
  <section class="route-management-page" aria-label="路由管理">
    <form class="route-filters" aria-label="路由筛选" @submit.prevent="search">
      <div class="route-filter-fields">
        <el-input v-model="filters.keyword" clearable aria-label="搜索路由" placeholder="名称、路径、路由名或页面索引" class="route-search" />
        <el-select v-model="filters.menuType" clearable aria-label="路由类型" placeholder="全部类型"><el-option label="目录" value="DIRECTORY" /><el-option label="页面" value="MENU" /></el-select>
        <el-select v-model="filters.visible" clearable aria-label="菜单展示" placeholder="全部展示"><el-option label="显示" :value="true" /><el-option label="隐藏" :value="false" /></el-select>
        <el-select v-model="filters.status" clearable aria-label="启用状态" placeholder="全部状态"><el-option label="启用" value="ENABLED" /><el-option label="停用" value="DISABLED" /></el-select>
      </div>
      <el-divider direction="vertical" class="route-filter-divider" />
      <div class="route-filter-actions"><el-button native-type="submit" type="primary" :loading="loading">查询</el-button><el-button :disabled="loading" @click="reset">重置</el-button></div>
    </form>
    <div v-if="errorMessage" class="route-notice" role="alert"><span>{{ errorMessage }}</span><el-button link type="primary" :loading="loading" @click="loadRoutes">重试</el-button></div>
    <div v-if="authorizationError" class="route-notice" role="alert"><span>{{ authorizationError }}</span><el-button link type="primary" :loading="refreshingAuthorization" @click="refreshAuthorization">刷新导航</el-button></div>
    <div class="route-workspace ui-panel">
      <div class="route-table-container">
        <el-table v-loading="loading" :data="errorMessage ? [] : filteredRouteTree" height="100%" :empty-text="emptyText" row-key="id" :tree-props="{ children: 'children' }" :indent="12" default-expand-all>
          <el-table-column class-name="route-tree-cell" label="名称 / 路由名" min-width="200"><template #default="{ row }"><div class="route-identity"><el-icon><component :is="resolveMenuIcon(row.icon)" /></el-icon><strong>{{ row.menuName }}</strong><code>{{ row.routeName || '—' }}</code></div></template></el-table-column>
          <el-table-column label="路径" prop="routePath" min-width="190" show-overflow-tooltip />
          <el-table-column label="页面索引" min-width="195" show-overflow-tooltip><template #default="{ row }"><span :class="{ 'route-missing': row.menuType === 'MENU' && !row.componentKey }">{{ row.menuType === 'DIRECTORY' ? '—' : row.componentKey || '未配置' }}</span></template></el-table-column>
          <el-table-column label="类型" width="78"><template #default="{ row }"><el-tag class="route-tag" size="small" effect="light" :type="row.menuType === 'DIRECTORY' ? 'info' : 'primary'">{{ row.menuType === 'DIRECTORY' ? '目录' : '页面' }}</el-tag></template></el-table-column>
          <el-table-column label="展示" width="78"><template #default="{ row }"><el-tag class="route-tag" size="small" effect="light" :type="row.visible ? 'success' : 'warning'">{{ row.visible ? '显示' : '隐藏' }}</el-tag></template></el-table-column>
          <el-table-column label="状态" width="78"><template #default="{ row }"><el-tag class="route-tag" size="small" effect="light" :type="row.status === 'ENABLED' ? 'success' : 'danger'">{{ row.status === 'ENABLED' ? '启用' : '停用' }}</el-tag></template></el-table-column>
          <el-table-column label="排序" prop="sort" width="75" />
          <el-table-column label="操作" fixed="right" :width="canManage ? 126 : 72"><template #default="{ row }"><el-button link type="primary" @click="openDetail(row)">详情</el-button><el-button v-if="canManage" link type="primary" @click="openDetail(row, true)">配置</el-button></template></el-table-column>
        </el-table>
      </div>
      <footer class="route-tree-summary"><span>{{ errorMessage || !loaded ? '—' : hasFilters ? `匹配 ${visibleRouteCount} 个节点` : `共 ${visibleRouteCount} 个节点` }}</span><span v-if="visibleRouteCount > 0">目录支持展开与收起</span></footer>
    </div>
    <el-drawer v-model="drawerVisible" :title="editing ? '路由配置' : '路由详情'" size="min(36rem, 100vw)" :close-on-click-modal="!saving" :close-on-press-escape="!saving" :show-close="!saving">
      <div v-loading="detailLoading" class="route-detail">
        <div v-if="detailError" class="route-notice" role="alert"><span>{{ detailError }}</span><el-button link @click="loadDetail">重试</el-button></div>
        <template v-if="detail">
          <el-form v-if="editing" label-position="top" @submit.prevent="save">
            <el-form-item label="菜单名称" required><el-input v-model="configuration.menuName" maxlength="100" show-word-limit :disabled="saving" /></el-form-item>
            <el-form-item label="图标"><el-select v-model="configuration.icon" clearable filterable placeholder="默认图标" :disabled="saving"><template #prefix><el-icon><component :is="resolveMenuIcon(configuration.icon)" /></el-icon></template><el-option v-for="name in iconOptions" :key="name" :label="name" :value="name"><span class="route-icon-option"><el-icon><component :is="resolveMenuIcon(name)" /></el-icon>{{ name }}</span></el-option></el-select></el-form-item>
            <div class="route-form-grid"><el-form-item label="排序" required><el-input-number v-model="configuration.sort" :min="0" :max="2147483647" :precision="0" controls-position="right" :disabled="saving" /></el-form-item><el-form-item label="菜单展示"><el-switch v-model="configuration.visible" active-text="显示" inactive-text="隐藏" :disabled="saving" /></el-form-item></div>
            <el-form-item label="启用状态"><el-radio-group v-model="configuration.status" :disabled="saving"><el-radio value="ENABLED">启用</el-radio><el-radio value="DISABLED">停用</el-radio></el-radio-group></el-form-item>
            <p class="route-hint">隐藏仅影响导航；停用会阻止普通用户访问{{ detail.menuType === 'DIRECTORY' ? '整个目录下的页面' : '此页面' }}。超级管理员不受影响。</p>
          </el-form>
          <dl class="route-facts">
            <template v-if="!editing"><div><dt>菜单名称</dt><dd>{{ detail.menuName }}</dd></div><div><dt>图标</dt><dd class="route-icon-option"><el-icon><component :is="resolveMenuIcon(detail.icon)" /></el-icon>{{ detail.icon || '默认' }}</dd></div><div><dt>排序</dt><dd>{{ detail.sort }}</dd></div><div><dt>展示 / 状态</dt><dd>{{ detail.visible ? '显示' : '隐藏' }} / {{ detail.status === 'ENABLED' ? '启用' : '停用' }}</dd></div></template>
            <div><dt>类型 / 父级</dt><dd>{{ detail.menuType === 'DIRECTORY' ? '目录' : '页面' }} / {{ detail.parentName || '顶级' }}</dd></div>
            <div><dt>路径</dt><dd>{{ detail.routePath || '—' }}</dd></div><div><dt>路由名</dt><dd>{{ detail.routeName || '—' }}</dd></div><div><dt>页面索引</dt><dd>{{ detail.componentKey || '—' }}</dd></div><div><dt>ID</dt><dd>{{ detail.id }}</dd></div><div><dt>创建时间</dt><dd>{{ formatTime(detail.createdAt) }}</dd></div><div><dt>更新时间</dt><dd>{{ formatTime(detail.updatedAt) }}</dd></div>
          </dl>
          <p v-if="saveError" class="route-notice" role="alert">{{ saveError }}</p>
        </template>
      </div>
      <template #footer><el-button :disabled="saving" @click="drawerVisible = false">{{ editing ? '取消' : '关闭' }}</el-button><el-button v-if="editing" type="primary" :loading="saving" :disabled="!detail || detailLoading || !canManage" @click="save">保存</el-button></template>
    </el-drawer>
  </section>
</template>

<style scoped lang="scss">
.route-management-page { display: flex; flex: 1; flex-direction: column; width: 100%; min-height: 0; gap: 0.625rem; }
.route-hint { margin: 0; color: var(--color-ink-soft); font-size: 0.72rem; line-height: 1.45; }
.route-filters { display: flex; align-items: center; gap: 0.5rem; flex-wrap: wrap; }
.route-filter-fields { display: flex; flex: 1 1 0; min-width: 0; align-items: center; gap: 0.5rem; }
.route-search { flex: 1; min-width: 13rem; }
.route-filter-fields > .el-select { width: 7.5rem; }
.route-filter-actions { display: flex; align-items: center; gap: 0.5rem; }
:deep(.route-filter-divider.el-divider--vertical) { height: 1.35rem; margin: 0 0.125rem; border-color: var(--color-line-subtle); }
.route-workspace { flex: 1; min-height: 0; display: flex; flex-direction: column; overflow: hidden; }
.route-table-container { flex: 1; min-height: 0; }
.route-icon-option { display: flex; align-items: center; gap: 0.5rem; }
.route-identity { display: flex; flex: 1 1 0; min-width: 0; align-items: center; gap: 0.36rem; white-space: nowrap; }
.route-identity > .el-icon { flex: 0 0 auto; color: var(--color-ink-muted); font-size: 0.88rem; }
.route-identity strong { min-width: 0; overflow: hidden; color: var(--color-ink-strong); font-size: 0.72rem; font-weight: 600; line-height: 1.1; text-overflow: ellipsis; }
.route-identity code { min-width: 0; flex: 0 1 auto; overflow: hidden; color: var(--color-ink-soft); font-family: var(--font-mono); font-size: 0.62rem; line-height: 1.1; text-overflow: ellipsis; }
.route-missing { color: var(--color-accent-danger); }
.route-workspace :deep(.el-table__header-wrapper th.el-table__cell) { padding: 0.35rem 0; color: var(--color-ink-muted); font-size: 0.76rem; font-weight: 650; }
.route-workspace :deep(.el-table__header-wrapper th.el-table__cell > .cell) { min-height: 23px; display: flex; align-items: center; }
.route-workspace :deep(.el-table__body-wrapper td.el-table__cell) { padding: calc(0.25rem + 3px) 0; font-size: 0.7rem; }
.route-workspace :deep(.el-table .cell) { line-height: 1.15; }
.route-workspace :deep(.el-table__body-wrapper .route-tree-cell .cell) { display: flex; align-items: center; padding-left: 0.5rem; }
.route-workspace :deep(.el-table__body-wrapper .route-tree-cell .el-table__indent) { flex: 0 0 auto; }
.route-workspace :deep(.el-table__body-wrapper .route-tree-cell .el-table__expand-icon), .route-workspace :deep(.el-table__body-wrapper .route-tree-cell .el-table__placeholder) { flex: 0 0 1.1rem; width: 1.1rem; height: 1.1rem; }
.route-workspace :deep(.el-table__body-wrapper .route-tree-cell .el-table__expand-icon) { color: var(--color-accent-primary); }
.route-workspace :deep(.route-tag.el-tag) { min-width: 3rem; height: 1.18rem; justify-content: center; padding-inline: 0.24rem; border-radius: var(--radius-small); font-size: 0.64rem; font-weight: 650; }
.route-tree-summary { display: flex; align-items: center; justify-content: space-between; gap: 0.75rem; min-height: 2.25rem; padding: 0.4rem 0.75rem; border-top: 1px solid var(--color-line-subtle); font-size: 0.68rem; color: var(--color-ink-soft); }
.route-notice { display: flex; align-items: center; justify-content: space-between; gap: 0.75rem; padding: 0.55rem 0.75rem; border-radius: var(--radius-control); color: var(--color-accent-danger); background: color-mix(in srgb, var(--color-accent-danger) 7%, var(--color-surface-base)); font-size: 0.78rem; }
.route-detail { min-height: 12rem; }
.route-form-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 1rem; }
.route-facts { margin: 1.2rem 0 0; border-top: 1px solid var(--color-line-subtle); }
.route-facts > div { display: grid; grid-template-columns: 6rem minmax(0, 1fr); gap: 1rem; padding: 0.75rem 0; border-bottom: 1px solid var(--color-line-subtle); font-size: 0.82rem; }
.route-facts dt { color: var(--color-ink-soft); }
.route-facts dd { margin: 0; overflow-wrap: anywhere; color: var(--color-ink-strong); }
@include at-most('tablet') { .route-filters { align-items: stretch; flex-direction: column; } .route-filter-fields { width: 100%; flex-wrap: wrap; } .route-search { flex-basis: 100%; } .route-filter-fields > .el-select { flex: 1; min-width: 6rem; } .route-filter-actions { width: 100%; } :deep(.route-filter-divider.el-divider--vertical) { display: none; } .route-tree-summary { padding: 0.45rem 0.65rem; } }
</style>
