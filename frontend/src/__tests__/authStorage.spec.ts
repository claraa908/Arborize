// @vitest-environment node
import { afterEach, beforeEach, describe, expect, it } from 'vitest'
import {
  AUTH_NAME_KEY,
  AUTH_ROLE_KEY,
  AUTH_TOKEN_KEY,
  clearAuthSession,
  readAuthName,
  readAuthRole,
  readAuthToken,
  saveAuthSession
} from '@/services/authStorage'

const values = new Map<string, string>()

const localStorageMock = {
  getItem: (key: string) => values.get(key) ?? null,
  setItem: (key: string, value: string) => values.set(key, value),
  removeItem: (key: string) => values.delete(key)
}

describe('authStorage', () => {
  beforeEach(() => {
    values.clear()
    Object.defineProperty(globalThis, 'localStorage', {
      configurable: true,
      value: localStorageMock
    })
  })

  afterEach(() => {
    Reflect.deleteProperty(globalThis, 'localStorage')
  })

  it('persists the token, user name, and role for a restored session', () => {
    saveAuthSession('jwt-token', 'Ana Silva', 'ROLE_AMMA')

    expect(readAuthToken()).toBe('jwt-token')
    expect(readAuthName()).toBe('Ana Silva')
    expect(readAuthRole()).toBe('ROLE_AMMA')
  })

  it('clears all session fields on logout', () => {
    saveAuthSession('jwt-token', 'Ana Silva', 'ROLE_CIDADAO')

    clearAuthSession()

    expect(readAuthToken()).toBeNull()
    expect(readAuthName()).toBeNull()
    expect(readAuthRole()).toBeNull()
    expect([...values.keys()]).not.toContain(AUTH_TOKEN_KEY)
    expect([...values.keys()]).not.toContain(AUTH_NAME_KEY)
    expect([...values.keys()]).not.toContain(AUTH_ROLE_KEY)
  })
})
