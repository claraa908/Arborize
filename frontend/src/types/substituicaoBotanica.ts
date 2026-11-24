export type SubstituicaoBotanicaStatus =
  | 'RECEBIDA'
  | 'EM_ANALISE'
  | 'APROVADA'
  | 'REJEITADA'
  | 'LAUDO_EMITIDO'
  | 'RETIRADA_EM_ANDAMENTO'
  | 'REPLANTIO_EM_ANDAMENTO'
  | 'CONCLUIDA'

export interface SubstituicaoEvento {
  status: SubstituicaoBotanicaStatus
  descricao: string
  criadoEm: string
}

export interface SubstituicaoRastreio {
  protocolo: string
  status: SubstituicaoBotanicaStatus
  criadaEm: string
  bairro: string
  descricaoArvore: string
  especieReposicao: string | null
  eventos: SubstituicaoEvento[]
}

export interface SubstituicaoGestao {
  id: number
  protocolo: string
  nomeSolicitante: string
  cpf: string
  telefone: string
  logradouro: string
  numeroEndereco: string
  bairro: string
  complemento: string | null
  pontoReferencia: string
  descricaoArvore: string
  nomeFoto: string | null
  status: SubstituicaoBotanicaStatus
  criadaEm: string
  atualizadaEm: string
  parecer: string | null
  tipoJustificativa: string | null
  justificativa: string | null
  especieReposicao: string | null
  dataVistoria: string | null
  nomePdf: string | null
}

export interface AcompanharSubstituicaoPayload {
  protocolo: string
  cpf: string
}

export interface LaudoSubstituicaoPayload {
  especieReposicaoId: number
  parecer: string
  tipoJustificativa: string
  justificativa: string
  dataVistoria: string
  arquivo: File
}
