<!--
  @file index.vue
  @project Pipker Framework
  @module Frontend Permission Point Directory
  @description Provides a read-only tree directory for interface permissions declared by Java PermissionEnum.
  @logic Loads the flat enum catalog, preserves declaration order while building colon-delimited namespace groups, and filters locally while retaining matching ancestor groups.
  @dependencies Vue、Element Plus、permission point API、frontend API contracts
  @index_tags page、permission、permission-point、tree、read-only、rbac、administration
  @author holic512
-->
<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { isAxiosError } from 'axios'
import { FolderOpened, Key } from '@element-plus/icons-vue'
import { ApiBusinessError } from '../../../core/api/contracts'
import type { SystemPermissionPoint } from '../../../core/api/contracts'
import { getPermissionPoints } from './api/permissionManagement'

type PermissionTreeNodeType = 'group' | 'permission'

interface PermissionTreeNode {
  id: string
  nodeType: PermissionTreeNodeType
  label: string
  code: string
  description: string
  groupPath: string
  children: PermissionTreeNode[]
}

const filters = reactive({ keyword: '' })
const appliedKeyword = ref('')
const permissionPoints = ref<SystemPermissionPoint[]>([])
const loading = ref(false)
const loaded = ref(false)
const errorMessage = ref('')
let loadSequence = 0

const permissionTree = computed(() => buildPermissionTree(permissionPoints.value))
const filteredPermissionTree = computed(() => filterPermissionTree(permissionTree.value, appliedKeyword.value))
const visiblePermissionCount = computed(() => countNodes(filteredPermissionTree.value, 'permission'))
const visibleGroupCount = computed(() => countResourceGroups(filteredPermissionTree.value))
const hasSearch = computed(() => appliedKeyword.value.length > 0)
const emptyText = computed(() => {
  if (errorMessage.value) return '加载失败，请重试'
  if (!loaded.value) return '正在加载…'
  if (hasSearch.value) return '没有符合筛选条件的权限点'
  return '暂无权限点定义'
})

onMounted(() => {
  void loadPermissionPoints()
})

async function loadPermissionPoints(): Promise<void> {
  const sequence = ++loadSequence
  loading.value = true
  loaded.value = false
  errorMessage.value = ''
  try {
    const result = await getPermissionPoints()
    if (sequence !== loadSequence) return
    permissionPoints.value = result
    loaded.value = true
  } catch (error) {
    if (sequence === loadSequence) errorMessage.value = readableError(error, '无法读取权限点目录，请重试。')
  } finally {
    if (sequence === loadSequence) loading.value = false
  }
}

function search(): void {
  appliedKeyword.value = filters.keyword.trim().toLocaleLowerCase()
}

function reset(): void {
  filters.keyword = ''
  appliedKeyword.value = ''
}

function buildPermissionTree(points: SystemPermissionPoint[]): PermissionTreeNode[] {
  const roots: PermissionTreeNode[] = []
  const groupsByPath = new Map<string, PermissionTreeNode>()

  for (const point of points) {
    const segments = point.code.split(':').map(segment => segment.trim()).filter(Boolean)
    if (segments.length === 0) continue

    const groupSegments = segments.slice(0, -1)
    let children = roots
    let groupPath = ''

    for (const segment of groupSegments) {
      groupPath = groupPath ? `${groupPath}:${segment}` : segment
      let group = groupsByPath.get(groupPath)
      if (!group) {
        group = {
          id: `group:${groupPath}`,
          nodeType: 'group',
          label: segment,
          code: groupPath,
          description: groupPath,
          groupPath,
          children: [],
        }
        groupsByPath.set(groupPath, group)
        children.push(group)
      }
      children = group.children
    }

    children.push({
      id: `point:${point.code}`,
      nodeType: 'permission',
      label: point.name,
      code: point.code,
      description: point.description,
      groupPath,
      children: [],
    })
  }

  return roots
}

function filterPermissionTree(nodes: PermissionTreeNode[], keyword: string): PermissionTreeNode[] {
  if (!keyword) return nodes

  return nodes.flatMap(node => {
    if (matchesPermissionNode(node, keyword)) return [node]
    const children = filterPermissionTree(node.children, keyword)
    return children.length > 0 ? [{ ...node, children }] : []
  })
}

