<script setup lang="ts">
import { ref } from 'vue'
import { AxiosError } from 'axios'
import { useAuthStore } from '@/stores/authStore'

defineProps<{
  isOpen: boolean
}>()

const emit = defineEmits<{
  (e: 'close'): void
  (e: 'login-success'): void
}>()

const auth = useAuthStore()
const showPassword = ref(false)
const login = ref('')
const password = ref('')
const errorMessage = ref('')
const isSubmitting = ref(false)

const handleSubmit = async () => {
  errorMessage.value = ''
  isSubmitting.value = true

  try {
    await auth.login({ login: login.value.trim(), senha: password.value })
    password.value = ''
    emit('close')
    emit('login-success')
  } catch (error) {
    errorMessage.value = error instanceof AxiosError && error.response?.status === 401
      ? 'Credenciais inválidas. Tente novamente.'
      : 'Não foi possível entrar. Verifique sua conexão e tente novamente.'
  } finally {
    isSubmitting.value = false
  }
}
</script>

<template>
  <Transition
    enter-active-class="ease-out duration-300"
    enter-from-class="opacity-0"
    enter-to-class="opacity-100"
    leave-active-class="ease-in duration-200"
    leave-from-class="opacity-100"
    leave-to-class="opacity-0"
  >
    <div v-if="isOpen" class="fixed inset-0 z-50 flex items-center justify-center bg-black/30 p-4 backdrop-blur-md">
      <section
        role="dialog"
        aria-modal="true"
        aria-labelledby="login-title"
        class="relative w-full max-w-md bg-white border border-white/40 rounded-[2.5rem] p-8 md:p-10 shadow-[0_20px_50px_rgba(0,0,0,0.15)] flex flex-col items-center"
      >
        <button
          type="button"
          @click="emit('close')"
          aria-label="Fechar login"
          class="absolute -top-3 -right-3 w-14 h-14 bg-amma-green hover:bg-amma-green-dark transition-all duration-300 hover:scale-110 active:scale-95 cursor-pointer flex items-center justify-center text-white focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-amma-green"
          style="border-radius: 40px 10px 40px 40px;"
        >
          <span class="material-symbols-outlined -mt-1 -mr-1">close</span>
        </button>

        <div class="inline-flex items-center gap-1.5 bg-[#e4efdc] text-emerald-800 text-xs font-semibold px-4 py-1.5 rounded-full mb-4">
          <span class="material-symbols-outlined">verified_user</span>
          <span class="leading-none pt-[2px]">Portal do Cidadão - AMMA Quixadá</span>
        </div>

        <h2 id="login-title" class="text-3xl font-extrabold text-amma-green mb-8 tracking-tight">Entrar</h2>

        <div v-if="errorMessage" role="alert" aria-live="assertive" class="w-full bg-red-50 border border-red-200 text-red-700 text-sm font-medium rounded-btn p-3.5 mb-4">
          {{ errorMessage }}
        </div>

        <form @submit.prevent="handleSubmit" class="w-full flex flex-col gap-4">
          <div class="relative">
            <label for="login-identificador" class="sr-only">E-mail ou CPF</label>
            <span class="material-symbols-outlined absolute left-4 top-1/2 -translate-y-1/2 text-amma-green-dark text-[22px]">person</span>
            <input
              id="login-identificador"
              v-model="login"
              name="login"
              type="text"
              autocomplete="username"
              placeholder="E-mail ou CPF"
              required
              aria-label="E-mail ou CPF"
              class="w-full bg-white/70 focus:bg-amma-green-lighter border-2 border-amma-green-light focus:border-amma-green rounded-btn py-4 pl-12 pr-4 text-amma-green-dark hover:bg-amma-green-lighter placeholder-amma-green outline-none focus-visible:ring-2 focus-visible:ring-amma-green transition-all text-base font-medium shadow-inner"
            />
          </div>

          <div class="relative">
            <label for="login-senha" class="sr-only">Senha</label>
            <span class="material-symbols-outlined absolute left-4 top-1/2 -translate-y-1/2 text-amma-green-dark text-[22px]">lock</span>
            <input
              id="login-senha"
              v-model="password"
              name="senha"
              :type="showPassword ? 'text' : 'password'"
              autocomplete="current-password"
              placeholder="Senha"
              required
              aria-label="Senha"
              class="w-full bg-white/70 focus:bg-amma-green-lighter border-2 border-amma-green-light focus:border-amma-green rounded-btn py-4 pl-12 pr-12 text-amma-green-dark hover:bg-amma-green-lighter placeholder-amma-green outline-none focus-visible:ring-2 focus-visible:ring-amma-green transition-all text-base font-medium shadow-inner"
            />
            <button
              type="button"
              @click="showPassword = !showPassword"
              :aria-label="showPassword ? 'Ocultar senha' : 'Mostrar senha'"
              class="absolute right-4 top-1/2 -translate-y-1/2 text-amma-green-dark hover:text-amma-green transition-colors cursor-pointer focus-visible:outline focus-visible:outline-2 focus-visible:outline-amma-green"
            >
              <span class="material-symbols-outlined text-[22px]">{{ showPassword ? 'visibility_off' : 'visibility' }}</span>
            </button>
          </div>

          <button
            type="submit"
            :disabled="isSubmitting"
            class="w-full mt-2 bg-amma-green hover:bg-amma-green-dark text-white font-bold py-4 rounded-btn flex items-center justify-center gap-2 shadow-lg shadow-emerald-900/10 efeito-zoom disabled:opacity-60 disabled:cursor-wait"
          >
            <span class="material-symbols-outlined text-[22px]">login</span>
            <span class="leading-none pt-[2px]">{{ isSubmitting ? 'Entrando...' : 'Entrar' }}</span>
          </button>
        </form>
      </section>
    </div>
  </Transition>
</template>
