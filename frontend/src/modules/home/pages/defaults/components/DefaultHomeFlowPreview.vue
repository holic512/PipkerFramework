<script setup lang="ts">
import { runtimeConfig } from '../../../../../core/config/runtime'
</script>

<template>
  <section id="flow" class="home-flow" aria-labelledby="home-flow-heading">
    <div class="home-flow__heading ui-page-frame">
      <div>
        <p class="ui-eyebrow">RUNTIME / AUTHORIZATION TRACE</p>
        <h2 id="home-flow-heading">一条可追溯的页面到达路径。</h2>
      </div>
      <p>
        会话恢复后，前端只根据授权快照装载可以访问的业务页面；菜单、路由与组件之间保持可核验的映射关系。
      </p>
    </div>

    <div class="home-flow__console ui-page-frame" :aria-label="`${runtimeConfig.appName} 授权运行时预览`">
      <div class="home-flow__toolbar">
        <div class="home-flow__window-controls" aria-hidden="true">
          <span></span>
          <span></span>
          <span></span>
        </div>
        <p>{{ runtimeConfig.appName }} / RUNTIME INSPECTOR</p>
        <span class="home-flow__online"><i aria-hidden="true"></i> AUTHORIZED</span>
      </div>

      <div class="home-flow__workspace">
        <aside class="home-flow__navigator" aria-label="授权数据范围">
          <p>SESSION SNAPSHOT</p>
          <ul>
            <li class="is-current"><span aria-hidden="true">●</span> SYSTEM 用户</li>
            <li><span aria-hidden="true">└</span> roles</li>
            <li><span aria-hidden="true">└</span> permissions</li>
            <li><span aria-hidden="true">└</span> menus</li>
            <li><span aria-hidden="true">└</span> routes</li>
          </ul>
          <div class="home-flow__request">
            <span>REQUEST</span>
            <code>GET /api/auth/me</code>
          </div>
        </aside>

        <div class="home-flow__canvas">
          <div class="home-flow__canvas-head">
            <div>
              <p>ROUTE RESOLUTION</p>
              <strong>建立当前账户可见的页面边界</strong>
            </div>
            <span>4 STAGES</span>
          </div>

          <ol class="home-flow__stages" :aria-label="`${runtimeConfig.appName} 系统处理阶段`">
            <li>
              <span class="home-flow__stage-number">01</span>
              <strong>SESSION</strong>
              <small>恢复 SYSTEM 会话</small>
              <code>accessToken</code>
            </li>
            <li>
              <span class="home-flow__stage-number">02</span>
              <strong>RBAC</strong>
              <small>读取角色、菜单与路由</small>
              <code>roles · menus · routes</code>
            </li>
            <li>
              <span class="home-flow__stage-number">03</span>
              <strong>ROUTES</strong>
              <small>按授权路由注册页面</small>
              <code>componentKey</code>
            </li>
            <li>
              <span class="home-flow__stage-number">04</span>
              <strong>API</strong>
              <small>以统一契约交付数据</small>
              <code>code · data</code>
            </li>
          </ol>

          <div class="home-flow__result">
            <span>ROUTE READY</span>
            <p>当前导航只呈现已经被授权的页面。</p>
            <i aria-hidden="true">↗</i>
          </div>
        </div>
      </div>
    </div>

    <div class="home-flow__legend ui-page-frame" aria-label="运行时结构说明">
      <span><i aria-hidden="true"></i> 会话恢复</span>
      <span><i aria-hidden="true"></i> 菜单授权</span>
      <span><i aria-hidden="true"></i> 组件映射</span>
      <span><i aria-hidden="true"></i> 契约交付</span>
    </div>
  </section>
</template>

<style scoped lang="scss">
.home-flow {
  padding: 0 clamp(1rem, 4vw, 3.5rem) clamp(6rem, 11vw, 10rem);
}

.home-flow__heading {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(15rem, 0.7fr);
  gap: clamp(1.5rem, 5vw, 5rem);
  align-items: end;
  padding-bottom: 1.75rem;
}

.home-flow__heading h2 {
  max-width: 32rem;
  margin: 0.75rem 0 0;
  color: var(--color-ink-strong);
  font-size: clamp(1.9rem, 3.6vw, 3.5rem);
  font-weight: 760;
  line-height: 1.14;
  letter-spacing: -0.065em;
  text-wrap: balance;
}

.home-flow__heading > p {
  max-width: 28rem;
  margin: 0;
  color: var(--color-ink-muted);
  font-size: 0.92rem;
  line-height: 1.85;
}

