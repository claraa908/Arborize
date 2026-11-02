<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { AxiosError } from 'axios'
import { cadastrarBolsistaIfce, listarBolsistasIfce, type IfceBolsista } from '@/services/ifceBolsistasService'
import { formatBrazilianPhone } from '@/utils/phone'
import { useFeedbackStore } from '@/stores/feedbackStore'

const feedback = useFeedbackStore()
const bolsistas = ref<IfceBolsista[]>([])

const isLoading = ref(true)
const loadError = ref('')
const isFormOpen = ref(false)
const isSubmitting = ref(false)
const attemptedSubmit = ref(false)
const formError = ref('')
const nome = ref('')
const cpf = ref('')
const email = ref('')
const whatsapp = ref('')
const senha = ref('')

const hasValidCpf = computed(() => cpf.value.replace(/\D/g, '').length === 11)

const resetForm = () => {
  nome.value = ''
  cpf.value = ''
  email.value = ''
  whatsapp.value = ''
  senha.value = ''
  attemptedSubmit.value = false
  formError.value = ''
}

const openForm = () => {
  resetForm()
  isFormOpen.value = true
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

const submitScholar = async () => {
  attemptedSubmit.value = true
  formError.value = ''
  if (!nome.value.trim() || !hasValidCpf.value || !email.value.trim() || !whatsapp.value.trim() || !senha.value) {
    formError.value = 'Preencha todos os campos obrigatórios.'
    return
  }
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email.value.trim())) {
    formError.value = 'Informe um e-mail válido.'
    return
  }

  isSubmitting.value = true
  try {
    const bolsista = await cadastrarBolsistaIfce({
      nome: nome.value.trim(),
      cpf: cpf.value.replace(/\D/g, ''),
      email: email.value.trim(),
      whatsapp: whatsapp.value,
      senha: senha.value,
    })
    bolsistas.value = [bolsista, ...bolsistas.value]
    closeForm()
    resetForm()
    feedback.showToast('Bolsista cadastrado com sucesso!', 'success')
  } catch (error) {
    if (error instanceof AxiosError && error.response?.status === 403) {
      formError.value = 'Somente o Gestor IFCE pode cadastrar bolsistas.'
    } else if (error instanceof AxiosError && typeof error.response?.data === 'string') {
      formError.value = error.response.data
    } else if (error instanceof AxiosError && typeof error.response?.data?.detail === 'string') {
      formError.value = error.response.data.detail
    } else {
      formError.value = 'Não foi possível cadastrar o bolsista. Verifique sua conexão e tente novamente.'
    }
  } finally {
    isSubmitting.value = false
  }
}

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
        <p class="text-xs font-bold uppercase tracking-wider text-amma-green">Laboratório IFCE</p>
        <h1 class="mt-1 text-2xl font-bold text-amma-green-dark">Equipe do Laboratório</h1>
        <p class="mt-1 max-w-2xl text-sm text-grey-text">Gerencie os bolsistas vinculados à equipe do laboratório.</p>
      </div>
      <button
        type="button"
        class="efeito-zoom inline-flex min-h-11 items-center justify-center gap-2 rounded-btn bg-amma-green px-5 py-2.5 font-bold text-white hover:bg-amma-green-dark focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-amma-green focus-visible:ring-offset-2"
        @click="openForm"
      >
        <span class="material-symbols-outlined" aria-hidden="true">person_add</span>
        Novo Bolsista
      </button>
    </div>

    <div class="rounded-box border border-amma-green-light bg-white p-4 shadow-sm md:p-6">
      <div class="mb-4 flex flex-wrap items-center justify-between gap-3">
        <div>
          <h2 class="text-lg font-bold text-amma-green-dark">Bolsistas cadastrados</h2>
          <p class="mt-1 text-sm text-grey-text">{{ bolsistas.length }} bolsista(s)</p>
        </div>
      </div>

      <div v-if="isLoading" class="flex min-h-44 items-center justify-center gap-3 text-sm font-semibold text-grey-text" role="status">
        <span class="material-symbols-outlined animate-spin text-amma-green" aria-hidden="true">progress_activity</span>
        Carregando equipe…
      </div>

      <div v-else-if="loadError" class="rounded-btn border border-feedback-danger-border bg-feedback-danger-bg p-4 text-sm text-feedback-danger" role="alert">
        <p>{{ loadError }}</p>
        <button type="button" class="mt-3 font-bold underline" @click="loadScholars">Tentar novamente</button>
      </div>

      <div v-else-if="bolsistas.length === 0" class="flex min-h-44 flex-col items-center justify-center rounded-btn bg-amma-green-lighter p-6 text-center">
        <span class="material-symbols-outlined text-4xl text-amma-green" aria-hidden="true">groups</span>
        <p class="mt-2 font-bold text-amma-green-dark">Nenhum bolsista cadastrado</p>
        <p class="mt-1 text-sm text-grey-text">Use “Novo Bolsista” para adicionar alguém à equipe.</p>
      </div>

      <div v-else class="overflow-x-auto rounded-btn border border-amma-green-light">
        <table class="w-full min-w-[620px] border-collapse text-left">
          <caption class="sr-only">Bolsistas da equipe do Laboratório IFCE</caption>
          <thead class="bg-amma-green-lighter text-xs uppercase tracking-wide text-amma-green-dark">
            <tr>
              <th scope="col" class="px-4 py-3 font-bold">Nome</th>
              <th scope="col" class="px-4 py-3 font-bold">E-mail</th>
              <th scope="col" class="px-4 py-3 font-bold">WhatsApp</th>
              <th scope="col" class="px-4 py-3 font-bold">Perfil</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-amma-green-light text-sm text-grey-text">
            <tr v-for="bolsista in bolsistas" :key="bolsista.id" class="hover:bg-amma-green-lighter/60">
              <td class="px-4 py-3 font-semibold text-amma-green-dark">{{ bolsista.nome }}</td>
              <td class="px-4 py-3">{{ bolsista.email }}</td>
              <td class="px-4 py-3">{{ bolsista.whatsapp }}</td>
              <td class="px-4 py-3">
                <span class="inline-flex items-center gap-1.5 rounded-full border border-feedback-success-border bg-feedback-success-bg px-2.5 py-1 text-xs font-bold text-feedback-success">
                  <span class="material-symbols-outlined text-sm" aria-hidden="true">science</span>
                  Bolsista IFCE
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
        aria-labelledby="new-ifce-scholar-title"
        class="my-auto max-h-[calc(100vh-2rem)] w-full max-w-2xl overflow-y-auto rounded-box border border-amma-green-light bg-white p-5 shadow-2xl md:p-8"
        @keydown.esc.prevent="closeForm"
      >
        <div class="mb-6 flex items-start justify-between gap-4">
          <div>
            <p class="text-xs font-bold uppercase tracking-wider text-amma-green">Equipe IFCE</p>
            <h2 id="new-ifce-scholar-title" class="mt-1 text-xl font-bold text-amma-green-dark">Cadastrar bolsista</h2>
            <p class="mt-1 text-sm text-grey-text">Preencha os dados do novo integrante.</p>
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

        <form class="space-y-4" @submit.prevent="submitScholar">
          <div class="grid grid-cols-1 gap-4 md:grid-cols-2">
            <div class="md:col-span-2">
              <label for="ifce-scholar-name" class="mb-1.5 block text-sm font-semibold text-grey-text">Nome completo *</label>
              <input id="ifce-scholar-name" v-model="nome" autocomplete="name" required class="w-full rounded-btn border-2 border-amma-green-light bg-white px-4 py-3 text-sm text-grey-text outline-none transition focus:border-amma-green focus:bg-amma-green-lighter focus-visible:ring-2 focus-visible:ring-amma-green" />
            </div>

            <div>
              <label for="ifce-scholar-cpf" class="mb-1.5 block text-sm font-semibold text-grey-text">CPF *</label>
              <input id="ifce-scholar-cpf" :value="cpf" inputmode="numeric" autocomplete="off" placeholder="000.000.000-00" required :aria-invalid="attemptedSubmit && !hasValidCpf" class="w-full rounded-btn border-2 border-amma-green-light bg-white px-4 py-3 text-sm text-grey-text outline-none transition focus:border-amma-green focus:bg-amma-green-lighter focus-visible:ring-2 focus-visible:ring-amma-green" @input="formatCpf" />
              <p v-if="attemptedSubmit && !hasValidCpf" class="mt-1 text-xs text-feedback-danger">Informe os 11 dígitos do CPF.</p>
            </div>

            <div>
              <label for="ifce-scholar-whatsapp" class="mb-1.5 block text-sm font-semibold text-grey-text">WhatsApp *</label>
              <input id="ifce-scholar-whatsapp" :value="whatsapp" inputmode="tel" autocomplete="tel" placeholder="(00) 00000-0000" required class="w-full rounded-btn border-2 border-amma-green-light bg-white px-4 py-3 text-sm text-grey-text outline-none transition focus:border-amma-green focus:bg-amma-green-lighter focus-visible:ring-2 focus-visible:ring-amma-green" @input="formatWhatsapp" />
            </div>

            <div class="md:col-span-2">
              <label for="ifce-scholar-email" class="mb-1.5 block text-sm font-semibold text-grey-text">E-mail *</label>
              <input id="ifce-scholar-email" v-model="email" type="email" autocomplete="email" required class="w-full rounded-btn border-2 border-amma-green-light bg-white px-4 py-3 text-sm text-grey-text outline-none transition focus:border-amma-green focus:bg-amma-green-lighter focus-visible:ring-2 focus-visible:ring-amma-green" />
            </div>

            <div class="md:col-span-2">
              <label for="ifce-scholar-password" class="mb-1.5 block text-sm font-semibold text-grey-text">Senha inicial *</label>
              <input id="ifce-scholar-password" v-model="senha" type="password" autocomplete="new-password" required class="w-full rounded-btn border-2 border-amma-green-light bg-white px-4 py-3 text-sm text-grey-text outline-none transition focus:border-amma-green focus:bg-amma-green-lighter focus-visible:ring-2 focus-visible:ring-amma-green" />
            </div>
          </div>

          <p v-if="formError" class="rounded-btn border border-feedback-danger-border bg-feedback-danger-bg p-3 text-sm text-feedback-danger" role="alert" aria-live="polite">{{ formError }}</p>

          <div class="flex flex-col-reverse gap-3 pt-2 sm:flex-row sm:justify-end">
            <button type="button" :disabled="isSubmitting" class="rounded-btn border-2 border-amma-green px-5 py-2.5 font-bold text-amma-green hover:bg-amma-green-lighter focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-amma-green disabled:opacity-60" @click="closeForm">
              Cancelar
            </button>
            <button type="submit" :disabled="isSubmitting" class="efeito-zoom inline-flex items-center justify-center gap-2 rounded-btn bg-amma-green px-6 py-2.5 font-bold text-white hover:bg-amma-green-dark focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-amma-green focus-visible:ring-offset-2 disabled:cursor-wait disabled:opacity-70">
              <span v-if="isSubmitting" class="material-symbols-outlined animate-spin text-lg" aria-hidden="true">progress_activity</span>
              <span v-else class="material-symbols-outlined text-lg" aria-hidden="true">person_add</span>
              {{ isSubmitting ? 'Salvando…' : 'Salvar bolsista' }}
            </button>
          </div>
        </form>
      </section>
    </div>
  </section>
</template>
