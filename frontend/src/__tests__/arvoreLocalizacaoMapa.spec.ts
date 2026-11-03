import { describe, expect, it, vi } from 'vitest'
import {
  aplicarSelecaoDeCoordenadas,
  coordenadasDoClique,
  criarManipuladorCliqueMapa,
} from '@/components/inventario/arvoreLocalizacaoSelecao'
import { trocarEstiloPreservandoCamera } from '@/components/inventario/inventarioMapaConfig'

describe('seleção de coordenadas no mapa de árvores', () => {
  it('converte o clique para latitude e longitude sem inverter os eixos', () => {
    expect(coordenadasDoClique({ lngLat: { lng: -39.123, lat: -4.876 } })).toEqual({
      latitude: -4.876,
      longitude: -39.123,
    })
  })

  it('posiciona o marcador e emite as coordenadas de forma síncrona', () => {
    const ordem: string[] = []
    const posicionarMarcador = vi.fn(() => ordem.push('marcador'))
    const atualizarLatitude = vi.fn(() => ordem.push('latitude'))
    const atualizarLongitude = vi.fn(() => ordem.push('longitude'))

    aplicarSelecaoDeCoordenadas(-4.876, -39.123, {
      posicionarMarcador,
      atualizarLatitude,
      atualizarLongitude,
    })

    expect(posicionarMarcador).toHaveBeenCalledWith(-4.876, -39.123)
    expect(atualizarLatitude).toHaveBeenCalledWith(-4.876)
    expect(atualizarLongitude).toHaveBeenCalledWith(-39.123)
    expect(ordem).toEqual(['marcador', 'latitude', 'longitude'])
  })

  it('mantém o handler de clique após movimentação, zoom e troca de estilo', () => {
    const coordenadas: Array<[number, number]> = []
    const listeners = new Map<string, (evento: { lngLat: { lng: number; lat: number } }) => void>()
    const camera = {
      center: [-39.02, -4.98] as [number, number],
      zoom: 16,
      bearing: 0,
      pitch: 0,
    }
    const mapa = {
      on: (evento: string, listener: (evento: { lngLat: { lng: number; lat: number } }) => void) =>
        listeners.set(evento, listener),
      off: (evento: string) => listeners.delete(evento),
      setStyle: vi.fn(),
      getCenter: () => ({ toArray: () => camera.center }),
      getZoom: () => camera.zoom,
      getBearing: () => camera.bearing,
      getPitch: () => camera.pitch,
    }
    const selecionar = (latitude: number, longitude: number) => {
      aplicarSelecaoDeCoordenadas(latitude, longitude, {
        posicionarMarcador: (lat, lng) => coordenadas.push([lng, lat]),
        atualizarLatitude: (lat) => coordenadas.push([0, lat]),
        atualizarLongitude: (lng) => coordenadas.push([lng, 0]),
      })
    }

    mapa.on('click', criarManipuladorCliqueMapa(selecionar))
    listeners.get('click')?.({ lngLat: { lng: -39.1, lat: -4.8 } })
    trocarEstiloPreservandoCamera(
      mapa as unknown as Parameters<typeof trocarEstiloPreservandoCamera>[0],
      'style-de-teste',
    )
    camera.center = [-39.2, -4.9]
    camera.zoom = 18
    listeners.get('click')?.({ lngLat: { lng: -39.3, lat: -4.7 } })

    expect(mapa.setStyle).toHaveBeenCalledTimes(1)
    expect(listeners.has('click')).toBe(true)
    expect(coordenadas).toEqual([
      [-39.1, -4.8],
      [0, -4.8],
      [-39.1, 0],
      [-39.3, -4.7],
      [0, -4.7],
      [-39.3, 0],
    ])
  })
})
