const { test } = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const ts = require('typescript')
const vue = require('vue')
const { parse, compileScript } = require('vue/compiler-sfc')

// Execute production scripts with real reactivity; replace I/O, UI services and lifecycle only.
// This is a behavioral unit harness, not a substitute for browser rendering or real authentication.
function loadSource(relativePath, dependencies, globals = {}) {
  const filename = path.resolve(__dirname, '../src', relativePath)
  let source = fs.readFileSync(filename, 'utf8')
  if (filename.endsWith('.vue')) {
    source = compileScript(parse(source, { filename }).descriptor, { id: 'feedback-test' }).content
  }
  source = ts.transpileModule(source, {
    compilerOptions: { target: ts.ScriptTarget.ES2022, module: ts.ModuleKind.CommonJS, esModuleInterop: true },
    fileName: filename.replace(/\.vue$/, '.ts'),
  }).outputText
  const module = { exports: {} }
  vm.runInNewContext(source, {
    module, exports: module.exports,
    require(name) {
      if (Object.hasOwn(dependencies, name)) return dependencies[name]
      if (name.endsWith('.vue') || name.startsWith('@element-plus/icons-vue') || name.startsWith('echarts/')) return {}
      throw new Error(`Unexpected dependency in ${relativePath}: ${name}`)
    },
    ...globals,
  }, { filename })
  return module.exports
}
const contracts = loadSource('core/api/contracts.ts', {})
const { getErrorMessage } = loadSource('core/api/errorMessage.ts', { './contracts': contracts })
const businessError = () => new contracts.ApiBusinessError(400, '业务失败')
const reject = async () => { throw businessError() }
const pageResult = { page: 1, pageSize: 10, total: 0, records: [] }
function deferred() {
  let resolve, reject
  const promise = new Promise((yes, no) => { resolve = yes; reject = no })
  return { promise, resolve, reject }
}
function fixture(name, overrides = {}, globals = {}) {
  const messages = [], teardowns = []
  const notification = Object.fromEntries(['success', 'error', 'warning', 'info'].map(type => [type, message => messages.push({ type, message })]))
  const dependencies = {
    vue: { ...vue, onMounted() {}, onBeforeUnmount(callback) { teardowns.push(callback) } },
    axios: { isAxiosError: error => error?.isAxiosError === true },
    'vue-router': { useRoute: () => ({ query: {}, meta: {}, fullPath: '/system/route' }), useRouter: () => ({ replace: async () => {} }) },
    'element-plus': { ElMessageBox: { confirm: async () => { throw 'cancel' } } },
    '../../../core/api/errorMessage': { getErrorMessage },
    '../../../core/notification': { notification },
    '../../../core/config/runtime': { runtimeConfig: { appName: '默认品牌' } },
    '../../../stores/session': { useSessionStore: () => ({ permissions: ['system:route:manage'], roles: ['SUPER_ADMIN'], routes: [] }) },
    '../../../router': { getDefaultAuthorizedPath: () => '/', router: { replace: async () => {}, hasRoute: () => true } },
    '../../../core/auth/sessionStorage': { readRememberedUsername: () => null, writeRememberedUsername() {}, clearRememberedUsername() {} },
    '../../../layouts/model/menuIcons': { menuIconNames: [], resolveMenuIcon() {} },
    'echarts/core': { use() {} },
    ...overrides,
  }
  const file = name === 'login' ? 'modules/auth/pages/LoginPage.vue' : `modules/system/${name}/index.vue`
  const component = loadSource(file, dependencies, globals).default
  return { state: component.setup({}, { expose() {} }), messages, unmount: () => teardowns.forEach(fn => fn()) }
}

test('shared error text accepts only non-empty business messages', () => {
  assert.equal(getErrorMessage(businessError(), 'fallback'), '业务失败')
  for (const error of [new contracts.ApiBusinessError(500, '  '), new Error('secret'), { message: 'secret' }, undefined]) {
    assert.equal(getErrorMessage(error, 'fallback'), 'fallback')
  }
})

