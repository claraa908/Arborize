// Dados exclusivamente de testes unitários; nunca importados pela aplicação.
import { AxiosError, AxiosHeaders, type AxiosResponse } from 'axios'
import type {
  ArvoreResumoResponseDTO,
  ArvoreResponseDTO,
  ArvorePaginaResponseDTO,
  ArvoreCadastroRequestDTO,
  ArvoreAtualizacaoRequestDTO,
} from '@/types/inventario'

export const resumo: ArvoreResumoResponseDTO = {
  id: 42,
  codigo: 'ARV-QXD-000042',
  especie: {
    id: 7,
    nomePopular: 'Espécie de teste',
    nomeCientifico: 'Nome de teste',
    familiaBotanica: null,
  },
  latitude: 0,
  longitude: 0,
  classificacaoTerritorial: 'BAIRRO_NAO_IDENTIFICADO',
  bairro: null,
  bairroNome: null,
  cep: null,
  referenciaLocalizacao: null,
  condicaoFitossanitaria: 'NAO_AVALIADA',
  fotoCapa: null,
}
export const arvore: ArvoreResponseDTO = {
  ...resumo,
  dataPlantio: null,
  dapCm: null,
  alturaEstimadaM: null,
  observacoes: null,
  criadoEm: '2026-01-01T12:00:00',
  atualizadoEm: '2026-01-01T12:00:00',
}
export const pagina: ArvorePaginaResponseDTO = {
  content: [resumo],
  page: 0,
  size: 20,
  totalElements: 1,
  totalPages: 1,
}
export const cadastro: ArvoreCadastroRequestDTO = {
  especieId: 7,
  latitude: 0,
  longitude: 0,
  classificacaoTerritorial: 'BAIRRO_NAO_IDENTIFICADO',
}
export const atualizacao: ArvoreAtualizacaoRequestDTO = {
  ...cadastro,
  bairroNome: null,
  cep: null,
  referenciaLocalizacao: null,
  dataPlantio: null,
  dapCm: null,
  alturaEstimadaM: null,
  condicaoFitossanitaria: 'NAO_AVALIADA',
  observacoes: null,
}
export function resposta<T>(data: T, status = 200): AxiosResponse<T> {
  return { data, status, statusText: '', headers: {}, config: { headers: new AxiosHeaders() } }
}
export function falhaHttp(status: number, data: unknown = ''): AxiosError<unknown> {
  return new AxiosError(
    'Mensagem interna de teste',
    'ERR_BAD_RESPONSE',
    undefined,
    undefined,
    resposta(data, status),
  )
}
