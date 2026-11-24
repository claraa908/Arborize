import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createMemoryHistory } from 'vue-router'
import EventoCard from '@/components/eventos/EventoCard.vue'
import TopBar from '@/components/layout/TopBar.vue'
import { createAppRouter } from '@/router'
import { eventoService } from '@/services/eventoService'
import EventosPublicoView from '@/views/public/eventos/EventosPublicoView.vue'
import EventoPublicoDetalhesView from '@/views/public/eventos/EventoPublicoDetalhesView.vue'
import type { EventoDTO } from '@/types/eventos'

const eventosMock: EventoDTO[] = [
  {
    id: 1,
    nome: 'Mutirão Encerrado no Lago',
    descricao: 'Ação concluída com plantio de mudas nativas.',
    dataInicio: '2026-03-10',
    dataTermino: '2026-03-10',
    localConcentracao: 'Lago dos Monólitos',
    destinoFinal: 'Margem Sul',
    horarioInicio: '07:30',
    horarioTermino: '10:30',
    status: 'ENCERRADO',
    documentos: [{ id: 10, contentType: 'image/jpeg' }],
  },
  {
    id: 2,
    nome: 'Plantio Confirmado na Praça',
    descricao: 'Mutirão aberto ao público para arborização urbana.',
    dataInicio: '2026-06-15',
    dataTermino: '2026-06-16',
    localConcentracao: 'Praça José de Barros',
    destinoFinal: 'Avenida Plácido Castelo',
    horarioInicio: '08:00',
    horarioTermino: '11:30',
    descricaoPontoPartida: 'Em frente ao coreto central.',
    descricaoEncerramento: 'Distribuição de mudas no ponto final.',
    status: 'CONFIRMADO',
    documentos: [{ id: 11, contentType: 'image/png' }],
  },
  {
    id: 3,
    nome: 'Evento Planejado Interno',
    descricao: 'Ainda em definição pela equipe.',
    dataInicio: '2026-08-01',
    dataTermino: '2026-08-01',
    localConcentracao: 'Sede AMMA',
    horarioInicio: '09:00',
    horarioTermino: '12:00',
    status: 'PLANEJADO',
  },
  {
    id: 4,
    nome: 'Evento Cancelado por Chuva',
    descricao: 'Cancelado devido às condições climáticas.',
    dataInicio: '2026-05-05',
    dataTermino: '2026-05-05',
    localConcentracao: 'Parque Municipal',
    horarioInicio: '08:00',
    horarioTermino: '10:00',
    status: 'CANCELADO',
  },
]

beforeEach(() => {
  vi.restoreAllMocks()
  setActivePinia(createPinia())
})

describe('EventoCard', () => {
  it('renderiza informações, trata fallback de banner no erro e emite select por clique e teclado', async () => {
    const evento = eventosMock[1]!
    const wrapper = mount(EventoCard, {
      props: {
        evento,
        bannerUrl: '/eventos/2/banner',
      },
    })

    expect(wrapper.text()).toContain('Confirmado')
    expect(wrapper.text()).toContain('Plantio Confirmado na Praça')
    expect(wrapper.text()).toContain('15/06/2026 a 16/06/2026')
    expect(wrapper.text()).toContain('Praça José de Barros')
    expect(wrapper.find('img').attributes('src')).toBe('/eventos/2/banner')

    await wrapper.find('img').trigger('error')
    expect(wrapper.find('[data-testid="evento-card-fallback"]').exists()).toBe(true)

    await wrapper.trigger('click')
    await wrapper.trigger('keydown.enter')
    await wrapper.trigger('keydown.space')

    expect(wrapper.emitted('select')).toEqual([[2], [2], [2]])
    wrapper.unmount()
  })
})

