export interface EventoCliqueMapaArvore {
  lngLat: { lng: number; lat: number }
}

export interface AtualizadoresSelecaoArvore {
  posicionarMarcador: (latitude: number, longitude: number) => void
  atualizarLatitude: (latitude: number) => void
  atualizarLongitude: (longitude: number) => void
}

export function coordenadasDoClique(evento: EventoCliqueMapaArvore) {
  return { latitude: evento.lngLat.lat, longitude: evento.lngLat.lng }
}

export function criarManipuladorCliqueMapa(
  selecionar: (latitude: number, longitude: number) => void,
) {
  return (evento: EventoCliqueMapaArvore) => {
    const { latitude, longitude } = coordenadasDoClique(evento)
    selecionar(latitude, longitude)
  }
}

export function aplicarSelecaoDeCoordenadas(
  latitude: number,
  longitude: number,
  atualizadores: AtualizadoresSelecaoArvore,
) {
  atualizadores.posicionarMarcador(latitude, longitude)
  atualizadores.atualizarLatitude(latitude)
  atualizadores.atualizarLongitude(longitude)
}
