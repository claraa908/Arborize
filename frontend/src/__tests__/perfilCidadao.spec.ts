import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createMemoryHistory } from 'vue-router'
import { createAppRouter } from '@/router'
import { api } from '@/services/api'
import * as usuarioService from '@/services/usuarioService'
import MeuPerfil from '@/components/profile/MeuPerfil.vue'
import ArborizacaoInicioView from '@/views/public/arborizacao/ArborizacaoInicioView.vue'
import { useAuthStore } from '@/stores/authStore'
import type {
  CidadaoAcompanhamentosResponse,
  UsuarioResponse,
} from '@/services/usuarioService'

const usuarioMock: UsuarioResponse = {
  id: 10,
  nome: 'Maria Silva',
  cpf: '12345678901',
  email: 'maria@example.invalid',
  whatsapp: '88999998888',
  endereco: 'Rua das Acácias, 45, Centro',
}

const acompanhamentosMock: CidadaoAcompanhamentosResponse = {
  solicitacoesMuda: [
    {
      id: 15,
      protocolo: 'SOL-2026-0015',
      especieNome: 'Ipê Amarelo',
      quantidadeMudas: 2,
      totalRecicaveis: 10,
      quantidadeRecebida: 10,
      status: 'APROVADA',
      motivoRejeicao: null,
      bairro: 'Centro',
      criadaEm: '2026-11-20T10:00:00',
    },
  ],
  substituicoesBotanicas: [
    {
      id: 30,
      protocolo: 'SUB-2026-0001',
      status: 'EM_ANALISE',
      bairro: 'Campo Novo',
      logradouro: 'Rua das Flores',
      numeroEndereco: '120',
      pontoReferencia: 'Ao lado da praça',
      descricaoArvore: 'Nim Indiano na calçada',
      criadaEm: '2026-11-22T14:30:00',
      historico: [
        {
          status: 'RECEBIDA',
          descricao: 'Solicitação recebida pelo portal.',
          criadoEm: '2026-11-22T14:30:00',
        },
        {
          status: 'EM_ANALISE',
          descricao: 'Equipe técnica iniciou a análise do pedido.',
          criadoEm: '2026-11-23T09:15:00',
        },
      ],
    },
  ],
}

beforeEach(() => {
  vi.restoreAllMocks()
  setActivePinia(createPinia())
})

describe('usuarioService.buscarMeusAcompanhamentos', () => {
  it('consulta GET /usuarios/me/acompanhamentos e retorna dados tipados', async () => {
    const getSpy = vi.spyOn(api, 'get').mockResolvedValue({ data: acompanhamentosMock })
    const result = await usuarioService.buscarMeusAcompanhamentos()
    expect(getSpy).toHaveBeenCalledExactlyOnceWith('/usuarios/me/acompanhamentos')
    expect(result).toEqual(acompanhamentosMock)
  })
})

