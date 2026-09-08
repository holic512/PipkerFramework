<script setup lang="ts">
import type { RouteLocationRaw } from 'vue-router'

defineProps<{
  consoleActionCopy: string
  consoleDestination: RouteLocationRaw
}>()

const technologyTags = [
  'Spring Boot',
  'Vue 3',
  '数据库驱动 RBAC',
  '动态菜单路由',
] as const
</script>

<template>
  <section id="overview" class="home-hero" aria-labelledby="default-home-heading">
    <p class="home-hero__eyebrow ui-eyebrow">PIPKER FRAMEWORK / DELIVERY BASELINE</p>
    <h1 id="default-home-heading">
      让授权定义页面，
      <span>让交付持续向前。</span>
    </h1>
    <p class="home-hero__intro">
      Pipker 为后台系统整理 SYSTEM 会话、数据库驱动 RBAC、动态菜单路由与统一 API
      契约，让每一个业务模块从清晰边界开始。
    </p>

    <div class="home-hero__actions">
      <RouterLink class="home-hero__primary ui-focusable" :to="consoleDestination">
        <span>{{ consoleActionCopy }}</span>
        <i aria-hidden="true">↗</i>
      </RouterLink>
      <a class="home-hero__secondary ui-focusable" href="#architecture">
        查看架构
        <span aria-hidden="true">↓</span>
      </a>
    </div>

    <ul class="home-hero__tags" aria-label="Pipker 技术与能力标签">
      <li v-for="tag in technologyTags" :key="tag">{{ tag }}</li>
    </ul>

    <div class="home-hero__caption" aria-label="首页用途说明">
      <span>PUBLIC HOME</span>
      <i aria-hidden="true"></i>
      <span>AUTHORIZATION-LED DELIVERY</span>
    </div>
  </section>
</template>

<style scoped lang="scss">
.home-hero {
  width: min(100% - 2rem, 72rem);
  display: grid;
  justify-items: center;
  padding: clamp(6rem, 11vw, 10rem) 0 clamp(4.25rem, 7vw, 6.75rem);
  margin: 0 auto;
  text-align: center;
}

.home-hero__eyebrow {
  animation: home-reveal 500ms 80ms both cubic-bezier(0.16, 1, 0.3, 1);
}

.home-hero h1 {
  max-width: 62rem;
  margin: 1.1rem 0 0;
  color: var(--color-ink-strong);
  font-size: clamp(2.75rem, 7.25vw, 6.75rem);
  font-weight: 780;
  line-height: 1.04;
  letter-spacing: -0.085em;
  text-wrap: balance;
  animation: home-reveal 580ms 140ms both cubic-bezier(0.16, 1, 0.3, 1);
}

.home-hero h1 span {
  display: block;
  color: var(--color-accent-primary);
}

.home-hero__intro {
  max-width: 43rem;
  margin: 1.55rem 0 0;
  color: var(--color-ink-muted);
  font-size: clamp(0.95rem, 1.45vw, 1.08rem);
  line-height: 1.9;
  text-wrap: pretty;
  animation: home-reveal 580ms 205ms both cubic-bezier(0.16, 1, 0.3, 1);
}

.home-hero__actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 0.72rem;
  margin-top: 2rem;
  animation: home-reveal 580ms 270ms both cubic-bezier(0.16, 1, 0.3, 1);
}

.home-hero__primary,
.home-hero__secondary {
  min-height: 3.1rem;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 0.72rem;
  padding: 0.45rem 0.7rem 0.45rem 1.05rem;
  border-radius: var(--radius-control);
  font-size: 0.84rem;
  font-weight: 780;
  text-decoration: none;
  transition: background-color 180ms ease, border-color 180ms ease, color 180ms ease, box-shadow 180ms ease, transform 180ms ease;
}

.home-hero__primary {
  color: var(--color-accent-on-primary);
  background: var(--color-accent-primary);
  border: 1px solid var(--color-accent-primary);
  box-shadow: var(--shadow-panel);
}

.home-hero__primary i {
  width: 2rem;
  height: 2rem;
  display: grid;
  place-items: center;
  color: var(--color-accent-primary);
  background: var(--color-surface-raised);
  border-radius: var(--radius-small);
  font-size: 1rem;
  font-style: normal;
  transition: transform 180ms ease;
}

.home-hero__primary:hover {
  color: var(--color-accent-on-primary);
  background: var(--color-accent-primary-hover);
  border-color: var(--color-accent-primary-hover);
  box-shadow: var(--shadow-floating);
  transform: translateY(-0.15rem);
}

.home-hero__primary:hover i {
  transform: rotate(12deg);
}

.home-hero__secondary {
  padding-right: 1.05rem;
  color: var(--color-ink-strong);
  background: color-mix(in srgb, var(--color-surface-base) 82%, transparent);
  border: 1px solid var(--color-line-subtle);
}

.home-hero__secondary:hover {
  color: var(--color-accent-primary);
  border-color: var(--color-line-strong);
  transform: translateY(-0.15rem);
}

.home-hero__secondary span {
  color: var(--color-ink-soft);
  font-size: 1rem;
}

.home-hero__tags {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 0.5rem;
  padding: 0;
  margin: 1.6rem 0 0;
  list-style: none;
  animation: home-reveal 580ms 335ms both cubic-bezier(0.16, 1, 0.3, 1);
}

.home-hero__tags li {
  padding: 0.4rem 0.68rem;
  color: var(--color-ink-muted);
  background: var(--color-surface-translucent);
  border: 1px solid var(--color-line-subtle);
  border-radius: var(--radius-pill);
  font-size: 0.65rem;
  font-weight: 680;
  letter-spacing: 0.025em;
}

.home-hero__caption {
  display: inline-flex;
  align-items: center;
  gap: 0.58rem;
  margin-top: 3.75rem;
  color: var(--color-ink-soft);
  font-size: 0.59rem;
  font-weight: 760;
  letter-spacing: 0.11em;
  animation: home-reveal 580ms 395ms both cubic-bezier(0.16, 1, 0.3, 1);
}

.home-hero__caption i {
  width: 2.1rem;
  height: 1px;
  display: block;
  background: var(--color-line-strong);
}

@keyframes home-reveal {
  from {
    opacity: 0;
    transform: translateY(1rem);
  }

  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@include at-most('phone') {
  .home-hero {
    width: min(100% - 1.5rem, 72rem);
    padding-top: 4.75rem;
  }

  .home-hero h1 {
    font-size: clamp(2.42rem, 13vw, 3.45rem);
  }

  .home-hero__actions {
    width: 100%;
    display: grid;
    grid-template-columns: 1fr;
  }

  .home-hero__primary,
  .home-hero__secondary {
    width: 100%;
  }

  .home-hero__caption {
    margin-top: 2.8rem;
    font-size: 0.52rem;
  }
}
</style>
