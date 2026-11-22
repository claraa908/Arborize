<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import AppBreadcrumb from '@/components/ui/AppBreadcrumb.vue'
import AppDataTable from '@/components/ui/AppDataTable.vue'
import { useFeedbackStore } from '@/stores/feedbackStore'
import { useSubstituicaoBotanicaStore } from '@/stores/substituicaoBotanicaStore'
import { substituicaoBotanicaService } from '@/services/substituicaoBotanicaService'
import { especieService } from '@/services/especieService'
import type { EspecieDTO } from '@/types/viveiro'
import type { SubstituicaoBotanicaStatus, SubstituicaoGestao } from '@/types/substituicaoBotanica'

const feedback = useFeedbackStore()
const store = useSubstituicaoBotanicaStore()
const search = ref('')
const selectedId = ref<number | null>(null)
const view = ref<'list' | 'details' | 'report' | 'progress'>('list')
const species = ref<EspecieDTO[]>([])
const loadingSpecies = ref(false)
const selectedFile = ref<File | null>(null)
const photoUrl = ref<string | null>(null)
const formError = ref('')
const reportForm = reactive({
  especieReposicaoId: '',
  parecer: 'Favorável à supressão',
  tipoJustificativa: 'Espécie exótica invasora',
  justificativa: '',
  dataVistoria: '',
  arquivo: null as File | null
})
const progressForm = reactive({
  status: '' as SubstituicaoBotanicaStatus | '',
  descricao: ''
})
const columns = [
  { key: 'protocolo', label: 'Protocolo' },
  { key: 'solicitante', label: 'Solicitante' },
  { key: 'local', label: 'Localização' },
  { key: 'criadaEm', label: 'Enviado em' },
  { key: 'status', label: 'Status', align: 'center' as const },
  { key: 'acoes', label: 'Ações', align: 'right' as const }
]
const labels: Record<SubstituicaoBotanicaStatus, string> = {
  RECEBIDA: 'Recebida',
  EM_ANALISE: 'Em análise',
  APROVADA: 'Aprovada',
  REJEITADA: 'Não aprovada',
  LAUDO_EMITIDO: 'Laudo emitido',
  RETIRADA_EM_ANDAMENTO: 'Retirada em andamento',
  REPLANTIO_EM_ANDAMENTO: 'Reposição em andamento',
  CONCLUIDA: 'Concluída'
}
const selected = computed(() => store.selected)
const filteredRequests = computed(() => {
  const term = search.value.trim().toLocaleLowerCase('pt-BR')
  return store.requests.filter((request) => [request.protocolo, request.nomeSolicitante, request.bairro, request.logradouro, labels[request.status]]
    .some((value) => value.toLocaleLowerCase('pt-BR').includes(term)))
})
const countReceived = computed(() => store.requests.filter((item) => item.status === 'RECEBIDA' || item.status === 'EM_ANALISE').length)
const countReports = computed(() => store.requests.filter((item) => item.nomePdf).length)
const nativeSpecies = computed(() => species.value.filter((item) => item.categoria.normalize('NFD').replace(/\p{M}/gu, '').toUpperCase() === 'NATIVA DA CAATINGA'))
const nextProgress = computed<SubstituicaoBotanicaStatus | null>(() => {
  if (selected.value?.status === 'LAUDO_EMITIDO') return 'RETIRADA_EM_ANDAMENTO'
  if (selected.value?.status === 'RETIRADA_EM_ANDAMENTO') return 'REPLANTIO_EM_ANDAMENTO'
  if (selected.value?.status === 'REPLANTIO_EM_ANDAMENTO') return 'CONCLUIDA'
  return null
})
const fieldClass = 'mt-1 w-full min-w-0 rounded-btn border-2 border-amma-green-light bg-white px-4 py-3 text-sm font-medium text-grey-text outline-none transition-all focus:border-amma-green focus:bg-amma-green-lighter focus-visible:ring-2 focus-visible:ring-amma-green'

onMounted(async () => {
  await store.carregar()
  loadingSpecies.value = true
  try {
    species.value = await especieService.getAll()
  } catch {
    species.value = []
  } finally {
    loadingSpecies.value = false
  }
})

onBeforeUnmount(() => {
  if (photoUrl.value) URL.revokeObjectURL(photoUrl.value)
})

