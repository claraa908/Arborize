<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AppBreadcrumb, { type BreadcrumbItem } from '@/components/ui/AppBreadcrumb.vue'
import {
  eventoPossuiBanner,
  formatarHorarioEvento,
  formatarPeriodoEvento,
  obterStatusVisualEvento,
} from '@/components/eventos/eventoPresentation'
import { eventoService } from '@/services/eventoService'
import { useEventoStore } from '@/stores/eventoStore'

const route = useRoute()
const router = useRouter()
const eventoStore = useEventoStore()

const bannerErro = ref(false)

const eventoId = computed(() => String(route.params.id ?? ''))
const evento = computed(() => eventoStore.eventoAtual)

const isCancelado = computed(() => evento.value?.status === 'CANCELADO')
const isIndisponivel = computed(
  () => eventoStore.detailStatus === 404 || evento.value?.status === 'PLANEJADO',
)
const isVisivel = computed(
  () => evento.value?.status === 'CONFIRMADO' || evento.value?.status === 'ENCERRADO',
)

const statusVisual = computed(() =>
  evento.value ? obterStatusVisualEvento(evento.value.status) : null,
)

const bannerUrl = computed(() => {
  const atual = evento.value
  if (!atual || !eventoPossuiBanner(atual)) return undefined
  if (atual.bannerUrl && atual.bannerUrl.trim() !== '') return atual.bannerUrl
  if (atual.id !== undefined && atual.id !== null) {
    return eventoService.getBannerUrl(atual.id)
  }
  return undefined
})

const mostrarBanner = computed(() => Boolean(bannerUrl.value) && !bannerErro.value)

const periodoFormatado = computed(() =>
  evento.value ? formatarPeriodoEvento(evento.value.dataInicio, evento.value.dataTermino) : 'Não informado',
)

const horarioFormatado = computed(() =>
  evento.value ? formatarHorarioEvento(evento.value.horarioInicio, evento.value.horarioTermino) : 'Não informado',
)

const breadcrumbItems = computed<BreadcrumbItem[]>(() => [
  { label: 'Eventos Ambientais', icon: 'calendar_month', routeName: 'eventos-publico' },
  { label: isVisivel.value && evento.value ? evento.value.nome : isCancelado.value && evento.value ? evento.value.nome : 'Detalhes do Evento' },
])

const carregarEvento = () => {
  bannerErro.value = false
  if (eventoId.value) {
    void eventoStore.fetchEventoById(eventoId.value)
  }
}

const voltarParaEventos = () => {
  void router.push({ name: 'eventos-publico' })
}

watch(eventoId, () => {
  carregarEvento()
})

onMounted(() => {
  carregarEvento()
})

onUnmounted(() => {
  eventoStore.clearEventoAtual()
})
</script>

