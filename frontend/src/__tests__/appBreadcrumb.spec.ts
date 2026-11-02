import { createSSRApp } from 'vue'
import { createMemoryHistory, createRouter } from 'vue-router'
import { renderToString } from 'vue/server-renderer'
import { describe, expect, it } from 'vitest'
import AppBreadcrumb, { type BreadcrumbItem } from '@/components/ui/AppBreadcrumb.vue'

async function renderizarBreadcrumb(items: BreadcrumbItem[]) {
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/sistema', name: 'dashboard', component: { template: '<div />' } },
      {
        path: '/sistema/inventario',
        name: 'inventario',
        component: { template: '<div />' },
      },
      {
        path: '/sistema/inventario/:id',
        name: 'inventario-detalhes',
        component: { template: '<div />' },
      },
      {
        path: '/sistema/inventario/:id/editar',
        name: 'inventario-editar',
        component: { template: '<div />' },
      },
    ],
  })
  await router.push({ name: 'inventario-editar', params: { id: '42' } })
  return renderToString(createSSRApp(AppBreadcrumb, { items }).use(router))
}

describe('AppBreadcrumb', () => {
  it('renderiza navegação semântica e retorna para o item anterior', async () => {
    const items: BreadcrumbItem[] = [
      { label: 'Início', routeName: 'dashboard', icon: 'home' },
      { label: 'Inventário', routeName: 'inventario' },
      { label: 'Árvore 42', routeName: 'inventario-detalhes', params: { id: '42' } },
      { label: 'Editar árvore' },
    ]
    const html = await renderizarBreadcrumb(items)

    expect(html).toContain('<nav aria-label="Caminho de navegação"')
    expect(html).toContain('aria-current="page"')
    expect(html).toContain('aria-label="Voltar para Árvore 42"')
    expect(html).toContain('href="/sistema/inventario/42"')
  })
})
