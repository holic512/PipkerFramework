/**
 * @file client.ts
 * @project Pipker Framework
 * @module Frontend HTTP Infrastructure
 * @description Provides the single Axios client, fresh Bearer token injection, and code/data/message envelope decoding.
 * @logic Adds the session token before every request, rejects non-200 envelopes as typed business errors, and exposes payload-only requests.
 * @dependencies Axios, runtimeConfig, sessionStorage, API contracts
 * @index_tags http, axios, authentication, api-response
 * @author holic512
 */
import axios from 'axios'
import type { AxiosRequestConfig, AxiosResponse, InternalAxiosRequestConfig } from 'axios'
import { runtimeConfig } from '../config/runtime'
import { clearAccessToken, readAccessToken } from '../auth/sessionStorage'
import { API_CODE, ApiBusinessError, type ApiResponse } from '../api/contracts'

export const httpClient = axios.create({
  baseURL: runtimeConfig.apiBaseUrl,
  timeout: runtimeConfig.httpTimeoutMs,
})

httpClient.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const accessToken = readAccessToken()
  if (accessToken) {
    config.headers.set('Authorization', `Bearer ${accessToken}`)
  }
  return config
})

httpClient.interceptors.response.use((response: AxiosResponse<ApiResponse<unknown>>) => {
  const envelope = response.data
  if (envelope?.code === API_CODE.SUCCESS) {
    return response
  }

  const error = new ApiBusinessError(
    envelope?.code ?? API_CODE.INTERNAL_ERROR,
    envelope?.message || 'The server returned an invalid response.',
  )
  if (error.code === API_CODE.AUTH_REQUIRED) {
    clearAccessToken()
  }
  return Promise.reject(error)
})

export async function requestApi<T>(config: AxiosRequestConfig): Promise<T> {
  const response = await httpClient.request<ApiResponse<T>>(config)
  if (response.data.data === null) {
    throw new ApiBusinessError(API_CODE.INTERNAL_ERROR, 'The server did not return the expected data.')
  }
  return response.data.data
}
