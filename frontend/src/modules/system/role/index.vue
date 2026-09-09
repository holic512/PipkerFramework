<!--
  @file index.vue
  @project Pipker Framework
  @module Frontend Role Management
  @description 提供系统角色的筛选分页、生命周期维护、页面与接口权限弹窗、批量操作、详情成员查看和成员密码重置工作台。
  @logic 优先显示可批量处理的角色清单；页面权限弹窗复用已落库路由树，接口权限弹窗只读取 Java 枚举权限点并把历史编码作为只读提示；所有雪花 ID 均以字符串传递以避免 JavaScript 精度丢失。
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

const interfacePermissionDialogVisible = ref(false)
const permissionPoints = ref<SystemPermissionPoint[]>([])
const rolePermissionConfiguration = ref<RolePermissionConfiguration | null>(null)
const selectedPermissionCodes = ref<string[]>([])
const interfacePermissionLoading = ref(false)
const interfacePermissionSaving = ref(false)

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
const permissionGroups = computed(() => {
  const groups = new Map<string, SystemPermissionPoint[]>()
  for (const point of permissionPoints.value) {
    const segments = point.code.split('-')
    const key = segments.length > 1 ? segments.slice(0, -1).join('-') : point.code
    const current = groups.get(key) ?? []
    current.push(point)
    groups.set(key, current)
  }
  return [...groups.entries()].map(([key, points]) => ({ key, points }))
})

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

