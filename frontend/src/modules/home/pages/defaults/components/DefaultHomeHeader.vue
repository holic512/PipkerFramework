<script setup lang="ts">
import type { RouteLocationRaw } from 'vue-router'
import ThemeSwitcher from '../../../../../components/ThemeSwitcher.vue'
import { runtimeConfig } from '../../../../../core/config/runtime'

defineProps<{
  consoleActionCopy: string
  consoleDestination: RouteLocationRaw
}>()
</script>

<template>
  <header class="home-header">
    <div class="home-header__frame">
      <RouterLink class="home-header__brand ui-focusable" to="/" :aria-label="`${runtimeConfig.appName} 首页`">
        <img class="home-header__mark" src="/brand/pipker-logo.webp" alt="" />
        <span class="home-header__brand-copy">
          <strong>{{ runtimeConfig.appName }}</strong>
          <small>系统门户</small>
        </span>
      </RouterLink>

      <nav class="home-header__nav" aria-label="首页内容导航">
        <a href="#overview">框架能力</a>
        <a href="#flow">授权与路由</a>
        <a href="#architecture">架构边界</a>
        <a href="#delivery">交付基线</a>
      </nav>

      <div class="home-header__actions">
        <p class="home-header__availability">
          <span aria-hidden="true"></span>
          PUBLIC ENTRY
        </p>
        <ThemeSwitcher />
        <RouterLink class="home-header__console ui-focusable" :to="consoleDestination">
          {{ consoleActionCopy }}
          <span aria-hidden="true">↗</span>
        </RouterLink>
      </div>

      <details class="home-header__mobile-menu">
        <summary class="ui-focusable" aria-label="展开首页内容导航">
          <span aria-hidden="true">≡</span>
          导航
        </summary>
        <nav aria-label="首页内容导航（移动端）">
          <a href="#overview">框架能力</a>
          <a href="#flow">授权与路由</a>
          <a href="#architecture">架构边界</a>
          <a href="#delivery">交付基线</a>
          <RouterLink :to="consoleDestination">{{ consoleActionCopy }}</RouterLink>
        </nav>
      </details>
    </div>
  </header>
</template>

<style scoped lang="scss">
.home-header {
  position: relative;
  z-index: 10;
  padding: 1rem clamp(1rem, 4vw, 3.5rem);
}

.home-header__frame {
  width: min(100%, 78rem);
  min-height: 3rem;
  display: flex;
  align-items: center;
  gap: 1rem;
  margin: 0 auto;
}

.home-header__brand {
  flex: 0 0 auto;
  display: inline-flex;
  align-items: center;
  gap: 0.62rem;
  color: var(--color-ink-strong);
  text-decoration: none;
}

.home-header__mark {
  width: 2rem;
  height: 2rem;
  display: block;
  object-fit: cover;
  border-radius: var(--radius-control);
}

.home-header__brand-copy {
  display: grid;
  gap: 0.08rem;
  line-height: 1;
}

.home-header__brand-copy strong {
  font-size: 0.78rem;
  font-weight: 850;
  letter-spacing: 0.12em;
}

.home-header__brand-copy small {
  color: var(--color-ink-soft);
  font-size: 0.5rem;
  font-weight: 750;
  letter-spacing: 0.14em;
}

.home-header__nav {
  display: flex;
  align-items: center;
  gap: clamp(0.7rem, 1.55vw, 1.4rem);
  margin-left: auto;
}

.home-header__nav a,
.home-header__console {
  color: var(--color-ink-muted);
  font-size: 0.75rem;
  font-weight: 720;
  text-decoration: none;
  transition: color 160ms ease, background-color 160ms ease, border-color 160ms ease, transform 160ms ease;
}

.home-header__nav a:hover {
  color: var(--color-ink-strong);
}

.home-header__actions {
  display: flex;
  align-items: center;
  gap: 0.62rem;
  margin-left: clamp(0.2rem, 1vw, 0.85rem);
}

.home-header__availability {
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
  margin: 0;
  color: var(--color-ink-soft);
  font-size: 0.57rem;
  font-weight: 800;
  letter-spacing: 0.1em;
  white-space: nowrap;
}

.home-header__availability span {
  width: 0.4rem;
  height: 0.4rem;
  display: block;
  background: var(--color-accent-success);
  border-radius: 50%;
  box-shadow: 0 0 0 0.2rem color-mix(in srgb, var(--color-accent-success) 15%, transparent);
}

.home-header__console {
  min-height: 2.25rem;
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.35rem 0.65rem;
  color: var(--color-accent-on-primary);
  background: var(--color-accent-primary);
  border: 1px solid var(--color-accent-primary);
  border-radius: var(--radius-control);
  white-space: nowrap;
}

.home-header__console span {
  font-size: 0.95rem;
}

.home-header__console:hover {
  color: var(--color-accent-on-primary);
  background: var(--color-accent-primary-hover);
  border-color: var(--color-accent-primary-hover);
  transform: translateY(-0.1rem);
}

.home-header__mobile-menu {
  display: none;
  position: relative;
  margin-left: auto;
}

.home-header__mobile-menu summary {
  min-height: 2.25rem;
  display: inline-flex;
  align-items: center;
  gap: 0.36rem;
  padding: 0.35rem 0.54rem;
  color: var(--color-ink-strong);
  cursor: pointer;
  list-style: none;
  background: var(--color-surface-translucent);
  border: 1px solid var(--color-line-subtle);
  border-radius: var(--radius-control);
  font-size: 0.72rem;
  font-weight: 750;
}

.home-header__mobile-menu summary::-webkit-details-marker {
  display: none;
}

.home-header__mobile-menu nav {
  width: min(17rem, calc(100vw - 2rem));
  position: absolute;
  top: calc(100% + 0.5rem);
  right: 0;
  display: grid;
  padding: 0.45rem;
  background: var(--color-surface-base);
  border: 1px solid var(--color-line-strong);
  border-radius: var(--radius-panel);
  box-shadow: var(--shadow-floating);
}

.home-header__mobile-menu a {
  padding: 0.68rem 0.72rem;
  color: var(--color-ink-muted);
  border-radius: var(--radius-small);
  font-size: 0.78rem;
  font-weight: 700;
  text-decoration: none;
}

.home-header__mobile-menu a:hover {
  color: var(--color-accent-on-primary);
  background: var(--color-accent-primary);
}

@include at-most('desktop') {
  .home-header__availability {
    display: none;
  }

  .home-header__nav {
    gap: 0.72rem;
  }
}

@include at-most('tablet') {
  .home-header {
    padding-right: 1rem;
    padding-left: 1rem;
  }

  .home-header__nav,
  .home-header__console {
    display: none;
  }

  .home-header__actions {
    margin-left: auto;
  }

  .home-header__mobile-menu {
    display: block;
  }
}

@include at-most('phone') {
  .home-header__frame {
    gap: 0.45rem;
  }

  .home-header__brand-copy {
    display: none;
  }

  .home-header__actions {
    gap: 0.35rem;
  }
}
</style>
