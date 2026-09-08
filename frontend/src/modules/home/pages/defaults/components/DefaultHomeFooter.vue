<script setup lang="ts">
import type { RouteLocationRaw } from 'vue-router'

defineProps<{
  consoleActionCopy: string
  consoleDestination: RouteLocationRaw
}>()
</script>

<template>
  <footer class="home-footer">
    <div class="home-footer__closing ui-page-frame">
      <p class="ui-eyebrow">READY FOR THE NEXT MODULE</p>
      <h2>让框架先处理边界，<br />让团队专注交付业务。</h2>
      <p>
        默认首页是可继续拆分的框架基线；当项目需要自己的对外入口时，只需启用同级的系统专属首页。
      </p>
      <RouterLink class="home-footer__action ui-focusable" :to="consoleDestination">
        {{ consoleActionCopy }}
        <span aria-hidden="true">↗</span>
      </RouterLink>
    </div>

    <div class="home-footer__maintenance ui-page-frame" aria-label="首页维护位置">
      <p>HOME MAINTENANCE</p>
      <dl>
        <div>
          <dt>框架默认首页</dt>
          <dd><code>src/modules/home/pages/defaults/</code></dd>
        </div>
        <div>
          <dt>系统专属首页</dt>
          <dd><code>src/modules/home/pages/custom/</code></dd>
        </div>
        <div>
          <dt>根路由配置</dt>
          <dd><code>src/router/index.ts</code></dd>
        </div>
      </dl>
    </div>

    <div class="home-footer__bottom ui-page-frame">
      <RouterLink class="home-footer__brand ui-focusable" to="/">
        <span aria-hidden="true">P</span>
        PIPKER FRAMEWORK
      </RouterLink>
      <p>PUBLIC DELIVERY BASELINE</p>
    </div>
  </footer>
</template>

<style scoped lang="scss">
.home-footer {
  color: var(--color-ink-on-contrast);
  background: var(--color-surface-contrast);
}

.home-footer__closing {
  display: grid;
  justify-items: center;
  padding: clamp(5.75rem, 10vw, 9.25rem) 1rem clamp(4.5rem, 8vw, 7rem);
  text-align: center;
}

.home-footer__closing .ui-eyebrow {
  color: var(--color-accent-secondary);
}

.home-footer__closing h2 {
  margin: 0.85rem 0 0;
  color: var(--color-ink-on-contrast);
  font-size: clamp(2.1rem, 4.8vw, 4.8rem);
  font-weight: 740;
  line-height: 1.13;
  letter-spacing: -0.075em;
  text-wrap: balance;
}

.home-footer__closing > p:not(.ui-eyebrow) {
  max-width: 38rem;
  margin: 1.45rem 0 0;
  color: var(--color-ink-on-contrast-muted);
  font-size: 0.94rem;
  line-height: 1.85;
}

.home-footer__action {
  min-height: 3rem;
  display: inline-flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.4rem 0.55rem 0.4rem 1rem;
  margin-top: 1.8rem;
  color: var(--color-accent-on-primary);
  background: var(--color-accent-primary);
  border: 1px solid var(--color-accent-primary);
  border-radius: var(--radius-control);
  font-size: 0.83rem;
  font-weight: 780;
  text-decoration: none;
  transition: background-color 180ms ease, border-color 180ms ease, transform 180ms ease;
}

.home-footer__action span {
  width: 1.9rem;
  height: 1.9rem;
  display: grid;
  place-items: center;
  color: var(--color-accent-primary);
  background: var(--color-surface-raised);
  border-radius: var(--radius-small);
  font-size: 1rem;
}

.home-footer__action:hover {
  color: var(--color-accent-on-primary);
  background: var(--color-accent-primary-hover);
  border-color: var(--color-accent-primary-hover);
  transform: translateY(-0.15rem);
}

.home-footer__maintenance {
  display: grid;
  grid-template-columns: minmax(9rem, 0.45fr) minmax(0, 1fr);
  gap: 1.5rem;
  padding: 1.25rem 0;
  border-top: 1px solid var(--color-line-contrast);
}

.home-footer__maintenance > p,
.home-footer__bottom p {
  margin: 0;
  color: var(--color-ink-on-contrast-muted);
  font-size: 0.58rem;
  font-weight: 800;
  letter-spacing: 0.11em;
}

.home-footer__maintenance dl {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 1rem;
  margin: 0;
}

.home-footer__maintenance dl div {
  min-width: 0;
}

.home-footer__maintenance dt {
  color: var(--color-ink-on-contrast-muted);
  font-size: 0.68rem;
}

.home-footer__maintenance dd {
  padding: 0;
  margin: 0.42rem 0 0;
}

.home-footer__maintenance code {
  display: inline-block;
  max-width: 100%;
  overflow-wrap: anywhere;
  padding: 0.16rem 0.3rem;
  color: var(--color-ink-on-contrast);
  background: var(--color-panel-overlay);
  border-radius: var(--radius-small);
  font-family: var(--font-family);
  font-size: 0.67rem;
}

.home-footer__bottom {
  min-height: 4.4rem;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  border-top: 1px solid var(--color-line-contrast);
}

.home-footer__brand {
  display: inline-flex;
  align-items: center;
  gap: 0.48rem;
  color: var(--color-ink-on-contrast);
  font-size: 0.67rem;
  font-weight: 820;
  letter-spacing: 0.09em;
  text-decoration: none;
}

.home-footer__brand span {
  width: 1.2rem;
  height: 1.2rem;
  display: grid;
  place-items: center;
  color: var(--color-accent-primary);
  background: var(--color-ink-on-contrast);
  border-radius: var(--radius-small);
  font-size: 0.66rem;
  letter-spacing: 0;
}

@include at-most('tablet') {
  .home-footer__maintenance {
    grid-template-columns: 1fr;
    padding-right: 1rem;
    padding-left: 1rem;
  }
}

@include at-most('phone') {
  .home-footer__closing {
    padding-right: 0.75rem;
    padding-left: 0.75rem;
  }

  .home-footer__closing h2 br {
    display: none;
  }

  .home-footer__maintenance dl {
    grid-template-columns: 1fr;
    gap: 0.85rem;
  }

  .home-footer__bottom {
    align-items: flex-start;
    flex-direction: column;
    justify-content: center;
    padding: 0.8rem 1rem;
  }
}
</style>
