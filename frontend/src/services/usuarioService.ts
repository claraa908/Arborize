import { api } from '@/services/api'

export interface UsuarioCadastroRequest {
  nome: string
  cpf: string
  email: string
  whatsapp: string
  endereco: string
  senha: string
}

export interface UsuarioResponse {
  id: number
  nome: string
  cpf: string
  email: string
  whatsapp: string
  endereco: string
}

export interface UsuarioPerfilRequest {
  nome: string
  whatsapp: string
  endereco: string
}

export interface AlterarSenhaRequest {
  senhaAtual: string
  novaSenha: string
}

export async function cadastrarUsuario(dados: UsuarioCadastroRequest) {
  const response = await api.post<UsuarioResponse>('/usuarios/cadastro', dados)
  return response.data
}

export async function buscarMeuPerfil() {
  const response = await api.get<UsuarioResponse>('/usuarios/me')
  return response.data
}

export async function atualizarMeuPerfil(dados: UsuarioPerfilRequest) {
  const response = await api.put<UsuarioResponse>('/usuarios/me', dados)
  return response.data
}

export async function alterarMinhaSenha(dados: AlterarSenhaRequest) {
  await api.put('/usuarios/me/senha', dados)
}
