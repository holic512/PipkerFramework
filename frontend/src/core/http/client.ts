/**
 * @file client.ts
 * @project Pipker Framework
 * @module HTTP infrastructure
 * @description Provides the single configured Axios client for all business API modules.
 * @logic Applies runtime transport defaults and deliberately exposes raw Axios responses until the backend contract defines authentication, error, and response-envelope rules.
 * @dependencies Axios, runtime configuration
 * @index_tags frontend,http,axios,infrastructure
 * @author holic512
 */
import axios from 'axios'
import { runtimeConfig } from '../config/runtime'

export const httpClient = axios.create({
  baseURL: runtimeConfig.apiBaseUrl,
  timeout: runtimeConfig.httpTimeoutMs,
})