function formatDate(value: string) {
  return new Intl.DateTimeFormat('pt-BR', { dateStyle: 'short' }).format(new Date(value))
}

function statusClass(status: SubstituicaoBotanicaStatus) {
  if (status === 'REJEITADA') return 'border-feedback-danger-border bg-feedback-danger-bg text-feedback-danger'
  if (status === 'APROVADA' || status === 'LAUDO_EMITIDO' || status === 'CONCLUIDA') return 'border-feedback-success-border bg-feedback-success-bg text-feedback-success'
  return 'border-feedback-warning-border bg-feedback-warning-bg text-feedback-warning'
}

async function abrirDetalhes(request: SubstituicaoGestao) {
  if (photoUrl.value) URL.revokeObjectURL(photoUrl.value)
  photoUrl.value = null
  selectedId.value = request.id
  view.value = 'details'
  await store.carregarDetalhe(request.id)
  if (store.selected?.nomeFoto) {
    try {
      const blob = await substituicaoBotanicaService.carregarFoto(request.id)
      photoUrl.value = URL.createObjectURL(blob)
    } catch {
      feedback.showToast('Não foi possível abrir a fotografia anexada.', 'error')
    }
  }
}

async function iniciarAnalise() {
  if (!selected.value) return
  if (await store.iniciarAnalise(selected.value.id)) feedback.showToast('Análise iniciada.', 'success')
}

async function decidir(status: 'APROVADA' | 'REJEITADA') {
  if (!selected.value) return
  const confirm = await feedback.showConfirm({
    title: status === 'APROVADA' ? 'Aprovar requerimento?' : 'Rejeitar requerimento?',
    message: status === 'APROVADA' ? 'A aprovação ficará registrada no histórico acessível ao cidadão.' : 'A decisão ficará registrada e será apresentada ao cidadão.',
    confirmText: status === 'APROVADA' ? 'Aprovar' : 'Rejeitar',
    variant: status === 'REJEITADA' ? 'danger' : 'primary'
  })
  if (!confirm) return
  const ok = await store.decidir(selected.value.id, status)
  if (ok) feedback.showToast(status === 'APROVADA' ? 'Requerimento aprovado.' : 'Requerimento rejeitado.', 'success')
}

function abrirLaudo() {
  reportForm.especieReposicaoId = ''
  reportForm.parecer = 'Favorável à supressão'
  reportForm.tipoJustificativa = 'Espécie exótica invasora'
  reportForm.justificativa = ''
  reportForm.dataVistoria = ''
  reportForm.arquivo = null
  selectedFile.value = null
  formError.value = ''
  view.value = 'report'
}

function selecionarPdf(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0] ?? null
  formError.value = ''
  if (!file) {
    selectedFile.value = null
    reportForm.arquivo = null
    return
  }
  if (!file.name.toLowerCase().endsWith('.pdf') || file.type !== 'application/pdf') {
    formError.value = 'Selecione um arquivo PDF.'
    input.value = ''
    return
  }
  if (file.size > 10 * 1024 * 1024) {
    formError.value = 'O PDF deve ter no máximo 10 MB.'
    input.value = ''
    return
  }
  selectedFile.value = file
  reportForm.arquivo = file
}

async function emitirLaudo() {
  formError.value = ''
  if (!selected.value || !reportForm.especieReposicaoId || !reportForm.justificativa.trim() || !reportForm.dataVistoria || !reportForm.arquivo) {
    formError.value = 'Preencha os campos obrigatórios e anexe o PDF assinado.'
    return
  }
  const ok = await store.emitirLaudo(selected.value.id, {
    especieReposicaoId: Number(reportForm.especieReposicaoId),
    parecer: reportForm.parecer,
    tipoJustificativa: reportForm.tipoJustificativa,
    justificativa: reportForm.justificativa.trim(),
    dataVistoria: reportForm.dataVistoria,
    arquivo: reportForm.arquivo
  })
  if (ok) {
    feedback.showToast('Laudo emitido e vinculado ao requerimento.', 'success')
    view.value = 'details'
  } else formError.value = store.error ?? 'Não foi possível emitir o laudo.'
}