async function openRoutePermissionDialog(role: SystemRoleSummary): Promise<void> {
  routePermissionDialogVisible.value = true
  routePermissionConfiguration.value = null
  selectedRouteIds.value = []
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
      <div class="role-filter-actions">
        <el-button :loading="loading" native-type="submit" type="primary">查询</el-button>
        <el-button :disabled="loading" @click="clearFilters">重置</el-button>
        <el-button type="primary" plain @click="openCreateDialog">新建角色</el-button>
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
      class="role-route-permission-dialog"
      title="页面权限"
      top="6vh"
      width="min(52rem, calc(100vw - 2rem))"
    >
      <section v-if="routePermissionConfiguration" v-loading="routePermissionLoading" class="role-route-permission">
        <header class="role-route-permission__header">
          <div>
            <p class="ui-eyebrow">ROLE / PAGE ACCESS</p>
            <h2>{{ routePermissionConfiguration.roleName }}</h2>
            <code>{{ routePermissionConfiguration.roleCode }}</code>
          </div>
          <div class="role-route-permission__metric">
            <strong>{{ selectedRouteCount }}</strong>
            <span>项页面权限</span>
          </div>
        </header>

        <p v-if="routePermissionConfiguration.allRoutes" class="role-route-permission__notice">
          SUPER_ADMIN 自动拥有全部启用页面。此角色不能手工收窄页面访问范围。
        </p>
        <p v-else-if="routePermissionConfiguration.status === 'DISABLED'" class="role-route-permission__notice">
          角色当前已停用，可查看已保存页面权限；启用角色后才可更新。
        </p>
        <div v-else class="role-route-permission__guide">
          <span><i class="role-route-permission__guide-dot" />勾选页面决定该角色可直接访问的路由。</span>
          <span><i class="role-route-permission__guide-dot role-route-permission__guide-dot--hidden" />“隐藏菜单”不出现在侧栏或页签，但已授权账户仍可通过 URL 访问。</span>
          <span><i class="role-route-permission__guide-dot role-route-permission__guide-dot--directory" />分类目录仅组织层级，不是可授予页面。</span>
        </div>

        <div class="role-route-permission__tree" :class="{ 'role-route-permission__tree--readonly': !canSaveRoutePermissions }">
          <RoleMenuTree
            :disabled="!canSaveRoutePermissions || routePermissionSaving"
            :menus="routePermissionConfiguration.routes"
            v-model="selectedRouteIds"
          />
        </div>
      </section>
      <div v-else v-loading="routePermissionLoading" class="role-route-permission__loading">正在读取角色页面权限…</div>
      <template #footer>
        <el-button @click="routePermissionDialogVisible = false">关闭</el-button>
        <el-button
          :disabled="!canSaveRoutePermissions"
          :loading="routePermissionSaving"
          type="primary"
          @click="saveRoutePermissions"
        >
          保存 {{ selectedRouteCount }} 项
        </el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="interfacePermissionDialogVisible"
      :close-on-click-modal="false"
      class="role-api-permission-dialog"
      title="接口权限"
      top="6vh"
      width="min(52rem, calc(100vw - 2rem))"
    >
      <section v-if="rolePermissionConfiguration" v-loading="interfacePermissionLoading" class="role-api-permission">
        <header class="role-api-permission__header">
          <div>
            <p class="ui-eyebrow">ROLE / API ACCESS</p>
            <h2>{{ rolePermissionConfiguration.roleName }}</h2>
            <code>{{ rolePermissionConfiguration.roleCode }}</code>
          </div>
          <div class="role-api-permission__metric">
            <strong>{{ selectedPermissionCount }}</strong>
            <span>项接口权限</span>
          </div>
        </header>

        <p v-if="rolePermissionConfiguration.allPermissions" class="role-api-permission__notice">
          SUPER_ADMIN 自动拥有当前 Java 权限枚举中的全部权限。新增枚举权限会自动生效，不能手工收窄。
        </p>
        <p v-else-if="rolePermissionConfiguration.status === 'DISABLED'" class="role-api-permission__notice">
          角色当前已停用，可查看已保存接口权限；启用角色后才可更新。
        </p>
        <p v-else class="role-api-permission__guide">
          权限点由后端 Java 枚举统一定义。勾选决定接口实际访问权，前端展示与按钮隐藏不替代后端校验。
        </p>

        <div class="role-api-permission__groups" :class="{ 'role-api-permission__groups--readonly': !canSaveInterfacePermissions }">
          <section v-for="group in permissionGroups" :key="group.key" class="role-api-permission__group">
            <header>
              <strong>{{ group.key }}</strong>
              <code>{{ group.key }}-*</code>
            </header>
            <el-checkbox-group v-model="selectedPermissionCodes" class="role-api-permission__options">
              <el-checkbox
                v-for="point in group.points"
                :key="point.code"
                :disabled="!canSaveInterfacePermissions || interfacePermissionSaving"
                :label="point.code"
                class="role-api-permission__option"
              >
                <span class="role-api-permission__option-copy">
                  <strong>{{ point.name }}</strong>
                  <code>{{ point.code }}</code>
                  <small>{{ point.description }}</small>
                </span>
              </el-checkbox>
            </el-checkbox-group>
          </section>
        </div>

        <section
          v-if="rolePermissionConfiguration.historicalPermissionCodes.length > 0"
          class="role-api-permission__historical"
        >
          <div>
            <p class="ui-eyebrow">HISTORICAL / INACTIVE</p>
            <h3>历史失效权限</h3>
          </div>
          <p>这些编码仍保留在数据库中，但当前 Java 枚举已不再定义，因此不会授予任何接口访问权，也不会随本次保存提交。</p>
          <div class="role-api-permission__historical-codes">
            <code v-for="code in rolePermissionConfiguration.historicalPermissionCodes" :key="code">{{ code }}</code>
          </div>
        </section>
      </section>
      <div v-else v-loading="interfacePermissionLoading" class="role-api-permission__loading">正在读取角色接口权限…</div>
      <template #footer>
        <el-button @click="interfacePermissionDialogVisible = false">关闭</el-button>
        <el-button
          :disabled="!canSaveInterfacePermissions"
          :loading="interfacePermissionSaving"
          type="primary"
          @click="saveInterfacePermissions"
        >
          保存 {{ selectedPermissionCount }} 项
        </el-button>
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
.role-filter-actions,
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

.role-search {
  flex: 1;
  min-width: 13rem;
}

.role-filters > .el-select {
  width: 7.5rem;
}

.role-filter-actions {
  flex-wrap: wrap;
  gap: 0.5rem;
  margin-left: auto;
}

.role-batch-actions {
  gap: 0.5rem;
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

.role-route-permission {
  min-height: 20rem;
}

.role-route-permission__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 1rem;
  padding: 0.15rem 0 1rem;
  border-bottom: 1px solid var(--color-line-subtle);
}

.role-route-permission__header h2 {
  margin: 0.38rem 0 0.2rem;
  font-size: 1.45rem;
}

.role-route-permission__header code {
  color: var(--color-ink-soft);
  font-family: var(--font-mono);
  font-size: 0.68rem;
}

.role-route-permission__metric {
  display: grid;
  min-width: 6.9rem;
  padding: 0.65rem 0.8rem;
  text-align: right;
  background: linear-gradient(135deg, color-mix(in srgb, var(--color-accent-primary) 14%, var(--color-surface-base)), var(--color-surface-base));
  border: 1px solid color-mix(in srgb, var(--color-accent-primary) 28%, var(--color-line-subtle));
  border-radius: var(--radius-control);
}

.role-route-permission__metric strong {
  color: var(--color-accent-primary);
  font-family: var(--font-mono);
  font-size: 1.35rem;
  line-height: 1;
}

.role-route-permission__metric span {
  margin-top: 0.24rem;
  color: var(--color-ink-soft);
  font-size: 0.65rem;
}

.role-route-permission__notice,
.role-route-permission__guide {
  margin: 1rem 0;
  padding: 0.75rem 0.85rem;
  border-radius: var(--radius-control);
  font-size: 0.78rem;
  line-height: 1.7;
}

.role-route-permission__notice {
  color: var(--color-ink-muted);
  background: var(--color-surface-muted);
  border: 1px solid var(--color-line-subtle);
}

.role-route-permission__guide {
  display: grid;
  gap: 0.38rem;
  color: var(--color-ink-muted);
  background: color-mix(in srgb, var(--color-accent-primary) 5%, var(--color-surface-base));
  border-left: 3px solid var(--color-accent-primary);
}

.role-route-permission__guide span {
  display: flex;
  align-items: flex-start;
  gap: 0.46rem;
}

.role-route-permission__guide-dot {
  width: 0.48rem;
  height: 0.48rem;
  flex: 0 0 auto;
  margin-top: 0.42rem;
  background: var(--color-accent-primary);
  border-radius: 50%;
}

.role-route-permission__guide-dot--hidden {
  background: var(--color-ink-soft);
}

.role-route-permission__guide-dot--directory {
  border-radius: 0.08rem;
  background: var(--color-accent-success);
}

.role-route-permission__tree {
  max-height: min(43vh, 31rem);
  padding: 0.55rem 0.25rem 0.8rem 0;
  overflow: auto;
}

.role-route-permission__tree--readonly {
  opacity: 0.76;
}

.role-route-permission__loading {
  min-height: 20rem;
  display: grid;
  place-items: center;
  color: var(--color-ink-muted);
}

.role-api-permission {
  min-height: 20rem;
}

.role-api-permission__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 1rem;
  padding: 0.15rem 0 1rem;
  border-bottom: 1px solid var(--color-line-subtle);
}

