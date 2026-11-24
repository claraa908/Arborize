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
  email?: string
  whatsapp?: string
  endereco?: string
}

export interface AlterarSenhaRequest {
  senhaAtual: string
  novaSenha: string
}

export interface SolicitacaoMudaAcompanhamento {
  id: number
  protocolo: string
  especieNome: string
  quantidadeMudas: number
  totalRecicaveis: number
  quantidadeRecebida: number | null
  status: string
  motivoRejeicao: string | null
  logradouro?: string | null
  numeroEndereco?: string | null
  bairro: string
  complemento?: string | null
  pontoReferencia?: string | null
  localPlantio?: string | null
  criadaEm: string
}

export interface SubstituicaoEventoHistorico {
  status: string
  descricao: string
  criadoEm: string
}

export interface SubstituicaoAcompanhamento {
  id: number
  protocolo: string
  status: string
  bairro: string
  logradouro: string
  numeroEndereco: string
  complemento?: string | null
  pontoReferencia?: string | null
  descricaoArvore: string
  criadaEm: string
  historico?: SubstituicaoEventoHistorico[]
}

export interface CidadaoAcompanhamentosResponse {
  solicitacoesMuda: SolicitacaoMudaAcompanhamento[]
  substituicoesBotanicas: SubstituicaoAcompanhamento[]
}

export async function cadastrarUsuario(dados: UsuarioCadastroRequest) {
  const response = await api.post<UsuarioResponse>('/usuarios/cadastro', dados)
  return response.data
}

export async function buscarMeuPerfil() {
  const response = await api.get<UsuarioResponse>('/usuarios/me')
  return response.data
}

export async function buscarMeusAcompanhamentos() {
  const response = await api.get<CidadaoAcompanhamentosResponse>('/usuarios/me/acompanhamentos')
  return response.data
}

export async function atualizarMeuPerfil(dados: UsuarioPerfilRequest) {
  const response = await api.put<UsuarioResponse>('/usuarios/me', dados)
  return response.data
}

export async function alterarMinhaSenha(dados: AlterarSenhaRequest) {
  await api.put('/usuarios/me/senha', dados)
}
