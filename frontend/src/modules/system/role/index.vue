<!--
  @file index.vue
  @project Pipker Framework
  @module Frontend Role Management
  @description 提供系统角色的筛选分页、生命周期维护、批量操作、详情成员查看和成员密码重置工作台。
  @logic 优先显示可批量处理的角色清单，角色详情按需加载成员分页；所有雪花 ID 均以字符串传递给后端以避免 JavaScript 精度丢失。
  @dependencies Vue、Element Plus、角色管理 API、frontend API contracts
  @index_tags page、rbac、role、pagination、batch、password-reset、administration
  @author holic512
-->
<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ApiBusinessError } from '../../../core/api/contracts'
import type {
  PageResult,
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
  getRolePage,
  resetRoleMemberPassword,
  updateRole,
} from './api/roleManagement'

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

const selectedRoleIds = computed(() => selectedRoles.value.map((role) => role.id))
const roleDialogTitle = computed(() => (editingRole.value ? '编辑角色' : '新建角色'))
const memberTotalLabel = computed(() => `${memberPage.value.total} 位成员`)

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
  <main class="role-management-page ui-page-frame">
    <header class="role-management-page__header">
      <div class="role-management-page__heading">
        <p class="ui-eyebrow">SYSTEM / ROLE CONTROL</p>
        <h1>角色管理</h1>
        <p>维护普通角色的生命周期、授权成员与账户恢复操作。框架保护角色始终由系统保留。</p>
      </div>
      <div class="role-management-page__header-actions">
        <span class="role-management-page__count">{{ rolePage.total }} 个角色</span>
        <el-button type="primary" @click="openCreateDialog">新建角色</el-button>
      </div>
    </header>

    <section class="role-management-page__filter ui-panel" aria-label="角色筛选">
      <div class="role-management-page__filter-fields">
        <el-input
          v-model="filters.keyword"
          clearable
          placeholder="按角色编码或名称筛选"
          @keyup.enter="applyFilters"
        />
        <el-select v-model="filters.status" clearable placeholder="全部状态">
          <el-option label="启用" value="ENABLED" />
          <el-option label="停用" value="DISABLED" />
        </el-select>
      </div>
      <div class="role-management-page__filter-actions">
        <el-button :loading="loading" type="primary" @click="applyFilters">查询</el-button>
        <el-button :disabled="loading" @click="clearFilters">重置</el-button>
      </div>
    </section>

    <p v-if="errorMessage" class="role-management-page__notice" role="alert">{{ errorMessage }}</p>

    <section class="role-management-page__workspace ui-panel">
      <div class="role-management-page__toolbar">
        <p>
          <strong>{{ selectedRoleIds.length }}</strong>
          <span>项已选择</span>
        </p>
        <div class="role-management-page__batch-actions">
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

      <el-table
        v-loading="loading"
        :data="rolePage.records"
        class="role-management-page__table"
        empty-text="没有符合当前筛选条件的角色"
        @selection-change="setSelectedRoles"
      >
        <el-table-column type="selection" width="48" />
        <el-table-column label="角色" min-width="210">
          <template #default="{ row }: { row: SystemRoleSummary }">
            <div class="role-cell">
              <strong>{{ row.roleName }}</strong>
              <code>{{ row.roleCode }}</code>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="说明" min-width="220" show-overflow-tooltip>
          <template #default="{ row }: { row: SystemRoleSummary }">
            {{ row.description || '—' }}
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }: { row: SystemRoleSummary }">
            <el-tag :type="statusTagType(row.status)" effect="plain">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="排序" width="82" prop="sort" />
        <el-table-column label="最近更新" min-width="164">
          <template #default="{ row }: { row: SystemRoleSummary }">{{ formatTime(row.updatedAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" fixed="right" min-width="216">
          <template #default="{ row }: { row: SystemRoleSummary }">
            <div class="role-management-page__row-actions">
              <el-button link type="primary" @click="openDetail(row)">详情</el-button>
              <el-button link type="primary" @click="openEditDialog(row)">编辑</el-button>
              <el-button link type="danger" @click="removeRole(row)">删除</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <footer class="role-management-page__pagination">
        <span>每页最多 100 条</span>
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
  </main>
</template>

<style scoped lang="scss">
.role-management-page {
  padding: clamp(1.25rem, 3.5vw, 3.25rem);
}