.role-api-permission__header h2,
.role-api-permission__historical h3 {
  margin: 0.38rem 0 0.2rem;
  color: var(--color-ink-strong);
  font-family: var(--font-display);
}

.role-api-permission__header h2 {
  font-size: 1.45rem;
}

.role-api-permission__header code,
.role-api-permission__group header code,
.role-api-permission__option-copy code,
.role-api-permission__historical-codes code {
  color: var(--color-ink-soft);
  font-family: var(--font-mono);
  font-size: 0.68rem;
}

.role-api-permission__metric {
  display: grid;
  min-width: 6.9rem;
  padding: 0.65rem 0.8rem;
  text-align: right;
  background: linear-gradient(135deg, color-mix(in srgb, var(--color-accent-primary) 14%, var(--color-surface-base)), var(--color-surface-base));
  border: 1px solid color-mix(in srgb, var(--color-accent-primary) 28%, var(--color-line-subtle));
  border-radius: var(--radius-control);
}

.role-api-permission__metric strong {
  color: var(--color-accent-primary);
  font-family: var(--font-mono);
  font-size: 1.35rem;
  line-height: 1;
}

.role-api-permission__metric span {
  margin-top: 0.24rem;
  color: var(--color-ink-soft);
  font-size: 0.65rem;
}

