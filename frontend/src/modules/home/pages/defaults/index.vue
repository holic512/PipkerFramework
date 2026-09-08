<!--
  @file index.vue
  @project Pipker Framework
  @module 默认公开首页
  @description 由多个职责单一的展示区块组合框架维护的公开长页首页。
  @logic 使用根首页选择器传入的控制台目标组合框架基线内容；项目专属替换实现由同级定制页面负责。
  @dependencies Vue Router、默认首页区块、公开首页选择器
  @index_tags 首页、默认页、公开路由、落地页、RBAC
  @author holic512
-->
<script setup lang="ts">
import type { RouteLocationRaw } from 'vue-router'
import DefaultHomeCapabilities from './components/DefaultHomeCapabilities.vue'
import DefaultHomeFlowPreview from './components/DefaultHomeFlowPreview.vue'
import DefaultHomeFooter from './components/DefaultHomeFooter.vue'
import DefaultHomeHeader from './components/DefaultHomeHeader.vue'
import DefaultHomeHero from './components/DefaultHomeHero.vue'
defineProps<{
  consoleActionCopy: string
  consoleDestination: RouteLocationRaw
}>()
</script>

<template>
  <main class="default-home">
    <div class="default-home__grid" aria-hidden="true"></div>
    <div class="default-home__glow default-home__glow--hero" aria-hidden="true"></div>
    <div class="default-home__glow default-home__glow--flow" aria-hidden="true"></div>

    <DefaultHomeHeader
      :console-action-copy="consoleActionCopy"
      :console-destination="consoleDestination"
    />
    <DefaultHomeHero
      :console-action-copy="consoleActionCopy"
      :console-destination="consoleDestination"
    />
    <DefaultHomeFlowPreview />
    <DefaultHomeCapabilities />
    <DefaultHomeFooter
      :console-action-copy="consoleActionCopy"
      :console-destination="consoleDestination"
    />
  </main>
</template>

<style scoped lang="scss">
.default-home {
  min-width: 0;
  position: relative;
  overflow: clip;
  color: var(--color-ink-strong);
  background: var(--color-surface-page);
  isolation: isolate;
}

.default-home__grid,
.default-home__glow {
  position: absolute;
  z-index: -1;
  pointer-events: none;
}

.default-home__grid {
  inset: 0 0 auto;
  height: min(76rem, 100vh);
  opacity: 0.7;
  background-image:
    linear-gradient(color-mix(in srgb, var(--color-ink-muted) 12%, transparent) 1px, transparent 1px),
    linear-gradient(90deg, color-mix(in srgb, var(--color-ink-muted) 12%, transparent) 1px, transparent 1px);
  background-size: 4.5rem 4.5rem;
  mask-image: linear-gradient(to bottom, black 0%, transparent 90%);
}

.default-home__glow {
  border-radius: 50%;
  filter: blur(1px);
}

.default-home__glow--hero {
  width: min(54rem, 92vw);
  aspect-ratio: 1;
  top: -36rem;
  left: 50%;
  background: radial-gradient(circle, var(--color-glow-primary) 0%, transparent 69%);
  transform: translateX(-50%);
}

.default-home__glow--flow {
  width: 42rem;
  height: 34rem;
  top: 45rem;
  right: -25rem;
  background: radial-gradient(ellipse, var(--color-glow-secondary) 0%, transparent 70%);
}
</style>
