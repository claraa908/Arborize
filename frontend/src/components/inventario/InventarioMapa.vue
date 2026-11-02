<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, useTemplateRef, watch } from 'vue'
import { Map as MapaLibre, Marker, NavigationControl, setWorkerUrl } from 'maplibre-gl'
import 'maplibre-gl/dist/maplibre-gl.css'
import workerUrl from 'maplibre-gl/dist/maplibre-gl-worker.mjs?worker&url'
import type { ArvoreResumoResponseDTO } from '@/types/inventario'
import ArvoreFotoMiniatura from './ArvoreFotoMiniatura.vue'
import {
  CENTRO_INICIAL,
  ZOOM_INICIAL,
  condicoesMapa,
  criarEstiloMapa,
  temCoordenadasValidas,
  obterEstiloSatelite,
  trocarEstiloPreservandoCamera,
} from './inventarioMapaConfig'

// O worker ESM da versão 6 precisa ser empacotado pelo Vite também em produção.
setWorkerUrl(workerUrl)

const props = withDefaults(
  defineProps<{
    arvores: readonly ArvoreResumoResponseDTO[]
    mensagemVazia?: string
    centroInicial?: [number, number]
    zoomInicial?: number
  }>(),
  {
    mensagemVazia: 'Nenhuma árvore carregada no mapa.',
    centroInicial: () => [...CENTRO_INICIAL],
    zoomInicial: ZOOM_INICIAL,
  },
)

const emit = defineEmits<{
  selecionar: [arvore: ArvoreResumoResponseDTO | null]
  abrirFicha: [id: number]
}>()
const container = useTemplateRef<HTMLDivElement>('mapaContainer')
const carregando = ref(true)
const erroMapa = ref(false)
const idSelecionado = defineModel<number | null>('selecionadaId', { default: null })
const baseAtual = ref<'mapa' | 'satelite'>('mapa')
const estiloSatelite = obterEstiloSatelite()
const tempoLimiteCarregamentoMs = 15000
const arvoresValidas = computed(() => props.arvores.filter(temCoordenadasValidas))
const selecionada = computed(
  () => arvoresValidas.value.find((arvore) => arvore.id === idSelecionado.value) ?? null,
)
// A instância WebGL não entra na reatividade do Vue.
let mapa: MapaLibre | null = null
let marcadores: { marker: Marker; button: HTMLButtonElement; click: () => void; id: number }[] = []
let timeoutCarregamento: number | undefined

function limparTimeoutCarregamento() {
  if (timeoutCarregamento !== undefined) {
    window.clearTimeout(timeoutCarregamento)
    timeoutCarregamento = undefined
  }
}

function iniciarTimeoutCarregamento() {
  limparTimeoutCarregamento()
  timeoutCarregamento = window.setTimeout(falhou, tempoLimiteCarregamentoMs)
}

function destacarSelecao() {
  for (const item of marcadores)
    item.button.setAttribute('aria-pressed', String(item.id === idSelecionado.value))
}

function selecionar(arvore: ArvoreResumoResponseDTO | null) {
  idSelecionado.value = arvore?.id ?? null
  destacarSelecao()
  emit('selecionar', arvore)
}

function removerMarcadores() {
  for (const item of marcadores) {
    item.button.removeEventListener('click', item.click)
    item.marker.remove()
  }
  marcadores = []
}

function atualizarMarcadores() {
  removerMarcadores()
  if (idSelecionado.value !== null && !selecionada.value) selecionar(null)
  if (!mapa) return
  for (const arvore of arvoresValidas.value) {
    const condicao = condicoesMapa[arvore.condicaoFitossanitaria]
    const button = document.createElement('button')
    button.type = 'button'
    button.className = `inventario-marcador ${condicao.classe}`
    button.textContent = condicao.simbolo
    button.setAttribute(
      'aria-label',
      `Árvore ${arvore.codigo}: ${arvore.especie.nomePopular}. ${condicao.rotulo}`,
    )
    button.setAttribute('aria-pressed', String(arvore.id === idSelecionado.value))
    button.title = `Árvore ${arvore.codigo} · ${condicao.rotulo}`
    const click = () => selecionar(arvore)
    button.addEventListener('click', click)
    const marker = new Marker({ element: button })
      .setLngLat([arvore.longitude, arvore.latitude])
      .addTo(mapa)
    marcadores.push({ marker, button, click, id: arvore.id })
  }
}

function localizarArvore(id: number) {
  const arvore = arvoresValidas.value.find((item) => item.id === id)
  if (!arvore || !mapa) return
  selecionar(arvore)
  mapa.jumpTo({ center: [arvore.longitude, arvore.latitude], zoom: Math.max(mapa.getZoom(), 16) })
  container.value?.scrollIntoView({ block: 'center' })
  mapa.getCanvas().focus({ preventScroll: true })
}

defineExpose({ localizarArvore })

