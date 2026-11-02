<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { AxiosError } from 'axios'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/authStore'
import { useFeedbackStore } from '@/stores/feedbackStore'
import { atualizarMeuPerfil, buscarMeuPerfil } from '@/services/usuarioService'
import ModalAlterarSenha from '@/components/modals/ModalAlterarSenha.vue'
import { formatBrazilianPhone } from '@/utils/phone'

const router = useRouter()
const auth = useAuthStore()
const feedback = useFeedbackStore()

const nome = ref('')
const email = ref('')
const cpf = ref('')
const whatsapp = ref('')
const endereco = ref('')
const isLoading = ref(true)
const isProfileLoaded = ref(false)
const isSaving = ref(false)
const isPasswordModalOpen = ref(false)
const errorMessage = ref('')

const profileMetrics = [
  { value: '2', label: 'Solicitações de substituição botânica', icon: 'sync' },
  { value: '5', label: 'Doações de mudas e insumos', icon: 'psychiatry' },
  { value: '12', label: 'Mudas coletadas', icon: 'psychiatry' }
]

const handleWhatsAppInput = (event: Event) => {
  whatsapp.value = formatBrazilianPhone((event.target as HTMLInputElement).value)
}

const sendToLogin = () => {
  auth.logout()
  void router.replace({ name: 'home', query: { auth: 'required', redirect: '/perfil' } })
}

const handleLoadError = (error: unknown) => {
  if (error instanceof AxiosError && error.response?.status === 401) {
    sendToLogin()
    return
  }
  errorMessage.value = 'Não foi possível carregar seus dados. Tente novamente.'
}

const loadProfile = async () => {
  isLoading.value = true
  isProfileLoaded.value = false
  errorMessage.value = ''
  try {
    const usuario = await buscarMeuPerfil()
    nome.value = usuario.nome
    email.value = usuario.email
    cpf.value = usuario.cpf.replace(/(\d{3})(\d{3})(\d{3})(\d{2})/, '$1.$2.$3-$4')
    whatsapp.value = formatBrazilianPhone(usuario.whatsapp)
    endereco.value = usuario.endereco
    auth.updateName(usuario.nome)
    isProfileLoaded.value = true
  } catch (error) {
    handleLoadError(error)
  } finally {
    isLoading.value = false
  }
}

const handleSubmit = async () => {
  errorMessage.value = ''
  isSaving.value = true
  try {
    const usuario = await atualizarMeuPerfil({
      nome: nome.value.trim(),
      whatsapp: whatsapp.value,
      endereco: endereco.value.trim()
    })
    nome.value = usuario.nome
    whatsapp.value = formatBrazilianPhone(usuario.whatsapp)
    endereco.value = usuario.endereco
    auth.updateName(usuario.nome)
    feedback.showToast('Perfil atualizado com sucesso', 'success')
  } catch (error) {
    if (error instanceof AxiosError && error.response?.status === 401) {
      sendToLogin()
    } else if (error instanceof AxiosError && typeof error.response?.data === 'string') {
      errorMessage.value = error.response.data
    } else {
      errorMessage.value = 'Não foi possível salvar as alterações. Tente novamente.'
    }
  } finally {
    isSaving.value = false
  }
}

const handleForgotPassword = () => {
  feedback.showToast('A recuperação de senha por e-mail estará disponível em breve.', 'info')
}

onMounted(loadProfile)
</script>

