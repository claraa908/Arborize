<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import capaBanner from '@/assets/images/capa-eventos.jpeg'
import EventoCard from '@/components/eventos/EventoCard.vue'
import {
  eventoPossuiBanner,
  isEventoVisivelPublicamente,
  ordenarEventosPublicos,
} from '@/components/eventos/eventoPresentation'
import { eventoService } from '@/services/eventoService'
import { useEventoStore } from '@/stores/eventoStore'
import type { EventoDTO } from '@/types/eventos'

type FiltroStatusPublico = 'TODOS' | 'CONFIRMADO' | 'ENCERRADO'

const router = useRouter()
const eventoStore = useEventoStore()

const searchQuery = ref('')
const selectedStatus = ref<FiltroStatusPublico>('TODOS')
const rootEl = ref<HTMLElement | null>(null)
const isScrolled = ref(false)

let scrollContainer: HTMLElement | Window | null = null
let isTicking = false

const eventosPublicos = computed(() =>
  ordenarEventosPublicos(eventoStore.eventos.filter(isEventoVisivelPublicamente)),
)

const contadores = computed(() => {
  const todos = eventosPublicos.value.length
  const confirmados = eventosPublicos.value.filter((e) => e.status === 'CONFIRMADO').length
  const encerrados = eventosPublicos.value.filter((e) => e.status === 'ENCERRADO').length
  return { todos, confirmados, encerrados }
})

const filtros = computed<Array<{ key: FiltroStatusPublico; label: string; count: number }>>(() => [
  { key: 'TODOS', label: 'Todos', count: contadores.value.todos },
  { key: 'CONFIRMADO', label: 'Confirmados', count: contadores.value.confirmados },
  { key: 'ENCERRADO', label: 'Encerrados', count: contadores.value.encerrados },
])

const eventosFiltrados = computed(() => {
  const termo = searchQuery.value.trim().toLowerCase()

  return eventosPublicos.value.filter((evt) => {
    const matchStatus = selectedStatus.value === 'TODOS' || evt.status === selectedStatus.value
    if (!matchStatus) return false

    if (!termo) return true

    return (
      evt.nome.toLowerCase().includes(termo) ||
      (evt.localConcentracao?.toLowerCase().includes(termo) ?? false) ||
      (evt.destinoFinal?.toLowerCase().includes(termo) ?? false)
    )
  })
})

const obterBannerUrl = (evento: EventoDTO): string | undefined => {
  if (!eventoPossuiBanner(evento)) return undefined
  if (evento.bannerUrl && evento.bannerUrl.trim() !== '') return evento.bannerUrl
  if (evento.id !== undefined && evento.id !== null) {
    return eventoService.getBannerUrl(evento.id)
  }
  return undefined
}

const openDetails = (id: string | number) => {
  void router.push({ name: 'eventos-publico-detalhes', params: { id: String(id) } })
}

const limparFiltros = () => {
  searchQuery.value = ''
  selectedStatus.value = 'TODOS'
}

const recarregarEventos = () => {
  void eventoStore.fetchEventos()
}

const handleScroll = (e: Event) => {
  if (isTicking) return
  isTicking = true

  requestAnimationFrame(() => {
    const target = e.target as HTMLElement | null
    const currentScroll = target && typeof target.scrollTop === 'number' ? target.scrollTop : window.scrollY

    if (currentScroll > 60 && !isScrolled.value) {
      isScrolled.value = true
    } else if (currentScroll < 20 && isScrolled.value) {
      isScrolled.value = false
    }

    isTicking = false
  })
}

onMounted(() => {
  void eventoStore.fetchEventos()

  const main = rootEl.value?.closest('main')
  if (main) {
    scrollContainer = main
    main.style.overflowAnchor = 'none'
    main.addEventListener('scroll', handleScroll, { passive: true })
  } else {
    scrollContainer = window
    window.addEventListener('scroll', handleScroll, { passive: true })
  }
})

onUnmounted(() => {
  if (scrollContainer) {
    scrollContainer.removeEventListener('scroll', handleScroll as EventListener)
  }
})
</script>

