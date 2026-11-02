import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { autenticarUsuario, type LoginRequest } from '@/services/authService'
import { clearAuthSession, readAuthName, readAuthRole, readAuthToken, saveAuthSession } from '@/services/authStorage'
import { USER_ROLES, type UserRole } from '@/types/auth'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(readAuthToken())
  const nome = ref(readAuthName() ?? '')
  const role = ref<UserRole | null>(readAuthRole())
  const isAuthenticated = computed(() => Boolean(token.value && role.value))
  const isCitizen = computed(() => isAuthenticated.value && role.value === 'ROLE_CIDADAO')
  const isAmmaAdmin = computed(() => isAuthenticated.value && role.value === 'ROLE_ADMINISTRADOR_AMMA')
  const canAccessManagement = computed(() => isAuthenticated.value && USER_ROLES.some(value => value !== 'ROLE_CIDADAO' && value === role.value))
  const canManageIfceTeam = computed(() => isAuthenticated.value && role.value === 'ROLE_GESTOR_IFCE')
  const dashboardRouteName = computed(() => isCitizen.value ? 'cidadao-dashboard' : canAccessManagement.value ? 'dashboard' : 'home')
  const profileRouteName = computed(() => isCitizen.value ? 'perfil' : 'perfil-institucional')

  async function login(credentials: LoginRequest) {
    const session = await autenticarUsuario(credentials)
    token.value = session.token
    nome.value = session.nome
    role.value = session.role
    saveAuthSession(session.token, session.nome, session.role)
    return session
  }

  function updateName(name: string) {
    nome.value = name
    if (token.value && role.value) saveAuthSession(token.value, name, role.value)
  }

  function logout() {
    token.value = null
    nome.value = ''
    role.value = null
    clearAuthSession()
  }

  return { token, nome, role, isAuthenticated, isCitizen, isAmmaAdmin, canAccessManagement, canManageIfceTeam, dashboardRouteName, profileRouteName, login, updateName, logout }
})
