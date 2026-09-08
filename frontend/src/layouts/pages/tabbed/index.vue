<!--
  @file index.vue
  @project Pipker Framework
  @module 页签应用布局
  @description 提供顶部工作区栏、授权页签与紧凑侧导航组成的第二套登录后控制台布局。
  @logic 基于同一份会话菜单树生成侧导航和页签入口；仅改变信息架构与响应式交互，不修改动态路由、菜单授权或会话状态。
  @dependencies Vue、Vue Router、Pinia 会话 Store、ThemeSwitcher、Element Plus、系统菜单类型
  @index_tags 布局、页签、导航、RBAC、动态路由、主题
  @author holic512
-->
<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute } from 'vue-router'
import type { SystemMenuNode } from '../../../core/api/contracts'
import ThemeSwitcher from '../../../components/ThemeSwitcher.vue'
import { useSessionStore } from '../../../stores/session'

const route = useRoute()
const sessionStore = useSessionStore()
const mobileNavigationOpen = ref(false)

const navigationItems = computed(() => flattenNavigation(sessionStore.menus))
const activePageName = computed(() => (
  navigationItems.value.find((item) => item.path === route.path)?.label ?? '授权工作区'
))

function closeMobileNavigation(): void {
  mobileNavigationOpen.value = false
}

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
        description: '已授权页面',
        depth,
      },
      ...children,
    ]
  })
}
</script>

<template>
  <div class="tabbed-shell" :class="{ 'tabbed-shell--navigation-open': mobileNavigationOpen }">
    <header class="tabbed-shell__header">
      <div class="tabbed-brand">
        <img class="tabbed-brand__logo" src="/brand/pipker-logo.webp" alt="Pipker" />
        <strong>PIPKER</strong>
        <span>CONTROL ROOM</span>
      </div>

      <p class="tabbed-shell__breadcrumb">
        <span>WORKSPACE</span>
        <i aria-hidden="true">/</i>
        {{ activePageName }}
      </p>

      <div class="tabbed-shell__actions">
        <el-tag effect="plain" type="success">
          {{ sessionStore.roles.length }} 个角色 · {{ sessionStore.permissions.length }} 项权限
        </el-tag>
        <ThemeSwitcher />
        <el-button text @click="sessionStore.logout">退出登录</el-button>
        <button
          class="tabbed-shell__navigation-toggle ui-focusable"
          type="button"
          :aria-expanded="mobileNavigationOpen"
          aria-controls="tabbed-navigation"
          @click="mobileNavigationOpen = !mobileNavigationOpen"
        >
          <span aria-hidden="true">☰</span>
          导航
        </button>
      </div>
    </header>

    <aside id="tabbed-navigation" class="tabbed-shell__sidebar" aria-label="已授权页面导航">
      <div class="tabbed-shell__sidebar-heading">
        <span>AUTHORIZED PAGES</span>
        <b>{{ navigationItems.length }}</b>
      </div>

      <nav class="tabbed-navigation">
        <RouterLink
          v-for="item in navigationItems"
          :key="item.id"
          :to="item.path"
          class="tabbed-navigation__item"
          :style="{ '--navigation-depth': `${item.depth * 0.55}rem` }"
          @click="closeMobileNavigation"
        >
          <span class="tabbed-navigation__code">{{ item.code }}</span>
          <span class="tabbed-navigation__copy">
            <strong>{{ item.label }}</strong>
            <small>{{ item.description }}</small>
          </span>
          <i aria-hidden="true">→</i>
        </RouterLink>
        <p v-if="navigationItems.length === 0" class="tabbed-navigation__empty">
          当前账户没有可装载的菜单。
        </p>
      </nav>

      <div class="tabbed-shell__sidebar-foot">
        <span>SESSION</span>
        <strong>{{ sessionStore.user?.username ?? 'SYSTEM' }}</strong>
      </div>
    </aside>

    <main class="tabbed-shell__main">
      <nav class="tabbed-tabs" aria-label="授权页面页签">
        <RouterLink
          v-for="item in navigationItems"
          :key="item.id"
          :to="item.path"
          class="tabbed-tabs__item ui-focusable"
        >
          <span>{{ item.code }}</span>
          {{ item.label }}
        </RouterLink>
        <span v-if="navigationItems.length === 0" class="tabbed-tabs__empty">等待授权菜单装载</span>
      </nav>

      <section class="tabbed-workspace">
        <div class="tabbed-workspace__label">
          <p>ACTIVE WORKSPACE</p>
          <span>{{ activePageName }}</span>
        </div>
        <RouterView />
      </section>

      <footer class="tabbed-shell__footer">
        <span>PIPKER FRAMEWORK</span>
        <p>DATABASE-DRIVEN AUTHORIZED NAVIGATION</p>
      </footer>
    </main>
  </div>
</template>

<style scoped lang="scss">
.tabbed-shell {
  min-height: 100svh;
  display: grid;
  grid-template-areas:
    'header header'
    'sidebar main';
  grid-template-columns: 15.75rem minmax(0, 1fr);
  grid-template-rows: 3.85rem minmax(0, 1fr);
  color: var(--color-ink-strong);
  background: var(--color-surface-page);
}

