import { api } from '@/services/api'

export type InstitutionalRole = 'FUNCIONARIO_AMMA' | 'GESTOR_IFCE'

export interface InstitutionalUser {
  id: number
  nome: string
  email: string
  role: InstitutionalRole | 'IFCE_GESTOR'
  dataCadastro: string
  status: 'ATIVO' | 'INATIVO'
}

export interface CreateInstitutionalUserRequest {
  nome: string
  cpf: string
  email: string
  whatsapp: string
  senha: string
  role: InstitutionalRole
}

export async function listarUsuariosInstitucionais() {
  const response = await api.get<InstitutionalUser[]>('/admin/usuarios')
  return response.data
}

export async function cadastrarUsuarioInstitucional(payload: CreateInstitutionalUserRequest) {
  const response = await api.post<InstitutionalUser>('/admin/usuarios', payload)
  return response.data
}