<template>
  <div class="min-h-full bg-[#eff1eb] px-4 py-6 sm:px-6 md:px-10 md:py-10">
    <section class="mx-auto w-full max-w-6xl rounded-[24px] bg-white p-5 shadow-lg sm:p-8 lg:p-10">
      <div v-if="errorMessage" role="alert" aria-live="assertive" class="mb-6 rounded-btn border border-red-200 bg-red-50 p-4 text-sm font-medium text-red-700">
        {{ errorMessage }}
      </div>

      <div v-if="isLoading" role="status" aria-live="polite" class="rounded-box border border-amma-green-light bg-white p-8 text-center text-amma-green-dark">
        Carregando seus dados...
      </div>

      <div v-else-if="isProfileLoaded">
        <header class="mb-7 sm:mb-8">
          <h1 class="text-3xl font-extrabold tracking-tight text-[#073c35] sm:text-5xl">Meu Perfil</h1>
          <p class="mt-2 text-base text-[#123c36] sm:text-2xl">Gerencie suas informações pessoais e visualize seu histórico</p>
        </header>

        <section aria-label="Resumo do histórico" class="mb-7 grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3 sm:gap-5">
          <article v-for="metric in profileMetrics" :key="metric.label" class="min-h-40 rounded-[18px] border-2 border-[#314c31] bg-[#edf2e8] p-5 sm:p-6">
            <div class="flex items-start justify-between gap-4">
              <p class="text-4xl font-extrabold leading-none text-[#183f27] sm:text-5xl">{{ metric.value }}</p>
              <span class="flex h-12 w-12 shrink-0 items-center justify-center rounded-[14px] bg-[#d9e2cf] text-[#234a2e]">
                <span class="material-symbols-outlined text-[26px]" aria-hidden="true">{{ metric.icon }}</span>
              </span>
            </div>
            <p class="mt-4 max-w-[18rem] text-lg leading-snug text-[#171b18] sm:text-xl">{{ metric.label }}</p>
          </article>
        </section>

        <div class="mb-7 border-t border-[#b8b8b8] sm:mb-8" aria-hidden="true"></div>

        <form @submit.prevent="handleSubmit" class="space-y-6">
          <div class="grid grid-cols-1 gap-x-6 gap-y-5 sm:grid-cols-2 sm:gap-y-6">
            <div class="flex flex-col gap-2">
              <label for="perfil-nome" class="text-base font-medium text-[#1f2825] sm:text-lg">Nome Completo</label>
              <input id="perfil-nome" v-model="nome" name="nome" type="text" autocomplete="name" required
                class="w-full rounded-[16px] border-2 border-[#314c31] bg-[#f1f2f0] px-4 py-3 text-base text-[#1c2522] outline-none transition focus:border-amma-green focus:ring-2 focus:ring-amma-green/30 sm:px-5 sm:py-3.5 sm:text-lg" />
            </div>

            <div class="flex flex-col gap-2">
              <label for="perfil-cpf" class="text-base font-medium text-[#1f2825] sm:text-lg">CPF</label>
              <div class="relative">
                <input id="perfil-cpf" :value="cpf" type="text" disabled
                  class="w-full cursor-not-allowed rounded-[16px] border-2 border-[#b7b7b7] bg-[#dedede] px-4 py-3 pr-12 text-base text-[#555] outline-none sm:px-5 sm:py-3.5 sm:text-lg" />
                <span class="material-symbols-outlined pointer-events-none absolute right-4 top-1/2 -translate-y-1/2 text-[22px] text-[#8b8b8b]" aria-hidden="true">lock</span>
              </div>
            </div>

            <div class="flex flex-col gap-2">
              <label for="perfil-email" class="text-base font-medium text-[#1f2825] sm:text-lg">E-mail</label>
              <div class="relative">
                <input id="perfil-email" :value="email" type="email" disabled
                  class="w-full cursor-not-allowed rounded-[16px] border-2 border-[#b7b7b7] bg-[#dedede] px-4 py-3 pr-12 text-base text-[#555] outline-none sm:px-5 sm:py-3.5 sm:text-lg" />
                <span class="material-symbols-outlined pointer-events-none absolute right-4 top-1/2 -translate-y-1/2 text-[22px] text-[#8b8b8b]" aria-hidden="true">lock</span>
              </div>
            </div>

            <div class="flex flex-col gap-2">
              <label for="perfil-whatsapp" class="text-base font-medium text-[#1f2825] sm:text-lg">WhatsApp</label>
              <input id="perfil-whatsapp" :value="whatsapp" @input="handleWhatsAppInput" name="whatsapp" type="tel" inputmode="tel" autocomplete="tel" required placeholder="(00) 00000-0000"
                class="w-full rounded-[16px] border-2 border-[#314c31] bg-[#f1f2f0] px-4 py-3 text-base text-[#1c2522] outline-none transition focus:border-amma-green focus:ring-2 focus:ring-amma-green/30 sm:px-5 sm:py-3.5 sm:text-lg" />
            </div>

            <div class="flex flex-col gap-2 sm:col-span-2">
              <label for="perfil-endereco" class="text-base font-medium text-[#1f2825] sm:text-lg">Endereço Completo</label>
              <input id="perfil-endereco" v-model="endereco" name="endereco" type="text" autocomplete="street-address" required placeholder="Rua, Número, Bairro"
                class="w-full rounded-[16px] border-2 border-[#314c31] bg-[#f1f2f0] px-4 py-3 text-base text-[#1c2522] outline-none transition focus:border-amma-green focus:ring-2 focus:ring-amma-green/30 sm:px-5 sm:py-3.5 sm:text-lg" />
            </div>
          </div>

          <div class="flex flex-wrap justify-end gap-3 pt-1 sm:gap-4">
            <button type="button" @click="isPasswordModalOpen = true" class="rounded-[18px] border-2 border-[#a6a6a6] bg-white px-5 py-3 text-base font-semibold text-[#777] hover:bg-gray-50 focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-amma-green sm:py-3.5">
              Alterar senha
            </button>
            <button type="submit" :disabled="isSaving" class="rounded-[18px] bg-[#194a28] px-6 py-3 text-base font-bold text-white shadow-sm transition-colors hover:bg-amma-green-dark focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-amma-green disabled:cursor-wait disabled:opacity-60 sm:py-3.5">
              {{ isSaving ? 'Salvando...' : 'Salvar alterações' }}
            </button>
            <button type="button" @click="loadProfile" :disabled="isSaving" class="rounded-[18px] border-2 border-[#a6a6a6] bg-[#a6a6a6] px-6 py-3 text-base font-semibold text-white hover:bg-[#898989] focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-amma-green disabled:cursor-wait disabled:opacity-60 sm:py-3.5">
              Cancelar
            </button>
          </div>
        </form>
      </div>

      <div v-else class="rounded-box border border-amma-green-light bg-white p-8 text-center">
        <p class="mb-4 text-sm font-medium text-grey-text">Não foi possível exibir seus dados.</p>
        <button type="button" @click="loadProfile" class="rounded-btn bg-amma-green px-5 py-2.5 text-sm font-bold text-white hover:bg-amma-green-dark focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-amma-green">
          Tentar novamente
        </button>
      </div>
    </section>

    <ModalAlterarSenha :is-open="isPasswordModalOpen" @close="isPasswordModalOpen = false" @forgot-password="handleForgotPassword" />
  </div>
</template>
