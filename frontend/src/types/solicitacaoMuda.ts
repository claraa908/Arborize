export interface SolicitacaoMudaForm {
  especieId: number
  quantidadeMudas: number
  logradouro: string
  numeroEndereco: string
  bairro: string
  complemento: string
  pontoReferencia: string
  nomeSolicitante: string
  cpf: string
  telefone: string
}

export interface ConversaoReciclagem {
  quantidade: number
  garrafasPorMuda: number
  totalGarrafas: number
}

export interface SolicitacaoMudaResponse {
  id: number
  protocolo: string
  status: string
  especie: { id: number; nomePopular: string }
  quantidadeMudas: number
  totalRecicaveis: number
  nomeSolicitante: string
  criadaEm: string
}

export interface ProblemDetailResponse {
  type?: string
  title?: string
  status?: number
  detail?: string
  code?: string
  errors?: Record<string, string>
}

export class SolicitacaoMudaError extends Error {
  constructor(
    message: string,
    readonly status?: number,
    readonly problem?: ProblemDetailResponse
  ) {
    super(message)
    this.name = 'SolicitacaoMudaError'
  }
}
