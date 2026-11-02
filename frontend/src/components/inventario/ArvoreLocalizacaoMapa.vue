<script setup lang="ts">
import { Map, Marker, NavigationControl, setWorkerUrl } from 'maplibre-gl'
import type { MapMouseEvent } from 'maplibre-gl'
import 'maplibre-gl/dist/maplibre-gl.css'
import workerUrl from 'maplibre-gl/dist/maplibre-gl-worker.mjs?worker&url'
import {
  computed,
  nextTick,
  onBeforeUnmount,
  onMounted,
  ref,
  shallowRef,
  useTemplateRef,
  watch,
} from 'vue'
import { Maximize2, Search, X } from 'lucide-vue-next'
import {
  CENTRO_INICIAL,
  ZOOM_INICIAL,
  criarEstiloMapa,
  obterEstiloHibrido,
  trocarEstiloPreservandoCamera,
} from './inventarioMapaConfig'
import { aplicarSelecaoDeCoordenadas, criarManipuladorCliqueMapa } from './arvoreLocalizacaoSelecao'
import { diagnosticarMapa } from './inventarioMapaDiagnostico'

setWorkerUrl(workerUrl)

const props = withDefaults(
  defineProps<{
    latitude: number | null
    longitude: number | null
    centroInicial?: [longitude: number, latitude: number]
    zoomInicial?: number
    erroLocalizacao?: string
  }>(),
  { zoomInicial: ZOOM_INICIAL },
)
const emit = defineEmits<{
  'update:latitude': [latitude: number]
  'update:longitude': [longitude: number]
  'localizacao-identificacao-iniciada': []
  'localizacao-identificada': [
    localizacao: {
      logradouro: string | null
      bairro: string | null
      localidade: string | null
      cep: string | null
    },
  ]
}>()
const container = useTemplateRef<HTMLDivElement>('mapaContainer')
const dialogo = useTemplateRef<HTMLDialogElement>('dialogo')
const campoBusca = useTemplateRef<HTMLInputElement>('campoBusca')
const botaoExpandir = useTemplateRef<HTMLButtonElement>('botaoExpandir')
const expandido = ref(false)
let overflowAnterior: string | null = null
let observadorTamanho: ResizeObserver | null = null
let quadroRedimensionamento: number | null = null
let tamanhoAnterior = { largura: 0, altura: 0 }
const endereco = ref('')
const buscandoEndereco = ref(false)
const identificandoLocalizacao = ref(false)
const mapaIndisponivel = ref(false)
const mensagemBusca = ref('')
const mensagemReverseGeocoding = ref('')
const erroBusca = ref(false)
const estiloHibridoDisponivel = computed(() => obterEstiloHibrido() !== null)
const baseCartografica = ref<'hibrido' | 'ruas'>(estiloHibridoDisponivel.value ? 'hibrido' : 'ruas')
const chaveMapTiler = computed(() => {
  const chave: unknown = import.meta.env.VITE_MAPTILER_API_KEY
  return typeof chave === 'string' ? chave.trim() : ''
})
const mapa = shallowRef<Map | null>(null)
const marker = shallowRef<Marker | null>(null)
let requisicaoBusca: AbortController | null = null
let requisicaoReverseGeocoding: AbortController | null = null
const pontoSelecionado = computed(() => coordenadaValida(props.latitude, props.longitude))
const estadoErro = computed(
  () => Boolean(props.erroLocalizacao) || mapaIndisponivel.value || erroBusca.value,
)
const mensagemEstado = computed(() => {
  if (props.erroLocalizacao) return props.erroLocalizacao
  if (mapaIndisponivel.value) return 'Não foi possível carregar o mapa. Tente o mapa de ruas.'
  if (buscandoEndereco.value) return 'Buscando endereço…'
  if (mensagemBusca.value) return mensagemBusca.value
  if (identificandoLocalizacao.value) return 'Identificando rua e bairro…'
  if (mensagemReverseGeocoding.value) return mensagemReverseGeocoding.value
  if (!chaveMapTiler.value)
    return 'Busca e satélite indisponíveis. Selecione o ponto no mapa de ruas.'
  return pontoSelecionado.value
    ? 'Ponto selecionado. Clique para ajustar.'
    : 'Clique no mapa para marcar a árvore.'
})

