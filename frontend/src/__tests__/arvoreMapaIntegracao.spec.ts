import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia } from 'pinia'
import { nextTick, ref } from 'vue'
import type { Marker } from 'maplibre-gl'
import { ElementoTeste, encontrar, renderer } from './mapaTestHost'
import { arvore } from './inventarioFixtures'

const execucao = vi.hoisted(() => ({ mapas: [] as unknown[], marcadores: [] as unknown[] }))
const navegacao = vi.hoisted(() => ({ push: vi.fn() }))
const fichaDeEdicao = vi.hoisted(() => ({ bairro: null as { id: number; nome: string; ativo: boolean } | null }))

vi.mock('maplibre-gl', async (importOriginal) => {
  const original = await importOriginal<typeof import('maplibre-gl')>()
  const { ElementoTeste } = await import('./mapaTestHost')
  class MapaTeste {
    listeners = new globalThis.Map<string, Set<(event: unknown) => void>>()
    canvas = new ElementoTeste('CANVAS')
    canvasContainer = new ElementoTeste('DIV')
    center = [-39.015, -4.97]
    zoom = 13
    _camera = {
      transform: {
        width: 800,
        height: 500,
        getCoveringTilesDetailsProvider: () => ({ allowWorldCopies: () => false }),
        isLocationOccluded: () => false,
      },
    }
    constructor(opcoes: { container: ElementoTeste }) {
      this.canvas.props.tabindex = '0'
      this.canvasContainer.appendChild(this.canvas)
      opcoes.container.appendChild(this.canvasContainer)
      execucao.mapas.push(this)
    }
    on(nome: string, handler: (event: unknown) => void) {
      if (!this.listeners.has(nome)) this.listeners.set(nome, new Set())
      this.listeners.get(nome)!.add(handler)
      return this
    }
    off(nome: string, handler: (event: unknown) => void) {
      this.listeners.get(nome)?.delete(handler)
      return this
    }
    listens(nome: string) {
      return (this.listeners.get(nome)?.size ?? 0) > 0
    }
    fire(nome: string, event: unknown) {
      const completo = { originalEvent: { target: this.canvas }, ...(event as object) }
      for (const handler of this.listeners.get(nome) ?? []) handler(completo)
    }
    addControl() {}
    getCanvasContainer() {
      return this.canvasContainer
    }
    getCanvas() {
      return this.canvas
    }
    resize = vi.fn(() => this.fire('move', { type: 'move' }))
    project(coordenada: Parameters<typeof original.LngLat.convert>[0]) {
      const local = original.LngLat.convert(coordenada)
      const ponto = {
        x: local.lng,
        y: local.lat,
        _add(offset: { x: number; y: number }) {
          this.x += offset.x
          this.y += offset.y
          return this
        },
        round() {
          return this
        },
      }
      return ponto
    }
    getCenter() {
      return { toArray: () => this.center }
    }
    getZoom() {
      return this.zoom
    }
    getBearing() {
      return 0
    }
    getPitch() {
      return 0
    }
    setStyle() {}
    flyTo = vi.fn((opcoes: { center: number[]; zoom: number }) => {
      this.center = opcoes.center
      this.zoom = opcoes.zoom
    })
    remove() {
      this.listeners.clear()
      this.canvasContainer.remove()
    }
  }
  class MarcadorReal extends original.Marker {
    private elementoTeste: ElementoTeste
    private coordenada = original.LngLat.convert([0, 0])

    constructor(opcoes: ConstructorParameters<typeof original.Marker>[0]) {
      const elemento =
        (opcoes?.element as unknown as ElementoTeste | undefined) ?? new ElementoTeste('DIV')
      super({ ...opcoes, element: elemento as unknown as HTMLElement })
      this.elementoTeste = elemento
      execucao.marcadores.push(this)
    }

    setLngLat(coordenada: Parameters<typeof original.LngLat.convert>[0]) {
      this.coordenada = original.LngLat.convert(coordenada)
      return this
    }

    getLngLat() {
      return this.coordenada
    }

    addTo(mapa: { getCanvasContainer: () => ElementoTeste }) {
      mapa.getCanvasContainer().appendChild(this.elementoTeste)
      return this
    }

    remove() {
      this.elementoTeste.remove()
      return this
    }

    getElement() {
      return this.elementoTeste as unknown as HTMLElement
    }
  }
  return {
    ...original,
    Map: MapaTeste,
    Marker: MarcadorReal,
    NavigationControl: class {},
    setWorkerUrl: vi.fn(),
  }
})

vi.mock('vue-router', async () => {
  const { h } = await import('vue')
  return {
    useRouter: () => ({ push: navegacao.push }),
    RouterLink: {
      setup:
        (_: unknown, { slots }: { slots: { default: () => unknown } }) =>
        () =>
          h('a', slots.default()),
    },
  }
})
vi.mock('@/components/inventario/ArvoreFotografias.vue', () => ({
  default: { render: () => null },
}))
vi.mock('@/components/inventario/useArvoreFicha', () => ({
  useArvoreFicha: () => ({
    arvore: ref({ ...arvore, latitude: -4.97, longitude: -39.015, bairro: fichaDeEdicao.bairro }),
    carregando: ref(false),
    erro: ref(null),
  }),
}))

import ArvoreFormulario from '@/components/inventario/ArvoreFormulario.vue'
import { especieService } from '@/services/especieService'
import { inventarioService, InventarioApiError } from '@/services/inventarioService'
import { diagnosticarMapa } from '@/components/inventario/inventarioMapaDiagnostico'
import { useFeedbackStore } from '@/stores/feedbackStore'
import InventarioMapa from '@/components/inventario/InventarioMapa.vue'

