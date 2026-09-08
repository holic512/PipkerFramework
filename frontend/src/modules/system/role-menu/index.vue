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
  <main class="role-menu-page ui-page-frame">
    <header class="role-menu-page__header">
      <div>
        <p class="role-menu-page__eyebrow ui-eyebrow">SYSTEM / ROLE ROUTES</p>
        <h1>角色路由</h1>
        <p>从角色出发配置可装载的页面菜单。接口权限仍由独立的 API 权限配置控制。</p>
      </div>
      <el-button :loading="loading" plain @click="loadConfiguration">重新读取</el-button>
    </header>

    <p v-if="errorMessage" class="role-menu-page__notice role-menu-page__notice--error" role="alert">{{ errorMessage }}</p>
    <p v-if="feedback" class="role-menu-page__notice role-menu-page__notice--success">{{ feedback }}</p>

    <section v-if="configuration" class="role-menu-page__workspace">
      <aside class="role-menu-page__roles ui-panel" aria-label="角色选择">
        <p class="role-menu-page__caption ui-eyebrow">启用角色</p>
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

      <section class="role-menu-page__editor ui-panel" aria-live="polite">
        <template v-if="selectedRole">
          <div class="role-menu-page__editor-heading">
            <div>
              <p class="role-menu-page__caption ui-eyebrow">页面菜单</p>
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

<style scoped lang="scss">
.role-menu-page {
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
  border-bottom: 1px solid var(--color-line-subtle);
}

.role-menu-page h1,
.role-menu-page h2 {
  color: var(--color-ink-strong);
  font-family: var(--font-family);
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
  color: var(--color-ink-muted);
  font-size: 0.88rem;
  line-height: 1.75;
}

.role-menu-page__notice {
  margin: 1rem 0 0;
  padding: 0.8rem 1rem;
  border: 1px solid transparent;
  border-radius: var(--radius-control);
  font-size: 0.84rem;
}

.role-menu-page__notice--error {
  color: var(--color-accent-danger);
  background: color-mix(in srgb, var(--color-accent-danger) 12%, var(--color-surface-base));
  border-color: color-mix(in srgb, var(--color-accent-danger) 30%, var(--color-line-subtle));
}

.role-menu-page__notice--success {
  color: var(--color-accent-success);
  background: color-mix(in srgb, var(--color-accent-success) 12%, var(--color-surface-base));
  border-color: color-mix(in srgb, var(--color-accent-success) 30%, var(--color-line-subtle));
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
  color: var(--color-ink-strong);
  text-align: left;
  cursor: pointer;
  background: transparent;
  border: 1px solid transparent;
  border-radius: var(--radius-small);
}

.role-option:hover,
.role-option--active {
  background: var(--color-surface-base);
  border-color: var(--color-line-strong);
}

.role-option--active {
  background: color-mix(in srgb, var(--color-accent-primary) 10%, var(--color-surface-base));
  border-color: var(--color-accent-primary);
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
  color: var(--color-ink-soft);
  font-family: var(--font-family);
  font-size: 0.62rem;
  font-style: normal;
}

.role-option em {
  padding: 0.2rem 0.36rem;
  color: var(--color-accent-primary);
  background: color-mix(in srgb, var(--color-accent-primary) 11%, var(--color-surface-base));
  border-radius: var(--radius-pill);
}

.role-menu-page__editor {
  min-height: 26rem;
}

.role-menu-page__editor-heading {
  margin-bottom: 1.4rem;
}

.role-menu-page__empty,
.role-menu-page__loading {
  color: var(--color-ink-muted);
  font-size: 0.9rem;
}

.role-menu-page__loading {
  margin-top: 1.5rem;
}

@include at-most('tablet') {
  .role-menu-page__workspace {
    grid-template-columns: 1fr;
  }

  .role-menu-page__header,
  .role-menu-page__editor-heading {
    flex-direction: column;
  }
}
</style>