<template>
  <div ref="rootEl" class="max-w-7xl mx-auto flex flex-col gap-6 relative pb-10" style="overflow-anchor: none;">
    <div
      class="sticky top-0 -mt-2 -mx-6 md:-mx-10 px-6 md:px-10 z-30 pt-1 pb-4 bg-white/70 backdrop-blur-md"
      style="overflow-anchor: none;"
    >
      <div
        class="relative overflow-hidden shadow-md transition-all duration-300 ease-out"
        :class="isScrolled ? 'rounded-btn p-2.5 md:p-3' : 'rounded-box py-4 px-5 md:py-5 md:px-6'"
        style="isolation: isolate;"
      >
        <img
          :src="capaBanner"
          alt=""
          aria-hidden="true"
          class="absolute inset-0 w-full h-full object-cover rounded-[inherit] pointer-events-none select-none blur-[2px] scale-105"
        />

        <div
          class="absolute inset-0 rounded-[inherit] pointer-events-none transition-all duration-300 ease-out"
          :class="isScrolled ? 'bg-white/80 backdrop-blur-xl border border-white/70 shadow-sm' : 'bg-transparent border-transparent'"
        ></div>

        <div
          class="relative z-10 transition-all duration-300 ease-out flex"
          :class="isScrolled ? 'flex-col lg:flex-row lg:items-center lg:justify-between gap-3' : 'flex-col items-center gap-3 md:gap-4'"
        >
          <div
            class="transition-all duration-300 ease-out flex"
            :class="isScrolled ? 'items-center gap-3 shrink-0' : 'flex-col items-center text-center'"
          >
            <div
              class="bg-white/95 rounded-full flex items-center justify-center border-2 border-white shadow-md transition-all duration-300 ease-out shrink-0"
              :class="isScrolled ? 'w-10 h-10 shadow-sm' : 'w-12 h-12 md:w-14 md:h-14 mb-1'"
            >
              <span
                class="material-symbols-outlined text-amma-green transition-all duration-300 ease-out select-none !leading-none"
                :style="{ fontSize: isScrolled ? '22px' : '32px' }"
                aria-hidden="true"
              >
                calendar_month
              </span>
            </div>

            <div :class="isScrolled ? 'flex flex-col' : 'flex flex-col items-center'">
              <h1
                class="font-black text-amma-green-dark transition-all duration-300 ease-out leading-tight drop-shadow-sm tracking-tight"
                :class="isScrolled ? 'text-lg md:text-xl' : 'text-xl md:text-2xl lg:text-3xl mb-1'"
              >
                Eventos Ambientais
              </h1>

              <div
                class="transition-all duration-300 ease-out"
                :class="isScrolled ? 'hidden' : 'block max-w-2xl mx-auto text-center'"
              >
                <p
                  class="inline-block bg-white/30 backdrop-blur-md px-4 py-1.5 rounded-full text-gray-800 text-xs md:text-sm font-semibold shadow-sm border border-white/50"
                >
                  Participe dos mutirões de plantio, campanhas e ações ambientais promovidos pela AMMA em Quixadá.
                </p>
              </div>
            </div>
          </div>

          <div
            class="transition-all duration-300 ease-out flex flex-col md:flex-row items-center gap-2.5 rounded-btn"
            :class="[
              isScrolled
                ? 'p-0 flex-1 lg:max-w-3xl lg:justify-end bg-transparent border-transparent shadow-none backdrop-blur-none'
                : 'p-2.5 md:p-3 w-full justify-between bg-white/30 backdrop-blur-md border border-white/50 shadow-[0_8px_24px_rgba(0,0,0,0.06)]',
            ]"
          >
            <div
              class="relative w-full transition-all duration-300 ease-out"
              :class="isScrolled ? 'md:w-64 lg:w-72' : 'md:w-1/2'"
            >
              <label for="eventos-publico-busca" class="sr-only">
                Buscar eventos por nome, local de concentração ou destino
              </label>
              <span
                class="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-amma-green-dark font-bold text-[20px] select-none"
                aria-hidden="true"
              >
                search
              </span>
              <input
                id="eventos-publico-busca"
                v-model="searchQuery"
                type="text"
                placeholder="Buscar por nome do evento ou local..."
                class="w-full bg-white text-gray-800 placeholder-gray-500 border-2 border-white focus:border-amma-green rounded-btn pl-10 pr-4 outline-none transition-all text-sm font-medium shadow-sm"
                :class="isScrolled ? 'py-1.5' : 'py-2'"
              />
            </div>

            <div
              role="group"
              aria-label="Filtrar eventos por status"
              class="w-full md:w-auto overflow-x-auto pb-1 md:pb-0 hide-scrollbar flex gap-1.5 shrink-0"
            >
              <button
                v-for="filtro in filtros"
                :key="filtro.key"
                type="button"
                :aria-pressed="selectedStatus === filtro.key"
                :class="[
                  'whitespace-nowrap rounded-btn font-bold transition-all border-2 text-xs shadow-sm inline-flex items-center gap-1.5 cursor-pointer focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-amma-green',
                  isScrolled ? 'px-3 py-1.5' : 'px-3.5 py-1.5 text-xs',
                  selectedStatus === filtro.key
                    ? 'bg-amma-green text-white border-amma-green shadow-md'
                    : 'bg-white text-gray-700 border-white hover:border-amma-green hover:text-amma-green',
                ]"
                @click="selectedStatus = filtro.key"
              >
                <span>{{ filtro.label }}</span>
                <span
                  :class="[
                    'px-1.5 py-0.5 rounded-full text-[10px]',
                    selectedStatus === filtro.key ? 'bg-white/20 text-white' : 'bg-amma-green-lighter text-amma-green-dark',
                  ]"
                >
                  {{ filtro.count }}
                </span>
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div
      v-if="eventoStore.isLoading"
      role="status"
      aria-live="polite"
      class="flex flex-col gap-4 pt-2"
    >
      <p class="sr-only">Carregando eventos ambientais...</p>
      <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6">
        <div
          v-for="n in 4"
          :key="n"
          class="bg-white rounded-box border border-amma-green-light shadow-sm h-[396px] overflow-hidden animate-pulse flex flex-col"
        >
          <div class="h-44 bg-amma-green-lighter"></div>
          <div class="p-5 flex flex-col gap-3 flex-1">
            <div class="h-3 w-24 bg-amma-green-light rounded-full"></div>
            <div class="h-5 w-3/4 bg-amma-green-light rounded-full"></div>
            <div class="h-4 w-full bg-amma-bg rounded-full"></div>
            <div class="h-4 w-5/6 bg-amma-bg rounded-full"></div>
            <div class="mt-auto pt-3 border-t border-amma-green-light/60 flex flex-col gap-2">
              <div class="h-3 w-2/3 bg-amma-bg rounded-full"></div>
              <div class="h-3 w-1/2 bg-amma-bg rounded-full"></div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div
      v-else-if="eventoStore.error"
      role="alert"
      class="bg-feedback-danger-bg rounded-box border border-feedback-danger-border p-10 flex flex-col items-center justify-center text-center mt-2"
    >
      <span class="material-symbols-outlined text-5xl text-feedback-danger mb-3" aria-hidden="true">
        error_outline
      </span>
      <h2 class="text-xl font-bold text-feedback-danger mb-2">Erro ao carregar eventos</h2>
      <p class="text-sm text-grey-text max-w-md">
        {{ eventoStore.error }} Verifique sua conexão e tente novamente.
      </p>
      <button
        type="button"
        class="mt-6 bg-amma-green hover:bg-amma-green-dark text-white font-bold py-2.5 px-6 rounded-btn transition-colors text-sm inline-flex items-center gap-2 cursor-pointer efeito-zoom focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-amma-green"
        @click="recarregarEventos"
      >
        <span class="material-symbols-outlined text-[18px]" aria-hidden="true">refresh</span>
        Tentar novamente
      </button>
    </div>

    <div
      v-else-if="eventosPublicos.length === 0"
      class="bg-white rounded-box shadow-sm border border-amma-green-light p-12 flex flex-col items-center justify-center text-center mt-2"
    >
      <span class="material-symbols-outlined text-6xl text-amma-green/35 mb-4" aria-hidden="true">
        event_upcoming
      </span>
      <h2 class="text-xl font-bold text-amma-green-dark mb-2">Nenhum evento disponível no momento</h2>
      <p class="text-sm text-grey-text max-w-md">
        Ainda não há eventos confirmados ou encerrados publicados pela AMMA. Volte em breve para acompanhar os próximos mutirões e ações ambientais.
      </p>
    </div>

    <div
      v-else-if="eventosFiltrados.length === 0"
      class="bg-white rounded-box shadow-sm border border-amma-green-light p-12 flex flex-col items-center justify-center text-center mt-2"
    >
      <span class="material-symbols-outlined text-6xl text-grey-text/30 mb-4" aria-hidden="true">
        search_off
      </span>
      <h2 class="text-xl font-bold text-amma-green-dark mb-2">Nenhum evento encontrado</h2>
      <p class="text-sm text-grey-text font-medium">
        Tente alterar os termos da pesquisa ou selecione outro filtro de status.
      </p>
      <button
        type="button"
        class="mt-6 bg-amma-green-lighter hover:bg-amma-green text-amma-green-dark hover:text-white font-bold py-2.5 px-6 rounded-btn transition-colors text-sm cursor-pointer focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-amma-green"
        @click="limparFiltros"
      >
        Limpar Filtros
      </button>
    </div>

    <div
      v-else
      class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6 pt-2"
    >
      <EventoCard
        v-for="evt in eventosFiltrados"
        :key="evt.id"
        :evento="evt"
        :banner-url="obterBannerUrl(evt)"
        @select="openDetails"
      />
    </div>
  </div>
</template>

<style scoped>
.hide-scrollbar::-webkit-scrollbar {
  display: none;
}
.hide-scrollbar {
  -ms-overflow-style: none;
  scrollbar-width: none;
}
</style>
