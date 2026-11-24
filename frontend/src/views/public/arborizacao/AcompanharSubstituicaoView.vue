<script setup lang="ts">
import { ref } from 'vue'
import { useRoute } from 'vue-router'
import { useFeedbackStore } from '@/stores/feedbackStore'
import AcompanharRequerimentoForm from '@/components/arborizacao/AcompanharRequerimentoForm.vue'
import { substituicaoBotanicaService } from '@/services/substituicaoBotanicaService'
import type { SubstituicaoBotanicaStatus, SubstituicaoRastreio } from '@/types/substituicaoBotanica'
import { isValidCpf } from '@/utils/cpf'

const route = useRoute()
const feedback = useFeedbackStore()
const protocolo = ref(typeof route.query.protocolo === 'string' ? route.query.protocolo : '')
const cpf = ref('')
const request = ref<SubstituicaoRastreio | null>(null)
const carregando = ref(false)
const erro = ref('')
const protocoloCopiado = ref(false)
const labels: Record<SubstituicaoBotanicaStatus, string> = {
  RECEBIDA: 'Recebida pela AMMA',
  EM_ANALISE: 'Em análise',
  APROVADA: 'Aprovada',
  REJEITADA: 'Não aprovada',
  LAUDO_EMITIDO: 'Laudo emitido',
  RETIRADA_EM_ANDAMENTO: 'Retirada em andamento',
  REPLANTIO_EM_ANDAMENTO: 'Reposição em andamento',
  CONCLUIDA: 'Concluída'
}

async function acompanhar() {
  erro.value = ''
  request.value = null
  if (!isValidCpf(cpf.value)) {
    erro.value = 'Informe um CPF válido para consultar o requerimento.'
    return
  }
  carregando.value = true
  try {
    request.value = await substituicaoBotanicaService.acompanhar({ protocolo: protocolo.value, cpf: cpf.value })
  } catch {
    erro.value = 'Não encontramos um requerimento com esses dados. Confira o protocolo e o CPF.'
    feedback.showToast(erro.value, 'error')
  } finally {
    carregando.value = false
  }
}

async function copiarProtocolo() {
  const protocoloAtual = request.value?.protocolo ?? protocolo.value
  if (!protocoloAtual) return
  try {
    await navigator.clipboard.writeText(protocoloAtual)
    protocoloCopiado.value = true
    feedback.showToast('Protocolo copiado para a área de transferência.', 'success')
  } catch {
    protocoloCopiado.value = false
    feedback.showToast('Não foi possível copiar o protocolo neste dispositivo.', 'error')
  }
}

function formatarData(value: string) {
  return new Intl.DateTimeFormat('pt-BR', { dateStyle: 'short', timeStyle: 'short' }).format(new Date(value))
}

</script>

