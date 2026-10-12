<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
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
  { key: 'acoes', label: 'Ações', align: 'center' },
]

const eventos = ref([])

const carregarEventos = async () => {
  try {
    const resposta = await fetch('http://localhost:8080/eventos')
    if (resposta.ok) {
      // Pega o JSON devolvido pelo Spring Boot e guarda na variável reativa
      const dados = await resposta.json()
      eventos.value = dados
    } else {
      console.error('Falha ao carregar eventos:', resposta.status)
    }
  } catch (erro) {
    console.error('Erro de conexão ao buscar eventos:', erro)
  }
}

onMounted(() => {
  carregarEventos()
})

const eventosFiltrados = computed(() => {
  if (!searchQuery.value) return eventos.value
  const query = searchQuery.value.toLowerCase()
  return eventos.value.filter(
    (evt) =>
      (evt.nome && evt.nome.toLowerCase().includes(query)) ||
      (evt.localConcentracao && evt.localConcentracao.toLowerCase().includes(query)) ||
      (evt.destinoFinal && evt.destinoFinal.toLowerCase().includes(query)),
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
          {{
            item.dataInicio === item.dataTermino
              ? item.dataInicio
              : `${item.dataInicio} - ${item.dataTermino}`
          }}
        </span>
      </template>

      <template #horario="{ item }">
        <span
          class="text-xs font-semibold text-grey-text/80 bg-black/5 px-2.5 py-1 rounded-full whitespace-nowrap"
        >
          {{ item.horarioInicio }} - {{ item.horarioTermino }}
        </span>
      </template>

      <template #local="{ item }">
        <span
          class="text-sm font-medium text-grey-text block truncate max-w-xs"
          :title="item.localConcentracao"
        >
          <!-- Altere item.local para item.localConcentracao -->
          {{ item.localConcentracao }}
        </span>
      </template>

      <template #status="{ item }">
        <span
          :class="[
            'text-xs font-bold px-3 py-1.5 rounded-full inline-block min-w-[100px]',
            statusClass(item.status),
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
