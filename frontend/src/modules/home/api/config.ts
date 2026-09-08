/**
 * @file config.ts
 * @project Pipker Framework
 * @module 公开首页配置
 * @description 声明根路由应渲染框架默认首页还是项目专属首页。
 * @logic 根首页选择器读取此源码开关；切换时不会改变公开 / URL，也不写入浏览器本地偏好。
 * @dependencies 首页选择器、公开首页页面
 * @index_tags 首页、接口层、配置、功能开关
 * @author holic512
 */

export const isCustomHomeEnabled = true