<template>
  <section class="mx-auto w-full max-w-5xl min-w-0 py-6 md:py-10" aria-labelledby="tracking-substitution-title">
    <header class="mb-6">
      <RouterLink :to="{ name: 'arborizacao' }" class="inline-flex min-h-11 items-center gap-2 rounded-btn px-2 text-sm font-semibold text-amma-green-dark focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-amma-green"><span class="material-symbols-outlined" aria-hidden="true">arrow_back</span> Arborização</RouterLink>
      <p class="mt-4 text-xs font-bold uppercase tracking-wider text-amma-green">Substituição botânica</p>
      <h1 id="tracking-substitution-title" class="mt-2 text-2xl font-bold text-amma-green-dark">Acompanhar requerimento</h1>
      <p class="mt-2 text-sm text-grey-text">Informe o protocolo e o CPF usado no envio. A consulta mostra apenas o andamento público do pedido.</p>
    </header>

    <AcompanharRequerimentoForm
      v-model:protocol="protocolo"
      v-model:cpf="cpf"
      :loading="carregando"
      :copied="protocoloCopiado"
      protocol-placeholder="NIM-2026-12345678"
      submit-label="Consultar"
      @submit="acompanhar"
      @copy-protocol="copiarProtocolo"
    />

    <div v-if="erro" class="mt-4 rounded-btn border border-feedback-danger-border bg-feedback-danger-bg px-4 py-3 text-sm text-feedback-danger" role="alert">{{ erro }}</div>

    <div v-if="request" class="mt-5 grid min-w-0 grid-cols-1 gap-5 md:grid-cols-[minmax(0,1.25fr)_minmax(0,0.75fr)]">
      <section class="min-w-0 rounded-box border border-amma-green-light bg-white p-5 shadow-sm md:p-7" aria-labelledby="timeline-title">
        <div class="flex min-w-0 flex-wrap items-start justify-between gap-3">
          <div class="min-w-0"><p class="text-xs font-bold uppercase tracking-wider text-grey-text">Protocolo</p><div class="mt-1 flex min-w-0 items-center gap-2"><h2 class="min-w-0 flex-1 break-all text-lg font-bold text-amma-green-dark">{{ request.protocolo }}</h2><button type="button" class="inline-flex h-9 w-9 shrink-0 items-center justify-center rounded-full text-amma-green hover:bg-amma-green-lighter focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-amma-green" :aria-label="protocoloCopiado ? 'Protocolo copiado' : 'Copiar protocolo'" @click="copiarProtocolo"><span class="material-symbols-outlined text-xl" aria-hidden="true">{{ protocoloCopiado ? 'check' : 'content_copy' }}</span></button><span class="sr-only" aria-live="polite">{{ protocoloCopiado ? 'Protocolo copiado para a área de transferência.' : '' }}</span></div></div>
          <span class="max-w-full rounded-full border border-amma-green-light bg-amma-green-lighter px-3 py-1 text-center text-sm font-bold text-amma-green-dark">{{ labels[request.status] }}</span>
        </div>
        <h3 id="timeline-title" class="mt-6 text-lg font-bold text-amma-green-dark">Andamento pela AMMA</h3>
        <ol class="mt-5 space-y-5">
          <li v-for="(event, index) in request.eventos" :key="`${event.status}-${event.criadoEm}`" class="flex min-w-0 gap-3">
            <span class="material-symbols-outlined mt-0.5 shrink-0" :class="event.status === 'REJEITADA' ? 'text-feedback-danger' : index === request.eventos.length - 1 ? 'text-amma-green' : 'text-feedback-success'" aria-hidden="true">{{ event.status === 'REJEITADA' ? 'cancel' : index === request.eventos.length - 1 ? 'radio_button_checked' : 'check_circle' }}</span>
            <div class="min-w-0 flex-1"><p class="break-words font-bold text-amma-green-dark">{{ labels[event.status] }}</p><p class="mt-1 break-words text-sm leading-5 text-grey-text">{{ event.descricao }}</p><time class="mt-1 block text-xs text-grey-text" :datetime="event.criadoEm">{{ formatarData(event.criadoEm) }}</time></div>
          </li>
        </ol>
      </section>
      <section class="min-w-0 rounded-box border border-amma-green-light bg-white p-5 shadow-sm md:p-7" aria-labelledby="tracking-summary-title">
        <h2 id="tracking-summary-title" class="text-lg font-bold text-amma-green-dark">Resumo do requerimento</h2>
        <dl class="mt-4 space-y-3 text-sm">
          <div class="flex min-w-0 justify-between gap-3 border-b border-amma-green-light pb-3"><dt class="shrink-0 text-grey-text">Enviado em</dt><dd class="text-right font-semibold text-amma-green-dark">{{ formatarData(request.criadaEm) }}</dd></div>
          <div class="flex min-w-0 justify-between gap-3 border-b border-amma-green-light pb-3"><dt class="shrink-0 text-grey-text">Bairro</dt><dd class="break-words text-right font-semibold text-amma-green-dark">{{ request.bairro }}</dd></div>
          <div class="min-w-0 border-b border-amma-green-light pb-3"><dt class="text-grey-text">Árvore informada</dt><dd class="mt-1 break-words font-semibold text-amma-green-dark">{{ request.descricaoArvore }}</dd></div>
          <div v-if="request.especieReposicao" class="flex min-w-0 justify-between gap-3"><dt class="text-grey-text">Espécie de reposição</dt><dd class="break-words text-right font-semibold text-amma-green-dark">{{ request.especieReposicao }}</dd></div>
        </dl>
        <p class="mt-5 rounded-btn border border-feedback-info-border bg-feedback-info-bg p-4 text-sm leading-5 text-feedback-info">A AMMA atualiza o andamento após cada etapa. Guarde seu protocolo para consultar novamente.</p>
      </section>
    </div>
  </section>
</template>
