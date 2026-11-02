// @vitest-environment node
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { inventarioService } from '@/services/inventarioService'
import { useInventarioStore } from '@/stores/inventarioStore'
import type { ArvorePaginaResponseDTO } from '@/types/inventario'
import { arvore, resumo, pagina, cadastro, atualizacao, falhaHttp } from './inventarioFixtures'

vi.mock('@/services/inventarioService', async (importOriginal) => {
  const original = await importOriginal<typeof import('@/services/inventarioService')>()
  return {
    ...original,
    inventarioService: {
      getBairros: vi.fn(),
      getAll: vi.fn(),
      getMapa: vi.fn(),
      getById: vi.fn(),
      create: vi.fn(),
      update: vi.fn(),
    },
  }
})

beforeEach(() => {
  setActivePinia(createPinia())
  vi.resetAllMocks()
})

function semChamadas() {
  for (const method of Object.values(inventarioService)) expect(method).not.toHaveBeenCalled()
}

describe('inventarioStore: camada de dados sem DOM ou HTTP real', () => {
  it('inicia sem dados, erros, carregamento ou requisições automáticas', () => {
    const store = useInventarioStore()
    expect(store.pagina).toBeNull()
    expect(store.registrosMapa).toBeNull()
    expect(store.arvoreSelecionada).toBeNull()
    expect(store.filtros).toEqual({})
    expect(Object.values(store.carregando)).toEqual([false, false, false, false, false])
    expect(Object.values(store.erros)).toEqual([null, null, null, null, null])
    semChamadas()
  })

  it('carrega a página com critérios atuais e controla o estado durante a consulta', async () => {
    const store = useInventarioStore()
    store.setFiltros({ search: 'teste', especieId: 7, condicao: 'MORTA' })
    let concluir!: (value: ArvorePaginaResponseDTO) => void
    vi.mocked(inventarioService.getAll).mockReturnValue(
      new Promise((resolve) => {
        concluir = resolve
      }),
    )
    const operation = store.fetchArvores(2, 10)
    expect(store.carregando.listagem).toBe(true)
    expect(store.pagina).toBeNull()
    expect(inventarioService.getAll).toHaveBeenCalledExactlyOnceWith({
      search: 'teste',
      especieId: 7,
      condicao: 'MORTA',
      page: 2,
      size: 10,
    })
    const result = { ...pagina, page: 2, size: 10 }
    concluir(result)
    expect(await operation).toEqual(result)
    expect(store.pagina).toEqual(result)
    expect(store.carregando.listagem).toBe(false)
    expect(store.erros.listagem).toBeNull()
  })

  it('usa página zero por padrão e mantém tabela e mapa independentes', async () => {
    const store = useInventarioStore()
    store.setFiltros({ bairro: 'Centro', especieId: 7 })
    vi.mocked(inventarioService.getMapa).mockResolvedValue([resumo])
    vi.mocked(inventarioService.getAll).mockResolvedValue(pagina)
    await store.fetchMapa()
    await store.fetchArvores()
    expect(inventarioService.getMapa).toHaveBeenCalledExactlyOnceWith({ bairro: 'Centro', especieId: 7 })
    expect(inventarioService.getAll).toHaveBeenCalledExactlyOnceWith({
      bairro: 'Centro',
      especieId: 7,
      page: 0,
      size: 20,
    })
    expect(store.pagina).toEqual(pagina)
    expect(store.registrosMapa).toEqual([resumo])
  })

  it('consulta e seleciona a ficha completa', async () => {
    const store = useInventarioStore()
    vi.mocked(inventarioService.getById).mockResolvedValue(arvore)
    expect(await store.fetchArvore(42)).toEqual(arvore)
    expect(store.arvoreSelecionada).toEqual(arvore)
    expect(inventarioService.getById).toHaveBeenCalledExactlyOnceWith(42)
    expect(store.carregando.ficha).toBe(false)
  })

  it('mantém somente bairros ativos e ordena as opções alfabeticamente', async () => {
    const nomes = [
      'Alto da Boa Vista', 'Alto São Francisco', 'Baviera', 'Bôto', 'Campo Novo',
      'Campo Velho', 'Carrascal I', 'Carrascal II', 'Cedro', 'Centro', 'Cohab',
      'Combate', 'Curicaca', 'Herval', 'Irajá', 'Jardim dos Monólitos', 'Monte Alegre',
      'Nova Jerusalém', 'Planalto Renascer', 'Planalto Universitário', 'Putiú',
      'Residencial', 'São João', 'Triângulo',
    ]
    vi.mocked(inventarioService.getBairros).mockResolvedValue([
      ...nomes.map((nome, index) => ({ id: index + 1, nome, ativo: true })),
      { id: 99, nome: 'Inativo', ativo: false },
    ])
    const store = useInventarioStore()

    const bairros = await store.fetchBairros()

    expect(bairros).toHaveLength(24)
    expect(bairros.some((bairro) => bairro.nome === 'Bôto')).toBe(true)
    expect(bairros.map((bairro) => bairro.nome)).toEqual(
      [...nomes].sort((a, b) => a.localeCompare(b, 'pt-BR')),
    )
    expect(bairros.every((bairro) => bairro.ativo)).toBe(true)
  })

  it('distingue resultado vazio confirmado de dados não consultados', async () => {
    const store = useInventarioStore()
    const vazia = { content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 }
    vi.mocked(inventarioService.getAll).mockResolvedValue(vazia)
    vi.mocked(inventarioService.getMapa).mockResolvedValue([])
    await store.fetchArvores()
    await store.fetchMapa()
    expect(store.pagina).toEqual(vazia)
    expect(store.registrosMapa).toEqual([])
    expect(store.erros.listagem).toBeNull()
    expect(store.erros.mapa).toBeNull()
  })

  it('aplicar/limpar filtros invalida resultados e seleção sem consultar a API', () => {
    const store = useInventarioStore()
    store.pagina = pagina
    store.registrosMapa = [resumo]
    store.arvoreSelecionada = arvore
    const filtros = { bairro: 'Centro', search: 'teste' }
    store.setFiltros(filtros)
    filtros.search = 'alterado fora da store'
    expect(store.filtros).toEqual({ bairro: 'Centro', search: 'teste' })
    expect(store.pagina).toBeNull()
    expect(store.registrosMapa).toBeNull()
    expect(store.arvoreSelecionada).toBeNull()
    store.limparFiltros()
    expect(store.filtros).toEqual({})
    semChamadas()
  })

  it('cadastra e utiliza apenas a resposta confirmada, sem recarregamento automático', async () => {
    const store = useInventarioStore()
    store.pagina = pagina
    store.registrosMapa = [resumo]
    vi.mocked(inventarioService.create).mockResolvedValue(arvore)
    expect(await store.createArvore(cadastro)).toEqual(arvore)
    expect(store.arvoreSelecionada).toEqual(arvore)
    expect(store.pagina).toBeNull()
    expect(store.registrosMapa).toBeNull()
    expect(inventarioService.create).toHaveBeenCalledExactlyOnceWith(cadastro)
    expect(inventarioService.getAll).not.toHaveBeenCalled()
    expect(inventarioService.getMapa).not.toHaveBeenCalled()
    expect(store.carregando.salvamento).toBe(false)
  })

  it('atualiza com payload completo e usa os dados retornados pelo servidor', async () => {
    const store = useInventarioStore()
    store.arvoreSelecionada = arvore
    store.pagina = pagina
    store.registrosMapa = [resumo]
    const updated = { ...arvore, condicaoFitossanitaria: 'MORTA' as const }
    vi.mocked(inventarioService.update).mockResolvedValue(updated)
    expect(await store.updateArvore(42, atualizacao)).toEqual(updated)
    expect(store.arvoreSelecionada).toEqual(updated)
    expect(store.pagina).toBeNull()
    expect(store.registrosMapa).toBeNull()
    expect(inventarioService.update).toHaveBeenCalledExactlyOnceWith(42, atualizacao)
    expect(inventarioService.getAll).not.toHaveBeenCalled()
  })

  for (const status of [401, 403]) {
    it.each([
      ['listagem', (store: ReturnType<typeof useInventarioStore>) => store.fetchArvores()],
      ['mapa', (store: ReturnType<typeof useInventarioStore>) => store.fetchMapa()],
      ['ficha', (store: ReturnType<typeof useInventarioStore>) => store.fetchArvore(42)],
      [
        'salvamento',
        (store: ReturnType<typeof useInventarioStore>) => store.createArvore(cadastro),
      ],
      [
        'salvamento',
        (store: ReturnType<typeof useInventarioStore>) => store.updateArvore(42, atualizacao),
      ],
    ] as const)(
      `%s rejeita ${status} como acesso negado, sem sucesso ou lista vazia`,
      async (operacao, action) => {
        const store = useInventarioStore()
        for (const method of Object.values(inventarioService))
          vi.mocked(method).mockRejectedValue(falhaHttp(status))
        await expect(action(store)).rejects.toMatchObject({ status, acessoNegado: true })
        expect(store.erros[operacao]).toMatchObject({ status, acessoNegado: true })
        expect(store.erros[operacao]).not.toHaveProperty('cause')
        expect(store.erros[operacao]).not.toHaveProperty('config')
        expect(store.carregando[operacao]).toBe(false)
        expect(store.pagina).toBeNull()
        expect(store.registrosMapa).toBeNull()
        expect(store.arvoreSelecionada).toBeNull()
      },
    )
  }

  it('404 na ficha limpa a seleção anterior e mantém erro explícito', async () => {
    const store = useInventarioStore()
    store.arvoreSelecionada = arvore
    vi.mocked(inventarioService.getById).mockRejectedValue(falhaHttp(404))
    await expect(store.fetchArvore(999)).rejects.toMatchObject({ status: 404 })
    expect(store.arvoreSelecionada).toBeNull()
    expect(store.erros.ficha).toMatchObject({ status: 404, acessoNegado: false })
  })

  it('falha ao atualizar preserva os dados anteriores e os dados preenchidos pelo chamador', async () => {
    const store = useInventarioStore()
    store.arvoreSelecionada = arvore
    store.pagina = pagina
    const payload = structuredClone(atualizacao)
    const before = structuredClone(payload)
    vi.mocked(inventarioService.update).mockRejectedValue(
      falhaHttp(400, {
        type: 'urn:arborize:inventario:entrada-invalida',
        status: 400,
        title: 'Requisição não concluída',
        detail: 'Confira os campos informados.',
        code: 'entrada-invalida',
        errors: [{ field: 'dapCm', message: 'O DAP deve ser positivo.' }],
      }),
    )
    await expect(store.updateArvore(42, payload)).rejects.toMatchObject({ status: 400 })
    expect(payload).toEqual(before)
    expect(store.arvoreSelecionada).toEqual(arvore)
    expect(store.pagina).toEqual(pagina)
    expect(store.erros.salvamento?.problem?.errors).toEqual([
      { field: 'dapCm', message: 'O DAP deve ser positivo.' },
    ])
  })

  it('falha do mapa não apaga a tabela carregada, e vice-versa', async () => {
    const store = useInventarioStore()
    store.pagina = pagina
    vi.mocked(inventarioService.getMapa).mockRejectedValue(falhaHttp(500))
    await expect(store.fetchMapa()).rejects.toMatchObject({ status: 500 })
    expect(store.pagina).toEqual(pagina)
    expect(store.registrosMapa).toBeNull()
    expect(store.erros.listagem).toBeNull()
    store.registrosMapa = [resumo]
    vi.mocked(inventarioService.getAll).mockRejectedValue(falhaHttp(500))
    await expect(store.fetchArvores()).rejects.toMatchObject({ status: 500 })
    expect(store.pagina).toBeNull()
    expect(store.registrosMapa).toEqual([resumo])
  })

  it('limpa o erro numa nova tentativa e libera carregamento após falha de rede', async () => {
    const store = useInventarioStore()
    vi.mocked(inventarioService.getAll).mockRejectedValue(new Error('falha'))
    await expect(store.fetchArvores()).rejects.toMatchObject({ status: null })
    expect(store.erros.listagem?.status).toBeNull()
    expect(store.carregando.listagem).toBe(false)
    vi.mocked(inventarioService.getAll).mockResolvedValue(pagina)
    const pending = store.fetchArvores()
    expect(store.erros.listagem).toBeNull()
    expect(store.carregando.listagem).toBe(true)
    await pending
    expect(store.carregando.listagem).toBe(false)
    expect(store.pagina).toEqual(pagina)
  })
})
