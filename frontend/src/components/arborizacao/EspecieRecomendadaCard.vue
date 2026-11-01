<script setup lang="ts">
import type { SpeciesRecommendation } from '@/types/assistentePlantio'

defineProps<{ species: SpeciesRecommendation }>()
defineEmits<{ select: [species: SpeciesRecommendation] }>()
</script>

<template>
  <article class="flex h-full flex-col overflow-hidden rounded-box border border-amma-green-light bg-white shadow-sm">
    <div class="flex h-40 items-center justify-center bg-amma-green-lighter">
      <img v-if="species.fotoUrl" :src="species.fotoUrl" :alt="`Foto de ${species.nomePopular}`" class="h-full w-full object-cover" />
      <div v-else class="flex flex-col items-center gap-2 text-amma-green-dark" aria-label="Foto indisponível">
        <span class="material-symbols-outlined text-5xl" aria-hidden="true">park</span>
        <span class="text-xs font-semibold">Foto indisponível</span>
      </div>
    </div>
    <div class="flex flex-1 flex-col p-5">
      <span class="inline-flex w-fit items-center gap-1 rounded-full border border-feedback-success-border bg-feedback-success-bg px-3 py-1 text-xs font-bold text-feedback-success">
        <span class="material-symbols-outlined text-base" aria-hidden="true">check_circle</span>
        Adequada às condições informadas
      </span>
      <h2 class="mt-4 text-lg font-bold text-amma-green-dark">{{ species.nomePopular }}</h2>
      <p class="mt-1 text-sm italic text-grey-text">{{ species.nomeCientifico }}</p>
      <p class="mt-4 text-sm font-semibold text-grey-text">Porte {{ species.porte }}</p>
      <button
        type="button"
        class="mt-auto rounded-btn border-2 border-amma-green bg-white px-4 py-2.5 font-bold text-amma-green transition-colors hover:bg-amma-green-lighter focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-amma-green"
        @click="$emit('select', species)"
      >Solicitar Muda</button>
    </div>
  </article>
</template>
