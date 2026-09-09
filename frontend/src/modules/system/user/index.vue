<!--
  @file index.vue
  @project Pipker Framework
  @module 系统用户管理
  @description 提供系统用户的筛选分页、创建编辑、多角色分配、状态维护、删除、详情与密码重置工作区。
  @logic 主列表只展示安全用户投影；创建和编辑使用完整角色选择，受保护账户仅可查看，所有写操作完成后重新读取当前页以保持角色和状态显示一致。
  @dependencies Vue、Element Plus、Pinia 会话 Store、用户管理 API、frontend API contracts
  @index_tags user、rbac、role-assignment、password-reset、pagination、administration
  @author holic512
-->
<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ApiBusinessError } from '../../../core/api/contracts'
import type {
  AssignableSystemRole,
  PageResult,
  SystemUserDetail,
  SystemUserStatus,
  SystemUserSummary,
} from '../../../core/api/contracts'
import { useSessionStore } from '../../../stores/session'
import {
  batchDeleteUsers,
  batchUpdateUserStatus,
  createUser,
  deleteUser,
  getAssignableRoles,
  getUserDetail,
  getUserPage,
  resetUserPassword,
  updateUser,
} from './api/userManagement'

interface UserFormState {
  username: string
  password: string
  nickname: string
  phone: string
  email: string
  status: SystemUserStatus
  roleIds: string[]
}

const EMPTY_USER_PAGE: PageResult<SystemUserSummary> = {
  page: 1,
  pageSize: 10,
  total: 0,
  records: [],
}

const session = useSessionStore()
const filters = reactive<{ keyword: string; status: SystemUserStatus | undefined }>({
  keyword: '',
  status: undefined,
})
const userPage = ref<PageResult<SystemUserSummary>>(EMPTY_USER_PAGE)
const selectedUsers = ref<SystemUserSummary[]>([])
const loading = ref(false)
const operating = ref(false)
const errorMessage = ref<string | null>(null)

const userDialogVisible = ref(false)
const editingUser = ref<SystemUserSummary | null>(null)
const userForm = reactive<UserFormState>(newUserForm())
const assignableRoles = ref<AssignableSystemRole[]>([])
const dialogLoading = ref(false)
const savingUser = ref(false)

const detailDrawerVisible = ref(false)
const detailLoading = ref(false)
const userDetail = ref<SystemUserDetail | null>(null)

const passwordDialogVisible = ref(false)
const selectedPasswordUser = ref<SystemUserSummary | null>(null)
const resetPassword = ref('')
const resettingPassword = ref(false)

const canManage = computed(() => session.permissions.includes('system-user-manage'))
const selectedUserIds = computed(() => selectedUsers.value.map(user => user.id))
const userDialogTitle = computed(() => editingUser.value ? '编辑用户' : '新建用户')

onMounted(() => {
  void loadUsers()
})

async function loadUsers(page = userPage.value.page): Promise<void> {
  loading.value = true
  errorMessage.value = null
  try {
    userPage.value = await getUserPage({
      page,
      pageSize: userPage.value.pageSize,
      keyword: filters.keyword.trim() || undefined,
      status: filters.status,
    })
    selectedUsers.value = []
  } catch (error) {
    selectedUsers.value = []
    errorMessage.value = readableError(error, '无法读取用户列表。')
  } finally {
    loading.value = false
  }
}

function applyFilters(): void {
  void loadUsers(1)
}

function clearFilters(): void {
  filters.keyword = ''
  filters.status = undefined
  void loadUsers(1)
}

function changeUserPage(page: number): void {
  void loadUsers(page)
}

function changeUserPageSize(pageSize: number): void {
  userPage.value = { ...userPage.value, page: 1, pageSize }
  void loadUsers(1)
}

function setSelectedUsers(users: SystemUserSummary[]): void {
  selectedUsers.value = users
}

function isUserSelectable(user: SystemUserSummary): boolean {
  return !user.protectedAccount
}

async function openCreateDialog(): Promise<void> {
  editingUser.value = null
  Object.assign(userForm, newUserForm())
  if (!await loadAssignableRoles()) {
    return
  }
  userDialogVisible.value = true
}

