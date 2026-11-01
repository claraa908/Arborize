<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useSolicitacaoMudaStore } from '@/stores/solicitacaoMudaStore'

const router = useRouter()
const store = useSolicitacaoMudaStore()
const result = computed(() => store.resultado)

onMounted(() => {
  if (!store.resultado) void router.replace({ name: 'arborizacao-assistente' })
})

const timeline = [
  { title: 'Solicitação Enviada', description: 'Pedido registrado no Arborize.', state: 'concluída', icon: 'check_circle' },
  { title: 'Aguardando recicláveis', description: 'Entregue os materiais no Eco Ponto.', state: 'em andamento', icon: 'hourglass_top' },
  { title: 'Aprovação da AMMA', description: 'Será liberada após a conferência.', state: 'bloqueada', icon: 'lock' },
  { title: 'Retirada da muda', description: 'Apresente o comprovante no ponto de retirada.', state: 'bloqueada', icon: 'lock' }
]
</script>

<template>
  <section v-if="result" class="mx-auto max-w-5xl py-8 md:py-12" aria-labelledby="tracking-title">
    <header class="mb-7">
      <p class="text-xs font-bold uppercase tracking-wider text-amma-green">Solicitação de Muda</p>
      <h1 id="tracking-title" class="mt-2 text-2xl font-bold text-amma-green-dark">Acompanhar solicitação</h1>
      <p class="mt-2 text-sm text-grey-text">Veja o andamento do seu pedido e as próximas etapas.</p>
    </header>

    <div class="grid grid-cols-1 gap-5 md:grid-cols-2">
      <section class="rounded-box border border-amma-green-light bg-white p-6 shadow-sm md:p-8" aria-label="Andamento da solicitação">
        <div class="flex flex-wrap items-center justify-between gap-3">
          <h2 class="text-lg font-bold text-amma-green-dark">Protocolo {{ result.protocolo }}</h2>
          <span class="rounded-full border border-feedback-warning-border bg-feedback-warning-bg px-3 py-1 text-sm font-bold text-feedback-warning">Pendente de recicláveis</span>
        </div>
        <ol class="mt-6 space-y-5">
          <li v-for="(step, index) in timeline" :key="step.title" class="flex gap-3">
            <span class="material-symbols-outlined mt-0.5" :class="index === 0 ? 'text-feedback-success' : index === 1 ? 'text-feedback-warning' : 'text-grey-text'" aria-hidden="true">{{ step.icon }}</span>
            <div>
              <p class="font-bold text-amma-green-dark">{{ step.title }} <span class="text-xs font-semibold text-grey-text">— {{ step.state }}</span></p>
              <p class="mt-1 text-sm text-grey-text">{{ step.description }}</p>
            </div>
          </li>
        </ol>
      </section>

      <section class="rounded-box border border-amma-green-light bg-white p-6 shadow-sm md:p-8" aria-labelledby="order-summary-title">
        <h2 id="order-summary-title" class="text-lg font-bold text-amma-green-dark">Resumo do pedido</h2>
        <dl class="mt-4 divide-y divide-amma-green-light text-sm">
          <div class="flex justify-between gap-4 py-3"><dt class="text-grey-text">Solicitante</dt><dd class="text-right font-semibold text-amma-green-dark">{{ result.nomeSolicitante }}</dd></div>
          <div class="flex justify-between gap-4 py-3"><dt class="text-grey-text">Espécie</dt><dd class="text-right font-semibold text-amma-green-dark">{{ result.especie.nomePopular }}</dd></div>
          <div class="flex justify-between gap-4 py-3"><dt class="text-grey-text">Quantidade</dt><dd class="font-semibold text-amma-green-dark">{{ result.quantidadeMudas }}</dd></div>
          <div class="flex justify-between gap-4 py-3"><dt class="text-grey-text">Recicláveis</dt><dd class="font-semibold text-amma-green-dark">{{ result.totalRecicaveis }} garrafas PET</dd></div>
        </dl>
        <p class="mt-5 rounded-btn border border-feedback-info-border bg-feedback-info-bg p-4 text-sm text-feedback-info">Guarde o protocolo para referência. Esta tela é exibida a partir da confirmação do envio.</p>
      </section>
    </div>
  </section>
</template>
