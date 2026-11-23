<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { AxiosError } from 'axios'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/authStore'
import { useFeedbackStore } from '@/stores/feedbackStore'
import { alterarMinhaSenha, atualizarMeuPerfil, buscarMeuPerfil } from '@/services/usuarioService'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()
const feedback = useFeedbackStore()

const nome = ref('')
const email = ref('')
const cpf = ref('')
const currentPassword = ref('')
const newPassword = ref('')
const confirmPassword = ref('')

const isLoading = ref(true)
const isProfileLoaded = ref(false)
const isSavingProfile = ref(false)
const isUpdatingPassword = ref(false)
const errorMessage = ref('')
const passwordErrorMessage = ref('')

const userInitials = computed(() => {
  const source = (nome.value || auth.nome || 'Usuário').trim()
  return (
    source
      .split(/\s+/)
      .slice(0, 2)
      .map((part) => part.charAt(0).toUpperCase())
      .join('') || 'U'
  )
})

const roleLabel = computed(
  () =>
    ({
      ROLE_BOLSISTA_IFCE: 'Bolsista IFCE',
      ROLE_GESTOR_IFCE: 'Gestor do Laboratório IFCE',
      ROLE_CIDADAO: 'Cidadão',
      ROLE_ADMINISTRADOR_AMMA: 'Administrador da AMMA',
      ROLE_FUNCIONARIO_AMMA: 'Funcionário da AMMA',
    })[auth.role ?? 'ROLE_FUNCIONARIO_AMMA'],
)

const institutionLabel = computed(() => {
  if (
    auth.role === 'ROLE_GESTOR_IFCE' ||
    auth.role === 'ROLE_BOLSISTA_IFCE'
  ) {
    return 'IFCE - Instituto Federal de Educação, Ciência e Tecnologia do Ceará'
  }
  return 'AMMA - Autarquia Municipal de Meio Ambiente de Quixadá'
})

const sendToLogin = () => {
  auth.logout()
  void router.replace({ name: 'home', query: { auth: 'required', redirect: route.fullPath } })
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
    auth.updateName(usuario.nome)
    isProfileLoaded.value = true
  } catch (error) {
    if (error instanceof AxiosError && error.response?.status === 401) {
      sendToLogin()
      return
    }
    errorMessage.value =
      error instanceof AxiosError && error.response?.status === 403
        ? 'Você não tem permissão para acessar estes dados.'
        : 'Não foi possível carregar seus dados. Tente novamente.'
  } finally {
    isLoading.value = false
  }
}

const handleSaveProfile = async () => {
  errorMessage.value = ''
  if (!nome.value.trim() || !email.value.trim()) {
    errorMessage.value = 'Por favor, preencha todos os campos obrigatórios'
    return
  }

  isSavingProfile.value = true
  try {
    const usuario = await atualizarMeuPerfil({
      nome: nome.value.trim(),
      email: email.value.trim(),
    })
    nome.value = usuario.nome
    email.value = usuario.email
    auth.updateName(usuario.nome)
    feedback.showToast('Perfil atualizado com sucesso', 'success')
  } catch (error) {
    if (error instanceof AxiosError && error.response?.status === 401) {
      sendToLogin()
    } else if (error instanceof AxiosError && error.response?.status === 403) {
      errorMessage.value = 'Você não tem permissão para atualizar estes dados.'
    } else if (error instanceof AxiosError && typeof error.response?.data === 'string' && error.response.data.trim()) {
      errorMessage.value = error.response.data
    } else {
      errorMessage.value = 'Não foi possível salvar as alterações. Tente novamente.'
    }
  } finally {
    isSavingProfile.value = false
  }
}

