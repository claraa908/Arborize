<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useFeedbackStore } from '@/stores/feedbackStore'
import AcompanharRequerimentoForm from '@/components/arborizacao/AcompanharRequerimentoForm.vue'
import { useAssistentePlantioStore } from '@/stores/assistentePlantioStore'
import { useSolicitacaoMudaStore } from '@/stores/solicitacaoMudaStore'
import { solicitacaoMudaService } from '@/services/solicitacaoMudaService'
import { solicitacaoStatus, type SolicitacaoMudaResponse } from '@/types/solicitacaoMuda'
import { isValidCpf } from '@/utils/cpf'

const route = useRoute()
const router = useRouter()
const feedback = useFeedbackStore()
const quizStore = useAssistentePlantioStore()
const store = useSolicitacaoMudaStore()
const resultadoConsultado = ref<SolicitacaoMudaResponse | null>(null)
const protocolo = ref(typeof route.query.protocolo === 'string' ? route.query.protocolo : '')
const cpf = ref('')
const erro = ref('')
const carregando = ref(false)
const protocolCopied = ref(false)
const result = computed(() => {
  if (resultadoConsultado.value) return resultadoConsultado.value
  const protocolFromRoute = typeof route.query.protocolo === 'string' ? route.query.protocolo : null
  return protocolFromRoute && store.resultado?.protocolo === protocolFromRoute ? store.resultado : null
})

onMounted(() => {
  if (result.value) protocolo.value = result.value.protocolo
})

const estadosEtapa = {
  concluida: { label: 'concluída', icon: 'check_circle', color: 'text-feedback-success' },
  atual: { label: 'em andamento', icon: 'hourglass_top', color: 'text-feedback-warning' },
  futura: { label: 'próxima etapa', icon: 'lock', color: 'text-grey-text' },
  rejeitada: { label: 'encerrada', icon: 'cancel', color: 'text-feedback-danger' }
}
const timeline = computed(() => {
  const status = result.value?.status
  const enviada = { title: 'Solicitação enviada', description: 'Pedido registrado no Arborize.', state: estadosEtapa.concluida }
  if (status === 'REJEITADA') {
    return [enviada, { title: 'Solicitação rejeitada', description: 'A solicitação foi encerrada.', state: estadosEtapa.rejeitada }]
  }
  const entregue = status === 'RETIRADA_CONCLUIDA'
  const aprovada = status === 'APROVADA' || entregue
  return [
    enviada,
    { title: 'Entrega de recicláveis', description: aprovada ? 'Garrafas PET conferidas pela AMMA.' : 'Entregue as garrafas PET na AMMA.', state: aprovada ? estadosEtapa.concluida : estadosEtapa.atual },
    { title: 'Aprovação do pedido', description: aprovada ? 'A AMMA aprovou o pedido e liberou o documento de retirada.' : 'A AMMA aprova após conferir as garrafas PET.', state: aprovada ? estadosEtapa.concluida : estadosEtapa.futura },
    { title: 'Retirada da muda', description: entregue ? 'Entrega da muda registrada.' : aprovada ? 'Pronta para retirada. Apresente no IFCE o documento emitido pela AMMA.' : 'Disponível após a aprovação.', state: entregue ? estadosEtapa.concluida : aprovada ? estadosEtapa.atual : estadosEtapa.futura }
  ]
})

async function acompanhar() {
  erro.value = ''
  resultadoConsultado.value = null
  if (!isValidCpf(cpf.value)) {
    erro.value = 'Informe um CPF válido para consultar o pedido.'
    return
  }

  carregando.value = true
  try {
    resultadoConsultado.value = await solicitacaoMudaService.acompanharSolicitacao({
      protocolo: protocolo.value,
      cpf: cpf.value
    })
  } catch {
    erro.value = 'Não encontramos uma solicitação com esses dados. Confira o protocolo e o CPF.'
    feedback.showToast(erro.value, 'error')
  } finally {
    carregando.value = false
  }
}

async function copiarProtocolo() {
  const protocol = result.value?.protocolo ?? protocolo.value
  if (!protocol) return
  try {
    await navigator.clipboard.writeText(protocol)
    protocolCopied.value = true
    feedback.showToast('Protocolo copiado para a área de transferência.', 'success')
  } catch {
    protocolCopied.value = false
    feedback.showToast('Não foi possível copiar o protocolo neste dispositivo.', 'error')
  }
}

function resetFluxo() {
  resultadoConsultado.value = null
  store.resetFluxo()
  quizStore.resetFluxo()
}

async function solicitarOutraMuda() {
  resetFluxo()
  await router.push({ name: 'arborizacao-assistente' })
}

async function voltarAoInicio() {
  resetFluxo()
  await router.push({ name: 'arborizacao' })
}
</script>

