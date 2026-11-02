import { api } from '@/services/api'
import type { UserRole } from '@/types/auth'

export interface LoginRequest {
  login: string
  senha: string
}

export interface LoginResponse {
  token: string
  tipo: 'Bearer'
  nome: string
  role: UserRole
}

export async function autenticarUsuario(dados: LoginRequest) {
  const response = await api.post<LoginResponse>('/auth/login', dados)
  return response.data
}
