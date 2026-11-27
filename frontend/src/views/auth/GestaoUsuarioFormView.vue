<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { AxiosError } from 'axios'
import AppBreadcrumb, { type BreadcrumbItem } from '@/components/ui/AppBreadcrumb.vue'
import { cadastrarUsuarioInstitucional, type InstitutionalRole } from '@/services/adminUsuariosService'
import { formatBrazilianPhone } from '@/utils/phone'
import { useFeedbackStore } from '@/stores/feedbackStore'

const router = useRouter()
const feedback = useFeedbackStore()

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

const breadcrumbItems: BreadcrumbItem[] = [
  { label: 'Painel de Gestão', icon: 'manage_accounts', routeName: 'gestao' },
  { label: 'Novo Usuário' },
]

const hasValidCpf = computed(() => cpf.value.replace(/\D/g, '').length === 11)

const getErrorMessage = (error: unknown, fallback: string) => {
  if (error instanceof AxiosError) {
    const responseData = error.response?.data
    if (typeof responseData === 'string' && responseData.trim()) return responseData
    if (responseData && typeof responseData.detail === 'string') return responseData.detail
  }
  return fallback
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

const goBack = () => {
  void router.push({ name: 'gestao' })
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
    await cadastrarUsuarioInstitucional({
      nome: nome.value.trim(),
      cpf: cpf.value.replace(/\D/g, ''),
      email: email.value.trim(),
      whatsapp: whatsapp.value,
      senha: senha.value,
      role: role.value,
    })
    feedback.showToast('Usuário cadastrado com sucesso', 'success')
    goBack()
  } catch (error) {
    formError.value = getErrorMessage(error, 'Não foi possível cadastrar o usuário. Verifique sua conexão e tente novamente.')
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
          <h1 class="text-2xl font-bold leading-none text-amma-green-dark">Novo usuário institucional</h1>
          <p class="mt-1 text-sm text-grey-text">Cadastre funcionários da AMMA e gestores do IFCE.</p>
        </div>
      </header>

      <section class="rounded-box border border-amma-green-light bg-white p-6 shadow-sm md:p-8">
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

          <div class="flex flex-col-reverse gap-3 border-t border-amma-green-light pt-5 sm:flex-row sm:justify-end">
            <button type="button" :disabled="isSubmitting" class="rounded-btn border-2 border-amma-green px-5 py-2.5 font-bold text-amma-green hover:bg-amma-green-lighter focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-amma-green disabled:opacity-60" @click="goBack">
              Cancelar
            </button>
            <button type="submit" :disabled="isSubmitting" class="efeito-zoom inline-flex items-center justify-center gap-2 rounded-btn bg-amma-green px-6 py-2.5 font-bold text-white hover:bg-amma-green-dark focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-amma-green focus-visible:ring-offset-2 disabled:cursor-wait disabled:opacity-70">
              <span v-if="isSubmitting" class="material-symbols-outlined animate-spin text-lg" aria-hidden="true">progress_activity</span>
              <span v-else class="material-symbols-outlined text-lg" aria-hidden="true">person_add</span>
              {{ isSubmitting ? 'Salvando…' : 'Salvar usuário' }}
            </button>
          </div>
        </form>
      </section>
    </div>
  </div>
</template>