function restaurarRolagem() {
  if (overflowAnterior !== null) {
    document.body.style.overflow = overflowAnterior
    overflowAnterior = null
  }
}

async function abrirMapaExpandido() {
  if (expandido.value || !dialogo.value) return
  expandido.value = true
  await nextTick()
  if (!dialogo.value || !expandido.value) return
  dialogo.value.showModal()
  overflowAnterior = document.body.style.overflow
  document.body.style.overflow = 'hidden'
  mapa.value?.resize()
  campoBusca.value?.focus()
}

async function fecharMapaExpandido() {
  dialogo.value?.close()
  expandido.value = false
  restaurarRolagem()
  await nextTick()
  mapa.value?.resize()
  botaoExpandir.value?.focus()
}

function manterFocoNoDialogo(evento: KeyboardEvent) {
  if (evento.key !== 'Tab' || !expandido.value || !dialogo.value) return
  const controles = Array.from(
    dialogo.value.querySelectorAll<HTMLElement>(
      'button:not([disabled]), input:not([disabled]), a[href], [tabindex="0"]',
    ),
  ).filter((elemento) => elemento.getClientRects().length > 0)
  const primeiro = controles[0]
  const ultimo = controles.at(-1)
  if (evento.shiftKey && document.activeElement === primeiro) {
    evento.preventDefault()
    ultimo?.focus()
  } else if (!evento.shiftKey && document.activeElement === ultimo) {
    evento.preventDefault()
    primeiro?.focus()
  }
}

function focarMapa() {
  container.value?.scrollIntoView({ behavior: 'smooth', block: 'center' })
  const alvo = mapa.value?.getCanvas() ?? container.value
  alvo?.focus({ preventScroll: true })
}

defineExpose({ focarMapa })

type FeicaoGeocodificacao = {
  id?: unknown
  text?: unknown
  place_name?: unknown
  place_type?: unknown
  address?: unknown
  context?: unknown
  properties?: unknown
}

function comoRegistro(valor: unknown): Record<string, unknown> | null {
  return typeof valor === 'object' && valor !== null ? (valor as Record<string, unknown>) : null
}

function textoDaFeicao(feicao: FeicaoGeocodificacao | null | undefined): string | null {
  const propriedades = comoRegistro(feicao?.properties)
  const valores = [feicao?.text, propriedades?.name, feicao?.place_name]
  return (
    valores.find((valor): valor is string => typeof valor === 'string' && valor.trim() !== '') ??
    null
  )
}

function tiposDaFeicao(feicao: FeicaoGeocodificacao): string[] {
  const propriedades = comoRegistro(feicao.properties)
  const placeTypes = Array.isArray(feicao.place_type) ? feicao.place_type : []
  const id = typeof feicao.id === 'string' ? feicao.id.split('.')[0] : ''
  const kind = typeof propriedades?.kind === 'string' ? propriedades.kind : ''
  return [...placeTypes, id, kind].filter((tipo): tipo is string => typeof tipo === 'string')
}

function temTipo(feicao: FeicaoGeocodificacao, tipos: string[]): boolean {
  const tiposDaResposta = tiposDaFeicao(feicao).map((tipo) => tipo.toLocaleLowerCase('pt-BR'))
  return tipos.some((tipo) => tiposDaResposta.includes(tipo))
}

