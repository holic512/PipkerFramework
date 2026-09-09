<!--
  @file RoleMenuTree.vue
  @project Pipker Framework
  @module Frontend Role Management
  @description 在角色管理页面递归渲染数据库页面路由树并维护勾选状态。
  @logic 将 DIRECTORY 节点作为只读分类，标记隐藏导航页面，并仅保存精确的字符串 MENU 路由 ID，与角色管理页面权限接口保持一致。
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
          type="checkbox"
          @change="toggleMenu(menu.id, $event)"
        />
        <span>{{ menu.name }}</span>
        <small>{{ menu.path }}</small>
        <em v-if="!menu.visible">隐藏菜单</em>
      </label>
      <p v-else class="role-menu-tree__directory">{{ menu.name }}</p>
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
  gap: 0.5rem;
  margin: 0;
  padding: 0 0 0 1.1rem;
  list-style: none;
}

.role-menu-tree__node > .role-menu-tree {
  margin-top: 0.45rem;
  border-left: 1px solid var(--color-line-subtle);
}

.role-menu-tree__directory,
.role-menu-tree__page {
  margin: 0;
}

.role-menu-tree__directory {
  color: var(--color-ink-muted);
  font-size: 0.74rem;
  font-weight: 800;
  letter-spacing: 0.06em;
}

.role-menu-tree__page {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto auto;
  align-items: center;
  gap: 0.62rem;
  min-height: 2.55rem;
  padding: 0.45rem 0.65rem;
  cursor: pointer;
  background: var(--color-surface-base);
  border: 1px solid var(--color-line-subtle);
  border-radius: var(--radius-small);
}

.role-menu-tree__page:has(input:checked) {
  background: color-mix(in srgb, var(--color-accent-primary) 10%, var(--color-surface-base));
  border-color: var(--color-accent-primary);
}

.role-menu-tree__page span {
  min-width: 0;
  font-size: 0.86rem;
  font-weight: 750;
}

.role-menu-tree__page small {
  color: var(--color-ink-soft);
  font-family: var(--font-family);
  font-size: 0.64rem;
}

.role-menu-tree__page em {
  padding: 0.16rem 0.32rem;
  color: var(--color-ink-soft);
  background: var(--color-surface-muted);
  border-radius: var(--radius-pill);
  font-size: 0.6rem;
  font-style: normal;
  white-space: nowrap;
}

@include at-most('phone') {
  .role-menu-tree__page {
    grid-template-columns: auto minmax(0, 1fr);
  }

  .role-menu-tree__page small {
    grid-column: 2;
  }

  .role-menu-tree__page em {
    grid-column: 2;
    justify-self: start;
  }
}
</style>