function trocarBase(base: 'mapa' | 'satelite') {
  if (!mapa || base === baseAtual.value) return
  const estilo = base === 'satelite' ? estiloSatelite : criarEstiloMapa()
  if (!estilo) return
  erroMapa.value = false
  carregando.value = true
  iniciarTimeoutCarregamento()
  baseAtual.value = base
  try {
    trocarEstiloPreservandoCamera(mapa, estilo)
  } catch {
    falhou()
  }
}

function carregou() {
  limparTimeoutCarregamento()
  carregando.value = false
  erroMapa.value = false
}
function falhou() {
  limparTimeoutCarregamento()
  erroMapa.value = true
  carregando.value = false
}

function destruirMapa() {
  limparTimeoutCarregamento()
  removerMarcadores()
  if (mapa) {
    mapa.off('load', carregou)
    mapa.off('style.load', carregou)
    mapa.off('error', falhou)
    mapa.off('webglcontextlost', falhou)
    // remove também controles, listeners, ResizeObserver e contexto WebGL da biblioteca.
    mapa.remove()
    mapa = null
  }
}

function iniciarMapa() {
  destruirMapa()
  if (!container.value) return
  carregando.value = true
  erroMapa.value = false
  iniciarTimeoutCarregamento()
  try {
    mapa = new MapaLibre({
      container: container.value,
      style: baseAtual.value === 'satelite' && estiloSatelite ? estiloSatelite : criarEstiloMapa(),
      center: props.centroInicial,
      zoom: props.zoomInicial,
      maxZoom: 19,
      renderWorldCopies: false,
      attributionControl: { compact: false },
      // MapLibre 6 observa o próprio container, inclusive ao recolher a sidebar.
      trackResize: true,
      locale: {
        'Map.Title': 'Mapa do Inventário Arbóreo',
        'NavigationControl.ZoomIn': 'Aproximar',
        'NavigationControl.ZoomOut': 'Afastar',
        'NavigationControl.ResetBearing': 'Orientar para o norte',
      },
    })
    mapa.on('load', carregou)
    mapa.on('style.load', carregou)
    mapa.on('error', falhou)
    mapa.on('webglcontextlost', falhou)
    mapa.addControl(new NavigationControl({ showCompass: false }), 'top-right')
    atualizarMarcadores()
  } catch {
    destruirMapa()
    // Falta de WebGL ou falha de inicialização não impede o restante da página.
    container.value.replaceChildren()
    falhou()
  }
}

onMounted(iniciarMapa)
onBeforeUnmount(destruirMapa)
// Atualiza apenas os marcadores; preserva instância, zoom e posição da câmera.
watch(() => props.arvores, atualizarMarcadores, { deep: true })
watch(idSelecionado, destacarSelecao)
</script>

