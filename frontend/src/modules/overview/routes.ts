/**
 * @file routes.ts
 * @project Pipker Framework
 * @module Overview module routing
 * @description Exposes routes owned by the framework overview business module.
 * @logic Keeps page ownership inside the module while allowing the central router to compose it beneath the shared layout.
 * @dependencies Vue Router, OverviewPage
 * @index_tags frontend,module,overview,routes
 * @author holic512
 */
import type { RouteRecordRaw } from 'vue-router'

export const overviewRoutes: RouteRecordRaw[] = [
  {
    path: '',
    name: 'overview',
    component: () => import('./pages/OverviewPage.vue'),
    meta: {
      title: '系统概览',
    },
  },
]