describe('EventosPublicoView (/eventos)', () => {
  it('exibe apenas CONFIRMADO e ENCERRADO ordenados, aplica filtros/busca e navega para detalhes', async () => {
    vi.spyOn(eventoService, 'getAll').mockResolvedValue(eventosMock)
    const pinia = createPinia()
    setActivePinia(pinia)
    const router = createAppRouter(createMemoryHistory())
    await router.push('/eventos')
    await router.isReady()

    const wrapper = mount(EventosPublicoView, {
      global: { plugins: [pinia, router] },
    })

    await flushPromises()

    const cards = wrapper.findAllComponents(EventoCard)
    expect(cards).toHaveLength(2)
    expect(cards[0]!.props('evento').id).toBe(2)
    expect(cards[1]!.props('evento').id).toBe(1)
    expect(wrapper.text()).not.toContain('Evento Planejado Interno')
    expect(wrapper.text()).not.toContain('Evento Cancelado por Chuva')

    const btnEncerrados = wrapper.findAll('button').find((b) => b.text().includes('Encerrados'))
    await btnEncerrados!.trigger('click')
    expect(wrapper.findAllComponents(EventoCard)).toHaveLength(1)
    expect(wrapper.text()).toContain('Mutirão Encerrado no Lago')

    const btnTodos = wrapper.findAll('button').find((b) => b.text().includes('Todos'))
    await btnTodos!.trigger('click')
    await wrapper.get('#eventos-publico-busca').setValue('plácido')
    expect(wrapper.findAllComponents(EventoCard)).toHaveLength(1)
    expect(wrapper.text()).toContain('Plantio Confirmado na Praça')

    await wrapper.get('#eventos-publico-busca').setValue('termo-inexistente')
    expect(wrapper.text()).toContain('Nenhum evento encontrado')
    await wrapper.findAll('button').find((b) => b.text().includes('Limpar Filtros'))!.trigger('click')
    expect(wrapper.findAllComponents(EventoCard)).toHaveLength(2)

    await wrapper.findAllComponents(EventoCard)[0]!.trigger('click')
    await flushPromises()
    expect(router.currentRoute.value.name).toBe('eventos-publico-detalhes')
    expect(router.currentRoute.value.params.id).toBe('2')

    wrapper.unmount()
  })

  it('exibe estado vazio quando não há eventos públicos publicados', async () => {
    vi.spyOn(eventoService, 'getAll').mockResolvedValue([eventosMock[2]!, eventosMock[3]!])
    const pinia = createPinia()
    setActivePinia(pinia)
    const router = createAppRouter(createMemoryHistory())
    await router.push('/eventos')
    await router.isReady()

    const wrapper = mount(EventosPublicoView, {
      global: { plugins: [pinia, router] },
    })
    await flushPromises()

    expect(wrapper.text()).toContain('Nenhum evento disponível no momento')
    wrapper.unmount()
  })

  it('exibe estado de erro sem mascarar como vazio e permite tentar novamente', async () => {
    const getAll = vi
      .spyOn(eventoService, 'getAll')
      .mockRejectedValueOnce(new Error('offline'))
      .mockResolvedValueOnce([eventosMock[1]!])

    const pinia = createPinia()
    setActivePinia(pinia)
    const router = createAppRouter(createMemoryHistory())
    await router.push('/eventos')
    await router.isReady()

    const wrapper = mount(EventosPublicoView, {
      global: { plugins: [pinia, router] },
    })
    await flushPromises()

    expect(wrapper.text()).toContain('Erro ao carregar eventos')
    expect(wrapper.text()).not.toContain('Nenhum evento disponível no momento')

    await wrapper.findAll('button').find((b) => b.text().includes('Tentar novamente'))!.trigger('click')
    await flushPromises()

    expect(getAll).toHaveBeenCalledTimes(2)
    expect(wrapper.findAllComponents(EventoCard)).toHaveLength(1)
    wrapper.unmount()
  })
})

