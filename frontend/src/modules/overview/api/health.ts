/**
 * @file health.ts
 * @project Pipker Framework
 * @module Overview module API
 * @description Defines the confirmed backend health-check request owned by the overview module.
 * @logic Calls the plain-text GET /api/ping endpoint through the shared HTTP client and returns only its documented response body.
 * @dependencies Axios HTTP client
 * @index_tags frontend,module,overview,api,health-check
 * @author holic512
 */
import { httpClient } from '../../../core/http/client'

export async function getBackendHealth(): Promise<string> {
  const response = await httpClient.get<string>('/ping')
  return response.data
}
