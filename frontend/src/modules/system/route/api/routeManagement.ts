/**
 * @file routeManagement.ts
 * @project Pipker Framework
 * @module Frontend Route Management
 * @description 封装系统路由树、详情及配置更新 API。
 * @logic 字符串传递雪花主键，完整树用于保留任意目录层级；PUT 仅更新展示配置，不提供新增删除或路由结构写入。
 * @dependencies requestApi、frontend API contracts
 * @index_tags api、route、system-menu、tree、configuration、read-only
 * @author holic512
 */
import type {
  SystemRouteDetail,
  SystemRouteMenuType,
  SystemRouteStatus,
  SystemRouteTreeNode,
} from '../../../../core/api/contracts'
import { requestApi } from '../../../../core/http/client'

export interface RouteTreeQuery {
  keyword?: string
  menuType?: SystemRouteMenuType
  visible?: boolean
  status?: SystemRouteStatus
}

/** 读取完整的目录—页面管理树，避免跨页丢失父子层级。 */
export function getRouteTree(): Promise<SystemRouteTreeNode[]> {
  return requestApi<SystemRouteTreeNode[]>({
    method: 'GET',
    url: '/admin/routes/tree',
  })
}

/** 读取一条路由定义的完整详情。 */
export function getRouteDetail(routeId: string): Promise<SystemRouteDetail> {
  return requestApi<SystemRouteDetail>({
    method: 'GET',
    url: `/admin/routes/${encodeURIComponent(routeId)}`,
  })
}

export interface RouteConfiguration {
  menuName: string
  icon: string | null
  sort: number
  visible: boolean
  status: SystemRouteStatus
}

export function updateRouteConfiguration(routeId: string, data: RouteConfiguration): Promise<SystemRouteDetail> {
  return requestApi<SystemRouteDetail>({
    method: 'PUT',
    url: `/admin/routes/${encodeURIComponent(routeId)}/configuration`,
    data,
  })
}
