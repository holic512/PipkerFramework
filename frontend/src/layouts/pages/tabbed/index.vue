<!--
  @file index.vue
  @project Pipker Framework
  @module 页签应用布局
  @description 渲染全宽顶栏、可折叠授权菜单树、访问历史页签与独立滚动工作区。
  @logic 从数据库菜单派生目录、面包屑和会话内页签，协调目录展开、页签关闭回退、桌面折叠与移动抽屉。
  @dependencies Vue、Vue Router、Pinia 应用 Store、Pinia 会话 Store、布局导航模型、ThemeSwitcher、Element Plus 图标
  @index_tags 布局、页签、访问历史、菜单树、面包屑、RBAC、响应式、主题
  @author holic512
-->
<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import {
  ArrowDown,
  Close,
  Expand,
  Fold,
  House,
  SwitchButton,
  User,
} from '@element-plus/icons-vue'
import { useRoute, useRouter } from 'vue-router'
import ThemeSwitcher from '../../../components/ThemeSwitcher.vue'
import { useAppStore } from '../../../stores/app'
import { useSessionStore } from '../../../stores/session'
import {
  buildLayoutNavigation,
  findActiveNavigationEntry,
  getDirectoryNavigationEntries,
  getNavigationTrail,
  getPageNavigationEntries,
  type LayoutNavigationEntry,
} from '../../model/navigation'

interface VisitedLayoutTab {
  id: number
  label: string
  path: string
  icon: LayoutNavigationEntry['icon']
}

const route = useRoute()
const router = useRouter()
const appStore = useAppStore()
const sessionStore = useSessionStore()
const mobileNavigationOpen = ref(false)
const avatarLoadFailed = ref(false)
const expandedDirectoryIds = ref<Set<number>>(new Set())
const visitedTabs = ref<VisitedLayoutTab[]>([])

const navigationEntries = computed(() => buildLayoutNavigation(sessionStore.menus))
const pageEntries = computed(() => getPageNavigationEntries(navigationEntries.value))
const directoryEntries = computed(() => getDirectoryNavigationEntries(navigationEntries.value))
const pinnedEntry = computed(() => pageEntries.value[0] ?? null)
const activeEntry = computed(() => findActiveNavigationEntry(
  navigationEntries.value,
  route.meta.menuId,
  route.path,
))
const navigationTrail = computed(() => getNavigationTrail(navigationEntries.value, activeEntry.value))
const visibleNavigationEntries = computed(() => navigationEntries.value.filter((entry) => (
  entry.ancestorIds.every((directoryId) => expandedDirectoryIds.value.has(directoryId))
)))
const userDisplayName = computed(() => (
  sessionStore.user?.nickname?.trim()
  || sessionStore.user?.username
  || 'SYSTEM'
))
const userInitial = computed(() => userDisplayName.value.slice(0, 1).toUpperCase())
const authorizationSummary = computed(() => (
  `${sessionStore.roles[0] ?? '已授权'} · ${sessionStore.permissions.length} 项权限`
))

watch([pageEntries, activeEntry], synchronizeNavigationState, { immediate: true })
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

function synchronizeNavigationState(): void {
  const authorizedIds = new Set(pageEntries.value.map((entry) => entry.id))
  const nextTabs = visitedTabs.value.filter((tab) => authorizedIds.has(tab.id))
  const pinnedPage = pinnedEntry.value
  const activePage = activeEntry.value

  if (pinnedPage?.path) {
    const existingPinnedIndex = nextTabs.findIndex((tab) => tab.id === pinnedPage.id)
    if (existingPinnedIndex >= 0) {
      const [existingPinnedTab] = nextTabs.splice(existingPinnedIndex, 1)
      if (existingPinnedTab) {
        nextTabs.unshift(existingPinnedTab)
      }
    } else {
      nextTabs.unshift(toVisitedTab(pinnedPage))
    }
  }

  if (activePage?.path && !nextTabs.some((tab) => tab.id === activePage.id)) {
    nextTabs.push(toVisitedTab(activePage))
  }
  visitedTabs.value = nextTabs

  const validDirectoryIds = new Set(directoryEntries.value.map((entry) => entry.id))
  const nextExpandedIds = new Set(
    [...expandedDirectoryIds.value].filter((directoryId) => validDirectoryIds.has(directoryId)),
  )
  for (const directoryId of activePage?.ancestorIds ?? []) {
    nextExpandedIds.add(directoryId)
  }
  expandedDirectoryIds.value = nextExpandedIds

  if (!activePage && pinnedPage?.path && typeof route.meta.menuId === 'number') {
    void router.replace(pinnedPage.path)
  }
}

