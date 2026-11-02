import type { RouteLocationResolved, RouteLocationRaw, Router } from 'vue-router'

interface SessionAccess {
  isAuthenticated: boolean
  isCitizen: boolean
  isAmmaAdmin: boolean
  canManageIfceTeam: boolean
  canAccessManagement: boolean
  dashboardRouteName: string
}

export function sessionNavigation(to: Pick<RouteLocationResolved, 'meta' | 'name' | 'fullPath'>, auth: SessionAccess) {
  const restricted = to.meta.requiresAuth || to.meta.requiresManagement || to.meta.requiresCitizen || to.meta.requiresAdmin || to.meta.requiresIfceTeam
  if (restricted && !auth.isAuthenticated) {
    return { name: 'home', query: { auth: 'required', redirect: to.fullPath } }
  }
  if (to.name === 'perfil' && auth.canAccessManagement) return { name: 'perfil-institucional' }
  if (to.name === 'perfil-institucional' && auth.isCitizen) return { name: 'perfil' }
  if (to.meta.requiresAdmin && !auth.isAmmaAdmin) return { name: 'home' }
  if (to.meta.requiresIfceTeam && !auth.canManageIfceTeam) return { name: 'home' }
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
  return { name: auth.dashboardRouteName }
}
