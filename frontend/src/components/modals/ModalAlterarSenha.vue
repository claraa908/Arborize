<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { AxiosError } from 'axios'
import { alterarMinhaSenha } from '@/services/usuarioService'
import { useFeedbackStore } from '@/stores/feedbackStore'

const props = defineProps<{ isOpen: boolean }>()
const emit = defineEmits<{
  (event: 'close'): void
  (event: 'forgot-password'): void
}>()

const feedback = useFeedbackStore()
const senhaAtual = ref('')
const novaSenha = ref('')
const confirmarSenha = ref('')
const mostrarSenhaAtual = ref(false)
const mostrarNovaSenha = ref(false)
const mostrarConfirmacao = ref(false)
const tentouEnviar = ref(false)
const enviando = ref(false)
const errorMessage = ref('')

const senhasDivergem = computed(() => Boolean(confirmarSenha.value) && novaSenha.value !== confirmarSenha.value)

watch([senhaAtual, novaSenha, confirmarSenha], () => {
  errorMessage.value = ''
})

watch(() => props.isOpen, (isOpen) => {
  if (!isOpen) resetForm()
})

function resetForm() {
  senhaAtual.value = ''
  novaSenha.value = ''
  confirmarSenha.value = ''
  mostrarSenhaAtual.value = false
  mostrarNovaSenha.value = false
  mostrarConfirmacao.value = false
  tentouEnviar.value = false
  errorMessage.value = ''
}

async function handleSubmit() {
  tentouEnviar.value = true
  errorMessage.value = ''

  if (!senhaAtual.value || !novaSenha.value || !confirmarSenha.value) {
    errorMessage.value = 'Por favor, preencha todos os campos obrigatórios'
    return
  }
  if (novaSenha.value.length < 8) {
    errorMessage.value = 'A nova senha deve ter pelo menos 8 caracteres.'
    return
  }
  if (novaSenha.value !== confirmarSenha.value) {
    errorMessage.value = 'As senhas informadas não coincidem.'
    return
  }

  enviando.value = true
  try {
    await alterarMinhaSenha({ senhaAtual: senhaAtual.value, novaSenha: novaSenha.value })
    emit('close')
    feedback.showToast('Senha atualizada com segurança!', 'success')
  } catch (error) {
    if (error instanceof AxiosError && typeof error.response?.data === 'string') {
      errorMessage.value = error.response.data
    } else {
      errorMessage.value = 'Não foi possível atualizar a senha. Tente novamente.'
    }
  } finally {
    enviando.value = false
  }
}

