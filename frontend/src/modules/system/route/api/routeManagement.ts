/**
 * @file routeManagement.ts
 * @project Pipker Framework
 * @module Frontend Route Management
 * @description 封装只读系统路由管理页的分页和详情 API 调用。
 * @logic 以字符串形式传递雪花主键，全部请求仅使用 GET，不暴露任何新建、编辑或删除命令。
 * @dependencies requestApi、frontend API contracts
 * @index_tags api、route、system-menu、pagination、read-only
 * @author holic512
 */
import type {
  PageResult,
  SystemRouteDetail,
  SystemRouteMenuType,
  SystemRouteStatus,
  SystemRouteSummary,
} from '../../../../core/api/contracts'
import { requestApi } from '../../../../core/http/client'

export interface RoutePageQuery {
  page: number
  pageSize: number
  keyword?: string
  menuType?: SystemRouteMenuType
  visible?: boolean
  status?: SystemRouteStatus
}

/** 读取已落库路由与目录定义的分页视图。 */
export function getRoutePage(query: RoutePageQuery): Promise<PageResult<SystemRouteSummary>> {
  return requestApi<PageResult<SystemRouteSummary>>({
    method: 'GET',
    url: '/api/admin/routes',
    params: query,
  })
}

/** 读取一条路由定义的完整详情。 */
export function getRouteDetail(routeId: string): Promise<SystemRouteDetail> {
  return requestApi<SystemRouteDetail>({
    method: 'GET',
    url: `/api/admin/routes/${encodeURIComponent(routeId)}`,
  })
}
