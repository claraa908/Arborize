// @vitest-environment node
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { api } from '@/services/api'
import { useInventarioConsulta } from '@/components/inventario/useInventarioConsulta'
import type { ArvoreResumoResponseDTO, InventarioListagemParams } from '@/types/inventario'
import { resumo, resposta, falhaHttp } from './inventarioFixtures'

const feedbackLocalDeErro = { suppressGlobalErrorToast: true }

// API simulada exclusivamente neste arquivo de teste. Não é importada pela aplicação.
const registros: ArvoreResumoResponseDTO[] = Array.from({ length: 25 }, (_, index) => ({
  ...resumo,
  id: index + 1,
  codigo: `ARV-QXD-${String(index + 1).padStart(6, '0')}`,
  especie: {
    ...resumo.especie,
    id: index < 15 ? 7 : 8,
    nomePopular: index < 15 ? 'Espécie teste A' : 'Espécie teste B',
    nomeCientifico: index < 15 ? 'Scientific alpha' : 'Scientific beta',
  },
  bairro: {
    id: index < 15 ? 3 : 4,
    nome: index < 15 ? 'Bairro teste A' : 'Bairro teste B',
    ativo: true,
  },
  bairroNome: index < 15 ? 'Bairro teste A' : 'Bairro teste B',
  cep: null,
  condicaoFitossanitaria: index % 2 ? 'MORTA' : 'NAO_AVALIADA',
}))

beforeEach(() => setActivePinia(createPinia()))
afterEach(() => vi.restoreAllMocks())

function simularConsulta() {
  return vi.spyOn(api, 'get').mockImplementation(async (url, config) => {
    const params = config?.params as InventarioListagemParams
    const search = params.search?.trim().toLocaleLowerCase('pt-BR') ?? ''
    const filtered = registros.filter(
      (item) =>
        (!search ||
          String(item.id).includes(search) ||
          item.codigo.toLocaleLowerCase('pt-BR').includes(search) ||
          item.especie.nomePopular.toLowerCase().includes(search) ||
          item.especie.nomeCientifico.toLowerCase().includes(search)) &&
        (!params.bairro ||
          (item.bairroNome ?? item.bairro?.nome ?? '')
            .toLocaleLowerCase('pt-BR')
            .includes(params.bairro.trim().toLocaleLowerCase('pt-BR'))) &&
        (params.especieId == null || item.especie.id === params.especieId) &&
        (params.condicao == null || item.condicaoFitossanitaria === params.condicao),
    )
    if (url === '/api/arvores/mapa') return resposta(filtered)
    const page = params.page ?? 0,
      size = params.size ?? 20
    return resposta({
      content: filtered.slice(page * size, (page + 1) * size),
      page,
      size,
      totalElements: filtered.length,
      totalPages: Math.ceil(filtered.length / size),
    })
  })
}