function matchesPermissionNode(node: PermissionTreeNode, keyword: string): boolean {
  return [node.code, node.label, node.description, node.groupPath]
    .some(value => value.toLocaleLowerCase().includes(keyword))
}

function countNodes(nodes: PermissionTreeNode[], nodeType: PermissionTreeNodeType): number {
  return nodes.reduce((count, node) => count + (node.nodeType === nodeType ? 1 : 0) + countNodes(node.children, nodeType), 0)
}

/** 统计资源命名空间，system 根节点用于承载树结构，不计入目录汇总。 */
function countResourceGroups(nodes: PermissionTreeNode[]): number {
  return nodes.reduce((count, node) => count
    + (node.nodeType === 'group' && node.groupPath.includes(':') ? 1 : 0)
    + countResourceGroups(node.children), 0)
}

function readableError(error: unknown, fallback: string): string {
  if (error instanceof ApiBusinessError) return error.message
  if (isAxiosError(error)) {
    if (error.response?.status === 403) return '没有查看权限，请联系管理员。'
    if (error.response?.status === 401) return '登录已失效，请重新登录。'
    if (error.response?.status === 404) return '权限点接口不存在，请检查服务版本。'
    if (!error.response) return '服务连接失败或请求超时，请重试。'
  }
  return fallback
}
</script>

