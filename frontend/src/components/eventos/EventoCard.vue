<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import type { EventoDTO } from '@/types/eventos'
import {
  formatarHorarioEvento,
  formatarPeriodoEvento,
  obterStatusVisualEvento,
} from '@/components/eventos/eventoPresentation'

const props = defineProps<{
  evento: EventoDTO
  bannerUrl?: string
}>()

const emit = defineEmits<{
  (e: 'select', id: string | number): void
}>()

const bannerErro = ref(false)

watch(
  () => props.bannerUrl,
  () => {
    bannerErro.value = false
  },
)

const mostrarBanner = computed(() => Boolean(props.bannerUrl) && !bannerErro.value)
const statusVisual = computed(() => obterStatusVisualEvento(props.evento.status))
const periodoFormatado = computed(() =>
  formatarPeriodoEvento(props.evento.dataInicio, props.evento.dataTermino),
)
const horarioFormatado = computed(() =>
  formatarHorarioEvento(props.evento.horarioInicio, props.evento.horarioTermino),
)

const handleSelect = () => {
  if (props.evento.id !== undefined && props.evento.id !== null) {
    emit('select', props.evento.id)
  }
}
</script>

<template>
  <article
    role="button"
    tabindex="0"
    :aria-label="`Abrir detalhes do evento ${evento.nome}`"
    class="bg-white rounded-box border border-amma-green-light shadow-sm hover:shadow-xl transition-all duration-300 flex flex-col h-[396px] overflow-hidden group cursor-pointer efeito-zoom focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-amma-green"
    @click="handleSelect"
    @keydown.enter.prevent="handleSelect"
    @keydown.space.prevent="handleSelect"
  >
    <div
      class="bg-amma-green-lighter h-44 w-full flex items-center justify-center relative overflow-hidden shrink-0 group-hover:bg-amma-green-light transition-colors"
    >
      <img
        v-if="mostrarBanner"
        :src="bannerUrl"
        :alt="`Banner do evento ${evento.nome}`"
        class="w-full h-full object-cover transition-all duration-500 ease-out group-hover:scale-110 group-hover:blur-[2px]"
        @error="bannerErro = true"
      />
      <div
        v-else
        data-testid="evento-card-fallback"
        class="w-full h-full flex flex-col items-center justify-center gap-1 transition-all duration-500 ease-out group-hover:scale-105"
      >
        <span class="material-symbols-outlined text-6xl text-amma-green/40 select-none" aria-hidden="true">
          calendar_month
        </span>
      </div>

      <span
        :class="[
          'absolute top-3 left-3 backdrop-blur-sm text-[10px] font-bold px-2.5 py-1 rounded-full shadow-sm uppercase tracking-wide z-10 inline-flex items-center gap-1',
          statusVisual.badgeClass,
        ]"
      >
        <span class="material-symbols-outlined text-[13px]" aria-hidden="true">{{ statusVisual.icon }}</span>
        {{ statusVisual.label }}
      </span>

      <div
        class="absolute inset-0 bg-black/25 opacity-0 group-hover:opacity-100 transition-all duration-300 flex items-center justify-center p-3 z-20"
      >
        <span
          class="bg-white/95 text-amma-green-dark text-xs md:text-sm font-bold px-4 py-2 rounded-full shadow-lg border border-white/80 flex items-center gap-1.5 translate-y-2 group-hover:translate-y-0 transition-all duration-300 select-none"
        >
          <span class="material-symbols-outlined text-[18px]" aria-hidden="true">visibility</span>
          Ver detalhes
        </span>
      </div>
    </div>

    <div class="p-5 flex flex-col flex-1 overflow-hidden">
      <div class="flex items-center gap-1.5 text-[11px] font-bold text-amma-green tracking-wider uppercase mb-1.5 shrink-0">
        <span class="material-symbols-outlined text-[14px]" aria-hidden="true">calendar_today</span>
        <span>{{ periodoFormatado }}</span>
      </div>

      <h3
        class="text-base font-bold text-amma-green-dark leading-snug mb-2 group-hover:text-amma-green transition-colors line-clamp-2 shrink-0"
      >
        {{ evento.nome }}
      </h3>

      <p class="text-xs md:text-sm text-grey-text line-clamp-2 mb-3 leading-relaxed">
        {{ evento.descricao || 'Confira os detalhes deste evento ambiental da AMMA.' }}
      </p>

      <div class="mt-auto pt-2.5 border-t border-amma-green-light/70 flex flex-col gap-1 text-xs font-medium text-grey-text/80 shrink-0">
        <div class="flex items-center gap-1.5 min-w-0">
          <span class="material-symbols-outlined text-[15px] text-amma-green shrink-0" aria-hidden="true">
            location_on
          </span>
          <span class="truncate" :title="evento.localConcentracao">
            {{ evento.localConcentracao || 'Local não informado' }}
          </span>
        </div>
        <div class="flex items-center gap-1.5">
          <span class="material-symbols-outlined text-[15px] text-amma-green shrink-0" aria-hidden="true">
            schedule
          </span>
          <span>{{ horarioFormatado }}</span>
        </div>
      </div>
    </div>
  </article>
</template>
