export const solicitacaoStatus = {
  PENDENTE_RECICLAVEIS: { label: 'Aguardando recicláveis', badge: 'border-feedback-warning-border bg-feedback-warning-bg text-feedback-warning' },
  APROVADA: { label: 'Aprovada', badge: 'border-feedback-success-border bg-feedback-success-bg text-feedback-success' },
  RETIRADA_CONCLUIDA: { label: 'Retirada concluída', badge: 'border-feedback-success-border bg-feedback-success-bg text-feedback-success' },
  REJEITADA: { label: 'Rejeitada', badge: 'border-feedback-danger-border bg-feedback-danger-bg text-feedback-danger' }
} as const

export type SolicitacaoMudaStatus = keyof typeof solicitacaoStatus
export type LocalPlantioMuda = 'CALCADA' | 'QUINTAL'

export interface SolicitacaoMudaCartao {
  protocolo: string
  status: SolicitacaoMudaStatus
  especie: string
  quantidadeMudas: number
  totalRecicaveis: number
  nomeSolicitante: string
}

export interface SolicitacaoMudaDetalhe extends SolicitacaoMudaCartao {
  destino: string
  localPlantio: LocalPlantioMuda | null
  quantidadeRecebida: number | null
  motivoRejeicao: string | null
}

export interface SolicitacaoMudaPagina {
  content: SolicitacaoMudaCartao[]
  page: number
  totalPages: number
}

export interface SolicitacaoMudaForm {
  localPlantio?: LocalPlantioMuda
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
  status: SolicitacaoMudaStatus
  especie: { id: number; nomePopular: string }
  quantidadeMudas: number
  totalRecicaveis: number
  nomeSolicitante: string
  criadaEm: string
}

export interface AcompanharSolicitacaoMudaPayload {
  protocolo: string
  cpf: string
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
