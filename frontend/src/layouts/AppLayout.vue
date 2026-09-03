<!--
  @file AppLayout.vue
  @project Pipker Framework
  @module Frontend Application Layout
  @description Renders the authenticated shell and database-authorized menu navigation.
  @logic Flattens the current session menu tree for navigation without maintaining any static business menu or overview route.
  @dependencies Vue, Vue Router, Pinia app store, Pinia session store, Element Plus
  @index_tags layout, navigation, rbac, dynamic-routing
  @author holic512
-->
<script setup lang="ts">
import { computed } from 'vue'
import type { SystemMenuNode } from '../core/api/contracts'
import { useAppStore } from '../stores/app'
import { useSessionStore } from '../stores/session'

const appStore = useAppStore()
const sessionStore = useSessionStore()

const navigationItems = computed(() => flattenNavigation(sessionStore.menus))

function flattenNavigation(menus: SystemMenuNode[], depth = 0): Array<{
  id: number
  path: string
  code: string
  label: string
  description: string
  depth: number
}> {
  return menus.flatMap((menu) => {
    const children = flattenNavigation(menu.children, depth + 1)
    if (menu.type !== 'MENU' || !menu.path) {
      return children
    }
    return [
      {
        id: menu.id,
        path: menu.path,
        code: String(menu.id).padStart(2, '0'),
        label: menu.name,
        description: menu.permission ? `权限 · ${menu.permission}` : '已授权菜单',
        depth,
      },
      ...children,
    ]
  })
}
</script>

<template>
  <div class="app-shell" :class="{ 'app-shell--compact': appStore.sidebarCollapsed }">
    <aside class="app-shell__sidebar" aria-label="主导航">
      <div class="brand">
        <span class="brand__mark">P</span>
        <div v-show="!appStore.sidebarCollapsed" class="brand__name">
          <strong>PIPKER</strong>
          <span>CONTROL ROOM</span>
        </div>
      </div>

      <nav class="navigation">
        <RouterLink
          v-for="item in navigationItems"
          :key="item.id"
          :to="item.path"
          class="navigation__item"
          :style="{ '--navigation-depth': `${item.depth * 0.55}rem` }"
        >
          <span class="navigation__code">{{ item.code }}</span>
          <span v-show="!appStore.sidebarCollapsed" class="navigation__copy">
            <strong>{{ item.label }}</strong>
            <small>{{ item.description }}</small>
          </span>
        </RouterLink>
        <p v-if="navigationItems.length === 0" v-show="!appStore.sidebarCollapsed" class="navigation__empty">
          当前账户没有可装载的菜单。
        </p>
      </nav>

      <el-button
        class="collapse-control"
        text
        :aria-label="appStore.sidebarCollapsed ? '展开导航' : '收起导航'"
        @click="appStore.toggleSidebar"
      >
        <span>{{ appStore.sidebarCollapsed ? '→' : '←' }}</span>
        <span v-show="!appStore.sidebarCollapsed">收起导航</span>
      </el-button>
    </aside>

    <main class="app-shell__main">
      <header class="app-shell__header">
        <p>SYSTEM / {{ sessionStore.user?.username ?? 'SESSION' }}</p>
        <div class="app-shell__session-actions">
          <el-tag effect="plain" type="success">
            {{ sessionStore.roles.length }} 个角色 · {{ sessionStore.permissions.length }} 项权限
          </el-tag>
          <el-button text @click="sessionStore.logout">退出登录</el-button>
        </div>
      </header>
      <section class="app-shell__content">
        <RouterView />
      </section>
    </main>
  </div>
</template>

<style scoped>
.app-shell {
  min-height: 100svh;
  display: grid;
  grid-template-columns: 17.25rem minmax(0, 1fr);
  background: var(--surface-base);
  transition: grid-template-columns 180ms ease;
}

.app-shell--compact {
  grid-template-columns: 5rem minmax(0, 1fr);
}

