// @vitest-environment node
import { afterEach, describe, expect, it, vi } from 'vitest'
import {
  obterEstiloSatelite,
  obterEstiloHibrido,
  trocarEstiloPreservandoCamera,
  CENTRO_INICIAL,
  ZOOM_INICIAL,
  condicoesMapa,
  criarEstiloMapa,
  temCoordenadasValidas,
} from '@/components/inventario/inventarioMapaConfig'

afterEach(() => vi.unstubAllEnvs())

describe('configuração e coordenadas do mapa do inventário', () => {
  it.each([
    [0, 0],
    [-4.97, -39.015],
    [-90, -180],
    [90, 180],
  ])('aceita coordenadas globais válidas: %s, %s', (latitude, longitude) => {
    expect(temCoordenadasValidas({ latitude, longitude })).toBe(true)
  })

  it.each([
    [91, 0],
    [-91, 0],
    [0, 181],
    [0, -181],
    [NaN, 0],
    [0, NaN],
    [Infinity, 0],
    [0, -Infinity],
  ])('descarta coordenadas inválidas: %s, %s', (latitude, longitude) => {
    expect(temCoordenadasValidas({ latitude, longitude })).toBe(false)
  })

  it('não converte null ou strings em coordenadas zero', () => {
    expect(
      temCoordenadasValidas({ latitude: null, longitude: 0 } as unknown as {
        latitude: number
        longitude: number
      }),
    ).toBe(false)
    expect(
      temCoordenadasValidas({ latitude: 0, longitude: '0' } as unknown as {
        latitude: number
        longitude: number
      }),
    ).toBe(false)
  })

  it('usa as quatro condições reais, com símbolos e texto além das cores', () => {
    expect(Object.keys(condicoesMapa)).toEqual([
      'NAO_AVALIADA',
      'APARENTEMENTE_SAUDAVEL',
      'SINAIS_COMPROMETIMENTO',
      'MORTA',
    ])
    expect(new Set(Object.values(condicoesMapa).map((item) => item.simbolo)).size).toBe(4)
    expect(new Set(Object.values(condicoesMapa).map((item) => item.classe)).size).toBe(4)
    for (const item of Object.values(condicoesMapa)) expect(item.rotulo.length).toBeGreaterThan(0)
  })

  it('configura somente mapa base raster, com atribuição visível pelo controle', () => {
    vi.stubEnv('VITE_INVENTARIO_MAP_STYLE_URL', '')
    const estilo = criarEstiloMapa()
    if (typeof estilo === 'string') throw new Error('Estilo padrão deve ser local')
    expect(estilo.version).toBe(8)
    expect(estilo.sources['osm-base']).toMatchObject({
      type: 'raster',
      tileSize: 256,
      tiles: ['https://tile.openstreetmap.org/{z}/{x}/{y}.png'],
      attribution: expect.stringContaining('https://www.openstreetmap.org/copyright'),
    })
    expect(estilo.layers).toEqual([{ id: 'osm-base', type: 'raster', source: 'osm-base' }])
    expect(Object.keys(estilo.sources)).toEqual(['osm-base'])
    expect(CENTRO_INICIAL).toEqual([-39.015, -4.97])
    expect(ZOOM_INICIAL).toBe(13)
  })

  it('permite trocar o estilo sem alterar o componente ou a store', () => {
    vi.stubEnv('VITE_INVENTARIO_MAP_STYLE_URL', ' https://mapa.example.test/style.json ')
    expect(criarEstiloMapa()).toBe('https://mapa.example.test/style.json')
  })
})

describe('base satélite configurável', () => {
  it('não habilita satélite sem chave', () => {
    vi.stubEnv('VITE_MAPTILER_API_KEY', '')
    expect(obterEstiloSatelite()).toBeNull()
    expect(obterEstiloHibrido()).toBeNull()
    vi.stubEnv('VITE_MAPTILER_API_KEY', '   ')
    expect(obterEstiloSatelite()).toBeNull()
    expect(obterEstiloHibrido()).toBeNull()
  })

  it('usa satellite-v4 com a chave de ambiente codificada', () => {
    vi.stubEnv('VITE_MAPTILER_API_KEY', ' chave somente teste& ')
    expect(obterEstiloSatelite()).toBe(
      'https://api.maptiler.com/maps/satellite-v4/style.json?key=chave%20somente%20teste%26',
    )
  })

  it('usa o estilo híbrido MapTiler com a chave de ambiente codificada', () => {
    vi.stubEnv('VITE_MAPTILER_API_KEY', ' chave somente teste& ')
    expect(obterEstiloHibrido()).toBe(
      'https://api.maptiler.com/maps/hybrid/style.json?key=chave%20somente%20teste%26',
    )
  })

  it('preserva a câmera no momento da troca sem reconstruir a instância', () => {
    const setStyle = vi.fn()
    const mapa = {
      setStyle,
      getCenter: () => ({ toArray: () => [-39, -5] }),
      getZoom: () => 17,
      getBearing: () => 12,
      getPitch: () => 25,
    }
    trocarEstiloPreservandoCamera(
      mapa as unknown as Parameters<typeof trocarEstiloPreservandoCamera>[0],
      'https://example.test/style.json',
    )
    expect(setStyle).toHaveBeenCalledTimes(1)
    const options = setStyle.mock.calls[0]![1]
    const estilo = {
      version: 8,
      sources: { satellite: { attribution: 'Créditos do provedor' } },
      layers: [],
      center: [0, 0],
      zoom: 0,
    }
    expect(options.diff).toBe(false)
    expect(options.transformStyle(undefined, estilo)).toEqual({
      ...estilo,
      center: [-39, -5],
      zoom: 17,
      bearing: 12,
      pitch: 25,
    })
  })
})