.home-flow__console {
  overflow: hidden;
  color: var(--color-ink-on-contrast);
  background: var(--color-surface-contrast);
  border: 1px solid var(--color-line-contrast);
  border-radius: var(--radius-panel);
  box-shadow: var(--shadow-floating);
}

.home-flow__toolbar {
  min-height: 2.85rem;
  display: flex;
  align-items: center;
  gap: 0.65rem;
  padding: 0 1rem;
  color: var(--color-ink-on-contrast-muted);
  background: var(--color-surface-contrast-raised);
  border-bottom: 1px solid var(--color-line-contrast);
}

.home-flow__window-controls {
  display: flex;
  gap: 0.28rem;
}

.home-flow__window-controls span {
  width: 0.46rem;
  height: 0.46rem;
  display: block;
  background: var(--color-ink-on-contrast-muted);
  border-radius: 50%;
  opacity: 0.65;
}

.home-flow__toolbar p,
.home-flow__online,
.home-flow__navigator > p,
.home-flow__request span,
.home-flow__canvas-head p,
.home-flow__canvas-head > span,
.home-flow__stage-number,
.home-flow__stages strong,
.home-flow__result span,
.home-flow__legend {
  font-size: 0.58rem;
  font-weight: 800;
  letter-spacing: 0.1em;
}

.home-flow__toolbar p {
  margin: 0;
}

.home-flow__online {
  display: inline-flex;
  align-items: center;
  gap: 0.38rem;
  margin-left: auto;
  color: var(--color-ink-on-contrast-muted);
  white-space: nowrap;
}

.home-flow__online i {
  width: 0.42rem;
  height: 0.42rem;
  display: block;
  background: var(--color-accent-success);
  border-radius: 50%;
  box-shadow: 0 0 0 0.2rem color-mix(in srgb, var(--color-accent-success) 20%, transparent);
}

.home-flow__workspace {
  min-height: 25rem;
  display: grid;
  grid-template-columns: minmax(11rem, 0.34fr) minmax(0, 1fr);
}

.home-flow__navigator {
  display: flex;
  flex-direction: column;
  padding: 1.35rem;
  color: var(--color-ink-on-contrast-muted);
  background: color-mix(in srgb, var(--color-surface-contrast-raised) 80%, transparent);
  border-right: 1px solid var(--color-line-contrast);
}

.home-flow__navigator > p {
  margin: 0;
}

.home-flow__navigator ul {
  display: grid;
  gap: 0.58rem;
  padding: 0;
  margin: 1.3rem 0 0;
  list-style: none;
}

.home-flow__navigator li {
  color: var(--color-ink-on-contrast-muted);
  font-size: 0.72rem;
  font-weight: 650;
}

.home-flow__navigator li span {
  display: inline-block;
  width: 1rem;
  color: var(--color-accent-secondary);
}

.home-flow__navigator li.is-current {
  color: var(--color-ink-on-contrast);
}

.home-flow__request {
  display: grid;
  gap: 0.42rem;
  padding: 0.75rem;
  margin-top: auto;
  background: var(--color-panel-overlay);
  border: 1px solid var(--color-line-contrast);
  border-radius: var(--radius-control);
}

.home-flow__request span {
  color: var(--color-ink-on-contrast-muted);
}

.home-flow__request code,
.home-flow__stages code {
  color: var(--color-ink-on-contrast);
  font-family: var(--font-family);
  font-size: 0.64rem;
}

.home-flow__canvas {
  min-width: 0;
  display: flex;
  flex-direction: column;
  padding: clamp(1.2rem, 3vw, 2.25rem);
  background-image:
    linear-gradient(color-mix(in srgb, var(--color-ink-on-contrast) 7%, transparent) 1px, transparent 1px),
    linear-gradient(90deg, color-mix(in srgb, var(--color-ink-on-contrast) 7%, transparent) 1px, transparent 1px),
    radial-gradient(circle at 74% 45%, var(--color-glow-primary) 0%, transparent 38%);
  background-size: 2.65rem 2.65rem, 2.65rem 2.65rem, auto;
}

.home-flow__canvas-head {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
}

.home-flow__canvas-head p {
  margin: 0;
  color: var(--color-ink-on-contrast-muted);
}

.home-flow__canvas-head strong {
  display: block;
  max-width: 22rem;
  margin-top: 0.55rem;
  color: var(--color-ink-on-contrast);
  font-size: clamp(1.15rem, 2.2vw, 1.7rem);
  font-weight: 700;
  line-height: 1.35;
  letter-spacing: -0.04em;
}

.home-flow__canvas-head > span {
  flex: 0 0 auto;
  color: var(--color-accent-secondary);
}

