<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AppBreadcrumb, { type BreadcrumbItem } from '@/components/ui/AppBreadcrumb.vue'
import AppDataTable, { type TableColumn } from '@/components/ui/AppDataTable.vue'
import MovimentacaoMudaForm from '@/components/viveiro/MovimentacaoMudaForm.vue'
import MovimentacoesTable from '@/components/viveiro/MovimentacoesTable.vue'
import { useMovimentacaoMudaStore } from '@/stores/movimentacaoMudaStore'
import { useViveiroStore } from '@/stores/viveiroStore'
import type { MovimentacaoMudaDTO, MovimentacaoMudaFiltros, MovimentacaoMudaPayload } from '@/types/movimentacaoMuda'

const router = useRouter()
const route = useRoute()
const viveiroStore = useViveiroStore()
const movimentacaoStore = useMovimentacaoMudaStore()
const numeroPtBr = new Intl.NumberFormat('pt-BR')
const searchQuery = ref('')
const currentMovementFilters = ref<MovimentacaoMudaFiltros>({})

const activeTab = computed(() => route.query.aba === 'movimentacoes' ? 'movimentacoes' : 'especies')
const movementQuery = computed(() => typeof route.query.movimentacao === 'string' ? route.query.movimentacao : '')
const formMode = computed(() => movementQuery.value !== '')
const movementId = computed(() => movementQuery.value && movementQuery.value !== 'nova' ? Number(movementQuery.value) : undefined)
const movimentacaoBreadcrumb = computed<BreadcrumbItem[]>(() => [
  { label: 'Viveiro', icon: 'psychiatry', onClick: fecharFormulario },
  { label: movementId.value ? 'Editar movimentação' : 'Registrar movimentação' },
])

const indicadores = computed(() => {
  const estoqueAtual = viveiroStore.especies.reduce((total, especie) => total + (especie.mudasDisponiveis ?? 0), 0)
  return [
    { titulo: 'Estoque atual', valor: numeroPtBr.format(estoqueAtual), descricao: 'Mudas disponíveis', icone: 'inventory_2', estiloIcone: 'bg-amma-green-lighter text-amma-green-dark', loading: viveiroStore.isLoading },
    { titulo: 'Em germinação', valor: '—', descricao: 'Etapa ainda não acompanhada', icone: 'psychiatry', estiloIcone: 'bg-feedback-warning-bg text-feedback-warning', loading: false },
    { titulo: 'Perdas no mês', valor: movimentacaoStore.resumo ? numeroPtBr.format(movimentacaoStore.resumo.perdasNoMes) : '—', descricao: 'Quantidade baixada no mês atual', icone: 'trending_down', estiloIcone: 'bg-feedback-danger-bg text-feedback-danger', loading: movimentacaoStore.isLoadingResumo },
    { titulo: 'Saídas no mês', valor: movimentacaoStore.resumo ? numeroPtBr.format(movimentacaoStore.resumo.saidasNoMes) : '—', descricao: 'Mudas destinadas a plantio no mês', icone: 'local_shipping', estiloIcone: 'bg-feedback-info-bg text-feedback-info', loading: movimentacaoStore.isLoadingResumo },
  ]
})

const colunas: TableColumn[] = [
  { key: 'especie', label: 'Espécie Botânica', width: '35%' },
  { key: 'categoria', label: 'Categoria' },
  { key: 'porteArvore', label: 'Porte', align: 'center' },
  { key: 'recomendadaCalcadas', label: 'Calçadas', align: 'center' },
  { key: 'mudasDisponiveis', label: 'Estoque Mudas', align: 'center' },
  { key: 'acoes', label: 'Ações', align: 'center' },
]

const especiesFiltradas = computed(() => {
  const query = searchQuery.value.trim().toLocaleLowerCase('pt-BR')
  if (!query) return viveiroStore.especies
  return viveiroStore.especies.filter((item) =>
    [item.nomePopular, item.nomeCientifico, item.categoria]
      .some((value) => value?.toLocaleLowerCase('pt-BR').includes(query)),
  )
})

