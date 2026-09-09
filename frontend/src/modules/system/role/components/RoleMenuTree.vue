<!--
  @file RoleMenuTree.vue
  @project Pipker Framework
  @module Frontend Role Management
  @description 在角色管理权限工作台中递归渲染经过筛选的数据库页面路由树并维护勾选状态。
  @logic 将 DIRECTORY 节点作为只读分类，使用紧凑的页面名称、路径和隐藏状态提示，并仅保存精确的字符串 MENU 路由 ID，与角色管理页面权限接口保持一致。
  @dependencies Vue, SystemMenuNode
  @index_tags rbac, role, tree, form, route, snowflake-id
  @author holic512
-->
<script setup lang="ts">
import { computed } from 'vue'
import type { SystemMenuNode } from '../../../../core/api/contracts'

defineOptions({ name: 'RoleMenuTree' })

const props = defineProps<{
  menus: SystemMenuNode[]
  modelValue: string[]
  disabled?: boolean
}>()

const emit = defineEmits<{
  'update:modelValue': [menuIds: string[]]
}>()

const selectedMenuIds = computed(() => new Set(props.modelValue))

function toggleMenu(menuId: string, event: Event): void {
  const checked = (event.target as HTMLInputElement).checked
  const nextMenuIds = new Set(props.modelValue)
  if (checked) {
    nextMenuIds.add(menuId)
  } else {
    nextMenuIds.delete(menuId)
  }
  emit('update:modelValue', [...nextMenuIds].sort((left, right) => left.localeCompare(right)))
}
</script>

<template>
  <ul class="role-menu-tree">
    <li v-for="menu in menus" :key="menu.id" class="role-menu-tree__node">
      <label v-if="menu.type === 'MENU'" class="role-menu-tree__page">
        <input
          :checked="selectedMenuIds.has(menu.id)"
          :disabled="disabled"
          :aria-label="`选择页面：${menu.name}`"
          type="checkbox"
          @change="toggleMenu(menu.id, $event)"
        />
        <span class="role-menu-tree__name">{{ menu.name }}</span>
        <small :title="menu.path ?? undefined">{{ menu.path }}</small>
        <span v-if="!menu.visible" aria-label="隐藏菜单" class="role-menu-tree__hidden" title="隐藏菜单">隐藏</span>
      </label>
      <p v-else class="role-menu-tree__directory">
        <span aria-hidden="true" class="role-menu-tree__directory-mark" />
        {{ menu.name }}
      </p>
      <RoleMenuTree
        v-if="menu.children.length > 0"
        :disabled="disabled"
        :menus="menu.children"
        :model-value="modelValue"
        @update:model-value="emit('update:modelValue', $event)"
      />
    </li>
  </ul>
</template>

<style scoped lang="scss">
.role-menu-tree {
  display: grid;
  gap: 0.35rem;
  margin: 0;
  padding: 0 0 0 0.85rem;
  list-style: none;
}

.role-menu-tree__node > .role-menu-tree {
  margin-top: 0.35rem;
  border-left: 1px solid var(--color-line-subtle);
}

.role-menu-tree__directory,
.role-menu-tree__page {
  margin: 0;
}

.role-menu-tree__directory {
  display: flex;
  align-items: center;
  gap: 0.38rem;
  min-height: 1.55rem;
  color: var(--color-ink-muted);
  font-size: 0.7rem;
  font-weight: 760;
}

.role-menu-tree__directory-mark {
  width: 0.42rem;
  height: 0.42rem;
  flex: 0 0 auto;
  border: 1px solid var(--color-ink-soft);
  border-radius: 0.08rem;
}

.role-menu-tree__page {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  align-items: center;
  gap: 0.48rem;
  min-height: 2.2rem;
  padding: 0.34rem 0.55rem;
  cursor: pointer;
  background: var(--color-surface-base);
  border: 1px solid var(--color-line-subtle);
  border-radius: var(--radius-small);
}

.role-menu-tree__page:has(input:checked) {
  background: color-mix(in srgb, var(--color-accent-primary) 10%, var(--color-surface-base));
  border-color: var(--color-accent-primary);
}

.role-menu-tree__page:focus-within {
  outline: 2px solid var(--color-focus-ring);
  outline-offset: 0.12rem;
}

.role-menu-tree__name {
  min-width: 0;
  overflow: hidden;
  color: var(--color-ink-strong);
  font-size: 0.78rem;
  font-weight: 700;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.role-menu-tree__page small {
  min-width: 0;
  max-width: 18rem;
  overflow: hidden;
  color: var(--color-ink-soft);
  font-family: var(--font-family);
  font-size: 0.61rem;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.role-menu-tree__hidden {
  padding: 0.12rem 0.28rem;
  color: var(--color-ink-soft);
  background: var(--color-surface-muted);
  border-radius: var(--radius-pill);
  font-size: 0.57rem;
  white-space: nowrap;
}

@include at-most('phone') {
  .role-menu-tree__page {
    grid-template-columns: auto minmax(0, 1fr);
  }

  .role-menu-tree__page small {
    grid-column: 2;
    max-width: none;
  }

  .role-menu-tree__hidden {
    grid-column: 2;
    justify-self: start;
  }
}
</style>