const apps: Array<{ unmount: () => void }> = []
function montar(editando = false) {
  const raiz = new ElementoTeste('ROOT')
  const app = renderer.createApp(ArvoreFormulario, {
    editando,
    identificador: editando ? '42' : undefined,
  })
  app.use(createPinia())
  app.mount(raiz)
  apps.push(app)
  return raiz
}
type MapaTeste = {
  fire: (nome: string, evento: unknown) => void
  flyTo: ReturnType<typeof vi.fn>
  center: number[]
  zoom: number
  listeners: Map<string, Set<unknown>>
  canvas: ElementoTeste
  resize: ReturnType<typeof vi.fn>
}
function mapaAtual() {
  return execucao.mapas.at(-1) as MapaTeste
}
async function estabilizar() {
  for (let i = 0; i < 8; i++) {
    await Promise.resolve()
    await nextTick()
  }
}
function coordenadas(raiz: ElementoTeste) {
  return [
    encontrar(
      raiz,
      (no) => no.props['aria-labelledby'] === 'arvore-latitude-titulo',
    ).textContent.trim(),
    encontrar(
      raiz,
      (no) => no.props['aria-labelledby'] === 'arvore-longitude-titulo',
    ).textContent.trim(),
  ].map((valor) => (Number.isFinite(Number(valor)) ? String(Number(valor)) : valor))
}
function campo(raiz: ElementoTeste, id: string) {
  return encontrar(raiz, (no) => no.props.id === id)
}
async function selecionar(raiz: ElementoTeste, id: string, valor: string) {
  const select = campo(raiz, id)
  select.value = valor
  select.options.forEach((opcao) => {
    opcao.selected = String(opcao.value) === valor
  })
  select.dispatch('change')
  await nextTick()
}
function selecionarArquivos(raiz: ElementoTeste, arquivos: File[]) {
  const input = encontrar(raiz, (no) => no.props.type === 'file')
  Object.assign(input, { files: arquivos })
  input.dispatch('change')
  return input
}
async function prepararCadastro(raiz: ElementoTeste) {
  await estabilizar()
  await selecionar(raiz, 'arvore-especie', '7')
  mapaAtual().fire('click', { lngLat: { lng: -39.02, lat: -4.98 } })
  await selecionar(raiz, 'arvore-bairro', '2')
  const referencia = campo(raiz, 'arvore-referencia')
  referencia.value = 'Praça central'
  referencia.dispatch('input')
  await estabilizar()
  return referencia
}
function catalogoDeTeste() {
  vi.mocked(especieService.getAll).mockResolvedValue([
    {
      id: 7,
      nomePopular: 'Espécie de teste',
      nomeCientifico: 'Espécie de teste',
      categoria: 'TESTE',
      porteArvore: 'TESTE',
      recomendadaCalcadas: false,
    },
  ])
}
beforeEach(() => {
  execucao.mapas.length = 0
  execucao.marcadores.length = 0
  fichaDeEdicao.bairro = null
  vi.stubEnv('VITE_MAPTILER_API_KEY', 'chave-exclusiva-de-teste')
  vi.stubGlobal('document', { activeElement: null, body: new ElementoTeste('BODY') })
  vi.stubGlobal('Document', class {})
  vi.stubGlobal('ShadowRoot', class {})
  vi.spyOn(especieService, 'getAll').mockResolvedValue([])
  vi.spyOn(inventarioService, 'getBairros').mockResolvedValue([
    { id: 5, nome: 'Bôto', ativo: true },
    { id: 99, nome: 'Inativo para teste', ativo: false },
    { id: 2, nome: 'Centro', ativo: true },
  ])
  navegacao.push.mockReset()
  vi.stubGlobal(
    'fetch',
    vi.fn().mockResolvedValue({ ok: true, status: 200, json: async () => ({ features: [] }) }),
  )
})
afterEach(() => {
  apps.splice(0).forEach((app) => app.unmount())
  vi.restoreAllMocks()
  vi.unstubAllGlobals()
  vi.unstubAllEnvs()
})