.tabbed-shell__header {
  grid-area: header;
  min-width: 0;
  display: flex;
  align-items: center;
  gap: 1.25rem;
  padding: 0 1rem;
  background: var(--color-surface-base);
  border-bottom: 1px solid var(--color-line-subtle);
}

.tabbed-brand {
  flex: 0 0 auto;
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  min-width: 13.25rem;
  color: var(--color-ink-strong);
}

.tabbed-brand__logo {
  width: 1.82rem;
  height: 1.82rem;
  display: block;
  object-fit: cover;
  border-radius: var(--radius-small);
  box-shadow: var(--shadow-panel);
}

.tabbed-brand strong {
  font-size: 0.88rem;
  font-weight: 850;
  letter-spacing: 0.11em;
}

.tabbed-brand span,
.tabbed-shell__breadcrumb,
.tabbed-shell__sidebar-heading,
.tabbed-shell__sidebar-foot span,
.tabbed-workspace__label p,
.tabbed-shell__footer {
  font-size: 0.62rem;
  font-weight: 760;
  letter-spacing: 0.09em;
}

.tabbed-brand span {
  color: var(--color-ink-soft);
}

.tabbed-shell__breadcrumb {
  display: inline-flex;
  gap: 0.45rem;
  min-width: 0;
  margin: 0;
  color: var(--color-ink-muted);
  white-space: nowrap;
}

.tabbed-shell__breadcrumb span {
  color: var(--color-accent-primary);
}

.tabbed-shell__breadcrumb i {
  color: var(--color-ink-soft);
  font-style: normal;
}

.tabbed-shell__actions {
  display: flex;
  align-items: center;
  gap: 0.55rem;
  margin-left: auto;
}

.tabbed-shell__actions :deep(.el-button) {
  color: var(--color-ink-muted);
}

.tabbed-shell__navigation-toggle {
  min-height: 2.1rem;
  display: none;
  align-items: center;
  gap: 0.35rem;
  padding: 0.3rem 0.52rem;
  color: var(--color-ink-strong);
  cursor: pointer;
  background: var(--color-surface-raised);
  border: 1px solid var(--color-line-subtle);
  border-radius: var(--radius-control);
  font-size: 0.72rem;
  font-weight: 720;
}

.tabbed-shell__sidebar {
  grid-area: sidebar;
  min-width: 0;
  display: flex;
  flex-direction: column;
  padding: 1rem 0.6rem 0.75rem;
  background: var(--color-surface-base);
  border-right: 1px solid var(--color-line-subtle);
}

.tabbed-shell__sidebar-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 0.55rem 0.8rem;
  color: var(--color-ink-soft);
}

.tabbed-shell__sidebar-heading b {
  min-width: 1.3rem;
  height: 1.3rem;
  display: grid;
  place-items: center;
  color: var(--color-accent-on-primary);
  background: var(--color-accent-primary);
  border-radius: var(--radius-small);
  font-size: 0.62rem;
}

.tabbed-navigation {
  display: grid;
  gap: 0.18rem;
  min-height: 0;
  overflow-y: auto;
}

.tabbed-navigation__item {
  min-height: 3.3rem;
  display: grid;
  grid-template-columns: 1.55rem minmax(0, 1fr) auto;
  gap: 0.5rem;
  align-items: center;
  padding: 0.45rem 0.5rem 0.45rem calc(0.5rem + var(--navigation-depth, 0rem));
  color: var(--color-ink-muted);
  border: 1px solid transparent;
  border-radius: var(--radius-control);
  text-decoration: none;
  transition: color 160ms ease, background-color 160ms ease, border-color 160ms ease, transform 160ms ease;
}

.tabbed-navigation__item:hover {
  color: var(--color-ink-strong);
  background: var(--color-surface-muted);
  border-color: var(--color-line-subtle);
  transform: translateX(0.12rem);
}

.tabbed-navigation__item.router-link-exact-active {
  color: var(--color-accent-on-primary);
  background: var(--color-accent-primary);
  border-color: var(--color-accent-primary);
}

.tabbed-navigation__code {
  width: 1.4rem;
  height: 1.4rem;
  display: grid;
  place-items: center;
  color: currentColor;
  border: 1px solid currentColor;
  border-radius: var(--radius-small);
  font-size: 0.58rem;
  font-weight: 800;
  opacity: 0.78;
}

.tabbed-navigation__copy {
  min-width: 0;
  display: grid;
  gap: 0.18rem;
}

