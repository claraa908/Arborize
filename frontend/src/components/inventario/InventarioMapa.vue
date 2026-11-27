<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, useTemplateRef, watch } from 'vue'
import { Map as MapaLibre, Marker, NavigationControl, setWorkerUrl } from 'maplibre-gl'
import 'maplibre-gl/dist/maplibre-gl.css'
import workerUrl from 'maplibre-gl/dist/maplibre-gl-worker.mjs?worker&url'
import type { ArvoreResumoResponseDTO } from '@/types/inventario'
import { iconeArvoreMapa } from './arvoreMapaIcone'
import './arvoreMapaIcone.css'
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
    filtrosAtivos?: boolean
    mensagemVazia?: string
    centroInicial?: [number, number]
    zoomInicial?: number
    selecaoAoLado?: boolean
    somenteVisualizacao?: boolean
  }>(),
  {
    mensagemVazia: 'Nenhuma árvore carregada no mapa.',
    centroInicial: () => [...CENTRO_INICIAL],
    zoomInicial: ZOOM_INICIAL,
    selecaoAoLado: false,
    somenteVisualizacao: false,
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
  if (props.somenteVisualizacao && arvore !== null) return
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
    button.className = `inventario-marcador arvore-mapa-icone ${condicao.classe}${props.somenteVisualizacao ? ' inventario-marcador-visualizacao' : ''}`
    button.innerHTML = `<span class="inventario-ondas" aria-hidden="true"></span>${iconeArvoreMapa}`
    button.setAttribute(
      'aria-label',
      `Árvore ${arvore.codigo}: ${arvore.especie.nomePopular}. ${condicao.rotulo}`,
    )
    if (props.somenteVisualizacao) {
      button.tabIndex = -1
    } else {
      button.setAttribute('aria-pressed', String(arvore.id === idSelecionado.value))
    }
    button.title = `Árvore ${arvore.codigo} · ${condicao.rotulo}`
    const click = () => selecionar(arvore)
    if (!props.somenteVisualizacao) {
      button.addEventListener('click', click)
    }
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
watch(selecionada, async () => {
  if (!props.selecaoAoLado) return
  await nextTick()
  mapa?.resize()
})
</script>

<template>
  <div
    class="inventario-mapa min-w-0"
    :class="{
      'inventario-mapa-filtrado': filtrosAtivos,
      'inventario-mapa-satelite': baseAtual === 'satelite',
    }"
  >
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
    <div
      :class="selecaoAoLado && selecionada && !somenteVisualizacao ? 'grid grid-cols-1 gap-4 lg:grid-cols-[minmax(0,1.25fr)_minmax(0,1fr)] lg:items-stretch' : ''"
    >
      <div class="relative min-w-0">
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

      <section
        v-if="selecionada && !somenteVisualizacao"
        aria-label="Árvore selecionada"
        aria-live="polite"
        :class="
          selecaoAoLado
            ? 'flex min-w-0 flex-col overflow-hidden rounded-btn border border-amma-green-light bg-white shadow-sm lg:h-[440px]'
            : 'mt-4 rounded-btn border border-amma-green-light bg-amma-green-lighter p-4'
        "
      >
        <template v-if="selecaoAoLado">
          <div class="relative w-full shrink-0">
            <ArvoreFotoMiniatura
              :arvore-id="selecionada.id"
              :foto-capa="selecionada.fotoCapa"
              tamanho="destaque"
              carregar-imediatamente
            />
            <button
              type="button"
              aria-label="Fechar seleção da árvore"
              class="absolute right-2.5 top-2.5 flex h-9 w-9 items-center justify-center rounded-full border border-amma-green-light bg-white/95 text-amma-green-dark shadow-sm hover:bg-amma-green-lighter focus-visible:outline-2 focus-visible:outline-amma-green"
              @click="selecionar(null)"
            >
              <span aria-hidden="true" class="material-symbols-outlined text-[20px]">close</span>
            </button>
          </div>

          <div class="flex min-h-0 flex-1 flex-col justify-between gap-3 overflow-y-auto p-4">
            <div class="min-w-0 break-words">
              <div class="flex flex-wrap items-center justify-between gap-2">
                <p class="text-xs font-bold uppercase tracking-wider text-grey-text">
                  {{ selecionada.codigo }}
                </p>
                <span class="inline-flex items-center rounded-full border border-amma-green-light bg-amma-green-lighter px-2.5 py-0.5 text-[11px] font-bold text-amma-green-dark">
                  {{ condicoesMapa[selecionada.condicaoFitossanitaria].rotulo }}
                </span>
              </div>
              <h3 class="mt-2 text-lg font-bold leading-snug text-amma-green-dark">
                {{ selecionada.especie.nomePopular }}
              </h3>
              <p class="text-sm italic text-grey-text">{{ selecionada.especie.nomeCientifico }}</p>
              <p
                v-if="selecionada.zonaRural === true || selecionada.classificacaoTerritorial === 'FORA_DA_AREA_URBANA'"
                class="mt-2.5 flex items-center gap-1.5 text-xs text-grey-text"
              >
                <span class="material-symbols-outlined text-[16px] text-amma-green" aria-hidden="true">location_on</span>
                <span>Localidade rural: {{ selecionada.localidadeRural || 'Não informado' }}</span>
              </p>
              <p
                v-else-if="selecionada.bairroNome || selecionada.bairro?.nome"
                class="mt-2.5 flex items-center gap-1.5 text-xs text-grey-text"
              >
                <span class="material-symbols-outlined text-[16px] text-amma-green" aria-hidden="true">location_on</span>
                <span>Bairro: {{ selecionada.bairroNome || selecionada.bairro?.nome }}</span>
              </p>
            </div>
            <button
              type="button"
              class="w-full shrink-0 rounded-btn bg-amma-green px-4 py-2.5 text-sm font-bold text-white shadow-xs transition-colors hover:bg-amma-green-dark focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-amma-green"
              @click="emit('abrirFicha', selecionada.id)"
            >
              Ver ficha técnica
            </button>
          </div>
        </template>

        <template v-else>
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
        </template>
      </section>
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

    <div class="mt-4 rounded-btn border border-amma-green-light/80 bg-amma-bg/70 p-3.5">
      <p class="mb-2.5 text-[11px] font-bold uppercase tracking-wider text-amma-green-dark">
        Condição fitossanitária
      </p>
      <ul
        aria-label="Legenda de condições fitossanitárias"
        :class="[
          'grid grid-cols-1 gap-2.5 sm:grid-cols-2',
          selecaoAoLado ? 'lg:grid-cols-4' : '',
        ]"
      >
        <li
          v-for="(condicao, valor) in condicoesMapa"
          :key="valor"
          class="flex items-center gap-2.5 rounded-btn border border-amma-green-light/70 bg-white px-3 py-2 text-xs font-semibold text-grey-text shadow-2xs"
        >
          <span
            aria-hidden="true"
            class="inventario-legenda arvore-mapa-icone shrink-0"
            :class="condicao.classe"
            v-html="iconeArvoreMapa"
          />
          <span class="min-w-0 leading-snug">{{ condicao.rotulo }}</span>
        </li>
      </ul>
    </div>
  </div>
