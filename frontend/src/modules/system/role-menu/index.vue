<!--
  @file index.vue
  @project Pipker Framework
  @module Frontend Role Route Management
  @description Provides the role-first page-route administration screen for SUPER_ADMIN.
  @logic Loads the canonical database menu tree, edits only ordinary role leaf-menu selections, and performs a full replacement save through the protected configuration API.
  @dependencies Vue, Element Plus, RoleMenuTree, roleMenu API
  @index_tags page, rbac, role-menu, administration
  @author holic512
-->
<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ApiBusinessError, type RoleMenuConfiguration, type RoleMenuConfigurationRole } from '../../../core/api/contracts'
import RoleMenuTree from './components/RoleMenuTree.vue'
import { getRoleMenuConfiguration, replaceRoleMenuAssignments } from './api/roleMenu'

const configuration = ref<RoleMenuConfiguration | null>(null)
const selectedRoleId = ref<number | null>(null)
const selectedMenuIds = ref<number[]>([])
const loading = ref(false)
const saving = ref(false)
const feedback = ref<string | null>(null)
const errorMessage = ref<string | null>(null)

const selectedRole = computed<RoleMenuConfigurationRole | null>(() => {
  if (!configuration.value || selectedRoleId.value === null) {
    return null
  }
  return configuration.value.roles.find((role) => role.id === selectedRoleId.value) ?? null
})

const selectedMenuCount = computed(() => selectedMenuIds.value.length)

onMounted(() => {
  void loadConfiguration()
})

async function loadConfiguration(): Promise<void> {
  loading.value = true
  errorMessage.value = null
  try {
    configuration.value = await getRoleMenuConfiguration()
    const initialRole = configuration.value.roles.find((role) => !role.allMenus) ?? configuration.value.roles[0] ?? null
    selectRole(initialRole)
  } catch (error) {
    errorMessage.value = readableError(error, '无法读取角色路由配置。')
  } finally {
    loading.value = false
  }
}

function selectRole(role: RoleMenuConfigurationRole | null): void {
  selectedRoleId.value = role?.id ?? null
  selectedMenuIds.value = role ? [...role.menuIds] : []
  feedback.value = null
}

async function save(): Promise<void> {
  const role = selectedRole.value
  if (!role || role.allMenus) {
    return
  }

  saving.value = true
  feedback.value = null
  errorMessage.value = null
  try {
    const result = await replaceRoleMenuAssignments(role.id, selectedMenuIds.value)
    role.menuIds = result.menuIds
    selectedMenuIds.value = [...result.menuIds]
    feedback.value = `已保存 ${result.menuIds.length} 个页面菜单。`
  } catch (error) {
    errorMessage.value = readableError(error, '保存角色路由失败。')
  } finally {
    saving.value = false
  }
}

function readableError(error: unknown, fallback: string): string {
  return error instanceof ApiBusinessError ? error.message : fallback
}
</script>

<template>
  <main class="role-menu-page">
    <header class="role-menu-page__header">
      <div>
        <p class="role-menu-page__eyebrow">SYSTEM / ROLE ROUTES</p>
        <h1>角色路由</h1>
        <p>从角色出发配置可装载的页面菜单。接口权限仍由独立的 API 权限配置控制。</p>
      </div>
      <el-button :loading="loading" plain @click="loadConfiguration">重新读取</el-button>
    </header>

    <p v-if="errorMessage" class="role-menu-page__notice role-menu-page__notice--error" role="alert">{{ errorMessage }}</p>
    <p v-if="feedback" class="role-menu-page__notice role-menu-page__notice--success">{{ feedback }}</p>

    <section v-if="configuration" class="role-menu-page__workspace">
      <aside class="role-menu-page__roles" aria-label="角色选择">
        <p class="role-menu-page__caption">启用角色</p>
        <button
          v-for="role in configuration.roles"
          :key="role.id"
          class="role-option"
          :class="{ 'role-option--active': role.id === selectedRoleId }"
          type="button"
          @click="selectRole(role)"
        >
          <span>
            <strong>{{ role.name }}</strong>
            <small>{{ role.code }}</small>
          </span>
          <em v-if="role.allMenus">全部</em>
          <em v-else>{{ role.menuIds.length }}</em>
        </button>
      </aside>

      <section class="role-menu-page__editor" aria-live="polite">
        <template v-if="selectedRole">
          <div class="role-menu-page__editor-heading">
            <div>
              <p class="role-menu-page__caption">页面菜单</p>
              <h2>{{ selectedRole.name }}</h2>
              <p>{{ selectedRole.allMenus ? 'SUPER_ADMIN 自动拥有全部启用菜单，不能手工收窄。' : '仅勾选可访问页面；目录只用于组织菜单树。' }}</p>
            </div>
            <el-button
              v-if="!selectedRole.allMenus"
              :disabled="loading"
              :loading="saving"
              type="primary"
              @click="save"
            >
              保存 {{ selectedMenuCount }} 项
            </el-button>
          </div>

          <RoleMenuTree
            :disabled="selectedRole.allMenus || saving"
            :menus="configuration.menus"
            v-model="selectedMenuIds"
          />
        </template>
        <p v-else class="role-menu-page__empty">当前没有可配置角色。</p>
      </section>
    </section>

    <section v-else-if="loading" class="role-menu-page__loading">正在读取角色与菜单配置…</section>
  </main>
