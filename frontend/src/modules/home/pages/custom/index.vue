<!--
  @file index.vue
  @project Pipker Framework
  @module 定制公开首页
  @description 在源码开关启用时，提供当前项目维护的公开首页初始占位实现。
  @logic 保持根公开路由不变，并使用选择器传入的控制台操作；同时明确后续系统专属实现与配置的维护位置。
  @dependencies Vue Router、ThemeSwitcher、公开首页选择器、公开首页配置
  @index_tags 首页、定制、占位页、公开路由、功能开关
  @author holic512
-->
<script setup lang="ts">
import type { RouteLocationRaw } from 'vue-router'
import ThemeSwitcher from '../../../../components/ThemeSwitcher.vue'

defineProps<{
  consoleActionCopy: string
  consoleDestination: RouteLocationRaw
}>()
</script>

<template>
  <main class="custom-home">
    <div class="custom-home__grid" aria-hidden="true"></div>
    <header class="custom-home__header">
      <RouterLink class="custom-home__brand ui-focusable" to="/" aria-label="Pipker Framework 首页">
        <img class="custom-home__logo" src="/brand/pipker-logo.webp" alt="" />
        PIPKER FRAMEWORK
      </RouterLink>
      <ThemeSwitcher />
    </header>

    <section class="custom-home__content" aria-labelledby="custom-home-heading">
      <p class="ui-eyebrow">CUSTOM HOME / ENABLED</p>
      <h1 id="custom-home-heading">系统专属首页已启用。</h1>
      <p>
        这里是项目私有的公开入口。保留当前最小骨架、主题切换和控制台跳转后，可以直接在本目录扩展属于这个系统的品牌、内容与交互。
      </p>
      <RouterLink class="custom-home__action ui-focusable" :to="consoleDestination">
        {{ consoleActionCopy }}
        <span aria-hidden="true">↗</span>
      </RouterLink>
    </section>

    <aside class="custom-home__guide" aria-label="定制首页维护位置">
      <p>EDIT THE PROJECT HOME</p>
      <div>
        <span>页面实现</span>
        <code>src/modules/home/pages/custom/index.vue</code>
      </div>
      <div>
        <span>源码开关</span>
        <code>src/modules/home/api/config.ts</code>
      </div>
      <small><code>isCustomHomeEnabled</code> 设为 <code>false</code> 后，将恢复框架默认首页。</small>
    </aside>
  </main>
</template>

<style scoped lang="scss">
.custom-home {
  min-height: 100svh;
  position: relative;
  display: grid;
  grid-template-rows: auto 1fr auto;
  gap: 2rem;
  overflow: clip;
  padding: clamp(1rem, 4vw, 3.5rem);
  color: var(--color-ink-strong);
  background: var(--color-surface-page);
}

.custom-home__grid {
  position: absolute;
  inset: 0;
  z-index: 0;
  pointer-events: none;
  background-image:
    linear-gradient(color-mix(in srgb, var(--color-ink-muted) 10%, transparent) 1px, transparent 1px),
    linear-gradient(90deg, color-mix(in srgb, var(--color-ink-muted) 10%, transparent) 1px, transparent 1px);
  background-size: 4rem 4rem;
  mask-image: radial-gradient(ellipse at center, black 0%, transparent 78%);
}

.custom-home__header,
.custom-home__content,
.custom-home__guide {
  width: min(100%, 72rem);
  position: relative;
  z-index: 1;
  margin-right: auto;
  margin-left: auto;
}

.custom-home__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.custom-home__brand {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  color: var(--color-ink-strong);
  font-size: 0.7rem;
  font-weight: 850;
  letter-spacing: 0.1em;
  text-decoration: none;
}

.custom-home__logo {
  width: 1.8rem;
  height: 1.8rem;
  display: block;
  object-fit: cover;
  border-radius: var(--radius-control);
}

.custom-home__content {
  align-self: center;
  max-width: 50rem;
  padding: clamp(3rem, 8vw, 7rem) 0;
}

.custom-home__content h1 {
  margin: 0.8rem 0 0;
  color: var(--color-ink-strong);
  font-size: clamp(2.5rem, 6.3vw, 5.8rem);
  font-weight: 760;
  line-height: 1.05;
  letter-spacing: -0.08em;
}

.custom-home__content > p:not(.ui-eyebrow) {
  max-width: 38rem;
  margin: 1.45rem 0 0;
  color: var(--color-ink-muted);
  font-size: 0.98rem;
  line-height: 1.9;
}

.custom-home__action {
  min-height: 3rem;
  display: inline-flex;
  align-items: center;
  gap: 0.7rem;
  padding: 0.42rem 0.55rem 0.42rem 1rem;
  margin-top: 1.8rem;
  color: var(--color-accent-on-primary);
  background: var(--color-accent-primary);
  border: 1px solid var(--color-accent-primary);
  border-radius: var(--radius-control);
  font-size: 0.82rem;
  font-weight: 780;
  text-decoration: none;
  transition: background-color 180ms ease, border-color 180ms ease, transform 180ms ease;
}

.custom-home__action span {
  width: 1.85rem;
  height: 1.85rem;
  display: grid;
  place-items: center;
  color: var(--color-accent-primary);
  background: var(--color-surface-raised);
  border-radius: var(--radius-small);
  font-size: 0.95rem;
}

.custom-home__action:hover {
  color: var(--color-accent-on-primary);
  background: var(--color-accent-primary-hover);
  border-color: var(--color-accent-primary-hover);
  transform: translateY(-0.15rem);
}

.custom-home__guide {
  display: grid;
  grid-template-columns: minmax(11rem, 0.45fr) repeat(2, minmax(0, 1fr));
  gap: 1rem;
  padding: 1rem;
  background: var(--color-surface-raised);
  border: 1px solid var(--color-line-subtle);
  border-radius: var(--radius-panel);
  box-shadow: var(--shadow-panel);
}

.custom-home__guide p,
.custom-home__guide span {
  margin: 0;
  color: var(--color-ink-soft);
  font-size: 0.6rem;
  font-weight: 800;
  letter-spacing: 0.09em;
}

.custom-home__guide div {
  display: grid;
  gap: 0.45rem;
}

.custom-home__guide code {
  overflow-wrap: anywhere;
  padding: 0.16rem 0.3rem;
  color: var(--color-ink-strong);
  background: var(--color-surface-muted);
  border-radius: var(--radius-small);
  font-family: var(--font-family);
  font-size: 0.7rem;
}

.custom-home__guide small {
  grid-column: 2 / -1;
  color: var(--color-ink-muted);
  font-size: 0.7rem;
  line-height: 1.5;
}

@include at-most('tablet') {
  .custom-home__guide {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .custom-home__guide p,
  .custom-home__guide small {
    grid-column: 1 / -1;
  }
}

@include at-most('phone') {
  .custom-home {
    padding: 1rem 0.75rem;
  }

  .custom-home__brand {
    font-size: 0.6rem;
  }

  .custom-home__guide {
    grid-template-columns: 1fr;
  }
}
</style>