.role-management-page__header,
.role-management-page__header-actions,
.role-management-page__filter,
.role-management-page__filter-fields,
.role-management-page__filter-actions,
.role-management-page__toolbar,
.role-management-page__batch-actions,
.role-management-page__pagination,
.role-detail__hero,
.role-detail__section-heading {
  display: flex;
  align-items: center;
}

.role-management-page__header {
  justify-content: space-between;
  gap: 2rem;
  padding: 0 0 1.6rem;
  border-bottom: 1px solid var(--color-line-subtle);
}

.role-management-page__heading h1,
.role-detail h2,
.role-detail h3 {
  color: var(--color-ink-strong);
  font-family: var(--font-display);
}

.role-management-page__heading h1 {
  margin: 0.45rem 0;
  font-size: clamp(2rem, 4vw, 3rem);
  letter-spacing: -0.055em;
}

.role-management-page__heading > p:last-child {
  max-width: 43rem;
  margin: 0;
  color: var(--color-ink-muted);
  font-size: 0.9rem;
  line-height: 1.75;
}

.role-management-page__header-actions {
  align-self: stretch;
  gap: 0.7rem;
  padding-left: 1.75rem;
  border-left: 1px solid var(--color-line-subtle);
}

.role-management-page__count {
  color: var(--color-ink-soft);
  font-family: var(--font-mono);
  font-size: 0.72rem;
  white-space: nowrap;
}

.role-management-page__filter {
  justify-content: space-between;
  gap: 1rem;
  margin-top: 1.3rem;
  padding: 0.85rem;
}

.role-management-page__filter-fields {
  gap: 0.65rem;
  flex: 1;
}

.role-management-page__filter-fields :deep(.el-input) {
  max-width: 22rem;
}

.role-management-page__filter-fields :deep(.el-select) {
  width: 8rem;
}

.role-management-page__filter-actions,
.role-management-page__batch-actions {
  gap: 0.5rem;
}

.role-management-page__notice {
  margin: 1rem 0 0;
  padding: 0.75rem 0.9rem;
  color: var(--color-accent-danger);
  background: color-mix(in srgb, var(--color-accent-danger) 10%, var(--color-surface-base));
  border: 1px solid color-mix(in srgb, var(--color-accent-danger) 30%, var(--color-line-subtle));
  border-radius: var(--radius-control);
  font-size: 0.84rem;
}

.role-management-page__workspace {
  margin-top: 1.1rem;
  overflow: hidden;
}

.role-management-page__toolbar {
  justify-content: space-between;
  gap: 1rem;
  min-height: 4rem;
  padding: 0.85rem 1rem;
  background: linear-gradient(100deg, var(--color-surface-muted), transparent 70%);
  border-bottom: 1px solid var(--color-line-subtle);
}

.role-management-page__toolbar p {
  display: flex;
  align-items: baseline;
  gap: 0.42rem;
  margin: 0;
  color: var(--color-ink-soft);
  font-size: 0.75rem;
}

.role-management-page__toolbar strong {
  color: var(--color-ink-strong);
  font-family: var(--font-mono);
  font-size: 1.25rem;
}

.role-management-page__table {
  width: 100%;
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

.role-management-page__row-actions {
  display: flex;
  gap: 0.35rem;
}

.role-management-page__pagination {
  justify-content: space-between;
  gap: 1rem;
  min-height: 4.25rem;
  padding: 0.75rem 1rem;
  color: var(--color-ink-soft);
  border-top: 1px solid var(--color-line-subtle);
  font-size: 0.72rem;
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
  .role-management-page__header,
  .role-management-page__filter,
  .role-management-page__toolbar,
  .role-detail__section-heading {
    align-items: stretch;
    flex-direction: column;
  }

  .role-management-page__header-actions {
    justify-content: space-between;
    padding: 1rem 0 0;
    border-top: 1px solid var(--color-line-subtle);
    border-left: 0;
  }

  .role-management-page__filter-fields {
    align-items: stretch;
    flex-direction: column;
  }

  .role-management-page__filter-fields :deep(.el-input),
  .role-management-page__filter-fields :deep(.el-select),
  .role-detail__section-heading :deep(.el-input) {
    max-width: none;
    width: 100%;
  }

  .role-management-page__pagination {
    align-items: flex-start;
    flex-direction: column;
  }

  .role-management-page__pagination :deep(.el-pagination),
  .role-detail__member-pagination {
    max-width: 100%;
    overflow-x: auto;
  }
}

@include at-most('phone') {
  .role-management-page {
    padding: 1rem;
  }

  .role-management-page__batch-actions {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
  }

  .role-management-page__batch-actions :deep(.el-button) {
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
