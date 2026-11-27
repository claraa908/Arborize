<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import AppDataTable, { type TableColumn } from '@/components/ui/AppDataTable.vue'
import { useEventoStore } from '@/stores/eventoStore'

const router = useRouter()
const eventoStore = useEventoStore()
const searchQuery = ref('')
const selectedStatusFilter = ref<string>('TODOS')

const colunas: TableColumn[] = [
  { key: 'nome', label: 'Nome do Evento', width: '27%' },
  { key: 'periodo', label: 'Período', align: 'center', width: '13%' },
  { key: 'horario', label: 'Horário', align: 'center', width: '13%' },
  { key: 'local', label: 'Local', width: '19%' },
  { key: 'status', label: 'Status', align: 'center', width: '13%' },
  { key: 'acoes', label: 'Ações', align: 'center', width: '15%', class: 'px-2' },
]

onMounted(() => {
  eventoStore.fetchEventos()
})

const contadores = computed(() => {
  const total = eventoStore.eventos.length
  const planejados = eventoStore.eventos.filter((e) => e.status === 'PLANEJADO').length
  const confirmados = eventoStore.eventos.filter((e) => e.status === 'CONFIRMADO').length
  const encerrados = eventoStore.eventos.filter((e) => e.status === 'ENCERRADO').length
  return { total, planejados, confirmados, encerrados }
})

const eventosFiltrados = computed(() => {
  return eventoStore.eventos.filter((evt) => {
    const matchBusca =
      !searchQuery.value ||
      (evt.nome && evt.nome.toLowerCase().includes(searchQuery.value.toLowerCase())) ||
      (evt.localConcentracao && evt.localConcentracao.toLowerCase().includes(searchQuery.value.toLowerCase())) ||
      (evt.destinoFinal && evt.destinoFinal.toLowerCase().includes(searchQuery.value.toLowerCase()))

    const matchStatus =
      selectedStatusFilter.value === 'TODOS' || evt.status === selectedStatusFilter.value

    return matchBusca && matchStatus
  })
})

const navigateToNew = () => {
  router.push({ name: 'eventos-cadastrar' })
}

const viewDetails = (id: string | number) => {
  router.push({ name: 'eventos-detalhes', params: { id } })
}

const editEvento = (id: string | number) => {
  router.push({ name: 'eventos-editar', params: { id } })
}

const handleDelete = async (id: string | number) => {
  await eventoStore.removeEvento(id)
}

const statusClass = (status: string) => {
  switch (status) {
    case 'CONFIRMADO':
      return 'bg-amma-green/20 text-amma-green-dark'
    case 'PLANEJADO':
      return 'bg-feedback-info-bg text-feedback-info border border-feedback-info-border'
    case 'ENCERRADO':
      return 'bg-black/10 text-grey-text'
    default:
      return 'bg-feedback-warning-bg text-feedback-warning border border-feedback-warning-border'
  }
}
</script>