async function carregarMovimentacoes(filtros: MovimentacaoMudaFiltros = {}, pagina = 0) {
  currentMovementFilters.value = { ...filtros }
  await movimentacaoStore.listar(filtros, pagina)
}

function selecionarAba(aba: 'especies' | 'movimentacoes') {
  void router.replace({ name: 'viveiro', query: { ...route.query, aba, movimentacao: undefined } })
}

function abrirNovaMovimentacao() {
  void router.push({ name: 'viveiro', query: { ...route.query, aba: 'movimentacoes', movimentacao: 'nova' } })
}

function editarMovimentacao(movimento: MovimentacaoMudaDTO) {
  void router.push({ name: 'viveiro', query: { ...route.query, aba: 'movimentacoes', movimentacao: String(movimento.id) } })
}

function fecharFormulario() {
  void router.replace({ name: 'viveiro', query: { aba: 'movimentacoes' } })
}

async function salvarMovimentacao(payload: MovimentacaoMudaPayload) {
  const salvo = await movimentacaoStore.salvar(payload, movementId.value)
  if (!salvo) return
  await Promise.all([
    viveiroStore.fetchEspecies(),
    carregarMovimentacoes(currentMovementFilters.value),
  ])
  fecharFormulario()
}

function abrirNovaEspecie() {
  void router.push({ name: 'viveiro-cadastrar' })
}

function visualizarEspecie(id: string | number) {
  void router.push({ name: 'viveiro-detalhes', params: { id } })
}

function editarEspecie(id: string | number) {
  void router.push({ name: 'viveiro-editar', params: { id } })
}

async function excluirEspecie(id: string | number) {
  await viveiroStore.removeEspecie(id)
}

watch(movementQuery, async (query) => {
  movimentacaoStore.formError = ''
  if (query && query !== 'nova') await movimentacaoStore.buscar(Number(query))
  else movimentacaoStore.movimentacaoAtual = null
}, { immediate: true })

onMounted(() => {
  void viveiroStore.fetchEspecies()
  void movimentacaoStore.buscarResumo()
  void carregarMovimentacoes()
})
</script>

