<script setup lang="ts">
const deliveryPillars = [
  {
    index: '01',
    title: '会话与授权',
    detail: 'SYSTEM 登录后恢复当前授权快照，前端状态只投影可验证的用户、角色、权限与菜单。',
    code: 'GET /api/auth/me',
  },
  {
    index: '02',
    title: '动态路由',
    detail: '页面菜单携带 routeName 与 componentKey；只存在于授权菜单中的页面才会注册到应用壳。',
    code: 'menus → componentKey',
  },
  {
    index: '03',
    title: 'API 契约',
    detail: '请求层统一处理认证头和业务解包，视图与 Store 不直接依赖原始 HTTP 响应结构。',
    code: '{ code, data, message }',
  },
  {
    index: '04',
    title: '数据演进',
    detail: 'Liquibase 将菜单、角色关联和数据库结构作为可追踪的演进记录，避免环境漂移。',
    code: 'Liquibase changesets',
  },
] as const
</script>

<template>
  <section id="architecture" class="home-capabilities" aria-labelledby="architecture-heading">
    <div class="home-capabilities__intro ui-page-frame">
      <p class="ui-eyebrow">ARCHITECTURE / ONE AUTHORIZATION CHAIN</p>
      <div>
        <h2 id="architecture-heading">角色、菜单与组件，都来自同一条授权链路。</h2>
        <p>
          Pipker 不把页面可见性分散在静态导航、路由表和接口分支中。后端授权快照提供菜单树，前端据此注册页面，并在下一次会话变化时同步清理旧路由。
        </p>
      </div>
    </div>

    <div class="home-capabilities__chain ui-page-frame" aria-label="授权到组件的结构链路">
      <article class="chain-card chain-card--source">
        <span>AUTHORITY</span>
        <strong>角色与菜单关联</strong>
        <p>数据库为页面访问边界提供唯一来源。</p>
        <i aria-hidden="true">01</i>
      </article>
      <div class="chain-link" aria-hidden="true"><span>→</span></div>
      <article class="chain-card">
        <span>MENU TREE</span>
        <strong>MENU 页面节点</strong>
        <p>目录只组织导航；页面菜单才具备路由信息。</p>
        <i aria-hidden="true">02</i>
      </article>
      <div class="chain-link" aria-hidden="true"><span>→</span></div>
      <article class="chain-card">
        <span>ROUTE MAP</span>
        <strong>path 与 routeName</strong>
        <p>授权数据被转换为当前会话有效的路由记录。</p>
        <i aria-hidden="true">03</i>
      </article>
      <div class="chain-link" aria-hidden="true"><span>→</span></div>
      <article class="chain-card chain-card--target">
        <span>VIEW ENTRY</span>
        <strong>componentKey</strong>
        <p>模块入口与菜单键对应，应用壳只渲染获准页面。</p>
        <i aria-hidden="true">04</i>
      </article>
    </div>

    <div class="home-capabilities__rule ui-page-frame">
      <span class="home-capabilities__rule-label">DESIGN RULE</span>
      <p>页面不是“默认存在后再隐藏”，而是“得到授权后才装载”。</p>
      <code>MENU + path + routeName + componentKey</code>
    </div>
  </section>

  <section id="delivery" class="home-delivery" aria-labelledby="delivery-heading">
    <div class="home-delivery__heading ui-page-frame">
      <div>
        <p class="ui-eyebrow">DELIVERY / STABLE BOUNDARIES</p>
        <h2 id="delivery-heading">基础设施不替业务做决定。</h2>
      </div>
      <p>
        登录、路由、请求与数据库演进各自只处理自己的边界。业务模块在这些约定上增加功能，而不是重新发明系统行为。
      </p>
    </div>

    <div class="home-delivery__feature-grid ui-page-frame">
      <article v-for="pillar in deliveryPillars" :key="pillar.index" class="delivery-card ui-panel">
        <div class="delivery-card__topline">
          <span>{{ pillar.index }}</span>
          <i aria-hidden="true"></i>
        </div>
        <h3>{{ pillar.title }}</h3>
        <p>{{ pillar.detail }}</p>
        <code>{{ pillar.code }}</code>
      </article>
    </div>

    <div class="home-delivery__contract ui-page-frame">
      <div class="home-delivery__contract-copy">
        <p class="ui-eyebrow">API RESPONSE CONTRACT</p>
        <h3>每个响应都带着可以被业务判断的结果。</h3>
        <p>
          客户端统一解包 <code>code</code>、<code>data</code> 与 <code>message</code>，认证失败与业务失败能在同一条请求链路中被明确处理。
        </p>
      </div>
      <div class="home-delivery__contract-code" aria-label="统一 API 响应结构示意">
        <span>{</span>
        <p><i>code</i>: 200,</p>
        <p><i>data</i>: { … },</p>
        <p><i>message</i>: <b>"OK"</b></p>
        <span>}</span>
      </div>
    </div>
  </section>
