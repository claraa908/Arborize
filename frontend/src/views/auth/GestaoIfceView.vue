<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { AxiosError } from 'axios'
import AppDataTable, { type TableColumn } from '@/components/ui/AppDataTable.vue'
import { listarBolsistasIfce, type IfceBolsista } from '@/services/ifceBolsistasService'

const bolsistas = ref<IfceBolsista[]>([])
const searchQuery = ref('')
const isLoading = ref(true)
const loadError = ref('')

const colunas: TableColumn[] = [
  { key: 'nome', label: 'Nome', width: '30%' },
  { key: 'email', label: 'E-mail' },
  { key: 'whatsapp', label: 'WhatsApp' },
  { key: 'perfil', label: 'Perfil', align: 'center' },
]

const bolsistasFiltrados = computed(() => {
  const termo = searchQuery.value.trim().toLocaleLowerCase('pt-BR')
  if (!termo) return bolsistas.value
  return bolsistas.value.filter((bolsista) =>
    [bolsista.nome, bolsista.email, bolsista.whatsapp]
      .some((campo) => campo?.toLocaleLowerCase('pt-BR').includes(termo)),
  )
})

const loadScholars = async () => {
  isLoading.value = true
  loadError.value = ''
  try {
    bolsistas.value = await listarBolsistasIfce()
  } catch (error) {
    if (error instanceof AxiosError && error.response?.status === 403) {
      loadError.value = 'Somente o Gestor IFCE pode visualizar a equipe.'
    } else if (error instanceof AxiosError && typeof error.response?.data === 'string') {
      loadError.value = error.response.data
    } else if (error instanceof AxiosError && typeof error.response?.data?.detail === 'string') {
      loadError.value = error.response.data.detail
    } else {
      loadError.value = 'Não foi possível carregar os bolsistas. Verifique sua conexão e tente novamente.'
    }
  } finally {
    isLoading.value = false
  }
}

onMounted(loadScholars)
</script>

<template>
  <section class="px-5 py-6 md:px-10 md:py-8">
    <div class="mb-6 flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
      <div>
        <p class="text-xs font-bold uppercase tracking-wider text-amma-green md:sr-only">Laboratório IFCE</p>
        <h1 class="mt-1 text-2xl font-bold text-amma-green-dark md:sr-only">Equipe do Laboratório</h1>
        <p class="mt-1 max-w-2xl text-sm text-grey-text">Gerencie os bolsistas vinculados à equipe do laboratório.</p>
      </div>
      <RouterLink
        :to="{ name: 'ifce-equipe-novo' }"
        class="efeito-zoom inline-flex min-h-11 items-center justify-center gap-2 rounded-btn bg-amma-green px-5 py-2.5 font-bold text-white hover:bg-amma-green-dark focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-amma-green focus-visible:ring-offset-2"
      >
        <span class="material-symbols-outlined" aria-hidden="true">person_add</span>
        Novo Bolsista
      </RouterLink>
    </div>

    <div v-if="isLoading" class="flex min-h-44 items-center justify-center gap-3 rounded-box border border-amma-green-light bg-white p-6 text-sm font-semibold text-grey-text shadow-sm" role="status">
      <span class="material-symbols-outlined animate-spin text-amma-green" aria-hidden="true">progress_activity</span>
      Carregando equipe…
    </div>

    <div v-else-if="loadError" class="rounded-box border border-feedback-danger-border bg-feedback-danger-bg p-6 text-sm text-feedback-danger shadow-sm" role="alert">
      <p>{{ loadError }}</p>
      <button type="button" class="mt-3 font-bold underline" @click="loadScholars">Tentar novamente</button>
    </div>

    <div v-else-if="bolsistas.length === 0" class="flex min-h-44 flex-col items-center justify-center rounded-box border border-amma-green-light bg-amma-green-lighter p-8 text-center">
      <span class="material-symbols-outlined text-4xl text-amma-green" aria-hidden="true">groups</span>
      <p class="mt-2 font-bold text-amma-green-dark">Nenhum bolsista cadastrado</p>
      <p class="mt-1 text-sm text-grey-text">Use “Novo Bolsista” para adicionar alguém à equipe.</p>
    </div>

    <AppDataTable
      v-else
      v-model:search="searchQuery"
      :columns="colunas"
      :items="bolsistasFiltrados"
      caption="Bolsistas da equipe do Laboratório IFCE"
      search-placeholder="Pesquisar bolsista por nome, e-mail ou WhatsApp..."
      empty-message="Nenhum bolsista encontrado com o filtro aplicado."
      :show-search="true"
      :show-summary="true"
      :allow-view-toggle="false"
      default-view="table"
    >
      <template #nome="{ item }">
        <span class="font-semibold text-amma-green-dark">{{ item.nome }}</span>
      </template>
      <template #email="{ item }">
        <span class="text-sm text-grey-text">{{ item.email }}</span>
      </template>
      <template #whatsapp="{ item }">
        <span class="whitespace-nowrap text-sm text-grey-text">{{ item.whatsapp }}</span>
      </template>
      <template #perfil>
        <span class="inline-flex items-center gap-1.5 rounded-full border border-feedback-success-border bg-feedback-success-bg px-2.5 py-1 text-xs font-bold text-feedback-success">
          <span class="material-symbols-outlined text-sm" aria-hidden="true">science</span>
          Bolsista IFCE
        </span>
      </template>
    </AppDataTable>
  </section>
</template>