async function openEditDialog(user: SystemUserSummary): Promise<void> {
  if (user.protectedAccount || !await loadAssignableRoles()) {
    return
  }
  dialogLoading.value = true
  userDialogVisible.value = true
  editingUser.value = user
  try {
    const detail = await getUserDetail(user.id)
    Object.assign(userForm, {
      username: detail.username,
      password: '',
      nickname: detail.nickname ?? '',
      phone: detail.phone ?? '',
      email: detail.email ?? '',
      status: detail.status,
      roleIds: detail.roles.map(role => role.id),
    })
  } catch (error) {
    ElMessage.error(readableError(error, '无法读取用户资料。'))
    userDialogVisible.value = false
  } finally {
    dialogLoading.value = false
  }
}

async function loadAssignableRoles(): Promise<boolean> {
  try {
    assignableRoles.value = await getAssignableRoles()
    return true
  } catch (error) {
    ElMessage.error(readableError(error, '无法读取可分配角色。'))
    return false
  }
}

async function saveUser(): Promise<void> {
  const username = userForm.username.trim()
  if ((!editingUser.value && !username) || (!editingUser.value && userForm.password.length < 8)) {
    ElMessage.warning('请填写用户名和至少 8 个字符的初始密码。')
    return
  }
  if (userForm.roleIds.length === 0) {
    ElMessage.warning('请至少分配一个启用角色。')
    return
  }

  savingUser.value = true
  try {
    const payload = {
      nickname: userForm.nickname.trim() || null,
      phone: userForm.phone.trim() || null,
      email: userForm.email.trim() || null,
      status: userForm.status,
      roleIds: [...new Set(userForm.roleIds)],
    }
    if (editingUser.value) {
      await updateUser(editingUser.value.id, payload)
      ElMessage.success('用户资料与角色分配已更新。')
    } else {
      await createUser({ ...payload, username, password: userForm.password })
      ElMessage.success('用户已创建并完成角色分配。')
    }
    userDialogVisible.value = false
    await loadUsers(editingUser.value ? userPage.value.page : 1)
  } catch (error) {
    ElMessage.error(readableError(error, '保存用户失败。'))
  } finally {
    savingUser.value = false
  }
}

async function batchChangeStatus(status: SystemUserStatus): Promise<void> {
  if (selectedUserIds.value.length === 0) {
    ElMessage.warning('请先选择至少一个普通用户。')
    return
  }
  const action = status === 'ENABLED' ? '启用' : '停用'
  if (!await confirm(`确认${action}已选的 ${selectedUserIds.value.length} 个用户吗？`, '批量用户状态')) {
    return
  }
  operating.value = true
  try {
    await batchUpdateUserStatus(selectedUserIds.value, status)
    ElMessage.success(`已${action} ${selectedUserIds.value.length} 个用户。`)
    await loadUsers()
  } catch (error) {
    ElMessage.error(readableError(error, `批量${action}失败。`))
  } finally {
    operating.value = false
  }
}

async function batchDelete(): Promise<void> {
  if (selectedUserIds.value.length === 0) {
    ElMessage.warning('请先选择至少一个普通用户。')
    return
  }
  if (!await confirm(
    `确认删除已选的 ${selectedUserIds.value.length} 个用户吗？其全部角色关联会一并移除。`,
    '批量删除用户',
    'warning',
  )) {
    return
  }
  operating.value = true
  try {
    await batchDeleteUsers(selectedUserIds.value)
    ElMessage.success('用户及其角色关联已删除。')
    await loadUsers(userPage.value.records.length === selectedUserIds.value.length && userPage.value.page > 1
      ? userPage.value.page - 1
      : userPage.value.page)
  } catch (error) {
    ElMessage.error(readableError(error, '批量删除失败。'))
  } finally {
    operating.value = false
  }
}

async function removeUser(user: SystemUserSummary): Promise<void> {
  if (!await confirm(`确认删除用户“${user.username}”吗？其全部角色关联会一并移除。`, '删除用户', 'warning')) {
    return
  }
  operating.value = true
  try {
    await deleteUser(user.id)
    ElMessage.success('用户及其角色关联已删除。')
    await loadUsers(userPage.value.records.length === 1 && userPage.value.page > 1
      ? userPage.value.page - 1
      : userPage.value.page)
  } catch (error) {
    ElMessage.error(readableError(error, '删除用户失败。'))
  } finally {
    operating.value = false
  }
}

async function openDetail(user: SystemUserSummary): Promise<void> {
  detailDrawerVisible.value = true
  userDetail.value = null
  detailLoading.value = true
  try {
    userDetail.value = await getUserDetail(user.id)
  } catch (error) {
    ElMessage.error(readableError(error, '无法读取用户详情。'))
  } finally {
    detailLoading.value = false
  }
}

