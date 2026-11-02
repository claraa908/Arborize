import type { StyleSpecification } from 'maplibre-gl'
import type { ArvoreResumoResponseDTO, CondicaoFitossanitaria } from '@/types/inventario'

// Referência aproximada para enquadramento de Quixadá, nunca um limite territorial.
export const CENTRO_INICIAL: [number, number] = [-39.015, -4.97]
export const ZOOM_INICIAL = 13

export const condicoesMapa: Record<
  CondicaoFitossanitaria,
  {
    rotulo: string
    simbolo: string
    classe: string
  }
> = {
  NAO_AVALIADA: { rotulo: 'Não avaliada', simbolo: '?', classe: 'condicao-nao-avaliada' },
  APARENTEMENTE_SAUDAVEL: {
    rotulo: 'Aparentemente saudável',
    simbolo: '✓',
    classe: 'condicao-saudavel',
  },
  SINAIS_COMPROMETIMENTO: {
    rotulo: 'Com sinais de comprometimento',
    simbolo: '!',
    classe: 'condicao-comprometida',
  },
  MORTA: { rotulo: 'Morta', simbolo: '×', classe: 'condicao-morta' },
}

export function temCoordenadasValidas(
  arvore: Pick<ArvoreResumoResponseDTO, 'latitude' | 'longitude'>,
): boolean {
  return (
    Number.isFinite(arvore.latitude) &&
    Number.isFinite(arvore.longitude) &&
    arvore.latitude >= -90 &&
    arvore.latitude <= 90 &&
    arvore.longitude >= -180 &&
    arvore.longitude <= 180
  )
}

export function criarEstiloMapa(): StyleSpecification | string {
  const configurado: unknown = import.meta.env.VITE_INVENTARIO_MAP_STYLE_URL
  if (typeof configurado === 'string' && configurado.trim()) return configurado.trim()
  // Uso interativo, sem prefetch/offline. Cache e Referer normais do navegador.
  // Política: https://operations.osmfoundation.org/policies/tiles/
  return {
    version: 8,
    sources: {
      'osm-base': {
        type: 'raster',
        tiles: ['https://tile.openstreetmap.org/{z}/{x}/{y}.png'],
        tileSize: 256,
        maxzoom: 19,
        attribution:
          '© <a href="https://www.openstreetmap.org/copyright" target="_blank" rel="noopener noreferrer">OpenStreetMap contributors</a>',
      },
    },
    layers: [{ id: 'osm-base', type: 'raster', source: 'osm-base' }],
  }
}

export function obterEstiloSatelite(): string | null {
  const chave: unknown = import.meta.env.VITE_MAPTILER_API_KEY
  if (typeof chave !== 'string' || !chave.trim()) return null
  return `https://api.maptiler.com/maps/satellite-v4/style.json?key=${encodeURIComponent(chave.trim())}`
}

export function obterEstiloHibrido(): string | null {
  const chave: unknown = import.meta.env.VITE_MAPTILER_API_KEY
  if (typeof chave !== 'string' || !chave.trim()) return null
  return `https://api.maptiler.com/maps/hybrid/style.json?key=${encodeURIComponent(chave.trim())}`
}

// A transformação é aplicada quando o novo estilo chega: preserva inclusive movimentos
// feitos enquanto ele carregava. Marcadores DOM e seleção não integram o estilo.
export function trocarEstiloPreservandoCamera(
  mapa: Pick<
    import('maplibre-gl').Map,
    'setStyle' | 'getCenter' | 'getZoom' | 'getBearing' | 'getPitch'
  >,
  estilo: StyleSpecification | string,
) {
  mapa.setStyle(estilo, {
    diff: false,
    transformStyle: (_anterior, proximo) => ({
      ...proximo,
      center: mapa.getCenter().toArray(),
      zoom: mapa.getZoom(),
      bearing: mapa.getBearing(),
      pitch: mapa.getPitch(),
    }),
  })
}
