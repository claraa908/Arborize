export type TipoMovimentacao = 'ENTRADA_GERMINACAO' | 'SAIDA_PLANTIO' | 'BAIXA_PERDA'

export interface MovimentacaoMudaDTO {
  id: number
  tipo: TipoMovimentacao
  especieId: number
  especieNomePopular: string
  especieNomeCientifico: string
  quantidade: number
  data: string
  destino: string | null
  responsavel: string | null
  observacoes: string | null
  comprovanteNome: string | null
}

export interface MovimentacaoMudaPayload {
  tipo: TipoMovimentacao
  especieId: number
  quantidade: number
  data: string
  destino: string
  responsavel: string
  observacoes: string
  comprovante: File | null
}

export interface MovimentacaoMudaFiltros {
  tipo?: TipoMovimentacao | ''
  especieId?: number | ''
  dataInicio?: string
  dataFim?: string
  busca?: string
  pagina?: number
}

export interface PaginaMovimentacoes {
  content: MovimentacaoMudaDTO[]
  number: number
  totalElements: number
  totalPages: number
}

export interface ResumoMovimentacoes {
  saidasNoMes: number
  perdasNoMes: number
}