function extrairLocalizacao(dados: unknown): {
  logradouro: string | null
  bairro: string | null
  localidade: string | null
  cep: string | null
} {
  const resposta = comoRegistro(dados)
  const feicoes = Array.isArray(resposta?.features)
    ? (resposta.features.map(comoRegistro).filter(Boolean) as FeicaoGeocodificacao[])
    : []
  const contextos = feicoes.flatMap((feicao) =>
    Array.isArray(feicao.context)
      ? (feicao.context.map(comoRegistro).filter(Boolean) as FeicaoGeocodificacao[])
      : [],
  )
  const feicaoBairro = [...feicoes, ...contextos].find((feicao) =>
    temTipo(feicao, ['neighbourhood', 'neighborhood']),
  )
  const feicaoLocalidade = [...feicoes, ...contextos].find((feicao) =>
    temTipo(feicao, ['locality', 'place', 'district']),
  )
  const feicaoCep = [...feicoes, ...contextos].find((feicao) =>
    temTipo(feicao, ['postcode', 'postal_code']),
  )
  const feicaoRua = [...feicoes, ...contextos].find((feicao) => temTipo(feicao, ['road']))
  const feicaoEndereco = feicoes.find((feicao) => temTipo(feicao, ['address']))
  const nomeRua = textoDaFeicao(feicaoRua)
  const nomeEndereco = textoDaFeicao(feicaoEndereco)
  const propriedadesEndereco = comoRegistro(feicaoEndereco?.properties)
  const numeroEndereco =
    (typeof feicaoEndereco?.address === 'string' && feicaoEndereco.address) ||
    (typeof propriedadesEndereco?.address === 'string' && propriedadesEndereco.address) ||
    ''
  const logradouro =
    nomeRua ??
    (nomeEndereco ? `${nomeEndereco}${numeroEndereco ? `, ${numeroEndereco}` : ''}` : null)

  return {
    logradouro,
    bairro: textoDaFeicao(feicaoBairro),
    localidade: textoDaFeicao(feicaoLocalidade),
    cep: textoDaFeicao(feicaoCep),
  }
}

function coordenadaValida(latitude: number | null, longitude: number | null): boolean {
  return (
    latitude !== null &&
    longitude !== null &&
    Number.isFinite(latitude) &&
    Number.isFinite(longitude) &&
    latitude >= -90 &&
    latitude <= 90 &&
    longitude >= -180 &&
    longitude <= 180
  )
}

function sincronizarMarcador() {
  diagnosticarMapa('props.sincronizadas', {
    coordenadasValidas: coordenadaValida(props.latitude, props.longitude),
  })
  if (!mapa.value) return
  if (!coordenadaValida(props.latitude, props.longitude)) {
    marker.value?.remove()
    marker.value = null
    return
  }
  posicionarMarcador(props.latitude!, props.longitude!)
}

function posicionarMarcador(latitude: number, longitude: number) {
  diagnosticarMapa('marcador.inicio', {
    mapaDisponivel: mapa.value !== null,
    marcadorExistente: marker.value !== null,
    coordenadasValidas: coordenadaValida(latitude, longitude),
  })
  if (!mapa.value || !coordenadaValida(latitude, longitude)) return
  if (!marker.value) {
    marker.value = new Marker({ color: '#f97316' })
      .setLngLat([longitude, latitude])
      .addTo(mapa.value)
    marker.value.getElement().setAttribute('aria-label', 'Localização selecionada para a árvore')
    marker.value.getElement().setAttribute('role', 'img')
    marker.value.getElement().setAttribute('title', 'Localização selecionada para a árvore')
    marker.value.getElement().style.pointerEvents = 'none'
  }
  marker.value.setLngLat([longitude, latitude])
  diagnosticarMapa('marcador.posicionado')
}

function selecionarCoordenadas(latitude: number, longitude: number) {
  aplicarSelecaoDeCoordenadas(latitude, longitude, {
    posicionarMarcador,
    atualizarLatitude: (valor) => emit('update:latitude', valor),
    atualizarLongitude: (valor) => emit('update:longitude', valor),
  })
  diagnosticarMapa('coordenadas.emitidas')
  void identificarLocalizacao(latitude, longitude)
}

const selecionarAoClique = criarManipuladorCliqueMapa(selecionarCoordenadas)
function aoClicarNoMapa(evento: MapMouseEvent) {
  diagnosticarMapa('mapa.clique')
  mensagemBusca.value = ''
  erroBusca.value = false
  selecionarAoClique(evento)
}

function diagnosticarCliqueDom(evento: MouseEvent) {
  const limites = container.value?.getBoundingClientRect()
  if (
    !limites ||
    evento.clientX < limites.left ||
    evento.clientX > limites.right ||
    evento.clientY < limites.top ||
    evento.clientY > limites.bottom
  )
    return
  diagnosticarMapa('dom.clique', { alvoCanvas: evento.target === mapa.value?.getCanvas() })
}