test('notification boundary loads its stylesheet and preserves all four return handles', () => {
  const calls = [], handle = { close() {} }
  const methods = ['success', 'error', 'warning', 'info']
  const { notification } = loadSource('core/notification.ts', {
    'element-plus': { ElNotification: Object.fromEntries(methods.map(type => [type, message => { calls.push({ type, message }); return handle }])) },
    'element-plus/es/components/notification/style/css': {},
  })
  for (const type of methods) assert.equal(notification[type]('测试'), handle)
  assert.equal(calls.length, 4)
})

test('login validation is a notification without a request; pending submits cannot duplicate requests', async () => {
  let requests = 0, redirects = 0
  const pending = deferred()
  const { state, messages } = fixture('login', {
    '../../../stores/session': { useSessionStore: () => ({ login: () => { requests++; return pending.promise } }) },
    'vue-router': { useRoute: () => ({ query: {} }), useRouter: () => ({ replace: async () => { redirects++ } }) },
  })
  await state.submit()
  assert.equal(messages[0].type, 'warning')
  assert.equal(requests, 0)
  state.credentials.username = 'test-user'
  state.credentials.password = 'test-only'
  messages.length = 0
  const first = state.submit()
  await state.submit()
  assert.equal(requests, 1)
  pending.resolve({})
  await first
  assert.equal(redirects, 1)
  assert.equal(messages.length, 0)
  assert.equal(state.submitting.value, false)
})

test('login business and network failures emit exactly one contextual error', async () => {
  for (const error of [businessError(), new Error('transport diagnostic')]) {
    const { state, messages } = fixture('login', {
      '../../../stores/session': { useSessionStore: () => ({ login: async () => { throw error } }) },
    })
    Object.assign(state.credentials, { username: 'test-user', password: 'test-only' })
    await state.submit()
    assert.equal(messages.length, 1)
    assert.equal(messages[0].type, 'error')
    assert.equal(messages[0].message, error instanceof contracts.ApiBusinessError ? '业务失败' : '无法连接到服务，请确认后端已启动后重试。')
    assert.equal(state.submitting.value, false)
  }
})

for (const [name, api, method, loader] of [
  ['user', 'userManagement', 'getUserPage', 'loadUsers'],
  ['role', 'roleManagement', 'getRolePage', 'loadRoles'],
  ['login-log', 'loginLogManagement', 'getLoginLogPage', 'loadLoginLogs'],
  ['permission', 'permissionManagement', 'getPermissionPoints', 'loadPermissionPoints'],
  ['route', 'routeManagement', 'getRouteTree', 'loadRoutes'],
]) {
  test(`${name}: failed load notifies once and a successful retry clears the state`, async () => {
    let fail = true
    const { state, messages } = fixture(name, {
      [`./api/${api}`]: { [method]: async () => { if (fail) throw businessError(); return ['route', 'permission'].includes(name) ? [] : pageResult } },
    })
    await state[loader]()
    assert.equal(state.loadFailed.value, true)
    assert.equal(messages.length, 1)
    fail = false
    await state[loader]()
    assert.equal(state.loadFailed.value, false)
    assert.equal(messages.length, 1)
    if (state.confirm) {
      assert.equal(await state.confirm('确认？', '测试'), false)
      assert.equal(messages.length, 1)
    }
  })
}

test('permission and route superseded failures stay silent', async () => {
  for (const [name, api, method, loader] of [
    ['permission', 'permissionManagement', 'getPermissionPoints', 'loadPermissionPoints'],
    ['route', 'routeManagement', 'getRouteTree', 'loadRoutes'],
  ]) {
    const old = deferred()
    let calls = 0
    const { state, messages } = fixture(name, {
      [`./api/${api}`]: { [method]: () => ++calls === 1 ? old.promise : Promise.resolve([]) },
    })
    const first = state[loader]()
    await state[loader]()
    old.reject(businessError())
    await first
    assert.equal(state.loadFailed.value, false)
    assert.equal(messages.length, 0)
  }
})

