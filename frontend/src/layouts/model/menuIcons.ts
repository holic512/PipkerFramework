/**
 * @file menuIcons.ts
 * @project Pipker Framework
 * @module 导航图标
 * @description 共享导航和路由配置的受控图标目录。
 * @logic 已知名称解析到图标组件，空值与历史未知名称使用默认图标。
 * @dependencies Element Plus 图标、Vue
 * @index_tags navigation、icon、route
 * @author holic512
 */
import type { Component } from 'vue'
import { House, Menu, Setting, Share, UserFilled, User, Folder, Document, List, Monitor, Tools, Bell, Search, DataAnalysis } from '@element-plus/icons-vue'

export const menuIcons: Readonly<Record<string, Component>> = {
  House, Menu, Setting, Share, UserFilled, User, Folder, Document, List, Monitor, Tools, Bell, Search, DataAnalysis,
}
export const menuIconNames = Object.keys(menuIcons)
export function resolveMenuIcon(name: string | null): Component {
  return (name && menuIcons[name]) || Menu
}
