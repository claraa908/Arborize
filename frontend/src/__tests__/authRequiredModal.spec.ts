import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { flushPromises, mount } from '@vue/test-utils'
import { createMemoryHistory } from 'vue-router'
import { createAppRouter } from '@/router'
import PublicLayout from '@/layouts/PublicLayout.vue'
import { cadastrarUsuario } from '@/services/usuarioService'
import { autenticarUsuario } from '@/services/authService'

vi.mock('@/components/layout/TopBar.vue', () => ({ default: { template: '<header />' } }))
vi.mock('@/components/feedback/AppToastContainer.vue', () => ({ default: { template: '<div />' } }))
vi.mock('@/views/public/HomeView.vue', () => ({ default: { render: () => null } }))
vi.mock('@/views/public/arborizacao/RecomendacoesView.vue', () => ({ default: { render: () => null } }))
vi.mock('@/services/usuarioService', () => ({ cadastrarUsuario: vi.fn() }))
vi.mock('@/services/authService', () => ({ autenticarUsuario: vi.fn() }))

let wrapper: ReturnType<typeof mount> | undefined
const values = new Map<string, string>()

beforeEach(() => {
  vi.clearAllMocks()
  values.clear()
  vi.stubGlobal('localStorage', {
    getItem: (key: string) => values.get(key) ?? null,
    setItem: (key: string, value: string) => values.set(key, value),
    removeItem: (key: string) => values.delete(key),
  })
  setActivePinia(createPinia())
})

afterEach(() => {
  wrapper?.unmount()
  wrapper = undefined
  vi.unstubAllGlobals()
})

describe('autenticação exigida para Arborização', () => {
  it('exibe aviso contextual, realiza auto-login após cadastro e retoma a solicitação da muda escolhida', async () => {
    vi.mocked(cadastrarUsuario).mockResolvedValue({
      id: 1,
      nome: 'Pessoa Teste',
      cpf: '12345678901',
      email: 'pessoa@example.invalid',
      whatsapp: '88999999999',
      endereco: 'Rua de Teste, 10',
    })
    vi.mocked(autenticarUsuario).mockResolvedValue({
      token: 'test-only-token',
      nome: 'Pessoa Teste',
      role: 'ROLE_CIDADAO',
      tipo: 'Bearer',
    })
    const pinia = createPinia()
    setActivePinia(pinia)
    const router = createAppRouter(createMemoryHistory())
    await router.push('/arborizacao/recomendacoes')
    await router.isReady()

    const requestedPath = '/arborizacao/solicitar-muda?especieId=15'
    await router.push(requestedPath)
    expect(router.currentRoute.value.name).toBe('arborizacao-recomendacoes')
    expect(router.currentRoute.value.query.redirect).toBe(requestedPath)
    expect(router.currentRoute.value.query.motivo).toBe('solicitar-muda')

    wrapper = mount(PublicLayout, {
      global: {
        plugins: [pinia, router],
        stubs: { RouterView: true },
      },
    })

    expect(wrapper.text()).toContain('Para solicitar a muda selecionada')

    await wrapper.findAll('button').find((button) => button.text().trim() === 'Cadastre-se')!.trigger('click')
    expect(wrapper.text()).toContain('Criar Conta')
    expect(wrapper.text()).toContain('Para solicitar a muda selecionada')
    expect(router.currentRoute.value.query.redirect).toBe(requestedPath)

    await wrapper.get('#cadastro-nome').setValue('Pessoa Teste')
    await wrapper.get('#cadastro-cpf').setValue('12345678901')
    await wrapper.get('#cadastro-email').setValue('pessoa@example.invalid')
    await wrapper.get('#cadastro-whatsapp').setValue('88999999999')
    await wrapper.get('#cadastro-endereco').setValue('Rua de Teste, 10')
    await wrapper.get('#cadastro-senha').setValue('senha-segura')
    await wrapper.get('#cadastro-confirmar-senha').setValue('senha-segura')
    await wrapper.get('form').trigger('submit')
    await flushPromises()

    expect(cadastrarUsuario).toHaveBeenCalledOnce()
    expect(autenticarUsuario).toHaveBeenCalledWith({
      login: 'pessoa@example.invalid',
      senha: 'senha-segura',
    })

    await vi.waitFor(() => expect(router.currentRoute.value.name).toBe('solicitar-muda'))
    expect(router.currentRoute.value.query).toEqual({ especieId: '15' })
  })

  it('fecha os modais sem autenticar e remove a intenção pendente preservando a tela pública atual', async () => {
    const pinia = createPinia()
    setActivePinia(pinia)
    const router = createAppRouter(createMemoryHistory())
    await router.push('/arborizacao/recomendacoes')
    await router.push('/arborizacao/solicitar-muda?especieId=15')
    await router.isReady()

    wrapper = mount(PublicLayout, {
      global: {
        plugins: [pinia, router],
        stubs: { RouterView: true },
      },
    })

    await wrapper.get('[aria-label="Fechar login"]').trigger('click')
    await vi.waitFor(() => expect(router.currentRoute.value.query.auth).toBeUndefined())
    expect(router.currentRoute.value.name).toBe('arborizacao-recomendacoes')
    expect(router.currentRoute.value.query.redirect).toBeUndefined()
    expect(router.currentRoute.value.query.motivo).toBeUndefined()

    await router.push('/arborizacao/solicitar-muda')
    await wrapper.findAll('button').find((button) => button.text().trim() === 'Cadastre-se')!.trigger('click')
    expect(wrapper.text()).toContain('Criar Conta')
    await wrapper.get('[aria-label="Fechar cadastro"]').trigger('click')
    await vi.waitFor(() => expect(router.currentRoute.value.query.auth).toBeUndefined())
    expect(router.currentRoute.value.name).toBe('arborizacao-recomendacoes')
    expect(router.currentRoute.value.query.redirect).toBeUndefined()
  })
})
