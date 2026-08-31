/**
 * @file runtime.ts
 * @project Pipker Framework
 * @module Runtime configuration
 * @description Normalizes public Vite variables into the application runtime configuration.
 * @logic Validates optional string values once at startup and exposes safe fallbacks to infrastructure modules.
 * @dependencies Vite import.meta.env
 * @index_tags frontend,configuration,runtime,http
 * @author holic512
 */
const DEFAULT_API_BASE_URL = '/api'
const DEFAULT_HTTP_TIMEOUT_MS = 10_000

function readNonEmpty(value: string | undefined, fallback: string): string {
  const normalizedValue = value?.trim()
  return normalizedValue ? normalizedValue : fallback
}

function readPositiveInteger(value: string | undefined, fallback: number): number {
  const parsedValue = Number(value)
  return Number.isInteger(parsedValue) && parsedValue > 0 ? parsedValue : fallback
}

export const runtimeConfig = {
  apiBaseUrl: readNonEmpty(import.meta.env.VITE_API_BASE_URL, DEFAULT_API_BASE_URL),
  httpTimeoutMs: readPositiveInteger(
    import.meta.env.VITE_HTTP_TIMEOUT_MS,
    DEFAULT_HTTP_TIMEOUT_MS,
  ),
} as const
