import { api, type ApiRequestConfig } from '@/services/api'
import type {
  MovimentacaoMudaDTO,
  MovimentacaoMudaFiltros,
  MovimentacaoMudaPayload,
  PaginaMovimentacoes,
  ResumoMovimentacoes,
} from '@/types/movimentacaoMuda'

const rota = '/api/viveiro/movimentacoes'
const configSemToast: ApiRequestConfig = { suppressGlobalErrorToast: true }

function criarFormulario(payload: MovimentacaoMudaPayload): FormData {
  const dados = new FormData()
  dados.append('tipo', payload.tipo)
  dados.append('especieId', String(payload.especieId))
  dados.append('quantidade', String(payload.quantidade))
  dados.append('data', payload.data)
  dados.append('destino', payload.destino)
  dados.append('responsavel', payload.responsavel)
  dados.append('observacoes', payload.observacoes)
  if (payload.comprovante) dados.append('comprovante', payload.comprovante)
  return dados
}

export const movimentacaoMudaService = {
  async listar(filtros: MovimentacaoMudaFiltros, pagina = 0): Promise<PaginaMovimentacoes> {
    const response = await api.get<PaginaMovimentacoes>(rota, {
      ...configSemToast,
      params: {
        tipo: filtros.tipo || undefined,
        especieId: filtros.especieId || undefined,
        dataInicio: filtros.dataInicio || undefined,
        dataFim: filtros.dataFim || undefined,
        busca: filtros.busca?.trim() || undefined,
        page: pagina,
        size: 10,
        sort: 'data,desc',
      },
    })
    return response.data
  },

  async buscar(id: number): Promise<MovimentacaoMudaDTO> {
    const response = await api.get<MovimentacaoMudaDTO>(`${rota}/${id}`, configSemToast)
    return response.data
  },

  async resumo(): Promise<ResumoMovimentacoes> {
    const response = await api.get<ResumoMovimentacoes>(`${rota}/resumo`, configSemToast)
    return response.data
  },

  async criar(payload: MovimentacaoMudaPayload): Promise<MovimentacaoMudaDTO> {
    const response = await api.post<MovimentacaoMudaDTO>(rota, criarFormulario(payload), {
      ...configSemToast,
      headers: { 'Content-Type': 'multipart/form-data' },
    })
    return response.data
  },

  async atualizar(id: number, payload: MovimentacaoMudaPayload): Promise<MovimentacaoMudaDTO> {
    const response = await api.put<MovimentacaoMudaDTO>(`${rota}/${id}`, criarFormulario(payload), {
      ...configSemToast,
      headers: { 'Content-Type': 'multipart/form-data' },
    })
    return response.data
  },

  async comprovante(id: number): Promise<Blob> {
    const response = await api.get<Blob>(`${rota}/${id}/comprovante`, {
      ...configSemToast,
      responseType: 'blob',
    })
    return response.data
  },
}
