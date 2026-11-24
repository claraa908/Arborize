
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { createSSRApp, nextTick } from 'vue'
import { renderToString } from 'vue/server-renderer'
import { mount } from '@vue/test-utils'
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
import AppSidebar from '@/components/layout/AppSidebar.vue'
import ArborizacaoInicioView from '@/views/public/arborizacao/ArborizacaoInicioView.vue'
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
    await router.push('/')
    expect(router.currentRoute.value.name).toBe('home')
    await router.push('/biblioteca')
    expect(router.currentRoute.value.name).toBe('biblioteca-catalogo')
    await router.push('/eventos')
    expect(router.currentRoute.value.name).toBe('eventos-publico')
    await router.push('/eventos/12')
    expect(router.currentRoute.value.name).toBe('eventos-publico-detalhes')
    await router.push('/arborizacao/substituicao-nim')
    expect(router.currentRoute.value.name).toBe('arborizacao-substituicao-nim')
    await router.push('/arborizacao/substituicao-nim/acompanhar')
    expect(router.currentRoute.value.name).toBe('arborizacao-substituicao-acompanhar')
    for (const path of ['/perfil', '/sistema', '/sistema/perfil', '/gestao', '/ifce/equipe']) {
      await router.push(path)
      expect(router.currentRoute.value.name).toBe('home')
      expect(router.currentRoute.value.query).toEqual({ auth: 'required', redirect: path })
    }

    for (const path of [
      '/arborizacao/assistente',
      '/arborizacao/recomendacoes',
      '/arborizacao/solicitar-muda',
      '/arborizacao/conversao',
      '/arborizacao/acompanhar',
    ]) {
      await router.push(path)
      expect(router.currentRoute.value.name).toBe('home')
      expect(router.currentRoute.value.query).toEqual({ auth: 'required', redirect: path })
    }

    await router.push('/arborizacao')
    expect(router.currentRoute.value.name).toBe('home')
    expect(router.currentRoute.value.query).toEqual({ auth: 'required', redirect: '/arborizacao' })
  })

  it.each(USER_ROLES)('direciona o login de %s, restaura sessão e mantém restrições específicas', async (role) => {
    const isIfceScholar = role === 'ROLE_BOLSISTA_IFCE'
    const isIfceManager = role === 'ROLE_GESTOR_IFCE'
    const { auth, router } = await session()
    vi.mocked(autenticarUsuario).mockResolvedValue({ token: 'test-only-token', nome: 'Pessoa Teste', role, tipo: 'Bearer' })
    await auth.login({ login: 'pessoa@example.invalid', senha: 'test-only' })
    await router.push(postLoginTarget(router, auth, undefined))
    expect(router.currentRoute.value.name).toBe(role === 'ROLE_CIDADAO' ? 'perfil' : isIfceScholar ? 'viveiro' : isIfceManager ? 'ifce-equipe' : 'dashboard')
    expect(router.currentRoute.value.matched[0]?.path).toBe(role === 'ROLE_CIDADAO' ? '/' : isIfceManager ? '/ifce' : '/sistema')
    for (const [path, name] of [
      ['/eventos', 'eventos-publico'],
      ['/eventos/12', 'eventos-publico-detalhes'],
      ['/arborizacao/substituicao-nim', 'arborizacao-substituicao-nim'],
      ['/arborizacao/substituicao-nim/acompanhar', 'arborizacao-substituicao-acompanhar'],
    ]) {
      await router.push(path)
      expect(router.currentRoute.value.name).toBe(name)
    }
    if (role === 'ROLE_FUNCIONARIO_AMMA' || role === 'ROLE_ADMINISTRADOR_AMMA') {
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
    expect(postLoginTarget(router, auth, '/sistema/inventario')).toEqual(isIfceManager ? { name: 'ifce-equipe' } : role === 'ROLE_CIDADAO' ? { name: 'perfil' } : isIfceScholar ? { name: 'viveiro' } : '/sistema/inventario')
    await router.push('/gestao')
    expect(router.currentRoute.value.name).toBe(isIfceScholar ? 'viveiro' : role === 'ROLE_ADMINISTRADOR_AMMA' ? 'gestao' : isIfceManager ? 'ifce-equipe' : 'home')
    await router.push('/ifce/equipe')
    expect(router.currentRoute.value.name).toBe(isIfceScholar ? 'viveiro' : role === 'ROLE_GESTOR_IFCE' ? 'ifce-equipe' : 'home')
  })

  it('limita a navegação de ROLE_BOLSISTA_IFCE ao Viveiro', async () => {
    const role = 'ROLE_BOLSISTA_IFCE'
    const { router, auth } = await session(role, '/sistema/inventario')
    expect(auth.isIfceScholar).toBe(true)
    expect(router.currentRoute.value.name).toBe('viveiro')
    expect(postLoginTarget(router, auth, '/sistema/inventario')).toEqual({ name: 'viveiro' })

    expect(postLoginTarget(router, auth, '/biblioteca')).toBe('/biblioteca')
    expect(postLoginTarget(router, auth, '/')).toBe('/')

    for (const path of ['/dashboard', '/sistema', '/sistema/inventario', '/gestao', '/ifce/equipe', '/arborizacao/invalida']) {
      await router.push(path)
      expect(router.currentRoute.value.name).toBe('viveiro')
    }

    await router.push('/sistema/perfil')
    expect(router.currentRoute.value.name).toBe('perfil-institucional')

    await router.push('/')
    expect(router.currentRoute.value.name).toBe('home')

    for (const [path, name] of [
      ['/biblioteca', 'biblioteca-catalogo'],
      ['/biblioteca/12', 'biblioteca-documento'],
      ['/arborizacao', 'arborizacao'],
      ['/arborizacao/assistente', 'arborizacao-assistente'],
      ['/arborizacao/recomendacoes', 'arborizacao-recomendacoes'],
      ['/arborizacao/solicitar-muda', 'solicitar-muda'],
      ['/arborizacao/conversao', 'arborizacao-conversao'],
      ['/arborizacao/acompanhar', 'arborizacao-acompanhar'],
    ]) {
      await router.push(path)
      expect(router.currentRoute.value.name).toBe(name)
    }

    for (const [path, name] of [
      ['/sistema/viveiro', 'viveiro'],
      ['/sistema/viveiro/cadastrar-especie', 'viveiro-cadastrar'],
      ['/sistema/viveiro/12', 'viveiro-detalhes'],
      ['/sistema/viveiro/12/editar', 'viveiro-editar'],
      ['/sistema/perfil', 'perfil-institucional'],
    ]) {
      await router.push(path)
      expect(router.currentRoute.value.name).toBe(name)
    }
  })

  it('preserva o site público e limita o gestor às demais áreas autorizadas', async () => {
    const { router, auth } = await session('ROLE_GESTOR_IFCE', '/sistema/inventario')
    expect(auth.canManageIfceTeam).toBe(true)
    expect(router.currentRoute.value.name).toBe('ifce-equipe')
    expect(postLoginTarget(router, auth, '/sistema/inventario')).toEqual({ name: 'ifce-equipe' })
    expect(postLoginTarget(router, auth, '/')).toBe('/')

    for (const [path, name] of [
      ['/', 'home'],
      ['/biblioteca', 'biblioteca-catalogo'],
      ['/biblioteca/12', 'biblioteca-documento'],
      ['/arborizacao', 'arborizacao'],
      ['/arborizacao/assistente', 'arborizacao-assistente'],
      ['/arborizacao/recomendacoes', 'arborizacao-recomendacoes'],
      ['/arborizacao/solicitar-muda', 'solicitar-muda'],
      ['/arborizacao/conversao', 'arborizacao-conversao'],
      ['/arborizacao/acompanhar', 'arborizacao-acompanhar'],
      ['/sistema/viveiro', 'viveiro'],
      ['/sistema/viveiro/cadastrar-especie', 'viveiro-cadastrar'],
      ['/sistema/viveiro/12', 'viveiro-detalhes'],
      ['/sistema/viveiro/12/editar', 'viveiro-editar'],
      ['/ifce/equipe', 'ifce-equipe'],
      ['/sistema/perfil', 'perfil-institucional'],
    ]) {
      await router.push(path)
      expect(router.currentRoute.value.name).toBe(name)
    }

    for (const path of ['/dashboard', '/perfil', '/sistema', '/sistema/inventario', '/sistema/biblioteca', '/gestao']) {
      await router.push(path)
      expect(router.currentRoute.value.name).toBe('ifce-equipe')
    }
  })

  it('impede URLs administrativas para cidadão e mantém o perfil público na URL direta', async () => {
    const { router } = await session('ROLE_CIDADAO', '/perfil')
    expect(router.currentRoute.value.name).toBe('perfil')
    for (const path of ['/sistema', '/sistema/inventario', '/sistema/viveiro', '/sistema/biblioteca']) {
      await router.push(path)
      expect(router.currentRoute.value.name).toBe('perfil')
    }
    await router.push('/sistema/perfil')
    expect(router.currentRoute.value.name).toBe('perfil')
  })

  it('mantém institucional no layout administrativo do perfil', async () => {
    const { router } = await session('ROLE_FUNCIONARIO_AMMA', '/perfil')
    expect(router.currentRoute.value.name).toBe('perfil-institucional')
    expect(router.currentRoute.value.matched[0]?.path).toBe('/sistema')
  })

  it('aceita somente destinos existentes e permitidos depois do login', async () => {
    const { router, auth } = await session('ROLE_CIDADAO')
    expect(postLoginTarget(router, auth, '/perfil')).toBe('/perfil')
    for (const target of ['/sistema', '/gestao', '//external.invalid', 'https://external.invalid', '/missing', '/\\external.invalid']) {
      expect(postLoginTarget(router, auth, target)).toEqual({ name: 'perfil' })
    }
  })

  it.each(USER_ROLES)('retoma a rota solicitada de Arborização para %s após o login', async (role) => {
    const { router, auth } = await session(role)
    const redirect = '/arborizacao/recomendacoes?origem=quiz'

    expect(postLoginTarget(router, auth, redirect)).toBe(redirect)
    await router.push(postLoginTarget(router, auth, redirect))
    expect(router.currentRoute.value.name).toBe('arborizacao-recomendacoes')
    expect(router.currentRoute.value.query.origem).toBe('quiz')
  })

  it('ignora destinos de Arborização não autorizados e usa o destino padrão do perfil', async () => {
    const { router, auth: scholar } = await session('ROLE_BOLSISTA_IFCE')
    expect(postLoginTarget(router, scholar, '/arborizacao/invalida')).toEqual({ name: 'viveiro' })
    expect(postLoginTarget(router, scholar, 'https://external.invalid/arborizacao')).toEqual({ name: 'viveiro' })

    const { auth: managerAuth } = await session('ROLE_GESTOR_IFCE')
    expect(postLoginTarget(router, managerAuth, '/arborizacao/invalida')).toEqual({ name: 'ifce-equipe' })
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
    expect(values.has('arborize.auth.token')).toBe(false)
    expect(values.has('arborize.auth.name')).toBe(false)
    expect(values.has('arborize.auth.role')).toBe(false)
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
    expect(html).toContain('Arborização')
    if (role === 'ROLE_CIDADAO') {
      expect(html).not.toContain('aria-label="Dashboard"')
      expect(html).toContain('Meu perfil')
      expect(html).not.toContain('Voltar ao painel')
    } else {
      expect(html).toContain('Voltar ao painel')
      expect(html).not.toContain('Meu perfil')
      expect(html).not.toContain('aria-label="Dashboard"')
    }
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

  it('mostra somente Viveiro no menu móvel do bolsista canônico', async () => {
    const role = 'ROLE_BOLSISTA_IFCE'
    const { router, pinia } = await session(role, '/sistema/viveiro')
    const originalWidth = window.innerWidth
    Object.defineProperty(window, 'innerWidth', { configurable: true, value: 375 })
    const wrapper = mount(AppSidebar, { global: { plugins: [pinia, router] } })

    try {
      await nextTick()
      const openMenu = wrapper.get('[aria-label="Abrir menu de navegação"]')
      expect(openMenu.attributes('aria-expanded')).toBe('false')
      await openMenu.trigger('click')
      await nextTick()

      const navigation = wrapper.findAll('nav')
      expect(navigation).toHaveLength(2)
      expect(navigation[1]!.text()).toContain('Viveiro')
      expect(navigation[1]!.text()).not.toContain('Dashboard e Relatórios')
      expect(navigation[1]!.text()).not.toContain('Biblioteca Digital')
      expect(navigation[1]!.text()).not.toContain('Inventário')
      expect(navigation[1]!.text()).not.toContain('Solicitações')
      expect(navigation[1]!.text()).not.toContain('Laudos')
      expect(navigation[1]!.text()).not.toContain('Eventos')
      expect(wrapper.find('[aria-label="Fechar menu de navegação"]').exists()).toBe(true)
    } finally {
      wrapper.unmount()
      Object.defineProperty(window, 'innerWidth', { configurable: true, value: originalWidth })
    }
  })

  it('mostra somente Viveiro no menu desktop do bolsista canônico', async () => {
    const { router, pinia } = await session('ROLE_BOLSISTA_IFCE', '/sistema/viveiro')
    const originalWidth = window.innerWidth
    Object.defineProperty(window, 'innerWidth', { configurable: true, value: 1280 })
    const wrapper = mount(AppSidebar, { global: { plugins: [pinia, router] } })

    try {
      await nextTick()
      const menuText = wrapper.find('nav').text()
      expect(menuText).toContain('Viveiro')
      expect(menuText).not.toContain('Dashboard e Relatórios')
      expect(menuText).not.toContain('Biblioteca Digital')
      expect(menuText).not.toContain('Inventário')
      expect(menuText).not.toContain('Solicitações')
      expect(menuText).not.toContain('Laudos')
      expect(menuText).not.toContain('Eventos')
      expect(wrapper.find('[aria-label="Abrir menu de navegação"]').exists()).toBe(false)
    } finally {
      wrapper.unmount()
      Object.defineProperty(window, 'innerWidth', { configurable: true, value: originalWidth })
    }
  })

  it('mostra somente Viveiro e Equipe no menu do gestor em desktop e móvel', async () => {
    const { router, pinia } = await session('ROLE_GESTOR_IFCE', '/ifce/equipe')
    const originalWidth = window.innerWidth

    try {
      for (const width of [1280, 375]) {
        Object.defineProperty(window, 'innerWidth', { configurable: true, value: width })
        const wrapper = mount(AppSidebar, { global: { plugins: [pinia, router] } })
        await nextTick()
        if (width < 768) {
          await wrapper.get('[aria-label="Abrir menu de navegação"]').trigger('click')
          await nextTick()
        }
        const navigation = wrapper.findAll('nav')
        const menuText = navigation.map(nav => nav.text()).join(' ')
        expect(menuText).toContain('Viveiro')
        expect(menuText).toContain('Equipe do Laboratório')
        expect(menuText).not.toContain('Dashboard e Relatórios')
        expect(menuText).not.toContain('Biblioteca Digital')
        expect(menuText).not.toContain('Inventário')
        expect(menuText).not.toContain('Solicitações')
        expect(menuText).not.toContain('Laudos')
        expect(menuText).not.toContain('Eventos')
        wrapper.unmount()
      }
    } finally {
      Object.defineProperty(window, 'innerWidth', { configurable: true, value: originalWidth })
    }
  })

  it('mantém no cabeçalho do gestor somente home, equipe, perfil próprio e saída', async () => {
    const { router, pinia } = await session('ROLE_GESTOR_IFCE', '/ifce/equipe')
    const root = new ElementoTeste('ROOT')
    const app = renderer.createApp(AppHeader).use(pinia).use(router)
    app.mount(root)

    expect(encontrar(root, no => no.props['aria-label'] === 'Visitar página pública').props.href).toBe('/')
    expect(encontrar(root, no => no.props['aria-label'] === 'Equipe do Laboratório IFCE')).toBeDefined()
    expect(encontrar(root, no => no.props['aria-label'] === 'Meu perfil')).toBeDefined()
    expect(encontrar(root, no => no.props.title === 'Sair')).toBeDefined()
    expect(root.textContent).not.toContain('Painel de Gestão')

    app.unmount()
  })

  it('preserva toda a navegação pública do gestor e volta à Equipe do IFCE pelo painel', async () => {
    const { router, pinia } = await session('ROLE_GESTOR_IFCE', '/')
    const root = new ElementoTeste('ROOT')
    const app = renderer.createApp(TopBar).use(pinia).use(router)
    app.mount(root)

    expect(root.textContent).toContain('Início')
    expect(root.textContent).toContain('Arborização')
    expect(root.textContent).toContain('Biblioteca Digital')
    encontrar(root, no => no.props['aria-label'] === 'Voltar ao painel').dispatch('click')
    await vi.waitFor(() => expect(router.currentRoute.value.name).toBe('ifce-equipe'))

    app.unmount()
  })

  it('mostra atalho público, perfil próprio e saída no cabeçalho do bolsista canônico', async () => {
    const role = 'ROLE_BOLSISTA_IFCE'
    const { router, pinia } = await session(role, '/sistema/viveiro')
    const root = new ElementoTeste('ROOT')
    const app = renderer.createApp(AppHeader).use(pinia).use(router)
    app.mount(root)
    const publicLink = encontrar(root, no => no.props['aria-label'] === 'Visitar página pública')
    expect(publicLink.props.href).toBe('/')
    encontrar(root, no => no.props['aria-label'] === 'Meu perfil').dispatch('click')
    await vi.waitFor(() => expect(router.currentRoute.value.name).toBe('perfil-institucional'))
    expect(encontrar(root, no => no.props.title === 'Sair')).toBeDefined()
    app.unmount()
  })

  it.each(USER_ROLES)('o link Eventos abre a página pública para %s', async (role) => {
    const { router, pinia } = await session(role, '/')
    const wrapper = mount(TopBar, { global: { plugins: [pinia, router] } })
    const eventsLink = wrapper.findAll('nav button').find((button) => button.text().trim() === 'Eventos')

    expect(eventsLink).toBeDefined()
    await eventsLink!.trigger('click')
    await vi.waitFor(() => expect(router.currentRoute.value.name).toBe('eventos-publico'))
    wrapper.unmount()
  })

  it.each(USER_ROLES)('Fazer requerimento abre o fluxo público para %s', async (role) => {
    const { router, pinia } = await session(role, '/arborizacao')
    const wrapper = mount(ArborizacaoInicioView, { global: { plugins: [pinia, router] } })
    const requestLink = wrapper.findAll('a').find((link) => link.text().includes('Fazer requerimento'))

    expect(requestLink).toBeDefined()
    await requestLink!.trigger('click')
    await vi.waitFor(() => expect(router.currentRoute.value.name).toBe('arborizacao-substituicao-nim'))
    wrapper.unmount()
  })
})