const handleUpdatePassword = async () => {
  passwordErrorMessage.value = ''

  if (!currentPassword.value || !newPassword.value || !confirmPassword.value) {
    passwordErrorMessage.value = 'Por favor, preencha todos os campos obrigatórios'
    return
  }
  if (newPassword.value.length < 8) {
    passwordErrorMessage.value = 'A nova senha deve ter pelo menos 8 caracteres.'
    return
  }
  if (newPassword.value !== confirmPassword.value) {
    passwordErrorMessage.value = 'As senhas informadas não coincidem.'
    return
  }

  isUpdatingPassword.value = true
  try {
    await alterarMinhaSenha({
      senhaAtual: currentPassword.value,
      novaSenha: newPassword.value,
    })
    currentPassword.value = ''
    newPassword.value = ''
    confirmPassword.value = ''
    feedback.showToast('Senha atualizada com segurança!', 'success')
  } catch (error) {
    if (error instanceof AxiosError && error.response?.status === 401) {
      sendToLogin()
    } else if (error instanceof AxiosError && typeof error.response?.data === 'string' && error.response.data.trim()) {
      passwordErrorMessage.value = error.response.data
    } else {
      passwordErrorMessage.value = 'Não foi possível atualizar a senha. Tente novamente.'
    }
  } finally {
    isUpdatingPassword.value = false
  }
}

onMounted(loadProfile)
</script>