</template>

<style scoped lang="scss">
.home-capabilities,
.home-delivery {
  padding: clamp(5.5rem, 10vw, 9.5rem) clamp(1rem, 4vw, 3.5rem);
}

.home-capabilities {
  background: var(--color-surface-base);
  border-top: 1px solid var(--color-line-subtle);
  border-bottom: 1px solid var(--color-line-subtle);
}

.home-capabilities__intro {
  display: grid;
  grid-template-columns: minmax(11rem, 0.42fr) minmax(0, 1fr);
  gap: clamp(1.5rem, 7vw, 8rem);
}

.home-capabilities__intro h2,
.home-delivery__heading h2 {
  max-width: 46rem;
  margin: 0;
  color: var(--color-ink-strong);
  font-size: clamp(2rem, 4.2vw, 4.2rem);
  font-weight: 760;
  line-height: 1.12;
  letter-spacing: -0.07em;
  text-wrap: balance;
}

.home-capabilities__intro p:not(.ui-eyebrow),
.home-delivery__heading > p {
  max-width: 39rem;
  margin: 1.3rem 0 0;
  color: var(--color-ink-muted);
  font-size: 0.96rem;
  line-height: 1.9;
}

.home-capabilities__chain {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto minmax(0, 1fr) auto minmax(0, 1fr) auto minmax(0, 1fr);
  gap: 0.5rem;
  align-items: stretch;
  margin-top: clamp(2.8rem, 6vw, 5rem);
}

.chain-card {
  min-height: 13rem;
  position: relative;
  display: flex;
  flex-direction: column;
  padding: 1.25rem;
  color: var(--color-ink-strong);
  background: var(--color-surface-raised);
  border: 1px solid var(--color-line-subtle);
  border-radius: var(--radius-panel);
  box-shadow: var(--shadow-panel);
  transition: border-color 180ms ease, box-shadow 180ms ease, transform 180ms ease;
}

.chain-card:hover {
  border-color: var(--color-line-strong);
  box-shadow: var(--shadow-floating);
  transform: translateY(-0.2rem);
}

.chain-card--source,
.chain-card--target {
  color: var(--color-ink-on-contrast);
  background: var(--color-surface-contrast);
  border-color: var(--color-line-contrast);
}

.chain-card > span,
.home-capabilities__rule-label,
.delivery-card__topline span,
.home-delivery__contract-code {
  font-size: 0.6rem;
  font-weight: 800;
  letter-spacing: 0.1em;
}

.chain-card > span {
  color: var(--color-accent-secondary);
}

.chain-card strong {
  margin-top: auto;
  font-size: 1rem;
  letter-spacing: -0.025em;
}

.chain-card p {
  min-height: 3.85em;
  margin: 0.55rem 0 0;
  color: var(--color-ink-muted);
  font-size: 0.76rem;
  line-height: 1.65;
}

.chain-card--source p,
.chain-card--target p {
  color: var(--color-ink-on-contrast-muted);
}

.chain-card i {
  position: absolute;
  top: 1.2rem;
  right: 1.2rem;
  color: var(--color-ink-soft);
  font-size: 0.65rem;
  font-style: normal;
  font-weight: 760;
}

.chain-card--source i,
.chain-card--target i {
  color: var(--color-ink-on-contrast-muted);
}

.chain-link {
  width: 1.35rem;
  display: grid;
  place-items: center;
  color: var(--color-ink-soft);
}

.chain-link span {
  width: 1.25rem;
  height: 1.25rem;
  display: grid;
  place-items: center;
  background: var(--color-surface-muted);
  border: 1px solid var(--color-line-subtle);
  border-radius: 50%;
  font-size: 0.72rem;
}

.home-capabilities__rule {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  gap: 1.25rem;
  align-items: center;
  padding: 1.15rem 0;
  border-bottom: 1px solid var(--color-line-subtle);
}

.home-capabilities__rule-label {
  color: var(--color-accent-primary);
}

.home-capabilities__rule p {
  margin: 0;
  color: var(--color-ink-strong);
  font-size: 0.85rem;
  font-weight: 690;
}

.home-capabilities__rule code,
.delivery-card code,
.home-delivery__contract-copy code {
  padding: 0.18rem 0.35rem;
  color: var(--color-ink-muted);
  background: var(--color-surface-muted);
  border-radius: var(--radius-small);
  font-family: var(--font-family);
  font-size: 0.7rem;
}

.home-delivery {
  background: var(--color-surface-page);
}

.home-delivery__heading {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(15rem, 0.68fr);
  gap: clamp(1.5rem, 6vw, 7rem);
  align-items: end;
}