function openPasswordDialog(user: SystemUserSummary): void {
  selectedPasswordUser.value = user
  resetPassword.value = ''
  passwordDialogVisible.value = true
}

async function submitPasswordReset(): Promise<void> {
  if (!selectedPasswordUser.value) {
    return
  }
  if (resetPassword.value.length < 8) {
    ElMessage.warning('新密码至少需要 8 个字符。')
    return
  }
  if (!await confirm(`确认重置用户“${selectedPasswordUser.value.username}”的密码吗？`, '重置密码', 'warning')) {
    return
  }
  resettingPassword.value = true
  try {
    await resetUserPassword(selectedPasswordUser.value.id, resetPassword.value)
    passwordDialogVisible.value = false
    resetPassword.value = ''
    ElMessage.success('密码已重置。请通过安全渠道通知用户。')
  } catch (error) {
    ElMessage.error(readableError(error, '重置密码失败。'))
  } finally {
    resettingPassword.value = false
  }
}

function statusLabel(status: SystemUserStatus): string {
  return status === 'ENABLED' ? '启用' : '停用'
}

function statusTagType(status: SystemUserStatus): 'success' | 'info' {
  return status === 'ENABLED' ? 'success' : 'info'
}

function roleTagType(status: SystemUserStatus): 'info' | 'warning' {
  return status === 'ENABLED' ? 'info' : 'warning'
}