</template>

<style scoped>
/* Anima somente o elemento interno: o transform do MapLibre posiciona o marcador. */
.inventario-legenda {
  width: 30px;
  height: 36px;
}
.inventario-legenda :deep(svg) {
  width: 26px;
  height: 32px;
}
:deep(.inventario-marcador) {
  cursor: pointer;
}
:deep(.inventario-marcador-visualizacao) {
  cursor: default;
}
:deep(.inventario-marcador:focus-visible) {
  outline: 3px solid var(--color-amma-green-dark);
  outline-offset: 3px;
  border-radius: 6px;
}
:deep(.inventario-ondas) {
  display: none;
}
.inventario-mapa-filtrado :deep(.inventario-ondas),
:deep(.inventario-marcador:hover .inventario-ondas),
:deep(.inventario-marcador[aria-pressed='true'] .inventario-ondas) {
  display: block;
  position: absolute;
  width: 28px;
  height: 28px;
  top: calc(50% - 14px);
  left: calc(50% - 14px);
  pointer-events: none;
  color: var(--inventario-cor-onda, #15803d);
}
.inventario-mapa-satelite {
  --inventario-cor-onda: white;
}
:deep(.inventario-ondas::before),
:deep(.inventario-ondas::after) {
  content: '';
  position: absolute;
  inset: 0;
  border: 3px solid currentColor;
  border-radius: 50%;
  animation: inventario-onda 2.8s ease-out infinite;
}
:deep(.inventario-ondas::after) {
  animation-delay: -1.4s;
}
@keyframes inventario-onda {
  from {
    transform: scale(0.9);
    opacity: 1;
  }
  40% {
    opacity: 0.85;
  }
  to {
    transform: scale(2.2);
    opacity: 0;
  }
}
@media (prefers-reduced-motion: reduce) {
  :deep(.inventario-ondas::before),
  :deep(.inventario-ondas::after) {
    animation: none;
  }
  :deep(.inventario-ondas::before) {
    transform: scale(1.35);
    opacity: 0.8;
  }
  :deep(.inventario-ondas::after) {
    display: none;
  }
}
:deep(.maplibregl-ctrl-attrib) {
  max-width: 100%;
  font-family: var(--font-sans);
}
</style>
