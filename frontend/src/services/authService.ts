import { api } from '@/services/api'

export interface LoginRequest {
  login: string
  senha: string
}

export interface LoginResponse {
  token: string
  tipo: 'Bearer'
  nome: string
}

export async function autenticarUsuario(dados: LoginRequest) {
  const response = await api.post<LoginResponse>('/auth/login', dados)
  return response.data
}
