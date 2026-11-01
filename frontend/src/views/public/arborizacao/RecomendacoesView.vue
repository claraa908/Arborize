<script setup lang="ts">
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'
import EspecieRecomendadaCard from '@/components/arborizacao/EspecieRecomendadaCard.vue'
import { useAssistentePlantioStore } from '@/stores/assistentePlantioStore'
import type { SpeciesRecommendation } from '@/types/assistentePlantio'

const router = useRouter()
const store = useAssistentePlantioStore()

onMounted(() => {
  if (store.recomendacoes === null && !store.errorMessage) void store.carregarRecomendacoes()
})

async function refazer() {
  store.refazerQuiz()
  await router.push({ name: 'arborizacao-assistente' })
}

function selecionar(species: SpeciesRecommendation) {
  store.selecionarEspecie(species)
  void router.push({ name: 'solicitar-muda', query: { especieId: species.id } })
}
</script>

<template>
  <section class="mx-auto max-w-6xl py-8 md:py-12" aria-labelledby="recommendations-title">
    <header class="mb-7">
      <p class="text-xs font-bold uppercase tracking-wider text-amma-green">Resultado do assistente</p>
      <h1 id="recommendations-title" class="mt-2 text-2xl font-bold text-amma-green-dark">Espécies recomendadas</h1>
      <p class="mt-2 text-sm text-grey-text">Com base nas suas respostas, estas espécies são as mais adequadas para o local informado.</p>
    </header>

    <div v-if="store.isLoading" class="grid grid-cols-1 gap-5 md:grid-cols-3" role="status" aria-live="polite">
      <div v-for="item in 3" :key="item" class="animate-pulse overflow-hidden rounded-box border border-amma-green-light bg-white">
        <div class="h-40 bg-amma-green-lighter"></div>
        <div class="space-y-3 p-5"><div class="h-4 w-2/3 rounded bg-amma-green-light"></div><div class="h-4 w-1/2 rounded bg-amma-green-light"></div></div>
      </div>
      <span class="sr-only">Buscando recomendações.</span>
    </div>

    <div v-else-if="store.errorMessage" class="rounded-box border border-feedback-danger-border bg-feedback-danger-bg p-5 text-feedback-danger" role="alert">
      <p class="font-semibold">{{ store.errorMessage }}</p>
      <button type="button" class="mt-4 rounded-btn border-2 border-amma-green bg-white px-5 py-2.5 font-bold text-amma-green hover:bg-amma-green-lighter focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-amma-green" @click="store.carregarRecomendacoes()">Tentar novamente</button>
    </div>

    <div v-else-if="store.recomendacoes?.length" class="grid grid-cols-1 gap-5 md:grid-cols-2 lg:grid-cols-3">
      <EspecieRecomendadaCard v-for="species in store.recomendacoes.slice(0, 3)" :key="species.id" :species="species" @select="selecionar" />
    </div>

    <div v-else class="rounded-box border border-amma-green-light bg-white p-8 text-center">
      <span class="material-symbols-outlined text-4xl text-amma-green" aria-hidden="true">park</span>
      <p v-if="store.respostas.largura_calcada === 'MENOR_1_5M' && store.respostas.local_plantio === 'CALCADA'" class="mt-3 text-lg font-bold text-amma-green-dark">
        Calçadas com menos de 1,5m não são indicadas para o plantio de árvores. Considere plantar no quintal ou usar vasos.
      </p>
      <p v-else class="mt-3 text-lg font-bold text-amma-green-dark">Não encontramos espécies adequadas para essas condições.</p>
      <button type="button" class="mt-5 rounded-btn border-2 border-amma-green bg-white px-5 py-2.5 font-bold text-amma-green hover:bg-amma-green-lighter focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-amma-green" @click="refazer">Refazer Quiz</button>
    </div>

    <div class="mt-6 flex flex-col items-center gap-3">
      <button v-if="!store.isLoading && !store.errorMessage && store.recomendacoes?.length" type="button" class="rounded-btn border-2 border-amma-green bg-white px-5 py-2.5 font-bold text-amma-green hover:bg-amma-green-lighter focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-amma-green" @click="refazer">⟳ Refazer Quiz</button>
      <p class="text-xs text-grey-text">As recomendações são calculadas pelo catálogo municipal.</p>
    </div>
  </section>
</template>
