import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { autenticarUsuario, type LoginRequest } from '@/services/authService'
import { clearAuthSession, readAuthName, readAuthToken, saveAuthSession } from '@/services/authStorage'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(readAuthToken())
  const nome = ref(readAuthName() ?? '')
  const isAuthenticated = computed(() => Boolean(token.value))

  async function login(credentials: LoginRequest) {
    const session = await autenticarUsuario(credentials)
    token.value = session.token
    nome.value = session.nome
    saveAuthSession(session.token, session.nome)
    return session
  }

  function updateName(name: string) {
    nome.value = name
    if (token.value) saveAuthSession(token.value, name)
  }

  function logout() {
    token.value = null
    nome.value = ''
    clearAuthSession()
  }

  return { token, nome, isAuthenticated, login, updateName, logout }
})
