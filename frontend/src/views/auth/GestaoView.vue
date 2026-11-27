<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { AxiosError } from 'axios'
import AppDataTable, { type TableColumn } from '@/components/ui/AppDataTable.vue'
import { listarUsuariosInstitucionais, type InstitutionalUser } from '@/services/adminUsuariosService'

const usuarios = ref<InstitutionalUser[]>([])
const searchQuery = ref('')
const isLoading = ref(false)
const loadError = ref('')

const colunas: TableColumn[] = [
  { key: 'nome', label: 'Nome', width: '28%' },
  { key: 'email', label: 'E-mail' },
  { key: 'role', label: 'Perfil' },
  { key: 'dataCadastro', label: 'Data de cadastro' },
  { key: 'status', label: 'Status', align: 'center' },
]

const roleLabels: Record<InstitutionalUser['role'], string> = {
  FUNCIONARIO_AMMA: 'Funcionário da AMMA',
  GESTOR_IFCE: 'Gestor do IFCE',
}

const usuariosFiltrados = computed(() => {
  const termo = searchQuery.value.trim().toLocaleLowerCase('pt-BR')
  if (!termo) return usuarios.value
  return usuarios.value.filter((usuario) =>
    [usuario.nome, usuario.email, roleLabels[usuario.role]]
      .some((campo) => campo?.toLocaleLowerCase('pt-BR').includes(termo)),
  )
})

const getErrorMessage = (error: unknown, fallback: string) => {
  if (error instanceof AxiosError) {
    const responseData = error.response?.data
    if (typeof responseData === 'string' && responseData.trim()) return responseData
    if (responseData && typeof responseData.detail === 'string') return responseData.detail
  }
  return fallback
}

const loadUsers = async () => {
  isLoading.value = true
  loadError.value = ''
  try {
    usuarios.value = await listarUsuariosInstitucionais()
  } catch (error) {
    loadError.value = getErrorMessage(error, 'Não foi possível carregar os usuários institucionais.')
  } finally {
    isLoading.value = false
  }
}

const formatDate = (value: string) => {
  const date = new Date(value)
  return Number.isNaN(date.getTime())
    ? 'Não informado'
    : new Intl.DateTimeFormat('pt-BR', { dateStyle: 'short', timeStyle: 'short' }).format(date)
}

onMounted(loadUsers)
</script>

<template>
  <section class="px-5 py-6 md:px-10 md:py-8">
    <div class="mb-6 flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
      <div>
        <p class="text-xs font-bold uppercase tracking-wider text-amma-green">Acesso restrito</p>
        <h1 class="mt-1 text-2xl font-bold text-amma-green-dark">Usuários institucionais</h1>
        <p class="mt-1 text-sm text-grey-text">Cadastre funcionários da AMMA e gestores do IFCE.</p>
      </div>
      <div class="flex flex-wrap items-center gap-3">
        <button
          type="button"
          class="rounded-btn border-2 border-amma-green px-4 py-2.5 text-sm font-bold text-amma-green hover:bg-amma-green-lighter focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-amma-green"
          :disabled="isLoading"
          @click="loadUsers"
        >
          Atualizar lista
        </button>
        <RouterLink
          :to="{ name: 'gestao-novo' }"
          class="efeito-zoom inline-flex min-h-11 items-center justify-center gap-2 rounded-btn bg-amma-green px-5 py-2.5 font-bold text-white hover:bg-amma-green-dark focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-amma-green focus-visible:ring-offset-2"
        >
          <span class="material-symbols-outlined" aria-hidden="true">person_add</span>
          Novo Usuário
        </RouterLink>
      </div>
    </div>

    <div v-if="isLoading" class="flex min-h-40 items-center justify-center gap-3 rounded-box border border-amma-green-light bg-white p-6 text-sm font-semibold text-grey-text shadow-sm" role="status">
      <span class="material-symbols-outlined animate-spin text-amma-green" aria-hidden="true">progress_activity</span>
      Carregando usuários institucionais…
    </div>

    <div v-else-if="loadError" class="rounded-box border border-feedback-danger-border bg-feedback-danger-bg p-6 text-sm text-feedback-danger shadow-sm" role="alert">
      <p>{{ loadError }}</p>
      <button type="button" class="mt-3 font-bold underline" @click="loadUsers">Tentar novamente</button>
    </div>

    <div v-else-if="usuarios.length === 0" class="flex min-h-40 flex-col items-center justify-center rounded-box border border-amma-green-light bg-amma-green-lighter p-8 text-center">
      <span class="material-symbols-outlined text-4xl text-amma-green" aria-hidden="true">groups</span>
      <p class="mt-2 font-bold text-amma-green-dark">Nenhum usuário institucional cadastrado</p>
      <p class="mt-1 text-sm text-grey-text">Use “Novo Usuário” para adicionar alguém à equipe.</p>
    </div>

    <AppDataTable
      v-else
      v-model:search="searchQuery"
      :columns="colunas"
      :items="usuariosFiltrados"
      caption="Usuários institucionais cadastrados"
      search-placeholder="Pesquisar por nome, e-mail ou perfil..."
      empty-message="Nenhum usuário encontrado com o filtro aplicado."
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
      <template #role="{ item }">
        <span class="text-sm text-grey-text">{{ roleLabels[item.role] }}</span>
      </template>
      <template #dataCadastro="{ item }">
        <span class="whitespace-nowrap text-sm text-grey-text">{{ formatDate(item.dataCadastro) }}</span>
      </template>
      <template #status="{ item }">
        <span class="inline-flex items-center gap-1.5 rounded-full border border-feedback-success-border bg-feedback-success-bg px-2.5 py-1 text-xs font-bold text-feedback-success">
          <span class="material-symbols-outlined text-sm" aria-hidden="true">check_circle</span>
          {{ item.status === 'ATIVO' ? 'Ativo' : 'Inativo' }}
        </span>
      </template>
    </AppDataTable>
  </section>
</template>
