<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import AppDataTable, { type TableColumn } from '@/components/ui/AppDataTable.vue'
import { useViveiroStore } from '@/stores/viveiroStore'

const router = useRouter()
const viveiroStore = useViveiroStore()
const searchQuery = ref('')

const colunas: TableColumn[] = [
  { key: 'especie', label: 'Espécie Botânica', width: '35%' },
  { key: 'categoria', label: 'Categoria' },
  { key: 'porteArvore', label: 'Porte', align: 'center' },
  { key: 'recomendadaCalcadas', label: 'Calçadas', align: 'center' },
  { key: 'mudasDisponiveis', label: 'Estoque Mudas', align: 'center' },
  { key: 'acoes', label: 'Ações', align: 'center' },
]

onMounted(() => {
  viveiroStore.fetchEspecies()
})

const especiesFiltradas = computed(() => {
  if (!searchQuery.value) return viveiroStore.especies
  const query = searchQuery.value.toLowerCase()
  return viveiroStore.especies.filter(
    (esp) =>
      (esp.nomePopular && esp.nomePopular.toLowerCase().includes(query)) ||
      (esp.nomeCientifico && esp.nomeCientifico.toLowerCase().includes(query)) ||
      (esp.categoria && esp.categoria.toLowerCase().includes(query)),
  )
})

const navigateToNew = () => {
  router.push({ name: 'viveiro-cadastrar' })
}

const viewDetails = (id: string | number) => {
  router.push({ name: 'viveiro-detalhes', params: { id } })
}

const editEspecie = (id: string | number) => {
  router.push({ name: 'viveiro-editar', params: { id } })
}

const handleDelete = async (id: string | number) => {
  await viveiroStore.removeEspecie(id)
}
</script>

<template>
  <div class="px-6 md:px-10 pt-6 flex flex-col gap-6">
    <AppDataTable
      v-model:search="searchQuery"
      :columns="colunas"
      :items="especiesFiltradas"
      search-placeholder="Pesquisar por nome popular, científico ou categoria..."
      button-label="Cadastrar Espécie"
      button-icon="add"
      empty-message="Nenhuma espécie botânica encontrada."
      @action-click="navigateToNew"
    >
      <template #especie="{ item }">
        <span class="font-semibold text-grey-text block">{{ item.nomePopular }}</span>
        <span class="text-xs italic text-grey-text/70 block">{{ item.nomeCientifico }}</span>
      </template>

      <template #categoria="{ item }">
        <span class="text-sm font-medium text-grey-text/80 bg-black/5 px-3 py-1 rounded-full">
          {{ item.categoria }}
        </span>
      </template>

      <template #porteArvore="{ item }">
        <span class="text-sm font-medium text-grey-text/80">{{ item.porteArvore }}</span>
      </template>

      <template #recomendadaCalcadas="{ item }">
        <span
          :class="[
            'text-xs font-bold px-3 py-1 rounded-full inline-block min-w-[60px]',
            item.recomendadaCalcadas
              ? 'bg-amma-green/20 text-amma-green-dark'
              : 'bg-feedback-danger-bg text-feedback-danger border border-feedback-danger-border',
          ]"
        >
          {{ item.recomendadaCalcadas ? 'Sim' : 'Não' }}
        </span>
      </template>

      <template #mudasDisponiveis="{ item }">
        <span class="text-sm font-bold text-amma-green-dark">
          {{ item.mudasDisponiveis || 0 }} un
        </span>
      </template>

      <template #acoes="{ item }">
        <div class="flex items-center justify-center gap-2">
          <button
            v-if="item.id"
            type="button"
            @click="viewDetails(item.id)"
            class="w-8 h-8 flex items-center justify-center text-amma-green hover:bg-amma-green-lighter rounded-full transition-colors efeito-zoom cursor-pointer"
            title="Visualizar Detalhes"
          >
            <span class="material-symbols-outlined text-[20px]">psychiatry</span>
          </button>
          <button
            v-if="item.id"
            type="button"
            @click="editEspecie(item.id)"
            class="w-8 h-8 flex items-center justify-center text-grey-text hover:text-amma-green-dark hover:bg-amma-green-lighter rounded-full transition-colors efeito-zoom cursor-pointer"
            title="Editar Espécie"
          >
            <span class="material-symbols-outlined text-[20px]">edit</span>
          </button>
          <button
            v-if="item.id"
            type="button"
            @click="handleDelete(item.id)"
            class="w-8 h-8 flex items-center justify-center text-feedback-danger hover:bg-feedback-danger-bg rounded-full transition-colors efeito-zoom cursor-pointer"
            title="Excluir Espécie"
          >
            <span class="material-symbols-outlined text-[20px]">delete</span>
          </button>
        </div>
      </template>
    </AppDataTable>
  </div>
</template>