</template>

<style scoped>
.role-menu-page {
  width: min(100%, 78rem);
  margin: 0 auto;
  padding: clamp(1.5rem, 4vw, 3.5rem);
}

.role-menu-page__header,
.role-menu-page__editor-heading {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 1.5rem;
}

.role-menu-page__header {
  padding-bottom: 2rem;
  border-bottom: 1px solid var(--line-subtle);
}

.role-menu-page__eyebrow,
.role-menu-page__caption {
  margin: 0;
  color: var(--accent-strong);
  font-family: var(--font-mono);
  font-size: 0.66rem;
  font-weight: 800;
  letter-spacing: 0.11em;
}

.role-menu-page h1,
.role-menu-page h2 {
  color: var(--ink-strong);
  font-family: var(--font-display);
}

.role-menu-page h1 {
  margin: 0.55rem 0;
  font-size: clamp(2rem, 4vw, 3.2rem);
}

.role-menu-page h2 {
  margin: 0.4rem 0;
  font-size: 1.7rem;
}

.role-menu-page__header p:not(.role-menu-page__eyebrow),
.role-menu-page__editor-heading p:not(.role-menu-page__caption) {
  max-width: 41rem;
  margin: 0;
  color: var(--ink-muted);
  font-size: 0.88rem;
  line-height: 1.75;
}

.role-menu-page__notice {
  margin: 1rem 0 0;
  padding: 0.8rem 1rem;
  border-radius: 0.4rem;
  font-size: 0.84rem;
}

.role-menu-page__notice--error {
  color: #8a2424;
  background: #fff1f0;
}

.role-menu-page__notice--success {
  color: #315a2d;
  background: #edf7e9;
}

.role-menu-page__workspace {
  display: grid;
  grid-template-columns: minmax(13rem, 0.32fr) minmax(0, 1fr);
  gap: clamp(1.25rem, 3vw, 3rem);
  margin-top: 1.75rem;
}

.role-menu-page__roles,
.role-menu-page__editor {
  padding: 1.25rem;
  background: #f5f8f2;
  border: 1px solid var(--line-subtle);
  border-radius: 0.55rem;
}

.role-menu-page__roles {
  align-self: start;
  display: grid;
  gap: 0.55rem;
}

.role-option {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.65rem;
  width: 100%;
  padding: 0.75rem;
  color: var(--ink-strong);
  text-align: left;
  cursor: pointer;
  background: transparent;
  border: 1px solid transparent;
  border-radius: 0.36rem;
}

.role-option:hover,
.role-option--active {
  background: #fbfcf8;
  border-color: #c5d8bc;
}

.role-option span {
  display: grid;
  gap: 0.2rem;
}

.role-option strong {
  font-size: 0.82rem;
}

.role-option small,
.role-option em {
  color: var(--ink-soft);
  font-family: var(--font-mono);
  font-size: 0.62rem;
  font-style: normal;
}

.role-option em {
  padding: 0.2rem 0.36rem;
  color: var(--accent-strong);
  background: #eaf3e4;
  border-radius: 999px;
}

.role-menu-page__editor {
  min-height: 26rem;
}

.role-menu-page__editor-heading {
  margin-bottom: 1.4rem;
}

.role-menu-page__empty,
.role-menu-page__loading {
  color: var(--ink-muted);
  font-size: 0.9rem;
}

.role-menu-page__loading {
  margin-top: 1.5rem;
}

@media (max-width: 48rem) {
  .role-menu-page__workspace {
    grid-template-columns: 1fr;
  }

  .role-menu-page__header,
  .role-menu-page__editor-heading {
    flex-direction: column;
  }
}
</style>
