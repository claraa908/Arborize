import type { RouteLocationResolved, RouteLocationRaw, Router } from 'vue-router'

interface SessionAccess {
  isAuthenticated: boolean
  isCitizen: boolean
  isIfceScholar: boolean
  isAmmaAdmin: boolean
  canManageCompensacao: boolean
  canManageIfceTeam: boolean
  canAccessManagement: boolean
  dashboardRouteName: string
}

const IFCE_SCHOLAR_ALLOWED_ROUTE_NAMES = new Set([
  'home',
  'eventos-publico',
  'eventos-publico-detalhes',
  'arborizacao',
  'arborizacao-substituicao-nim',
  'arborizacao-substituicao-acompanhar',
  'arborizacao-assistente',
  'arborizacao-recomendacoes',
  'solicitar-muda',
  'arborizacao-conversao',
  'arborizacao-acompanhar',
  'biblioteca-catalogo',
  'biblioteca-documento',
  'viveiro',
  'viveiro-cadastrar',
  'viveiro-detalhes',
  'viveiro-editar',
  'perfil-institucional',
])

const IFCE_MANAGER_ALLOWED_ROUTE_NAMES = new Set([
  'home',
  'eventos-publico',
  'eventos-publico-detalhes',
  'biblioteca-catalogo',
  'biblioteca-documento',
  'arborizacao',
  'arborizacao-substituicao-nim',
  'arborizacao-substituicao-acompanhar',
  'arborizacao-assistente',
  'arborizacao-recomendacoes',
  'solicitar-muda',
  'arborizacao-conversao',
  'arborizacao-acompanhar',
  'viveiro',
  'viveiro-cadastrar',
  'viveiro-detalhes',
  'viveiro-editar',
  'ifce-equipe',
  'ifce-equipe-novo',
  'perfil-institucional',
])

export function sessionNavigation(
  to: Pick<RouteLocationResolved, 'meta' | 'name' | 'fullPath'>,
  auth: SessionAccess,
  from?: Pick<RouteLocationResolved, 'name' | 'matched'>,
) {
  const restricted = to.meta.requiresAuth || to.meta.requiresManagement || to.meta.requiresCitizen || to.meta.requiresAdmin || to.meta.requiresIfceTeam || to.meta.requiresAmmaStaff
  if (restricted && !auth.isAuthenticated) {
    const origemPreservada =
      from?.matched?.length && (from.name === 'arborizacao' || from.name === 'arborizacao-recomendacoes')
        ? from.name
        : 'home'
    const motivo =
      to.name === 'solicitar-muda'
        ? 'solicitar-muda'
        : to.name === 'arborizacao-substituicao-nim'
          ? 'substituicao-nim'
          : undefined
    return {
      name: origemPreservada,
      query: {
        auth: 'required',
        redirect: to.fullPath,
        ...(motivo ? { motivo } : {}),
      },
    }
  }
  if (auth.isIfceScholar && (typeof to.name !== 'string' || !IFCE_SCHOLAR_ALLOWED_ROUTE_NAMES.has(to.name))) {
    return { name: 'viveiro' }
  }
  if (auth.canManageIfceTeam && (typeof to.name !== 'string' || !IFCE_MANAGER_ALLOWED_ROUTE_NAMES.has(to.name))) {
    return { name: 'ifce-equipe' }
  }
  if (to.name === 'perfil' && auth.canAccessManagement) return { name: 'perfil-institucional' }
  if (to.name === 'perfil-institucional' && auth.isCitizen) return { name: 'perfil' }
  if (to.meta.requiresAdmin && !auth.isAmmaAdmin) return { name: 'home' }
  if (to.meta.requiresIfceTeam && !auth.canManageIfceTeam) return { name: 'home' }
  if (to.meta.requiresAmmaStaff && !auth.canManageCompensacao) return { name: auth.dashboardRouteName }
  if (to.meta.requiresCitizen && !auth.isCitizen) return { name: auth.dashboardRouteName }
  if (to.meta.requiresManagement && !auth.canAccessManagement) return { name: auth.dashboardRouteName }
  return true
}

export function postLoginTarget(router: Router, auth: SessionAccess, redirect: unknown): RouteLocationRaw {
  if (typeof redirect === 'string' && redirect.startsWith('/') && !redirect.startsWith('//') && !redirect.includes('\\')) {
    const target = router.resolve(redirect)
    const profileAlias = (target.name === 'perfil' && auth.canAccessManagement) || (target.name === 'perfil-institucional' && auth.isCitizen)
    if (target.matched.length && (sessionNavigation(target, auth) === true || profileAlias)) return redirect
  }

  if (auth.isIfceScholar) return { name: auth.dashboardRouteName }
  if (auth.canManageIfceTeam) return { name: 'ifce-equipe' }
  return { name: auth.dashboardRouteName }
}
