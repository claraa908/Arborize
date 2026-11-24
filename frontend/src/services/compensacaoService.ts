import axios from 'axios'
import { api, type ApiRequestConfig } from '@/services/api'
import type { CompensacaoStatus, TermoCompensacao } from '@/types/compensacao'

export interface TermoCompensacaoPayload {
  razaoSocial: string
  cnpj: string
  numeroTermo?: string
  dataAssinatura?: string
  prazoEntrega?: string
  quantidadeAcordada: number
}

interface ProblemFieldError { field: string; message: string }
interface ProblemResponse { detail?: string; errors?: ProblemFieldError[] }
export class CompensacaoApiError extends Error {
  constructor(message: string, readonly fields: Record<string, string> = {}) { super(message); this.name = 'CompensacaoApiError' }
}

const quietErrorToast: ApiRequestConfig = { suppressGlobalErrorToast: true }

function erroAmigavel(error: unknown, mensagem: string): Error {
  if (axios.isAxiosError(error)) {
    const body = error.response?.data as ProblemResponse | undefined
    const fields = Object.fromEntries((body?.errors ?? []).map((item) => [item.field, item.message]))
    return new CompensacaoApiError(body?.detail || mensagem, fields)
  }
  return new Error(mensagem)
}

function normalizar(term: TermoCompensacao): TermoCompensacao {
  return {
    ...term,
    numeroTermo: term.numeroTermo || '',
    dataAssinatura: term.dataAssinatura || '',
    prazoEntrega: term.prazoEntrega || '',
    entregas: (term.entregas ?? []).map((item) => ({ ...item, registradaEm: item.registradaEm || '' }))
  }
}

export const compensacaoService = {
  async listar(): Promise<TermoCompensacao[]> {
    try { return (await api.get<TermoCompensacao[]>('/api/termos-compensacao', quietErrorToast)).data.map(normalizar) }
    catch (error) { throw erroAmigavel(error, 'Não foi possível carregar os termos de compensação.') }
  },
  async consultar(id: number): Promise<TermoCompensacao> {
    try { return normalizar((await api.get<TermoCompensacao>(`/api/termos-compensacao/${id}`, quietErrorToast)).data) }
    catch (error) { throw erroAmigavel(error, 'Não foi possível carregar este termo.') }
  },
  async criar(dados: TermoCompensacaoPayload, arquivo: File): Promise<TermoCompensacao> {
    const form = new FormData()
    form.append('dados', new Blob([JSON.stringify(dados)], { type: 'application/json' }))
    form.append('arquivo', arquivo)
    try { return normalizar((await api.post<TermoCompensacao>('/api/termos-compensacao', form, quietErrorToast)).data) }
    catch (error) { throw erroAmigavel(error, 'Não foi possível cadastrar o termo. Os dados foram mantidos.') }
  },
  async atualizar(id: number, dados: TermoCompensacaoPayload): Promise<TermoCompensacao> {
    try { return normalizar((await api.put<TermoCompensacao>(`/api/termos-compensacao/${id}`, dados, quietErrorToast)).data) }
    catch (error) { throw erroAmigavel(error, 'Não foi possível atualizar o termo. Os dados foram mantidos.') }
  },
  async atualizarStatus(id: number, status: CompensacaoStatus): Promise<TermoCompensacao> {
    try { return normalizar((await api.patch<TermoCompensacao>(`/api/termos-compensacao/${id}/status`, { status }, quietErrorToast)).data) }
    catch (error) { throw erroAmigavel(error, 'Não foi possível atualizar o estado do acordo.') }
  },
  async registrarEntrega(id: number, quantidade: number, recibo: File): Promise<TermoCompensacao> {
    const form = new FormData()
    form.append('quantidade', String(quantidade))
    form.append('recibo', recibo)
    try { return normalizar((await api.post<TermoCompensacao>(`/api/termos-compensacao/${id}/entregas`, form, quietErrorToast)).data) }
    catch (error) { throw erroAmigavel(error, 'Não foi possível registrar a entrega. Os dados foram mantidos.') }
  },
  async baixarDocumento(id: number, entregaId?: number): Promise<Blob> {
    const path = entregaId == null
      ? `/api/termos-compensacao/${id}/documento`
      : `/api/termos-compensacao/${id}/entregas/${entregaId}/documento`
    try { return (await api.get<Blob>(path, { ...quietErrorToast, responseType: 'blob' })).data }
    catch (error) { throw erroAmigavel(error, 'Não foi possível acessar o documento.') }
  }
}
