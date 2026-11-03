// @vitest-environment node
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createSSRApp, ref } from 'vue'
import { renderToString } from 'vue/server-renderer'
import { createMemoryHistory, createRouter, RouterView } from 'vue-router'
import { InventarioApiError } from '@/services/inventarioService'
import { useArvoreFicha } from '@/components/inventario/useArvoreFicha'
import ArvoreDetalhesView from '@/views/auth/inventario/ArvoreDetalhesView.vue'
import { arvore } from './inventarioFixtures'

vi.mock('@/components/inventario/InventarioMapa.vue', () => ({
  default: {
    props: ['arvores'],
    template: '<div role="region" aria-label="Mapa da ficha">{{ arvores[0]?.id }}</div>',
  },
}))

vi.mock('@/components/inventario/ArvoreFotografias.vue', () => ({
  default: {
    props: ['arvoreId', 'permitirUpload'],
    template: '<section aria-label="Galeria da árvore">Fotos de {{ arvoreId }}</section>',
  },
}))

vi.mock('@/components/inventario/useArvoreFicha', () => ({
  useArvoreFicha: vi.fn(),
}))

async function renderizarFicha() {
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/sistema', name: 'dashboard', component: { template: '<div />' } },
      { path: '/sistema/inventario', name: 'inventario', component: { template: '<div />' } },
      {
        path: '/sistema/inventario/:id',
        name: 'inventario-detalhes',
        component: ArvoreDetalhesView,
      },
      {
        path: '/sistema/inventario/:id/editar',
        name: 'inventario-editar',
        component: { template: '<div />' },
      },
    ],
  })
  await router.push('/sistema/inventario/42')
  await router.isReady()
  return renderToString(createSSRApp({ render: () => h(RouterView) }).use(router))
}

import { h } from 'vue'

beforeEach(() => vi.clearAllMocks())

describe('ArvoreDetalhesView: apresentação da ficha', () => {
  it('apresenta dados reais fornecidos, mapa, valores ausentes e link de edição', async () => {
    const arvoreSemOpcionais = {
      ...arvore,
      especie: { ...arvore.especie, familiaBotanica: null },
      bairro: null,
      referenciaLocalizacao: null,
      dataPlantio: null,
      dapCm: null,
      alturaEstimadaM: null,
      observacoes: null,
    }
    vi.mocked(useArvoreFicha).mockReturnValue({
      arvore: ref(arvoreSemOpcionais),
      carregando: ref(false),
      erro: ref(null),
      recarregar: vi.fn(),
    })

    const html = await renderizarFicha()
    expect(html).toContain(arvore.codigo)
    expect(html).toContain(arvore.especie.nomePopular)
    expect(html).toContain(arvore.especie.nomeCientifico)
    expect(html).toContain('Não avaliada')
    expect(html).toContain('Mapa da ficha')
    expect(html).toContain('Não informado')
    expect(html).toContain(`href="/sistema/inventario/${arvore.id}/editar"`)
    expect(html).toContain('sm:grid-cols-2')
    expect(html).not.toContain('Não carregado')
  })

  it('apresenta o estado de carregamento sem informações de árvores anteriores', async () => {
    vi.mocked(useArvoreFicha).mockReturnValue({
      arvore: ref(null),
      carregando: ref(true),
      erro: ref(null),
      recarregar: vi.fn(),
    })
    const html = await renderizarFicha()
    expect(html).toContain('Carregando ficha técnica')
    expect(html).not.toContain(arvore.especie.nomePopular)
  })

  it('apresenta registro inexistente e não revela uma ficha anterior', async () => {
    vi.mocked(useArvoreFicha).mockReturnValue({
      arvore: ref(null),
      carregando: ref(false),
      erro: ref(new InventarioApiError(404, null, undefined)),
      recarregar: vi.fn(),
    })
    const html = await renderizarFicha()
    expect(html).toContain('Árvore não encontrada')
    expect(html).toContain('Não foi encontrada uma árvore com o identificador 42.')
    expect(html).not.toContain(arvore.especie.nomePopular)
  })

  it.each([401, 403])(
    'trata HTTP %s como acesso negado, sem exibir ficha ou vazio',
    async (status) => {
      vi.mocked(useArvoreFicha).mockReturnValue({
        arvore: ref(null),
        carregando: ref(false),
        erro: ref(new InventarioApiError(status, null, undefined)),
        recarregar: vi.fn(),
      })
      const html = await renderizarFicha()
      expect(html).toContain('Acesso ao inventário indisponível')
      expect(html).toContain('Não foi possível acessar esta ficha.')
      expect(html).not.toContain(`HTTP ${status}`)
      expect(html).not.toContain(arvore.especie.nomePopular)
      expect(html).not.toContain('Nenhuma árvore cadastrada')
    },
  )
})