test('route saving distinguishes a successful save from failed navigation refresh', async () => {
  let refreshFails = true
  const { state, messages } = fixture('route', {
    './api/routeManagement': { updateRouteConfiguration: async () => ({ id: '1' }), getRouteTree: async () => [] },
    '../../../stores/session': { useSessionStore: () => ({ permissions: ['system:route:manage'], routes: [], refreshAuthorization: async () => { if (refreshFails) throw businessError() } }) },
  })
  state.detail.value = { id: '1' }
  state.configuration.menuName = '测试'
  await state.save()
  assert.equal(messages.length, 2)
  assert.equal(messages[0].type, 'success')
  assert.equal(messages[1].type, 'warning')
  assert.match(messages[1].message, /配置已保存/)
  assert.equal(state.authorizationRefreshFailed.value, true)
  assert.equal(state.saving.value, false)
  refreshFails = false
  await state.refreshAuthorization()
  assert.equal(state.authorizationRefreshFailed.value, false)
})

const monitorApi = {
  getApiLogOverview: reject, getApiEndpointPage: reject, getApiAccessLogPage: reject, getApiAccessLogDetail: reject,
}
test('monitor aggregates parallel failures, supports independent retries, and isolates detail errors', async () => {
  const { state, messages } = fixture('api-monitor', { './api/apiLogManagement': monitorApi })
  await state.loadAll()
  assert.equal(messages.length, 1)
  for (const label of ['接口概览', '接口分析', '慢请求明细']) assert.ok(messages[0].message.includes(label))
  assert.equal(state.overviewFailed.value, true)
  await state.loadLogs()
  assert.equal(messages.length, 2)
  state.logFailed.value = false
  state.logPage.value = pageResult
  await state.openDetail('test-log')
  assert.equal(messages.length, 3)
  assert.equal(state.logFailed.value, false)
  assert.equal(state.logPage.value.total, 0)
  assert.equal(state.detailVisible.value, false)
})

test('monitor ignores unmounted failures, including pending aggregate results', async () => {
  const overview = deferred(), logs = deferred()
  const { state, messages, unmount } = fixture('api-monitor', { './api/apiLogManagement': {
    ...monitorApi, getApiLogOverview: () => overview.promise, getApiAccessLogPage: () => logs.promise,
  } })
  const loading = state.loadAll()
  await Promise.resolve()
  unmount()
  overview.reject(businessError())
  logs.reject(businessError())
  await loading
  assert.equal(messages.length, 0)
})

test('monitor aggregation drops a failed partition that has already been retried successfully', async () => {
  const overview = deferred()
  let endpointCalls = 0
  const { state, messages } = fixture('api-monitor', { './api/apiLogManagement': {
    ...monitorApi,
    getApiLogOverview: () => overview.promise,
    getApiEndpointPage: async () => { if (++endpointCalls === 1) throw businessError(); return pageResult },
    getApiAccessLogPage: async () => pageResult,
  } })
  const loading = state.loadAll()
  await Promise.resolve()
  await state.loadEndpoints()
  overview.reject(businessError())
  await loading
  assert.equal(messages.length, 1)
  assert.ok(messages[0].message.includes('接口概览'))
  assert.ok(!messages[0].message.includes('接口分析'))
  assert.equal(state.endpointFailed.value, false)
})

test('route detail ignores superseded failures while a current failure remains retryable', async () => {
  const previous = deferred()
  let calls = 0
  const { state, messages } = fixture('route', { './api/routeManagement': {
    getRouteDetail: () => ++calls === 1 ? previous.promise : reject(),
  } })
  const first = state.loadDetail()
  await state.loadDetail()
  previous.reject(businessError())
  await first
  assert.equal(messages.length, 1)
  assert.equal(state.detailFailed.value, true)
  assert.equal(state.detail.value, null)
  assert.equal(state.loadFailed.value, false)
})
