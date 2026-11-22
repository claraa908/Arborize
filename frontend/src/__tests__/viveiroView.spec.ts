import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { flushPromises, mount } from '@vue/test-utils'
import { nextTick } from 'vue'
import { createMemoryHistory, createRouter } from 'vue-router'
import ViveiroView from '@/views/auth/viveiro/ViveiroView.vue'
import { especieService } from '@/services/especieService'
import { movimentacaoMudaService } from '@/services/movimentacaoMudaService'
import { useFeedbackStore } from '@/stores/feedbackStore'
import { useMovimentacaoMudaStore } from '@/stores/movimentacaoMudaStore'
import { useViveiroStore } from '@/stores/viveiroStore'
import type { EspecieDTO } from '@/types/viveiro'

function especie(id: number, nomePopular: string, mudasDisponiveis: number): EspecieDTO {
  return {
    id,
    nomePopular,
    nomeCientifico: `Nome científico ${id}`,
    categoria: 'Nativa da Caatinga',
    porteArvore: 'Médio',
    recomendadaCalcadas: true,
    mudasDisponiveis,
  }
}

function montarPainel() {
  const pinia = createPinia()
  setActivePinia(pinia)
  const especieStore = useViveiroStore()
  const movimentoStore = useMovimentacaoMudaStore()
  const buscarEspecies = vi.spyOn(especieStore, 'fetchEspecies').mockResolvedValue()
  const buscarLista = vi.spyOn(movimentoStore, 'listar').mockResolvedValue()
  const buscarResumo = vi.spyOn(movimentoStore, 'buscarResumo').mockResolvedValue()
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/', name: 'viveiro', component: { template: '<div />' } },
      { path: '/nova', name: 'viveiro-cadastrar', component: { template: '<div />' } },
      { path: '/:id', name: 'viveiro-detalhes', component: { template: '<div />' } },
      { path: '/:id/editar', name: 'viveiro-editar', component: { template: '<div />' } },
    ],
  })
  const wrapper = mount(ViveiroView, { global: { plugins: [pinia, router] } })

  return { wrapper, especieStore, movimentoStore, buscarEspecies, buscarLista, buscarResumo, router }
}

beforeEach(() => {
  vi.restoreAllMocks()
  setActivePinia(createPinia())
})

afterEach(() => vi.restoreAllMocks())