describe('EventoPublicoDetalhesView (/eventos/:id)', () => {
  it('exibe os detalhes completos de um evento confirmado e navega de volta pelo breadcrumb', async () => {
    vi.spyOn(eventoService, 'getById').mockResolvedValue(eventosMock[1]!)
    const pinia = createPinia()
    setActivePinia(pinia)
    const router = createAppRouter(createMemoryHistory())
    await router.push('/eventos/2')
    await router.isReady()

    const wrapper = mount(EventoPublicoDetalhesView, {
      global: { plugins: [pinia, router] },
    })
    await flushPromises()

    expect(wrapper.text()).toContain('Plantio Confirmado na Praça')
    expect(wrapper.text()).toContain('Confirmado')
    expect(wrapper.text()).toContain('Praça José de Barros')
    expect(wrapper.text()).toContain('Avenida Plácido Castelo')
    expect(wrapper.text()).toContain('Em frente ao coreto central.')
    expect(wrapper.text()).toContain('Distribuição de mudas no ponto final.')

    await wrapper.findAll('button').find((b) => b.text().includes('Eventos Ambientais'))!.trigger('click')
    await flushPromises()
    expect(router.currentRoute.value.name).toBe('eventos-publico')

    wrapper.unmount()
  })

  it('exibe aviso explícito ao acessar diretamente um evento CANCELADO', async () => {
    vi.spyOn(eventoService, 'getById').mockResolvedValue(eventosMock[3]!)
    const pinia = createPinia()
    setActivePinia(pinia)
    const router = createAppRouter(createMemoryHistory())
    await router.push('/eventos/4')
    await router.isReady()

    const wrapper = mount(EventoPublicoDetalhesView, {
      global: { plugins: [pinia, router] },
    })
    await flushPromises()

    expect(wrapper.text()).toContain('Cancelado')
    expect(wrapper.text()).toContain('Este evento foi cancelado pela AMMA')
    wrapper.unmount()
  })

  it('exibe estado de não encontrado para evento PLANEJADO ou resposta 404', async () => {
    const getById = vi
      .spyOn(eventoService, 'getById')
      .mockResolvedValueOnce(eventosMock[2]!)
      .mockRejectedValueOnce({ response: { status: 404 } })

    const pinia = createPinia()
    setActivePinia(pinia)
    const router = createAppRouter(createMemoryHistory())
    await router.push('/eventos/3')
    await router.isReady()

    const wrapper = mount(EventoPublicoDetalhesView, {
      global: { plugins: [pinia, router] },
    })
    await flushPromises()
    expect(wrapper.text()).toContain('Evento não encontrado')

    await router.push('/eventos/999')
    await flushPromises()
    expect(getById).toHaveBeenCalledTimes(2)
    expect(wrapper.text()).toContain('Evento não encontrado')
    wrapper.unmount()
  })

  it('exibe erro amigável com tentar novamente em falha de rede', async () => {
    const getById = vi
      .spyOn(eventoService, 'getById')
      .mockRejectedValueOnce(new Error('network'))
      .mockResolvedValueOnce(eventosMock[1]!)

    const pinia = createPinia()
    setActivePinia(pinia)
    const router = createAppRouter(createMemoryHistory())
    await router.push('/eventos/2')
    await router.isReady()

    const wrapper = mount(EventoPublicoDetalhesView, {
      global: { plugins: [pinia, router] },
    })
    await flushPromises()
    expect(wrapper.text()).toContain('Erro ao carregar detalhes')

    await wrapper.findAll('button').find((b) => b.text().includes('Tentar novamente'))!.trigger('click')
    await flushPromises()
    expect(getById).toHaveBeenCalledTimes(2)
    expect(wrapper.text()).toContain('Plantio Confirmado na Praça')
    wrapper.unmount()
  })
})

describe('TopBar e rotas públicas de eventos', () => {
  it('navega para /eventos ao clicar em Eventos e mantém o item ativo em /eventos/:id', async () => {
    const pinia = createPinia()
    setActivePinia(pinia)
    const router = createAppRouter(createMemoryHistory())
    await router.push('/')
    await router.isReady()

    const wrapper = mount(TopBar, {
      global: { plugins: [pinia, router] },
    })

    const btnEventos = wrapper.findAll('nav button').find((b) => b.text().trim() === 'Eventos')
    expect(btnEventos).toBeDefined()
    await btnEventos!.trigger('click')
    await flushPromises()

    expect(router.currentRoute.value.name).toBe('eventos-publico')
    expect(btnEventos!.classes()).toContain('bg-amma-green-light')

    await router.push('/eventos/2')
    await flushPromises()
    expect(router.currentRoute.value.name).toBe('eventos-publico-detalhes')
    expect(btnEventos!.classes()).toContain('bg-amma-green-light')

    wrapper.unmount()
  })
})
