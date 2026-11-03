// @vitest-environment node
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { createSSRApp, nextTick } from 'vue'
import { renderToString } from 'vue/server-renderer'
import { createMemoryHistory } from 'vue-router'
import { createAppRouter } from '@/router'
import { postLoginTarget, sessionNavigation } from '@/router/sessionNavigation'
import { USER_ROLES, type UserRole } from '@/types/auth'
import { useAuthStore } from '@/stores/authStore'
import { useFeedbackStore } from '@/stores/feedbackStore'
import { saveAuthSession } from '@/services/authStorage'
import { autenticarUsuario } from '@/services/authService'
import TopBar from '@/components/layout/TopBar.vue'
import AppHeader from '@/components/layout/AppHeader.vue'
import CidadaoDashboardView from '@/views/public/CidadaoDashboardView.vue'
import { ElementoTeste, encontrar, renderer } from './mapaTestHost'

vi.mock('@/views/public/HomeView.vue', () => ({ default: { render: () => null } }))

vi.mock('vue-router', async (importOriginal) => {
  const original = await importOriginal<typeof import('vue-router')>()
  return { ...original, createWebHistory: original.createMemoryHistory }
})
vi.mock('@/services/authService', () => ({ autenticarUsuario: vi.fn() }))

const values = new Map<string, string>()
beforeEach(() => {
  values.clear()
  vi.clearAllMocks()
  vi.stubGlobal('localStorage', {
    getItem: (key: string) => values.get(key) ?? null,
    setItem: (key: string, value: string) => values.set(key, value),
    removeItem: (key: string) => values.delete(key),
  })
  setActivePinia(createPinia())
})
afterEach(() => vi.unstubAllGlobals())

async function session(role?: UserRole, path = '/') {
  if (role) saveAuthSession('test-only-token', 'Pessoa Teste', role)
  const pinia = createPinia()
  setActivePinia(pinia)
  const auth = useAuthStore()
  const router = createAppRouter(createMemoryHistory())
  await router.push(path)
  await router.isReady()
  return { pinia, auth, router }
}

describe('navegação real por sessão', () => {
  it('preserva as páginas públicas do visitante e pede login para rotas protegidas', async () => {
    const { auth, router, pinia } = await session(undefined, '/biblioteca')
    expect(auth.isAuthenticated).toBe(false)
    expect(router.currentRoute.value.name).toBe('biblioteca-catalogo')
    const html = await renderToString(createSSRApp(TopBar).use(pinia).use(router))
    expect(html).toContain('Cadastrar-se')
    expect(html).toContain('Entrar')
    expect(html).not.toContain('Voltar ao painel')
    for (const path of ['/dashboard', '/perfil', '/sistema', '/sistema/perfil', '/gestao', '/ifce/equipe']) {
      await router.push(path)
      expect(router.currentRoute.value.name).toBe('home')
      expect(router.currentRoute.value.query).toEqual({ auth: 'required', redirect: path })
    }
  })

  it.each(USER_ROLES)('direciona o login de %s, restaura sessão e mantém restrições específicas', async (role) => {
    const { auth, router } = await session()
    vi.mocked(autenticarUsuario).mockResolvedValue({ token: 'test-only-token', nome: 'Pessoa Teste', role, tipo: 'Bearer' })
    await auth.login({ login: 'pessoa@example.invalid', senha: 'test-only' })
    await router.push(postLoginTarget(router, auth, undefined))
    expect(router.currentRoute.value.name).toBe(role === 'ROLE_CIDADAO' ? 'cidadao-dashboard' : 'dashboard')
    expect(router.currentRoute.value.matched[0]?.path).toBe(role === 'ROLE_CIDADAO' ? '/' : '/sistema')
    if (role !== 'ROLE_CIDADAO') {
      for (const path of ['/sistema/inventario', '/sistema/viveiro', '/sistema/biblioteca', '/sistema/solicitacoes', '/sistema/laudos', '/sistema/eventos']) {
        const target = router.resolve(path)
        expect(target.matched.length).toBeGreaterThan(0)
        expect(sessionNavigation(target, auth)).toBe(true)
      }
    }
    const restored = useAuthStore(createPinia())
    expect(restored.isAuthenticated).toBe(true)
    expect(restored.role).toBe(role)
    expect(restored.dashboardRouteName).toBe(auth.dashboardRouteName)
    await router.push('/gestao')
    expect(router.currentRoute.value.name).toBe(role === 'ROLE_ADMINISTRADOR_AMMA' ? 'gestao' : 'home')
    await router.push('/ifce/equipe')
    expect(router.currentRoute.value.name).toBe(role === 'ROLE_GESTOR_IFCE' ? 'ifce-equipe' : 'home')
  })

  it('impede URLs administrativas para cidadão e mantém o perfil público na URL direta', async () => {
    const { router } = await session('ROLE_CIDADAO', '/perfil')
    expect(router.currentRoute.value.name).toBe('perfil')
    for (const path of ['/sistema', '/sistema/inventario', '/sistema/viveiro', '/sistema/biblioteca']) {
      await router.push(path)
      expect(router.currentRoute.value.name).toBe('cidadao-dashboard')
    }
    await router.push('/sistema/perfil')
    expect(router.currentRoute.value.name).toBe('perfil')
  })

  it('mantém institucional no layout administrativo do perfil e evita dashboard do cidadão', async () => {
    const { router } = await session('ROLE_FUNCIONARIO_AMMA', '/perfil')
    expect(router.currentRoute.value.name).toBe('perfil-institucional')
    expect(router.currentRoute.value.matched[0]?.path).toBe('/sistema')
    await router.push('/dashboard')
    expect(router.currentRoute.value.name).toBe('dashboard')
  })

  it('aceita somente destinos existentes e permitidos depois do login', async () => {
    const { router, auth } = await session('ROLE_CIDADAO')
    expect(postLoginTarget(router, auth, '/perfil')).toBe('/perfil')
    for (const target of ['/sistema', '/gestao', '//external.invalid', 'https://external.invalid', '/missing', '/\\external.invalid']) {
      expect(postLoginTarget(router, auth, target)).toEqual({ name: 'cidadao-dashboard' })
    }
  })

  it('não reconhece como institucional uma sessão incompleta ou papel desconhecido', async () => {
    values.set('arborize.auth.role', 'ROLE_ADMINISTRADOR_AMMA')
    const incomplete = useAuthStore(createPinia())
    expect(incomplete.isAmmaAdmin).toBe(false)
    expect(incomplete.canAccessManagement).toBe(false)
    values.set('arborize.auth.token', 'test-only-token')
    values.set('arborize.auth.role', 'ROLE_UNKNOWN')
    const unknown = useAuthStore(createPinia())
    expect(unknown.isAuthenticated).toBe(false)
    expect(unknown.canAccessManagement).toBe(false)
  })
})

