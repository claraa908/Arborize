<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import AppDataTable, { type TableColumn } from '@/components/ui/AppDataTable.vue'
import CompensacaoStatusBadge from '@/components/compensacao/CompensacaoStatusBadge.vue'
import CompensacaoListaEstado from '@/components/compensacao/CompensacaoListaEstado.vue'
import { useCompensacaoStore } from '@/stores/compensacaoStore'

const router = useRouter()
const store = useCompensacaoStore()
const busca = ref('')
const termos = computed(() => store.termos)
const isLoading = computed(() => store.isLoading)
const loadError = computed(() => store.error)
const colunas: TableColumn[] = [
  { key: 'empresa', label: 'Empresa', width: '28%' },
  { key: 'termo', label: 'Termo' },
  { key: 'acordado', label: 'Acordado', align: 'center' },
  { key: 'entregue', label: 'Entregue', align: 'center' },
  { key: 'status', label: 'Status', align: 'center' },
  { key: 'acoes', label: 'Ações', align: 'center' },
]

const linhas = computed(() => termos.value
  .filter((termo) => `${termo.razaoSocial} ${termo.cnpj} ${termo.numeroTermo}`.toLowerCase().includes(busca.value.toLowerCase()))
  )

const resumo = computed(() => ({
  acordos: termos.value.length,
  mudasAcordadas: termos.value.reduce((total, termo) => total + termo.quantidadeAcordada, 0),
  mudasEntregues: termos.value.reduce((total, termo) => total + termo.quantidadeEntregue, 0),
  emAberto: termos.value.filter((termo) => termo.status === 'EM_ABERTO').length,
}))

const abrirDetalhe = (id: number) => router.push({ name: 'compensacao-detalhes', params: { id } })
const abrirEdicao = (id: number) => router.push({ name: 'compensacao-editar', params: { id } })

onMounted(() => { void store.carregarLista() })

function retryLoad() {
  void store.carregarLista()
}
</script>

