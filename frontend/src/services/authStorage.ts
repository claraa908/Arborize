export const AUTH_TOKEN_KEY = 'arborize.auth.token'
export const AUTH_NAME_KEY = 'arborize.auth.name'

export function readAuthToken() {
  return typeof localStorage === 'undefined' ? null : localStorage.getItem(AUTH_TOKEN_KEY)
}

export function readAuthName() {
  return typeof localStorage === 'undefined' ? null : localStorage.getItem(AUTH_NAME_KEY)
}

export function saveAuthSession(token: string, name: string) {
  localStorage.setItem(AUTH_TOKEN_KEY, token)
  localStorage.setItem(AUTH_NAME_KEY, name)
}

export function clearAuthSession() {
  localStorage.removeItem(AUTH_TOKEN_KEY)
  localStorage.removeItem(AUTH_NAME_KEY)
}