.app-shell__sidebar {
  position: sticky;
  top: 0;
  height: 100svh;
  display: flex;
  flex-direction: column;
  padding: 1.4rem 0.8rem 1.25rem;
  box-sizing: border-box;
  color: #ecf0e7;
  background:
    linear-gradient(150deg, rgba(179, 220, 146, 0.09), transparent 32%),
    linear-gradient(180deg, #13211e 0%, #0d1715 100%);
  border-right: 1px solid rgba(216, 234, 203, 0.12);
}

.brand {
  min-height: 3rem;
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0 0.45rem;
  overflow: hidden;
}

.brand__mark {
  flex: 0 0 2.15rem;
  width: 2.15rem;
  height: 2.15rem;
  display: grid;
  place-items: center;
  color: #13211e;
  background: #d8edc5;
  font-family: var(--font-display);
  font-size: 1.3rem;
  font-weight: 800;
  border-radius: 0.35rem;
  box-shadow: 0 0 0 1px rgba(216, 237, 197, 0.3), 0 0.6rem 2rem rgba(0, 0, 0, 0.24);
}

.brand__name {
  display: grid;
  gap: 0.08rem;
  white-space: nowrap;
  letter-spacing: 0.08em;
}

.brand__name strong {
  font-family: var(--font-display);
  font-size: 0.87rem;
}

.brand__name span {
  color: rgba(236, 240, 231, 0.54);
  font-size: 0.59rem;
  font-weight: 700;
}

.navigation {
  margin-top: 3.25rem;
  display: grid;
  gap: 0.35rem;
}

.navigation__item {
  min-height: 3.85rem;
  display: flex;
  align-items: center;
  gap: 0.8rem;
  padding: 0.65rem 0.55rem 0.65rem calc(0.55rem + var(--navigation-depth, 0rem));
  color: rgba(236, 240, 231, 0.66);
  border: 1px solid transparent;
  border-radius: 0.4rem;
  text-decoration: none;
  transition: background 160ms ease, color 160ms ease, border-color 160ms ease;
}

.navigation__empty {
  margin: 0.8rem 0.55rem;
  color: rgba(236, 240, 231, 0.5);
  font-size: 0.73rem;
  line-height: 1.6;
}

.navigation__item:hover,
.navigation__item.router-link-exact-active {
  color: #f7faf2;
  background: rgba(216, 237, 197, 0.1);
  border-color: rgba(216, 237, 197, 0.17);
}

.navigation__code {
  flex: 0 0 1.7rem;
  color: #bed8a9;
  font-family: var(--font-mono);
  font-size: 0.68rem;
}

.navigation__copy {
  display: grid;
  gap: 0.28rem;
  min-width: 0;
}

.navigation__copy strong {
  font-size: 0.85rem;
  font-weight: 650;
}

.navigation__copy small {
  color: rgba(236, 240, 231, 0.48);
  font-size: 0.7rem;
}

.collapse-control {
  width: 100%;
  margin-top: auto;
  justify-content: flex-start;
  gap: 0.75rem;
  color: rgba(236, 240, 231, 0.68);
  font-size: 0.73rem;
}

.collapse-control:hover {
  color: #f7faf2;
}

.app-shell__main {
  min-width: 0;
}

.app-shell__header {
  min-height: 4.9rem;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 3.2rem;
  border-bottom: 1px solid var(--line-subtle);
}

.app-shell__header p {
  margin: 0;
  color: var(--ink-muted);
  font-family: var(--font-mono);
  font-size: 0.67rem;
  font-weight: 700;
  letter-spacing: 0.095em;
}

.app-shell__session-actions {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.app-shell__session-actions :deep(.el-button) {
  color: var(--ink-muted);
}

.app-shell__content {
  max-width: 90rem;
  margin: 0 auto;
  padding: clamp(2rem, 5vw, 5rem) clamp(1.25rem, 5vw, 4.5rem);
}

@media (max-width: 52rem) {
  .app-shell,
  .app-shell--compact {
    display: block;
  }

  .app-shell__sidebar {
    position: static;
    width: 100%;
    height: auto;
    min-height: 4.5rem;
    padding: 0.7rem 1rem;
    flex-direction: row;
    align-items: center;
    gap: 0.7rem;
  }

  .navigation {
    margin: 0;
    flex: 1;
  }

  .navigation__item {
    min-height: 2.65rem;
    padding: 0.35rem 0.5rem;
  }

  .navigation__copy small,
  .collapse-control span:last-child {
    display: none;
  }

  .collapse-control {
    width: auto;
    margin: 0;
  }

  .app-shell__header {
    min-height: 4rem;
    padding: 0 1.25rem;
  }

  .app-shell__session-actions :deep(.el-tag) {
    display: none;
  }
}
</style>
