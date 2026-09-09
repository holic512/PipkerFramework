<!--
  @file index.vue
  @project Pipker Framework
  @module Frontend Role Management
  @description 提供系统角色的筛选分页、生命周期维护、页面与接口权限工作台、批量操作、详情成员查看和成员密码重置工作台。
  @logic 优先显示可批量处理的角色清单；页面与接口权限工作台在本地筛选树和权限点、只对当前结果执行批量选择，并把历史编码作为只读信息保留；所有雪花 ID 均以字符串传递以避免 JavaScript 精度丢失。
  @dependencies Vue、Element Plus、角色页面权限树、角色管理 API、frontend API contracts
  @index_tags page、rbac、role、route、permission、pagination、batch、password-reset、administration
  @author holic512
-->
<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ApiBusinessError } from '../../../core/api/contracts'
import type {
  PageResult,
  RolePermissionConfiguration,
  RoleRouteConfiguration,
  SystemPermissionPoint,
  SystemMenuNode,
  SystemRoleDetail,
  SystemRoleMember,
  SystemRoleStatus,
  SystemRoleSummary,
} from '../../../core/api/contracts'
import {
  batchDeleteRoles,
  batchUpdateRoleStatus,
  createRole,
  deleteRole,
  getRoleDetail,
  getRoleMemberPage,
  getPermissionPoints,
  getRolePage,
  getRolePermissionConfiguration,
  getRoleRouteConfiguration,
  resetRoleMemberPassword,
  replaceRolePermissionConfiguration,
  replaceRoleRouteConfiguration,
  updateRole,
} from './api/roleManagement'
import RoleMenuTree from './components/RoleMenuTree.vue'

interface RoleFormState {
  roleCode: string
  roleName: string
  description: string
  status: SystemRoleStatus
  sort: number
}

interface PermissionGroup {
  key: string
  points: SystemPermissionPoint[]
}

const EMPTY_ROLE_PAGE: PageResult<SystemRoleSummary> = {
  page: 1,
  pageSize: 10,
  total: 0,
  records: [],
}

const EMPTY_MEMBER_PAGE: PageResult<SystemRoleMember> = {
  page: 1,
  pageSize: 10,
  total: 0,
  records: [],
}

const filters = reactive<{
  keyword: string
  status: SystemRoleStatus | undefined
}>({
  keyword: '',
  status: undefined,
})
const rolePage = ref<PageResult<SystemRoleSummary>>(EMPTY_ROLE_PAGE)
const selectedRoles = ref<SystemRoleSummary[]>([])
const loading = ref(false)
const operating = ref(false)
const errorMessage = ref<string | null>(null)

const roleDialogVisible = ref(false)
const editingRole = ref<SystemRoleSummary | null>(null)
const savingRole = ref(false)
const roleForm = reactive<RoleFormState>(newRoleForm())

const detailDrawerVisible = ref(false)
const roleDetail = ref<SystemRoleDetail | null>(null)
const memberKeyword = ref('')
const memberPage = ref<PageResult<SystemRoleMember>>(EMPTY_MEMBER_PAGE)
const detailLoading = ref(false)
const memberLoading = ref(false)

const passwordDialogVisible = ref(false)
const selectedMember = ref<SystemRoleMember | null>(null)
const resetPassword = ref('')
const resettingPassword = ref(false)

const routePermissionDialogVisible = ref(false)
const routePermissionConfiguration = ref<RoleRouteConfiguration | null>(null)
const selectedRouteIds = ref<string[]>([])
const routePermissionLoading = ref(false)
const routePermissionSaving = ref(false)
const routePermissionKeyword = ref('')
const routePermissionOnlySelected = ref(false)

const interfacePermissionDialogVisible = ref(false)
const permissionPoints = ref<SystemPermissionPoint[]>([])
const rolePermissionConfiguration = ref<RolePermissionConfiguration | null>(null)
const selectedPermissionCodes = ref<string[]>([])
const interfacePermissionLoading = ref(false)
const interfacePermissionSaving = ref(false)
const interfacePermissionKeyword = ref('')
const interfacePermissionOnlySelected = ref(false)

const selectedRoleIds = computed(() => selectedRoles.value.map((role) => role.id))
const roleDialogTitle = computed(() => (editingRole.value ? '编辑角色' : '新建角色'))
const memberTotalLabel = computed(() => `${memberPage.value.total} 位成员`)
const selectedRouteCount = computed(() => selectedRouteIds.value.length)
const canSaveRoutePermissions = computed(() => routePermissionConfiguration.value !== null
  && routePermissionConfiguration.value.status === 'ENABLED'
  && !routePermissionConfiguration.value.allRoutes)
const selectedPermissionCount = computed(() => selectedPermissionCodes.value.length)
const canSaveInterfacePermissions = computed(() => rolePermissionConfiguration.value !== null
  && rolePermissionConfiguration.value.status === 'ENABLED'
  && !rolePermissionConfiguration.value.allPermissions)
const permissionGroups = computed<PermissionGroup[]>(() => {
  const groups = new Map<string, SystemPermissionPoint[]>()
  for (const point of permissionPoints.value) {
    const segments = point.code.split(':')
    const key = segments.length > 1 ? segments.slice(0, -1).join(':') : point.code
    const current = groups.get(key) ?? []
    current.push(point)
    groups.set(key, current)
  }
  return [...groups.entries()].map(([key, points]) => ({ key, points }))
})
const filteredPermissionGroups = computed<PermissionGroup[]>(() => {
  const keyword = interfacePermissionKeyword.value.trim().toLocaleLowerCase()
  const selectedCodes = new Set(selectedPermissionCodes.value)

  return permissionGroups.value.flatMap((group) => {
    const groupMatches = keyword.length > 0 && group.key.toLocaleLowerCase().includes(keyword)
    const points = group.points.filter((point) => {
      const pointMatches = !keyword || groupMatches || [point.name, point.code, point.description]
        .some(value => value.toLocaleLowerCase().includes(keyword))
      return pointMatches && (!interfacePermissionOnlySelected.value || selectedCodes.has(point.code))
    })
    return points.length > 0 ? [{ key: group.key, points }] : []
  })
})
const visiblePermissionCodes = computed(() => filteredPermissionGroups.value.flatMap(group => group.points.map(point => point.code)))
const interfacePermissionTotal = computed(() => permissionPoints.value.length)
const routePermissionTree = computed<SystemMenuNode[]>(() => {
  const configuration = routePermissionConfiguration.value
  if (!configuration) return []
  return filterRoutePermissionTree(
    configuration.routes,
    routePermissionKeyword.value.trim().toLocaleLowerCase(),
    routePermissionOnlySelected.value,
    new Set(selectedRouteIds.value),
  )
})
const visibleRouteIds = computed(() => collectMenuIds(routePermissionTree.value))
const routePermissionTotal = computed(() => countMenuNodes(routePermissionConfiguration.value?.routes ?? []))

onMounted(() => {
  void loadRoles()
})

