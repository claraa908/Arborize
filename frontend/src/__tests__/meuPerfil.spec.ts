// @vitest-environment node
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { nextTick } from 'vue'
import { createMemoryHistory } from 'vue-router'
import { AxiosError } from 'axios'
import { createAppRouter } from '@/router'
import { saveAuthSession } from '@/services/authStorage'
import { buscarMeuPerfil, atualizarMeuPerfil } from '@/services/usuarioService'
import { useAuthStore } from '@/stores/authStore'
import { useFeedbackStore } from '@/stores/feedbackStore'
import PerfilView from '@/views/public/PerfilView.vue'
import PerfilInstitucionalView from '@/views/auth/PerfilInstitucionalView.vue'
import { ElementoTeste, encontrar, renderer } from './mapaTestHost'

vi.mock('@/views/public/HomeView.vue', () => ({ default: { render: () => null } }))

vi.mock('vue-router', async (importOriginal) => {
  const original = await importOriginal<typeof import('vue-router')>()
  return { ...original, createWebHistory: original.createMemoryHistory }
})
vi.mock('@/services/usuarioService', () => ({ buscarMeuPerfil: vi.fn(), atualizarMeuPerfil: vi.fn(), alterarMinhaSenha: vi.fn() }))

const user = { id: 42, nome: 'Pessoa Teste', email: 'pessoa@example.invalid', cpf: '12345678901', whatsapp: '88999990000', endereco: 'Endereço de teste' }
const apps: Array<{ unmount(): void }> = []
beforeEach(() => {
  vi.clearAllMocks()
  const values = new Map<string, string>()
  vi.stubGlobal('localStorage', {
    getItem: (key: string) => values.get(key) ?? null,
    setItem: (key: string, value: string) => values.set(key, value),
    removeItem: (key: string) => values.delete(key),
  })
  vi.stubGlobal('document', { activeElement: null })
  vi.stubGlobal('Document', class {})
  vi.stubGlobal('ShadowRoot', class {})
  vi.mocked(buscarMeuPerfil).mockResolvedValue(user)
})
afterEach(() => {
  apps.splice(0).forEach(app => app.unmount())
  vi.unstubAllGlobals()
})

async function mount(institutional = false) {
  saveAuthSession('test-only-token', 'Pessoa Teste', institutional ? 'ROLE_FUNCIONARIO_AMMA' : 'ROLE_CIDADAO')
  const pinia = createPinia()
  setActivePinia(pinia)
  const auth = useAuthStore()
  const feedback = useFeedbackStore()
  const toast = vi.spyOn(feedback, 'showToast')
  const router = createAppRouter(createMemoryHistory())
  await router.push(institutional ? '/sistema/perfil' : '/perfil')
  const root = new ElementoTeste('ROOT')
  const app = renderer.createApp(institutional ? PerfilInstitucionalView : PerfilView).use(pinia).use(router)
  app.mount(root)
  apps.push(app)
  return { root, auth, router, toast }
}
const input = (root: ElementoTeste, id: string) => encontrar(root, no => no.props.id === id)
const flush = async () => { await Promise.resolve(); await nextTick() }

function change(root: ElementoTeste, id: string, value: string) {
  const field = input(root, id)
  field.value = value
  field.dispatch('input')
}
function failure(status: number) {
  return new AxiosError('test-only', undefined, undefined, undefined, { status, data: {}, statusText: '', headers: {}, config: {} as never })
}

describe('formulário real compartilhado de perfil', () => {
  it.each([false, true])('carrega dados da API no perfil institucional=%s e salva somente campos permitidos', async institutional => {
    const { root, auth, toast } = await mount(institutional)
    await flush()
    expect(input(root, 'perfil-nome').value).toBe(user.nome)
    expect(input(root, 'perfil-email').props).toHaveProperty('disabled', '')
    expect(input(root, 'perfil-cpf').props).toHaveProperty('disabled', '')
    expect(root.textContent).not.toContain('Mudas coletadas')
    expect(root.textContent).not.toContain('Doações de mudas e insumos')
    change(root, 'perfil-nome', ' Nome atualizado ')
    change(root, 'perfil-endereco', ' Endereço atualizado ')
    await nextTick()
    vi.mocked(atualizarMeuPerfil).mockResolvedValue({ ...user, nome: 'Nome atualizado', endereco: 'Endereço atualizado' })
    await encontrar(root, no => no.tagName === 'FORM').dispatch('submit')
    await flush()
    const payload = vi.mocked(atualizarMeuPerfil).mock.calls[0]?.[0]
    expect(payload).toEqual({ nome: 'Nome atualizado', endereco: 'Endereço atualizado', whatsapp: '(88) 99999-0000' })
    expect(Object.keys(payload ?? {}).sort()).toEqual(['endereco', 'nome', 'whatsapp'])
    expect(auth.nome).toBe('Nome atualizado')
    expect(toast).toHaveBeenCalledWith('Perfil atualizado com sucesso', 'success')
  })

  it('mostra carregamento e erro com nova tentativa sem apresentar formulário sem dados', async () => {
    let reject!: (reason: unknown) => void
    vi.mocked(buscarMeuPerfil).mockReturnValue(new Promise((_resolve, fail) => { reject = fail }))
    const { root } = await mount()
    expect(root.textContent).toContain('Carregando seus dados')
    expect(() => input(root, 'perfil-nome')).toThrow()
    reject(new Error('network'))
    await flush()
    expect(root.textContent).toContain('Não foi possível carregar seus dados')
    expect(() => input(root, 'perfil-nome')).toThrow()
    vi.mocked(buscarMeuPerfil).mockResolvedValue(user)
    await encontrar(root, no => no.tagName === 'BUTTON' && no.textContent.includes('Tentar novamente')).dispatch('click')
    await flush()
    expect(input(root, 'perfil-nome').value).toBe(user.nome)
  })

  it.each([400, 403, 500])('mantém preenchimento e não mostra sucesso após falha HTTP %s', async status => {
    const { root, toast } = await mount(true)
    await flush()
    change(root, 'perfil-nome', 'Nome preservado')
    await nextTick()
    vi.mocked(atualizarMeuPerfil).mockRejectedValue(failure(status))
    await encontrar(root, no => no.tagName === 'FORM').dispatch('submit')
    await flush()
    expect(input(root, 'perfil-nome').value).toBe('Nome preservado')
    expect(root.textContent).toContain(status === 403 ? 'Você não tem permissão' : 'Não foi possível salvar')
    expect(toast).not.toHaveBeenCalled()
  })

  it('identifica 403 na leitura sem exibir dados pessoais', async () => {
    vi.mocked(buscarMeuPerfil).mockRejectedValue(failure(403))
    const { root } = await mount(true)
    await flush()
    expect(root.textContent).toContain('Você não tem permissão')
    expect(() => input(root, 'perfil-nome')).toThrow()
  })

  it.each([false, true])('expiração pede novo login com destino correto, institucional=%s', async institutional => {
    vi.mocked(buscarMeuPerfil).mockRejectedValue(failure(401))
    const { router, auth } = await mount(institutional)
    await vi.waitFor(() => expect(router.currentRoute.value.name).toBe('home'))
    expect(auth.isAuthenticated).toBe(false)
    expect(router.currentRoute.value.query).toEqual({ auth: 'required', redirect: institutional ? '/sistema/perfil' : '/perfil' })
  })
})