<template>
  <div class="flex w-full flex-col">
    <template v-if="formMode">
      <AppBreadcrumb :items="movimentacaoBreadcrumb" />
      <div class="flex flex-col gap-6 px-6 pb-8 pt-6 md:px-10">
        <header class="flex items-center gap-4">
          <div class="flex h-12 w-12 shrink-0 items-center justify-center rounded-full bg-amma-green-lighter/50 text-amma-green shadow-sm" aria-hidden="true">
            <span class="material-symbols-outlined text-[24px]">{{ movementId ? 'edit' : 'sync_alt' }}</span>
          </div>
          <div>
            <h1 class="text-2xl font-bold leading-none text-amma-green-dark">{{ movementId ? 'Editar movimentação' : 'Registrar movimentação' }}</h1>
            <p class="mt-1 text-sm text-grey-text">Registre entradas, saídas para plantio e perdas de mudas.</p>
          </div>
        </header>
        <div v-if="movementId && movimentacaoStore.isLoadingDetalhe" class="rounded-box border border-amma-green-light bg-white p-8 text-center" role="status">Carregando movimentação...</div>
        <div v-else-if="movementId && !movimentacaoStore.movimentacaoAtual" class="rounded-box border border-feedback-danger-border bg-feedback-danger-bg p-6 text-center" role="alert">
          <p class="font-semibold text-feedback-danger">{{ movimentacaoStore.formError || 'Movimentação não encontrada.' }}</p>
          <button type="button" class="mt-3 text-sm font-bold text-amma-green-dark underline" @click="fecharFormulario">Voltar ao histórico</button>
        </div>
        <MovimentacaoMudaForm
          v-else
          :especies="viveiroStore.especies"
          :initial-movement="movimentacaoStore.movimentacaoAtual"
          :loading="viveiroStore.isLoading"
          :saving="movimentacaoStore.isSaving"
          :error="movimentacaoStore.formError"
          @save="salvarMovimentacao"
          @cancel="fecharFormulario"
          @proof="movimentacaoStore.abrirComprovante"
        />
      </div>
    </template>

    <div v-else class="flex flex-col gap-6 px-4 pb-8 pt-6 sm:px-6 md:px-10">
      <section aria-label="Resumo do estoque" :aria-busy="viveiroStore.isLoading || movimentacaoStore.isLoadingResumo" class="grid grid-cols-1 gap-4 sm:grid-cols-2 2xl:grid-cols-4">
        <article v-for="indicador in indicadores" :key="indicador.titulo" class="min-w-0 rounded-box border border-amma-green-light bg-white p-5 shadow-sm sm:p-6">
          <div class="flex items-start justify-between gap-3">
            <h2 class="text-sm font-bold text-grey-text sm:text-base">{{ indicador.titulo }}</h2>
            <span :class="['flex h-10 w-10 shrink-0 items-center justify-center rounded-btn', indicador.estiloIcone]" aria-hidden="true"><span class="material-symbols-outlined text-[22px]">{{ indicador.icone }}</span></span>
          </div>
          <div class="mt-4 flex min-h-11 items-baseline gap-2">
            <span v-if="indicador.loading" class="h-8 w-24 animate-pulse rounded bg-amma-green-lighter" role="status" :aria-label="`Carregando indicador: ${indicador.titulo}`"></span>
            <p v-else class="text-3xl font-black leading-none text-amma-green-dark">{{ indicador.valor }}</p>
            <span v-if="indicador.titulo === 'Estoque atual' && !indicador.loading" class="text-xs font-semibold text-grey-text">mudas</span>
          </div>
          <p class="mt-3 min-h-8 text-xs font-medium text-grey-text/70">{{ indicador.descricao }}</p>
        </article>
      </section>
      <p v-if="movimentacaoStore.resumoError" class="-mt-3 text-sm text-feedback-danger" role="alert">{{ movimentacaoStore.resumoError }}</p>

      <div class="border-b border-amma-green-light" role="tablist" aria-label="Seções do Viveiro">
        <button type="button" role="tab" id="aba-especies" aria-controls="painel-viveiro" :aria-selected="activeTab === 'especies'" :class="['mr-5 border-b-2 px-1 py-3 text-sm font-bold transition-colors', activeTab === 'especies' ? 'border-amma-green text-amma-green-dark' : 'border-transparent text-grey-text/70 hover:text-amma-green']" @click="selecionarAba('especies')">Espécies</button>
        <button type="button" role="tab" id="aba-movimentacoes" aria-controls="painel-viveiro" :aria-selected="activeTab === 'movimentacoes'" :class="['border-b-2 px-1 py-3 text-sm font-bold transition-colors', activeTab === 'movimentacoes' ? 'border-amma-green text-amma-green-dark' : 'border-transparent text-grey-text/70 hover:text-amma-green']" @click="selecionarAba('movimentacoes')">Movimentações</button>
      </div>

      <div id="painel-viveiro" role="tabpanel" :aria-labelledby="activeTab === 'especies' ? 'aba-especies' : 'aba-movimentacoes'">
        <div v-if="activeTab === 'movimentacoes'">
          <MovimentacoesTable
            :pagina="movimentacaoStore.pagina"
            :especies="viveiroStore.especies"
            :loading="movimentacaoStore.isLoadingListagem"
            :error="movimentacaoStore.listagemError"
            @filter="(filters) => carregarMovimentacoes(filters, 0)"
            @page="(filters, page) => carregarMovimentacoes(filters, page)"
            @edit="editarMovimentacao"
            @create="abrirNovaMovimentacao"
            @proof="movimentacaoStore.abrirComprovante"
            @retry="carregarMovimentacoes"
          />
        </div>

        <div v-else-if="viveiroStore.isLoading" class="rounded-box border border-amma-green-light bg-white p-5 shadow-sm" role="status" aria-label="Carregando espécies do Viveiro">
          <span class="sr-only">Carregando espécies do Viveiro</span>
          <div class="flex flex-col gap-4" aria-hidden="true"><div v-for="linha in 4" :key="linha" class="h-12 animate-pulse rounded-btn bg-amma-bg"></div></div>
        </div>
        <div v-else-if="viveiroStore.loadError" class="rounded-box border border-feedback-danger-border bg-feedback-danger-bg p-6 text-center" role="alert">
          <p class="font-semibold text-feedback-danger">{{ viveiroStore.loadError }}</p>
          <button type="button" class="mt-3 rounded-btn border border-feedback-danger px-4 py-2 text-sm font-bold text-feedback-danger" @click="viveiroStore.fetchEspecies">Tentar novamente</button>
        </div>
        <AppDataTable
          v-else
          :columns="colunas"
          :items="especiesFiltradas"
          v-model:search="searchQuery"
          search-placeholder="Pesquisar por nome popular, científico ou categoria..."
          button-label="Cadastrar Espécie"
          button-icon="add"
          empty-message="Nenhuma espécie botânica encontrada."
          default-view="table"
          @action-click="abrirNovaEspecie"
        >
          <template #header-actions>
            <button type="button" class="flex min-h-10 items-center justify-center gap-1.5 rounded-btn border-2 border-amma-green bg-amma-green px-5 py-2 text-sm font-bold text-white transition-colors hover:bg-amma-green-dark" @click="abrirNovaMovimentacao">
              <span class="material-symbols-outlined text-[18px]" aria-hidden="true">sync_alt</span>Registrar Movimentação
            </button>
            <button type="button" @click="abrirNovaEspecie" class="flex min-h-10 items-center justify-center gap-1.5 rounded-btn border-2 border-amma-green bg-white px-5 py-2 text-sm font-bold text-amma-green outline-none transition-colors hover:bg-amma-green hover:text-white">
              <span class="material-symbols-outlined text-[18px]" aria-hidden="true">add</span>Cadastrar Espécie
            </button>
          </template>
          <template #especie="{ item }"><span class="block font-semibold text-grey-text">{{ item.nomePopular }}</span><span class="block text-xs italic text-grey-text/70">{{ item.nomeCientifico }}</span></template>
          <template #categoria="{ item }"><span class="rounded-full bg-black/5 px-3 py-1 text-sm font-medium text-grey-text/80">{{ item.categoria }}</span></template>
          <template #porteArvore="{ item }"><span class="text-sm font-medium text-grey-text/80">{{ item.porteArvore }}</span></template>
          <template #recomendadaCalcadas="{ item }"><span :class="['inline-block min-w-[60px] rounded-full px-3 py-1 text-xs font-bold', item.recomendadaCalcadas ? 'bg-amma-green/20 text-amma-green-dark' : 'border border-feedback-danger-border bg-feedback-danger-bg text-feedback-danger']">{{ item.recomendadaCalcadas ? 'Sim' : 'Não' }}</span></template>
          <template #mudasDisponiveis="{ item }"><span class="text-sm font-bold text-amma-green-dark">{{ item.mudasDisponiveis || 0 }} un</span></template>
          <template #acoes="{ item }"><div class="flex items-center justify-center gap-2">
            <button v-if="item.id" type="button" @click="visualizarEspecie(item.id)" class="flex h-8 w-8 items-center justify-center rounded-full text-amma-green hover:bg-amma-green-lighter" title="Visualizar detalhes" :aria-label="`Visualizar ${item.nomePopular}`"><span class="material-symbols-outlined text-[20px]" aria-hidden="true">psychiatry</span></button>
            <button v-if="item.id" type="button" @click="editarEspecie(item.id)" class="flex h-8 w-8 items-center justify-center rounded-full text-grey-text hover:bg-amma-green-lighter hover:text-amma-green-dark" title="Editar espécie" :aria-label="`Editar ${item.nomePopular}`"><span class="material-symbols-outlined text-[20px]" aria-hidden="true">edit</span></button>
            <button v-if="item.id" type="button" @click="excluirEspecie(item.id)" class="flex h-8 w-8 items-center justify-center rounded-full text-feedback-danger hover:bg-feedback-danger-bg" title="Excluir espécie" :aria-label="`Excluir ${item.nomePopular}`"><span class="material-symbols-outlined text-[20px]" aria-hidden="true">delete</span></button>
          </div></template>
        </AppDataTable>
      </div>
    </div>
  </div>
</template>