<template>
  <div class="px-6 md:px-10 pt-6 md:pt-6 max-w-5xl mx-auto">
    <div
      v-if="errorMessage"
      role="alert"
      aria-live="assertive"
      class="mb-6 rounded-btn border border-feedback-danger-border bg-feedback-danger-bg p-4 text-sm font-medium text-feedback-danger"
    >
      {{ errorMessage }}
    </div>

    <div
      v-if="isLoading"
      role="status"
      aria-live="polite"
      class="rounded-box border border-amma-green-light bg-white p-8 text-center text-amma-green-dark"
    >
      Carregando seus dados...
    </div>

    <div v-else-if="isProfileLoaded" class="grid grid-cols-1 md:grid-cols-3 gap-8">
      <div class="md:col-span-1 flex flex-col gap-6">
        <div class="bg-white rounded-box shadow-sm border border-amma-green-light p-8 flex flex-col items-center text-center">
          <div class="relative mb-6">
            <div
              class="w-32 h-32 bg-amma-green-light text-amma-green flex items-center justify-center font-bold text-4xl shadow-lg border-4 border-white"
              style="border-radius: 60px 15px 60px 60px;"
            >
              {{ userInitials }}
            </div>
            <button
              type="button"
              aria-label="Foto do perfil"
              class="absolute bottom-0 right-0 w-10 h-10 bg-white border border-amma-green-light text-amma-green hover:text-amma-green rounded-full flex items-center justify-center shadow-sm efeito-zoom"
            >
              <span class="material-symbols-outlined text-[20px]">photo_camera</span>
            </button>
          </div>

          <h3 class="text-xl font-extrabold text-amma-green-dark mb-1">{{ nome }}</h3>
          <span class="text-sm font-semibold text-amma-green-dark bg-amma-green-lighter px-3 py-1 rounded-full mb-4">{{ roleLabel }}</span>
          <p class="text-sm text-grey-text font-medium">{{ institutionLabel }}</p>
        </div>
      </div>

      <div class="md:col-span-2 flex flex-col gap-6">
        <form @submit.prevent="handleSaveProfile" class="bg-white rounded-box shadow-sm border border-amma-green-light p-8">
          <h4 class="text-lg font-bold text-amma-green-dark mb-6 flex items-center gap-2">
            <span class="material-symbols-outlined text-amma-green">person</span>
            Dados Pessoais
          </h4>

          <div class="grid grid-cols-1 md:grid-cols-2 gap-5 mb-6">
            <div class="flex flex-col gap-1.5 md:col-span-2">
              <label for="perfil-nome" class="text-sm font-semibold text-grey-text pl-1">Nome Completo</label>
              <input
                id="perfil-nome"
                v-model="nome"
                type="text"
                class="w-full bg-white focus:bg-amma-green-lighter border-2 border-amma-green-light focus:border-amma-green rounded-btn py-3 px-4 text-grey-text outline-none transition-all text-sm font-medium"
              />
            </div>

            <div class="flex flex-col gap-1.5">
              <label for="perfil-email" class="text-sm font-semibold text-grey-text pl-1">E-mail</label>
              <input
                id="perfil-email"
                v-model="email"
                type="email"
                class="w-full bg-white focus:bg-amma-green-lighter border-2 border-amma-green-light focus:border-amma-green rounded-btn py-3 px-4 text-grey-text outline-none transition-all text-sm font-medium"
              />
            </div>

            <div class="flex flex-col gap-1.5">
              <label for="perfil-cpf" class="text-sm font-semibold text-grey-text pl-1">CPF</label>
              <input
                id="perfil-cpf"
                v-model="cpf"
                type="text"
                class="w-full bg-amma-green-lighter border-2 border-transparent rounded-btn py-3 px-4 text-grey-text/60 outline-none text-sm font-medium cursor-not-allowed"
                disabled
              />
            </div>
          </div>

          <div class="flex justify-end">
            <button
              type="submit"
              :disabled="isSavingProfile"
              class="bg-amma-green hover:bg-amma-green-dark text-white font-bold py-2.5 px-6 rounded-btn shadow-md text-sm border-none outline-none efeito-zoom disabled:opacity-60 disabled:cursor-wait"
            >
              {{ isSavingProfile ? 'Salvando...' : 'Salvar Alterações' }}
            </button>
          </div>
        </form>

        <form @submit.prevent="handleUpdatePassword" class="bg-white rounded-box shadow-sm border border-amma-green-light p-8">
          <h4 class="text-lg font-bold text-amma-green-dark mb-6 flex items-center gap-2">
            <span class="material-symbols-outlined text-amma-green">lock</span>
            Segurança e Senha
          </h4>

          <div
            v-if="passwordErrorMessage"
            role="alert"
            aria-live="assertive"
            class="mb-5 rounded-btn border border-feedback-danger-border bg-feedback-danger-bg p-3.5 text-sm font-medium text-feedback-danger"
          >
            {{ passwordErrorMessage }}
          </div>

          <div class="grid grid-cols-1 gap-5 mb-6">
            <div class="flex flex-col gap-1.5">
              <label for="perfil-senha-atual" class="text-sm font-semibold text-grey-text pl-1">Senha Atual</label>
              <input
                id="perfil-senha-atual"
                v-model="currentPassword"
                type="password"
                autocomplete="current-password"
                class="w-full bg-white focus:bg-amma-green-lighter border-2 border-amma-green-light focus:border-amma-green rounded-btn py-3 px-4 text-grey-text outline-none transition-all text-sm font-medium"
              />
            </div>

            <div class="grid grid-cols-1 md:grid-cols-2 gap-5">
              <div class="flex flex-col gap-1.5">
                <label for="perfil-nova-senha" class="text-sm font-semibold text-grey-text pl-1">Nova Senha</label>
                <input
                  id="perfil-nova-senha"
                  v-model="newPassword"
                  type="password"
                  autocomplete="new-password"
                  class="w-full bg-white focus:bg-amma-green-lighter border-2 border-amma-green-light focus:border-amma-green rounded-btn py-3 px-4 text-grey-text outline-none transition-all text-sm font-medium"
                />
              </div>
              <div class="flex flex-col gap-1.5">
                <label for="perfil-confirmar-senha" class="text-sm font-semibold text-grey-text pl-1">Confirmar Nova Senha</label>
                <input
                  id="perfil-confirmar-senha"
                  v-model="confirmPassword"
                  type="password"
                  autocomplete="new-password"
                  class="w-full bg-white focus:bg-amma-green-lighter border-2 border-amma-green-light focus:border-amma-green rounded-btn py-3 px-4 text-grey-text outline-none transition-all text-sm font-medium"
                />
              </div>
            </div>
          </div>

          <div class="flex justify-end">
            <button
              type="submit"
              :disabled="isUpdatingPassword"
              class="bg-amma-green-dark hover:bg-amma-green text-white font-bold py-2.5 px-6 rounded-btn shadow-md text-sm border-none outline-none efeito-zoom disabled:opacity-60 disabled:cursor-wait"
            >
              {{ isUpdatingPassword ? 'Atualizando...' : 'Atualizar Senha' }}
            </button>
          </div>
        </form>
      </div>
    </div>

    <div v-else class="rounded-box border border-amma-green-light bg-white p-8 text-center">
      <p class="mb-4 text-sm font-medium text-grey-text">Não foi possível exibir seus dados.</p>
      <button
        type="button"
        @click="loadProfile"
        class="rounded-btn bg-amma-green px-5 py-2.5 text-sm font-bold text-white hover:bg-amma-green-dark focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-amma-green"
      >
        Tentar novamente
      </button>
    </div>
  </div>
</template>
