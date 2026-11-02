type EtapaMapa =
  | 'mapa.inicializado' | 'mapa.listeners' | 'mapa.erro' | 'mapa.descartado'
  | 'dom.clique' | 'mapa.clique' | 'marcador.inicio' | 'marcador.posicionado'
  | 'coordenadas.emitidas' | 'formulario.latitude' | 'formulario.longitude' | 'props.sincronizadas'
  | 'busca.acionada' | 'busca.bloqueada' | 'busca.requisicao' | 'busca.resposta'
  | 'busca.resultado' | 'busca.centralizada' | 'busca.concluida' | 'busca.falhou'
  | 'reversa.resposta' | 'reversa.falhou'

type DetalhesMapa = {
  chaveConfigurada?: boolean
  consultaPreenchida?: boolean
  mapaDisponivel?: boolean
  coordenadasValidas?: boolean
  marcadorExistente?: boolean
  alvoCanvas?: boolean
  cliqueRegistrado?: boolean
  status?: number
  resultados?: number
  fase?: 'requisicao' | 'resposta' | 'centralizacao' | 'selecao'
}

export function diagnosticarMapa(etapa: EtapaMapa, detalhes: DetalhesMapa = {}) {
  if (!import.meta.env.DEV) return
  try {
    if (globalThis.sessionStorage?.getItem('arborize:diagnostico-mapa') !== '1') return
    console.info('[Arborize:mapa]', etapa, detalhes)
  } catch {
    return
  }
}