.role-api-permission__notice,
.role-api-permission__guide {
  margin: 1rem 0;
  padding: 0.75rem 0.85rem;
  border-radius: var(--radius-control);
  font-size: 0.78rem;
  line-height: 1.7;
}

.role-api-permission__notice {
  color: var(--color-ink-muted);
  background: var(--color-surface-muted);
  border: 1px solid var(--color-line-subtle);
}

.role-api-permission__guide {
  color: var(--color-ink-muted);
  background: color-mix(in srgb, var(--color-accent-primary) 5%, var(--color-surface-base));
  border-left: 3px solid var(--color-accent-primary);
}

.role-api-permission__groups {
  display: grid;
  gap: 0.75rem;
  max-height: min(39vh, 27rem);
  padding: 0.1rem 0.25rem 0.8rem 0;
  overflow: auto;
}

.role-api-permission__groups--readonly {
  opacity: 0.76;
}

.role-api-permission__group {
  overflow: hidden;
  border: 1px solid var(--color-line-subtle);
  border-radius: var(--radius-control);
}

.role-api-permission__group header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  padding: 0.58rem 0.75rem;
  background: var(--color-surface-muted);
  border-bottom: 1px solid var(--color-line-subtle);
}

.role-api-permission__group header strong {
  color: var(--color-ink-strong);
  font-family: var(--font-mono);
  font-size: 0.72rem;
  letter-spacing: 0.04em;
}

.role-api-permission__options {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.role-api-permission__option {
  display: flex;
  align-items: flex-start;
  min-width: 0;
  margin: 0;
  padding: 0.75rem;
  white-space: normal;
  border-right: 1px solid var(--color-line-subtle);
  border-bottom: 1px solid var(--color-line-subtle);
}

.role-api-permission__option:nth-child(even) {
  border-right: 0;
}

.role-api-permission__option:nth-last-child(-n + 2) {
  border-bottom: 0;
}

.role-api-permission__option :deep(.el-checkbox__label) {
  min-width: 0;
  padding-left: 0.5rem;
}

.role-api-permission__option-copy {
  display: grid;
  gap: 0.18rem;
}

.role-api-permission__option-copy strong {
  color: var(--color-ink-strong);
  font-size: 0.79rem;
}

.role-api-permission__option-copy small {
  color: var(--color-ink-muted);
  font-size: 0.68rem;
  line-height: 1.55;
}

.role-api-permission__historical {
  display: grid;
  gap: 0.6rem;
  margin-top: 0.4rem;
  padding: 0.9rem;
  background: color-mix(in srgb, var(--color-ink-soft) 7%, var(--color-surface-base));
  border: 1px dashed var(--color-line-subtle);
  border-radius: var(--radius-control);
}

.role-api-permission__historical h3 {
  font-size: 1rem;
}

.role-api-permission__historical > p {
  margin: 0;
  color: var(--color-ink-muted);
  font-size: 0.74rem;
  line-height: 1.65;
}

.role-api-permission__historical-codes {
  display: flex;
  flex-wrap: wrap;
  gap: 0.4rem;
}

.role-api-permission__historical-codes code {
  padding: 0.32rem 0.42rem;
  color: var(--color-ink-muted);
  background: var(--color-surface-muted);
  border-radius: 0.25rem;
}

.role-api-permission__loading {
  min-height: 20rem;
  display: grid;
  place-items: center;
  color: var(--color-ink-muted);
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
  .role-route-permission__header,
  .role-api-permission__header {
    align-items: stretch;
    flex-direction: column;
  }

  .role-search {
    flex-basis: 100%;
  }

  .role-filters > .el-select {
    flex: 1;
    min-width: 6rem;
  }

  .role-filter-actions {
    margin-left: 0;
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

  .role-route-permission__metric {
    text-align: left;
  }

  .role-api-permission__metric {
    text-align: left;
  }

  .role-api-permission__options {
    grid-template-columns: 1fr;
  }

  .role-api-permission__option,
  .role-api-permission__option:nth-child(even),
  .role-api-permission__option:nth-last-child(-n + 2) {
    border-right: 0;
    border-bottom: 1px solid var(--color-line-subtle);
  }

  .role-api-permission__option:last-child {
    border-bottom: 0;
  }
}
</style>