async function identificarLocalizacao(latitude: number, longitude: number) {
  requisicaoReverseGeocoding?.abort()
  mensagemReverseGeocoding.value = ''
  emit('localizacao-identificacao-iniciada')

  if (!chaveMapTiler.value) {
    mensagemReverseGeocoding.value = 'Ponto selecionado. Informe uma referência, se necessário.'
    return
  }

  const controle = new AbortController()
  requisicaoReverseGeocoding = controle
  identificandoLocalizacao.value = true

  try {
    const parametros = new URLSearchParams({ key: chaveMapTiler.value, language: 'pt' })
    const url = `https://api.maptiler.com/geocoding/${longitude},${latitude}.json?${parametros}`
    const resposta = await fetch(url, { signal: controle.signal })
    diagnosticarMapa('reversa.resposta', { status: resposta.status })
    if (!resposta.ok) throw new Error(`HTTP ${resposta.status}`)
    const localizacao = extrairLocalizacao(await resposta.json())
    if (requisicaoReverseGeocoding !== controle) return
    emit('localizacao-identificada', localizacao)
    mensagemReverseGeocoding.value =
      localizacao.bairro || localizacao.logradouro || localizacao.cep
        ? 'Sugestões de localização disponíveis. Confira os dados.'
        : 'Ponto selecionado. Informe uma referência, se necessário.'
  } catch (erro) {
    if (erro instanceof Error && erro.name === 'AbortError') return
    if (requisicaoReverseGeocoding !== controle) return
    diagnosticarMapa('reversa.falhou')
    mensagemReverseGeocoding.value = 'Não foi possível identificar rua e bairro neste ponto.'
  } finally {
    if (requisicaoReverseGeocoding === controle) {
      requisicaoReverseGeocoding = null
      identificandoLocalizacao.value = false
    }
  }
}

function aoErroMapa(evento: { error?: unknown }) {
  const erro = evento.error
  const status =
    typeof erro === 'object' && erro !== null && 'status' in erro && typeof erro.status === 'number'
      ? erro.status
      : undefined
  diagnosticarMapa('mapa.erro', { status })
  mapaIndisponivel.value = true
  container.value?.setAttribute('data-mapa-indisponivel', 'true')
}

function selecionarBase(base: 'hibrido' | 'ruas') {
  if (!mapa.value || base === baseCartografica.value) return
  const estilo = base === 'hibrido' ? obterEstiloHibrido() : criarEstiloMapa()
  if (!estilo) return

  try {
    trocarEstiloPreservandoCamera(mapa.value, estilo)
    baseCartografica.value = base
    mapaIndisponivel.value = false
  } catch {
    mapaIndisponivel.value = true
  }
}