async function loadRoles(page = rolePage.value.page): Promise<void> {
  loading.value = true
  errorMessage.value = null
  try {
    rolePage.value = await getRolePage({
      page,
      pageSize: rolePage.value.pageSize,
      keyword: filters.keyword.trim() || undefined,
      status: filters.status,
    })
    selectedRoles.value = []
  } catch (error) {
    selectedRoles.value = []
    errorMessage.value = readableError(error, '无法读取角色列表。')
  } finally {
    loading.value = false
  }
}

function applyFilters(): void {
  void loadRoles(1)
}

function clearFilters(): void {
  filters.keyword = ''
  filters.status = undefined
  void loadRoles(1)
}

function changeRolePage(page: number): void {
  void loadRoles(page)
}

function changeRolePageSize(pageSize: number): void {
  rolePage.value = { ...rolePage.value, page: 1, pageSize }
  void loadRoles(1)
}

function setSelectedRoles(roles: SystemRoleSummary[]): void {
  selectedRoles.value = roles
}

function openCreateDialog(): void {
  editingRole.value = null
  Object.assign(roleForm, newRoleForm())
  roleDialogVisible.value = true
}

function openEditDialog(role: SystemRoleSummary): void {
  editingRole.value = role
  Object.assign(roleForm, {
    roleCode: role.roleCode,
    roleName: role.roleName,
    description: role.description ?? '',
    status: role.status,
    sort: role.sort,
  })
  roleDialogVisible.value = true
}

async function saveRole(): Promise<void> {
  const roleName = roleForm.roleName.trim()
  const roleCode = roleForm.roleCode.trim()
  if (!roleName || (!editingRole.value && !roleCode)) {
    ElMessage.warning('请填写角色编码和角色名称。')
    return
  }
  if (roleForm.sort < 0) {
    ElMessage.warning('排序值不能小于 0。')
    return
  }

  savingRole.value = true
  try {
    const payload = {
      roleName,
      description: roleForm.description.trim() || null,
      status: roleForm.status,
      sort: roleForm.sort,
    }
    if (editingRole.value) {
      await updateRole(editingRole.value.id, payload)
      ElMessage.success('角色资料已更新。')
    } else {
      await createRole({ ...payload, roleCode })
      ElMessage.success('角色已创建。')
    }
    roleDialogVisible.value = false
    await loadRoles(editingRole.value ? rolePage.value.page : 1)
  } catch (error) {
    ElMessage.error(readableError(error, '保存角色失败。'))
  } finally {
    savingRole.value = false
  }
}

async function batchChangeStatus(status: SystemRoleStatus): Promise<void> {
  if (selectedRoleIds.value.length === 0) {
    ElMessage.warning('请先选择至少一个普通角色。')
    return
  }
  const action = status === 'ENABLED' ? '启用' : '停用'
  if (!await confirm(`确认${action}已选的 ${selectedRoleIds.value.length} 个角色吗？`, '批量角色状态')) {
    return
  }

  operating.value = true
  try {
    await batchUpdateRoleStatus(selectedRoleIds.value, status)
    ElMessage.success(`已${action} ${selectedRoleIds.value.length} 个角色。`)
    await loadRoles()
  } catch (error) {
    ElMessage.error(readableError(error, `批量${action}失败。`))
  } finally {
    operating.value = false
  }
}

async function batchDelete(): Promise<void> {
  if (selectedRoleIds.value.length === 0) {
    ElMessage.warning('请先选择至少一个普通角色。')
    return
  }
  if (!await confirm(
    `确认删除已选的 ${selectedRoleIds.value.length} 个角色吗？关联的用户、菜单和权限关系会一并移除。`,
    '批量删除角色',
    'warning',
  )) {
    return
  }

  operating.value = true
  try {
    await batchDeleteRoles(selectedRoleIds.value)
    ElMessage.success('角色及其关联关系已删除。')
    await loadRoles(rolePage.value.records.length === selectedRoleIds.value.length && rolePage.value.page > 1
      ? rolePage.value.page - 1
      : rolePage.value.page)
  } catch (error) {
    ElMessage.error(readableError(error, '批量删除失败。'))
  } finally {
    operating.value = false
  }
}

async function removeRole(role: SystemRoleSummary): Promise<void> {
  if (!await confirm(
    `确认删除“${role.roleName}”吗？它的用户、菜单和权限关联会一并移除。`,
    '删除角色',
    'warning',
  )) {
    return
  }

  operating.value = true
  try {
    await deleteRole(role.id)
    ElMessage.success('角色已删除。')
    await loadRoles(rolePage.value.records.length === 1 && rolePage.value.page > 1
      ? rolePage.value.page - 1
      : rolePage.value.page)
  } catch (error) {
    ElMessage.error(readableError(error, '删除角色失败。'))
  } finally {
    operating.value = false
  }
}

function selectedPermissionCountInGroup(group: PermissionGroup): number {
  const selectedCodes = new Set(selectedPermissionCodes.value)
  return group.points.filter(point => selectedCodes.has(point.code)).length
}

function isPermissionGroupChecked(group: PermissionGroup): boolean {
  return group.points.length > 0 && selectedPermissionCountInGroup(group) === group.points.length
}

function isPermissionGroupIndeterminate(group: PermissionGroup): boolean {
  const selectedCount = selectedPermissionCountInGroup(group)
  return selectedCount > 0 && selectedCount < group.points.length
}

function orderedPermissionCodes(codes: Iterable<string>): string[] {
  const selectedCodes = new Set(codes)
  return permissionPoints.value
    .map(point => point.code)
    .filter(code => selectedCodes.has(code))
}

function setPermissionGroupSelection(group: PermissionGroup, value: boolean | string | number): void {
  if (!canSaveInterfacePermissions.value) return

  const nextCodes = new Set(selectedPermissionCodes.value)
  const checked = value === true || value === 'true' || value === 1
  for (const point of group.points) {
    if (checked) {
      nextCodes.add(point.code)
    } else {
      nextCodes.delete(point.code)
    }
  }
  selectedPermissionCodes.value = orderedPermissionCodes(nextCodes)
}

function selectVisiblePermissions(): void {
  if (!canSaveInterfacePermissions.value) return
  const nextCodes = new Set(selectedPermissionCodes.value)
  visiblePermissionCodes.value.forEach(code => nextCodes.add(code))
  selectedPermissionCodes.value = orderedPermissionCodes(nextCodes)
}

function clearVisiblePermissions(): void {
  if (!canSaveInterfacePermissions.value) return
  const visibleCodes = new Set(visiblePermissionCodes.value)
  selectedPermissionCodes.value = orderedPermissionCodes(
    selectedPermissionCodes.value.filter(code => !visibleCodes.has(code)),
  )
}

function countMenuNodes(nodes: SystemMenuNode[]): number {
  return nodes.reduce((count, node) => count
    + (node.type === 'MENU' ? 1 : 0)
    + countMenuNodes(node.children), 0)
}

function collectMenuIds(nodes: SystemMenuNode[]): string[] {
  return nodes.flatMap(node => node.type === 'MENU'
    ? [node.id]
    : collectMenuIds(node.children))
}

function matchesRouteNode(node: SystemMenuNode, keyword: string): boolean {
  if (!keyword) return true
  return [node.name, node.path, node.routeName, node.componentKey]
    .filter((value): value is string => Boolean(value))
    .some(value => value.toLocaleLowerCase().includes(keyword))
}