<template>
  <div class="flex w-full flex-col gap-4 pb-8">
    <AppBreadcrumb class="-mx-6 -mt-2 md:-mx-10" :items="breadcrumbItems" />
    <div class="mx-auto flex w-full max-w-7xl flex-col gap-4">

    <div
      v-if="eventoStore.isLoadingDetail"
      role="status"
      aria-live="polite"
      class="bg-white rounded-box shadow-sm border border-amma-green-light p-8 md:p-10 animate-pulse flex flex-col lg:flex-row gap-8 min-h-[420px]"
    >
      <p class="sr-only">Carregando detalhes do evento...</p>
      <div class="w-full lg:w-1/2 h-64 lg:h-auto bg-amma-green-lighter rounded-btn"></div>
      <div class="w-full lg:w-1/2 flex flex-col gap-4">
        <div class="h-6 w-28 bg-amma-green-light rounded-full"></div>
        <div class="h-8 w-3/4 bg-amma-green-light rounded-full"></div>
        <div class="h-4 w-full bg-amma-bg rounded-full"></div>
        <div class="h-4 w-5/6 bg-amma-bg rounded-full"></div>
        <div class="h-24 w-full bg-amma-bg rounded-btn mt-4"></div>
      </div>
    </div>

    <div
      v-else-if="isCancelado && evento"
      role="alert"
      class="bg-feedback-warning-bg rounded-box border border-feedback-warning-border p-8 md:p-12 flex flex-col items-center justify-center text-center gap-3"
    >
      <span class="material-symbols-outlined text-5xl text-feedback-warning" aria-hidden="true">
        event_busy
      </span>
      <span
        class="inline-flex items-center gap-1 rounded-full px-3 py-1 text-xs font-bold uppercase tracking-wide bg-feedback-danger-bg text-feedback-danger border border-feedback-danger-border"
      >
        <span class="material-symbols-outlined text-[14px]" aria-hidden="true">event_busy</span>
        Cancelado
      </span>
      <h1 class="text-2xl font-bold text-amma-green-dark">{{ evento.nome }}</h1>
      <p class="text-sm text-grey-text max-w-lg">
        Este evento foi cancelado pela AMMA e não está mais ativo na programação pública.
      </p>
      <button
        type="button"
        class="mt-4 bg-amma-green hover:bg-amma-green-dark text-white font-bold py-2.5 px-6 rounded-btn transition-colors text-sm inline-flex items-center gap-2 cursor-pointer efeito-zoom focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-amma-green"
        @click="voltarParaEventos"
      >
        <span class="material-symbols-outlined text-[18px]" aria-hidden="true">arrow_back</span>
        Voltar para Eventos
      </button>
    </div>

    <div
      v-else-if="isIndisponivel"
      class="bg-white rounded-box shadow-sm border border-amma-green-light p-10 md:p-12 flex flex-col items-center justify-center text-center gap-3"
    >
      <span class="material-symbols-outlined text-6xl text-grey-text/35" aria-hidden="true">
        event_note
      </span>
      <h1 class="text-2xl font-bold text-amma-green-dark">Evento não encontrado</h1>
      <p class="text-sm text-grey-text max-w-md">
        O evento solicitado não foi localizado ou ainda não está disponível para consulta pública.
      </p>
      <button
        type="button"
        class="mt-4 bg-amma-green hover:bg-amma-green-dark text-white font-bold py-2.5 px-6 rounded-btn transition-colors text-sm inline-flex items-center gap-2 cursor-pointer efeito-zoom focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-amma-green"
        @click="voltarParaEventos"
      >
        <span class="material-symbols-outlined text-[18px]" aria-hidden="true">arrow_back</span>
        Voltar para Eventos
      </button>
    </div>

    <div
      v-else-if="eventoStore.detailError"
      role="alert"
      class="bg-feedback-danger-bg rounded-box border border-feedback-danger-border p-10 flex flex-col items-center justify-center text-center gap-3"
    >
      <span class="material-symbols-outlined text-5xl text-feedback-danger" aria-hidden="true">
        error_outline
      </span>
      <h1 class="text-xl font-bold text-feedback-danger">Erro ao carregar detalhes</h1>
      <p class="text-sm text-grey-text max-w-md">
        {{ eventoStore.detailError }} Verifique sua conexão e tente novamente.
      </p>
      <div class="mt-4 flex flex-wrap items-center justify-center gap-3">
        <button
          type="button"
          class="bg-amma-green hover:bg-amma-green-dark text-white font-bold py-2.5 px-6 rounded-btn transition-colors text-sm inline-flex items-center gap-2 cursor-pointer efeito-zoom focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-amma-green"
          @click="carregarEvento"
        >
          <span class="material-symbols-outlined text-[18px]" aria-hidden="true">refresh</span>
          Tentar novamente
        </button>
        <button
          type="button"
          class="border-2 border-amma-green text-amma-green hover:bg-amma-green-lighter font-bold py-2.5 px-6 rounded-btn transition-colors text-sm inline-flex items-center gap-2 cursor-pointer focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-amma-green"
          @click="voltarParaEventos"
        >
          Voltar para Eventos
        </button>
      </div>
    </div>

    <article
      v-else-if="isVisivel && evento && statusVisual"
      class="bg-white rounded-box shadow-sm border border-amma-green-light overflow-hidden flex flex-col lg:flex-row"
    >
      <div
        class="w-full lg:w-1/2 bg-amma-bg border-b lg:border-b-0 lg:border-r border-amma-green-light flex flex-col"
      >
        <div class="relative w-full h-64 sm:h-80 lg:h-96 bg-amma-green-lighter flex items-center justify-center overflow-hidden">
          <img
            v-if="mostrarBanner"
            :src="bannerUrl"
            :alt="`Banner do evento ${evento.nome}`"
            class="w-full h-full object-cover"
            @error="bannerErro = true"
          />
          <div
            v-else
            data-testid="evento-detalhe-banner-fallback"
            class="flex flex-col items-center justify-center gap-2 p-6 text-center"
          >
            <div class="w-20 h-20 rounded-full bg-white flex items-center justify-center shadow-sm border border-amma-green-light">
              <span class="material-symbols-outlined text-4xl text-amma-green" aria-hidden="true">
                calendar_month
              </span>
            </div>
            <span class="text-xs font-semibold text-grey-text">Evento Ambiental da AMMA</span>
          </div>

          <span
            :class="[
              'absolute top-4 left-4 text-xs uppercase tracking-wider font-bold px-3 py-1 rounded-full shadow-sm inline-flex items-center gap-1.5',
              statusVisual.badgeClass,
            ]"
          >
            <span class="material-symbols-outlined text-[15px]" aria-hidden="true">{{ statusVisual.icon }}</span>
            {{ statusVisual.label }}
          </span>
        </div>

        <div class="p-6 md:p-8 grid grid-cols-1 sm:grid-cols-2 gap-4 bg-amma-green-lighter/60 flex-1">
          <div class="bg-white rounded-btn border border-amma-green-light p-4 flex items-start gap-3">
            <span class="material-symbols-outlined text-amma-green text-2xl shrink-0" aria-hidden="true">
              calendar_today
            </span>
            <div class="min-w-0">
              <p class="text-[11px] font-bold uppercase tracking-wider text-grey-text">Período</p>
              <p class="text-sm font-bold text-amma-green-dark mt-0.5">{{ periodoFormatado }}</p>
            </div>
          </div>

          <div class="bg-white rounded-btn border border-amma-green-light p-4 flex items-start gap-3">
            <span class="material-symbols-outlined text-amma-green text-2xl shrink-0" aria-hidden="true">
              schedule
            </span>
            <div class="min-w-0">
              <p class="text-[11px] font-bold uppercase tracking-wider text-grey-text">Horário</p>
              <p class="text-sm font-bold text-amma-green-dark mt-0.5">{{ horarioFormatado }}</p>
            </div>
          </div>

          <div class="bg-white rounded-btn border border-amma-green-light p-4 flex items-start gap-3 sm:col-span-2">
            <span class="material-symbols-outlined text-amma-green text-2xl shrink-0" aria-hidden="true">
              location_on
            </span>
            <div class="min-w-0">
              <p class="text-[11px] font-bold uppercase tracking-wider text-grey-text">Local de Concentração</p>
              <p class="text-sm font-bold text-amma-green-dark mt-0.5">
                {{ evento.localConcentracao || 'Não informado' }}
              </p>
            </div>
          </div>

          <div
            v-if="evento.destinoFinal"
            class="bg-white rounded-btn border border-amma-green-light p-4 flex items-start gap-3 sm:col-span-2"
          >
            <span class="material-symbols-outlined text-amma-green text-2xl shrink-0" aria-hidden="true">
              flag
            </span>
            <div class="min-w-0">
              <p class="text-[11px] font-bold uppercase tracking-wider text-grey-text">Destino Final</p>
              <p class="text-sm font-bold text-amma-green-dark mt-0.5">{{ evento.destinoFinal }}</p>
            </div>
          </div>
        </div>
      </div>

      <div class="w-full lg:w-1/2 p-6 md:p-8 flex flex-col justify-between gap-6 bg-white">
        <div class="flex flex-col gap-5">
          <div>
            <p class="text-xs font-bold uppercase tracking-wider text-amma-green mb-1">
              {{ evento.status === 'ENCERRADO' ? 'Histórico de Eventos' : 'Programação Confirmada' }}
            </p>
            <h1 class="text-2xl md:text-3xl font-bold text-amma-green-dark leading-tight">
              {{ evento.nome }}
            </h1>
          </div>

          <section aria-labelledby="sobre-evento-heading" class="flex flex-col gap-2">
            <h2 id="sobre-evento-heading" class="text-sm font-bold uppercase tracking-wider text-amma-green-dark">
              Sobre o Evento
            </h2>
            <p class="text-sm text-grey-text leading-relaxed whitespace-pre-line">
              {{ evento.descricao || 'Descrição não informada.' }}
            </p>
          </section>

          <section
            v-if="evento.descricaoPontoPartida"
            aria-labelledby="ponto-partida-heading"
            class="rounded-btn bg-amma-green-lighter border border-amma-green-light p-4 flex flex-col gap-1.5"
          >
            <h2
              id="ponto-partida-heading"
              class="text-xs font-bold uppercase tracking-wider text-amma-green-dark flex items-center gap-1.5"
            >
              <span class="material-symbols-outlined text-[16px] text-amma-green" aria-hidden="true">
                trip_origin
              </span>
              Orientações do Ponto de Partida
            </h2>
            <p class="text-sm text-grey-text leading-relaxed">{{ evento.descricaoPontoPartida }}</p>
          </section>

          <section
            v-if="evento.descricaoEncerramento"
            aria-labelledby="encerramento-heading"
            class="rounded-btn bg-amma-bg border border-amma-green-light p-4 flex flex-col gap-1.5"
          >
            <h2
              id="encerramento-heading"
              class="text-xs font-bold uppercase tracking-wider text-amma-green-dark flex items-center gap-1.5"
            >
              <span class="material-symbols-outlined text-[16px] text-amma-green" aria-hidden="true">
                sports_score
              </span>
              Encerramento
            </h2>
            <p class="text-sm text-grey-text leading-relaxed">{{ evento.descricaoEncerramento }}</p>
          </section>
        </div>
      </div>
    </article>
    </div>
  </div>
</template>