async function buscarEndereco() {
  const consulta = endereco.value.trim()
  diagnosticarMapa('busca.acionada', {
    consultaPreenchida: consulta.length > 0,
    chaveConfigurada: chaveMapTiler.value.length > 0,
    mapaDisponivel: mapa.value !== null,
  })
  mensagemBusca.value = ''
  erroBusca.value = false

  if (!consulta) {
    diagnosticarMapa('busca.bloqueada')
    erroBusca.value = true
    mensagemBusca.value = 'Informe um endereço para pesquisar.'
    return
  }
  if (!chaveMapTiler.value) {
    diagnosticarMapa('busca.bloqueada')
    erroBusca.value = true
    mensagemBusca.value =
      'A busca de endereços não está disponível. Escolha o local diretamente no mapa.'
    return
  }
  if (!mapa.value) {
    diagnosticarMapa('busca.bloqueada')
    erroBusca.value = true
    mensagemBusca.value = 'O mapa não está disponível para centralizar o endereço pesquisado.'
    return
  }

  requisicaoBusca?.abort()
  const controle = new AbortController()
  requisicaoBusca = controle
  buscandoEndereco.value = true
  let fase: 'requisicao' | 'resposta' | 'centralizacao' | 'selecao' = 'requisicao'

  try {
    const query = encodeURIComponent(`${consulta}, Quixadá, Ceará, Brasil`)
    const url = `https://api.maptiler.com/geocoding/${query}.json?key=${encodeURIComponent(chaveMapTiler.value)}`
    diagnosticarMapa('busca.requisicao')
    const resposta = await fetch(url, { signal: controle.signal })
    fase = 'resposta'
    diagnosticarMapa('busca.resposta', { status: resposta.status })
    if (!resposta.ok) throw new Error(`HTTP ${resposta.status}`)

    const dados: unknown = await resposta.json()
    const features =
      typeof dados === 'object' &&
      dados !== null &&
      'features' in dados &&
      Array.isArray(dados.features)
        ? dados.features
        : []
    const resultado = features[0] as
      { center?: unknown; place_name?: string; text?: string } | undefined
    const centro = resultado?.center
    diagnosticarMapa('busca.resultado', {
      resultados: features.length,
      coordenadasValidas: Array.isArray(centro) && coordenadaValida(centro[1], centro[0]),
    })
    if (
      !Array.isArray(centro) ||
      centro.length < 2 ||
      !Number.isFinite(centro[0]) ||
      !Number.isFinite(centro[1]) ||
      !coordenadaValida(centro[1], centro[0])
    ) {
      mensagemBusca.value = 'Nenhum endereço correspondente foi encontrado.'
      return
    }

    if (!resultado) {
      mensagemBusca.value = 'Nenhum endereço correspondente foi encontrado.'
      return
    }

    if (!mapa.value) return
    const [longitude, latitude] = centro as [number, number]
    fase = 'centralizacao'
    mapa.value.flyTo({ center: [longitude, latitude], zoom: 16 })
    diagnosticarMapa('busca.centralizada')
    fase = 'selecao'
    selecionarCoordenadas(latitude, longitude)
    diagnosticarMapa('busca.concluida')
    mensagemBusca.value = 'Endereço localizado. Clique no mapa para ajustar o ponto.'
  } catch (erro) {
    if (erro instanceof Error && erro.name === 'AbortError') return
    diagnosticarMapa('busca.falhou', { fase })
    erroBusca.value = true
    mensagemBusca.value =
      'Não foi possível pesquisar o endereço. Verifique a conexão ou escolha o local diretamente no mapa.'
  } finally {
    if (requisicaoBusca === controle) {
      requisicaoBusca = null
      buscandoEndereco.value = false
    }
  }
}

onMounted(() => {
  if (!container.value) return
  try {
    mapa.value = new Map({
      container: container.value,
      style: obterEstiloHibrido() ?? criarEstiloMapa(),
      center: props.centroInicial ?? CENTRO_INICIAL,
      zoom: props.zoomInicial,
      maxZoom: 19,
      renderWorldCopies: false,
      attributionControl: { compact: true },
      trackResize: true,
      locale: {
        'Map.Title': 'Selecionar localização da árvore',
        'NavigationControl.ZoomIn': 'Aproximar',
        'NavigationControl.ZoomOut': 'Afastar',
        'NavigationControl.ResetBearing': 'Orientar para o norte',
      },
    })
    mapa.value.addControl(new NavigationControl({ showCompass: false }), 'bottom-right')
    diagnosticarMapa('mapa.inicializado', { chaveConfigurada: chaveMapTiler.value.length > 0 })
    mapa.value.on('click', aoClicarNoMapa)
    mapa.value.on('error', aoErroMapa)
    diagnosticarMapa('mapa.listeners', { cliqueRegistrado: mapa.value.listens('click') })
    if (import.meta.env.DEV && typeof window !== 'undefined') {
      window.addEventListener('click', diagnosticarCliqueDom, true)
    }
    sincronizarMarcador()
    if (typeof ResizeObserver !== 'undefined') {
      observadorTamanho = new ResizeObserver(([entrada]) => {
        if (!entrada) return
        const { width: largura, height: altura } = entrada.contentRect
        if (largura === tamanhoAnterior.largura && altura === tamanhoAnterior.altura) return
        tamanhoAnterior = { largura, altura }
        if (quadroRedimensionamento !== null) cancelAnimationFrame(quadroRedimensionamento)
        quadroRedimensionamento = requestAnimationFrame(() => {
          quadroRedimensionamento = null
          mapa.value?.resize()
        })
      })
      observadorTamanho.observe(container.value)
    }
  } catch {
    diagnosticarMapa('mapa.erro')
    marker.value?.remove()
    marker.value = null
    mapa.value?.remove()
    mapa.value = null
    mapaIndisponivel.value = true
    container.value.setAttribute('data-mapa-indisponivel', 'true')
  }
})

