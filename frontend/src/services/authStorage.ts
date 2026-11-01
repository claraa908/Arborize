import { isUserRole, type UserRole } from '@/types/auth'

export const AUTH_TOKEN_KEY = 'arborize.auth.token'
export const AUTH_NAME_KEY = 'arborize.auth.name'
export const AUTH_ROLE_KEY = 'arborize.auth.role'

export function readAuthToken() {
  return typeof localStorage === 'undefined' ? null : localStorage.getItem(AUTH_TOKEN_KEY)
}

export function readAuthName() {
  return typeof localStorage === 'undefined' ? null : localStorage.getItem(AUTH_NAME_KEY)
}

export function readAuthRole(): UserRole | null {
  if (typeof localStorage === 'undefined') return null
  const role = localStorage.getItem(AUTH_ROLE_KEY)
  return isUserRole(role) ? role : null
}

export function saveAuthSession(token: string, name: string, role: UserRole) {
  localStorage.setItem(AUTH_TOKEN_KEY, token)
  localStorage.setItem(AUTH_NAME_KEY, name)
  localStorage.setItem(AUTH_ROLE_KEY, role)
}

export function clearAuthSession() {
  localStorage.removeItem(AUTH_TOKEN_KEY)
  localStorage.removeItem(AUTH_NAME_KEY)
  localStorage.removeItem(AUTH_ROLE_KEY)
}