function filterRoutePermissionTree(
  nodes: SystemMenuNode[],
  keyword: string,
  onlySelected: boolean,
  selectedIds: Set<string>,
): SystemMenuNode[] {
  return nodes.flatMap((node) => {
    if (node.type === 'MENU') {
      if (onlySelected && !selectedIds.has(node.id)) return []
      return matchesRouteNode(node, keyword) ? [node] : []
    }

    const children = keyword && matchesRouteNode(node, keyword) && !onlySelected
      ? node.children
      : filterRoutePermissionTree(node.children, keyword, onlySelected, selectedIds)
    return children.length > 0 ? [{ ...node, children }] : []
  })
}

function orderedRouteIds(ids: Iterable<string>): string[] {
  const configuration = routePermissionConfiguration.value
  if (!configuration) return [...ids]
  const selectedIds = new Set(ids)
  return collectMenuIds(configuration.routes).filter(id => selectedIds.has(id))
}

function selectVisibleRoutes(): void {
  if (!canSaveRoutePermissions.value) return
  const nextIds = new Set(selectedRouteIds.value)
  visibleRouteIds.value.forEach(id => nextIds.add(id))
  selectedRouteIds.value = orderedRouteIds(nextIds)
}

function clearVisibleRoutes(): void {
  if (!canSaveRoutePermissions.value) return
  const visibleIds = new Set(visibleRouteIds.value)
  selectedRouteIds.value = orderedRouteIds(
    selectedRouteIds.value.filter(id => !visibleIds.has(id)),
  )
}

async function openRoutePermissionDialog(role: SystemRoleSummary): Promise<void> {
  routePermissionDialogVisible.value = true
  routePermissionConfiguration.value = null
  selectedRouteIds.value = []
  routePermissionKeyword.value = ''
  routePermissionOnlySelected.value = false
  routePermissionLoading.value = true
  try {
    const configuration = await getRoleRouteConfiguration(role.id)
    routePermissionConfiguration.value = configuration
    selectedRouteIds.value = [...configuration.routeIds]
  } catch (error) {
    ElMessage.error(readableError(error, '无法读取角色页面权限。'))
  } finally {
    routePermissionLoading.value = false
  }
}

async function saveRoutePermissions(): Promise<void> {
  const configuration = routePermissionConfiguration.value
  if (!configuration || !canSaveRoutePermissions.value) {
    return
  }
  if (!await confirm(
    `确认将“${configuration.roleName}”的页面访问权限更新为 ${selectedRouteCount.value} 项吗？`,
    '更新页面权限',
    'warning',
  )) {
    return
  }

  routePermissionSaving.value = true
  try {
    const result = await replaceRoleRouteConfiguration(configuration.roleId, selectedRouteIds.value)
    routePermissionConfiguration.value = {
      ...configuration,
      routeIds: [...result.routeIds],
    }
    selectedRouteIds.value = [...result.routeIds]
    ElMessage.success(`已保存 ${result.routeIds.length} 项页面权限；受影响账户将使用新的路由与导航授权。`)
  } catch (error) {
    ElMessage.error(readableError(error, '保存角色页面权限失败。'))
  } finally {
    routePermissionSaving.value = false
  }
}

async function openInterfacePermissionDialog(role: SystemRoleSummary): Promise<void> {
  interfacePermissionDialogVisible.value = true
  rolePermissionConfiguration.value = null
  permissionPoints.value = []
  selectedPermissionCodes.value = []
  interfacePermissionKeyword.value = ''
  interfacePermissionOnlySelected.value = false
  interfacePermissionLoading.value = true
  try {
    const [points, configuration] = await Promise.all([
      getPermissionPoints(),
      getRolePermissionConfiguration(role.id),
    ])
    permissionPoints.value = points
    rolePermissionConfiguration.value = configuration
    selectedPermissionCodes.value = [...configuration.permissionCodes]
  } catch (error) {
    ElMessage.error(readableError(error, '无法读取角色接口权限。'))
  } finally {
    interfacePermissionLoading.value = false
  }
}

async function saveInterfacePermissions(): Promise<void> {
  const configuration = rolePermissionConfiguration.value
  if (!configuration || !canSaveInterfacePermissions.value) {
    return
  }
  if (!await confirm(
    `确认将“${configuration.roleName}”的接口权限更新为 ${selectedPermissionCount.value} 项吗？`,
    '更新接口权限',
    'warning',
  )) {
    return
  }

  interfacePermissionSaving.value = true
  try {
    const result = await replaceRolePermissionConfiguration(
      configuration.roleId,
      selectedPermissionCodes.value,
    )
    rolePermissionConfiguration.value = {
      ...configuration,
      permissionCodes: [...result.permissionCodes],
    }
    selectedPermissionCodes.value = [...result.permissionCodes]
    ElMessage.success(`已保存 ${result.permissionCodes.length} 项接口权限；受影响账户将使用新的后端访问权限。`)
  } catch (error) {
    ElMessage.error(readableError(error, '保存角色接口权限失败。'))
  } finally {
    interfacePermissionSaving.value = false
  }
}

async function openDetail(role: SystemRoleSummary): Promise<void> {
  detailDrawerVisible.value = true
  roleDetail.value = null
  memberPage.value = EMPTY_MEMBER_PAGE
  memberKeyword.value = ''
  detailLoading.value = true
  try {
    roleDetail.value = await getRoleDetail(role.id)
    await loadMembers(1)
  } catch (error) {
    ElMessage.error(readableError(error, '无法读取角色详情。'))
  } finally {
    detailLoading.value = false
  }
}

async function loadMembers(page = memberPage.value.page): Promise<void> {
  if (!roleDetail.value) {
    return
  }
  memberLoading.value = true
  try {
    memberPage.value = await getRoleMemberPage(roleDetail.value.id, {
      page,
      pageSize: memberPage.value.pageSize,
      keyword: memberKeyword.value.trim() || undefined,
    })
  } catch (error) {
    ElMessage.error(readableError(error, '无法读取角色成员。'))
  } finally {
    memberLoading.value = false
  }
}

function changeMemberPage(page: number): void {
  void loadMembers(page)
}

function changeMemberPageSize(pageSize: number): void {
  memberPage.value = { ...memberPage.value, page: 1, pageSize }
  void loadMembers(1)
}

function openPasswordDialog(member: SystemRoleMember): void {
  selectedMember.value = member
  resetPassword.value = ''
  passwordDialogVisible.value = true
}

async function submitPasswordReset(): Promise<void> {
  if (!roleDetail.value || !selectedMember.value) {
    return
  }
  if (resetPassword.value.length < 8) {
    ElMessage.warning('新密码至少需要 8 个字符。')
    return
  }
  if (!await confirm(`确认重置账户“${selectedMember.value.username}”的密码吗？`, '重置密码', 'warning')) {
    return
  }

  resettingPassword.value = true
  try {
    await resetRoleMemberPassword(roleDetail.value.id, selectedMember.value.id, resetPassword.value)
    passwordDialogVisible.value = false
    resetPassword.value = ''
    ElMessage.success('密码已重置。请通过安全渠道通知账户持有人。')
  } catch (error) {
    ElMessage.error(readableError(error, '重置密码失败。'))
  } finally {
    resettingPassword.value = false
  }
}