.tabbed-navigation__copy strong {
  overflow: hidden;
  font-size: 0.78rem;
  font-weight: 710;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.tabbed-navigation__copy small {
  overflow: hidden;
  font-size: 0.61rem;
  text-overflow: ellipsis;
  white-space: nowrap;
  opacity: 0.66;
}

.tabbed-navigation__item > i {
  font-size: 0.75rem;
  font-style: normal;
  opacity: 0;
  transform: translateX(-0.18rem);
  transition: opacity 160ms ease, transform 160ms ease;
}

.tabbed-navigation__item:hover > i,
.tabbed-navigation__item.router-link-exact-active > i {
  opacity: 1;
  transform: translateX(0);
}

.tabbed-navigation__empty {
  padding: 0.85rem 0.55rem;
  margin: 0;
  color: var(--color-ink-soft);
  font-size: 0.72rem;
  line-height: 1.65;
}

.tabbed-shell__sidebar-foot {
  display: grid;
  gap: 0.28rem;
  padding: 0.9rem 0.55rem 0.2rem;
  margin-top: auto;
  border-top: 1px solid var(--color-line-subtle);
}

.tabbed-shell__sidebar-foot span {
  color: var(--color-ink-soft);
}

.tabbed-shell__sidebar-foot strong {
  overflow: hidden;
  color: var(--color-ink-muted);
  font-size: 0.75rem;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.tabbed-shell__main {
  grid-area: main;
  min-width: 0;
  display: grid;
  grid-template-rows: auto minmax(0, 1fr) auto;
}

.tabbed-tabs {
  display: flex;
  gap: 0.35rem;
  min-width: 0;
  overflow-x: auto;
  padding: 0.6rem 1rem;
  background: var(--color-surface-raised);
  border-bottom: 1px solid var(--color-line-subtle);
  scrollbar-width: thin;
}

.tabbed-tabs__item {
  min-height: 2.05rem;
  display: inline-flex;
  flex: 0 0 auto;
  align-items: center;
  gap: 0.44rem;
  padding: 0.32rem 0.64rem;
  color: var(--color-ink-muted);
  background: var(--color-surface-base);
  border: 1px solid var(--color-line-subtle);
  border-radius: var(--radius-small);
  font-size: 0.72rem;
  font-weight: 700;
  text-decoration: none;
  transition: color 160ms ease, background-color 160ms ease, border-color 160ms ease;
}

.tabbed-tabs__item span {
  color: var(--color-ink-soft);
  font-size: 0.58rem;
  font-weight: 850;
}

.tabbed-tabs__item:hover {
  color: var(--color-ink-strong);
  border-color: var(--color-line-strong);
}

.tabbed-tabs__item.router-link-exact-active {
  color: var(--color-accent-on-primary);
  background: var(--color-accent-primary);
  border-color: var(--color-accent-primary);
}

.tabbed-tabs__item.router-link-exact-active span {
  color: inherit;
  opacity: 0.72;
}

.tabbed-tabs__empty {
  align-self: center;
  padding: 0 0.25rem;
  color: var(--color-ink-soft);
  font-size: 0.7rem;
}

.tabbed-workspace {
  min-width: 0;
  max-width: 100rem;
  width: 100%;
  padding: clamp(1.25rem, 3vw, 2.75rem);
  margin: 0 auto;
}

.tabbed-workspace__label {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  padding-bottom: 0.85rem;
  margin-bottom: 1.25rem;
  border-bottom: 1px solid var(--color-line-subtle);
}

.tabbed-workspace__label p {
  margin: 0;
  color: var(--color-ink-soft);
}

.tabbed-workspace__label span {
  overflow: hidden;
  color: var(--color-ink-muted);
  font-size: 0.78rem;
  font-weight: 700;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.tabbed-shell__footer {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  padding: 0.9rem clamp(1.25rem, 3vw, 2.75rem);
  color: var(--color-ink-soft);
  background: var(--color-surface-base);
  border-top: 1px solid var(--color-line-subtle);
}

.tabbed-shell__footer p {
  margin: 0;
}

@include at-most('tablet') {
  .tabbed-shell {
    display: block;
  }

  .tabbed-shell__header {
    min-height: 3.75rem;
  }

  .tabbed-brand {
    min-width: 0;
  }

  .tabbed-brand span,
  .tabbed-shell__breadcrumb,
  .tabbed-shell__actions :deep(.el-tag),
  .tabbed-shell__actions :deep(.el-button) {
    display: none;
  }

  .tabbed-shell__navigation-toggle {
    display: inline-flex;
  }

  .tabbed-shell__sidebar {
    width: min(18rem, calc(100vw - 2rem));
    height: calc(100svh - 3.75rem);
    position: fixed;
    top: 3.75rem;
    left: 0;
    z-index: 20;
    box-shadow: var(--shadow-floating);
    transform: translateX(-104%);
    transition: transform 200ms ease;
  }

  .tabbed-shell--navigation-open .tabbed-shell__sidebar {
    transform: translateX(0);
  }

  .tabbed-shell__main {
    min-height: calc(100svh - 3.75rem);
  }
}

@include at-most('phone') {
  .tabbed-shell__header {
    padding: 0 0.75rem;
  }

  .tabbed-brand strong {
    font-size: 0.76rem;
  }

  .tabbed-shell__actions {
    gap: 0.3rem;
  }

  .tabbed-tabs {
    padding: 0.5rem 0.75rem;
  }

  .tabbed-workspace {
    padding: 1rem 0.75rem;
  }

  .tabbed-shell__footer {
    flex-direction: column;
    padding: 0.8rem 0.75rem;
    font-size: 0.54rem;
  }
}
</style>
