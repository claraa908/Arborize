<script setup lang="ts">
import { computed, nextTick, onMounted, ref } from 'vue'
import { AxiosError } from 'axios'
import {
  cadastrarUsuarioInstitucional,
  listarUsuariosInstitucionais,
  type InstitutionalRole,
  type InstitutionalUser,
} from '@/services/adminUsuariosService'
import { formatBrazilianPhone } from '@/utils/phone'
import { useFeedbackStore } from '@/stores/feedbackStore'

const feedback = useFeedbackStore()
const usuarios = ref<InstitutionalUser[]>([])
const isLoading = ref(false)
const loadError = ref('')
const isFormOpen = ref(false)
const isSubmitting = ref(false)
const formError = ref('')
const attemptedSubmit = ref(false)
const nomeInput = ref<HTMLInputElement | null>(null)

const nome = ref('')
const cpf = ref('')
const email = ref('')
const whatsapp = ref('')
const senha = ref('')
const role = ref<InstitutionalRole>('FUNCIONARIO_AMMA')

const hasValidCpf = computed(() => cpf.value.replace(/\D/g, '').length === 11)
const roleLabels: Record<InstitutionalUser['role'], string> = {
  FUNCIONARIO_AMMA: 'Funcionário da AMMA',
  GESTOR_IFCE: 'Gestor do IFCE',
  IFCE_GESTOR: 'Gestor do IFCE',
}

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

const resetForm = () => {
  nome.value = ''
  cpf.value = ''
  email.value = ''
  whatsapp.value = ''
  senha.value = ''
  role.value = 'FUNCIONARIO_AMMA'
  formError.value = ''
  attemptedSubmit.value = false
}

const openForm = async () => {
  resetForm()
  isFormOpen.value = true
  await nextTick()
  nomeInput.value?.focus()
}

const closeForm = () => {
  if (isSubmitting.value) return
  isFormOpen.value = false
}

const formatCpf = (event: Event) => {
  const input = event.target as HTMLInputElement
  const digits = input.value.replace(/\D/g, '').slice(0, 11)
  cpf.value = digits
    .replace(/(\d{3})(\d)/, '$1.$2')
    .replace(/(\d{3})(\d)/, '$1.$2')
    .replace(/(\d{3})(\d{1,2})$/, '$1-$2')
}

const formatWhatsapp = (event: Event) => {
  whatsapp.value = formatBrazilianPhone((event.target as HTMLInputElement).value)
}

const formatDate = (value: string) => {
  const date = new Date(value)
  return Number.isNaN(date.getTime())
    ? 'Não informado'
    : new Intl.DateTimeFormat('pt-BR', { dateStyle: 'short', timeStyle: 'short' }).format(date)
}

