<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { useCompensacaoStore } from '@/stores/compensacaoStore'
import { useFeedbackStore } from '@/stores/feedbackStore'
import { compensacaoService } from '@/services/compensacaoService'
import AppBreadcrumb from '@/components/ui/AppBreadcrumb.vue'
import CompensacaoStatusBadge from '@/components/compensacao/CompensacaoStatusBadge.vue'

const route = useRoute()
const store = useCompensacaoStore()
const feedback = useFeedbackStore()
const termo = computed(() => store.termoAtual)
onMounted(() => { void store.carregarTermo(Number(route.params.id)) })

async function baixarDocumento(entregaId?: number) {
  try {
    const blob = await compensacaoService.baixarDocumento(Number(route.params.id), entregaId)
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = entregaId == null ? termo.value?.arquivoTermo ?? 'termo.pdf' : 'recibo'
    link.click()
    URL.revokeObjectURL(url)
  } catch (error) {
    feedback.showToast(error instanceof Error ? error.message : 'Não foi possível acessar o documento.', 'error')
  }
}
</script>

<template>
  <div class="px-6 md:px-10 pt-5 pb-8 flex flex-col gap-5">
    <AppBreadcrumb :items="[
      { label: 'Compensação Ambiental', routeName: 'compensacao-lista', icon: 'balance' },
      { label: termo?.numeroTermo ?? 'Termo' },
    ]" :show-back="false" />

    <div v-if="store.isLoading" class="rounded-box border border-amma-green-light bg-white p-8 text-center text-grey-text" role="status">Carregando o termo…</div>
    <div v-else-if="store.error" class="rounded-box border border-feedback-danger-border bg-feedback-danger-bg p-8 text-center" role="alert">
      <h1 class="text-xl font-bold text-amma-green-dark">Não foi possível carregar o termo</h1><p class="mt-2 text-sm text-grey-text">{{ store.error }}</p>
      <button type="button" class="mt-4 font-bold text-amma-green underline" @click="store.carregarTermo(Number(route.params.id))">Tentar novamente</button>
    </div>
    <div v-else-if="!termo" class="rounded-box border border-feedback-warning-border bg-feedback-warning-bg p-8 text-center" role="alert">
      <h1 class="text-xl font-bold text-amma-green-dark">Termo não encontrado</h1>
      <p class="mt-2 text-sm text-grey-text">O registro solicitado não está disponível.</p>
      <RouterLink :to="{ name: 'compensacao-lista' }" class="mt-5 inline-flex min-h-11 items-center rounded-btn bg-amma-green px-5 font-bold text-white">Voltar aos termos</RouterLink>
    </div>

    <template v-else>
      <header class="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <p class="text-sm font-semibold text-amma-green">{{ termo.numeroTermo }}</p>
          <h1 class="text-2xl md:text-3xl font-bold text-amma-green-dark mt-1">Termo de compensação</h1>
          <p class="text-sm md:text-base text-grey-text mt-1">{{ termo.razaoSocial }}</p>
        </div>
        <RouterLink :to="{ name: 'compensacao-editar', params: { id: termo.id } }" class="inline-flex min-h-11 items-center justify-center gap-2 rounded-btn border-2 border-amma-green bg-white px-5 font-bold text-amma-green hover:bg-amma-green-lighter focus-visible:outline focus-visible:outline-2 focus-visible:outline-amma-green">
          <span class="material-symbols-outlined" aria-hidden="true">edit</span>
          Consultar / editar
        </RouterLink>
      </header>

      <section class="grid grid-cols-1 sm:grid-cols-3 gap-4" aria-label="Resumo do acordo">
        <article class="rounded-box border border-amma-green-light bg-white p-5">
          <h2 class="text-sm font-semibold text-grey-text">Mudas acordadas</h2>
          <p class="mt-2 text-3xl font-bold text-amma-green-dark">{{ termo.quantidadeAcordada.toLocaleString('pt-BR') }}</p>
        </article>
        <article class="rounded-box border border-amma-green-light bg-white p-5">
          <h2 class="text-sm font-semibold text-grey-text">Mudas entregues</h2>
          <p class="mt-2 text-3xl font-bold text-amma-green-dark">{{ termo.quantidadeEntregue.toLocaleString('pt-BR') }}</p>
        </article>
        <article class="rounded-box border border-amma-green-light bg-white p-5">
          <h2 class="text-sm font-semibold text-grey-text">Status do acordo</h2>
          <CompensacaoStatusBadge :status="termo.status" />
          <p class="text-xs text-grey-text mt-2">Atualizado pelas entregas registradas ou pela equipe AMMA quando o acordo inclui outros insumos.</p>
        </article>
      </section>

      <section class="rounded-box border border-amma-green-light bg-white p-5 md:p-7">
        <h2 class="text-lg font-bold text-amma-green-dark">Dados do compromisso</h2>
        <dl class="mt-5 grid grid-cols-1 sm:grid-cols-2 gap-5">
          <div><dt class="text-xs font-semibold uppercase tracking-wider text-grey-text">Razão social</dt><dd class="mt-1 font-semibold text-grey-text">{{ termo.razaoSocial }}</dd></div>
          <div><dt class="text-xs font-semibold uppercase tracking-wider text-grey-text">CNPJ</dt><dd class="mt-1 font-semibold text-grey-text">{{ termo.cnpj }}</dd></div>
          <div><dt class="text-xs font-semibold uppercase tracking-wider text-grey-text">Data de assinatura</dt><dd class="mt-1 font-semibold text-grey-text">{{ termo.dataAssinatura }}</dd></div>
          <div><dt class="text-xs font-semibold uppercase tracking-wider text-grey-text">Prazo de entrega</dt><dd class="mt-1 font-semibold text-grey-text">{{ termo.prazoEntrega }}</dd></div>
        </dl>
        <div class="mt-6 border-t border-amma-green-light pt-5">
          <h3 class="text-sm font-bold text-amma-green-dark">Termo de Compromisso assinado</h3>
          <button type="button" class="mt-2 inline-flex min-h-10 items-center gap-2 text-sm font-semibold text-amma-green underline focus-visible:outline focus-visible:outline-2 focus-visible:outline-amma-green" @click="baixarDocumento()"><span class="material-symbols-outlined" aria-hidden="true">picture_as_pdf</span>{{ termo.arquivoTermo }} — baixar PDF</button>
        </div>
      </section>

      <section class="rounded-box border border-amma-green-light bg-white p-5 md:p-7">
        <div class="flex flex-col sm:flex-row sm:items-end sm:justify-between gap-3">
          <div>
            <h2 class="text-lg font-bold text-amma-green-dark">Recibos de entrega</h2>
            <p class="text-sm text-grey-text mt-1">Histórico acumulado das parcelas comprovadas à autarquia.</p>
          </div>
          <RouterLink :to="{ name: 'compensacao-editar', params: { id: termo.id } }" class="inline-flex min-h-10 items-center justify-center gap-2 rounded-btn border-2 border-amma-green bg-white px-4 font-bold text-amma-green hover:bg-amma-green-lighter focus-visible:outline focus-visible:outline-2 focus-visible:outline-amma-green">
            <span class="material-symbols-outlined" aria-hidden="true">add</span>Registrar entrega
          </RouterLink>
        </div>
        <div v-if="termo.entregas.length" class="mt-5 divide-y divide-amma-green-light rounded-btn border border-amma-green-light">
          <div v-for="entrega in termo.entregas" :key="entrega.id" class="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-2 p-4">
            <div><p class="font-semibold text-grey-text">{{ entrega.quantidade }} mudas</p><p class="text-xs text-grey-text mt-1">{{ entrega.registradaEm }}</p></div>
            <button type="button" class="inline-flex min-h-10 items-center gap-2 text-sm font-semibold text-amma-green underline focus-visible:outline focus-visible:outline-2 focus-visible:outline-amma-green" @click="baixarDocumento(entrega.id)"><span class="material-symbols-outlined" aria-hidden="true">attach_file</span>{{ entrega.arquivoNome }} — baixar recibo</button>
          </div>
        </div>
        <p v-else class="mt-5 rounded-btn border border-amma-green-light bg-amma-bg p-5 text-sm text-grey-text">Nenhuma entrega registrada. O status permanece Em aberto até que uma parcela seja comprovada.</p>
      </section>
    </template>
  </div>
</template>
