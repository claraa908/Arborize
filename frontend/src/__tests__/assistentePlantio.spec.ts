import { beforeEach, describe, expect, it, vi } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { defineComponent, h } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AssistentePlantioView from '@/views/public/arborizacao/AssistentePlantioView.vue'
import RecomendacoesView from '@/views/public/arborizacao/RecomendacoesView.vue'
import { assistentePlantioQuestions } from '@/data/assistentePlantioQuestions'
import { buscarRecomendacoes } from '@/services/recomendacaoService'
import { useAssistentePlantioStore } from '@/stores/assistentePlantioStore'
import type { SpeciesRecommendation } from '@/types/assistentePlantio'

vi.mock('@/services/recomendacaoService', () => ({ buscarRecomendacoes: vi.fn() }))
vi.mock('vue-router', () => ({ useRoute: vi.fn(), useRouter: vi.fn() }))

const especie: SpeciesRecommendation = {
  id: 15,
  nomePopular: 'Espécie de teste',
  nomeCientifico: 'Testus arbor',
  porte: 'PEQUENO',
}

const SpeciesCardStub = defineComponent({
  props: { species: { type: Object, required: true } },
  emits: ['select'],
  setup(props, { emit }) {
    return () =>
      h(
        'button',
        { type: 'button', onClick: () => emit('select', props.species) },
        (props.species as SpeciesRecommendation).nomePopular,
      )
  },
})

let pinia: ReturnType<typeof createPinia>
const routerPush = vi.fn()
const routeQuery: Record<string, string> = {}

function mountWithStore(component: typeof AssistentePlantioView | typeof RecomendacoesView) {
  return mount(component, {
    global: {
      plugins: [pinia],
      stubs: { EspecieRecomendadaCard: SpeciesCardStub },
    },
  })
}

beforeEach(() => {
  vi.resetAllMocks()
  pinia = createPinia()
  setActivePinia(pinia)
  vi.mocked(useRouter).mockReturnValue({ push: routerPush } as never)
  vi.mocked(useRoute).mockReturnValue({ query: routeQuery } as never)
  Object.keys(routeQuery).forEach((key) => delete routeQuery[key])
})

describe('assistente de plantio', () => {
  it('valida a resposta antes de avançar e preserva a resposta ao voltar', async () => {
    const store = useAssistentePlantioStore()
    const wrapper = mountWithStore(AssistentePlantioView)

    await wrapper.findAll('button').at(-1)!.trigger('click')
    expect(wrapper.get('[role="alert"]').exists()).toBe(true)
    expect(store.currentIndex).toBe(0)

    await wrapper.get('input[type="radio"]').setValue()
    await wrapper.findAll('button').at(-1)!.trigger('click')
    expect(store.currentIndex).toBe(1)
    expect(store.respostas.largura_calcada).toBe(assistentePlantioQuestions[0].options[0].value)

    await wrapper.get('button').trigger('click')
    expect(store.currentIndex).toBe(0)
    expect(store.respostas.largura_calcada).toBe(assistentePlantioQuestions[0].options[0].value)
    wrapper.unmount()
  })

  it('mostra o aviso quando o usuário retorna à etapa para escolher uma espécie', async () => {
    routeQuery.aviso = 'especie-obrigatoria'
    const wrapper = mountWithStore(AssistentePlantioView)

    await flushPromises()

    expect(wrapper.get('[role="status"]').text().toLowerCase()).toContain('escolha uma esp')
    wrapper.unmount()
  })

  it('envia as dez respostas ao concluir o questionário e abre as recomendações', async () => {
    const store = useAssistentePlantioStore()
    store.irParaPergunta(assistentePlantioQuestions.length - 1)
    store.setResposta('quantidade_mudas', 'UMA')
    vi.mocked(buscarRecomendacoes).mockResolvedValue([especie])
    const wrapper = mountWithStore(AssistentePlantioView)

    await wrapper.findAll('button').at(-1)!.trigger('click')
    await flushPromises()

    const sentAnswers = vi.mocked(buscarRecomendacoes).mock.calls[0][0]
    expect(sentAnswers).toHaveLength(10)
    expect(sentAnswers.at(-1)).toEqual({ perguntaId: 'quantidade_mudas', valor: 'UMA' })
    expect(store.recomendacoes).toEqual([especie])
    expect(routerPush).toHaveBeenCalledExactlyOnceWith({ name: 'arborizacao-recomendacoes' })
    wrapper.unmount()
  })

  it('mantém as respostas e apresenta estado de erro quando o serviço falha', async () => {
    const store = useAssistentePlantioStore()
    store.setResposta('largura_calcada', 'MAIOR_2_4M')
    vi.mocked(buscarRecomendacoes).mockRejectedValue(new Error('indisponível'))

    await store.carregarRecomendacoes()

    expect(store.respostas.largura_calcada).toBe('MAIOR_2_4M')
    expect(store.recomendacoes).toBeNull()
    expect(store.errorMessage).toContain('poss')
    expect(store.isLoading).toBe(false)
  })

  it('ignora uma segunda consulta enquanto a primeira ainda está carregando', async () => {
    const store = useAssistentePlantioStore()
    let concluir!: (value: SpeciesRecommendation[]) => void
    vi.mocked(buscarRecomendacoes).mockReturnValue(
      new Promise((resolve) => {
        concluir = resolve
      }),
    )

    const primeiraConsulta = store.carregarRecomendacoes()
    await store.carregarRecomendacoes()

    expect(store.isLoading).toBe(true)
    expect(buscarRecomendacoes).toHaveBeenCalledOnce()
    concluir([especie])
    await primeiraConsulta
    expect(store.isLoading).toBe(false)
    expect(store.recomendacoes).toEqual([especie])
  })

  it('reinicia o questionário e seleciona a espécie para a solicitação', async () => {
    const store = useAssistentePlantioStore()
    store.setResposta('largura_calcada', 'MAIOR_2_4M')
    store.selecionarEspecie(especie)
    store.irParaPergunta(4)
    store.refazerQuiz()

    expect(store.respostas).toEqual({})
    expect(store.currentIndex).toBe(0)
    expect(store.especieSelecionada).toBeNull()
    expect(store.errorMessage).toBeNull()

    store.recomendacoes = [especie]
    const wrapper = mountWithStore(RecomendacoesView)
    await wrapper.get('button').trigger('click')

    expect(store.especieSelecionada).toEqual(especie)
    expect(routerPush).toHaveBeenCalledExactlyOnceWith({
      name: 'solicitar-muda',
      query: { especieId: especie.id },
    })
    wrapper.unmount()
  })
})
