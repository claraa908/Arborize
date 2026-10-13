<script setup lang="ts">
import { ref, computed, onMounted } from 'vue' // onMounted adicionado
import { useRouter } from 'vue-router'
import AppDataTable, { type TableColumn } from '@/components/ui/AppDataTable.vue'

const router = useRouter()
const searchQuery = ref('')

const colunas: TableColumn[] = [
  { key: 'especie', label: 'Espécie Botânica', width: '35%' },
  { key: 'categoria', label: 'Categoria' },
  { key: 'porteArvore', label: 'Porte', align: 'center' }, // Atualizado
  { key: 'recomendadaCalcadas', label: 'Calçadas', align: 'center' }, // Atualizado
  { key: 'mudasDisponiveis', label: 'Estoque Mudas', align: 'center' },
  { key: 'acoes', label: 'Ações', align: 'center' },
]

const especies = ref([])

const carregarEspecies = async () => {
  try {
    const resposta = await fetch('http://localhost:8080/especies')
    if (resposta.ok) {
      const dados = await resposta.json()
      especies.value = dados
    } else {
      console.error('Falha ao carregar espécies:', resposta.status)
    }
  } catch (erro) {
    console.error('Erro de conexão ao buscar espécies:', erro)
  }
}

onMounted(() => {
  carregarEspecies()
})

const especiesFiltradas = computed(() => {
  if (!searchQuery.value) return especies.value
  const query = searchQuery.value.toLowerCase()
  return especies.value.filter(
    (esp) =>
      (esp.nomePopular && esp.nomePopular.toLowerCase().includes(query)) ||
      (esp.nomeCientifico && esp.nomeCientifico.toLowerCase().includes(query)) ||
      (esp.categoria && esp.categoria.toLowerCase().includes(query)),
  )
})

const navigateToNew = () => {
  router.push({ name: 'viveiro-cadastrar' })
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
              : 'bg-red-100 text-red-700',
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

      <template #acoes>
        <div class="flex items-center justify-center gap-2">
          <button
            type="button"
            class="w-8 h-8 flex items-center justify-center text-amma-green hover:bg-amma-green-lighter rounded-full transition-colors efeito-zoom cursor-pointer"
            title="Visualizar Detalhes"
          >
            <span class="material-symbols-outlined text-[20px]">psychiatry</span>
          </button>
          <button
            type="button"
            class="w-8 h-8 flex items-center justify-center text-grey-text hover:text-amma-green-dark hover:bg-amma-green-lighter rounded-full transition-colors efeito-zoom cursor-pointer"
            title="Editar Espécie"
          >
            <span class="material-symbols-outlined text-[20px]">edit</span>
          </button>
        </div>
      </template>
    </AppDataTable>
  </div>
</template>