describe('componentes reais de navegação', () => {
  it.each(USER_ROLES)('apresenta controles públicos coerentes para %s', async (role) => {
    const { router, pinia } = await session(role, '/biblioteca')
    const html = await renderToString(createSSRApp(TopBar).use(pinia).use(router))
    expect(html).not.toContain('Cadastrar-se')
    expect(html).not.toContain('Entrar')
    expect(html).toContain('Sair')
    expect(html).toContain('Biblioteca Digital')
    if (role === 'ROLE_CIDADAO') {
      expect(html).toContain('aria-label="Dashboard"')
      expect(html).toContain('Meu perfil')
      expect(html).not.toContain('Voltar ao painel')
    } else {
      expect(html).toContain('Voltar ao painel')
      expect(html).not.toContain('Meu perfil')
      expect(html).not.toContain('aria-label="Dashboard"')
    }
  })

  it('dashboard do cidadão usa o nome da sessão e links reais, sem indicadores fictícios', async () => {
    const { router, pinia } = await session('ROLE_CIDADAO', '/dashboard')
    const html = await renderToString(createSSRApp(CidadaoDashboardView).use(pinia).use(router))
    expect(html).toContain('Pessoa Teste')
    expect(html).toContain('href="/perfil"')
    expect(html).toContain('href="/arborizacao"')
    expect(html).toContain('href="/biblioteca"')
    expect(html).not.toContain('Mudas coletadas')
    expect(html).not.toContain('Solicitações de substituição')
  })

  it.each(['ROLE_CIDADAO', 'ROLE_ADMINISTRADOR_AMMA'] as const)('logout pela navbar limpa e encerra a sessão %s', async (role) => {
    const { router, pinia, auth } = await session(role, '/biblioteca')
    const root = new ElementoTeste('ROOT')
    const app = renderer.createApp(TopBar).use(pinia).use(router)
    app.mount(root)
    encontrar(root, no => no.props['aria-label'] === 'Sair').dispatch('click')
    await vi.waitFor(() => expect(router.currentRoute.value.name).toBe('home'))
    await nextTick()
    expect(auth.isAuthenticated).toBe(false)
    expect(values.size).toBe(0)
    expect(root.textContent).toContain('Cadastrar-se')
    app.unmount()
  })

  it('cabeçalho institucional visita o site, volta ao dashboard e abre perfil próprio', async () => {
    const { router, pinia, auth } = await session('ROLE_ADMINISTRADOR_AMMA', '/sistema')
    const root = new ElementoTeste('ROOT')
    const app = renderer.createApp(AppHeader).use(pinia).use(router)
    app.mount(root)
    const publicLink = encontrar(root, no => no.props['aria-label'] === 'Visitar página pública')
    expect(publicLink.props.href).toBe('/')
    encontrar(root, no => no.props['aria-label'] === 'Meu perfil').dispatch('click')
    await vi.waitFor(() => expect(router.currentRoute.value.name).toBe('perfil-institucional'))
    await router.push('/')
    const topRoot = new ElementoTeste('ROOT')
    const top = renderer.createApp(TopBar).use(pinia).use(router)
    top.mount(topRoot)
    encontrar(topRoot, no => no.props['aria-label'] === 'Voltar ao painel').dispatch('click')
    await vi.waitFor(() => expect(router.currentRoute.value.name).toBe('dashboard'))
    expect(auth.isAuthenticated).toBe(true)
    top.unmount()
    vi.spyOn(useFeedbackStore(pinia), 'showConfirm').mockResolvedValue(true)
    encontrar(root, no => no.props.title === 'Sair').dispatch('click')
    await vi.waitFor(() => expect(router.currentRoute.value.name).toBe('home'))
    expect(auth.isAuthenticated).toBe(false)
    app.unmount()
  })
})
