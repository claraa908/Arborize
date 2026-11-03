<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useFeedbackStore } from '@/stores/feedbackStore'
import { useAssistentePlantioStore } from '@/stores/assistentePlantioStore'
import { useSolicitacaoMudaStore } from '@/stores/solicitacaoMudaStore'

const route = useRoute()
const router = useRouter()
const feedback = useFeedbackStore()
const quizStore = useAssistentePlantioStore()
const store = useSolicitacaoMudaStore()
const speciesName = computed(() => store.resultado?.especie.nomePopular ?? quizStore.especieSelecionada?.nomePopular ?? 'espécie selecionada')

onMounted(async () => {
  if (!store.form.especieId) {
    await router.replace({ name: 'arborizacao-assistente', query: { aviso: 'especie-obrigatoria' } })
  } else if (!store.conversao) {
    await store.calcularConversao()
  }
})

async function voltar() {
  await router.push({ name: 'solicitar-muda', query: { especieId: route.query.especieId ?? store.form.especieId } })
}

async function enviar() {
  if (!(await store.enviarSolicitacao()) || !store.resultado) return
  feedback.showToast(`Solicitação enviada com sucesso. Protocolo ${store.resultado.protocolo}.`, 'success')
  await router.push({ name: 'arborizacao-acompanhar' })
}
</script>

<template>
  <section class="mx-auto max-w-5xl py-8 md:py-12" aria-labelledby="conversion-title">
    <header class="mb-7">
      <p class="text-xs font-bold uppercase tracking-wider text-amma-green">Eco Ponto</p>
      <h1 id="conversion-title" class="mt-2 text-2xl font-bold text-amma-green-dark">Conversão Eco Ponto</h1>
      <p class="mt-2 text-sm text-grey-text">Confira a contrapartida de recicláveis antes de enviar o pedido.</p>
    </header>

    <div v-if="store.isCalculating" class="rounded-box bg-white p-8 text-center text-grey-text" role="status" aria-live="polite">Calculando garrafas PET necessárias…</div>
    <div v-else-if="store.conversionError" class="rounded-box border border-feedback-danger-border bg-feedback-danger-bg p-5 text-feedback-danger" role="alert">
      <p>{{ store.conversionError }}</p>
      <button type="button" class="mt-4 rounded-btn border-2 border-amma-green bg-white px-5 py-2.5 font-bold text-amma-green focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-amma-green" @click="store.calcularConversao()">Tentar novamente</button>
    </div>
    <div v-else-if="store.conversao" class="grid grid-cols-1 gap-5 md:grid-cols-2">
      <div class="flex min-h-64 flex-col items-center justify-center rounded-box bg-amma-green p-8 text-center text-white">
        <span class="material-symbols-outlined text-4xl" aria-hidden="true">recycling</span>
        <p class="mt-3 text-lg font-bold">Para {{ store.conversao.quantidade }} {{ store.conversao.quantidade === 1 ? 'muda' : 'mudas' }} de {{ speciesName }}</p>
        <p class="mt-2 text-5xl font-bold" aria-live="polite">{{ store.conversao.totalGarrafas }}</p>
        <p class="text-lg">garrafas PET</p>
      </div>
      <div class="rounded-box border border-amma-green-light bg-white p-6 md:p-8">
        <h2 class="text-lg font-bold text-amma-green-dark">Resumo do pedido</h2>
        <dl class="mt-5 divide-y divide-amma-green-light text-sm">
          <div class="flex justify-between gap-4 py-3"><dt class="text-grey-text">Quantidade de mudas</dt><dd class="font-bold text-amma-green-dark">{{ store.conversao.quantidade }}</dd></div>
          <div class="flex justify-between gap-4 py-3"><dt class="text-grey-text">Espécie</dt><dd class="text-right font-bold text-amma-green-dark">{{ speciesName }}</dd></div>
          <div class="flex justify-between gap-4 py-3"><dt class="text-grey-text">Recicláveis necessários</dt><dd class="font-bold text-amma-green-dark">{{ store.conversao.totalGarrafas }} garrafas PET</dd></div>
        </dl>
      </div>
      <div class="flex flex-col-reverse gap-3 border-t border-amma-green-light pt-5 sm:flex-row sm:justify-between md:col-span-2">
        <button type="button" class="rounded-btn border-2 border-amma-green bg-white px-6 py-2.5 font-bold text-amma-green hover:bg-amma-green-lighter focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-amma-green" @click="voltar">Voltar</button>
        <button type="button" :disabled="store.isSubmitting" class="efeito-zoom rounded-btn bg-amma-green px-6 py-2.5 font-bold text-white hover:bg-amma-green-dark focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-amma-green disabled:cursor-not-allowed disabled:opacity-60" @click="enviar">
          {{ store.isSubmitting ? 'Enviando solicitação…' : 'Enviar Solicitação' }}
        </button>
      </div>
      <p v-if="store.submitError" class="md:col-span-2 rounded-btn border border-feedback-danger-border bg-feedback-danger-bg p-4 text-sm text-feedback-danger" role="alert">
        {{ store.submitError }}
        <button type="button" class="ml-2 font-bold underline" @click="enviar">Tentar novamente</button>
      </p>
    </div>
  </section>
</template>