function toVisitedTab(entry: LayoutNavigationEntry): VisitedLayoutTab {
  return {
    id: entry.id,
    label: entry.label,
    path: entry.path ?? '/',
    icon: entry.icon,
  }
}

function toggleDirectory(directoryId: number): void {
  const nextExpandedIds = new Set(expandedDirectoryIds.value)
  if (nextExpandedIds.has(directoryId)) {
    nextExpandedIds.delete(directoryId)
  } else {
    nextExpandedIds.add(directoryId)
  }
  expandedDirectoryIds.value = nextExpandedIds
}

function isDirectoryExpanded(directoryId: number): boolean {
  return expandedDirectoryIds.value.has(directoryId)
}

function navigateToTab(tab: VisitedLayoutTab): void {
  if (tab.path !== route.path) {
    void router.push(tab.path)
  }
}

function closeTab(tabId: number): void {
  if (tabId === pinnedEntry.value?.id) {
    return
  }

  const closingIndex = visitedTabs.value.findIndex((tab) => tab.id === tabId)
  if (closingIndex < 0) {
    return
  }

  const closingActiveTab = activeEntry.value?.id === tabId
  const fallbackTab = visitedTabs.value[closingIndex + 1] ?? visitedTabs.value[closingIndex - 1]
  visitedTabs.value = visitedTabs.value.filter((tab) => tab.id !== tabId)

  if (closingActiveTab && fallbackTab) {
    void router.push(fallbackTab.path)
  }
}

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
    class="tabbed-layout"
    :class="{
      'tabbed-layout--compact': appStore.sidebarCollapsed,
      'tabbed-layout--navigation-open': mobileNavigationOpen,
    }"
  >
    <header class="tabbed-layout__header">
      <RouterLink class="tabbed-layout__brand ui-focusable" to="/" aria-label="Pipker Framework 首页">
        <img class="tabbed-layout__brand-mark" src="/brand/pipker-logo.webp" alt="" />
        <span class="tabbed-layout__brand-copy">
          <strong>PIPKER</strong>
          <small>FRAMEWORK</small>
        </span>
      </RouterLink>

      <button
        class="tabbed-layout__mobile-toggle ui-focusable"
        type="button"
        :aria-expanded="mobileNavigationOpen"
        aria-controls="tabbed-layout-navigation"
        :aria-label="mobileNavigationOpen ? '关闭导航' : '打开导航'"
        @click="mobileNavigationOpen = !mobileNavigationOpen"
      >
        <Expand aria-hidden="true" />
      </button>

      <nav class="tabbed-breadcrumb" aria-label="面包屑导航">
        <span>{{ sessionStore.roles[0] ?? '授权工作区' }}</span>
        <template v-if="navigationTrail.length > 0">
          <template v-for="entry in navigationTrail" :key="entry.id">
            <i aria-hidden="true">/</i>
            <strong>{{ entry.label }}</strong>
          </template>
        </template>
      </nav>

      <div class="tabbed-layout__actions">
        <span class="tabbed-layout__authorization">{{ authorizationSummary }}</span>
        <RouterLink class="tabbed-layout__icon-action ui-focusable" to="/" aria-label="返回公开首页" title="返回公开首页">
          <House aria-hidden="true" />
        </RouterLink>
        <ThemeSwitcher compact />
        <div class="tabbed-layout__user" :title="userDisplayName">
          <img
            v-if="sessionStore.user?.avatar && !avatarLoadFailed"
            :key="sessionStore.user.avatar"
            :src="sessionStore.user.avatar"
            alt=""
            @error="markAvatarLoadFailed"
          />
          <span v-else class="tabbed-layout__user-fallback" aria-hidden="true">
            <User v-if="!userInitial" />
            <template v-else>{{ userInitial }}</template>
          </span>
          <span>{{ userDisplayName }}</span>
        </div>
        <button
          class="tabbed-layout__icon-action ui-focusable"
          type="button"
          aria-label="退出登录"
          title="退出登录"
          @click="sessionStore.logout"
        >
          <SwitchButton aria-hidden="true" />
        </button>
      </div>
    </header>

    <aside id="tabbed-layout-navigation" class="tabbed-layout__sidebar" aria-label="授权页面导航">
      <div class="tabbed-layout__sidebar-heading">
        <span>导航菜单</span>
        <b>{{ pageEntries.length }}</b>
      </div>

      <nav class="tabbed-navigation">
        <template v-for="entry in visibleNavigationEntries" :key="entry.id">
          <button
            v-if="entry.type === 'DIRECTORY'"
            class="tabbed-navigation__directory ui-focusable"
            type="button"
            :style="{ '--navigation-depth': `${entry.depth * 0.7}rem` }"
            :aria-expanded="isDirectoryExpanded(entry.id)"
            :aria-label="`${isDirectoryExpanded(entry.id) ? '收起' : '展开'}${entry.label}`"
            :title="entry.label"
            @click="toggleDirectory(entry.id)"
          >
            <span class="tabbed-navigation__icon" aria-hidden="true">
              <component :is="entry.icon" />
            </span>
            <span class="tabbed-navigation__copy">{{ entry.label }}</span>
            <ArrowDown class="tabbed-navigation__chevron" aria-hidden="true" />
          </button>

          <RouterLink
            v-else-if="entry.path"
            :to="entry.path"
            class="tabbed-navigation__item ui-focusable"
            :style="{ '--navigation-depth': `${entry.depth * 0.7}rem` }"
            :aria-label="entry.label"
            :title="entry.label"
            @click="closeMobileNavigation"
          >
            <span class="tabbed-navigation__icon" aria-hidden="true">
              <component :is="entry.icon" />
            </span>
            <span class="tabbed-navigation__copy">{{ entry.label }}</span>
          </RouterLink>
        </template>

        <p v-if="pageEntries.length === 0" class="tabbed-navigation__empty">
          当前账户没有可装载的菜单。
        </p>
      </nav>

      <button
        class="tabbed-layout__collapse ui-focusable"
        type="button"
        :aria-label="appStore.sidebarCollapsed ? '展开侧边栏' : '收起侧边栏'"
        @click="appStore.toggleSidebar"
      >
        <component :is="appStore.sidebarCollapsed ? Expand : Fold" aria-hidden="true" />
        <span>收起导航</span>
      </button>
    </aside>

    <button
      class="tabbed-layout__backdrop"
      type="button"
      aria-label="关闭导航"
      @click="closeMobileNavigation"
    ></button>

    <main class="tabbed-layout__main">
      <nav class="tabbed-history" aria-label="访问历史页签">
        <div
          v-for="tab in visitedTabs"
          :key="tab.id"
          class="tabbed-history__tab"
          :class="{ 'tabbed-history__tab--active': activeEntry?.id === tab.id }"
        >
          <button
            class="tabbed-history__link ui-focusable"
            type="button"
            :aria-current="activeEntry?.id === tab.id ? 'page' : undefined"
            @click="navigateToTab(tab)"
          >
            <component :is="tab.icon" aria-hidden="true" />
            <span>{{ tab.label }}</span>
          </button>
          <button
            v-if="tab.id !== pinnedEntry?.id"
            class="tabbed-history__close ui-focusable"
            type="button"
            :aria-label="`关闭${tab.label}页签`"
            @click="closeTab(tab.id)"
          >
            <Close aria-hidden="true" />
          </button>
        </div>
        <span v-if="visitedTabs.length === 0" class="tabbed-history__empty">等待授权页面装载</span>
      </nav>

      <section class="tabbed-layout__workspace">
        <RouterView />
      </section>

      <footer class="tabbed-layout__footer">
        <span>PIPKER FRAMEWORK</span>
        <p>DATABASE-DRIVEN AUTHORIZED NAVIGATION</p>
      </footer>
    </main>
  </div>
</template>

<style scoped lang="scss" src="@/styles/layout/pages/tabbed/index.scss"></style>
