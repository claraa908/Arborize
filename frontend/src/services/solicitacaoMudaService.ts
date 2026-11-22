import axios from 'axios'
import { api } from '@/services/api'
import type {
  AcompanharSolicitacaoMudaPayload,
  ConversaoReciclagem,
  ProblemDetailResponse,
  SolicitacaoMudaForm,
  SolicitacaoMudaResponse
} from '@/types/solicitacaoMuda'
import { SolicitacaoMudaError } from '@/types/solicitacaoMuda'

function toDomainError(error: unknown, fallback: string): SolicitacaoMudaError {
  if (axios.isAxiosError<ProblemDetailResponse>(error)) {
    return new SolicitacaoMudaError(fallback, error.response?.status, error.response?.data)
  }
  return new SolicitacaoMudaError(fallback)
}

export const solicitacaoMudaService = {
  async acompanharSolicitacao(payload: AcompanharSolicitacaoMudaPayload): Promise<SolicitacaoMudaResponse> {
    try {
      const response = await api.post<SolicitacaoMudaResponse>('/api/solicitacoes-muda/acompanhar', {
        protocolo: payload.protocolo.trim(),
        cpf: payload.cpf.replace(/\D/g, '')
      })
      return response.data
    } catch (error) {
      throw toDomainError(error, 'Não encontramos uma solicitação com esses dados.')
    }
  },

  async calcularConversao(quantidade: number): Promise<ConversaoReciclagem> {
    try {
      const response = await api.get<ConversaoReciclagem>('/api/solicitacoes-muda/conversao', {
        params: { quantidade }
      })
      return response.data
    } catch (error) {
      throw toDomainError(error, 'Não foi possível calcular os recicláveis necessários.')
    }
  },

  async criarSolicitacao(form: SolicitacaoMudaForm): Promise<SolicitacaoMudaResponse> {
    const payload = {
      ...form,
      cpf: form.cpf.replace(/\D/g, ''),
      telefone: form.telefone.replace(/\D/g, '')
    }
    try {
      const response = await api.post<SolicitacaoMudaResponse>('/api/solicitacoes-muda', payload)
      return response.data
    } catch (error) {
      throw toDomainError(error, 'Não foi possível enviar sua solicitação agora. Seus dados foram mantidos.')
    }
  }
}