function handleForgotPassword() {
  emit('close')
  emit('forgot-password')
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
    <div v-if="isOpen" class="fixed inset-0 z-[70] flex items-center justify-center bg-black/40 p-4 backdrop-blur-sm">
      <section role="dialog" aria-modal="true" aria-labelledby="alterar-senha-title" class="relative w-full max-w-lg rounded-[2rem] border border-white/50 bg-white p-6 shadow-2xl sm:p-9">
        <button type="button" @click="emit('close')" aria-label="Fechar alteração de senha" class="absolute right-4 top-4 flex h-10 w-10 items-center justify-center rounded-full text-grey-text hover:bg-amma-green-lighter hover:text-amma-green-dark focus-visible:outline focus-visible:outline-2 focus-visible:outline-amma-green">
          <span class="material-symbols-outlined">close</span>
        </button>

        <div class="mb-6 pr-10">
          <h2 id="alterar-senha-title" class="text-2xl font-extrabold text-amma-green-dark">Alterar senha</h2>
          <p class="mt-1 text-sm text-grey-text">Confirme sua senha atual para cadastrar uma nova.</p>
        </div>

        <div v-if="errorMessage" role="alert" aria-live="assertive" class="mb-5 rounded-btn border border-red-200 bg-red-50 p-3.5 text-sm font-medium text-red-700">
          {{ errorMessage }}
        </div>

        <form @submit.prevent="handleSubmit" novalidate class="flex flex-col gap-4">
          <div class="flex flex-col gap-1.5">
            <label for="senha-atual" class="pl-1 text-sm font-semibold text-grey-text">Senha atual</label>
            <div class="relative">
              <input id="senha-atual" v-model="senhaAtual" :type="mostrarSenhaAtual ? 'text' : 'password'" autocomplete="current-password" :aria-invalid="tentouEnviar && !senhaAtual" class="w-full rounded-btn border-2 border-amma-green-light bg-white px-4 py-3 pr-12 text-sm text-grey-text outline-none focus:border-amma-green focus-visible:ring-2 focus-visible:ring-amma-green" />
              <button type="button" @click="mostrarSenhaAtual = !mostrarSenhaAtual" :aria-label="mostrarSenhaAtual ? 'Ocultar senha atual' : 'Mostrar senha atual'" class="absolute right-3 top-1/2 -translate-y-1/2 p-1 text-amma-green-dark focus-visible:outline focus-visible:outline-2 focus-visible:outline-amma-green">
                <span class="material-symbols-outlined">{{ mostrarSenhaAtual ? 'visibility_off' : 'visibility' }}</span>
              </button>
            </div>
            <button type="button" @click="handleForgotPassword" class="self-start pl-1 text-sm font-semibold text-amma-green-dark hover:underline focus-visible:outline focus-visible:outline-2 focus-visible:outline-amma-green">
              Esqueci a senha atual
            </button>
          </div>

          <div class="grid grid-cols-1 gap-4 sm:grid-cols-2">
            <div class="flex flex-col gap-1.5">
              <label for="nova-senha" class="pl-1 text-sm font-semibold text-grey-text">Nova senha</label>
              <div class="relative">
                <input id="nova-senha" v-model="novaSenha" :type="mostrarNovaSenha ? 'text' : 'password'" autocomplete="new-password" :aria-invalid="tentouEnviar && novaSenha.length < 8" class="w-full rounded-btn border-2 border-amma-green-light bg-white px-4 py-3 pr-12 text-sm text-grey-text outline-none focus:border-amma-green focus-visible:ring-2 focus-visible:ring-amma-green" />
                <button type="button" @click="mostrarNovaSenha = !mostrarNovaSenha" :aria-label="mostrarNovaSenha ? 'Ocultar nova senha' : 'Mostrar nova senha'" class="absolute right-3 top-1/2 -translate-y-1/2 p-1 text-amma-green-dark focus-visible:outline focus-visible:outline-2 focus-visible:outline-amma-green">
                  <span class="material-symbols-outlined">{{ mostrarNovaSenha ? 'visibility_off' : 'visibility' }}</span>
                </button>
              </div>
              <span class="pl-1 text-xs text-grey-text">Mínimo de 8 caracteres</span>
            </div>

            <div class="flex flex-col gap-1.5">
              <label for="confirmar-nova-senha" class="pl-1 text-sm font-semibold text-grey-text">Confirmar nova senha</label>
              <div class="relative">
                <input id="confirmar-nova-senha" v-model="confirmarSenha" :type="mostrarConfirmacao ? 'text' : 'password'" autocomplete="new-password" :aria-invalid="senhasDivergem" class="w-full rounded-btn border-2 border-amma-green-light bg-white px-4 py-3 pr-12 text-sm text-grey-text outline-none focus:border-amma-green focus-visible:ring-2 focus-visible:ring-amma-green" />
                <button type="button" @click="mostrarConfirmacao = !mostrarConfirmacao" :aria-label="mostrarConfirmacao ? 'Ocultar confirmação' : 'Mostrar confirmação'" class="absolute right-3 top-1/2 -translate-y-1/2 p-1 text-amma-green-dark focus-visible:outline focus-visible:outline-2 focus-visible:outline-amma-green">
                  <span class="material-symbols-outlined">{{ mostrarConfirmacao ? 'visibility_off' : 'visibility' }}</span>
                </button>
              </div>
              <span v-if="senhasDivergem" class="pl-1 text-xs font-medium text-red-700" role="status">As senhas não coincidem.</span>
            </div>
          </div>

          <button type="submit" :disabled="senhasDivergem || enviando" class="mt-2 w-full rounded-btn bg-amma-green px-5 py-3.5 font-bold text-white transition-colors hover:bg-amma-green-dark focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-amma-green disabled:cursor-not-allowed disabled:opacity-50">
            {{ enviando ? 'Atualizando...' : 'Salvar nova senha' }}
          </button>
        </form>
      </section>
    </div>
  </Transition>
</template>
