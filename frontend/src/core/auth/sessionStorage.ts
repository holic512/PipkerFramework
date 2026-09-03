/**
 * @file sessionStorage.ts
 * @project Pipker Framework
 * @module Frontend Authentication Storage
 * @description Encapsulates the short-lived browser storage used for the system Bearer token.
 * @logic Reads the token for every request and provides the single removal path used by logout and invalid-session recovery.
 * @dependencies Web Storage API
 * @index_tags authentication, token, session-storage
 * @author holic512
 */

const ACCESS_TOKEN_KEY = 'pipker.system.access-token'

export function readAccessToken(): string | null {
  return window.sessionStorage.getItem(ACCESS_TOKEN_KEY)
}

export function writeAccessToken(accessToken: string): void {
  window.sessionStorage.setItem(ACCESS_TOKEN_KEY, accessToken)
}

export function clearAccessToken(): void {
  window.sessionStorage.removeItem(ACCESS_TOKEN_KEY)
}
