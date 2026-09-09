/**
 * @file sessionStorage.ts
 * @project Pipker Framework
 * @module Frontend Authentication Storage
 * @description Encapsulates browser storage for the system Bearer token and the optional remembered username.
 * @logic Reads a current-session token before a persistent token, writes to exactly one token store, and provides the single cleanup path used by logout and invalid-session recovery.
 * @dependencies Web Storage API
 * @index_tags authentication, token, local-storage, session-storage, remembered-username
 * @author holic512
 */

const ACCESS_TOKEN_KEY = 'pipker.system.access-token'
const REMEMBERED_USERNAME_KEY = 'pipker.system.remembered-username'

export type SessionPersistence = 'session' | 'persistent'

export function readAccessToken(): string | null {
  return window.sessionStorage.getItem(ACCESS_TOKEN_KEY)
    ?? window.localStorage.getItem(ACCESS_TOKEN_KEY)
}

export function writeAccessToken(accessToken: string, persistence: SessionPersistence): void {
  clearAccessToken()
  const storage = persistence === 'persistent' ? window.localStorage : window.sessionStorage
  storage.setItem(ACCESS_TOKEN_KEY, accessToken)
}

export function clearAccessToken(): void {
  window.sessionStorage.removeItem(ACCESS_TOKEN_KEY)
  window.localStorage.removeItem(ACCESS_TOKEN_KEY)
}

export function readRememberedUsername(): string | null {
  return window.localStorage.getItem(REMEMBERED_USERNAME_KEY)
}

export function writeRememberedUsername(username: string): void {
  window.localStorage.setItem(REMEMBERED_USERNAME_KEY, username.trim())
}

export function clearRememberedUsername(): void {
  window.localStorage.removeItem(REMEMBERED_USERNAME_KEY)
}