describe('T13: filtros e paginação com service e store reais, HTTP simulado', () => {
  it('consulta inicial usa tabela paginada e mapa completo sem carregar catálogo de bairros', async () => {
    const get = simularConsulta()
    const consulta = useInventarioConsulta()
    await consulta.consultar()
    expect(get).toHaveBeenCalledTimes(2)
    expect(consulta.store.pagina?.content).toHaveLength(20)
    expect(consulta.store.registrosMapa).toHaveLength(25)
    expect(consulta.store.pagina?.totalElements).toBe(25)
    expect(consulta.store.opcoesEspecies.map((item) => item.id)).toEqual([7, 8])
    expect(get.mock.calls.some(([url]) => url === '/api/bairros')).toBe(false)
  })

  it.each(['2', 'teste B', 'scientific beta'])(
    'pesquisa parcial por %s sincroniza mapa e tabela',
    async (search) => {
      simularConsulta()
      const consulta = useInventarioConsulta()
      consulta.criterios.search = search
      await consulta.aplicarFiltros()
      expect(consulta.store.pagina?.content.map((item) => item.id)).toEqual(
        consulta.store.registrosMapa?.map((item) => item.id),
      )
      expect(consulta.store.pagina?.content.length).toBeGreaterThan(0)
    },
  )

  it('pesquisa parcial pelo código institucional sem diferenciar caixa ou espaços externos', async () => {
    simularConsulta()
    const consulta = useInventarioConsulta()
    consulta.criterios.search = '  arv-qxd-00002  '
    await consulta.aplicarFiltros()
    expect(consulta.store.pagina?.content.map((item) => item.codigo)).toEqual([
      'ARV-QXD-000020',
      'ARV-QXD-000021',
      'ARV-QXD-000022',
      'ARV-QXD-000023',
      'ARV-QXD-000024',
      'ARV-QXD-000025',
    ])
    expect(consulta.store.registrosMapa?.map((item) => item.id)).toEqual(
      consulta.store.pagina?.content.map((item) => item.id),
    )
  })

  it('combina pesquisa, bairro, espécie e condição e volta à página zero', async () => {
    const get = simularConsulta()
    const consulta = useInventarioConsulta()
    await consulta.consultar()
    await consulta.mudarPagina(1)
    consulta.idSelecionado.value = 24
    Object.assign(consulta.criterios, {
      search: 'alpha',
      bairro: 'teste a',
      especieId: 7,
      condicao: 'MORTA',
    })
    await consulta.aplicarFiltros()
    expect(get).toHaveBeenLastCalledWith('/api/arvores/mapa', {
      params: { search: 'alpha', bairro: 'teste a', especieId: 7, condicao: 'MORTA' },
      ...feedbackLocalDeErro,
    })
    expect(consulta.store.pagina?.page).toBe(0)
    expect(consulta.store.pagina?.totalElements).toBe(7)
    expect(consulta.store.registrosMapa).toHaveLength(7)
    expect(consulta.idSelecionado.value).toBeNull()
    expect(get.mock.calls.some(([url]) => url === '/api/bairros')).toBe(false)
  })

  it('filtro textual parcial e sem distinção de caixa mantém tabela e mapa sincronizados', async () => {
    simularConsulta()
    const consulta = useInventarioConsulta()
    consulta.criterios.bairro = 'BAIRRO TESTE b'
    await consulta.aplicarFiltros()
    expect(consulta.store.pagina?.content.map((item) => item.id)).toEqual(
      consulta.store.registrosMapa?.map((item) => item.id),
    )
    expect(consulta.store.pagina?.content).toHaveLength(10)
  })

  it('seletor usa nome para tabela e mapa; Todos os bairros remove só o critério territorial', async () => {
    const registrosBoto = [1, 2].map((id) => ({
      ...resumo,
      id,
      bairro: { id: id === 1 ? 4 : 10, nome: id === 1 ? 'Bôto' : 'Centro', ativo: true },
      bairroNome: id === 1 ? 'Bôto' : 'Centro',
    }))
    const get = vi.spyOn(api, 'get').mockImplementation(async (url, config) => {
      const params = config?.params as InventarioListagemParams
      const filtered = registrosBoto.filter(
        (item) =>
          (!params.search || item.especie.nomePopular.toLowerCase().includes(params.search.toLowerCase())) &&
          (!params.bairro || (item.bairroNome ?? '').includes(params.bairro)) &&
          (params.especieId == null || item.especie.id === params.especieId) &&
          (params.condicao == null || item.condicaoFitossanitaria === params.condicao),
      )
      if (url === '/api/arvores/mapa') return resposta(filtered)
      const page = params.page ?? 0
      const size = params.size ?? 20
      return resposta({
        content: filtered.slice(page * size, (page + 1) * size),
        page,
        size,
        totalElements: filtered.length,
        totalPages: Math.ceil(filtered.length / size),
      })
    })
    const consulta = useInventarioConsulta()
    consulta.tamanhoPagina.value = 50
    Object.assign(consulta.criterios, {
      search: 'Espécie de teste',
      bairro: 'Bôto',
      especieId: 7,
      condicao: 'NAO_AVALIADA',
    })

    await consulta.aplicarFiltros()

    expect(get.mock.calls.map(([url, config]) => [url, config?.params])).toEqual([
      [
        '/api/arvores',
        {
          search: 'Espécie de teste',
          bairro: 'Bôto',
          especieId: 7,
          condicao: 'NAO_AVALIADA',
          page: 0,
          size: 50,
        },
      ],
      [
        '/api/arvores/mapa',
        { search: 'Espécie de teste', bairro: 'Bôto', especieId: 7, condicao: 'NAO_AVALIADA' },
      ],
    ])
    expect(consulta.store.pagina?.content.map((item) => item.id)).toEqual([1])
    expect(consulta.store.pagina?.content.map((item) => item.id)).toEqual(
      consulta.store.registrosMapa?.map((item) => item.id),
    )

    get.mockClear()
    consulta.criterios.bairro = ''
    await consulta.aplicarFiltros()

    expect(get.mock.calls.map(([url, config]) => [url, config?.params])).toEqual([
      [
        '/api/arvores',
        {
          search: 'Espécie de teste',
          especieId: 7,
          condicao: 'NAO_AVALIADA',
          page: 0,
          size: 50,
        },
      ],
      ['/api/arvores/mapa', { search: 'Espécie de teste', especieId: 7, condicao: 'NAO_AVALIADA' }],
    ])
    expect(consulta.store.pagina?.content.map((item) => item.id)).toEqual([1, 2])
    expect(consulta.store.pagina?.content.map((item) => item.id)).toEqual(
      consulta.store.registrosMapa?.map((item) => item.id),
    )
  })

  it('paginar solicita somente a tabela e preserva mapa e seleção', async () => {
    const get = simularConsulta()
    const consulta = useInventarioConsulta()
    await consulta.consultar()
    consulta.idSelecionado.value = 24
    get.mockClear()
    await consulta.mudarPagina(1)
    expect(get).toHaveBeenCalledExactlyOnceWith('/api/arvores', {
      params: { page: 1, size: 20 },
      ...feedbackLocalDeErro,
    })
    expect(consulta.store.pagina?.content.map((item) => item.id)).toEqual([21, 22, 23, 24, 25])
    expect(consulta.store.registrosMapa).toHaveLength(25)
    expect(consulta.idSelecionado.value).toBe(24)
    await consulta.mudarPagina(-1)
    await consulta.mudarPagina(2)
    expect(get).toHaveBeenCalledTimes(1)
  })

  it('mudar tamanho volta à página zero sem consultar o mapa outra vez', async () => {
    const get = simularConsulta()
    const consulta = useInventarioConsulta()
    await consulta.consultar()
    get.mockClear()
    consulta.tamanhoPagina.value = 10
    await consulta.mudarTamanho()
    expect(get).toHaveBeenCalledExactlyOnceWith('/api/arvores', {
      params: { page: 0, size: 10 },
      ...feedbackLocalDeErro,
    })
    expect(consulta.store.pagina?.totalPages).toBe(3)
    expect(consulta.store.registrosMapa).toHaveLength(25)
  })

  it('limpar filtros consulta novamente os dois endpoints', async () => {
    simularConsulta()
    const consulta = useInventarioConsulta()
    consulta.criterios.condicao = 'MORTA'
    await consulta.aplicarFiltros()
    await consulta.limparFiltros()
    expect(consulta.store.filtros).toEqual({})
    expect(consulta.criterios.condicao).toBeUndefined()
    expect(consulta.store.registrosMapa).toHaveLength(25)
    expect(consulta.store.pagina?.page).toBe(0)
  })

  it('não interpreta ausência de resultados como erro', async () => {
    simularConsulta()
    const consulta = useInventarioConsulta()
    consulta.criterios.search = 'texto sem correspondência'
    await consulta.aplicarFiltros()
    expect(consulta.store.pagina?.content).toEqual([])
    expect(consulta.store.registrosMapa).toEqual([])
    expect(consulta.store.erros.listagem).toBeNull()
    expect(consulta.filtrosAtivos.value).toBe(true)
  })

  it.each([401, 403])(
    'HTTP %s é acesso indisponível, sem resultados fictícios ou catálogos presumidos',
    async (status) => {
      vi.spyOn(api, 'get').mockRejectedValue(falhaHttp(status))
      const consulta = useInventarioConsulta()
      await consulta.consultar()
      expect(consulta.acessoNegado.value?.status).toBe(status)
      expect(consulta.store.pagina).toBeNull()
      expect(consulta.store.registrosMapa).toBeNull()
      expect(consulta.store.opcoesCarregadas).toBe(false)
      expect(consulta.consultando.value).toBe(false)
    },
  )

  it('falha do mapa não descarta a tabela autorizada', async () => {
    const get = simularConsulta()
    const original = get.getMockImplementation()!
    get.mockImplementation((url, config) =>
      url?.endsWith('/mapa') ? Promise.reject(falhaHttp(500)) : original(url, config),
    )
    const consulta = useInventarioConsulta()
    await consulta.consultar()
    expect(consulta.store.pagina?.content).toHaveLength(20)
    expect(consulta.store.registrosMapa).toBeNull()
    expect(consulta.store.erros.mapa?.status).toBe(500)
  })

  it('impede aplicar filtros e paginar enquanto a consulta estiver em andamento', async () => {
    const get = vi.spyOn(api, 'get')
    const consulta = useInventarioConsulta()
    consulta.store.carregando.mapa = true
    await consulta.consultar()
    await consulta.aplicarFiltros()
    await consulta.limparFiltros()
    await consulta.mudarPagina(1)
    expect(get).not.toHaveBeenCalled()
  })
})
