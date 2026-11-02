import axios, { type AxiosRequestConfig } from 'axios'
import { useFeedbackStore } from '@/stores/feedbackStore'

export type ApiRequestConfig = AxiosRequestConfig & {
  suppressGlobalErrorToast?: boolean
}

export const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL || 'http://localhost:8080',
})

function objeto(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null && !Array.isArray(value)
}

function nomeCampo(campo: string): string {
  const nomes: Record<string, string> = {
    especieId: 'espécie',
    latitude: 'latitude',
    longitude: 'longitude',
    classificacaoTerritorial: 'situação territorial',
    bairroId: 'bairro',
    bairroNome: 'bairro',
    cep: 'CEP',
    referenciaLocalizacao: 'referência de localização',
    dataPlantio: 'data de plantio',
    dapCm: 'DAP (cm)',
    alturaEstimadaM: 'altura estimada (m)',
    condicaoFitossanitaria: 'condição fitossanitária',
    observacoes: 'observações',
    file: 'fotografia',
  }
  return nomes[campo] ?? campo
}

api.interceptors.response.use(
  (response) => response,
  (error: unknown) => {
    if (axios.isAxiosError<unknown>(error)) {
      const status = error.response?.status
      const config = error.config as ApiRequestConfig | undefined
      if (status != null && status >= 400 && status < 600 && !config?.suppressGlobalErrorToast) {
        const body = error.response?.data
        const detail = objeto(body) && typeof body.detail === 'string' ? body.detail : ''
        const title = objeto(body) && typeof body.title === 'string' ? body.title : ''
        const fieldErrors =
          objeto(body) && Array.isArray(body.errors)
            ? body.errors.flatMap((item) => {
                if (
                  !objeto(item) ||
                  typeof item.field !== 'string' ||
                  typeof item.message !== 'string'
                ) {
                  return []
                }
                return [`${nomeCampo(item.field)}: ${item.message}`]
              })
            : []
        const message = [detail || title || `A solicitação falhou (HTTP ${status}).`, ...fieldErrors]
          .filter(Boolean)
          .join(' ')
        const feedback = useFeedbackStore()
        feedback.showToast(
          message,
          'error',
          6000,
          status === 401 || status === 403 ? 'Acesso negado' : title || 'Falha na solicitação',
        )
      }
    }
    return Promise.reject(error)
  },
)
