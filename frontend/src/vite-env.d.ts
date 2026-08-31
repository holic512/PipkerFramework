/**
 * @file vite-env.d.ts
 * @project Pipker Framework
 * @module Runtime environment typing
 * @description Declares the public Vite variables used by the frontend transport boundary.
 * @logic Limits browser-exposed configuration to the VITE_* values documented in .env.example.
 * @dependencies Vite client types
 * @index_tags frontend,vite,environment,typing
 * @author holic512
 */
/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_API_BASE_URL?: string
  readonly VITE_HTTP_TIMEOUT_MS?: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}