<template>
  <section class="permission-point-page" aria-label="权限点">
    <form class="permission-filters" aria-label="权限点筛选" @submit.prevent="search">
      <div class="permission-filter-fields">
        <el-input
          v-model="filters.keyword"
          clearable
          aria-label="搜索权限点"
          placeholder="权限编码、名称或说明"
          class="permission-search"
        />
      </div>
      <el-divider direction="vertical" class="permission-filter-divider" />
      <div class="permission-filter-actions">
        <el-button native-type="submit" type="primary" :loading="loading">查询</el-button>
        <el-button :disabled="loading" @click="reset">重置</el-button>
      </div>
    </form>

    <div v-if="errorMessage" class="permission-notice" role="alert">
      <span>{{ errorMessage }}</span>
      <el-button link type="primary" :loading="loading" @click="loadPermissionPoints">重试</el-button>
    </div>

    <div class="permission-workspace ui-panel">
      <div class="permission-table-container">
        <el-table
          v-loading="loading"
          :data="errorMessage ? [] : filteredPermissionTree"
          height="100%"
          :empty-text="emptyText"
          row-key="id"
          :tree-props="{ children: 'children' }"
          :indent="12"
          default-expand-all
        >
          <el-table-column class-name="permission-tree-cell" label="权限点 / 编码" min-width="280">
            <template #default="{ row }">
              <div class="permission-identity">
                <el-icon :class="`permission-identity__icon permission-identity__icon--${row.nodeType}`">
                  <component :is="row.nodeType === 'group' ? FolderOpened : Key" />
                </el-icon>
                <div class="permission-identity__copy">
                  <strong>{{ row.label }}</strong>
                  <code>{{ row.code }}</code>
                </div>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="说明" min-width="330" show-overflow-tooltip>
            <template #default="{ row }">
              <span v-if="row.nodeType === 'group'" class="permission-group-description">分组路径：{{ row.groupPath }}</span>
              <span v-else>{{ row.description }}</span>
            </template>
          </el-table-column>
          <el-table-column label="类型" width="102">
            <template #default="{ row }">
              <el-tag class="permission-tag" size="small" effect="light" :type="row.nodeType === 'group' ? 'info' : 'primary'">
                {{ row.nodeType === 'group' ? '分组' : '接口权限' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="来源" width="168">
            <template #default="{ row }">
              <span class="permission-source">{{ row.nodeType === 'group' ? '由编码生成' : 'Java PermissionEnum' }}</span>
            </template>
          </el-table-column>
        </el-table>
      </div>
      <footer class="permission-tree-summary">
        <span v-if="errorMessage || !loaded">—</span>
        <span v-else-if="hasSearch">匹配 {{ visiblePermissionCount }} 个权限点，{{ visibleGroupCount }} 个分组</span>
        <span v-else>共 {{ visiblePermissionCount }} 个权限点，{{ visibleGroupCount }} 个分组</span>
        <span v-if="loaded && permissionPoints.length > 0">目录支持展开与收起</span>
      </footer>
    </div>
  </section>
</template>

<style scoped lang="scss">
.permission-point-page { display: flex; flex: 1; flex-direction: column; width: 100%; min-height: 0; gap: 0.625rem; }
.permission-filters { display: flex; align-items: center; gap: 0.5rem; flex-wrap: wrap; }
.permission-filter-fields { display: flex; flex: 1 1 0; min-width: 0; align-items: center; gap: 0.5rem; }
.permission-search { flex: 1; min-width: 13rem; }
.permission-filter-actions { display: flex; align-items: center; gap: 0.5rem; }
:deep(.permission-filter-divider.el-divider--vertical) { height: 1.35rem; margin: 0 0.125rem; border-color: var(--color-line-subtle); }
.permission-notice { display: flex; align-items: center; justify-content: space-between; gap: 0.75rem; padding: 0.55rem 0.75rem; border-radius: var(--radius-control); color: var(--color-accent-danger); background: color-mix(in srgb, var(--color-accent-danger) 7%, var(--color-surface-base)); font-size: 0.78rem; }
.permission-workspace { flex: 1; min-height: 0; display: flex; flex-direction: column; overflow: hidden; }
.permission-table-container { flex: 1; min-height: 0; }
.permission-identity { display: flex; flex: 1 1 0; min-width: 0; align-items: center; gap: 0.42rem; white-space: nowrap; }
.permission-identity__icon { flex: 0 0 auto; font-size: 0.9rem; }
.permission-identity__icon--group { color: var(--color-ink-muted); }
.permission-identity__icon--permission { color: var(--color-accent-primary); }
.permission-identity__copy { display: flex; min-width: 0; flex-direction: column; gap: 0.12rem; }
.permission-identity__copy strong { min-width: 0; overflow: hidden; color: var(--color-ink-strong); font-size: 0.72rem; font-weight: 600; line-height: 1.15; text-overflow: ellipsis; }
.permission-identity__copy code { min-width: 0; overflow: hidden; color: var(--color-ink-soft); font-family: var(--font-mono); font-size: 0.62rem; line-height: 1.15; text-overflow: ellipsis; }
.permission-group-description { color: var(--color-ink-muted); }
.permission-source { color: var(--color-ink-soft); font-family: var(--font-mono); font-size: 0.64rem; }
.permission-workspace :deep(.el-table__header-wrapper th.el-table__cell) { padding: 0.35rem 0; color: var(--color-ink-muted); font-size: 0.76rem; font-weight: 650; }
.permission-workspace :deep(.el-table__header-wrapper th.el-table__cell > .cell) { min-height: 23px; display: flex; align-items: center; }
.permission-workspace :deep(.el-table__body-wrapper td.el-table__cell) { padding: calc(0.25rem + 3px) 0; font-size: 0.7rem; }
.permission-workspace :deep(.el-table .cell) { line-height: 1.15; }
.permission-workspace :deep(.el-table__body-wrapper .permission-tree-cell .cell) { display: flex; align-items: center; padding-left: 0.5rem; }
.permission-workspace :deep(.el-table__body-wrapper .permission-tree-cell .el-table__indent) { flex: 0 0 auto; }
.permission-workspace :deep(.el-table__body-wrapper .permission-tree-cell .el-table__expand-icon), .permission-workspace :deep(.el-table__body-wrapper .permission-tree-cell .el-table__placeholder) { flex: 0 0 1.1rem; width: 1.1rem; height: 1.1rem; }
.permission-workspace :deep(.el-table__body-wrapper .permission-tree-cell .el-table__expand-icon) { color: var(--color-accent-primary); }
.permission-workspace :deep(.permission-tag.el-tag) { min-width: 4.1rem; height: 1.18rem; justify-content: center; padding-inline: 0.24rem; border-radius: var(--radius-small); font-size: 0.64rem; font-weight: 650; }
.permission-tree-summary { display: flex; align-items: center; justify-content: space-between; gap: 0.75rem; min-height: 2.25rem; padding: 0.4rem 0.75rem; border-top: 1px solid var(--color-line-subtle); color: var(--color-ink-soft); font-size: 0.68rem; }

@media (max-width: 720px) {
  .permission-filter-fields { flex-basis: 100%; }
  .permission-filter-divider { display: none; }
  .permission-filter-actions { width: 100%; justify-content: flex-end; }
  .permission-workspace { overflow-x: auto; }
  .permission-table-container { min-width: 48rem; }
}
</style>