function formatTime(value: string | null): string {
  if (!value) {
    return '—'
  }
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? value : new Intl.DateTimeFormat('zh-CN', {
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

function newUserForm(): UserFormState {
  return {
    username: '',
    password: '',
    nickname: '',
    phone: '',
    email: '',
    status: 'ENABLED',
    roleIds: [],
  }
}
</script>

<template>
  <section class="user-management-page" aria-label="用户管理">
    <form class="user-filters" aria-label="用户筛选" @submit.prevent="applyFilters">
      <el-input v-model="filters.keyword" clearable aria-label="搜索用户" class="user-search" placeholder="用户名、昵称、手机或邮箱" />
      <el-select v-model="filters.status" clearable aria-label="用户状态" placeholder="全部状态">
        <el-option label="启用" value="ENABLED" />
        <el-option label="停用" value="DISABLED" />
      </el-select>
      <div class="user-filter-actions">
        <el-button native-type="submit" type="primary" :loading="loading">查询</el-button>
        <el-button :disabled="loading" @click="clearFilters">重置</el-button>
        <el-button v-if="canManage" type="primary" plain @click="openCreateDialog">新建用户</el-button>
        <div v-if="canManage" class="user-batch-actions" aria-label="批量操作">
          <el-button :disabled="selectedUserIds.length === 0" :loading="operating" @click="batchChangeStatus('ENABLED')">批量启用</el-button>
          <el-button :disabled="selectedUserIds.length === 0" :loading="operating" @click="batchChangeStatus('DISABLED')">批量停用</el-button>
          <el-button :disabled="selectedUserIds.length === 0" :loading="operating" type="danger" plain @click="batchDelete">批量删除</el-button>
        </div>
      </div>
    </form>

    <div v-if="errorMessage" class="user-notice" role="alert">
      <span>{{ errorMessage }}</span>
      <el-button link type="primary" :loading="loading" @click="loadUsers()">重试</el-button>
    </div>

    <section class="user-workspace ui-panel">
      <div class="user-table-container">
        <el-table v-loading="loading" :data="errorMessage ? [] : userPage.records" height="100%" empty-text="没有符合当前筛选条件的用户" @selection-change="setSelectedUsers">
          <el-table-column v-if="canManage" type="selection" width="48" :selectable="isUserSelectable" />
          <el-table-column label="账户 / 昵称" min-width="178">
            <template #default="{ row }: { row: SystemUserSummary }">
              <div class="user-identity"><strong>{{ row.username }}</strong><code>{{ row.nickname || '未设置昵称' }}</code></div>
            </template>
          </el-table-column>
          <el-table-column label="联系方式" min-width="185" show-overflow-tooltip>
            <template #default="{ row }: { row: SystemUserSummary }">
              <div class="user-contact"><span>{{ row.phone || '—' }}</span><small>{{ row.email || '—' }}</small></div>
            </template>
          </el-table-column>
          <el-table-column label="角色" min-width="180" show-overflow-tooltip>
            <template #default="{ row }: { row: SystemUserSummary }">
              <div class="user-role-list"><el-tag v-for="role in row.roles" :key="role.id" class="user-role-tag" size="small" effect="light" :type="roleTagType(role.status)">{{ role.roleName }}</el-tag><span v-if="row.roles.length === 0">未分配</span></div>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="78">
            <template #default="{ row }: { row: SystemUserSummary }"><el-tag class="user-status-tag" size="small" effect="light" :type="statusTagType(row.status)">{{ statusLabel(row.status) }}</el-tag></template>
          </el-table-column>
          <el-table-column label="最近登录" min-width="154"><template #default="{ row }: { row: SystemUserSummary }">{{ formatTime(row.lastLoginTime) }}</template></el-table-column>
          <el-table-column label="操作" fixed="right" :width="canManage ? 304 : 72">
            <template #default="{ row }: { row: SystemUserSummary }">
              <div class="user-row-actions">
                <el-button link type="primary" @click="openDetail(row)">详情</el-button>
                <template v-if="canManage && !row.protectedAccount">
                  <el-button link type="primary" @click="openEditDialog(row)">编辑</el-button>
                  <el-button link type="warning" @click="openPasswordDialog(row)">重置密码</el-button>
                  <el-button link type="danger" @click="removeUser(row)">删除</el-button>
                </template>
                <span v-else-if="row.protectedAccount" class="user-protected">受保护</span>
              </div>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <footer class="user-page-summary">
        <span>{{ errorMessage ? '—' : `共 ${userPage.total} 个用户${selectedUserIds.length > 0 ? `，已选择 ${selectedUserIds.length} 项` : ''}` }}</span>
        <el-pagination background :current-page="userPage.page" :page-size="userPage.pageSize" :page-sizes="[10, 20, 50, 100]" :total="userPage.total" layout="sizes, prev, pager, next" @current-change="changeUserPage" @size-change="changeUserPageSize" />
      </footer>
    </section>

    <el-dialog v-model="userDialogVisible" :close-on-click-modal="!savingUser" :close-on-press-escape="!savingUser" :title="userDialogTitle" width="min(38rem, calc(100vw - 2rem))">
      <el-form v-loading="dialogLoading" class="user-form" label-position="top">
        <div class="user-form__grid">
          <el-form-item label="用户名" required><el-input v-model="userForm.username" :disabled="editingUser !== null || dialogLoading || savingUser" maxlength="64" placeholder="例如 content_editor" /></el-form-item>
          <el-form-item v-if="!editingUser" label="初始密码" required><el-input v-model="userForm.password" :disabled="dialogLoading || savingUser" maxlength="128" placeholder="至少 8 个字符" show-password type="password" /></el-form-item>
          <el-form-item label="昵称"><el-input v-model="userForm.nickname" :disabled="dialogLoading || savingUser" maxlength="100" placeholder="用户显示名称" /></el-form-item>
          <el-form-item label="状态" required><el-select v-model="userForm.status" :disabled="dialogLoading || savingUser"><el-option label="启用" value="ENABLED" /><el-option label="停用" value="DISABLED" /></el-select></el-form-item>
        </div>
        <div class="user-form__grid">
          <el-form-item label="手机号"><el-input v-model="userForm.phone" :disabled="dialogLoading || savingUser" maxlength="32" placeholder="可选" /></el-form-item>
          <el-form-item label="邮箱"><el-input v-model="userForm.email" :disabled="dialogLoading || savingUser" maxlength="255" placeholder="可选" /></el-form-item>
        </div>
        <el-form-item label="角色分配" required>
          <el-select v-model="userForm.roleIds" :disabled="dialogLoading || savingUser" collapse-tags collapse-tags-tooltip filterable multiple placeholder="选择至少一个启用角色">
            <el-option v-for="role in assignableRoles" :key="role.id" :label="`${role.roleName} (${role.roleCode})`" :value="role.id" />
          </el-select>
          <p class="user-form__hint">只能分配启用的普通角色。SUPER_ADMIN 账户与角色由框架保护，不在此处维护。</p>
        </el-form-item>
      </el-form>
      <template #footer><el-button :disabled="savingUser" @click="userDialogVisible = false">取消</el-button><el-button type="primary" :loading="savingUser" :disabled="dialogLoading" @click="saveUser">保存用户</el-button></template>
    </el-dialog>

    <el-drawer v-model="detailDrawerVisible" :size="'min(38rem, 100vw)'" title="用户详情">
      <section v-if="userDetail" v-loading="detailLoading" class="user-detail">
        <header class="user-detail__hero"><div><h2>{{ userDetail.username }}</h2><code>{{ userDetail.nickname || '未设置昵称' }}</code></div><el-tag class="user-status-tag" size="small" effect="light" :type="statusTagType(userDetail.status)">{{ statusLabel(userDetail.status) }}</el-tag></header>
        <p v-if="userDetail.protectedAccount" class="user-detail__notice">此账户具有 SUPER_ADMIN 角色，受框架保护，只能查看。</p>
        <dl class="user-detail__facts">
          <div><dt>手机号</dt><dd>{{ userDetail.phone || '—' }}</dd></div><div><dt>邮箱</dt><dd>{{ userDetail.email || '—' }}</dd></div>
          <div><dt>最近登录</dt><dd>{{ formatTime(userDetail.lastLoginTime) }}</dd></div><div><dt>创建时间</dt><dd>{{ formatTime(userDetail.createdAt) }}</dd></div>
          <div><dt>更新时间</dt><dd>{{ formatTime(userDetail.updatedAt) }}</dd></div>
        </dl>
        <section class="user-detail__roles"><h3>角色分配</h3><div class="user-role-list"><el-tag v-for="role in userDetail.roles" :key="role.id" class="user-role-tag" size="small" effect="light" :type="roleTagType(role.status)">{{ role.roleName }} / {{ role.roleCode }}</el-tag><span v-if="userDetail.roles.length === 0">未分配角色</span></div></section>
      </section>
      <div v-else v-loading="detailLoading" class="user-detail__loading">正在读取用户详情…</div>
    </el-drawer>

    <el-dialog v-model="passwordDialogVisible" :close-on-click-modal="false" title="重置用户密码" width="min(28rem, calc(100vw - 2rem))">
      <p class="user-password-copy">将为 <strong>{{ selectedPasswordUser?.username }}</strong> 设置新的登录密码。密码不会显示或以明文形式保存。</p>
      <el-input v-model="resetPassword" maxlength="128" placeholder="请输入至少 8 个字符的新密码" show-password type="password" />
      <template #footer><el-button @click="passwordDialogVisible = false">取消</el-button><el-button type="warning" :loading="resettingPassword" @click="submitPasswordReset">确认重置</el-button></template>
    </el-dialog>
  </section>
</template>

<style scoped lang="scss">
.user-management-page { display: flex; flex: 1; flex-direction: column; width: 100%; min-height: 0; gap: 0.625rem; }
.user-filters, .user-filter-actions, .user-batch-actions, .user-detail__hero { display: flex; align-items: center; }
.user-filters { flex-wrap: wrap; gap: 0.5rem; }
.user-search { flex: 1; min-width: 13rem; }
.user-filters > .el-select { width: 7.5rem; }
.user-filter-actions { flex-wrap: wrap; gap: 0.5rem; margin-left: auto; }
.user-batch-actions { gap: 0.5rem; }
.user-notice { display: flex; align-items: center; justify-content: space-between; gap: 0.75rem; padding: 0.55rem 0.75rem; color: var(--color-accent-danger); background: color-mix(in srgb, var(--color-accent-danger) 7%, var(--color-surface-base)); border-radius: var(--radius-control); font-size: 0.78rem; }
.user-workspace { display: flex; flex: 1; flex-direction: column; min-height: 0; overflow: hidden; }
.user-table-container { flex: 1; min-height: 0; }
.user-identity { display: flex; flex: 1 1 0; min-width: 0; align-items: center; gap: 0.36rem; white-space: nowrap; }
.user-identity strong, .user-identity code { min-width: 0; overflow: hidden; text-overflow: ellipsis; }
.user-identity strong { color: var(--color-ink-strong); font-size: 0.72rem; font-weight: 600; line-height: 1.1; }
.user-identity code, .user-contact small, .user-detail code { color: var(--color-ink-soft); font-family: var(--font-mono); font-size: 0.62rem; line-height: 1.1; }
.user-contact { display: grid; gap: 0.16rem; min-width: 0; }
.user-contact span, .user-contact small { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.user-role-list { display: flex; flex-wrap: wrap; gap: 0.25rem; align-items: center; min-width: 0; color: var(--color-ink-soft); font-size: 0.68rem; }
.user-workspace :deep(.el-table__header-wrapper th.el-table__cell) { padding: 0.35rem 0; color: var(--color-ink-muted); font-size: 0.76rem; font-weight: 650; }
.user-workspace :deep(.el-table__body-wrapper td.el-table__cell) { padding: calc(0.25rem + 3px) 0; font-size: 0.7rem; }
.user-workspace :deep(.el-table .cell) { line-height: 1.15; }
.user-workspace :deep(.user-status-tag.el-tag), .user-workspace :deep(.user-role-tag.el-tag) { min-width: 3rem; height: 1.18rem; justify-content: center; padding-inline: 0.24rem; border-radius: var(--radius-small); font-size: 0.64rem; font-weight: 650; }
.user-workspace :deep(.user-role-tag.el-tag) { min-width: 0; }
.user-row-actions { display: flex; align-items: center; gap: 0.35rem; }
.user-protected { color: var(--color-ink-soft); font-size: 0.65rem; white-space: nowrap; }
.user-page-summary { display: flex; align-items: center; justify-content: space-between; gap: 0.75rem; min-height: 2.25rem; padding: 0.4rem 0.75rem; color: var(--color-ink-soft); border-top: 1px solid var(--color-line-subtle); font-size: 0.68rem; }
.user-form__grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 0 1rem; }
.user-form :deep(.el-select) { width: 100%; }
.user-form__hint { margin: 0.4rem 0 0; color: var(--color-ink-soft); font-size: 0.72rem; line-height: 1.5; }
.user-detail { padding: 0 0.35rem 1.25rem; }
.user-detail__hero { justify-content: space-between; gap: 1rem; padding-bottom: 1.35rem; border-bottom: 1px solid var(--color-line-subtle); }
.user-detail__hero h2, .user-detail__roles h3 { margin: 0; color: var(--color-ink-strong); font-family: var(--font-display); }
.user-detail__hero h2 { margin-bottom: 0.28rem; font-size: 1.65rem; }
.user-detail__notice { margin: 1rem 0; padding: 0.65rem 0.75rem; color: var(--color-ink-muted); background: var(--color-surface-muted); border: 1px solid var(--color-line-subtle); border-radius: var(--radius-control); font-size: 0.76rem; line-height: 1.55; }
.user-detail__facts { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 0; margin: 1rem 0 1.5rem; border: 1px solid var(--color-line-subtle); border-radius: var(--radius-control); overflow: hidden; }
.user-detail__facts div { min-height: 4.8rem; padding: 0.8rem; background: var(--color-surface-muted); border-right: 1px solid var(--color-line-subtle); border-bottom: 1px solid var(--color-line-subtle); }
.user-detail__facts div:nth-child(even) { border-right: 0; }
.user-detail__facts div:last-child { border-bottom: 0; }
.user-detail__facts dt { color: var(--color-ink-soft); font-size: 0.67rem; font-weight: 760; letter-spacing: 0.08em; }
.user-detail__facts dd { margin: 0.42rem 0 0; overflow-wrap: anywhere; color: var(--color-ink-strong); font-size: 0.8rem; line-height: 1.45; }
.user-detail__roles { display: grid; gap: 0.7rem; }
.user-detail__roles h3 { font-size: 1.05rem; }
.user-detail__loading { min-height: 15rem; display: grid; place-items: center; color: var(--color-ink-muted); }
.user-password-copy { margin: 0 0 1rem; color: var(--color-ink-muted); font-size: 0.88rem; line-height: 1.7; }
.user-password-copy strong { color: var(--color-ink-strong); }
@include at-most('tablet') { .user-filters, .user-detail__hero { align-items: stretch; flex-direction: column; } .user-search { flex-basis: 100%; } .user-filters > .el-select { flex: 1; min-width: 6rem; } .user-filter-actions { margin-left: 0; } .user-page-summary { padding: 0.45rem 0.65rem; } .user-page-summary :deep(.el-pagination) { max-width: 100%; overflow-x: auto; } }
@include at-most('phone') { .user-batch-actions { display: grid; grid-template-columns: repeat(3, 1fr); } .user-batch-actions :deep(.el-button) { width: 100%; margin: 0; } .user-form__grid, .user-detail__facts { grid-template-columns: 1fr; } .user-detail__facts div, .user-detail__facts div:nth-child(even) { border-right: 0; border-bottom: 1px solid var(--color-line-subtle); } .user-detail__facts div:last-child { border-bottom: 0; } }
</style>
