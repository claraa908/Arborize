<script setup lang="ts">
import { reactive, watch } from 'vue'
import type { EspecieDTO } from '@/types/viveiro'
import type { MovimentacaoMudaDTO, MovimentacaoMudaFiltros, PaginaMovimentacoes, TipoMovimentacao } from '@/types/movimentacaoMuda'

const props = defineProps<{
  pagina: PaginaMovimentacoes | null
  especies: EspecieDTO[]
  loading?: boolean
  error?: string
}>()

const emit = defineEmits<{
  filter: [filtros: MovimentacaoMudaFiltros]
  page: [filtros: MovimentacaoMudaFiltros, pagina: number]
  edit: [movimento: MovimentacaoMudaDTO]
  create: []
  proof: [id: number]
  retry: []
}>()

const filtros = reactive<MovimentacaoMudaFiltros>({ tipo: '', especieId: '', dataInicio: '', dataFim: '', busca: '' })
const tipos: { value: TipoMovimentacao; label: string }[] = [
  { value: 'ENTRADA_GERMINACAO', label: 'Entrada / Germinação' },
  { value: 'SAIDA_PLANTIO', label: 'Saída para plantio' },
  { value: 'BAIXA_PERDA', label: 'Baixa por perda' },
]

watch(() => [props.pagina?.number, props.loading], ([page]) => {
  if (typeof page === 'number') filtros.pagina = page
})

function aplicarFiltros() {
  emit('filter', { ...filtros })
}

function limparFiltros() {
  Object.assign(filtros, { tipo: '', especieId: '', dataInicio: '', dataFim: '', busca: '' })
  aplicarFiltros()
}

function nomeTipo(tipo: TipoMovimentacao) {
  return tipos.find((item) => item.value === tipo)?.label ?? tipo
}

function dataPtBr(data: string) {
  const [ano, mes, dia] = data.split('-')
  return `${dia}/${mes}/${ano}`
}
</script>