const submitUser = async () => {
  attemptedSubmit.value = true
  formError.value = ''

  if (!nome.value.trim() || !cpf.value.trim() || !email.value.trim() || !whatsapp.value.trim() || !senha.value) {
    formError.value = 'Preencha todos os campos obrigatórios.'
    return
  }
  if (!hasValidCpf.value) {
    formError.value = 'O CPF deve conter 11 dígitos.'
    return
  }
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email.value.trim())) {
    formError.value = 'Informe um e-mail válido.'
    return
  }

  isSubmitting.value = true
  try {
    const usuario = await cadastrarUsuarioInstitucional({
      nome: nome.value.trim(),
      cpf: cpf.value.replace(/\D/g, ''),
      email: email.value.trim(),
      whatsapp: whatsapp.value,
      senha: senha.value,
      role: role.value,
    })
    usuarios.value = [...usuarios.value, usuario].sort((a, b) => a.nome.localeCompare(b.nome, 'pt-BR'))
    isFormOpen.value = false
    resetForm()
    feedback.showToast('Usuário cadastrado com sucesso', 'success')
  } catch (error) {
    formError.value = getErrorMessage(error, 'Não foi possível cadastrar o usuário. Verifique sua conexão e tente novamente.')
  } finally {
    isSubmitting.value = false
  }
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
      <button
        type="button"
        class="efeito-zoom inline-flex min-h-11 items-center justify-center gap-2 rounded-btn bg-amma-green px-5 py-2.5 font-bold text-white hover:bg-amma-green-dark focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-amma-green focus-visible:ring-offset-2"
        @click="openForm"
      >
        <span class="material-symbols-outlined" aria-hidden="true">person_add</span>
        Novo Usuário
      </button>
    </div>

    <div class="rounded-box border border-amma-green-light bg-white p-4 shadow-sm md:p-6">
      <div class="mb-4 flex items-center justify-between gap-3">
        <div>
          <h2 class="text-lg font-bold text-amma-green-dark">Equipe cadastrada</h2>
          <p class="mt-1 text-sm text-grey-text">{{ usuarios.length }} usuário(s)</p>
        </div>
        <button
          type="button"
          class="rounded-btn border-2 border-amma-green px-3 py-2 text-sm font-bold text-amma-green hover:bg-amma-green-lighter focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-amma-green"
          :disabled="isLoading"
          @click="loadUsers"
        >
          Atualizar lista
        </button>
      </div>

      <div v-if="isLoading" class="flex min-h-40 items-center justify-center gap-3 text-sm font-semibold text-grey-text" role="status">
        <span class="material-symbols-outlined animate-spin text-amma-green" aria-hidden="true">progress_activity</span>
        Carregando usuários institucionais…
      </div>

      <div v-else-if="loadError" class="rounded-btn border border-feedback-danger-border bg-feedback-danger-bg p-4 text-sm text-feedback-danger" role="alert">
        <p>{{ loadError }}</p>
        <button type="button" class="mt-3 font-bold underline" @click="loadUsers">Tentar novamente</button>
      </div>

      <div v-else-if="usuarios.length === 0" class="flex min-h-40 flex-col items-center justify-center rounded-btn bg-amma-green-lighter p-6 text-center">
        <span class="material-symbols-outlined text-4xl text-amma-green" aria-hidden="true">groups</span>
        <p class="mt-2 font-bold text-amma-green-dark">Nenhum usuário institucional cadastrado</p>
        <p class="mt-1 text-sm text-grey-text">Use “Novo Usuário” para adicionar alguém à equipe.</p>
      </div>

      <div v-else class="overflow-x-auto rounded-btn border border-amma-green-light">
        <table class="w-full min-w-[700px] border-collapse text-left">
          <caption class="sr-only">Usuários institucionais cadastrados</caption>
          <thead class="bg-amma-green-lighter text-xs uppercase tracking-wide text-amma-green-dark">
            <tr>
              <th scope="col" class="px-4 py-3 font-bold">Nome</th>
              <th scope="col" class="px-4 py-3 font-bold">E-mail</th>
              <th scope="col" class="px-4 py-3 font-bold">Perfil</th>
              <th scope="col" class="px-4 py-3 font-bold">Data de cadastro</th>
              <th scope="col" class="px-4 py-3 font-bold">Status</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-amma-green-light text-sm text-grey-text">
            <tr v-for="usuario in usuarios" :key="usuario.id" class="hover:bg-amma-green-lighter/60">
              <td class="px-4 py-3 font-semibold text-amma-green-dark">{{ usuario.nome }}</td>
              <td class="px-4 py-3">{{ usuario.email }}</td>
              <td class="px-4 py-3">{{ roleLabels[usuario.role] }}</td>
              <td class="px-4 py-3">{{ formatDate(usuario.dataCadastro) }}</td>
              <td class="px-4 py-3">
                <span class="inline-flex items-center gap-1.5 rounded-full border border-feedback-success-border bg-feedback-success-bg px-2.5 py-1 text-xs font-bold text-feedback-success">
                  <span class="material-symbols-outlined text-sm" aria-hidden="true">check_circle</span>
                  {{ usuario.status === 'ATIVO' ? 'Ativo' : 'Inativo' }}
                </span>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <div
      v-if="isFormOpen"
      class="fixed inset-0 z-[70] flex items-center justify-center overflow-y-auto bg-black/50 p-4"
      @click.self="closeForm"
    >
      <section
        role="dialog"
        aria-modal="true"
        aria-labelledby="new-institutional-user-title"
        class="my-auto max-h-[calc(100vh-2rem)] w-full max-w-2xl overflow-y-auto rounded-box border border-amma-green-light bg-white p-5 shadow-2xl md:p-8"
        @keydown.esc.prevent="closeForm"
      >
        <div class="mb-6 flex items-start justify-between gap-4">
          <div>
            <p class="text-xs font-bold uppercase tracking-wider text-amma-green">Acesso institucional</p>
            <h2 id="new-institutional-user-title" class="mt-1 text-xl font-bold text-amma-green-dark">Novo usuário</h2>
            <p class="mt-1 text-sm text-grey-text">A senha definida será usada no primeiro acesso.</p>
          </div>
          <button
            type="button"
            class="flex h-10 w-10 shrink-0 items-center justify-center rounded-full text-grey-text hover:bg-amma-green-lighter hover:text-amma-green-dark focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-amma-green"
            aria-label="Fechar formulário"
            :disabled="isSubmitting"
            @click="closeForm"
          >
            <span class="material-symbols-outlined" aria-hidden="true">close</span>
          </button>
        </div>

        <form class="space-y-4" @submit.prevent="submitUser">
          <div class="grid grid-cols-1 gap-4 md:grid-cols-2">
            <div class="md:col-span-2">
              <label for="institutional-name" class="mb-1.5 block text-sm font-semibold text-grey-text">Nome completo *</label>
              <input id="institutional-name" ref="nomeInput" v-model="nome" autocomplete="name" required class="w-full rounded-btn border-2 border-amma-green-light bg-white px-4 py-3 text-sm text-grey-text outline-none transition focus:border-amma-green focus:bg-amma-green-lighter focus-visible:ring-2 focus-visible:ring-amma-green" />
            </div>

            <div>
              <label for="institutional-cpf" class="mb-1.5 block text-sm font-semibold text-grey-text">CPF *</label>
              <input id="institutional-cpf" :value="cpf" inputmode="numeric" autocomplete="off" placeholder="000.000.000-00" required :aria-invalid="attemptedSubmit && !hasValidCpf" class="w-full rounded-btn border-2 border-amma-green-light bg-white px-4 py-3 text-sm text-grey-text outline-none transition focus:border-amma-green focus:bg-amma-green-lighter focus-visible:ring-2 focus-visible:ring-amma-green" @input="formatCpf" />
              <p v-if="attemptedSubmit && cpf && !hasValidCpf" class="mt-1 text-xs text-feedback-danger">Informe os 11 dígitos do CPF.</p>
            </div>

            <div>
              <label for="institutional-whatsapp" class="mb-1.5 block text-sm font-semibold text-grey-text">WhatsApp *</label>
              <input id="institutional-whatsapp" :value="whatsapp" inputmode="tel" autocomplete="tel" placeholder="(00) 00000-0000" required class="w-full rounded-btn border-2 border-amma-green-light bg-white px-4 py-3 text-sm text-grey-text outline-none transition focus:border-amma-green focus:bg-amma-green-lighter focus-visible:ring-2 focus-visible:ring-amma-green" @input="formatWhatsapp" />
            </div>

            <div class="md:col-span-2">
              <label for="institutional-email" class="mb-1.5 block text-sm font-semibold text-grey-text">E-mail *</label>
              <input id="institutional-email" v-model="email" type="email" autocomplete="email" required class="w-full rounded-btn border-2 border-amma-green-light bg-white px-4 py-3 text-sm text-grey-text outline-none transition focus:border-amma-green focus:bg-amma-green-lighter focus-visible:ring-2 focus-visible:ring-amma-green" />
            </div>

            <div>
              <label for="institutional-role" class="mb-1.5 block text-sm font-semibold text-grey-text">Nível de acesso *</label>
              <select id="institutional-role" v-model="role" required class="w-full rounded-btn border-2 border-amma-green-light bg-white px-4 py-3 text-sm text-grey-text outline-none transition focus:border-amma-green focus:bg-amma-green-lighter focus-visible:ring-2 focus-visible:ring-amma-green">
                <option value="FUNCIONARIO_AMMA">Funcionário AMMA</option>
                <option value="GESTOR_IFCE">Gestor IFCE</option>
              </select>
            </div>

            <div>
              <label for="institutional-password" class="mb-1.5 block text-sm font-semibold text-grey-text">Senha inicial *</label>
              <input id="institutional-password" v-model="senha" type="password" autocomplete="new-password" required class="w-full rounded-btn border-2 border-amma-green-light bg-white px-4 py-3 text-sm text-grey-text outline-none transition focus:border-amma-green focus:bg-amma-green-lighter focus-visible:ring-2 focus-visible:ring-amma-green" />
            </div>
          </div>

          <p v-if="formError" class="rounded-btn border border-feedback-danger-border bg-feedback-danger-bg p-3 text-sm text-feedback-danger" role="alert" aria-live="polite">{{ formError }}</p>

          <div class="flex flex-col-reverse gap-3 pt-2 sm:flex-row sm:justify-end">
            <button type="button" :disabled="isSubmitting" class="rounded-btn border-2 border-amma-green px-5 py-2.5 font-bold text-amma-green hover:bg-amma-green-lighter focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-amma-green disabled:opacity-60" @click="closeForm">
              Cancelar
            </button>
            <button type="submit" :disabled="isSubmitting" class="efeito-zoom inline-flex items-center justify-center gap-2 rounded-btn bg-amma-green px-6 py-2.5 font-bold text-white hover:bg-amma-green-dark focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-amma-green focus-visible:ring-offset-2 disabled:cursor-wait disabled:opacity-70">
              <span v-if="isSubmitting" class="material-symbols-outlined animate-spin text-lg" aria-hidden="true">progress_activity</span>
              {{ isSubmitting ? 'Salvando…' : 'Salvar usuário' }}
            </button>
          </div>
        </form>
      </section>
    </div>
  </section>
</template>
