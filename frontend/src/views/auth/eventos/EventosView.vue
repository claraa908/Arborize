<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import AppDataTable, { type TableColumn } from '@/components/ui/AppDataTable.vue'

const router = useRouter()
const searchQuery = ref('')

const colunas: TableColumn[] = [
  { key: 'nome', label: 'Nome do Evento', width: '32%' },
  { key: 'periodo', label: 'Período', align: 'center' },
  { key: 'horario', label: 'Horário', align: 'center' },
  { key: 'local', label: 'Local de Concentração' },
  { key: 'status', label: 'Status', align: 'center' },
  { key: 'acoes', label: 'Ações', align: 'center' }
]

const eventos = ref([
  {
    id: '1',
    nome: 'Caminhada Ecológica no Cedro',
    descricao: 'Atividade de sensibilização e boas vivências com a natureza.',
    dataInicio: '18/10/2026',
    dataTermino: '18/10/2026',
    horarioInicio: '06:30',
    horarioTermino: '10:30',
    local: 'UFC Campus Quixadá',
    destinoFinal: 'Açude do Cedro',
    status: 'CONFIRMADO'
  },
  {
    id: '2',
    nome: 'Oficina de Compostagem e Mudas Nativas',
    descricao: 'Capacitação prática sobre produção de adubo e cultivo de espécies da Caatinga.',
    dataInicio: '24/10/2026',
    dataTermino: '24/10/2026',
    horarioInicio: '08:00',
    horarioTermino: '12:00',
    local: 'Viveiro Municipal de Quixadá',
    destinoFinal: '',
    status: 'CONFIRMADO'
  },
  {
    id: '3',
    nome: 'Mutirão de Plantio Urbano',
    descricao: 'Plantio de mudas de Ipê e Aroeira ao longo da avenida central.',
    dataInicio: '02/11/2026',
    dataTermino: '03/11/2026',
    horarioInicio: '07:00',
    horarioTermino: '11:00',
    local: 'Praça da Estação',
    destinoFinal: 'Avenida Plácido Castelo',
    status: 'PLANEJADO'
  },
  {
    id: '4',
    nome: 'Seminário de Educação Ambiental nas Escolas',
    descricao: 'Ciclo de palestras e dinâmicas educativas para jovens.',
    dataInicio: '05/09/2026',
    dataTermino: '05/09/2026',
    horarioInicio: '14:00',
    horarioTermino: '17:30',
    local: 'Auditório da AMMA',
    destinoFinal: '',
    status: 'ENCERRADO'
  }
])

const eventosFiltrados = computed(() => {
  if (!searchQuery.value) return eventos.value
  const query = searchQuery.value.toLowerCase()
  return eventos.value.filter(evt =>
    evt.nome.toLowerCase().includes(query) ||
    evt.local.toLowerCase().includes(query) ||
    (evt.destinoFinal && evt.destinoFinal.toLowerCase().includes(query))
  )
})

const navigateToNew = () => {
  router.push({ name: 'eventos-cadastrar' })
}

const statusClass = (status: string) => {
  switch (status) {
    case 'CONFIRMADO':
      return 'bg-amma-green/20 text-amma-green-dark'
    case 'PLANEJADO':
      return 'bg-blue-100 text-blue-700'
    case 'ENCERRADO':
      return 'bg-black/10 text-grey-text'
    default:
      return 'bg-orange-100 text-orange-700'
  }
}
</script>

<template>
  <div class="px-6 md:px-10 pt-6 flex flex-col gap-6">
    <AppDataTable
      v-model:search="searchQuery"
      :columns="colunas"
      :items="eventosFiltrados"
      search-placeholder="Pesquisar por nome do evento ou local..."
      button-label="Cadastrar Evento"
      button-icon="add"
      empty-message="Nenhum evento socioambiental encontrado."
      @action-click="navigateToNew"
    >
      <template #nome="{ item }">
        <span class="font-semibold text-grey-text block">{{ item.nome }}</span>
        <span v-if="item.destinoFinal" class="text-xs text-grey-text/70 block mt-0.5">
          Destino: {{ item.destinoFinal }}
        </span>
      </template>

      <template #periodo="{ item }">
        <span class="text-sm font-medium text-grey-text/80 whitespace-nowrap">
          {{ item.dataInicio === item.dataTermino ? item.dataInicio : `${item.dataInicio} - ${item.dataTermino}` }}
        </span>
      </template>

      <template #horario="{ item }">
        <span class="text-xs font-semibold text-grey-text/80 bg-black/5 px-2.5 py-1 rounded-full whitespace-nowrap">
          {{ item.horarioInicio }} - {{ item.horarioTermino }}
        </span>
      </template>

      <template #local="{ item }">
        <span class="text-sm font-medium text-grey-text block truncate max-w-xs" :title="item.local">
          {{ item.local }}
        </span>
      </template>

      <template #status="{ item }">
        <span 
          :class="[
            'text-xs font-bold px-3 py-1.5 rounded-full inline-block min-w-[100px]',
            statusClass(item.status)
          ]"
        >
          {{ item.status }}
        </span>
      </template>

      <template #acoes>
        <div class="flex items-center justify-center gap-2">
          <button 
            type="button"
            class="w-8 h-8 flex items-center justify-center text-amma-green hover:bg-amma-green-lighter rounded-full transition-colors efeito-zoom cursor-pointer" 
            title="Visualizar Detalhes"
          >
            <span class="material-symbols-outlined text-[20px]">event</span>
          </button>
          <button 
            type="button"
            class="w-8 h-8 flex items-center justify-center text-grey-text hover:text-amma-green-dark hover:bg-amma-green-lighter rounded-full transition-colors efeito-zoom cursor-pointer" 
            title="Editar Evento"
          >
            <span class="material-symbols-outlined text-[20px]">edit</span>
          </button>
        </div>
      </template>
    </AppDataTable>
  </div>
</template>