<template>
  <section class="flex flex-col gap-5" aria-labelledby="historico-movimentacoes-titulo" :aria-busy="loading">
    <div class="flex flex-col justify-between gap-3 sm:flex-row sm:items-end">
      <div>
        <h2 id="historico-movimentacoes-titulo" class="text-xl font-bold text-amma-green-dark">Histórico de movimentações</h2>
        <p class="mt-1 text-sm text-grey-text">Consulte os registros e edite uma movimentação quando necessário.</p>
      </div>
      <button type="button" class="inline-flex min-h-11 items-center justify-center gap-2 rounded-btn bg-amma-green px-5 py-2 text-sm font-bold text-white hover:bg-amma-green-dark" @click="emit('create')">
        <span class="material-symbols-outlined text-[19px]" aria-hidden="true">add</span>Registrar movimentação
      </button>
    </div>

    <form class="grid grid-cols-1 gap-3 rounded-box border border-amma-green-light bg-white p-4 shadow-sm sm:grid-cols-2 xl:grid-cols-6" @submit.prevent="aplicarFiltros">
      <label class="flex flex-col gap-1 text-xs font-semibold text-grey-text xl:col-span-2">Buscar
        <input v-model="filtros.busca" type="search" placeholder="Espécie, destino ou responsável" class="min-h-10 rounded-btn border border-amma-green-light px-3 text-sm font-normal outline-none focus:border-amma-green focus:ring-2 focus:ring-amma-green/20" />
      </label>
      <label class="flex flex-col gap-1 text-xs font-semibold text-grey-text">Tipo
        <select v-model="filtros.tipo" class="min-h-10 rounded-btn border border-amma-green-light bg-white px-3 text-sm font-normal outline-none focus:border-amma-green"><option value="">Todos</option><option v-for="tipo in tipos" :key="tipo.value" :value="tipo.value">{{ tipo.label }}</option></select>
      </label>
      <label class="flex flex-col gap-1 text-xs font-semibold text-grey-text">Espécie
        <select v-model="filtros.especieId" class="min-h-10 rounded-btn border border-amma-green-light bg-white px-3 text-sm font-normal outline-none focus:border-amma-green"><option value="">Todas</option><option v-for="especie in especies" :key="especie.id" :value="Number(especie.id)">{{ especie.nomePopular }}</option></select>
      </label>
      <label class="flex flex-col gap-1 text-xs font-semibold text-grey-text">De
        <input v-model="filtros.dataInicio" type="date" class="min-h-10 rounded-btn border border-amma-green-light px-2 text-sm font-normal outline-none focus:border-amma-green" />
      </label>
      <label class="flex flex-col gap-1 text-xs font-semibold text-grey-text">Até
        <input v-model="filtros.dataFim" type="date" class="min-h-10 rounded-btn border border-amma-green-light px-2 text-sm font-normal outline-none focus:border-amma-green" />
      </label>
      <div class="flex items-end gap-2 sm:col-span-2 xl:col-span-6 xl:justify-end">
        <button type="button" class="min-h-10 rounded-btn border border-amma-green-light px-4 text-sm font-bold text-grey-text hover:bg-amma-bg" @click="limparFiltros">Limpar</button>
        <button type="submit" class="min-h-10 rounded-btn bg-amma-green px-5 text-sm font-bold text-white hover:bg-amma-green-dark">Filtrar</button>
      </div>
    </form>

    <div v-if="loading" class="rounded-box border border-amma-green-light bg-white p-5" role="status" aria-label="Carregando histórico de movimentações">
      <span class="sr-only">Carregando histórico de movimentações</span>
      <div class="flex flex-col gap-3" aria-hidden="true"><div v-for="n in 5" :key="n" class="h-12 animate-pulse rounded-btn bg-amma-bg"></div></div>
    </div>
    <div v-else-if="error" class="rounded-box border border-feedback-danger-border bg-feedback-danger-bg p-6 text-center" role="alert">
      <p class="font-semibold text-feedback-danger">{{ error }}</p>
      <button type="button" class="mt-3 rounded-btn border border-feedback-danger px-4 py-2 text-sm font-bold text-feedback-danger" @click="emit('retry')">Tentar novamente</button>
    </div>
    <div v-else class="overflow-hidden rounded-box border border-amma-green-light bg-white shadow-sm">
      <div class="max-h-[460px] overflow-auto">
        <table class="w-full min-w-[900px] border-collapse text-left">
          <caption class="sr-only">Histórico de movimentações de mudas</caption>
          <thead class="sticky top-0 z-10 bg-amma-bg shadow-[inset_0_-1px_0_0_var(--color-amma-green-light)]"><tr class="text-sm text-amma-green-dark">
            <th class="bg-amma-bg px-5 py-4 font-bold">Data</th><th class="bg-amma-bg px-5 py-4 font-bold">Tipo</th><th class="bg-amma-bg px-5 py-4 font-bold">Espécie</th><th class="bg-amma-bg px-5 py-4 text-right font-bold">Quantidade</th><th class="bg-amma-bg px-5 py-4 font-bold">Destino / responsável</th><th class="bg-amma-bg px-5 py-4 text-center font-bold">Ações</th>
          </tr></thead>
          <tbody v-if="pagina?.content.length">
            <tr v-for="movimento in pagina.content" :key="movimento.id" class="border-b border-amma-green-light/60 last:border-0 hover:bg-amma-green-lighter/30">
              <td class="whitespace-nowrap px-5 py-4 text-sm text-grey-text">{{ dataPtBr(movimento.data) }}</td>
              <td class="px-5 py-4"><span class="inline-flex rounded-full bg-amma-green-lighter px-3 py-1 text-xs font-bold text-amma-green-dark">{{ nomeTipo(movimento.tipo) }}</span></td>
              <td class="px-5 py-4"><span class="block text-sm font-semibold text-grey-text">{{ movimento.especieNomePopular }}</span><span class="text-xs italic text-grey-text/70">{{ movimento.especieNomeCientifico }}</span></td>
              <td class="whitespace-nowrap px-5 py-4 text-right text-sm font-bold text-amma-green-dark">{{ movimento.quantidade.toLocaleString('pt-BR') }} un</td>
              <td class="px-5 py-4 text-sm text-grey-text"><span class="block">{{ movimento.destino || '—' }}</span><span class="text-xs text-grey-text/70">{{ movimento.responsavel || 'Responsável não informado' }}</span></td>
              <td class="px-5 py-4"><div class="flex items-center justify-center gap-1">
                <button v-if="movimento.comprovanteNome" type="button" class="flex h-9 w-9 items-center justify-center rounded-full text-amma-green hover:bg-amma-green-lighter" :aria-label="`Abrir comprovante da movimentação de ${movimento.especieNomePopular}`" @click="emit('proof', movimento.id)"><span class="material-symbols-outlined" aria-hidden="true">attach_file</span></button>
                <button type="button" class="flex h-9 w-9 items-center justify-center rounded-full text-amma-green hover:bg-amma-green-lighter" :aria-label="`Editar movimentação de ${movimento.especieNomePopular}`" @click="emit('edit', movimento)"><span class="material-symbols-outlined" aria-hidden="true">edit</span></button>
              </div></td>
            </tr>
          </tbody>
          <tbody v-else><tr><td colspan="6" class="px-6 py-14 text-center">
            <span class="material-symbols-outlined text-4xl text-grey-text/30" aria-hidden="true">inventory</span>
            <p class="mt-2 font-semibold text-grey-text">Nenhuma movimentação encontrada.</p>
            <p class="mt-1 text-sm text-grey-text/70">Registre a primeira entrada, saída ou baixa para começar o histórico.</p>
            <button type="button" class="mt-4 rounded-btn bg-amma-green px-4 py-2 text-sm font-bold text-white" @click="emit('create')">Registrar movimentação</button>
          </td></tr></tbody>
        </table>
      </div>
      <div v-if="pagina && pagina.totalElements > 0" class="flex items-center justify-between border-t border-amma-green-light px-5 py-3 text-sm text-grey-text">
        <span>{{ pagina.totalElements }} registros · página {{ pagina.number + 1 }} de {{ pagina.totalPages || 1 }}</span>
        <div class="flex gap-2"><button type="button" :disabled="pagina.number <= 0" class="rounded-btn border border-amma-green-light px-3 py-1.5 font-semibold disabled:opacity-40" @click="emit('page', { ...filtros }, pagina.number - 1)">Anterior</button><button type="button" :disabled="pagina.number + 1 >= pagina.totalPages" class="rounded-btn border border-amma-green-light px-3 py-1.5 font-semibold disabled:opacity-40" @click="emit('page', { ...filtros }, pagina.number + 1)">Próxima</button></div>
      </div>
    </div>
  </section>
</template>
