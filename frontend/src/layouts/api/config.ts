/**
 * @file config.ts
 * @project Pipker Framework
 * @module 应用布局配置
 * @description 声明登录后控制台应渲染当前侧栏布局还是页签工作区布局。
 * @logic 路由稳定入口读取此源码配置并挂载对应页面；配置切换不会改变会话、数据库菜单或动态路由规则。
 * @dependencies 应用布局选择器、侧栏布局、页签布局
 * @index_tags 布局、接口层、配置、功能开关、控制台
 * @author holic512
 */

export type ApplicationLayoutVariant = 'sidebar' | 'tabbed'

export const applicationLayoutVariant: ApplicationLayoutVariant = 'sidebar'
