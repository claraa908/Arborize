<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import AppDataTable, { type TableColumn } from '@/components/ui/AppDataTable.vue'
import { useLibraryStore } from '@/stores/libraryStore'

const router = useRouter()
const libraryStore = useLibraryStore()
const searchQuery = ref('')
const selectedStatusFilter = ref<'TODOS' | 'PUBLICADO' | 'RASCUNHO'>('TODOS')

const colunas: TableColumn[] = [
  { key: 'titulo', label: 'Título do Documento', width: '40%' },
  { key: 'categoria', label: 'Categoria' },
  { key: 'status', label: 'Status', align: 'center' },
  { key: 'data', label: 'Data', align: 'center' },
  { key: 'acoes', label: 'Ações', align: 'center' }
]

onMounted(() => {
  libraryStore.fetchAdminMaterials()
})

const contadores = computed(() => {
  const total = libraryStore.adminMaterials.length
  const publicados = libraryStore.adminMaterials.filter((d) => d.status === 'PUBLICADO').length
  const rascunhos = libraryStore.adminMaterials.filter((d) => d.status === 'RASCUNHO').length
  return { total, publicados, rascunhos }
})

const documentosFiltrados = computed(() => {
  return libraryStore.adminMaterials.filter((doc) => {
    const matchBusca =
      !searchQuery.value ||
      doc.titulo.toLowerCase().includes(searchQuery.value.toLowerCase()) ||
      doc.categoria.toLowerCase().includes(searchQuery.value.toLowerCase())

    const matchStatus =
      selectedStatusFilter.value === 'TODOS' || doc.status === selectedStatusFilter.value

    return matchBusca && matchStatus
  })
})

const navigateToNew = () => {
  router.push({ name: 'biblioteca-nova' })
}

const viewDetails = (id: string) => {
  router.push({ name: 'biblioteca-detalhes', params: { id } })
}

const editDocument = (id: string) => {
  router.push({ name: 'biblioteca-editar', params: { id } })
}

const handleDelete = async (id: string) => {
  await libraryStore.removeDocument(id)
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
        <span>Todos os Registos</span>
        <span class="bg-black/10 px-2 py-0.5 rounded-full text-[11px]">{{ contadores.total }}</span>
      </button>

      <button
        type="button"
        @click="selectedStatusFilter = 'PUBLICADO'"
        :class="[
          'px-4 py-1.5 rounded-btn text-xs font-bold transition-all border-2 flex items-center gap-2 cursor-pointer',
          selectedStatusFilter === 'PUBLICADO'
            ? 'bg-amma-green text-white border-amma-green shadow-xs'
            : 'bg-white text-grey-text border-amma-green-light hover:border-amma-green'
        ]"
      >
        <span class="w-2 h-2 rounded-full bg-emerald-500"></span>
        <span>Publicados no Catálogo</span>
        <span class="bg-black/10 px-2 py-0.5 rounded-full text-[11px]">{{ contadores.publicados }}</span>
      </button>

      <button
        type="button"
        @click="selectedStatusFilter = 'RASCUNHO'"
        :class="[
          'px-4 py-1.5 rounded-btn text-xs font-bold transition-all border-2 flex items-center gap-2 cursor-pointer',
          selectedStatusFilter === 'RASCUNHO'
            ? 'bg-amma-green text-white border-amma-green shadow-xs'
            : 'bg-white text-grey-text border-amma-green-light hover:border-amma-green'
        ]"
      >
        <span class="w-2 h-2 rounded-full bg-amber-500"></span>
        <span>Rascunhos / Arquivados</span>
        <span class="bg-black/10 px-2 py-0.5 rounded-full text-[11px]">{{ contadores.rascunhos }}</span>
      </button>
    </div>

    <AppDataTable
      v-model:search="searchQuery"
      :columns="colunas"
      :items="documentosFiltrados"
      search-placeholder="Pesquisar por título ou categoria..."
      button-label="Novo Documento"
      button-icon="add"
      empty-message="Nenhum documento encontrado com os filtros aplicados."
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
            item.status === 'PUBLICADO'
              ? 'bg-amma-green/20 text-amma-green-dark'
              : 'bg-feedback-warning-bg text-feedback-warning border border-feedback-warning-border'
          ]"
        >
          {{ item.status === 'PUBLICADO' ? 'PUBLICADO' : 'RASCUNHO' }}
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
            @click="editDocument(item.id)"
            class="w-8 h-8 flex items-center justify-center text-grey-text hover:text-amma-green-dark hover:bg-amma-green-lighter rounded-full transition-colors efeito-zoom cursor-pointer"
            title="Editar Documento"
          >
            <span class="material-symbols-outlined text-[20px]">edit</span>
          </button>
          <button
            type="button"
            @click="handleDelete(item.id)"
            class="w-8 h-8 flex items-center justify-center text-feedback-danger hover:bg-feedback-danger-bg rounded-full transition-colors efeito-zoom cursor-pointer"
            title="Excluir Documento"
          >
            <span class="material-symbols-outlined text-[20px]">delete</span>
          </button>
        </div>
      </template>
    </AppDataTable>
  </div>
</template>