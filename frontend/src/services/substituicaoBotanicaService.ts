import { api } from '@/services/api'
import type {
  AcompanharSubstituicaoPayload,
  LaudoSubstituicaoPayload,
  SubstituicaoGestao,
  SubstituicaoRastreio,
  SubstituicaoBotanicaStatus
} from '@/types/substituicaoBotanica'

function requestFormData<T extends object>(form: T) {
  const data = new FormData()
  Object.entries(form).forEach(([key, value]) => {
    if (value instanceof File || typeof value === 'string' || typeof value === 'number') {
      if (value !== '') data.append(key, value instanceof File ? value : String(value))
    }
  })
  return data
}

export const substituicaoBotanicaService = {
  async criar(form: FormData): Promise<SubstituicaoRastreio> {
    const response = await api.post<SubstituicaoRastreio>('/api/substituicoes-botanicas', form)
    return response.data
  },

  async acompanhar(payload: AcompanharSubstituicaoPayload): Promise<SubstituicaoRastreio> {
    const response = await api.post<SubstituicaoRastreio>('/api/substituicoes-botanicas/acompanhar', {
      protocolo: payload.protocolo.trim(),
      cpf: payload.cpf.replace(/\D/g, '')
    })
    return response.data
  },

  async listarGestao(): Promise<SubstituicaoGestao[]> {
    const response = await api.get<SubstituicaoGestao[]>('/api/substituicoes-botanicas/gestao')
    return response.data
  },

  async buscarGestao(id: number): Promise<SubstituicaoGestao> {
    const response = await api.get<SubstituicaoGestao>(`/api/substituicoes-botanicas/gestao/${id}`)
    return response.data
  },

  async iniciarAnalise(id: number): Promise<SubstituicaoGestao> {
    const response = await api.put<SubstituicaoGestao>(`/api/substituicoes-botanicas/gestao/${id}/analise`)
    return response.data
  },

  async decidir(id: number, status: 'APROVADA' | 'REJEITADA', justificativa?: string): Promise<SubstituicaoGestao> {
    const response = await api.put<SubstituicaoGestao>(`/api/substituicoes-botanicas/gestao/${id}/decisao`, { status, justificativa })
    return response.data
  },

  async registrarProgresso(id: number, status: SubstituicaoBotanicaStatus, descricao: string): Promise<SubstituicaoGestao> {
    const response = await api.post<SubstituicaoGestao>(`/api/substituicoes-botanicas/gestao/${id}/eventos`, { status, descricao })
    return response.data
  },

  async emitirLaudo(id: number, payload: LaudoSubstituicaoPayload): Promise<SubstituicaoGestao> {
    const form = requestFormData(payload)
    const response = await api.post<SubstituicaoGestao>(`/api/substituicoes-botanicas/gestao/${id}/laudo`, form)
    return response.data
  },

  async baixarLaudo(id: number): Promise<Blob> {
    const response = await api.get<Blob>(`/api/substituicoes-botanicas/gestao/${id}/laudo/arquivo`, { responseType: 'blob' })
    return response.data
  },

  async carregarFoto(id: number): Promise<Blob> {
    const response = await api.get<Blob>(`/api/substituicoes-botanicas/gestao/${id}/foto`, { responseType: 'blob' })
    return response.data
  }
}
