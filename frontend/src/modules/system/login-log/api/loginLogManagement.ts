/**
 * @file loginLogManagement.ts
 * @project Pipker Framework
 * @module Frontend Login Log Management API
 * @description 封装管理员认证生命周期日志只读分页查询。
 * @logic 将账号/IP/TraceId 关键词、事件、结果和 ISO 本地时间范围传给统一认证客户端，不提供写入、删除或导出请求。
 * @dependencies requestApi、frontend API contracts
 * @index_tags api、authentication、audit、login-log、pagination、read-only
 * @author holic512
 */

import type {
  AuthenticationEventResult,
  AuthenticationEventType,
  PageResult,
  SystemLoginLogSummary,
} from '../../../../core/api/contracts'
import { requestApi } from '../../../../core/http/client'

export interface LoginLogPageQuery {
  page: number
  pageSize: number
  keyword?: string
  eventType?: AuthenticationEventType
  result?: AuthenticationEventResult
  startTime?: string
  endTime?: string
}

export function getLoginLogPage(params: LoginLogPageQuery): Promise<PageResult<SystemLoginLogSummary>> {
  return requestApi<PageResult<SystemLoginLogSummary>>({
    method: 'GET',
    url: '/admin/login-logs',
    params,
  })
}