describe('painel do Viveiro', () => {
  it('soma o estoque, mostra indicadores mensais e preserva a tabela de espécies', async () => {
    const { wrapper, especieStore, movimentoStore, buscarEspecies } = montarPainel()
    especieStore.especies = [especie(1, 'Ipê-amarelo', 1200), especie(2, 'Aroeira', 34)]
    movimentoStore.resumo = { saidasNoMes: 12, perdasNoMes: 3 }
    await nextTick()

    const cards = wrapper.findAll('section[aria-label="Resumo do estoque"] article')
    expect(buscarEspecies).toHaveBeenCalledOnce()
    expect(cards).toHaveLength(4)
    expect(cards.map(card => card.text())).toEqual([
      expect.stringContaining('1.234'),
      expect.stringContaining('—'),
      expect.stringContaining('3'),
      expect.stringContaining('12'),
    ])
    expect(wrapper.find('table').exists()).toBe(true)
    expect(wrapper.text()).toContain('Ipê-amarelo')
    expect(wrapper.text()).toContain('1200 un')
    expect(wrapper.get('section[aria-label="Resumo do estoque"]').classes()).toEqual(
      expect.arrayContaining(['grid-cols-1', 'sm:grid-cols-2', '2xl:grid-cols-4']),
    )
  })

  it('mantém as duas abas e abre o formulário pelo botão de movimentação', async () => {
    const { wrapper, router } = montarPainel()
    const tabs = wrapper.findAll('[role="tab"]')
    expect(tabs.map(tab => tab.text())).toEqual(['Espécies', 'Movimentações'])
    expect(tabs[0]?.attributes('aria-selected')).toBe('true')

    await wrapper.get('#aba-movimentacoes').trigger('click')
    await flushPromises()
    expect(router.currentRoute.value.query.aba).toBe('movimentacoes')
    expect(wrapper.find('#historico-movimentacoes-titulo').exists()).toBe(true)

    await wrapper.get('#historico-movimentacoes-titulo').element.scrollIntoView?.()
    const botao = wrapper.findAll('button').find(button => button.text().includes('Registrar movimentação'))
    expect(botao).toBeDefined()
    await botao?.trigger('click')
    await flushPromises()
    expect(router.currentRoute.value.query.movimentacao).toBe('nova')
    expect(wrapper.text()).toContain('Registrar movimentação')
    expect(wrapper.find('#movimentacao-especie').exists()).toBe(true)
  })

  it('mostra o estado vazio de espécies quando não há registros', () => {
    const { wrapper } = montarPainel()
    expect(wrapper.findAll('section[aria-label="Resumo do estoque"] article')[0]?.text()).toContain('0')
    expect(wrapper.text()).toContain('Nenhuma espécie botânica encontrada.')
  })

  it('mostra skeletons dos indicadores e da lista durante o carregamento', () => {
    const pinia = createPinia()
    setActivePinia(pinia)
    const especieStore = useViveiroStore()
    especieStore.isLoading = true
    vi.spyOn(especieStore, 'fetchEspecies').mockResolvedValue()
    vi.spyOn(useMovimentacaoMudaStore(), 'listar').mockResolvedValue()
    vi.spyOn(useMovimentacaoMudaStore(), 'buscarResumo').mockResolvedValue()
    const router = createRouter({ history: createMemoryHistory(), routes: [{ path: '/', name: 'viveiro', component: { template: '<div />' } }] })

    const wrapper = mount(ViveiroView, { global: { plugins: [pinia, router] } })

    expect(wrapper.get('section[aria-label="Resumo do estoque"]').attributes('aria-busy')).toBe('true')
    expect(wrapper.findAll('[aria-label^="Carregando indicador:"]')).toHaveLength(1)
    expect(wrapper.find('[aria-label="Carregando espécies do Viveiro"]').exists()).toBe(true)
    expect(wrapper.find('table').exists()).toBe(false)
  })

  it('notifica quando o catálogo falha ao carregar', async () => {
    const pinia = createPinia()
    setActivePinia(pinia)
    const feedback = useFeedbackStore()
    const showToast = vi.spyOn(feedback, 'showToast')
    vi.spyOn(especieService, 'getAll').mockRejectedValue(new Error('Falha HTTP'))
    vi.spyOn(useMovimentacaoMudaStore(), 'listar').mockResolvedValue()
    vi.spyOn(useMovimentacaoMudaStore(), 'buscarResumo').mockResolvedValue()
    const router = createRouter({ history: createMemoryHistory(), routes: [{ path: '/', name: 'viveiro', component: { template: '<div />' } }] })

    const wrapper = mount(ViveiroView, { global: { plugins: [pinia, router] } })
    await flushPromises()

    expect(showToast).toHaveBeenCalledWith('Erro ao carregar catálogo de espécies do viveiro.', 'error')
    expect(wrapper.text()).toContain('Erro ao carregar o catálogo de espécies do viveiro.')
    expect(wrapper.text()).toContain('Tentar novamente')
  })

  it('registra, filtra, edita e atualiza estoque sem recarregar a página', async () => {
    const pinia = createPinia()
    setActivePinia(pinia)
    const inicial = especie(7, 'Ipê-amarelo', 10)
    const aposEntrada = especie(7, 'Ipê-amarelo', 13)
    const aposEdicao = especie(7, 'Ipê-amarelo', 12)
    const getEspecies = vi.spyOn(especieService, 'getAll')
      .mockResolvedValueOnce([inicial])
      .mockResolvedValueOnce([aposEntrada])
      .mockResolvedValueOnce([aposEdicao])
    const movimento = {
      id: 31,
      tipo: 'ENTRADA_GERMINACAO' as const,
      especieId: 7,
      especieNomePopular: 'Ipê-amarelo',
      especieNomeCientifico: 'Handroanthus albus',
      quantidade: 3,
      data: '2026-11-22',
      destino: null,
      responsavel: null,
      observacoes: null,
      comprovanteNome: null,
    }
    const movimentoEditado = { ...movimento, quantidade: 2 }
    const paginaVazia = { content: [], number: 0, totalElements: 0, totalPages: 0 }
    const paginaComMovimento = { content: [movimento], number: 0, totalElements: 1, totalPages: 1 }
    const listar = vi.spyOn(movimentacaoMudaService, 'listar')
      .mockResolvedValueOnce(paginaVazia)
      .mockResolvedValueOnce(paginaComMovimento)
      .mockResolvedValueOnce(paginaComMovimento)
      .mockResolvedValueOnce({ ...paginaComMovimento, content: [movimentoEditado] })
    const resumo = vi.spyOn(movimentacaoMudaService, 'resumo')
      .mockResolvedValue({ saidasNoMes: 0, perdasNoMes: 0 })
    const criar = vi.spyOn(movimentacaoMudaService, 'criar').mockResolvedValue(movimento)
    const buscar = vi.spyOn(movimentacaoMudaService, 'buscar').mockResolvedValue(movimento)
    const atualizar = vi.spyOn(movimentacaoMudaService, 'atualizar').mockResolvedValue(movimentoEditado)
    const router = createRouter({
      history: createMemoryHistory(),
      routes: [
        { path: '/', name: 'viveiro', component: { template: '<div />' } },
        { path: '/nova', name: 'viveiro-cadastrar', component: { template: '<div />' } },
        { path: '/:id', name: 'viveiro-detalhes', component: { template: '<div />' } },
        { path: '/:id/editar', name: 'viveiro-editar', component: { template: '<div />' } },
      ],
    })
    const wrapper = mount(ViveiroView, { global: { plugins: [pinia, router] } })
    await flushPromises()

    await wrapper.get('#aba-movimentacoes').trigger('click')
    await flushPromises()
    await wrapper.get('section[aria-labelledby="historico-movimentacoes-titulo"] > div > button').trigger('click')
    await flushPromises()
    await wrapper.get('#movimentacao-especie').setValue('7')
    await wrapper.get('#movimentacao-quantidade').setValue('3')
    await wrapper.get('form').trigger('submit')
    await flushPromises()

    expect(criar).toHaveBeenCalledWith(expect.objectContaining({ especieId: 7, quantidade: 3 }))
    expect(getEspecies).toHaveBeenCalledTimes(2)
    expect(listar).toHaveBeenCalledTimes(2)
    expect(resumo).toHaveBeenCalledTimes(2)
    expect(router.currentRoute.value.query).toMatchObject({ aba: 'movimentacoes' })
    expect(wrapper.find('section[aria-label="Resumo do estoque"] article').text()).toContain('13')

    await wrapper.get('input[type="search"]').setValue('mutirão')
    await wrapper.get('section[aria-labelledby="historico-movimentacoes-titulo"] form').trigger('submit')
    await flushPromises()
    expect(listar).toHaveBeenLastCalledWith(expect.objectContaining({ busca: 'mutirão' }), 0)

    await wrapper.get('[aria-label="Editar movimentação de Ipê-amarelo"]').trigger('click')
    await flushPromises()
    expect(router.currentRoute.value.query.movimentacao).toBe('31')
    expect(buscar).toHaveBeenCalledWith(31)
    await wrapper.get('#movimentacao-quantidade').setValue('2')
    await wrapper.get('form').trigger('submit')
    await flushPromises()

    expect(atualizar).toHaveBeenCalledWith(31, expect.objectContaining({ quantidade: 2 }))
    expect(getEspecies).toHaveBeenCalledTimes(3)
    expect(listar).toHaveBeenCalledTimes(4)
    expect(resumo).toHaveBeenCalledTimes(3)
    expect(wrapper.find('section[aria-label="Resumo do estoque"] article').text()).toContain('12')
    expect(router.currentRoute.value.query).toMatchObject({ aba: 'movimentacoes' })
    expect(router.currentRoute.value.query.movimentacao).toBeUndefined()
    wrapper.unmount()
  })

  it('mantém o formulário aberto e mostra o problema da API quando o estoque não permite salvar', async () => {
    const { wrapper, especieStore, router } = montarPainel()
    especieStore.especies = [especie(7, 'Ipê-amarelo', 2)]
    vi.spyOn(movimentacaoMudaService, 'criar').mockRejectedValue({
      response: { data: { detail: 'A quantidade informada excede o estoque disponível para esta espécie.' } },
    })

    await wrapper.get('#aba-movimentacoes').trigger('click')
    await flushPromises()
    await wrapper.get('section[aria-labelledby="historico-movimentacoes-titulo"] > div > button').trigger('click')
    await flushPromises()
    await wrapper.findAll('button').find((button) => button.text().includes('Saída para plantio'))?.trigger('click')
    await wrapper.get('#movimentacao-especie').setValue('7')
    await wrapper.get('#movimentacao-quantidade').setValue('5')
    await wrapper.get('#movimentacao-destino').setValue('Praça de teste')
    await wrapper.get('form').trigger('submit')
    await flushPromises()

    expect(wrapper.get('[role="alert"]').text()).toContain('excede o estoque disponível')
    expect(router.currentRoute.value.query.movimentacao).toBe('nova')
    expect(especieStore.especies[0]?.mudasDisponiveis).toBe(2)
    wrapper.unmount()
  })

  it.each([
    [401, 'Sua sessão expirou ou não é válida. Saia da conta e entre novamente'],
    [403, 'Seu perfil não tem permissão para registrar movimentações.'],
  ])('diferencia sessão inválida de perfil sem permissão (HTTP %i)', async (status, mensagem) => {
    const { wrapper, especieStore } = montarPainel()
    especieStore.especies = [especie(7, 'Ipê-amarelo', 10)]
    vi.spyOn(movimentacaoMudaService, 'criar').mockRejectedValue({
      isAxiosError: true,
      response: { status, data: {} },
    })

    await wrapper.get('#aba-movimentacoes').trigger('click')
    await flushPromises()
    await wrapper.get('section[aria-labelledby="historico-movimentacoes-titulo"] > div > button').trigger('click')
    await flushPromises()
    await wrapper.get('#movimentacao-especie').setValue('7')
    await wrapper.get('#movimentacao-quantidade').setValue('1')
    await wrapper.get('form').trigger('submit')
    await flushPromises()

    expect(wrapper.get('[role="alert"]').text()).toContain(mensagem)
    wrapper.unmount()
  })
})