.home-flow__stages {
  position: relative;
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 0.65rem;
  padding: 0;
  margin: auto 0 1.35rem;
  list-style: none;
}

.home-flow__stages::before {
  width: calc(100% - 8rem);
  height: 1px;
  position: absolute;
  top: 2.15rem;
  right: 4rem;
  z-index: 0;
  background: linear-gradient(90deg, transparent, var(--color-accent-secondary), transparent);
  content: '';
  opacity: 0.72;
}

.home-flow__stages li {
  min-width: 0;
  min-height: 8.55rem;
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  padding: 0.85rem;
  background: var(--color-panel-overlay);
  border: 1px solid var(--color-line-contrast);
  border-radius: var(--radius-control);
  transition: background-color 180ms ease, border-color 180ms ease, transform 180ms ease;
}

.home-flow__stages li:hover {
  background: var(--color-panel-overlay-hover);
  border-color: var(--color-accent-secondary);
  transform: translateY(-0.2rem);
}

.home-flow__stage-number {
  color: var(--color-accent-secondary);
}

.home-flow__stages strong {
  margin-top: auto;
  color: var(--color-ink-on-contrast);
}

.home-flow__stages small {
  min-height: 2.2em;
  margin-top: 0.35rem;
  color: var(--color-ink-on-contrast-muted);
  font-size: 0.66rem;
  line-height: 1.45;
}

.home-flow__stages code {
  display: block;
  overflow: hidden;
  margin-top: 0.55rem;
  color: var(--color-accent-secondary);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.home-flow__result {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  gap: 0.85rem;
  align-items: center;
  padding: 0.75rem 0.85rem;
  color: var(--color-ink-on-contrast);
  background: color-mix(in srgb, var(--color-panel-overlay-hover) 72%, transparent);
  border: 1px solid var(--color-line-contrast);
  border-radius: var(--radius-control);
}

.home-flow__result span {
  color: var(--color-accent-secondary);
}

.home-flow__result p {
  margin: 0;
  color: var(--color-ink-on-contrast-muted);
  font-size: 0.71rem;
}

.home-flow__result i {
  font-size: 1rem;
  font-style: normal;
}

.home-flow__legend {
  display: flex;
  flex-wrap: wrap;
  gap: 1rem 1.5rem;
  padding-top: 1rem;
  color: var(--color-ink-soft);
}

.home-flow__legend span {
  display: inline-flex;
  align-items: center;
  gap: 0.42rem;
}

.home-flow__legend i {
  width: 0.38rem;
  height: 0.38rem;
  display: block;
  background: var(--color-accent-primary);
  border-radius: 50%;
}

@include at-most('tablet') {
  .home-flow__heading {
    grid-template-columns: 1fr;
    gap: 1rem;
  }

  .home-flow__workspace {
    grid-template-columns: 1fr;
  }

  .home-flow__navigator {
    display: grid;
    grid-template-columns: auto 1fr auto;
    gap: 1rem;
    align-items: center;
    padding: 1rem 1.2rem;
    border-right: 0;
    border-bottom: 1px solid var(--color-line-contrast);
  }

  .home-flow__navigator ul {
    grid-template-columns: repeat(4, minmax(0, 1fr));
    gap: 0.4rem;
    margin: 0;
  }

  .home-flow__request {
    margin: 0;
  }
}

@include at-most('phone') {
  .home-flow {
    padding-right: 0.75rem;
    padding-left: 0.75rem;
  }

  .home-flow__heading {
    padding-bottom: 1.15rem;
  }

  .home-flow__toolbar {
    padding: 0 0.75rem;
  }

  .home-flow__toolbar p {
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .home-flow__workspace {
    min-height: 0;
  }

  .home-flow__navigator {
    grid-template-columns: 1fr;
    gap: 0.8rem;
  }

  .home-flow__navigator ul {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .home-flow__request {
    max-width: 14rem;
  }

  .home-flow__canvas-head {
    display: grid;
    gap: 0.85rem;
  }

  .home-flow__stages {
    grid-template-columns: 1fr;
    margin-top: 1.65rem;
  }

  .home-flow__stages::before {
    width: 1px;
    height: calc(100% - 5rem);
    top: 2.5rem;
    right: auto;
    left: 1.95rem;
    background: linear-gradient(var(--color-accent-secondary), transparent);
  }

  .home-flow__stages li {
    min-height: 7rem;
  }

  .home-flow__result {
    grid-template-columns: 1fr auto;
  }

  .home-flow__result p {
    grid-column: 1 / -1;
    grid-row: 2;
  }

  .home-flow__result i {
    grid-column: 2;
    grid-row: 1;
  }
}
</style>
