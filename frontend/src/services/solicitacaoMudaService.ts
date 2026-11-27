import axios from 'axios'
import { api, type ApiRequestConfig } from '@/services/api'
import type {
  AcompanharSolicitacaoMudaPayload,
  ConversaoReciclagem,
  ProblemDetailResponse,
  SolicitacaoMudaForm,
  SolicitacaoMudaDetalhe,
  SolicitacaoMudaPagina,
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
  async listar(protocolo: string, page: number, size = 10): Promise<SolicitacaoMudaPagina> {
    try {
      const config: ApiRequestConfig = { params: { protocolo, page, size }, suppressGlobalErrorToast: true }
      return (await api.get<SolicitacaoMudaPagina>(gestao, config)).data
    } catch (error) {
      throw toDomainError(error, 'Não foi possível carregar as solicitações.')
    }
  },

  async detalhar(protocolo: string): Promise<SolicitacaoMudaDetalhe> {
    try {
      return (await api.get<SolicitacaoMudaDetalhe>(rotaGestao(protocolo), configGestao)).data
    } catch (error) {
      throw toDomainError(error, 'Não foi possível carregar a solicitação.')
    }
  },

  receber(protocolo: string, quantidadeRecebida: number) {
    return decidir(protocolo, 'recebimento', { quantidadeRecebida })
  },

  async documento(protocolo: string): Promise<Blob> {
    try {
      return (await api.get<Blob>(rotaGestao(protocolo) + '/documento', {
        ...configGestao,
        responseType: 'blob'
      })).data
    } catch (error) {
      throw toDomainError(error, 'Não foi possível baixar o documento de retirada.')
    }
  },

  rejeitar(protocolo: string, motivo: string) {
    return decidir(protocolo, 'rejeicao', { motivo })
  },

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

const gestao = '/api/solicitacoes-muda/gestao'
const configGestao: ApiRequestConfig = { suppressGlobalErrorToast: true }

function rotaGestao(protocolo: string) {
  return `${gestao}/${encodeURIComponent(protocolo)}`
}

async function decidir(protocolo: string, acao: 'recebimento' | 'rejeicao', dados: object) {
  try {
    return (await api.post<SolicitacaoMudaDetalhe>(`${rotaGestao(protocolo)}/${acao}`, dados, configGestao)).data
  } catch (error) {
    throw toDomainError(error, 'Não foi possível concluir a operação. Tente novamente.')
  }
}
