import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createMemoryHistory, createRouter } from 'vue-router'
import ArborizacaoInicioView from '@/views/public/arborizacao/ArborizacaoInicioView.vue'
import SolicitarMudaView from '@/views/public/arborizacao/SolicitarMudaView.vue'
import { buscarMeuPerfil } from '@/services/usuarioService'
import { solicitacaoMudaService } from '@/services/solicitacaoMudaService'
import { useAssistentePlantioStore } from '@/stores/assistentePlantioStore'
import { useSolicitacaoMudaStore } from '@/stores/solicitacaoMudaStore'

vi.mock('@/services/usuarioService', () => ({ buscarMeuPerfil: vi.fn() }))
vi.mock('@/services/solicitacaoMudaService', () => ({
  solicitacaoMudaService: {
    calcularConversao: vi.fn(),
  },
}))

function createTestRouter() {
  return createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/arborizacao', name: 'arborizacao', component: ArborizacaoInicioView },
      { path: '/arborizacao/assistente', name: 'arborizacao-assistente', component: { template: '<div />' } },
      { path: '/arborizacao/recomendacoes', name: 'arborizacao-recomendacoes', component: { template: '<div />' } },
      { path: '/arborizacao/substituicao-nim', name: 'arborizacao-substituicao-nim', component: { template: '<div />' } },
      { path: '/arborizacao/substituicao-nim/acompanhar', name: 'arborizacao-substituicao-acompanhar', component: { template: '<div />' } },
      { path: '/arborizacao/solicitar-muda', name: 'solicitar-muda', component: SolicitarMudaView },
      { path: '/arborizacao/conversao', name: 'arborizacao-conversao', component: { template: '<div />' } },
      { path: '/arborizacao/acompanhar', name: 'arborizacao-acompanhar', component: { template: '<div />' } },
      { path: '/perfil', name: 'perfil', component: { template: '<div />' } },
    ],
  })
}

describe('ArborizacaoInicioView e SolicitarMudaView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    setActivePinia(createPinia())
  })

  it('exibe os dois cards de Arborização com imagens de capa e links corretos', async () => {
    const pinia = createPinia()
    setActivePinia(pinia)
    const router = createTestRouter()
    await router.push('/arborizacao')
    await router.isReady()

    const wrapper = mount(ArborizacaoInicioView, {
      global: { plugins: [pinia, router] },
    })

    const images = wrapper.findAll('img')
    expect(images).toHaveLength(2)
    expect(wrapper.text()).toContain('Retirada de Nim Indiano')
    expect(wrapper.text()).toContain('Assistente de plantio')
    expect(wrapper.find('a[href="/arborizacao/substituicao-nim"]').exists()).toBe(true)
    expect(wrapper.find('a[href="/arborizacao/assistente"]').exists()).toBe(true)
    wrapper.unmount()
  })

  it('remove a seção de Identificação, preenche dados do perfil logado e avança para conversão', async () => {
    vi.mocked(buscarMeuPerfil).mockResolvedValue({
      id: 7,
      nome: 'Maria Silva',
      cpf: '12345678901',
      email: 'maria@example.invalid',
      whatsapp: '(88) 99999-1234',
      endereco: 'Rua Central, 100',
    })
    vi.mocked(solicitacaoMudaService.calcularConversao).mockResolvedValue({
      quantidade: 2,
      garrafasPorMuda: 5,
      totalGarrafas: 10,
    })

    const pinia = createPinia()
    setActivePinia(pinia)
    const quizStore = useAssistentePlantioStore()
    quizStore.selecionarEspecie({
      id: 15,
      nomePopular: 'Ipê-amarelo',
      nomeCientifico: 'Handroanthus albus',
      porte: 'MEDIO',
    })
    quizStore.setResposta('quantidade_mudas', 'DUAS')

    const router = createTestRouter()
    await router.push('/arborizacao/solicitar-muda?especieId=15')
    await router.isReady()

    const wrapper = mount(SolicitarMudaView, {
      global: { plugins: [pinia, router] },
    })
    await flushPromises()

    expect(wrapper.find('#identity-title').exists()).toBe(false)
    expect(wrapper.find('#nomeSolicitante').exists()).toBe(false)
    expect(wrapper.find('#cpf').exists()).toBe(false)
    expect(wrapper.find('#telefone').exists()).toBe(false)

    await wrapper.get('#logradouro').setValue('Rua José de Queiroz Pessoa')
    await wrapper.get('#numeroEndereco').setValue('412')
    await wrapper.get('#bairro').setValue('Centro')
    await wrapper.get('#pontoReferencia').setValue('Em frente à praça')
    await wrapper.get('form').trigger('submit')
    await flushPromises()

    const mudaStore = useSolicitacaoMudaStore()
    expect(mudaStore.form.nomeSolicitante).toBe('Maria Silva')
    expect(mudaStore.form.cpf).toBe('12345678901')
    expect(mudaStore.form.telefone).toBe('(88) 99999-1234')
    expect(solicitacaoMudaService.calcularConversao).toHaveBeenCalledWith(2)
    expect(router.currentRoute.value.name).toBe('arborizacao-conversao')
    wrapper.unmount()
  })
})
