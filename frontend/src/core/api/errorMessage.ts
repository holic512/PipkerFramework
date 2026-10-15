/**
 * @file errorMessage.ts
 * @project Pipker Framework
 * @module Frontend Error Feedback
 * @description Extracts user-facing business errors without exposing transport diagnostics.
 * @logic Uses non-empty business messages and otherwise preserves the caller's contextual fallback; never emits notifications.
 * @dependencies API contracts
 * @index_tags frontend, error, notification, api
 * @author holic512
 */
import { ApiBusinessError } from './contracts'

export function getErrorMessage(error: unknown, fallback: string): string {
  return error instanceof ApiBusinessError && error.message.trim()
    ? error.message
    : fallback
}