describe('Mapa e formulário reais integrados sem WebGL', () => {
  it('apresenta um único seletor nativo com bairros ativos em ordem alfabética', async () => {
    const raiz = montar()
    await estabilizar()

    expect(() => campo(raiz, 'bairro-pesquisa')).toThrow()
    expect(campo(raiz, 'arvore-bairro').options.map((option) => option.textContent.trim())).toEqual([
      'Selecione o bairro',
      'Bôto',
      'Centro',
    ])
    expect(campo(raiz, 'arvore-bairro').props.required).toBe('')
  })

  it('remove o erro territorial depois que um bairro ativo é selecionado', async () => {
    catalogoDeTeste()
    const raiz = montar()
    await estabilizar()
    await selecionar(raiz, 'arvore-especie', '7')
    mapaAtual().fire('click', { lngLat: { lng: -39.02, lat: -4.98 } })
    await encontrar(raiz, (no) => no.tagName === 'FORM').dispatch('submit')
    await estabilizar()

    expect(campo(raiz, 'arvore-bairro').props['aria-invalid']).toBe(true)
    expect(raiz.textContent).toContain('Selecione um bairro ativo no catálogo.')

    await selecionar(raiz, 'arvore-bairro', '2')
    expect(campo(raiz, 'arvore-bairro').props['aria-invalid']).toBe(false)
    expect(raiz.textContent).not.toContain('Selecione um bairro ativo no catálogo.')
  })

  it('carrega a associação ativa de uma árvore existente no seletor de edição', async () => {
    catalogoDeTeste()
    fichaDeEdicao.bairro = { id: 2, nome: 'Centro', ativo: true }
    const raiz = montar(true)
    await estabilizar()
    const atualizar = vi.spyOn(inventarioService, 'update').mockResolvedValue({
      ...arvore,
      bairro: fichaDeEdicao.bairro,
    })
    await encontrar(raiz, (no) => no.tagName === 'FORM').dispatch('submit')
    await estabilizar()

    expect(atualizar).toHaveBeenCalledWith(42, expect.objectContaining({ bairroId: 2 }))
  })

  it('o primeiro clique cria um marcador projetável e atualiza os outputs do formulário', async () => {
    const raiz = montar()
    await estabilizar()
    mapaAtual().fire('click', { lngLat: { lng: -39.02, lat: -4.98 } })
    await estabilizar()
    expect(coordenadas(raiz)).toEqual(['-4.98', '-39.02'])
    const posicao = (execucao.marcadores[0] as Marker).getLngLat()
    expect(posicao.lng).toBeCloseTo(-39.02)
    expect(posicao.lat).toBeCloseTo(-4.98)
  })

  it.each(['click', 'keydown'])(
    'a busca por %s aplica uma resposta 200 ao mapa, marcador e formulário',
    async (evento) => {
      const raiz = montar()
      await estabilizar()
      vi.mocked(fetch).mockResolvedValueOnce({
        ok: true,
        status: 200,
        json: async () => ({
          features: [{ center: [-39.03, -4.99], place_name: 'Endereço de teste' }],
        }),
      } as Response)
      const input = encontrar(raiz, (no) => no.props.id === 'arvore-busca-endereco')
      input.value = 'Rua de teste'
      input.dispatch('input')
      await nextTick()
      if (evento === 'keydown') await input.dispatch('keydown', { key: 'Enter' })
      else
        await encontrar(
          raiz,
          (no) => no.tagName === 'BUTTON' && no.textContent.trim() === 'Buscar',
        ).dispatch('click')
      await estabilizar()
      expect(mapaAtual().flyTo).toHaveBeenCalledWith({ center: [-39.03, -4.99], zoom: 16 })
      expect(coordenadas(raiz)).toEqual(['-4.99', '-39.03'])
      expect(raiz.textContent).toContain(
        'Endereço localizado. Clique no mapa para ajustar o ponto.',
      )
      expect(raiz.textContent).not.toContain('Não foi possível pesquisar')
      const requisicao = new URL(String(vi.mocked(fetch).mock.calls[0]![0]))
      expect(decodeURIComponent(requisicao.pathname)).toContain(
        'Rua de teste, Quixadá, Ceará, Brasil',
      )
      expect(requisicao.searchParams.has('key')).toBe(true)
    },
  )

  it('captura coordenadas enquanto o reverse geocoding permanece pendente', async () => {
    vi.mocked(fetch).mockReturnValue(new Promise(() => undefined))
    const raiz = montar()
    await estabilizar()
    mapaAtual().fire('click', { lngLat: { lng: -39.04, lat: -4.96 } })
    await nextTick()
    expect(coordenadas(raiz)).toEqual(['-4.96', '-39.04'])
  })

  it('pré-seleciona somente bairro ativo com correspondência única e envia o ID do catálogo', async () => {
    catalogoDeTeste()
    const getBairros = vi.spyOn(inventarioService, 'getBairros')
    const raiz = montar()
    await estabilizar()
    await selecionar(raiz, 'arvore-especie', '7')
    let resolver!: (response: Response) => void
    vi.mocked(fetch).mockReturnValueOnce(
      new Promise((resolve) => {
        resolver = resolve
      }),
    )

    mapaAtual().fire('click', { lngLat: { lng: -39.02, lat: -4.98 } })
    await nextTick()
    expect(coordenadas(raiz)).toEqual(['-4.98', '-39.02'])
    resolver({
      ok: true,
      status: 200,
      json: async () => ({
        features: [
          { id: 'neighbourhood.1', place_type: ['neighbourhood'], text: ' centro ' },
          { id: 'locality.1', place_type: ['locality'], text: 'Quixadá' },
          { id: 'postcode.1', place_type: ['postcode'], text: '63900-000' },
          { id: 'road.1', place_type: ['road'], text: 'Rua das Árvores' },
        ],
      }),
    } as Response)
    await estabilizar()
    expect(campo(raiz, 'arvore-bairro').options.map((option) => option.textContent.trim())).toEqual([
      'Selecione o bairro',
      'Bôto',
      'Centro',
    ])
    expect(campo(raiz, 'arvore-cep').value).toBe('63900-000')
    expect(campo(raiz, 'arvore-referencia').value).toBe('Rua das Árvores')
    expect(raiz.textContent).toContain('Sugestão do mapa')

    const criar = vi.spyOn(inventarioService, 'create').mockResolvedValue(arvore)
    await encontrar(raiz, (no) => no.tagName === 'FORM').dispatch('submit')
    await estabilizar()
    expect(criar).toHaveBeenCalledWith(
      expect.objectContaining({
        zonaRural: false,
        bairroId: 2,
        bairroNome: null,
        cep: '63900-000',
        referenciaLocalizacao: 'Rua das Árvores',
      }),
    )
    expect(getBairros).toHaveBeenCalledOnce()
  })

  it('não deixa resposta reversa atrasada sobrescrever bairro, CEP, referência ou estado editados', async () => {
    let resolver!: (response: Response) => void
    vi.mocked(fetch).mockReturnValueOnce(
      new Promise((resolve) => {
        resolver = resolve
      }),
    )
    const raiz = montar()
    await estabilizar()
    mapaAtual().fire('click', { lngLat: { lng: -39.02, lat: -4.98 } })
    await nextTick()
    expect(coordenadas(raiz)).toEqual(['-4.98', '-39.02'])
    await selecionar(raiz, 'arvore-bairro', '2')
    for (const [id, valor] of [
      ['arvore-cep', '00123-000'],
      ['arvore-referencia', 'Referência digitada'],
    ]) {
      const input = campo(raiz, id)
      input.value = valor
      input.dispatch('input')
    }
    resolver({
      ok: true,
      status: 200,
      json: async () => ({
        features: [
          { id: 'neighbourhood.1', place_type: ['neighbourhood'], text: 'Sugestão tardia' },
          { id: 'postcode.1', place_type: ['postcode'], text: '63999-999' },
          { id: 'road.1', place_type: ['road'], text: 'Rua tardia' },
        ],
      }),
    } as Response)
    await estabilizar()
    expect(campo(raiz, 'arvore-bairro').value).toBe('2')
    expect(campo(raiz, 'arvore-cep').value).toBe('00123-000')
    expect(campo(raiz, 'arvore-referencia').value).toBe('Referência digitada')
    expect(campo(raiz, 'arvore-zona-rural').checked).toBe(false)
  })

  it('alternar para rural exige localidade e não altera as coordenadas', async () => {
    const raiz = montar()
    await estabilizar()
    mapaAtual().fire('click', { lngLat: { lng: -39.02, lat: -4.98 } })
    await estabilizar()
    const rural = campo(raiz, 'arvore-zona-rural')
    rural.checked = true
    rural.dispatch('change')
    await estabilizar()
    expect(campo(raiz, 'arvore-localidade-rural')).toBeTruthy()
    expect(() => campo(raiz, 'arvore-bairro')).toThrow()
    await encontrar(raiz, (no) => no.tagName === 'FORM').dispatch('submit')
    await estabilizar()
    expect(raiz.textContent).toContain('Informe a localidade rural.')
    const localidade = campo(raiz, 'arvore-localidade-rural')
    localidade.value = 'Sítio Boa Vista'
    localidade.dispatch('input')
    await estabilizar()
    expect(coordenadas(raiz)).toEqual(['-4.98', '-39.02'])
    expect(campo(raiz, 'arvore-cep').value).toBe('')
    expect(campo(raiz, 'arvore-zona-rural').checked).toBe(true)
  })

  it('não converte resultado de geocodificação em classificação fora da área urbana', async () => {
    let resolver!: (response: Response) => void
    vi.mocked(fetch).mockReturnValueOnce(
      new Promise((resolve) => {
        resolver = resolve
      }),
    )
    const raiz = montar()
    await estabilizar()
    mapaAtual().fire('click', { lngLat: { lng: -39.02, lat: -4.98 } })
    await nextTick()
    const rural = campo(raiz, 'arvore-zona-rural')
    rural.checked = true
    rural.dispatch('change')
    await nextTick()
    campo(raiz, 'arvore-localidade-rural').value = 'Comunidade de teste'
    campo(raiz, 'arvore-localidade-rural').dispatch('input')
    resolver({
      ok: true,
      status: 200,
      json: async () => ({
        features: [
          { id: 'neighbourhood.1', place_type: ['neighbourhood'], text: 'Bairro sugerido' },
          { id: 'postcode.1', place_type: ['postcode'], text: '63900-000' },
        ],
      }),
    } as Response)
    await estabilizar()
    expect(campo(raiz, 'arvore-zona-rural').checked).toBe(true)
    expect(campo(raiz, 'arvore-localidade-rural').value).toBe('Comunidade de teste')
    expect(campo(raiz, 'arvore-cep').value).toBe('63900-000')
  })

  it('permite selecionar sem chave e não faz geocodificação', async () => {
    vi.stubEnv('VITE_MAPTILER_API_KEY', '')
    const raiz = montar()
    await estabilizar()
    mapaAtual().fire('click', { lngLat: { lng: -39.04, lat: -4.96 } })
    await estabilizar()
    expect(coordenadas(raiz)).toEqual(['-4.96', '-39.04'])
    expect(fetch).not.toHaveBeenCalled()
  })

  it('preserva o marcador e a seleção depois de zoom, movimento e troca de base', async () => {
    const raiz = montar()
    await estabilizar()
    const mapa = mapaAtual()
    mapa.fire('click', { lngLat: { lng: -39.02, lat: -4.98 } })
    mapa.center = [-39.04, -4.96]
    mapa.zoom = 18
    mapa.fire('move', { type: 'move' })
    await encontrar(
      raiz,
      (no) => no.tagName === 'BUTTON' && no.textContent.trim() === 'Mapa de ruas',
    ).dispatch('click')
    mapa.fire('click', { lngLat: { lng: -39.05, lat: -4.95 } })
    await estabilizar()
    expect(execucao.mapas).toHaveLength(1)
    expect(execucao.marcadores).toHaveLength(1)
    expect(coordenadas(raiz)).toEqual(['-4.95', '-39.05'])
  })

  it('inicializa a edição com marcador e permite corrigir a coordenada', async () => {
    const raiz = montar(true)
    await estabilizar()
    expect(coordenadas(raiz)).toEqual(['-4.97', '-39.015'])
    expect(execucao.marcadores).toHaveLength(1)
    mapaAtual().fire('click', { lngLat: { lng: -39.02, lat: -4.98 } })
    await estabilizar()
    expect(coordenadas(raiz)).toEqual(['-4.98', '-39.02'])
    expect(execucao.marcadores).toHaveLength(1)
  })

  it.each([401, 403, 429, 500])(
    'preserva a seleção anterior e mostra falha quando a pesquisa retorna %s',
    async (status) => {
      const raiz = montar()
      await estabilizar()
      mapaAtual().fire('click', { lngLat: { lng: -39.02, lat: -4.98 } })
      await estabilizar()
      vi.mocked(fetch).mockResolvedValueOnce({ ok: false, status } as Response)
      const input = encontrar(raiz, (no) => no.props.id === 'arvore-busca-endereco')
      input.value = 'Rua de teste'
      input.dispatch('input')
      await nextTick()
      await encontrar(
        raiz,
        (no) => no.tagName === 'BUTTON' && no.textContent.trim() === 'Buscar',
      ).dispatch('click')
      await estabilizar()
      expect(coordenadas(raiz)).toEqual(['-4.98', '-39.02'])
      expect(mapaAtual().flyTo).not.toHaveBeenCalled()
      expect(raiz.textContent).toContain('Não foi possível pesquisar o endereço')
    },
  )

  it('informa ausência de resultado sem criar marcador', async () => {
    const raiz = montar()
    await estabilizar()
    const input = encontrar(raiz, (no) => no.props.id === 'arvore-busca-endereco')
    input.value = 'Rua de teste'
    input.dispatch('input')
    await nextTick()
    await input.dispatch('keydown', { key: 'Enter' })
    await estabilizar()
    expect(execucao.marcadores).toHaveLength(0)
    expect(raiz.textContent).toContain('Nenhum endereço correspondente')
  })

  it('remove marcador e listeners ao sair da tela', async () => {
    montar()
    await estabilizar()
    const mapa = mapaAtual()
    mapa.fire('click', { lngLat: { lng: -39.02, lat: -4.98 } })
    const elemento = (execucao.marcadores[0] as Marker).getElement() as unknown as ElementoTeste
    expect(elemento.parent).not.toBeNull()
    apps.pop()!.unmount()
    expect(mapa.listeners.size).toBe(0)
    expect(elemento.parent).toBeNull()
  })

  it('registra etapas de desenvolvimento sem chave, endereço ou coordenadas', async () => {
    vi.stubEnv('DEV', true)
    vi.stubGlobal('sessionStorage', { getItem: () => '1' })
    const log = vi.spyOn(console, 'info').mockImplementation(() => undefined)
    const raiz = montar()
    await estabilizar()
    mapaAtual().fire('click', { lngLat: { lng: -39.02, lat: -4.98 } })
    await estabilizar()
    const etapas = log.mock.calls.map((args) => args[1])
    expect(etapas).toEqual(
      expect.arrayContaining([
        'mapa.listeners',
        'mapa.clique',
        'marcador.posicionado',
        'formulario.latitude',
        'formulario.longitude',
        'coordenadas.emitidas',
        'props.sincronizadas',
      ]),
    )
    expect(coordenadas(raiz)).toEqual(['-4.98', '-39.02'])
    vi.mocked(fetch).mockResolvedValueOnce({ ok: false, status: 403 } as Response)
    const input = encontrar(raiz, (no) => no.props.id === 'arvore-busca-endereco')
    input.value = 'Endereço reservado de teste'
    input.dispatch('input')
    await nextTick()
    await input.dispatch('keydown', { key: 'Enter' })
    await estabilizar()
    expect(log).toHaveBeenCalledWith('[Arborize:mapa]', 'busca.resposta', { status: 403 })
    const registro = JSON.stringify(log.mock.calls)
    expect(registro).not.toContain('chave-exclusiva-de-teste')
    expect(registro).not.toContain('api.maptiler.com')
    expect(registro).not.toContain('-39.02')
    expect(registro).not.toContain('-4.98')
    expect(registro).not.toContain('Endereço reservado de teste')
  })

  it('não produz diagnósticos em produção, mesmo com opção de sessão ativa', () => {
    vi.stubEnv('DEV', false)
    vi.stubGlobal('sessionStorage', { getItem: () => '1' })
    const log = vi.spyOn(console, 'info').mockImplementation(() => undefined)
    diagnosticarMapa('mapa.clique')
    expect(log).not.toHaveBeenCalled()
  })

  it('não produz diagnósticos sem ativação explícita', () => {
    vi.stubEnv('DEV', true)
    vi.stubGlobal('sessionStorage', { getItem: () => null })
    const log = vi.spyOn(console, 'info').mockImplementation(() => undefined)
    diagnosticarMapa('mapa.clique')
    expect(log).not.toHaveBeenCalled()
  })

  it('carrega a foto somente da árvore selecionada e revoga a anterior ao trocar seleção', async () => {
    const documento = document as unknown as Record<string, unknown>
    vi.stubGlobal('document', {
      ...documento,
      createElement: (nome: string) => new ElementoTeste(nome.toUpperCase()),
    })
    vi.stubGlobal('window', {
      document,
      setTimeout: vi.fn(() => 1),
      clearTimeout: vi.fn(),
      devicePixelRatio: 1,
    })
    const recuperar = vi
      .spyOn(inventarioService, 'recuperarFoto')
      .mockResolvedValue(new Blob(['foto']))
    vi.spyOn(URL, 'createObjectURL').mockImplementation(() => 'blob:foto-selecionada')
    vi.spyOn(URL, 'revokeObjectURL').mockImplementation(() => undefined)
    const registros = [
      {
        ...arvore,
        id: 42,
        codigo: 'ARV-QXD-000042',
        latitude: -4.97,
        longitude: -39.01,
        fotoCapa: { id: 8, contentType: 'image/jpeg' as const },
      },
      {
        ...arvore,
        id: 43,
        codigo: 'ARV-QXD-000043',
        latitude: -4.98,
        longitude: -39.02,
        fotoCapa: { id: 9, contentType: 'image/png' as const },
      },
      {
        ...arvore,
        id: 44,
        codigo: 'ARV-QXD-000044',
        latitude: -4.99,
        longitude: -39.03,
        fotoCapa: null,
      },
    ]
    const raiz = new ElementoTeste('ROOT')
    const app = renderer.createApp(InventarioMapa, { arvores: registros })
    app.mount(raiz)
    apps.push(app)
    await estabilizar()
    expect(recuperar).not.toHaveBeenCalled()

    const primeiroMarcador = execucao.marcadores[0] as Marker
    ;(primeiroMarcador.getElement() as unknown as ElementoTeste).dispatch('click')
    await estabilizar()
    expect(recuperar).toHaveBeenCalledExactlyOnceWith(42, 8, { suppressGlobalErrorToast: true })
    expect(raiz.textContent).toContain('ARV-QXD-000042')

    const segundoMarcador = execucao.marcadores[1] as Marker
    ;(segundoMarcador.getElement() as unknown as ElementoTeste).dispatch('click')
    await estabilizar()
    expect(recuperar).toHaveBeenCalledTimes(2)
    expect(recuperar).toHaveBeenLastCalledWith(43, 9, { suppressGlobalErrorToast: true })
    expect(URL.revokeObjectURL).toHaveBeenCalledWith('blob:foto-selecionada')
    expect(raiz.textContent).toContain('ARV-QXD-000043')

    const terceiroMarcador = execucao.marcadores[2] as Marker
    ;(terceiroMarcador.getElement() as unknown as ElementoTeste).dispatch('click')
    await estabilizar()
    expect(recuperar).toHaveBeenCalledTimes(2)
    expect(raiz.textContent).toContain('Sem fotografia')
  })

  it('amplia a mesma instância, aplica o ponto e retorna ao formulário com foco restaurado', async () => {
    const salvar = vi.spyOn(inventarioService, 'create')
    const raiz = montar()
    await estabilizar()
    const mapa = mapaAtual()
    mapa.center = [-39.04, -4.96]
    mapa.zoom = 17
    document.body.style.overflow = 'auto'
    await encontrar(raiz, (no) => no.props['aria-label'] === 'Ampliar mapa').dispatch('click')
    await estabilizar()
    const dialogo = encontrar(raiz, (no) => no.tagName === 'DIALOG')
    expect(dialogo.open).toBe(true)
    expect(document.body.style.overflow).toBe('hidden')
    expect(document.activeElement).toBe(campo(raiz, 'arvore-busca-endereco'))
    expect(mapa.resize).toHaveBeenCalledTimes(1)
    expect(execucao.mapas).toHaveLength(1)
    expect(mapa.center).toEqual([-39.04, -4.96])
    expect(mapa.zoom).toBe(17)

    mapa.fire('click', { lngLat: { lng: -39.06, lat: -4.94 } })
    await estabilizar()
    expect(coordenadas(raiz)).toEqual(['-4.94', '-39.06'])
    await encontrar(
      raiz,
      (no) => no.tagName === 'BUTTON' && no.textContent.trim() === 'Usar localização',
    ).dispatch('click')
    await estabilizar()
    expect(dialogo.open).toBe(false)
    expect(document.body.style.overflow).toBe('auto')
    expect(document.activeElement).toBe(
      encontrar(raiz, (no) => no.props['aria-label'] === 'Ampliar mapa'),
    )
    expect(mapa.resize).toHaveBeenCalledTimes(2)
    expect(execucao.marcadores).toHaveLength(1)
    expect(salvar).not.toHaveBeenCalled()

    mapa.fire('click', { lngLat: { lng: -39.07, lat: -4.93 } })
    await estabilizar()
    expect(coordenadas(raiz)).toEqual(['-4.93', '-39.07'])
  })

  it.each(['click', 'keydown'])(
    'pesquisa por %s no mapa ampliado e mantém o ponto após confirmar',
    async (evento) => {
      const raiz = montar()
      await estabilizar()
      await encontrar(raiz, (no) => no.props['aria-label'] === 'Ampliar mapa').dispatch('click')
      await estabilizar()
      vi.mocked(fetch).mockResolvedValueOnce({
        ok: true,
        status: 200,
        json: async () => ({ features: [{ center: [-39.03, -4.99] }] }),
      } as Response)
      const input = campo(raiz, 'arvore-busca-endereco')
      input.value = 'Endereço de teste'
      input.dispatch('input')
      await nextTick()
      if (evento === 'keydown') await input.dispatch('keydown', { key: 'Enter' })
      else
        await encontrar(
          raiz,
          (no) => no.tagName === 'BUTTON' && no.textContent.trim() === 'Buscar',
        ).dispatch('click')
      await estabilizar()
      expect(coordenadas(raiz)).toEqual(['-4.99', '-39.03'])
      mapaAtual().fire('click', { lngLat: { lng: -39.031, lat: -4.991 } })
      await estabilizar()
      await encontrar(
        raiz,
        (no) => no.tagName === 'BUTTON' && no.textContent.trim() === 'Usar localização',
      ).dispatch('click')
      await estabilizar()
      expect(coordenadas(raiz)).toEqual(['-4.991', '-39.031'])
      expect(execucao.mapas).toHaveLength(1)
    },
  )

  it('fecha pelo cancelamento nativo sem perder o ponto e restaura rolagem ao desmontar', async () => {
    const raiz = montar()
    await estabilizar()
    document.body.style.overflow = 'scroll'
    await encontrar(raiz, (no) => no.props['aria-label'] === 'Ampliar mapa').dispatch('click')
    await estabilizar()
    const dialogo = encontrar(raiz, (no) => no.tagName === 'DIALOG')
    expect(
      encontrar(
        raiz,
        (no) => no.tagName === 'BUTTON' && no.textContent.trim() === 'Usar localização',
      ).props.disabled,
    ).toBe(true)
    mapaAtual().fire('click', { lngLat: { lng: -39.02, lat: -4.98 } })
    await dialogo.dispatch('cancel')
    await estabilizar()
    expect(dialogo.open).toBe(false)
    expect(coordenadas(raiz)).toEqual(['-4.98', '-39.02'])
    expect(document.body.style.overflow).toBe('scroll')
    await encontrar(raiz, (no) => no.props['aria-label'] === 'Ampliar mapa').dispatch('click')
    await estabilizar()
    apps.pop()!.unmount()
    expect(document.body.style.overflow).toBe('scroll')
  })

  it('rola e foca a espécie no primeiro envio inválido sem disparar toast ou requisição', async () => {
    const criar = vi.spyOn(inventarioService, 'create')
    const raiz = montar()
    await estabilizar()
    const toast = vi.spyOn(useFeedbackStore(), 'showToast')
    await encontrar(raiz, (no) => no.tagName === 'FORM').dispatch('submit')
    await estabilizar()
    const especie = campo(raiz, 'arvore-especie')
    expect(document.activeElement).toBe(especie)
    expect(especie.rolamentos).toHaveLength(1)
    expect(especie.props['aria-invalid']).toBe(true)
    expect(campo(raiz, 'especie-erro').textContent).toContain('Selecione uma espécie')
    expect(criar).not.toHaveBeenCalled()
    expect(toast).not.toHaveBeenCalled()
  })

  it('mantém Tab e Shift+Tab dentro dos controles do mapa ampliado', async () => {
    const raiz = montar()
    await estabilizar()
    await encontrar(raiz, (no) => no.props['aria-label'] === 'Ampliar mapa').dispatch('click')
    mapaAtual().fire('click', { lngLat: { lng: -39.02, lat: -4.98 } })
    await estabilizar()
    const dialogo = encontrar(raiz, (no) => no.tagName === 'DIALOG')
    const confirmar = encontrar(
      raiz,
      (no) => no.tagName === 'BUTTON' && no.textContent.trim() === 'Usar localização',
    )
    confirmar.focus()
    const impedir = vi.fn()
    dialogo.dispatch('keydown', { key: 'Tab', shiftKey: false, preventDefault: impedir })
    expect(document.activeElement).toBe(mapaAtual().canvas)
    dialogo.dispatch('keydown', { key: 'Tab', shiftKey: true, preventDefault: impedir })
    expect(document.activeElement).toBe(confirmar)
    expect(impedir).toHaveBeenCalledTimes(2)
  })

  it('agenda redimensionamento fora do observer, ignora tamanhos repetidos e cancela ao desmontar', async () => {
    let notificar!: (entradas: Array<{ contentRect: { width: number; height: number } }>) => void
    const desconectar = vi.fn()
    vi.stubGlobal(
      'ResizeObserver',
      class {
        constructor(callback: typeof notificar) {
          notificar = callback
        }
        observe() {}
        disconnect = desconectar
      },
    )
    let quadro: (() => void) | null = null
    const agendar = vi.fn((callback: () => void) => {
      quadro = callback
      return 1
    })
    const cancelar = vi.fn(() => {
      quadro = null
    })
    vi.stubGlobal('requestAnimationFrame', agendar)
    vi.stubGlobal('cancelAnimationFrame', cancelar)
    montar()
    await estabilizar()
    notificar([{ contentRect: { width: 600, height: 400 } }])
    expect(mapaAtual().resize).not.toHaveBeenCalled()
    quadro?.()
    expect(mapaAtual().resize).toHaveBeenCalledTimes(1)
    notificar([{ contentRect: { width: 600, height: 400 } }])
    expect(agendar).toHaveBeenCalledTimes(1)
    notificar([{ contentRect: { width: 700, height: 400 } }])
    expect(agendar).toHaveBeenCalledTimes(2)
    apps.pop()!.unmount()
    expect(cancelar).toHaveBeenCalledWith(1)
    expect(desconectar).toHaveBeenCalledOnce()
  })

  it('leva o foco ao canvas quando faltam coordenadas e remove o erro após o clique', async () => {
    catalogoDeTeste()
    const raiz = montar()
    await estabilizar()
    await selecionar(raiz, 'arvore-especie', '7')
    await encontrar(raiz, (no) => no.tagName === 'FORM').dispatch('submit')
    await estabilizar()
    expect(document.activeElement).toBe(mapaAtual().canvas)
    const regiao = encontrar(
      raiz,
      (no) => no.props['aria-label'] === 'Mapa auxiliar para selecionar coordenadas',
    )
    expect(regiao.rolamentos).toHaveLength(1)
    expect(regiao.props['aria-invalid']).toBe(true)
    expect(campo(raiz, 'arvore-localizacao-estado').textContent).toContain('Marque a localização')
    mapaAtual().fire('click', { lngLat: { lng: -39.02, lat: -4.98 } })
    await estabilizar()
    expect(regiao.props['aria-invalid']).toBe(false)
    expect(coordenadas(raiz)).toEqual(['-4.98', '-39.02'])
  })

  it('foca erros de medição no painel de dados e preserva os valores preenchidos', async () => {
    catalogoDeTeste()
    const raiz = montar()
    await estabilizar()
    await selecionar(raiz, 'arvore-especie', '7')
    mapaAtual().fire('click', { lngLat: { lng: -39.02, lat: -4.98 } })
    await selecionar(raiz, 'arvore-bairro', '2')
    const dap = campo(raiz, 'arvore-dap')
    dap.value = '-1'
    dap.dispatch('input')
    await encontrar(raiz, (no) => no.tagName === 'FORM').dispatch('submit')
    await estabilizar()
    expect(document.activeElement).toBe(dap)
    expect(dap.rolamentos).toHaveLength(1)
    expect(dap.props['aria-invalid']).toBe(true)
    expect(String(dap.value)).toBe('-1')
    expect(coordenadas(raiz)).toEqual(['-4.98', '-39.02'])
  })

  it('foca o campo indicado pelo ProblemDetail sem apagar o formulário', async () => {
    catalogoDeTeste()
    const raiz = montar()
    await estabilizar()
    await selecionar(raiz, 'arvore-especie', '7')
    mapaAtual().fire('click', { lngLat: { lng: -39.020123456, lat: -4.980987654 } })
    await selecionar(raiz, 'arvore-bairro', '2')
    const altura = campo(raiz, 'arvore-altura')
    altura.value = '3'
    altura.dispatch('input')
    const criar = vi.spyOn(inventarioService, 'create').mockRejectedValue(
      new InventarioApiError(
        400,
        {
          status: 400,
          type: 'about:blank',
          title: 'Entrada inválida',
          detail: 'Revise a altura.',
          errors: [{ field: 'alturaEstimadaM', message: 'Altura rejeitada para este registro.' }],
        },
        undefined,
      ),
    )
    await encontrar(raiz, (no) => no.tagName === 'FORM').dispatch('submit')
    await estabilizar()
    expect(document.activeElement).toBe(altura)
    expect(altura.props['aria-invalid']).toBe(true)
    expect(String(altura.value)).toBe('3')
    expect(raiz.textContent).toContain('Altura rejeitada')
    expect(coordenadas(raiz)).toEqual(['-4.980988', '-39.020123'])
    expect(criar).toHaveBeenCalledWith(
      expect.objectContaining({
        latitude: -4.980987654,
        longitude: -39.020123456,
      }),
    )
  })

  it('seleciona, pré-visualiza, remove e substitui arquivos sem desmontar o mapa ou limpar campos', async () => {
    vi.stubEnv('VITE_MAPTILER_API_KEY', '')
    vi.spyOn(URL, 'createObjectURL').mockImplementation(
      (arquivo) => `blob:${(arquivo as File).name}`,
    )
    vi.spyOn(URL, 'revokeObjectURL').mockImplementation(() => undefined)
    const raiz = montar()
    const referencia = await prepararCadastro(raiz)
    const mapa = mapaAtual()
    const original = new File(['imagem'], 'original.jpg', { type: 'image/jpeg' })
    selecionarArquivos(raiz, [original])
    await estabilizar()

    const painelDados = encontrar(raiz, (no) =>
      String(no.props.class).includes('xl:overflow-y-auto'),
    )
    expect(String(painelDados.props.class)).toContain('flex-none')
    expect(String(painelDados.props.class)).toContain('xl:flex-1')
    expect(raiz.textContent).toContain('original.jpg')
    expect(encontrar(raiz, (no) => no.tagName === 'IMG').props.src).toBe('blob:original.jpg')
    expect(referencia.value).toBe('Praça central')
    expect(coordenadas(raiz)).toEqual(['-4.98', '-39.02'])
    expect(mapaAtual()).toBe(mapa)
    expect(execucao.mapas).toHaveLength(1)

    await encontrar(
      raiz,
      (no) =>
        no.tagName === 'BUTTON' && no.props['aria-label'] === 'Remover original.jpg da seleção',
    ).dispatch('click')
    await estabilizar()
    expect(URL.revokeObjectURL).toHaveBeenCalledWith('blob:original.jpg')
    expect(raiz.textContent).not.toContain('original.jpg')

    const substituta = new File(['imagem nova'], 'substituta.webp', { type: 'image/webp' })
    selecionarArquivos(raiz, [substituta])
    await estabilizar()
    expect(raiz.textContent).toContain('substituta.webp')
    expect(referencia.value).toBe('Praça central')
    expect(coordenadas(raiz)).toEqual(['-4.98', '-39.02'])
    expect(mapaAtual()).toBe(mapa)
  })

  it('mantém o formulário visível e utilizável quando o arquivo é inválido ou excede o tamanho', async () => {
    const criarBlob = vi.spyOn(URL, 'createObjectURL').mockImplementation(() => 'blob:valida')
    vi.spyOn(URL, 'revokeObjectURL').mockImplementation(() => undefined)
    const raiz = montar()
    const referencia = await prepararCadastro(raiz)
    const mapa = mapaAtual()
    const invalida = new File(['imagem'], 'imagem.gif', { type: 'image/gif' })
    selecionarArquivos(raiz, [invalida])
    await estabilizar()
    expect(raiz.textContent).toContain('use uma imagem JPG, PNG ou WebP válida')
    expect(campo(raiz, 'arvore-especie')).toBeTruthy()
    expect(referencia.value).toBe('Praça central')
    expect(coordenadas(raiz)).toEqual(['-4.98', '-39.02'])
    expect(mapaAtual()).toBe(mapa)

    const grande = new File([new Uint8Array(2 * 1024 * 1024 + 1)], 'grande.png', {
      type: 'image/png',
    })
    selecionarArquivos(raiz, [grande])
    await estabilizar()
    expect(raiz.textContent).toContain('o limite é 2 MB')
    expect(campo(raiz, 'arvore-referencia').value).toBe('Praça central')
    expect(mapaAtual()).toBe(mapa)

    const arquivos = [1, 2, 3, 4].map(
      (numero) => new File([String(numero)], `foto-${numero}.jpg`, { type: 'image/jpeg' }),
    )
    selecionarArquivos(raiz, arquivos)
    await estabilizar()
    expect(raiz.textContent).toContain('3 de 3 selecionadas')
    expect(raiz.textContent).toContain('Cada árvore pode possuir até 3 fotografias')
    expect(campo(raiz, 'arvore-referencia').value).toBe('Praça central')
    expect(mapaAtual()).toBe(mapa)

    await encontrar(
      raiz,
      (no) => no.tagName === 'BUTTON' && no.props['aria-label'] === 'Remover foto-1.jpg da seleção',
    ).dispatch('click')
    await estabilizar()

    criarBlob.mockImplementation(() => {
      throw new TypeError('Blob URL indisponível')
    })
    selecionarArquivos(raiz, [new File(['imagem'], 'sem-preview.png', { type: 'image/png' })])
    await estabilizar()
    expect(raiz.textContent).toContain('não foi possível preparar a pré-visualização')
    expect(raiz.textContent).toContain('2 de 3 selecionadas')
    expect(campo(raiz, 'arvore-especie')).toBeTruthy()
    expect(mapaAtual()).toBe(mapa)
  })

  it('cria a árvore antes de enviar as fotografias e navega após confirmar todos os uploads', async () => {
    vi.stubEnv('VITE_MAPTILER_API_KEY', '')
    vi.spyOn(URL, 'createObjectURL').mockImplementation(() => 'blob:foto')
    vi.spyOn(URL, 'revokeObjectURL').mockImplementation(() => undefined)
    const ordem: string[] = []
    const criar = vi.spyOn(inventarioService, 'create').mockImplementation(async () => {
      ordem.push('criar')
      return arvore
    })
    const enviar = vi
      .spyOn(inventarioService, 'adicionarFoto')
      .mockImplementation(async (id, arquivo) => {
        ordem.push(`foto-${arquivo.name}`)
        return {
          id: 101,
          arvoreId: id,
          contentType: 'image/jpeg',
          sizeBytes: arquivo.size,
          criadoEm: '2026-11-01T12:00:00',
        }
      })
    catalogoDeTeste()
    const raiz = montar()
    await prepararCadastro(raiz)
    const arquivo = new File(['imagem'], 'rua.jpg', { type: 'image/jpeg' })
    selecionarArquivos(raiz, [arquivo])
    await estabilizar()
    expect(criar).not.toHaveBeenCalled()
    expect(enviar).not.toHaveBeenCalled()

    await encontrar(raiz, (no) => no.tagName === 'FORM').dispatch('submit')
    await estabilizar()

    expect(ordem).toEqual(['criar', 'foto-rua.jpg'])
    expect(criar).toHaveBeenCalledOnce()
    expect(enviar).toHaveBeenCalledExactlyOnceWith(42, arquivo)
    expect(navegacao.push).toHaveBeenCalledWith({ name: 'inventario-detalhes', params: { id: 42 } })
  })

  it('preserva somente uploads pendentes após falha parcial e não duplica a árvore na nova tentativa', async () => {
    vi.stubEnv('VITE_MAPTILER_API_KEY', '')
    vi.spyOn(URL, 'createObjectURL').mockImplementation(
      (arquivo) => `blob:${(arquivo as File).name}`,
    )
    vi.spyOn(URL, 'revokeObjectURL').mockImplementation(() => undefined)
    const criar = vi.spyOn(inventarioService, 'create').mockResolvedValue(arvore)
    const fotoUm = new File(['um'], 'um.jpg', { type: 'image/jpeg' })
    const fotoDois = new File(['dois'], 'dois.png', { type: 'image/png' })
    const enviar = vi
      .spyOn(inventarioService, 'adicionarFoto')
      .mockResolvedValueOnce({
        id: 101,
        arvoreId: 42,
        contentType: 'image/jpeg',
        sizeBytes: fotoUm.size,
        criadoEm: '2026-11-01T12:00:00',
      })
      .mockRejectedValueOnce(new Error('Falha de rede'))
      .mockResolvedValueOnce({
        id: 102,
        arvoreId: 42,
        contentType: 'image/png',
        sizeBytes: fotoDois.size,
        criadoEm: '2026-11-01T12:01:00',
      })
    catalogoDeTeste()
    const raiz = montar()
    await prepararCadastro(raiz)
    const input = selecionarArquivos(raiz, [fotoUm, fotoDois])
    await estabilizar()
    await encontrar(raiz, (no) => no.tagName === 'FORM').dispatch('submit')
    await estabilizar()

    expect(criar).toHaveBeenCalledOnce()
    expect(enviar).toHaveBeenCalledTimes(2)
    expect(enviar).toHaveBeenNthCalledWith(1, 42, fotoUm)
    expect(enviar).toHaveBeenNthCalledWith(2, 42, fotoDois)
    expect(raiz.textContent).toContain('A árvore 42 foi cadastrada')
    expect(raiz.textContent).toContain('Tente novamente sem cadastrar a árvore outra vez')
    expect(raiz.textContent).toContain('dois.png')
    expect(raiz.textContent).not.toContain('um.jpg')
    expect(navegacao.push).not.toHaveBeenCalled()
    expect(encontrar(raiz, (no) => no.tagName === 'FIELDSET').props.disabled).toBe(true)
    expect(input.props.disabled).toBe(false)

    await encontrar(raiz, (no) => no.tagName === 'FORM').dispatch('submit')
    await estabilizar()
    expect(criar).toHaveBeenCalledOnce()
    expect(enviar).toHaveBeenCalledTimes(3)
    expect(enviar).toHaveBeenNthCalledWith(3, 42, fotoDois)
    expect(navegacao.push).toHaveBeenCalledWith({ name: 'inventario-detalhes', params: { id: 42 } })
  })
})
