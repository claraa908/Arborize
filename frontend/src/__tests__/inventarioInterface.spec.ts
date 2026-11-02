// @vitest-environment node
import { describe, expect, it, vi } from 'vitest'
import { createSSRApp } from 'vue'
import { renderToString } from 'vue/server-renderer'
import { createPinia, setActivePinia } from 'pinia'
import { createMemoryHistory, createRouter } from 'vue-router'
import AppDataTable from '@/components/ui/AppDataTable.vue'
import InventarioView from '@/views/auth/InventarioView.vue'
import { useInventarioStore } from '@/stores/inventarioStore'
import { InventarioApiError } from '@/services/inventarioService'
import { resumo } from './inventarioFixtures'

// SSR sem DOM: o mapa WebGL é substituído apenas neste teste de apresentação.
vi.mock('@/components/inventario/InventarioMapa.vue', () => ({
  default: {
    props: ['arvores', 'mensagemVazia', 'selecionadaId'],
    template: '<div>{{ mensagemVazia }}</div>',
  },
}))

async function tabela(
  pagination?: { page: number; totalPages: number; totalElements: number },
  loading = false,
) {
  return renderToString(
    createSSRApp(AppDataTable, {
      columns: [{ key: 'id', label: 'Código' }],
      items: [{ id: 42 }],
      defaultView: 'table',
      showSearch: false,
      allowViewToggle: false,
      selectedId: 42,
      pagination,
      loading,
    }),
  )
}
function botao(html: string, label: string) {
  return html.match(new RegExp(`<button[^>]*aria-label="${label}"[^>]*>`))?.[0] ?? ''
}

describe('apresentação da paginação compartilhada', () => {
  it('usa total real e primeira página, com destaque da seleção', async () => {
    const html = await tabela({ page: 0, totalPages: 3, totalElements: 45 })
    expect(html).toContain('45 registros')
    expect(html).toMatch(/Página 1 de\s*3/)
    expect(botao(html, 'Página anterior')).toMatch(/\sdisabled(?:[\s=>])/)
    expect(botao(html, 'Próxima página')).not.toMatch(/\sdisabled(?:[\s=>])/)
    expect(html).toContain('bg-amma-green-lighter')
  })
  it('desabilita próxima na última página e ambas durante consulta', async () => {
    const ultima = await tabela({ page: 2, totalPages: 3, totalElements: 45 })
    expect(botao(ultima, 'Próxima página')).toMatch(/\sdisabled(?:[\s=>])/)
    expect(botao(ultima, 'Página anterior')).not.toMatch(/\sdisabled(?:[\s=>])/)
    const carregando = await tabela({ page: 1, totalPages: 3, totalElements: 45 }, true)
    expect(botao(carregando, 'Próxima página')).toMatch(/\sdisabled(?:[\s=>])/)
    expect(botao(carregando, 'Página anterior')).toMatch(/\sdisabled(?:[\s=>])/)
  })
  it('preserva navegação desabilitada dos consumidores sem paginação', async () => {
    const html = await tabela()
    expect(html).toContain('1 registros')
    expect(botao(html, 'Próxima página')).toMatch(/\sdisabled(?:[\s=>])/)
    expect(botao(html, 'Página anterior')).toMatch(/\sdisabled(?:[\s=>])/)
  })
})

async function pagina(configurar: (store: ReturnType<typeof useInventarioStore>) => void) {
  const pinia = createPinia()
  setActivePinia(pinia)
  configurar(useInventarioStore())
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/sistema/inventario', name: 'inventario', component: InventarioView },
      { path: '/sistema', name: 'dashboard', component: { template: '<div />' } },
      {
        path: '/sistema/inventario/nova',
        name: 'inventario-nova',
        component: { template: '<div />' },
      },
      {
        path: '/sistema/inventario/:id',
        name: 'inventario-detalhes',
        component: { template: '<div />' },
      },
    ],
  })
  await router.push('/sistema/inventario')
  return renderToString(createSSRApp(InventarioView).use(pinia).use(router))
}

describe('estados visuais do inventário', () => {
  it.each([401, 403])('HTTP %s não apresenta ausência de árvores', async (status) => {
    const html = await pagina((store) => {
      store.erros.listagem = new InventarioApiError(status, null, undefined)
      store.erros.mapa = new InventarioApiError(status, null, undefined)
    })
    expect(html).toContain('Acesso ao inventário indisponível')
    expect(html).toContain('Não foi possível acessar o inventário.')
    expect(html).not.toContain('Nenhuma árvore cadastrada.')
    expect(html).not.toContain('0 registros')
  })
  it('vazio legítimo aparece somente depois de consulta concluída', async () => {
    const html = await pagina((store) => {
      store.pagina = { content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 }
      store.registrosMapa = []
    })
    expect(html).toContain('Nenhuma árvore cadastrada.')
    expect(html).toContain('0 registros')
    expect(html).not.toContain('Acesso ao inventário indisponível')
  })
  it('renderiza link da ficha e ação de localização para registro real recebido', async () => {
    const html = await pagina((store) => {
      store.pagina = { content: [resumo], page: 0, size: 20, totalElements: 1, totalPages: 1 }
      store.registrosMapa = [resumo]
    })
    expect(html).toContain(`href="/sistema/inventario/${resumo.id}"`)
    expect(html).toContain(resumo.codigo)
    expect(html).toContain(`Localizar árvore ${resumo.codigo} no mapa`)
    expect(html).toContain(resumo.especie.nomePopular)
  })

  it('renderiza o filtro territorial como seletor do catálogo por nome', async () => {
    const html = await pagina((store) => {
      store.opcoesBairros = [
        { id: 5, nome: 'Bôto', ativo: true },
        { id: 2, nome: 'Centro', ativo: true },
      ]
    })

    expect(html).toContain('Todos os bairros')
    expect(html).toMatch(/<select[^>]*id="inventario-bairro"/)
    expect(html).toContain('<option value="Bôto">Bôto</option>')
    expect(html).toContain('<option value="Centro">Centro</option>')
    expect(html).not.toContain('Digite um bairro')
  })
})