<template>
  <div class="px-6 md:px-10 pt-6 pb-8 flex flex-col gap-6">
    <div class="flex flex-col md:flex-row md:items-end md:justify-between gap-4">
      <div>
        <h1 class="text-2xl md:text-3xl font-bold text-amma-green-dark">Termos de compensação ambiental</h1>
        <p class="text-sm md:text-base text-grey-text mt-2">Acompanhe as mudas acordadas e entregues por empreendimentos.</p>
      </div>
      <RouterLink
        :to="{ name: 'compensacao-novo' }"
        class="inline-flex min-h-11 items-center justify-center gap-2 bg-amma-green hover:bg-amma-green-dark text-white font-bold py-2.5 px-6 rounded-btn efeito-zoom focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-amma-green"
      >
        <span class="material-symbols-outlined" aria-hidden="true">add</span>
        Novo termo
      </RouterLink>
    </div>

    <p class="rounded-btn border border-feedback-info-border bg-feedback-info-bg px-4 py-3 text-sm text-feedback-info" role="note">
      Acordos, quantitativos e estados são atualizados pelos registros da AMMA e recibos apresentados.
    </p>

    <section class="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-4 gap-4" aria-label="Resumo dos acordos">
      <article v-for="card in [
        { label: 'Termos registrados', value: resumo.acordos, icon: 'description', suffix: 'acordos' },
        { label: 'Mudas acordadas', value: resumo.mudasAcordadas.toLocaleString('pt-BR'), icon: 'psychiatry', suffix: 'no total' },
        { label: 'Mudas entregues', value: resumo.mudasEntregues.toLocaleString('pt-BR'), icon: 'task_alt', suffix: 'com recibo' },
        { label: 'Em aberto', value: resumo.emAberto, icon: 'flag', suffix: 'termos' },
      ]" :key="card.label" class="bg-white border border-amma-green-light rounded-box p-5 flex flex-col gap-4">
        <div class="flex items-center justify-between gap-3">
          <h2 class="text-sm font-bold text-amma-green-dark">{{ card.label }}</h2>
          <span class="w-10 h-10 rounded-full bg-amma-green-lighter text-amma-green flex items-center justify-center material-symbols-outlined" aria-hidden="true">{{ card.icon }}</span>
        </div>
        <p class="text-3xl font-bold text-grey-text">{{ card.value }}</p>
        <p class="text-xs text-grey-text">{{ card.suffix }}</p>
      </article>
    </section>

    <section class="bg-white border border-amma-green-light rounded-box p-4 md:p-6">
      <div class="mb-5 max-w-xl">
        <label for="busca-termo" class="sr-only">Pesquisar termos</label>
        <div class="relative">
          <span class="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-grey-text/60" aria-hidden="true">search</span>
          <input id="busca-termo" v-model="busca" type="search" placeholder="Pesquisar por empresa, CNPJ ou termo..." class="w-full bg-white focus:bg-amma-green-lighter border-2 border-amma-green-light focus:border-amma-green rounded-btn py-2.5 pl-10 pr-4 text-grey-text outline-none transition-all text-sm font-medium">
        </div>
      </div>
      <CompensacaoListaEstado :loading="isLoading" :error="loadError" :empty="linhas.length === 0" @retry="retryLoad">
      <div class="md:hidden flex flex-col gap-4" aria-label="Lista de termos">
        <article v-for="item in linhas" :key="item.id" class="rounded-btn border border-amma-green-light p-4">
          <div class="flex items-start justify-between gap-3">
            <div class="min-w-0"><h2 class="font-bold text-grey-text break-words">{{ item.razaoSocial }}</h2><p class="mt-1 text-xs text-grey-text">CNPJ {{ item.cnpj }}</p></div>
            <CompensacaoStatusBadge :status="item.status" />
          </div>
          <dl class="mt-4 grid grid-cols-2 gap-3 text-sm">
            <div><dt class="text-xs text-grey-text">Termo</dt><dd class="font-semibold text-grey-text">{{ item.numeroTermo }}</dd></div>
            <div><dt class="text-xs text-grey-text">Entregue / acordado</dt><dd class="font-semibold text-grey-text">{{ item.quantidadeEntregue }} / {{ item.quantidadeAcordada }} mudas</dd></div>
          </dl>
          <div class="mt-4 flex gap-2 border-t border-amma-green-light pt-3">
            <button type="button" @click="abrirDetalhe(item.id)" class="min-h-10 flex-1 rounded-btn border border-amma-green px-3 font-bold text-amma-green focus-visible:outline focus-visible:outline-2 focus-visible:outline-amma-green" :aria-label="`Consultar termo ${item.numeroTermo}`">Consultar</button>
            <button type="button" @click="abrirEdicao(item.id)" class="min-h-10 flex-1 rounded-btn bg-amma-green px-3 font-bold text-white focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-amma-green" :aria-label="`Editar termo ${item.numeroTermo}`">Editar / recibo</button>
          </div>
        </article>
      </div>
      <AppDataTable
        class="hidden md:block"
        :columns="colunas"
        :items="linhas"
        :loading="isLoading"
        :show-search="false"
        :show-summary="false"
        :allow-view-toggle="false"
        :default-view="'table'"
        :caption="'Acordos de compensação ambiental — registros fictícios de demonstração'"
        empty-message="Nenhum termo de compensação encontrado. Cadastre um novo termo para iniciar o acompanhamento."
      >
        <template #empresa="{ item }">
          <span class="block font-bold text-grey-text">{{ item.razaoSocial }}</span>
          <span class="block text-xs text-grey-text mt-1">CNPJ {{ item.cnpj }}</span>
        </template>
        <template #termo="{ item }">
          <span class="text-sm font-semibold text-grey-text">{{ item.numeroTermo }}</span>
        </template>
        <template #acordado="{ item }">
          <span class="whitespace-nowrap text-sm text-grey-text">{{ item.quantidadeAcordada.toLocaleString('pt-BR') }} mudas</span>
        </template>
        <template #entregue="{ item }">
          <span class="whitespace-nowrap text-sm text-grey-text">{{ item.quantidadeEntregue.toLocaleString('pt-BR') }} ({{ Math.round(item.quantidadeEntregue / item.quantidadeAcordada * 100) }}%)</span>
        </template>
        <template #status="{ item }">
          <CompensacaoStatusBadge :status="item.status" />
        </template>
        <template #acoes="{ item }">
          <div class="flex items-center justify-center gap-2">
            <button type="button" @click="abrirDetalhe(item.id)" class="w-10 h-10 rounded-full text-amma-green hover:bg-amma-green-lighter focus-visible:outline focus-visible:outline-2 focus-visible:outline-amma-green" :aria-label="`Consultar termo ${item.numeroTermo}`">
              <span class="material-symbols-outlined" aria-hidden="true">visibility</span>
            </button>
            <button type="button" @click="abrirEdicao(item.id)" class="w-10 h-10 rounded-full text-amma-green hover:bg-amma-green-lighter focus-visible:outline focus-visible:outline-2 focus-visible:outline-amma-green" :aria-label="`Editar termo ${item.numeroTermo}`">
              <span class="material-symbols-outlined" aria-hidden="true">edit</span>
            </button>
          </div>
        </template>
      </AppDataTable>
      </CompensacaoListaEstado>
    </section>
  </div>
</template>