<template>
  <div class="inventario-mapa min-w-0">
    <div
      role="group"
      aria-label="Visualização do mapa"
      :aria-busy="carregando"
      class="mb-3 flex w-fit max-w-full gap-1 rounded-btn border border-amma-green-light bg-amma-green-lighter p-1"
    >
      <button
        type="button"
        :aria-pressed="baseAtual === 'mapa'"
        class="rounded-btn px-4 py-2 text-sm font-bold focus-visible:outline-2 focus-visible:outline-amma-green disabled:opacity-50"
        :class="baseAtual === 'mapa' ? 'bg-amma-green text-white' : 'text-amma-green-dark'"
        @click="trocarBase('mapa')"
      >
        Mapa
      </button>
      <button
        type="button"
        :aria-pressed="baseAtual === 'satelite'"
        :disabled="!estiloSatelite"
        :aria-describedby="!estiloSatelite ? 'satelite-configuracao' : undefined"
        class="rounded-btn px-4 py-2 text-sm font-bold focus-visible:outline-2 focus-visible:outline-amma-green disabled:cursor-not-allowed disabled:opacity-50"
        :class="baseAtual === 'satelite' ? 'bg-amma-green text-white' : 'text-amma-green-dark'"
        @click="trocarBase('satelite')"
      >
        Satélite
      </button>
    </div>
    <p v-if="!estiloSatelite" id="satelite-configuracao" class="mb-3 text-xs text-grey-text">
      A visualização por satélite precisa ser configurada antes do uso. O mapa de ruas continua disponível.
    </p>
    <div
      v-if="erroMapa"
      role="alert"
      class="mb-3 flex flex-col gap-3 rounded-btn border border-feedback-danger-border bg-feedback-danger-bg p-4 text-sm text-feedback-danger sm:flex-row sm:items-center sm:justify-between"
    >
      <p>
        Não foi possível carregar o mapa selecionado. Você pode tentar novamente ou voltar ao mapa
        de ruas.
      </p>
      <button
        type="button"
        class="shrink-0 rounded-btn border border-feedback-danger px-3 py-2 font-bold focus-visible:outline-2 focus-visible:outline-offset-2"
        @click="iniciarMapa"
      >
        Tentar novamente
      </button>
    </div>
    <p v-if="carregando" role="status" class="mb-3 text-sm text-grey-text">Carregando mapa…</p>
    <div class="relative">
      <div
        ref="mapaContainer"
        role="region"
        aria-label="Mapa interativo do Inventário Arbóreo"
        class="h-80 w-full overflow-hidden rounded-btn border border-amma-green-light bg-amma-green-lighter sm:h-96 lg:h-[440px]"
      />
      <a
        v-if="baseAtual === 'satelite'"
        href="https://www.maptiler.com/"
        target="_blank"
        rel="noopener noreferrer"
        class="absolute bottom-7 left-2 rounded bg-white/90 p-1"
        aria-label="MapTiler"
      >
        <img src="https://api.maptiler.com/resources/logo.svg" alt="MapTiler" class="h-5 w-auto" />
      </a>
    </div>
    <p v-if="arvores.length === 0" class="mt-3 text-sm text-grey-text">
      {{ mensagemVazia }}
    </p>
    <p
      v-if="arvoresValidas.length < arvores.length"
      role="status"
      class="mt-3 text-sm text-feedback-warning"
    >
      Há registros com coordenadas inválidas que não puderam ser exibidos.
    </p>
    <ul
      aria-label="Legenda de condições fitossanitárias"
      class="mt-4 flex flex-wrap gap-x-5 gap-y-3 text-xs text-grey-text"
    >
      <li v-for="(condicao, valor) in condicoesMapa" :key="valor" class="flex items-center gap-2">
        <span aria-hidden="true" class="inventario-legenda" :class="condicao.classe">{{
          condicao.simbolo
        }}</span>
        {{ condicao.rotulo }}
      </li>
    </ul>
    <section
      v-if="selecionada"
      aria-label="Árvore selecionada"
      aria-live="polite"
      class="mt-4 rounded-btn border border-amma-green-light bg-amma-green-lighter p-4"
    >
      <div class="flex items-start justify-between gap-3">
        <ArvoreFotoMiniatura
          :arvore-id="selecionada.id"
          :foto-capa="selecionada.fotoCapa"
          carregar-imediatamente
        />
        <div class="min-w-0 flex-1 break-words">
          <p class="text-xs font-bold uppercase tracking-wide text-grey-text">
            {{ selecionada.codigo }}
          </p>
          <h3 class="mt-1 font-bold text-amma-green-dark">{{ selecionada.especie.nomePopular }}</h3>
          <p class="text-sm italic text-grey-text">{{ selecionada.especie.nomeCientifico }}</p>
          <p class="mt-2 text-sm text-grey-text">
            {{ condicoesMapa[selecionada.condicaoFitossanitaria].rotulo }}
          </p>
          <p
            v-if="selecionada.zonaRural === true || selecionada.classificacaoTerritorial === 'FORA_DA_AREA_URBANA'"
            class="mt-1 text-xs text-grey-text"
          >
            Localidade rural: {{ selecionada.localidadeRural || 'Não informado' }}
          </p>
          <p
            v-else-if="selecionada.bairroNome || selecionada.bairro?.nome"
            class="mt-1 text-xs text-grey-text"
          >
            Bairro: {{ selecionada.bairroNome || selecionada.bairro?.nome }}
          </p>
        </div>
        <button
          type="button"
          aria-label="Fechar seleção da árvore"
          class="rounded-btn p-2 text-amma-green-dark hover:bg-amma-green-light focus-visible:outline-2"
          @click="selecionar(null)"
        >
          <span aria-hidden="true" class="material-symbols-outlined">close</span>
        </button>
      </div>
      <button
        type="button"
        class="mt-3 rounded-btn bg-amma-green px-4 py-2 text-sm font-bold text-white hover:bg-amma-green-dark focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-amma-green"
        @click="emit('abrirFicha', selecionada.id)"
      >
        Ver ficha técnica
      </button>
    </section>
  </div>
</template>

<style scoped>
/* Os marcadores são nós DOM da biblioteca, por isso recebem estilos via :deep. */
.inventario-legenda,
:deep(.inventario-marcador) {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-family: var(--font-sans);
  font-weight: 800;
  border-radius: 50%;
}
.inventario-legenda {
  width: 1.25rem;
  height: 1.25rem;
  flex-shrink: 0;
}
:deep(.inventario-marcador) {
  width: 2.25rem;
  height: 2.25rem;
  border: 2px solid white;
  box-shadow: 0 2px 6px rgb(0 0 0 / 25%);
  font-size: 1.25rem;
  cursor: pointer;
}
:deep(.inventario-marcador:hover),
:deep(.inventario-marcador:focus-visible),
:deep(.inventario-marcador[aria-pressed='true']) {
  outline: 3px solid var(--color-amma-green-dark);
  outline-offset: 3px;
}
.condicao-nao-avaliada,
:deep(.condicao-nao-avaliada) {
  background: var(--color-grey-text);
}
.condicao-saudavel,
:deep(.condicao-saudavel) {
  background: var(--color-amma-green-dark);
}
.condicao-comprometida,
:deep(.condicao-comprometida) {
  background: var(--color-feedback-warning);
}
.condicao-morta,
:deep(.condicao-morta) {
  background: var(--color-feedback-danger);
}
:deep(.maplibregl-ctrl-attrib) {
  max-width: 100%;
  font-family: var(--font-sans);
}
</style>