.home-delivery__heading > p {
  margin: 0;
}

.home-delivery__feature-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 0.75rem;
  margin-top: clamp(2.75rem, 5vw, 4.5rem);
}

.delivery-card {
  min-width: 0;
  min-height: 15.5rem;
  display: flex;
  flex-direction: column;
  padding: 1.2rem;
  transition: border-color 180ms ease, box-shadow 180ms ease, transform 180ms ease;
}

.delivery-card:hover {
  border-color: var(--color-line-strong);
  box-shadow: var(--shadow-floating);
  transform: translateY(-0.2rem);
}

.delivery-card__topline {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.delivery-card__topline span {
  color: var(--color-accent-primary);
}

.delivery-card__topline i {
  width: 1.85rem;
  height: 1.85rem;
  display: block;
  background:
    linear-gradient(90deg, transparent 44%, var(--color-accent-primary) 44%, var(--color-accent-primary) 56%, transparent 56%),
    linear-gradient(transparent 44%, var(--color-accent-primary) 44%, var(--color-accent-primary) 56%, transparent 56%);
  border: 1px solid var(--color-line-subtle);
  border-radius: var(--radius-small);
  opacity: 0.8;
}

.delivery-card h3 {
  margin: auto 0 0;
  color: var(--color-ink-strong);
  font-size: 1.05rem;
  letter-spacing: -0.035em;
}

.delivery-card p {
  margin: 0.65rem 0 0;
  color: var(--color-ink-muted);
  font-size: 0.76rem;
  line-height: 1.7;
}

.delivery-card code {
  display: block;
  overflow: hidden;
  margin-top: 1.1rem;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.home-delivery__contract {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(16rem, 0.68fr);
  gap: clamp(1.5rem, 7vw, 8rem);
  align-items: center;
  padding: clamp(1.5rem, 4vw, 3.5rem);
  margin-top: clamp(2.5rem, 5vw, 4.5rem);
  color: var(--color-ink-on-contrast);
  background: var(--color-surface-contrast);
  border: 1px solid var(--color-line-contrast);
  border-radius: var(--radius-panel);
  box-shadow: var(--shadow-floating);
}

.home-delivery__contract-copy .ui-eyebrow {
  color: var(--color-accent-secondary);
}

.home-delivery__contract-copy h3 {
  max-width: 34rem;
  margin: 0.8rem 0 0;
  color: var(--color-ink-on-contrast);
  font-size: clamp(1.45rem, 2.8vw, 2.4rem);
  font-weight: 720;
  line-height: 1.28;
  letter-spacing: -0.055em;
}

.home-delivery__contract-copy > p:not(.ui-eyebrow) {
  max-width: 36rem;
  margin: 1rem 0 0;
  color: var(--color-ink-on-contrast-muted);
  font-size: 0.84rem;
  line-height: 1.8;
}

.home-delivery__contract-copy code {
  color: var(--color-ink-on-contrast);
  background: var(--color-panel-overlay);
}

.home-delivery__contract-code {
  display: grid;
  gap: 0.44rem;
  padding: 1.25rem;
  color: var(--color-ink-on-contrast-muted);
  background: var(--color-panel-overlay);
  border: 1px solid var(--color-line-contrast);
  border-radius: var(--radius-control);
}

.home-delivery__contract-code > span {
  color: var(--color-accent-secondary);
  font-size: 1rem;
}

.home-delivery__contract-code p {
  margin: 0;
  padding-left: 0.75rem;
}

.home-delivery__contract-code i {
  color: var(--color-ink-on-contrast);
  font-style: normal;
}

.home-delivery__contract-code b {
  color: var(--color-accent-secondary);
  font-weight: 750;
}

@include at-most('tablet') {
  .home-capabilities__intro,
  .home-delivery__heading,
  .home-delivery__contract {
    grid-template-columns: 1fr;
    gap: 1.25rem;
  }

  .home-capabilities__chain {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .chain-link {
    display: none;
  }

  .home-delivery__feature-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@include at-most('phone') {
  .home-capabilities,
  .home-delivery {
    padding-right: 0.75rem;
    padding-left: 0.75rem;
  }

  .home-capabilities__chain,
  .home-delivery__feature-grid {
    grid-template-columns: 1fr;
  }

  .chain-card,
  .delivery-card {
    min-height: 0;
  }

  .chain-card strong,
  .delivery-card h3 {
    margin-top: 2.2rem;
  }

  .home-capabilities__rule {
    grid-template-columns: 1fr;
    gap: 0.65rem;
  }

  .home-capabilities__rule code {
    justify-self: start;
  }

  .home-delivery__contract {
    padding: 1.25rem;
  }
}
</style>
