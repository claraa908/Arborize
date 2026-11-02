// @vitest-environment node
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createSSRApp, h, ref } from 'vue'
import { renderToString } from 'vue/server-renderer'
import { createMemoryHistory, createRouter, RouterView } from 'vue-router'
import { InventarioApiError } from '@/services/inventarioService'
import { useArvoreFicha } from '@/components/inventario/useArvoreFicha'
import ArvoreFormView from '@/views/auth/inventario/ArvoreFormView.vue'
import { arvore } from './inventarioFixtures'
import { createPinia } from 'pinia'

vi.mock('@/components/inventario/ArvoreLocalizacaoMapa.vue', () => ({
  default: {
    props: ['latitude', 'longitude', 'centroInicial'],
    template:
      '<div role="region" aria-label="Mapa auxiliar" :data-center="centroInicial?.join(\',\')">{{ latitude }},{{ longitude }}</div>',
  },
}))

vi.mock('@/components/inventario/ArvoreFotografias.vue', () => ({
  default: {
    props: ['arvoreId', 'permitirUpload', 'permitirRemocao'],
    template: '<section aria-label="Galeria administrativa">Fotografias habilitadas</section>',
  },
}))

vi.mock('@/components/inventario/useArvoreFicha', () => ({
  useArvoreFicha: vi.fn(),
}))

async function renderizarEdicao() {
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/sistema', name: 'dashboard', component: { template: '<div />' } },
      { path: '/sistema/inventario', name: 'inventario', component: { template: '<div />' } },
      {
        path: '/sistema/inventario/:id',
        name: 'inventario-detalhes',
        component: { template: '<div />' },
      },
      {
        path: '/sistema/inventario/:id/editar',
        name: 'inventario-editar',
        component: ArvoreFormView,
      },
    ],
  })
  await router.push('/sistema/inventario/42/editar')
  await router.isReady()
  const app = createSSRApp({ render: () => h(RouterView) })
    .use(router)
    .use(createPinia())
  return renderToString(app)
}

beforeEach(() => vi.clearAllMocks())

describe('ArvoreFormView: edição funcional', () => {
  it('preenche os campos com a árvore consultada e centraliza o mapa em suas coordenadas', async () => {
    vi.mocked(useArvoreFicha).mockReturnValue({
      arvore: ref({
        ...arvore,
        latitude: -4.97,
        longitude: -39.015,
        referenciaLocalizacao: 'Praça central',
        dataPlantio: '2020-06-15',
        dapCm: 12.5,
        alturaEstimadaM: 4.2,
        observacoes: 'Registro de campo',
      }),
      carregando: ref(false),
      erro: ref(null),
      recarregar: vi.fn(),
    })

    const html = await renderizarEdicao()
    expect(html).toContain('Editar árvore')
    expect(html).toContain(arvore.codigo)
    expect(html).toContain('>-4.970000</output>')
    expect(html).toContain('>-39.015000</output>')
    expect(html).toContain('value="2020-06-15"')
    expect(html).toContain('value="12.5"')
    expect(html).toContain('value="4.2"')
    expect(html).toContain('Praça central')
    expect(html).toContain('Registro de campo')
    expect(html).toContain('data-center="-39.015,-4.97"')
    expect(html).toContain('Salvar alterações')
    expect(html).toContain('Fotografias habilitadas')
    expect(html).not.toContain('Não carregado')
  })

  it('não mostra campos editáveis antes de concluir o carregamento', async () => {
    vi.mocked(useArvoreFicha).mockReturnValue({
      arvore: ref(null),
      carregando: ref(true),
      erro: ref(null),
      recarregar: vi.fn(),
    })
    const html = await renderizarEdicao()
    expect(html).toContain('Carregando árvore para edição')
    expect(html).not.toContain('Salvar alterações')
    expect(html).not.toContain('Identificação botânica')
  })

  it.each([
    [404, 'Árvore não encontrada'],
    [401, 'Acesso ao inventário indisponível'],
    [403, 'Acesso ao inventário indisponível'],
  ])('apresenta erro HTTP %s sem exibir formulário vazio', async (status, titulo) => {
    vi.mocked(useArvoreFicha).mockReturnValue({
      arvore: ref(null),
      carregando: ref(false),
      erro: ref(new InventarioApiError(status, null, undefined)),
      recarregar: vi.fn(),
    })
    const html = await renderizarEdicao()
    expect(html).toContain(titulo)
    expect(html).not.toContain('Salvar alterações')
    expect(html).not.toContain('Selecione uma espécie')
  })
})