function abrirProgresso() {
  if (!nextProgress.value) return
  progressForm.status = nextProgress.value
  progressForm.descricao = ''
  formError.value = ''
  view.value = 'progress'
}

async function registrarProgresso() {
  if (!selected.value || !progressForm.status || !progressForm.descricao.trim()) {
    formError.value = 'Descreva a etapa realizada para atualizar o cidadão.'
    return
  }
  const ok = await store.registrarProgresso(selected.value.id, progressForm.status, progressForm.descricao.trim())
  if (ok) {
    feedback.showToast('Andamento atualizado.', 'success')
    view.value = 'details'
  } else formError.value = store.error ?? 'Não foi possível atualizar o andamento.'
}

async function baixarPdf() {
  if (!selected.value) return
  try {
    const blob = await substituicaoBotanicaService.baixarLaudo(selected.value.id)
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = selected.value.nomePdf ?? `${selected.value.protocolo}-laudo.pdf`
    link.click()
    URL.revokeObjectURL(url)
  } catch {
    feedback.showToast('Não foi possível baixar o laudo.', 'error')
  }
}

function voltarLista() {
  view.value = 'list'
  selectedId.value = null
  store.selected = null
}
</script>

<template>
  <section class="flex w-full flex-col" aria-labelledby="laudos-title">
    <AppBreadcrumb
      :items="[{ label: 'Início', routeName: 'dashboard', icon: 'home' }, { label: 'Laudos' }]"
      :show-back="false"
    />
    <div class="flex min-w-0 flex-col gap-8 px-6 py-8 md:px-10">
    <template v-if="view === 'list'">
      <header class="flex min-w-0 flex-col justify-between gap-4 sm:flex-row sm:items-center">
        <div class="min-w-0"><p class="mb-2 text-xs font-bold uppercase tracking-widest text-grey-text">Substituição botânica</p><h1 id="laudos-title" class="text-2xl font-bold text-amma-green-dark">Laudos técnicos</h1><p class="mt-1 max-w-3xl text-sm text-grey-text">Analise os requerimentos de retirada de Nim e acompanhe os laudos e as etapas de execução.</p></div>
        <button type="button" class="inline-flex min-h-11 shrink-0 items-center justify-center gap-2 rounded-btn border-2 border-amma-green bg-white px-5 py-2.5 text-sm font-bold text-amma-green hover:bg-amma-green-lighter focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-amma-green" @click="store.carregar"><span class="material-symbols-outlined" aria-hidden="true">refresh</span> Atualizar</button>
      </header>

      <div class="grid min-w-0 grid-cols-1 gap-4 md:grid-cols-3" aria-label="Resumo dos requerimentos">
        <article class="min-w-0 rounded-box border border-amma-green-light bg-white p-5 shadow-sm"><p class="text-sm font-semibold text-grey-text">Requerimentos</p><p class="mt-2 text-3xl font-bold text-amma-green-dark">{{ store.requests.length }}</p><p class="mt-2 text-xs text-grey-text">Registros recebidos pela AMMA</p></article>
        <article class="min-w-0 rounded-box border border-amma-green-light bg-white p-5 shadow-sm"><p class="text-sm font-semibold text-grey-text">Em análise</p><p class="mt-2 text-3xl font-bold text-amma-green-dark">{{ countReceived }}</p><p class="mt-2 text-xs text-grey-text">Aguardando decisão técnica</p></article>
        <article class="min-w-0 rounded-box border border-amma-green-light bg-white p-5 shadow-sm"><p class="text-sm font-semibold text-grey-text">Laudos emitidos</p><p class="mt-2 text-3xl font-bold text-amma-green-dark">{{ countReports }}</p><p class="mt-2 text-xs text-grey-text">PDFs assinados vinculados aos pedidos</p></article>
      </div>

      <section class="min-w-0 rounded-box border border-amma-green-light bg-white p-4 shadow-sm md:p-6" aria-labelledby="request-list-title">
        <div class="mb-5 flex min-w-0 flex-col gap-1"><h2 id="request-list-title" class="text-lg font-bold text-amma-green-dark">Requerimentos de retirada</h2><p class="text-sm text-grey-text">Pesquise por protocolo, solicitante, bairro ou status.</p></div>
        <div v-if="store.isLoading" class="space-y-3" role="status" aria-label="Carregando requerimentos"><div v-for="item in 3" :key="item" class="h-16 animate-pulse rounded-btn bg-amma-bg" /></div>
        <div v-else-if="store.error" class="rounded-btn border border-feedback-danger-border bg-feedback-danger-bg p-4 text-sm text-feedback-danger" role="alert"><p>{{ store.error }}</p><button type="button" class="mt-3 rounded-btn border border-feedback-danger px-4 py-2 font-bold focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-feedback-danger" @click="store.carregar">Tentar novamente</button></div>
        <div v-else-if="store.requests.length === 0" class="rounded-btn border border-dashed border-amma-green-light bg-amma-green-lighter p-8 text-center"><span class="material-symbols-outlined text-3xl text-amma-green" aria-hidden="true">inbox</span><p class="mt-2 font-semibold text-amma-green-dark">Nenhum requerimento recebido</p><p class="mt-1 text-sm text-grey-text">Os pedidos enviados pelos cidadãos aparecerão aqui.</p></div>
        <div v-else class="w-full max-w-full min-w-0">
          <AppDataTable
            class="w-full max-w-full min-w-0"
            v-model:search="search"
            :columns="columns"
            :items="filteredRequests"
            item-key="id"
            caption="Requerimentos de retirada de Nim"
            search-placeholder="Protocolo, solicitante ou bairro"
            empty-message="Nenhum requerimento corresponde à busca."
            :loading="store.isLoading"
            :show-search="true"
            :show-summary="true"
            :allow-view-toggle="true"
            default-view="grid"
          >
            <template #card="{ item }">
              <div class="flex min-w-0 flex-col gap-3">
                <div class="flex min-w-0 flex-wrap items-start justify-between gap-2"><div class="min-w-0"><p class="break-all text-xs font-bold uppercase tracking-wider text-grey-text">{{ item.protocolo }}</p><p class="mt-1 break-words font-bold text-amma-green-dark">{{ item.nomeSolicitante }}</p></div><span class="max-w-full rounded-full border px-2.5 py-1 text-center text-xs font-bold" :class="statusClass(item.status)">{{ labels[item.status] }}</span></div>
                <p class="break-words text-sm text-grey-text">{{ item.logradouro }}, {{ item.numeroEndereco }} · {{ item.bairro }}</p>
                <div class="flex flex-wrap items-center justify-between gap-2 border-t border-amma-green-light pt-3"><time class="text-xs text-grey-text" :datetime="item.criadaEm">Enviado {{ formatDate(item.criadaEm) }}</time><button type="button" class="min-h-10 rounded-btn bg-amma-green px-4 py-2 text-sm font-bold text-white hover:bg-amma-green-dark focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-amma-green" @click="abrirDetalhes(item)">Abrir requerimento</button></div>
              </div>
            </template>
            <template #solicitante="{ item }"><span class="block min-w-36 max-w-56 break-words text-sm font-medium text-grey-text">{{ item.nomeSolicitante }}</span></template>
            <template #local="{ item }"><span class="block min-w-40 max-w-56 break-words text-sm text-grey-text">{{ item.logradouro }}, {{ item.numeroEndereco }} · {{ item.bairro }}</span></template>
            <template #criadaEm="{ item }"><span class="whitespace-nowrap text-sm text-grey-text">{{ formatDate(item.criadaEm) }}</span></template>
            <template #status="{ item }"><span class="inline-flex max-w-40 rounded-full border px-2.5 py-1 text-center text-xs font-bold" :class="statusClass(item.status)">{{ labels[item.status] }}</span></template>
            <template #acoes="{ item }"><button type="button" class="min-h-10 rounded-btn border-2 border-amma-green px-3 py-1.5 text-sm font-bold text-amma-green hover:bg-amma-green-lighter focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-amma-green" @click="abrirDetalhes(item)">Abrir</button></template>
          </AppDataTable>
        </div>
      </section>
    </template>

    <template v-else-if="selected">
      <header class="flex min-w-0 flex-col justify-between gap-4 sm:flex-row sm:items-center"><div class="min-w-0"><p class="mb-2 text-xs font-bold uppercase tracking-widest text-grey-text">Laudos técnicos</p><h1 id="laudos-title" class="break-all text-2xl font-bold text-amma-green-dark">{{ view === 'report' ? 'Emitir laudo' : view === 'progress' ? 'Registrar andamento' : 'Requerimento ' + selected.protocolo }}</h1><p class="mt-1 text-sm text-grey-text">Dados do requerimento enviados pelo cidadão e persistidos no Arborize.</p></div><button type="button" class="min-h-11 shrink-0 rounded-btn border-2 border-amma-green bg-white px-4 py-2 font-bold text-amma-green hover:bg-amma-green-lighter focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-amma-green" @click="voltarLista">Voltar à lista</button></header>

      <div v-if="store.isLoading" class="h-48 animate-pulse rounded-box bg-amma-green-lighter" role="status" aria-label="Carregando requerimento" />
      <div v-else-if="store.error" class="rounded-btn border border-feedback-danger-border bg-feedback-danger-bg p-4 text-sm text-feedback-danger" role="alert">{{ store.error }} <button type="button" class="ml-2 underline" @click="abrirDetalhes(selected)">Tentar novamente</button></div>

      <template v-else-if="view === 'details'">
        <div class="grid min-w-0 grid-cols-1 gap-5 xl:grid-cols-[minmax(0,1.2fr)_minmax(18rem,0.8fr)]">
          <article class="min-w-0 rounded-box border border-amma-green-light bg-white p-5 shadow-sm md:p-7">
            <div class="flex min-w-0 flex-wrap items-start justify-between gap-3 border-b border-amma-green-light pb-4"><div class="min-w-0"><p class="break-all text-xs font-bold uppercase tracking-wider text-grey-text">{{ selected.protocolo }}</p><h2 class="mt-1 text-lg font-bold text-amma-green-dark">Dados do solicitante</h2></div><span class="rounded-full border px-3 py-1 text-sm font-bold" :class="statusClass(selected.status)">{{ labels[selected.status] }}</span></div>
            <dl class="mt-5 grid min-w-0 grid-cols-1 gap-4 sm:grid-cols-2">
              <div class="min-w-0"><dt class="text-xs font-bold uppercase tracking-wider text-grey-text">Nome completo</dt><dd class="mt-1 break-words font-semibold text-amma-green-dark">{{ selected.nomeSolicitante }}</dd></div>
              <div class="min-w-0"><dt class="text-xs font-bold uppercase tracking-wider text-grey-text">CPF / Telefone</dt><dd class="mt-1 break-words font-semibold text-amma-green-dark">{{ selected.cpf }} · {{ selected.telefone }}</dd></div>
              <div class="min-w-0 sm:col-span-2"><dt class="text-xs font-bold uppercase tracking-wider text-grey-text">Localização informada</dt><dd class="mt-1 break-words font-semibold text-amma-green-dark">{{ selected.logradouro }}, {{ selected.numeroEndereco }} · {{ selected.bairro }}<span v-if="selected.complemento"> · {{ selected.complemento }}</span></dd><dd class="mt-1 break-words text-sm text-grey-text">Referência: {{ selected.pontoReferencia }}</dd></div>
              <div class="min-w-0 sm:col-span-2"><dt class="text-xs font-bold uppercase tracking-wider text-grey-text">Descrição da árvore e motivo</dt><dd class="mt-1 whitespace-pre-wrap break-words text-sm leading-6 text-grey-text">{{ selected.descricaoArvore }}</dd></div>
              <div class="min-w-0 sm:col-span-2"><dt class="text-xs font-bold uppercase tracking-wider text-grey-text">Fotografia anexada</dt><dd v-if="photoUrl" class="mt-2"><img :src="photoUrl" :alt="`Fotografia anexada ao protocolo ${selected.protocolo}`" class="max-h-80 max-w-full rounded-box border border-amma-green-light object-contain" /></dd><dd v-else class="mt-1 text-sm text-grey-text">{{ selected.nomeFoto ?? 'Nenhuma fotografia enviada' }}</dd></div>
            </dl>
          </article>
          <aside class="min-w-0 rounded-box border border-amma-green-light bg-white p-5 shadow-sm md:p-7" aria-labelledby="actions-title">
            <h2 id="actions-title" class="text-lg font-bold text-amma-green-dark">Ações da AMMA</h2><p class="mt-1 text-sm text-grey-text">As atualizações são registradas no histórico acessível ao cidadão.</p>
            <div v-if="selected.nomePdf" class="mt-4 rounded-btn border border-feedback-success-border bg-feedback-success-bg p-4"><p class="break-words text-sm font-semibold text-feedback-success">Laudo: {{ selected.nomePdf }}</p><p class="mt-1 text-sm text-grey-text">{{ selected.especieReposicao }} · vistoria {{ selected.dataVistoria }}</p><button type="button" class="mt-3 min-h-10 rounded-btn border-2 border-amma-green bg-white px-4 py-2 text-sm font-bold text-amma-green focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-amma-green" @click="baixarPdf">Baixar PDF</button></div>
            <div class="mt-5 flex flex-col gap-3">
              <button v-if="selected.status === 'RECEBIDA'" type="button" :disabled="store.isSubmitting" class="min-h-11 rounded-btn bg-amma-green px-5 py-2.5 font-bold text-white hover:bg-amma-green-dark focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-amma-green disabled:opacity-60" @click="iniciarAnalise">Iniciar análise</button>
              <template v-if="selected.status === 'EM_ANALISE'"><button type="button" :disabled="store.isSubmitting" class="min-h-11 rounded-btn bg-amma-green px-5 py-2.5 font-bold text-white hover:bg-amma-green-dark focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-amma-green disabled:opacity-60" @click="decidir('APROVADA')">Aprovar requerimento</button><button type="button" :disabled="store.isSubmitting" class="min-h-11 rounded-btn border-2 border-feedback-danger-border bg-feedback-danger-bg px-5 py-2.5 font-bold text-feedback-danger focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-feedback-danger disabled:opacity-60" @click="decidir('REJEITADA')">Rejeitar requerimento</button></template>
              <button v-if="selected.status === 'APROVADA'" type="button" class="min-h-11 rounded-btn bg-amma-green px-5 py-2.5 font-bold text-white hover:bg-amma-green-dark focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-amma-green" @click="abrirLaudo">Emitir laudo assinado</button>
              <button v-if="nextProgress" type="button" class="min-h-11 rounded-btn bg-amma-green px-5 py-2.5 font-bold text-white hover:bg-amma-green-dark focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-amma-green" @click="abrirProgresso">{{ nextProgress === 'RETIRADA_EM_ANDAMENTO' ? 'Registrar retirada' : nextProgress === 'REPLANTIO_EM_ANDAMENTO' ? 'Registrar reposição' : 'Concluir processo' }}</button>
              <p v-if="['REJEITADA', 'CONCLUIDA'].includes(selected.status)" class="rounded-btn border border-amma-green-light bg-amma-bg p-4 text-sm text-grey-text">Este processo não possui novas etapas disponíveis.</p>
            </div>
          </aside>
        </div>
      </template>

      <form v-else-if="view === 'report'" class="min-w-0 rounded-box border border-amma-green-light bg-white p-5 shadow-sm md:p-7" @submit.prevent="emitirLaudo">
        <div class="mb-5 rounded-btn border border-feedback-info-border bg-feedback-info-bg px-4 py-3 text-sm text-feedback-info">O PDF e os dados do laudo serão vinculados ao requerimento {{ selected.protocolo }}.</div>
        <div class="grid min-w-0 grid-cols-1 gap-4 md:grid-cols-2">
          <div class="min-w-0"><label for="report-opinion" class="text-sm font-semibold text-grey-text">Parecer técnico *</label><select id="report-opinion" v-model="reportForm.parecer" required :class="fieldClass"><option>Favorável à supressão</option><option>Não favorável à supressão</option></select></div>
          <div class="min-w-0"><label for="report-reason-type" class="text-sm font-semibold text-grey-text">Motivo *</label><select id="report-reason-type" v-model="reportForm.tipoJustificativa" required :class="fieldClass"><option>Espécie exótica invasora</option><option>Risco estrutural</option><option>Justificativa biológica</option></select></div>
          <div class="min-w-0 md:col-span-2"><label for="report-justification" class="text-sm font-semibold text-grey-text">Justificativa técnica *</label><textarea id="report-justification" v-model.trim="reportForm.justificativa" required maxlength="5000" rows="4" :class="fieldClass" /></div>
          <div class="min-w-0"><label for="report-native-species" class="text-sm font-semibold text-grey-text">Espécie nativa de reposição *</label><select id="report-native-species" v-model="reportForm.especieReposicaoId" required :disabled="loadingSpecies || nativeSpecies.length === 0" :class="fieldClass"><option value="">{{ loadingSpecies ? 'Carregando espécies…' : nativeSpecies.length ? 'Selecione uma espécie' : 'Nenhuma espécie nativa cadastrada' }}</option><option v-for="item in nativeSpecies" :key="String(item.id)" :value="item.id">{{ item.nomePopular }} — {{ item.nomeCientifico }}</option></select></div>
          <div class="min-w-0"><label for="report-inspection-date" class="text-sm font-semibold text-grey-text">Data da vistoria *</label><input id="report-inspection-date" v-model="reportForm.dataVistoria" required type="date" :class="fieldClass" /></div>
          <div class="min-w-0 md:col-span-2"><label for="report-pdf" class="text-sm font-semibold text-grey-text">Laudo oficial assinado em PDF * (até 10 MB)</label><div class="mt-1 rounded-box border-2 border-dashed border-amma-green-light bg-amma-green-lighter p-5"><input id="report-pdf" type="file" accept="application/pdf,.pdf" required class="block w-full min-w-0 text-sm text-grey-text file:mr-4 file:rounded-btn file:border-0 file:bg-white file:px-4 file:py-2 file:font-bold file:text-amma-green-dark focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-amma-green" aria-describedby="report-pdf-name" @change="selecionarPdf" /><p id="report-pdf-name" class="mt-2 break-words text-xs text-grey-text">{{ selectedFile?.name ?? 'Selecione o documento assinado.' }}</p></div></div>
        </div>
        <p v-if="formError" class="mt-4 rounded-btn border border-feedback-danger-border bg-feedback-danger-bg p-3 text-sm text-feedback-danger" role="alert">{{ formError }}</p>
        <div class="mt-6 flex flex-col-reverse gap-3 border-t border-amma-green-light pt-5 sm:flex-row sm:justify-end"><button type="button" class="min-h-11 rounded-btn border-2 border-amma-green px-5 py-2.5 font-bold text-amma-green focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-amma-green" @click="view = 'details'">Cancelar</button><button type="submit" :disabled="store.isSubmitting || loadingSpecies" :aria-busy="store.isSubmitting" class="min-h-11 rounded-btn bg-amma-green px-6 py-2.5 font-bold text-white hover:bg-amma-green-dark focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-amma-green disabled:opacity-60">{{ store.isSubmitting ? 'Enviando…' : 'Emitir e vincular laudo' }}</button></div>
      </form>

      <form v-else class="min-w-0 rounded-box border border-amma-green-light bg-white p-5 shadow-sm md:p-7" @submit.prevent="registrarProgresso">
        <h2 class="text-lg font-bold text-amma-green-dark">{{ progressForm.status ? labels[progressForm.status] : 'Atualizar etapa' }}</h2><p class="mt-1 text-sm text-grey-text">Descreva o andamento para informar o cidadão.</p>
        <div class="mt-5 min-w-0"><label for="progress-description" class="text-sm font-semibold text-grey-text">Descrição do andamento *</label><textarea id="progress-description" v-model.trim="progressForm.descricao" required maxlength="500" rows="4" :class="fieldClass" /></div>
        <p v-if="formError" class="mt-4 rounded-btn border border-feedback-danger-border bg-feedback-danger-bg p-3 text-sm text-feedback-danger" role="alert">{{ formError }}</p>
        <div class="mt-6 flex flex-col-reverse gap-3 border-t border-amma-green-light pt-5 sm:flex-row sm:justify-end"><button type="button" class="min-h-11 rounded-btn border-2 border-amma-green px-5 py-2.5 font-bold text-amma-green focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-amma-green" @click="view = 'details'">Cancelar</button><button type="submit" :disabled="store.isSubmitting" class="min-h-11 rounded-btn bg-amma-green px-6 py-2.5 font-bold text-white hover:bg-amma-green-dark focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-amma-green disabled:opacity-60">{{ store.isSubmitting ? 'Salvando…' : 'Salvar andamento' }}</button></div>
      </form>
    </template>
    </div>
  </section>
</template>
