/**
 * @file navigation.ts
 * @project Pipker Framework
 * @module 应用布局导航模型
 * @description 将数据库授权菜单树转换为两套应用布局可共享的层级导航条目。
 * @logic 排序并保留目录祖先链，仅暴露具有有效路径的页面菜单，同时将受控图标名称解析为 Vue 组件。
 * @dependencies Vue、Element Plus 图标、SystemMenuNode
 * @index_tags 布局、导航、菜单树、面包屑、图标、RBAC
 * @author holic512
 */

import type { Component } from 'vue'
import { resolveMenuIcon } from './menuIcons'
import type { SystemMenuNode } from '../../core/api/contracts'

export interface LayoutNavigationEntry {
  id: string
  parentId: string | null
  label: string
  type: SystemMenuNode['type']
  path: string | null
  routeName: string | null
  icon: Component
  iconName: string | null
  depth: number
  ancestorIds: string[]
}

export function buildLayoutNavigation(menus: SystemMenuNode[]): LayoutNavigationEntry[] {
  return collectEntries(sortMenus(menus), [], 0)
}

export function findActiveNavigationEntry(
  entries: LayoutNavigationEntry[],
  routeId: unknown,
  currentPath: string,
): LayoutNavigationEntry | null {
  if (typeof routeId === 'string') {
    const matchingMenu = entries.find((entry) => entry.type === 'MENU' && entry.id === routeId)
    if (matchingMenu) {
      return matchingMenu
    }
  }

  return entries.find((entry) => entry.type === 'MENU' && entry.path === currentPath) ?? null
}

export function getNavigationTrail(
  entries: LayoutNavigationEntry[],
  activeEntry: LayoutNavigationEntry | null,
): LayoutNavigationEntry[] {
  if (!activeEntry) {
    return []
  }

  const trailIds = new Set([...activeEntry.ancestorIds, activeEntry.id])
  return entries.filter((entry) => trailIds.has(entry.id))
}

export function getPageNavigationEntries(entries: LayoutNavigationEntry[]): LayoutNavigationEntry[] {
  return entries.filter((entry) => entry.type === 'MENU' && entry.path !== null)
}

export function getDirectoryNavigationEntries(entries: LayoutNavigationEntry[]): LayoutNavigationEntry[] {
  return entries.filter((entry) => entry.type === 'DIRECTORY')
}

function collectEntries(
  menus: SystemMenuNode[],
  directoryAncestorIds: string[],
  depth: number,
): LayoutNavigationEntry[] {
  return menus.flatMap((menu) => {
    if (menu.type === 'DIRECTORY') {
      const childEntries = collectEntries(
        sortMenus(menu.children),
        [...directoryAncestorIds, menu.id],
        depth + 1,
      )
      if (!childEntries.some((entry) => entry.type === 'MENU')) {
        return []
      }

      return [
        toNavigationEntry(menu, directoryAncestorIds, depth),
        ...childEntries,
      ]
    }

    const childEntries = collectEntries(sortMenus(menu.children), directoryAncestorIds, depth + 1)
    if (!menu.path) {
      return childEntries
    }

    return [
      toNavigationEntry(menu, directoryAncestorIds, depth),
      ...childEntries,
    ]
  })
}

function toNavigationEntry(
  menu: SystemMenuNode,
  ancestorIds: string[],
  depth: number,
): LayoutNavigationEntry {
  return {
    id: menu.id,
    parentId: menu.parentId,
    label: menu.name,
    type: menu.type,
    path: menu.path,
    routeName: menu.routeName,
    icon: resolveMenuIcon(menu.icon),
    iconName: menu.icon,
    depth,
    ancestorIds,
  }
}

function sortMenus(menus: SystemMenuNode[]): SystemMenuNode[] {
  return [...menus].sort((left, right) => {
    const sortDifference = (left.sort ?? Number.MAX_SAFE_INTEGER) - (right.sort ?? Number.MAX_SAFE_INTEGER)
    return sortDifference === 0 ? left.id.localeCompare(right.id) : sortDifference
  })
}
