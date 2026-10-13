<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import AppDataTable, { type TableColumn } from '@/components/ui/AppDataTable.vue'
import axios from 'axios'

const router = useRouter()
const searchQuery = ref('')

const colunas: TableColumn[] = [
  { key: 'titulo', label: 'Título do Documento', width: '40%' },
  { key: 'categoria', label: 'Categoria' },
  { key: 'status', label: 'Status', align: 'center' },
  { key: 'data', label: 'Data', align: 'center' },
  { key: 'acoes', label: 'Ações', align: 'center' }
]

const documentos = ref<any[]>([])

// Função para buscar todos os documentos do Admin (incluindo rascunhos)
const carregarDocumentosAdmin = async () => {
  try {
    const response = await axios.get('http://localhost:8080/api/materials/admin')

    // Mapeia os dados vindos do back-end para as chaves que a tabela espera
    documentos.value = response.data.map((item: any) => ({
      id: String(item.id),
      titulo: item.title,
      categoria: item.category,
      status: item.status,
      data: item.uploadDate ? new Date(item.uploadDate).toLocaleDateString('pt-BR') : '-'
    }))
  } catch (error) {
    console.error('Erro ao carregar documentos do admin:', error)
  }
}

// Executa a busca assim que o componente é montado na tela
onMounted(() => {
  carregarDocumentosAdmin()
})

const documentosFiltrados = computed(() => {
  if (!searchQuery.value) return documentos.value
  const query = searchQuery.value.toLowerCase()
  return documentos.value.filter(doc =>
    doc.titulo.toLowerCase().includes(query) ||
    doc.categoria.toLowerCase().includes(query)
  )
})

const navigateToNew = () => {
  router.push({ name: 'biblioteca-nova' })
}

const viewDetails = (id: string) => {
  router.push({ name: 'biblioteca-detalhes', params: { id } })
}
</script>

<template>
  <div class="px-6 md:px-10 pt-6 flex flex-col gap-6">
    <AppDataTable
      v-model:search="searchQuery"
      :columns="colunas"
      :items="documentosFiltrados"
      search-placeholder="Pesquisar por título ou categoria..."
      button-label="Novo Documento"
      button-icon="add"
      empty-message="Nenhum documento encontrado."
      @action-click="navigateToNew"
    >
      <template #titulo="{ item }">
        <span class="font-semibold text-grey-text block">{{ item.titulo }}</span>
      </template>

      <template #categoria="{ item }">
        <span class="text-sm font-medium text-grey-text/80 bg-black/5 px-3 py-1 rounded-full">
          {{ item.categoria }}
        </span>
      </template>

      <template #status="{ item }">
        <span
          :class="[
            'text-xs font-bold px-3 py-1.5 rounded-full inline-block min-w-[100px]',
            item.status === 'PUBLICADO' ? 'bg-amma-green/20 text-amma-green-dark' : 'bg-orange-100 text-orange-700'
          ]"
        >
          {{ item.status }}
        </span>
      </template>

      <template #data="{ item }">
        <span class="text-sm font-medium text-grey-text/80">{{ item.data }}</span>
      </template>

      <template #acoes="{ item }">
        <div class="flex items-center justify-center gap-2">
          <button
            type="button"
            @click="viewDetails(item.id)"
            class="w-8 h-8 flex items-center justify-center text-amma-green hover:bg-amma-green-lighter rounded-full transition-colors efeito-zoom cursor-pointer"
            title="Visualizar Detalhes"
          >
            <span class="material-symbols-outlined text-[20px]">list_alt</span>
          </button>
          <button
            type="button"
            class="w-8 h-8 flex items-center justify-center text-grey-text hover:text-amma-green-dark hover:bg-amma-green-lighter rounded-full transition-colors efeito-zoom cursor-pointer"
            title="Editar Documento"
          >
            <span class="material-symbols-outlined text-[20px]">edit</span>
          </button>
        </div>
      </template>
    </AppDataTable>
  </div>
</template>