function statusTagType(status: SystemRoleStatus): 'success' | 'info' {
  return status === 'ENABLED' ? 'success' : 'info'
}

function statusLabel(status: SystemRoleStatus): string {
  return status === 'ENABLED' ? '启用' : '停用'
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

async function confirm(
  message: string,
  title: string,
  type: 'warning' | 'info' = 'info',
): Promise<boolean> {
  try {
    await ElMessageBox.confirm(message, title, {
      confirmButtonText: '确认',
      cancelButtonText: '取消',
      type,
      closeOnClickModal: false,
    })
    return true
  } catch {
    return false
  }
}

function newRoleForm(): RoleFormState {
  return {
    roleCode: '',
    roleName: '',
    description: '',
    status: 'ENABLED',
    sort: 100,
  }
}
</script>

<template>
  <section class="role-management-page" aria-label="角色管理">
    <form class="role-filters" aria-label="角色筛选" @submit.prevent="applyFilters">
      <div class="role-filter-fields">
        <el-input
          v-model="filters.keyword"
          clearable
          aria-label="搜索角色"
          class="role-search"
          placeholder="角色编码或名称"
        />
        <el-select v-model="filters.status" clearable aria-label="角色状态" placeholder="全部状态">
          <el-option label="启用" value="ENABLED" />
          <el-option label="停用" value="DISABLED" />
        </el-select>
      </div>
      <el-divider direction="vertical" class="role-filter-divider" />
      <div class="role-filter-actions">
        <div class="role-query-actions">
          <el-button :loading="loading" native-type="submit" type="primary">查询</el-button>
          <el-button :disabled="loading" @click="clearFilters">重置</el-button>
        </div>
        <el-divider direction="vertical" class="role-filter-divider" />
        <el-button type="primary" plain @click="openCreateDialog">新建角色</el-button>
        <el-divider direction="vertical" class="role-filter-divider" />
        <div class="role-batch-actions" aria-label="批量操作">
          <el-button :disabled="selectedRoleIds.length === 0" :loading="operating" @click="batchChangeStatus('ENABLED')">
            批量启用
          </el-button>
          <el-button :disabled="selectedRoleIds.length === 0" :loading="operating" @click="batchChangeStatus('DISABLED')">
            批量停用
          </el-button>
          <el-button :disabled="selectedRoleIds.length === 0" :loading="operating" type="danger" plain @click="batchDelete">
            批量删除
          </el-button>
        </div>
      </div>
    </form>

    <div v-if="errorMessage" class="role-notice" role="alert">
      <span>{{ errorMessage }}</span>
      <el-button link type="primary" :loading="loading" @click="loadRoles()">重试</el-button>
    </div>

    <section class="role-workspace ui-panel">
      <div class="role-table-container">
        <el-table
          v-loading="loading"
          :data="errorMessage ? [] : rolePage.records"
          height="100%"
          empty-text="没有符合当前筛选条件的角色"
          @selection-change="setSelectedRoles"
        >
          <el-table-column type="selection" width="48" />
          <el-table-column label="角色 / 编码" min-width="210">
            <template #default="{ row }: { row: SystemRoleSummary }">
              <div class="role-identity"><strong>{{ row.roleName }}</strong><code>{{ row.roleCode }}</code></div>
            </template>
          </el-table-column>
          <el-table-column label="说明" min-width="220" show-overflow-tooltip>
            <template #default="{ row }: { row: SystemRoleSummary }">{{ row.description || '—' }}</template>
          </el-table-column>
          <el-table-column label="状态" width="78">
            <template #default="{ row }: { row: SystemRoleSummary }">
              <el-tag class="role-tag" size="small" effect="light" :type="statusTagType(row.status)">{{ statusLabel(row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="排序" width="75" prop="sort" />
          <el-table-column label="最近更新" min-width="154">
            <template #default="{ row }: { row: SystemRoleSummary }">{{ formatTime(row.updatedAt) }}</template>
          </el-table-column>
          <el-table-column label="操作" fixed="right" min-width="342">
            <template #default="{ row }: { row: SystemRoleSummary }">
              <div class="role-row-actions">
                <el-button link type="primary" @click="openDetail(row)">详情</el-button>
                <el-button link type="primary" @click="openRoutePermissionDialog(row)">页面权限</el-button>
                <el-button link type="primary" @click="openInterfacePermissionDialog(row)">接口权限</el-button>
                <el-button link type="primary" @click="openEditDialog(row)">编辑</el-button>
                <el-button link type="danger" @click="removeRole(row)">删除</el-button>
              </div>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <footer class="role-page-summary">
        <span>{{ errorMessage ? '—' : `共 ${rolePage.total} 个角色${selectedRoleIds.length > 0 ? `，已选择 ${selectedRoleIds.length} 项` : ''}` }}</span>
        <el-pagination
          background
          :current-page="rolePage.page"
          :page-size="rolePage.pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :total="rolePage.total"
          layout="sizes, prev, pager, next"
          @current-change="changeRolePage"
          @size-change="changeRolePageSize"
        />
      </footer>
    </section>

    <el-dialog v-model="roleDialogVisible" :close-on-click-modal="false" :title="roleDialogTitle" width="min(32rem, calc(100vw - 2rem))">
      <el-form class="role-form" label-position="top">
        <el-form-item label="角色编码" required>
          <el-input
            v-model="roleForm.roleCode"
            :disabled="editingRole !== null"
            maxlength="64"
            placeholder="例如 CONTENT_REVIEWER"
          />
          <p v-if="editingRole" class="role-form__hint">角色编码创建后不可修改，以保证现有授权语义稳定。</p>
        </el-form-item>
        <el-form-item label="角色名称" required>
          <el-input v-model="roleForm.roleName" maxlength="100" placeholder="例如 内容审核员" />
        </el-form-item>
        <el-form-item label="功能说明">
          <el-input v-model="roleForm.description" :rows="3" maxlength="500" show-word-limit type="textarea" />
        </el-form-item>
        <div class="role-form__pair">
          <el-form-item label="状态" required>
            <el-select v-model="roleForm.status">
              <el-option label="启用" value="ENABLED" />
              <el-option label="停用" value="DISABLED" />
            </el-select>
          </el-form-item>
          <el-form-item label="排序" required>
            <el-input-number v-model="roleForm.sort" :min="0" :step="10" controls-position="right" />
          </el-form-item>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="roleDialogVisible = false">取消</el-button>
        <el-button :loading="savingRole" type="primary" @click="saveRole">保存角色</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="detailDrawerVisible" :size="'min(46rem, 100vw)'" title="角色详情">
      <section v-if="roleDetail" v-loading="detailLoading" class="role-detail">
        <div class="role-detail__hero">
          <div>
            <p class="ui-eyebrow">ROLE PROFILE</p>
            <h2>{{ roleDetail.roleName }}</h2>
            <code>{{ roleDetail.roleCode }}</code>
          </div>
          <el-tag :type="statusTagType(roleDetail.status)" effect="plain">{{ statusLabel(roleDetail.status) }}</el-tag>
        </div>
        <dl class="role-detail__facts">
          <div><dt>功能说明</dt><dd>{{ roleDetail.description || '未填写说明' }}</dd></div>
          <div><dt>排序值</dt><dd>{{ roleDetail.sort }}</dd></div>
          <div><dt>成员数量</dt><dd>{{ roleDetail.memberCount }}</dd></div>
          <div><dt>最后更新</dt><dd>{{ formatTime(roleDetail.updatedAt) }}</dd></div>
        </dl>

        <section class="role-detail__members">
          <div class="role-detail__section-heading">
            <div>
              <p class="ui-eyebrow">ROLE MEMBERS</p>
              <h3>成员账户 <small>{{ memberTotalLabel }}</small></h3>
            </div>
            <el-input
              v-model="memberKeyword"
              clearable
              placeholder="筛选成员"
              @clear="loadMembers(1)"
              @keyup.enter="loadMembers(1)"
            >
              <template #append><el-button :loading="memberLoading" @click="loadMembers(1)">查询</el-button></template>
            </el-input>
          </div>
          <el-table v-loading="memberLoading" :data="memberPage.records" empty-text="当前角色还没有成员">
            <el-table-column label="账户" min-width="150">
              <template #default="{ row }: { row: SystemRoleMember }">
                <div class="role-cell"><strong>{{ row.nickname || row.username }}</strong><code>{{ row.username }}</code></div>
              </template>
            </el-table-column>
            <el-table-column label="状态" width="86">
              <template #default="{ row }: { row: SystemRoleMember }">
                <el-tag :type="statusTagType(row.status)" effect="plain">{{ statusLabel(row.status) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="最近登录" min-width="154">
              <template #default="{ row }: { row: SystemRoleMember }">{{ formatTime(row.lastLoginTime) }}</template>
            </el-table-column>
            <el-table-column label="操作" width="110" fixed="right">
              <template #default="{ row }: { row: SystemRoleMember }">
                <el-button link type="warning" @click="openPasswordDialog(row)">重置密码</el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-pagination
            class="role-detail__member-pagination"
            background
            :current-page="memberPage.page"
            :page-size="memberPage.pageSize"
            :page-sizes="[10, 20, 50, 100]"
            :total="memberPage.total"
            layout="sizes, prev, pager, next"
            @current-change="changeMemberPage"
            @size-change="changeMemberPageSize"
          />
        </section>
      </section>
      <div v-else v-loading="detailLoading" class="role-detail__loading">正在读取角色详情…</div>
    </el-drawer>

    <el-dialog
      v-model="routePermissionDialogVisible"
      :close-on-click-modal="false"
      class="role-permission-dialog role-route-permission-dialog"
      title="页面权限"
      top="6vh"
      width="min(72rem, calc(100vw - 2rem))"
    >
      <section v-if="routePermissionConfiguration" v-loading="routePermissionLoading" class="role-permission-workbench role-route-permission">
        <header class="role-permission-identity">
          <div class="role-permission-identity__copy">
            <strong>{{ routePermissionConfiguration.roleName }}</strong>
            <code>{{ routePermissionConfiguration.roleCode }}</code>
          </div>
          <div class="role-permission-count" aria-label="已选页面权限数量">
            <strong>{{ selectedRouteCount }}</strong>
            <span>/ {{ routePermissionTotal }}</span>
          </div>
        </header>

        <div class="role-permission-toolbar">
          <el-input
            v-model="routePermissionKeyword"
            clearable
            aria-label="搜索页面权限"
            class="role-permission-toolbar__search"
            placeholder="搜索页面名称、路径或路由名"
          />
          <el-checkbox v-model="routePermissionOnlySelected" aria-label="只看已选页面">只看已选</el-checkbox>
          <div class="role-permission-toolbar__actions">
            <el-button
              :disabled="!canSaveRoutePermissions || visibleRouteIds.length === 0 || routePermissionSaving"
              text
              @click="selectVisibleRoutes"
            >
              全选当前
            </el-button>
            <el-button
              :disabled="!canSaveRoutePermissions || visibleRouteIds.length === 0 || routePermissionSaving"
              text
              @click="clearVisibleRoutes"
            >
              清空当前
            </el-button>
          </div>
        </div>

        <p v-if="routePermissionConfiguration.allRoutes" class="role-route-permission__notice">
          自动拥有全部启用页面 · 不可编辑
        </p>
        <p v-else-if="routePermissionConfiguration.status === 'DISABLED'" class="role-route-permission__notice">
          角色已停用 · 仅查看
        </p>

        <div class="role-permission-scroll" :class="{ 'role-permission-scroll--readonly': !canSaveRoutePermissions }">
          <RoleMenuTree
            v-if="routePermissionTree.length > 0"
            :disabled="!canSaveRoutePermissions || routePermissionSaving"
            :menus="routePermissionTree"
            v-model="selectedRouteIds"
          />
          <div v-else class="role-permission-empty">
            {{ routePermissionOnlySelected ? '当前没有已选页面' : routePermissionKeyword ? '没有匹配的页面' : '暂无可配置页面' }}
          </div>
        </div>
      </section>
      <div v-else v-loading="routePermissionLoading" class="role-route-permission__loading">正在读取角色页面权限…</div>
      <template #footer>
        <span class="role-permission-footer__summary">已选 {{ selectedRouteCount }} / {{ routePermissionTotal }}</span>
        <div class="role-permission-footer__actions">
          <el-button @click="routePermissionDialogVisible = false">关闭</el-button>
          <el-button
            :disabled="!canSaveRoutePermissions"
            :loading="routePermissionSaving"
            type="primary"
            @click="saveRoutePermissions"
          >
            保存更改（{{ selectedRouteCount }}）
          </el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog
      v-model="interfacePermissionDialogVisible"
      :close-on-click-modal="false"
      class="role-permission-dialog role-api-permission-dialog"
      title="接口权限"
      top="6vh"
      width="min(72rem, calc(100vw - 2rem))"
    >
      <section v-if="rolePermissionConfiguration" v-loading="interfacePermissionLoading" class="role-permission-workbench role-api-permission">
        <header class="role-permission-identity">
          <div class="role-permission-identity__copy">
            <strong>{{ rolePermissionConfiguration.roleName }}</strong>
            <code>{{ rolePermissionConfiguration.roleCode }}</code>
          </div>
          <div class="role-permission-count" aria-label="已选接口权限数量">
            <strong>{{ selectedPermissionCount }}</strong>
            <span>/ {{ interfacePermissionTotal }}</span>
          </div>
        </header>

        <div class="role-permission-toolbar">
          <el-input
            v-model="interfacePermissionKeyword"
            clearable
            aria-label="搜索接口权限"
            class="role-permission-toolbar__search"
            placeholder="搜索权限名称、编码或说明"
          />
          <el-checkbox v-model="interfacePermissionOnlySelected" aria-label="只看已选接口权限">只看已选</el-checkbox>
          <div class="role-permission-toolbar__actions">
            <el-button
              :disabled="!canSaveInterfacePermissions || visiblePermissionCodes.length === 0 || interfacePermissionSaving"
              text
              @click="selectVisiblePermissions"
            >
              全选当前
            </el-button>
            <el-button
              :disabled="!canSaveInterfacePermissions || visiblePermissionCodes.length === 0 || interfacePermissionSaving"
              text
              @click="clearVisiblePermissions"
            >
              清空当前
            </el-button>
          </div>
        </div>

        <p v-if="rolePermissionConfiguration.allPermissions" class="role-api-permission__notice">
          自动拥有全部当前权限 · 不可编辑
        </p>
        <p v-else-if="rolePermissionConfiguration.status === 'DISABLED'" class="role-api-permission__notice">
          角色已停用 · 仅查看
        </p>

        <div class="role-permission-scroll" :class="{ 'role-permission-scroll--readonly': !canSaveInterfacePermissions }">
          <div v-if="filteredPermissionGroups.length > 0" class="role-api-permission__groups">
            <section v-for="group in filteredPermissionGroups" :key="group.key" class="role-api-permission__group">
              <header class="role-api-permission__group-header">
                <el-checkbox
                  :aria-label="`选择 ${group.key} 分组`"
                  :disabled="!canSaveInterfacePermissions || interfacePermissionSaving"
                  :indeterminate="isPermissionGroupIndeterminate(group)"
                  :model-value="isPermissionGroupChecked(group)"
                  @change="setPermissionGroupSelection(group, $event)"
                >
                  <code>{{ group.key }}</code>
                </el-checkbox>
                <span>{{ selectedPermissionCountInGroup(group) }} / {{ group.points.length }}</span>
              </header>
              <el-checkbox-group v-model="selectedPermissionCodes" class="role-api-permission__options">
                <el-checkbox
                  v-for="point in group.points"
                  :key="point.code"
                  :disabled="!canSaveInterfacePermissions || interfacePermissionSaving"
                  :label="point.code"
                  class="role-api-permission__option"
                >
                  <el-tooltip :content="point.description" placement="top-start" :show-after="180">
                    <span
                      class="role-api-permission__option-copy"
                      :aria-label="`${point.name}：${point.description}`"
                      tabindex="0"
                    >
                      <strong>{{ point.name }}</strong>
                      <code>{{ point.code }}</code>
                    </span>
                  </el-tooltip>
                </el-checkbox>
              </el-checkbox-group>
            </section>
          </div>
          <div v-else class="role-permission-empty">
            {{ interfacePermissionOnlySelected ? '当前没有已选权限' : interfacePermissionKeyword ? '没有匹配的权限' : '暂无可配置权限' }}
          </div>

          <details v-if="rolePermissionConfiguration.historicalPermissionCodes.length > 0" class="role-api-permission__historical">
            <summary>历史失效权限 {{ rolePermissionConfiguration.historicalPermissionCodes.length }} 项</summary>
            <p>仅供识别，不参与当前权限计数，也不会随本次保存提交。</p>
            <div class="role-api-permission__historical-codes">
              <code v-for="code in rolePermissionConfiguration.historicalPermissionCodes" :key="code">{{ code }}</code>
            </div>
          </details>
        </div>
      </section>
      <div v-else v-loading="interfacePermissionLoading" class="role-api-permission__loading">正在读取角色接口权限…</div>
      <template #footer>
        <span class="role-permission-footer__summary">已选 {{ selectedPermissionCount }} / {{ interfacePermissionTotal }}</span>
        <div class="role-permission-footer__actions">
          <el-button @click="interfacePermissionDialogVisible = false">关闭</el-button>
          <el-button
            :disabled="!canSaveInterfacePermissions"
            :loading="interfacePermissionSaving"
            type="primary"
            @click="saveInterfacePermissions"
          >
            保存更改（{{ selectedPermissionCount }}）
          </el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog v-model="passwordDialogVisible" :close-on-click-modal="false" title="重置成员密码" width="min(28rem, calc(100vw - 2rem))">
      <p class="password-reset-copy">
        将为 <strong>{{ selectedMember?.username }}</strong> 设置新的登录密码。密码不会显示或以明文形式保存。
      </p>
      <el-input v-model="resetPassword" maxlength="128" placeholder="请输入至少 8 个字符的新密码" show-password type="password" />
      <template #footer>
        <el-button @click="passwordDialogVisible = false">取消</el-button>
        <el-button :loading="resettingPassword" type="warning" @click="submitPasswordReset">确认重置</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<style scoped lang="scss">
.role-management-page {
  display: flex;
  flex: 1;
  flex-direction: column;
  width: 100%;
  min-height: 0;
  gap: 0.625rem;
}

.role-filters,
.role-filter-fields,
.role-filter-actions,
.role-query-actions,
.role-batch-actions,
.role-detail__hero,
.role-detail__section-heading {
  display: flex;
  align-items: center;
}

.role-detail h2,
.role-detail h3,
.role-route-permission h2 {
  color: var(--color-ink-strong);
  font-family: var(--font-display);
}

.role-filters {
  flex-wrap: wrap;
  gap: 0.5rem;
}

.role-filter-fields {
  flex: 1 1 0;
  min-width: 0;
  gap: 0.5rem;
}

.role-search {
  flex: 1;
  min-width: 13rem;
}

.role-filter-fields > .el-select {
  width: 7.5rem;
}

.role-filter-actions {
  flex-wrap: wrap;
  gap: 0.5rem;
}

.role-query-actions,
.role-batch-actions {
  gap: 0.5rem;
}

:deep(.role-filter-divider.el-divider--vertical) {
  height: 1.35rem;
  margin: 0 0.125rem;
  border-color: var(--color-line-subtle);
}

.role-notice {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
  padding: 0.55rem 0.75rem;
  color: var(--color-accent-danger);
  background: color-mix(in srgb, var(--color-accent-danger) 7%, var(--color-surface-base));
  border-radius: var(--radius-control);
  font-size: 0.78rem;
}

.role-workspace {
  display: flex;
  flex: 1;
  flex-direction: column;
  min-height: 0;
  overflow: hidden;
}

.role-table-container {
  flex: 1;
  min-height: 0;
}

.role-cell {
  display: grid;
  gap: 0.22rem;
}

.role-cell strong {
  color: var(--color-ink-strong);
  font-size: 0.83rem;
  font-weight: 760;
}

.role-cell code,
.role-detail code {
  color: var(--color-ink-soft);
  font-family: var(--font-mono);
  font-size: 0.69rem;
}

.role-identity {
  display: flex;
  flex: 1 1 0;
  min-width: 0;
  align-items: center;
  gap: 0.36rem;
  white-space: nowrap;
}

.role-identity strong,
.role-identity code {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
}

.role-identity strong {
  color: var(--color-ink-strong);
  font-size: 0.72rem;
  font-weight: 600;
  line-height: 1.1;
}

.role-identity code {
  flex: 0 1 auto;
  color: var(--color-ink-soft);
  font-family: var(--font-mono);
  font-size: 0.62rem;
  line-height: 1.1;
}

.role-workspace :deep(.el-table__header-wrapper th.el-table__cell) {
  padding: 0.35rem 0;
  color: var(--color-ink-muted);
  font-size: 0.76rem;
  font-weight: 650;
}

.role-workspace :deep(.el-table__body-wrapper td.el-table__cell) {
  padding: calc(0.25rem + 3px) 0;
  font-size: 0.7rem;
}

.role-workspace :deep(.el-table .cell) {
  line-height: 1.15;
}

.role-workspace :deep(.role-tag.el-tag) {
  min-width: 3rem;
  height: 1.18rem;
  justify-content: center;
  padding-inline: 0.24rem;
  border-radius: var(--radius-small);
  font-size: 0.64rem;
  font-weight: 650;
}

.role-row-actions {
  display: flex;
  gap: 0.35rem;
  white-space: nowrap;
}

.role-page-summary {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
  min-height: 2.25rem;
  padding: 0.4rem 0.75rem;
  color: var(--color-ink-soft);
  border-top: 1px solid var(--color-line-subtle);
  font-size: 0.68rem;
}

.role-form__pair {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1rem;
}

.role-form__pair :deep(.el-select),
.role-form__pair :deep(.el-input-number) {
  width: 100%;
}

.role-form__hint {
  margin: 0.4rem 0 0;
  color: var(--color-ink-soft);
  font-size: 0.75rem;
  line-height: 1.5;
}

.role-detail {
  padding: 0 0.35rem 1.25rem;
}

.role-detail__hero {
  justify-content: space-between;
  gap: 1rem;
  padding-bottom: 1.35rem;
  border-bottom: 1px solid var(--color-line-subtle);
}

.role-detail__hero h2 {
  margin: 0.4rem 0 0.25rem;
  font-size: 1.65rem;
}

.role-detail__facts {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0;
  margin: 1rem 0 1.75rem;
  border: 1px solid var(--color-line-subtle);
  border-radius: var(--radius-control);
  overflow: hidden;
}

.role-detail__facts div {
  min-height: 5rem;
  padding: 0.85rem;
  background: var(--color-surface-muted);
  border-bottom: 1px solid var(--color-line-subtle);
}

.role-detail__facts div:nth-child(odd) {
  border-right: 1px solid var(--color-line-subtle);
}

.role-detail__facts div:nth-last-child(-n + 2) {
  border-bottom: 0;
}

.role-detail__facts dt {
  color: var(--color-ink-soft);
  font-size: 0.67rem;
  font-weight: 760;
  letter-spacing: 0.08em;
}

.role-detail__facts dd {
  margin: 0.42rem 0 0;
  color: var(--color-ink-strong);
  font-size: 0.82rem;
  line-height: 1.45;
}

.role-detail__section-heading {
  justify-content: space-between;
  gap: 1rem;
  margin-bottom: 0.85rem;
}

.role-detail__section-heading h3 {
  margin: 0.35rem 0 0;
  font-size: 1.15rem;
}

.role-detail__section-heading h3 small {
  margin-left: 0.35rem;
  color: var(--color-ink-soft);
  font-family: var(--font-mono);
  font-size: 0.66rem;
  font-weight: 500;
}

.role-detail__section-heading :deep(.el-input) {
  max-width: 18rem;
}

.role-detail__member-pagination {
  justify-content: flex-end;
  margin-top: 0.85rem;
}

.role-detail__loading {
  min-height: 15rem;
  display: grid;
  place-items: center;
  color: var(--color-ink-muted);
}

.role-permission-dialog :deep(.el-dialog) {
  display: flex;
  flex-direction: column;
  height: min(78vh, 52rem);
  margin: 0 auto;
}

:deep(.el-dialog.role-permission-dialog) {
  display: flex;
  flex-direction: column;
  height: min(78vh, 52rem);
  margin: 0 auto;
}

.role-permission-dialog :deep(.el-dialog__header) {
  flex: 0 0 auto;
  margin-right: 0;
  padding: 1rem 1.25rem 0.85rem;
  border-bottom: 1px solid var(--color-line-subtle);
}

.role-permission-dialog :deep(.el-dialog__title) {
  color: var(--color-ink-strong);
  font-size: 1rem;
  font-weight: 760;
}

.role-permission-dialog :deep(.el-dialog__headerbtn) {
  top: 0.85rem;
  right: 1rem;
}

.role-permission-dialog :deep(.el-dialog__body) {
  display: flex;
  flex: 1 1 auto;
  min-height: 0;
  padding: 1rem 1.25rem 0;
  overflow: hidden;
}

.role-permission-dialog :deep(.el-dialog__footer) {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  flex: 0 0 auto;
  padding: 0.8rem 1.25rem;
  border-top: 1px solid var(--color-line-subtle);
}

.role-permission-workbench {
  display: flex;
  flex: 1 1 auto;
  min-height: 0;
  flex-direction: column;
  width: 100%;
}

.role-permission-identity {
  display: flex;
  flex: 0 0 auto;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  padding-bottom: 0.85rem;
  border-bottom: 1px solid var(--color-line-subtle);
}

.role-permission-identity__copy {
  display: flex;
  min-width: 0;
  align-items: baseline;
  gap: 0.65rem;
}

.role-permission-identity__copy strong {
  min-width: 0;
  overflow: hidden;
  color: var(--color-ink-strong);
  font-family: var(--font-display);
  font-size: 1.25rem;
  font-weight: 760;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.role-permission-identity__copy code {
  min-width: 0;
  overflow: hidden;
  color: var(--color-ink-soft);
  font-family: var(--font-mono);
  font-size: 0.68rem;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.role-permission-count {
  display: flex;
  flex: 0 0 auto;
  align-items: baseline;
  color: var(--color-ink-soft);
  font-family: var(--font-mono);
  font-size: 0.72rem;
  white-space: nowrap;
}

.role-permission-count strong {
  color: var(--color-accent-primary);
  font-size: 1.5rem;
  line-height: 1;
}

.role-permission-toolbar {
  display: flex;
  flex: 0 0 auto;
  align-items: center;
  gap: 0.65rem;
  padding: 0.8rem 0;
  border-bottom: 1px solid var(--color-line-subtle);
}

.role-permission-toolbar__search {
  flex: 1 1 20rem;
  min-width: 12rem;
}

.role-permission-toolbar > :deep(.el-checkbox) {
  flex: 0 0 auto;
  margin-right: 0.1rem;
}

.role-permission-toolbar__actions {
  display: flex;
  flex: 0 0 auto;
  align-items: center;
  gap: 0.2rem;
}

.role-permission-toolbar__actions :deep(.el-button) {
  margin: 0;
  padding-right: 0.4rem;
  padding-left: 0.4rem;
  font-size: 0.73rem;
}

.role-route-permission__notice,
.role-api-permission__notice {
  flex: 0 0 auto;
  margin: 0.75rem 0 0;
  padding: 0.55rem 0.7rem;
  color: var(--color-ink-muted);
  background: var(--color-surface-muted);
  border: 1px solid var(--color-line-subtle);
  border-radius: var(--radius-control);
  font-size: 0.72rem;
  line-height: 1.45;
}

.role-permission-scroll {
  flex: 1 1 auto;
  min-height: 0;
  padding: 0.85rem 0.25rem 0.75rem 0;
  overflow: auto;
}

.role-permission-scroll--readonly {
  opacity: 0.82;
}

.role-permission-empty {
  min-height: 12rem;
  display: grid;
  place-items: center;
  color: var(--color-ink-soft);
  font-size: 0.78rem;
}

.role-permission-footer__summary {
  color: var(--color-ink-soft);
  font-family: var(--font-mono);
  font-size: 0.7rem;
}

.role-permission-footer__actions {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.role-route-permission__loading,
.role-api-permission__loading {
  min-height: 20rem;
  display: grid;
  place-items: center;
  color: var(--color-ink-muted);
}

.role-api-permission__groups {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  align-items: start;
  gap: 0.75rem;
}

.role-api-permission__group {
  min-width: 0;
  overflow: hidden;
  background: var(--color-surface-base);
  border: 1px solid var(--color-line-subtle);
  border-radius: var(--radius-control);
}

.role-api-permission__group-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
  min-width: 0;
  padding: 0.5rem 0.65rem;
  background: var(--color-surface-muted);
  border-bottom: 1px solid var(--color-line-subtle);
}

.role-api-permission__group-header :deep(.el-checkbox) {
  min-width: 0;
  margin: 0;
}

.role-api-permission__group-header :deep(.el-checkbox__label) {
  min-width: 0;
  overflow: hidden;
  padding-left: 0.42rem;
  color: var(--color-ink-strong);
  font-family: var(--font-mono);
  font-size: 0.7rem;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.role-api-permission__group-header > span {
  flex: 0 0 auto;
  color: var(--color-ink-soft);
  font-family: var(--font-mono);
  font-size: 0.64rem;
}

.role-api-permission__group-header code,
.role-api-permission__option-copy code,
.role-api-permission__historical-codes code {
  color: inherit;
  font-family: var(--font-mono);
  font-size: inherit;
}

.role-api-permission__options {
  display: flex;
  flex-direction: column;
}

.role-api-permission__option {
  display: flex;
  align-items: flex-start;
  width: 100%;
  min-width: 0;
  min-height: 3rem;
  margin: 0;
  padding: 0.55rem 0.65rem;
  white-space: normal;
  border-bottom: 1px solid var(--color-line-subtle);
}

.role-api-permission__option:last-child {
  border-bottom: 0;
}

.role-api-permission__option:hover,
.role-api-permission__option.is-checked {
  background: color-mix(in srgb, var(--color-accent-primary) 6%, var(--color-surface-base));
}

.role-api-permission__option :deep(.el-checkbox__label) {
  display: block;
  min-width: 0;
  padding-left: 0.45rem;
}

.role-api-permission__option-copy {
  display: grid;
  min-width: 0;
  gap: 0.12rem;
  cursor: help;
  border-radius: var(--radius-small);
}

.role-api-permission__option-copy:focus-visible {
  outline: 2px solid var(--color-focus-ring);
  outline-offset: 0.15rem;
}

.role-api-permission__option-copy strong,
.role-api-permission__option-copy code {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.role-api-permission__option-copy strong {
  color: var(--color-ink-strong);
  font-size: 0.76rem;
  font-weight: 700;
  line-height: 1.2;
}

.role-api-permission__option-copy code {
  color: var(--color-ink-soft);
  font-size: 0.63rem;
  line-height: 1.2;
}

.role-api-permission__historical {
  display: grid;
  gap: 0.55rem;
  margin-top: 0.75rem;
  padding: 0.65rem 0.75rem;
  background: color-mix(in srgb, var(--color-ink-soft) 7%, var(--color-surface-base));
  border: 1px dashed var(--color-line-subtle);
  border-radius: var(--radius-control);
}

.role-api-permission__historical summary {
  color: var(--color-ink-muted);
  cursor: pointer;
  font-size: 0.72rem;
  font-weight: 700;
}

.role-api-permission__historical > p {
  margin: 0;
  color: var(--color-ink-muted);
  font-size: 0.7rem;
  line-height: 1.55;
}

.role-api-permission__historical-codes {
  display: flex;
  flex-wrap: wrap;
  gap: 0.35rem;
}

.role-api-permission__historical-codes code {
  padding: 0.25rem 0.35rem;
  color: var(--color-ink-muted);
  background: var(--color-surface-muted);
  border-radius: var(--radius-small);
}

.password-reset-copy {
  margin: 0 0 1rem;
  color: var(--color-ink-muted);
  font-size: 0.88rem;
  line-height: 1.7;
}

.password-reset-copy strong {
  color: var(--color-ink-strong);
}

@include at-most('tablet') {
  .role-filters,
  .role-detail__section-heading,
  .role-permission-identity {
    align-items: stretch;
    flex-direction: column;
  }

  .role-permission-toolbar {
    flex-wrap: wrap;
  }

  .role-permission-toolbar__search {
    flex-basis: 100%;
  }

  .role-permission-toolbar__actions {
    margin-left: auto;
  }

  .role-api-permission__groups {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .role-search {
    flex-basis: 100%;
  }

  .role-filter-fields {
    width: 100%;
    flex-wrap: wrap;
  }

  .role-filter-fields > .el-select {
    flex: 1;
    min-width: 6rem;
  }

  .role-filter-actions {
    width: 100%;
  }

  :deep(.role-filter-divider.el-divider--vertical) {
    display: none;
  }

  .role-detail__section-heading :deep(.el-input) {
    max-width: none;
    width: 100%;
  }

  .role-page-summary {
    padding: 0.45rem 0.65rem;
  }

  .role-page-summary :deep(.el-pagination),
  .role-detail__member-pagination {
    max-width: 100%;
    overflow-x: auto;
  }
}

@include at-most('phone') {
  .role-permission-dialog :deep(.el-dialog),
  :deep(.el-dialog.role-permission-dialog) {
    width: calc(100vw - 1rem) !important;
    height: calc(100vh - 1rem);
    top: 0.5rem !important;
    margin-top: 0.5rem;
  }

  .role-permission-dialog :deep(.el-dialog__header) {
    padding: 0.8rem 0.9rem 0.7rem;
  }

  .role-permission-dialog :deep(.el-dialog__body) {
    padding: 0.8rem 0.9rem 0;
  }

  .role-permission-dialog :deep(.el-dialog__footer) {
    display: grid;
    gap: 0.65rem;
    padding: 0.7rem 0.9rem;
  }

  .role-permission-identity {
    align-items: flex-start;
  }

  .role-permission-identity__copy {
    align-items: flex-start;
    flex-direction: column;
    gap: 0.18rem;
  }

  .role-permission-toolbar {
    align-items: stretch;
    flex-direction: column;
  }

  .role-permission-toolbar__search {
    width: 100%;
  }

  .role-permission-toolbar__actions {
    justify-content: flex-end;
    margin-left: 0;
  }

  .role-permission-footer__actions {
    justify-content: flex-end;
  }

  .role-api-permission__groups {
    grid-template-columns: 1fr;
  }

  .role-batch-actions {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
  }

  .role-batch-actions :deep(.el-button) {
    width: 100%;
    margin: 0;
  }

  .role-form__pair,
  .role-detail__facts {
    grid-template-columns: 1fr;
  }

  .role-detail__facts div,
  .role-detail__facts div:nth-child(odd),
  .role-detail__facts div:nth-last-child(-n + 2) {
    border-right: 0;
    border-bottom: 1px solid var(--color-line-subtle);
  }

  .role-detail__facts div:last-child {
    border-bottom: 0;
  }

}
</style>