watch(() => [props.latitude, props.longitude], sincronizarMarcador)
watch(
  () => props.erroLocalizacao,
  (erro) => {
    const canvas = mapa.value?.getCanvas()
    if (erro) canvas?.setAttribute('aria-describedby', 'arvore-localizacao-estado')
    else canvas?.removeAttribute('aria-describedby')
  },
)

onBeforeUnmount(() => {
  dialogo.value?.close()
  restaurarRolagem()
  observadorTamanho?.disconnect()
  if (quadroRedimensionamento !== null) cancelAnimationFrame(quadroRedimensionamento)
  if (import.meta.env.DEV && typeof window !== 'undefined') {
    window.removeEventListener('click', diagnosticarCliqueDom, true)
  }
  diagnosticarMapa('mapa.descartado')
  requisicaoBusca?.abort()
  requisicaoReverseGeocoding?.abort()
  marker.value?.remove()
  marker.value = null
  if (mapa.value) {
    mapa.value.off('click', aoClicarNoMapa)
    mapa.value.off('error', aoErroMapa)
    mapa.value.remove()
    mapa.value = null
  }
})
</script>

<template>
  <div class="h-full min-h-0">
    <dialog
      ref="dialogo"
      aria-label="Selecionar localização da árvore em mapa ampliado"
      class="fixed inset-0 m-auto h-dvh max-h-none w-screen max-w-none overflow-hidden overscroll-contain border-0 bg-white p-3 text-grey-text shadow-xl backdrop:bg-black/40 backdrop:backdrop-blur-xs sm:h-[calc(100dvh-2rem)] sm:w-[calc(100vw-2rem)] sm:rounded-box sm:border sm:border-amma-green-light sm:p-5"
      @cancel.prevent="fecharMapaExpandido"
      @keydown.capture="manterFocoNoDialogo"
    />
    <Teleport :to="dialogo ?? 'body'" :disabled="!expandido">
      <section
        aria-label="Selecionar coordenadas no mapa"
        class="flex h-full min-h-0 flex-col gap-2"
      >
        <div
          class="relative flex-1 overflow-hidden rounded-btn border bg-amma-green-lighter focus-within:ring-2 focus-within:ring-amma-green"
          :class="[
            expandido ? 'min-h-0' : 'min-h-[22rem]',
            erroLocalizacao
              ? 'border-feedback-danger ring-2 ring-feedback-danger'
              : 'border-amma-green-light',
          ]"
        >
          <div
            ref="mapaContainer"
            role="region"
            aria-label="Mapa auxiliar para selecionar coordenadas"
            aria-describedby="arvore-localizacao-estado"
            :aria-invalid="Boolean(erroLocalizacao)"
            tabindex="-1"
            class="absolute inset-0 h-full w-full"
          />
          <div class="pointer-events-none absolute inset-x-3 top-3 z-10 flex flex-col gap-2">
            <div
              role="search"
              class="pointer-events-auto flex min-w-0 gap-1 rounded-btn border border-amma-green-light bg-white p-1 shadow-md"
            >
              <label for="arvore-busca-endereco" class="sr-only">Buscar endereço</label>
              <input
                id="arvore-busca-endereco"
                ref="campoBusca"
                v-model="endereco"
                type="search"
                autocomplete="street-address"
                placeholder="Buscar endereço em Quixadá"
                class="min-h-11 min-w-0 flex-1 rounded-btn bg-white px-3 py-2 text-sm text-grey-text outline-none placeholder:text-grey-text/70 focus-visible:ring-2 focus-visible:ring-amma-green"
                :disabled="buscandoEndereco"
                aria-describedby="arvore-localizacao-estado"
                @keydown.enter.prevent="buscarEndereco"
              />
              <button
                type="button"
                class="flex min-h-11 shrink-0 items-center gap-1.5 rounded-btn border border-amma-green-light px-3 py-2 text-sm font-semibold text-amma-green-dark transition-colors hover:bg-amma-green-lighter focus-visible:outline-2 focus-visible:outline-amma-green disabled:cursor-not-allowed disabled:opacity-60"
                :disabled="buscandoEndereco || !chaveMapTiler"
                @click="buscarEndereco"
              >
                <Search :size="16" aria-hidden="true" />
                {{ buscandoEndereco ? 'Buscando…' : 'Buscar' }}
              </button>
            </div>
            <div class="flex flex-wrap items-start justify-between gap-2">
              <div
                role="group"
                aria-label="Base cartográfica"
                class="pointer-events-auto flex rounded-btn border border-amma-green-light bg-white p-1 shadow-sm"
              >
                <button
                  type="button"
                  class="min-h-9 rounded-btn px-3 py-1.5 text-xs font-semibold transition-colors focus-visible:outline-2 focus-visible:outline-amma-green"
                  :class="
                    baseCartografica === 'ruas'
                      ? 'bg-amma-green-lighter text-amma-green-dark'
                      : 'text-grey-text hover:bg-amma-green-lighter'
                  "
                  :aria-pressed="baseCartografica === 'ruas'"
                  @click="selecionarBase('ruas')"
                >
                  Mapa de ruas
                </button>
                <button
                  type="button"
                  class="min-h-9 rounded-btn px-3 py-1.5 text-xs font-semibold transition-colors focus-visible:outline-2 focus-visible:outline-amma-green disabled:cursor-not-allowed disabled:opacity-50"
                  :class="
                    baseCartografica === 'hibrido'
                      ? 'bg-amma-green-lighter text-amma-green-dark'
                      : 'text-grey-text hover:bg-amma-green-lighter'
                  "
                  :aria-pressed="baseCartografica === 'hibrido'"
                  :disabled="!estiloHibridoDisponivel"
                  :title="
                    !estiloHibridoDisponivel ? 'Visualização por satélite indisponível' : undefined
                  "
                  @click="selecionarBase('hibrido')"
                >
                  Satélite
                </button>
              </div>
              <button
                v-if="!expandido"
                ref="botaoExpandir"
                type="button"
                aria-label="Ampliar mapa"
                title="Ampliar mapa"
                class="pointer-events-auto flex min-h-11 items-center gap-2 rounded-btn border border-amma-green-light bg-white px-3 text-xs font-semibold text-amma-green-dark shadow-sm hover:bg-amma-green-lighter focus-visible:outline-2 focus-visible:outline-amma-green"
                @click="abrirMapaExpandido"
              >
                <Maximize2 :size="16" aria-hidden="true" />
                <span class="hidden sm:inline">Ampliar</span>
              </button>
              <button
                v-else
                type="button"
                aria-label="Voltar ao formulário"
                title="Voltar ao formulário"
                class="pointer-events-auto flex h-11 w-11 shrink-0 items-center justify-center rounded-full border border-amma-green-light bg-white text-grey-text shadow-sm hover:bg-amma-green-lighter focus-visible:outline-2 focus-visible:outline-amma-green"
                @click="fecharMapaExpandido"
              >
                <X :size="20" aria-hidden="true" />
              </button>
            </div>
          </div>
        </div>
        <p
          id="arvore-localizacao-estado"
          :role="estadoErro ? 'alert' : 'status'"
          class="shrink-0 text-xs leading-5"
          :class="estadoErro ? 'font-semibold text-feedback-danger' : 'text-grey-text'"
        >
          {{ mensagemEstado }}
        </p>
        <footer
          v-if="expandido"
          class="flex shrink-0 items-center justify-end border-t border-amma-green-light pt-3"
        >
          <button
            type="button"
            class="min-h-11 rounded-btn bg-amma-green px-5 py-2 text-sm font-bold text-white hover:bg-amma-green-dark focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-amma-green disabled:cursor-not-allowed disabled:opacity-50"
            :disabled="!pontoSelecionado"
            @click="fecharMapaExpandido"
          >
            Usar localização
          </button>
        </footer>
      </section>
    </Teleport>
  </div>
</template>
