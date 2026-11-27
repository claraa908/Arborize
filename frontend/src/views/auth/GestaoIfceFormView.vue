<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { AxiosError } from 'axios'
import AppBreadcrumb, { type BreadcrumbItem } from '@/components/ui/AppBreadcrumb.vue'
import { cadastrarBolsistaIfce } from '@/services/ifceBolsistasService'
import { formatBrazilianPhone } from '@/utils/phone'
import { useFeedbackStore } from '@/stores/feedbackStore'

const router = useRouter()
const feedback = useFeedbackStore()

const isSubmitting = ref(false)
const attemptedSubmit = ref(false)
const formError = ref('')
const nomeInput = ref<HTMLInputElement | null>(null)
const nome = ref('')
const cpf = ref('')
const email = ref('')
const whatsapp = ref('')
const senha = ref('')

const breadcrumbItems: BreadcrumbItem[] = [
  { label: 'Equipe do Laboratório', icon: 'groups', routeName: 'ifce-equipe' },
  { label: 'Novo Bolsista' },
]

const hasValidCpf = computed(() => cpf.value.replace(/\D/g, '').length === 11)

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

const goBack = () => {
  void router.push({ name: 'ifce-equipe' })
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
    await cadastrarBolsistaIfce({
      nome: nome.value.trim(),
      cpf: cpf.value.replace(/\D/g, ''),
      email: email.value.trim(),
      whatsapp: whatsapp.value,
      senha: senha.value,
    })
    feedback.showToast('Bolsista cadastrado com sucesso!', 'success')
    goBack()
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

onMounted(() => {
  nomeInput.value?.focus()
})
</script>

<template>
  <div class="flex w-full flex-col">
    <AppBreadcrumb :items="breadcrumbItems" />

    <div class="flex flex-col gap-6 px-6 pb-8 pt-6 md:px-10">
      <header class="flex items-center gap-4">
        <div
          aria-hidden="true"
          class="flex h-12 w-12 shrink-0 items-center justify-center rounded-full bg-amma-green-lighter/50 text-amma-green shadow-sm"
        >
          <span class="material-symbols-outlined text-[24px]">person_add</span>
        </div>
        <div>
          <h1 class="text-2xl font-bold leading-none text-amma-green-dark">Cadastrar bolsista</h1>
          <p class="mt-1 text-sm text-grey-text">Adicione um novo bolsista vinculado à equipe do laboratório IFCE.</p>
        </div>
      </header>

      <section class="rounded-box border border-amma-green-light bg-white p-6 shadow-sm md:p-8">
        <form class="space-y-4" @submit.prevent="submitScholar">
          <div class="grid grid-cols-1 gap-4 md:grid-cols-2">
            <div class="md:col-span-2">
              <label for="ifce-scholar-name" class="mb-1.5 block text-sm font-semibold text-grey-text">Nome completo *</label>
              <input id="ifce-scholar-name" ref="nomeInput" v-model="nome" autocomplete="name" required class="w-full rounded-btn border-2 border-amma-green-light bg-white px-4 py-3 text-sm text-grey-text outline-none transition focus:border-amma-green focus:bg-amma-green-lighter focus-visible:ring-2 focus-visible:ring-amma-green" />
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

          <div class="flex flex-col-reverse gap-3 border-t border-amma-green-light pt-5 sm:flex-row sm:justify-end">
            <button type="button" :disabled="isSubmitting" class="rounded-btn border-2 border-amma-green px-5 py-2.5 font-bold text-amma-green hover:bg-amma-green-lighter focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-amma-green disabled:opacity-60" @click="goBack">
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
  </div>
</template>