<template>
  <div class="px-6 md:px-10 pt-6 flex flex-col gap-6">
    <div class="flex flex-wrap items-center gap-2">
      <button
        type="button"
        @click="selectedStatusFilter = 'TODOS'"
        :class="[
          'px-4 py-1.5 rounded-btn text-xs font-bold transition-all border-2 flex items-center gap-2 cursor-pointer',
          selectedStatusFilter === 'TODOS'
            ? 'bg-amma-green text-white border-amma-green shadow-xs'
            : 'bg-white text-grey-text border-amma-green-light hover:border-amma-green'
        ]"
      >
        <span>Todos os Eventos</span>
        <span class="bg-black/10 px-2 py-0.5 rounded-full text-[11px]">{{ contadores.total }}</span>
      </button>

      <button
        type="button"
        @click="selectedStatusFilter = 'PLANEJADO'"
        :class="[
          'px-4 py-1.5 rounded-btn text-xs font-bold transition-all border-2 flex items-center gap-2 cursor-pointer',
          selectedStatusFilter === 'PLANEJADO'
            ? 'bg-amma-green text-white border-amma-green shadow-xs'
            : 'bg-white text-grey-text border-amma-green-light hover:border-amma-green'
        ]"
      >
        <span class="w-2 h-2 rounded-full bg-blue-500"></span>
        <span>Planejados</span>
        <span class="bg-black/10 px-2 py-0.5 rounded-full text-[11px]">{{ contadores.planejados }}</span>
      </button>

      <button
        type="button"
        @click="selectedStatusFilter = 'CONFIRMADO'"
        :class="[
          'px-4 py-1.5 rounded-btn text-xs font-bold transition-all border-2 flex items-center gap-2 cursor-pointer',
          selectedStatusFilter === 'CONFIRMADO'
            ? 'bg-amma-green text-white border-amma-green shadow-xs'
            : 'bg-white text-grey-text border-amma-green-light hover:border-amma-green'
        ]"
      >
        <span class="w-2 h-2 rounded-full bg-emerald-500"></span>
        <span>Confirmados</span>
        <span class="bg-black/10 px-2 py-0.5 rounded-full text-[11px]">{{ contadores.confirmados }}</span>
      </button>

      <button
        type="button"
        @click="selectedStatusFilter = 'ENCERRADO'"
        :class="[
          'px-4 py-1.5 rounded-btn text-xs font-bold transition-all border-2 flex items-center gap-2 cursor-pointer',
          selectedStatusFilter === 'ENCERRADO'
            ? 'bg-amma-green text-white border-amma-green shadow-xs'
            : 'bg-white text-grey-text border-amma-green-light hover:border-amma-green'
        ]"
      >
        <span class="w-2 h-2 rounded-full bg-gray-400"></span>
        <span>Encerrados</span>
        <span class="bg-black/10 px-2 py-0.5 rounded-full text-[11px]">{{ contadores.encerrados }}</span>
      </button>
    </div>

    <AppDataTable
      v-model:search="searchQuery"
      :columns="colunas"
      :items="eventosFiltrados"
      default-view="table"
      fixed-layout
      max-body-height-class="max-h-[max(520px,calc(100vh-260px))]"
      search-placeholder="Pesquisar por nome do evento ou local..."
      button-label="Cadastrar Evento"
      button-icon="add"
      empty-message="Nenhum evento socioambiental encontrado."
      @action-click="navigateToNew"
    >
      <template #nome="{ item }">
        <span class="font-semibold text-grey-text block truncate" :title="item.nome">
          {{ item.nome }}
        </span>
        <span
          v-if="item.destinoFinal"
          class="text-xs text-grey-text/70 block mt-0.5 truncate"
          :title="item.destinoFinal"
        >
          Destino: {{ item.destinoFinal }}
        </span>
      </template>

      <template #periodo="{ item }">
        <span v-if="item.dataInicio === item.dataTermino" class="text-xs font-medium text-grey-text/80 whitespace-nowrap">
          {{ item.dataInicio }}
        </span>
        <div v-else class="flex flex-col text-xs font-medium text-grey-text/80 leading-tight whitespace-nowrap">
          <span>{{ item.dataInicio }}</span>
          <span class="text-grey-text/60">até {{ item.dataTermino }}</span>
        </div>
      </template>

      <template #horario="{ item }">
        <span
          class="text-xs font-semibold text-grey-text/80 bg-black/5 px-2 py-1 rounded-full whitespace-nowrap"
        >
          {{ item.horarioInicio }} - {{ item.horarioTermino }}
        </span>
      </template>

      <template #local="{ item }">
        <span
          class="text-sm font-medium text-grey-text block truncate"
          :title="item.localConcentracao"
        >
          {{ item.localConcentracao }}
        </span>
      </template>

      <template #status="{ item }">
        <span
          :class="[
            'text-[11px] font-bold px-2.5 py-1 rounded-full inline-block whitespace-nowrap',
            statusClass(item.status),
          ]"
        >
          {{ item.status }}
        </span>
      </template>

      <template #acoes="{ item }">
        <div v-if="item.id" class="flex items-center justify-center gap-2">
          <button
            type="button"
            @click="viewDetails(item.id)"
            class="flex h-8 w-8 items-center justify-center rounded-full text-amma-green transition-colors hover:bg-amma-green-lighter cursor-pointer"
            title="Visualizar Detalhes"
            :aria-label="`Visualizar ${item.nome}`"
          >
            <span class="material-symbols-outlined text-[20px]" aria-hidden="true">event</span>
          </button>
          <button
            type="button"
            @click="editEvento(item.id)"
            class="flex h-8 w-8 items-center justify-center rounded-full text-grey-text transition-colors hover:bg-amma-green-lighter hover:text-amma-green-dark cursor-pointer"
            title="Editar Evento"
            :aria-label="`Editar ${item.nome}`"
          >
            <span class="material-symbols-outlined text-[20px]" aria-hidden="true">edit</span>
          </button>
          <button
            type="button"
            @click="handleDelete(item.id)"
            class="flex h-8 w-8 items-center justify-center rounded-full text-feedback-danger transition-colors hover:bg-feedback-danger-bg cursor-pointer"
            title="Excluir Evento"
            :aria-label="`Excluir ${item.nome}`"
          >
            <span class="material-symbols-outlined text-[20px]" aria-hidden="true">delete</span>
          </button>
        </div>
      </template>
    </AppDataTable>
  </div>
</template>