describe('MeuPerfil (layout 2 colunas + acompanhamentos reais)', () => {
  it('abre em Meus Acompanhamentos, filtra por tipo e alterna para Dados Pessoais e Segurança', async () => {
    vi.spyOn(usuarioService, 'buscarMeuPerfil').mockResolvedValue(usuarioMock)
    vi.spyOn(usuarioService, 'buscarMeusAcompanhamentos').mockResolvedValue(acompanhamentosMock)
    const updateSpy = vi
      .spyOn(usuarioService, 'atualizarMeuPerfil')
      .mockResolvedValue({ ...usuarioMock, nome: 'Maria Oliveira' })

    const pinia = createPinia()
    setActivePinia(pinia)
    const router = createAppRouter(createMemoryHistory())
    await router.push('/')
    await router.isReady()

    const wrapper = mount(MeuPerfil, {
      global: { plugins: [pinia, router] },
    })
    await flushPromises()

    expect(wrapper.text()).toContain('Maria Silva')
    expect(wrapper.text()).toContain('123.456.789-01')
    expect(wrapper.text()).toContain('SOL-2026-0015')
    expect(wrapper.text()).toContain('Ipê Amarelo')
    expect(wrapper.text()).toContain('SUB-2026-0001')

    // Ao expandir um segundo card, o primeiro se retrai automaticamente
    const btnMudaCard = wrapper.findAll('article button').find((b) => b.text().includes('SOL-2026-0015'))
    const btnSubCard = wrapper.findAll('article button').find((b) => b.text().includes('SUB-2026-0001'))
    await btnMudaCard!.trigger('click')
    expect(btnMudaCard!.attributes('aria-expanded')).toBe('true')
    await btnSubCard!.trigger('click')
    expect(btnMudaCard!.attributes('aria-expanded')).toBe('false')
    expect(btnSubCard!.attributes('aria-expanded')).toBe('true')
    await btnSubCard!.trigger('click')
    expect(btnSubCard!.attributes('aria-expanded')).toBe('false')

    const btnSubstituicoes = wrapper
      .findAll('button')
      .find((b) => b.text().includes('Substituição de Nim'))
    await btnSubstituicoes!.trigger('click')
    expect(wrapper.text()).toContain('SUB-2026-0001')
    expect(wrapper.text()).not.toContain('SOL-2026-0015')

    const cardHeader = wrapper.findAll('article button').find((b) => b.text().includes('SUB-2026-0001'))
    expect(cardHeader).toBeDefined()
    expect(wrapper.text()).not.toContain('Equipe técnica iniciou a análise do pedido.')
    await cardHeader!.trigger('click')
    expect(wrapper.text()).toContain('Etapas do processo')
    expect(wrapper.text()).toContain('Equipe técnica iniciou a análise do pedido.')
    expect(wrapper.text()).toContain('próxima etapa')

    const btnDados = wrapper.findAll('button').find((b) => b.text().includes('Dados Pessoais'))
    await btnDados!.trigger('click')
    await wrapper.get('#perfil-nome').setValue('Maria Oliveira')
    await wrapper.get('form').trigger('submit')
    await flushPromises()

    expect(updateSpy).toHaveBeenCalledOnce()
    expect(wrapper.text()).toContain('Maria Oliveira')

    const btnSeguranca = wrapper.findAll('button').find((b) => b.text().includes('Segurança e Senha'))
    await btnSeguranca!.trigger('click')
    expect(wrapper.text()).toContain('Alterar senha de acesso')

    wrapper.unmount()
  })

  it('exibe estado vazio limpo quando cidadão não tem acompanhamentos (sem links de atalho)', async () => {
    vi.spyOn(usuarioService, 'buscarMeuPerfil').mockResolvedValue(usuarioMock)
    vi.spyOn(usuarioService, 'buscarMeusAcompanhamentos').mockResolvedValue({
      solicitacoesMuda: [],
      substituicoesBotanicas: [],
    })

    const pinia = createPinia()
    setActivePinia(pinia)
    const router = createAppRouter(createMemoryHistory())
    await router.push('/')
    await router.isReady()

    const wrapper = mount(MeuPerfil, {
      global: { plugins: [pinia, router] },
    })
    await flushPromises()

    expect(wrapper.text()).toContain('Você ainda não possui solicitações')
    expect(wrapper.findAll('a')).toHaveLength(0)
    wrapper.unmount()
  })

  it('isola erro nos acompanhamentos com retry sem bloquear acesso a Dados Pessoais', async () => {
    vi.spyOn(usuarioService, 'buscarMeuPerfil').mockResolvedValue(usuarioMock)
    const acompSpy = vi
      .spyOn(usuarioService, 'buscarMeusAcompanhamentos')
      .mockRejectedValueOnce(new Error('offline'))
      .mockResolvedValueOnce(acompanhamentosMock)

    const pinia = createPinia()
    setActivePinia(pinia)
    const router = createAppRouter(createMemoryHistory())
    await router.push('/')
    await router.isReady()

    const wrapper = mount(MeuPerfil, {
      global: { plugins: [pinia, router] },
    })
    await flushPromises()

    expect(wrapper.text()).toContain('Erro ao carregar acompanhamentos')
    expect(wrapper.text()).toContain('Maria Silva')

    const btnRetry = wrapper.findAll('button').find((b) => b.text().includes('Tentar novamente'))
    await btnRetry!.trigger('click')
    await flushPromises()

    expect(acompSpy).toHaveBeenCalledTimes(2)
    expect(wrapper.text()).toContain('SOL-2026-0015')
    wrapper.unmount()
  })

  it('pré-seleciona filtro e expande protocolo quando aberto via query de acompanhamento', async () => {
    vi.spyOn(usuarioService, 'buscarMeuPerfil').mockResolvedValue(usuarioMock)
    vi.spyOn(usuarioService, 'buscarMeusAcompanhamentos').mockResolvedValue(acompanhamentosMock)

    const pinia = createPinia()
    setActivePinia(pinia)
    const router = createAppRouter(createMemoryHistory())
    await router.push({ path: '/', query: { filtro: 'SUBSTITUICOES', protocolo: 'SUB-2026-0001' } })
    await router.isReady()

    const wrapper = mount(MeuPerfil, {
      global: { plugins: [pinia, router] },
    })
    await flushPromises()

    expect(wrapper.text()).toContain('SUB-2026-0001')
    expect(wrapper.text()).not.toContain('SOL-2026-0015')
    expect(wrapper.text()).toContain('Etapas do processo')
    expect(wrapper.text()).toContain('Equipe técnica iniciou a análise do pedido.')
    wrapper.unmount()
  })

  it('redireciona links de acompanhamento da tela Arborização para /perfil com o filtro certo para cidadãos', async () => {
    const pinia = createPinia()
    setActivePinia(pinia)
    const auth = useAuthStore(pinia)
    auth.token = 'test-token'
    auth.role = 'ROLE_CIDADAO'
    auth.nome = 'Maria Silva'

    const router = createAppRouter(createMemoryHistory())
    await router.push('/arborizacao')
    await router.isReady()

    const wrapper = mount(ArborizacaoInicioView, {
      global: { plugins: [pinia, router] },
    })

    const linkRequerimento = wrapper.findAll('a').find((a) => a.text().includes('Acompanhar requerimento'))
    const linkMuda = wrapper.findAll('a').find((a) => a.text().includes('Acompanhar pedido de muda'))

    expect(linkRequerimento?.attributes('href')).toBe('/perfil?filtro=SUBSTITUICOES')
    expect(linkMuda?.attributes('href')).toBe('/perfil?filtro=MUDAS')
    wrapper.unmount()
  })
})
