<!--
  @file index.vue
  @project Pipker Framework
  @module 侧栏应用布局
  @description 渲染全高分组侧栏、轻量面包屑顶栏与数据库授权页面工作区。
  @logic 将授权目录呈现为分组标题、页面呈现为路由入口，并协调桌面折叠、移动抽屉、头像回退及会话操作。
  @dependencies Vue、Vue Router、Pinia 应用 Store、Pinia 会话 Store、布局导航模型、ThemeSwitcher、Element Plus 图标
  @index_tags 布局、侧栏、导航分组、面包屑、RBAC、响应式、主题
  @author holic512
-->
<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { Expand, Fold, House, SwitchButton, User } from '@element-plus/icons-vue'
import { useRoute } from 'vue-router'
import ThemeSwitcher from '../../../components/ThemeSwitcher.vue'
import { runtimeConfig } from '../../../core/config/runtime'
import { useAppStore } from '../../../stores/app'
import { useSessionStore } from '../../../stores/session'
import {
  buildLayoutNavigation,
  findActiveNavigationEntry,
  getNavigationTrail,
  getPageNavigationEntries,
} from '../../model/navigation'

const route = useRoute()
const appStore = useAppStore()
const sessionStore = useSessionStore()
const mobileNavigationOpen = ref(false)
const avatarLoadFailed = ref(false)

const navigationEntries = computed(() => buildLayoutNavigation(sessionStore.menus))
const pageEntries = computed(() => getPageNavigationEntries(navigationEntries.value))
const activeEntry = computed(() => findActiveNavigationEntry(
  navigationEntries.value,
  route.meta.routeId,
  route.path,
))
const navigationTrail = computed(() => getNavigationTrail(navigationEntries.value, activeEntry.value))
const userDisplayName = computed(() => (
  sessionStore.user?.nickname?.trim()
  || sessionStore.user?.username
  || 'SYSTEM'
))
const userInitial = computed(() => userDisplayName.value.slice(0, 1).toUpperCase())

watch(() => route.fullPath, closeMobileNavigation)
watch(() => sessionStore.user?.avatar, () => {
  avatarLoadFailed.value = false
})

onMounted(() => {
  window.addEventListener('keydown', handleWindowKeydown)
})

onBeforeUnmount(() => {
  window.removeEventListener('keydown', handleWindowKeydown)
})

function closeMobileNavigation(): void {
  mobileNavigationOpen.value = false
}

function handleWindowKeydown(event: KeyboardEvent): void {
  if (event.key === 'Escape') {
    closeMobileNavigation()
  }
}

function markAvatarLoadFailed(): void {
  avatarLoadFailed.value = true
}
</script>

<template>
  <div
    class="sidebar-layout"
    :class="{
      'sidebar-layout--compact': appStore.sidebarCollapsed,
      'sidebar-layout--navigation-open': mobileNavigationOpen,
    }"
  >
    <aside id="sidebar-layout-navigation" class="sidebar-layout__sidebar" aria-label="主导航">
      <RouterLink class="sidebar-layout__brand ui-focusable" to="/" :aria-label="`${runtimeConfig.appName} 首页`">
        <img class="sidebar-layout__brand-mark" src="/brand/pipker-logo.webp" alt="" />
        <span class="sidebar-layout__brand-copy">
          <strong>{{ runtimeConfig.appName }}</strong>
          <small>管理控制台</small>
        </span>
      </RouterLink>

      <nav class="sidebar-navigation" aria-label="授权页面">
        <template v-for="entry in navigationEntries" :key="entry.id">
          <p
            v-if="entry.type === 'DIRECTORY'"
            class="sidebar-navigation__group"
            :style="{ '--navigation-depth': `${entry.depth * 0.65}rem` }"
            :title="entry.label"
          >
            <span class="sidebar-navigation__icon" aria-hidden="true">
              <component :is="entry.icon" />
            </span>
            <span class="sidebar-navigation__group-label">{{ entry.label }}</span>
          </p>

          <RouterLink
            v-else-if="entry.path"
            :to="entry.path"
            class="sidebar-navigation__item ui-focusable"
            :style="{ '--navigation-depth': `${entry.depth * 0.65}rem` }"
            :aria-label="entry.label"
            :title="entry.label"
            @click="closeMobileNavigation"
          >
            <span class="sidebar-navigation__icon" aria-hidden="true">
              <component :is="entry.icon" />
            </span>
            <span class="sidebar-navigation__copy">{{ entry.label }}</span>
          </RouterLink>
        </template>

        <p v-if="pageEntries.length === 0" class="sidebar-navigation__empty">
          当前账户没有可装载的菜单。
        </p>
      </nav>

      <button
        class="sidebar-layout__collapse ui-focusable"
        type="button"
        :aria-label="appStore.sidebarCollapsed ? '展开侧边栏' : '收起侧边栏'"
        @click="appStore.toggleSidebar"
      >
        <component :is="appStore.sidebarCollapsed ? Expand : Fold" aria-hidden="true" />
        <span>收起侧边栏</span>
      </button>
    </aside>

    <button
      class="sidebar-layout__backdrop"
      type="button"
      aria-label="关闭导航"
      @click="closeMobileNavigation"
    ></button>

    <main class="sidebar-layout__main">
      <header class="sidebar-layout__header">
        <button
          class="sidebar-layout__mobile-toggle ui-focusable"
          type="button"
          :aria-expanded="mobileNavigationOpen"
          aria-controls="sidebar-layout-navigation"
          :aria-label="mobileNavigationOpen ? '关闭导航' : '打开导航'"
          @click="mobileNavigationOpen = !mobileNavigationOpen"
        >
          <Expand aria-hidden="true" />
        </button>

        <nav class="sidebar-breadcrumb" aria-label="面包屑导航">
          <RouterLink class="ui-focusable" to="/">管理控制台</RouterLink>
          <template v-if="navigationTrail.length > 0">
            <template v-for="entry in navigationTrail" :key="entry.id">
              <span class="sidebar-breadcrumb__separator" aria-hidden="true">/</span>
              <span>{{ entry.label }}</span>
            </template>
          </template>
          <template v-else>
            <span class="sidebar-breadcrumb__separator" aria-hidden="true">/</span>
            <span>授权工作区</span>
          </template>
        </nav>

        <div class="sidebar-layout__actions">
          <RouterLink class="sidebar-layout__icon-action ui-focusable" to="/" aria-label="返回公开首页" title="返回公开首页">
            <House aria-hidden="true" />
          </RouterLink>
          <ThemeSwitcher compact />
          <div class="sidebar-layout__user" :title="userDisplayName">
            <img
              v-if="sessionStore.user?.avatar && !avatarLoadFailed"
              :key="sessionStore.user.avatar"
              :src="sessionStore.user.avatar"
              alt=""
              @error="markAvatarLoadFailed"
            />
            <span v-else class="sidebar-layout__user-fallback" aria-hidden="true">
              <User v-if="!userInitial" />
              <template v-else>{{ userInitial }}</template>
            </span>
            <span class="sidebar-layout__user-name">{{ userDisplayName }}</span>
          </div>
          <button
            class="sidebar-layout__icon-action ui-focusable"
            type="button"
            aria-label="退出登录"
            title="退出登录"
            @click="sessionStore.logout"
          >
            <SwitchButton aria-hidden="true" />
          </button>
        </div>
      </header>

      <section class="sidebar-layout__content">
        <RouterView />
      </section>
    </main>
  </div>
</template>

<style scoped lang="scss" src="@/styles/layout/pages/sidebar/index.scss"></style>
