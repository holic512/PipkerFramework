<!--
  @file RoleMenuTree.vue
  @project Pipker Framework
  @module Frontend Role Route Management
  @description Renders the database menu hierarchy as a recursive page-menu selection tree.
  @logic Makes DIRECTORY nodes informational and persists only MENU leaf IDs, matching the backend role-menu contract.
  @dependencies Vue, SystemMenuNode
  @index_tags rbac, role-menu, tree, form
  @author holic512
-->
<script setup lang="ts">
import { computed } from 'vue'
import type { SystemMenuNode } from '../../../../core/api/contracts'

defineOptions({ name: 'RoleMenuTree' })

const props = defineProps<{
  menus: SystemMenuNode[]
  modelValue: number[]
  disabled?: boolean
}>()

const emit = defineEmits<{
  'update:modelValue': [menuIds: number[]]
}>()

const selectedMenuIds = computed(() => new Set(props.modelValue))

function toggleMenu(menuId: number, event: Event): void {
  const checked = (event.target as HTMLInputElement).checked
  const nextMenuIds = new Set(props.modelValue)
  if (checked) {
    nextMenuIds.add(menuId)
  } else {
    nextMenuIds.delete(menuId)
  }
  emit('update:modelValue', [...nextMenuIds].sort((left, right) => left - right))
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

<style scoped>
.role-menu-tree {
  display: grid;
  gap: 0.5rem;
  margin: 0;
  padding: 0 0 0 1.1rem;
  list-style: none;
}

.role-menu-tree__node > .role-menu-tree {
  margin-top: 0.45rem;
  border-left: 1px solid var(--line-subtle);
}

.role-menu-tree__directory,
.role-menu-tree__page {
  margin: 0;
}

.role-menu-tree__directory {
  color: var(--ink-muted);
  font-size: 0.74rem;
  font-weight: 800;
  letter-spacing: 0.06em;
}

.role-menu-tree__page {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  align-items: center;
  gap: 0.62rem;
  min-height: 2.55rem;
  padding: 0.45rem 0.65rem;
  cursor: pointer;
  background: #fbfcf8;
  border: 1px solid var(--line-subtle);
  border-radius: 0.36rem;
}

.role-menu-tree__page:has(input:checked) {
  background: #f0f7eb;
  border-color: #b9d4ad;
}

.role-menu-tree__page span {
  min-width: 0;
  font-size: 0.86rem;
  font-weight: 750;
}

.role-menu-tree__page small {
  color: var(--ink-soft);
  font-family: var(--font-mono);
  font-size: 0.64rem;
}

@media (max-width: 40rem) {
  .role-menu-tree__page {
    grid-template-columns: auto minmax(0, 1fr);
  }

  .role-menu-tree__page small {
    grid-column: 2;
  }
}
</style>
