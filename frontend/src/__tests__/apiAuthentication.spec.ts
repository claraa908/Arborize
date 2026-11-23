import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import type { AxiosAdapter, InternalAxiosRequestConfig } from 'axios'
import { api } from '@/services/api'
import { saveAuthSession } from '@/services/authStorage'
import { buscarRecomendacoes } from '@/services/recomendacaoService'
import { solicitacaoMudaService } from '@/services/solicitacaoMudaService'

const storage = new Map<string, string>()
const requests: InternalAxiosRequestConfig[] = []
const adapter: AxiosAdapter = async (config) => {
  requests.push(config)
  const data = config.url?.endsWith('/conversao')
    ? { quantidade: 2, garrafasPorMuda: 10, totalGarrafas: 20 }
    : config.url?.endsWith('/solicitacoes-muda')
      ? { id: 1, protocolo: 'TESTE', status: 'PENDENTE' }
      : []
  return { data, status: 200, statusText: 'OK', headers: {}, config }
}

let originalAdapter: AxiosAdapter

beforeEach(() => {
  storage.clear()
  requests.length = 0
  vi.stubGlobal('localStorage', {
    getItem: (key: string) => storage.get(key) ?? null,
    setItem: (key: string, value: string) => storage.set(key, value),
    removeItem: (key: string) => storage.delete(key),
  })
  saveAuthSession('test-only-token', 'Pessoa Teste', 'ROLE_CIDADAO')
  originalAdapter = api.defaults.adapter as AxiosAdapter
  api.defaults.adapter = adapter
})

afterEach(() => {
  api.defaults.adapter = originalAdapter
  vi.unstubAllGlobals()
})

describe('autenticação das chamadas do fluxo de Arborização', () => {
  it('envia o token do usuário nas APIs de recomendação, conversão e solicitação', async () => {
    await buscarRecomendacoes([{ perguntaId: 'local', valor: 'quintal' }])
    await solicitacaoMudaService.calcularConversao(2)
    await solicitacaoMudaService.criarSolicitacao({
      especieId: 1,
      quantidadeMudas: 2,
      logradouro: 'Rua de Teste',
      numeroEndereco: '10',
      bairro: 'Centro',
      complemento: '',
      pontoReferencia: '',
      nomeSolicitante: 'Pessoa Teste',
      cpf: '123.456.789-01',
      telefone: '(88) 99999-9999',
    })

    expect(requests.map(({ method, url }) => `${method?.toUpperCase()} ${url}`)).toEqual([
      'POST /api/assistente-plantio/recomendar',
      'GET /api/solicitacoes-muda/conversao',
      'POST /api/solicitacoes-muda',
    ])
    expect(requests.every(({ headers }) => headers.Authorization === 'Bearer test-only-token')).toBe(true)
  })
})
