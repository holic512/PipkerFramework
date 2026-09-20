/**
 * @file apiLogManagement.ts
 * @project Pipker Framework
 * @module Frontend API Monitor API
 * @description 封装接口访问概览、接口维度聚合、慢请求明细和详情的只读管理请求。
 * @logic 统一透传本地 ISO 时间范围、慢阈值、白名单排序和分页条件，不提供日志删除、导出或配置写入能力。
 * @dependencies requestApi、frontend API contracts
 * @index_tags api、api-monitor、observability、performance、slow-request、read-only
 * @author holic512
 */

import type {
  ApiAccessEndpointSummary,
  ApiAccessLogDetail,
  ApiAccessLogSummary,
  ApiAccessOverview,
  PageResult,
} from '../../../../core/api/contracts'
import { requestApi } from '../../../../core/http/client'

export type ApiEndpointSort = 'REQUEST_COUNT' | 'SUCCESS_RATE' | 'AVG_DURATION' | 'MAX_DURATION' | 'SLOW_COUNT'
export type SortDirection = 'ASC' | 'DESC'

export interface ApiLogTimeQuery {
  startTime?: string
  endTime?: string
}

export interface ApiLogOverviewQuery extends ApiLogTimeQuery {
  slowThresholdMs?: number
  includeMonitorEndpoints?: boolean
}

export interface ApiEndpointPageQuery extends ApiLogOverviewQuery {
  page: number
  pageSize: number
  keyword?: string
  sortBy?: ApiEndpointSort
  sortDirection?: SortDirection
}

export interface ApiAccessLogPageQuery extends ApiLogTimeQuery {
  page: number
  pageSize: number
  keyword?: string
  httpMethod?: string
  success?: boolean
  minDurationMs?: number
  routePattern?: string
  unresolvedRoute?: boolean
}

export function getApiLogOverview(params: ApiLogOverviewQuery): Promise<ApiAccessOverview> {
  return requestApi<ApiAccessOverview>({ method: 'GET', url: '/admin/api-logs/overview', params })
}

export function getApiEndpointPage(params: ApiEndpointPageQuery): Promise<PageResult<ApiAccessEndpointSummary>> {
  return requestApi<PageResult<ApiAccessEndpointSummary>>({ method: 'GET', url: '/admin/api-logs/endpoints', params })
}

export function getApiAccessLogPage(params: ApiAccessLogPageQuery): Promise<PageResult<ApiAccessLogSummary>> {
  return requestApi<PageResult<ApiAccessLogSummary>>({ method: 'GET', url: '/admin/api-logs', params })
}

export function getApiAccessLogDetail(logId: string): Promise<ApiAccessLogDetail> {
  return requestApi<ApiAccessLogDetail>({ method: 'GET', url: `/admin/api-logs/${encodeURIComponent(logId)}` })
}