<template>
  <section class="mx-auto w-full max-w-5xl min-w-0 py-6 md:py-10" aria-labelledby="tracking-title">
    <header class="mb-7 min-w-0">
      <RouterLink :to="{ name: 'arborizacao' }" class="inline-flex min-h-11 items-center gap-2 rounded-btn px-2 text-sm font-semibold text-amma-green-dark focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-amma-green" @click="resetFluxo">
        <span class="material-symbols-outlined" aria-hidden="true">arrow_back</span> Arborização
      </RouterLink>
      <p class="mt-4 text-xs font-bold uppercase tracking-wider text-amma-green">Solicitação de muda</p>
      <h1 id="tracking-title" class="mt-2 text-2xl font-bold text-amma-green-dark">Acompanhar pedido de muda</h1>
      <p class="mt-2 text-sm text-grey-text">Consulte o andamento usando o protocolo e o CPF informado no pedido.</p>
    </header>

    <AcompanharRequerimentoForm
      v-if="!result"
      v-model:protocol="protocolo"
      v-model:cpf="cpf"
      :loading="carregando"
      :copied="protocolCopied"
      protocol-placeholder="SOL-2026-0001"
      submit-label="Consultar pedido"
      @submit="acompanhar"
      @copy-protocol="copiarProtocolo"
    />

    <div v-if="erro" class="mt-4 rounded-btn border border-feedback-danger-border bg-feedback-danger-bg px-4 py-3 text-sm text-feedback-danger" role="alert">{{ erro }}</div>

    <div v-if="result" class="grid min-w-0 grid-cols-1 gap-5 md:grid-cols-2">
      <section class="min-w-0 rounded-box border border-amma-green-light bg-white p-5 shadow-sm md:p-7" aria-label="Andamento da solicitação">
        <div class="flex min-w-0 flex-wrap items-start justify-between gap-3">
          <div class="min-w-0">
            <p class="text-xs font-bold uppercase tracking-wider text-grey-text">Protocolo</p>
            <div class="mt-1 flex min-w-0 items-center gap-2">
              <h2 class="min-w-0 flex-1 break-all text-lg font-bold text-amma-green-dark">{{ result.protocolo }}</h2>
              <button type="button" class="inline-flex h-9 w-9 shrink-0 items-center justify-center rounded-full text-amma-green hover:bg-amma-green-lighter focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-amma-green" :aria-label="protocolCopied ? 'Protocolo copiado' : 'Copiar protocolo'" @click="copiarProtocolo">
                <span class="material-symbols-outlined text-xl" aria-hidden="true">{{ protocolCopied ? 'check' : 'content_copy' }}</span>
              </button>
              <span class="sr-only" aria-live="polite">{{ protocolCopied ? 'Protocolo copiado para a área de transferência.' : '' }}</span>
            </div>
          </div>
          <span class="max-w-full rounded-full border px-3 py-1 text-center text-sm font-bold" :class="solicitacaoStatus[result.status].badge">{{ solicitacaoStatus[result.status].label }}</span>
        </div>
        <ol class="mt-6 space-y-5">
          <li v-for="step in timeline" :key="step.title" class="flex min-w-0 gap-3">
            <span class="material-symbols-outlined mt-0.5 shrink-0" :class="step.state.color" aria-hidden="true">{{ step.state.icon }}</span>
            <div class="min-w-0">
              <p class="break-words font-bold text-amma-green-dark">{{ step.title }} <span class="text-xs font-semibold text-grey-text">— {{ step.state.label }}</span></p>
              <p class="mt-1 break-words text-sm text-grey-text">{{ step.description }}</p>
            </div>
          </li>
        </ol>
      </section>

      <section class="min-w-0 rounded-box border border-amma-green-light bg-white p-5 shadow-sm md:p-7" aria-labelledby="order-summary-title">
        <h2 id="order-summary-title" class="text-lg font-bold text-amma-green-dark">Resumo do pedido</h2>
        <dl class="mt-4 divide-y divide-amma-green-light text-sm">
          <div class="flex min-w-0 justify-between gap-4 py-3"><dt class="shrink-0 text-grey-text">Solicitante</dt><dd class="break-words text-right font-semibold text-amma-green-dark">{{ result.nomeSolicitante }}</dd></div>
          <div class="flex min-w-0 justify-between gap-4 py-3"><dt class="shrink-0 text-grey-text">Espécie</dt><dd class="break-words text-right font-semibold text-amma-green-dark">{{ result.especie.nomePopular }}</dd></div>
          <div class="flex min-w-0 justify-between gap-4 py-3"><dt class="shrink-0 text-grey-text">Quantidade</dt><dd class="font-semibold text-amma-green-dark">{{ result.quantidadeMudas }}</dd></div>
          <div class="flex min-w-0 justify-between gap-4 py-3"><dt class="shrink-0 text-grey-text">Recicláveis</dt><dd class="font-semibold text-amma-green-dark">{{ result.totalRecicaveis }} garrafas PET</dd></div>
        </dl>
        <p class="mt-5 rounded-btn border border-feedback-info-border bg-feedback-info-bg p-4 text-sm text-feedback-info">A solicitação e o status são consultados no cadastro do Arborize. Guarde o protocolo para consultas futuras.</p>
      </section>
    </div>

    <div v-if="result" class="mt-7 flex flex-col-reverse gap-3 border-t border-amma-green-light pt-5 sm:flex-row sm:items-center sm:justify-between">
      <button type="button" class="rounded-btn border-2 border-amma-green bg-white px-5 py-2.5 font-bold text-amma-green hover:bg-amma-green-lighter focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-amma-green" @click="solicitarOutraMuda">Solicitar outra muda</button>
      <button type="button" class="rounded-btn bg-amma-green px-6 py-2.5 font-bold text-white hover:bg-amma-green-dark focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-amma-green" @click="voltarAoInicio">Voltar para Arborização</button>
    </div>
  </section>
</template>
