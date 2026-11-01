<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { AxiosError } from 'axios'
import { cadastrarUsuario } from '@/services/usuarioService'
import { useFeedbackStore } from '@/stores/feedbackStore'
import { formatBrazilianPhone } from '@/utils/phone'

const props = defineProps<{
  isOpen: boolean
}>()

const emit = defineEmits<{
  (e: 'close'): void
  (e: 'open-login'): void
}>()

const nomeCompleto = ref('')
const cpf = ref('')
const email = ref('')
const whatsapp = ref('')
const endereco = ref('')
const senha = ref('')
const confirmarSenha = ref('')

const showPassword = ref(false)
const showConfirmPassword = ref(false)
const errorMessage = ref('')
const isSubmitting = ref(false)
const attemptedSubmit = ref(false)
const feedback = useFeedbackStore()

const isMissing = (value: string) => attemptedSubmit.value && !value.trim()
const passwordsDoNotMatch = computed(() =>
  attemptedSubmit.value && Boolean(confirmarSenha.value) && confirmarSenha.value !== senha.value
)

const resetForm = () => {
  nomeCompleto.value = ''
  cpf.value = ''
  email.value = ''
  whatsapp.value = ''
  endereco.value = ''
  senha.value = ''
  confirmarSenha.value = ''
  showPassword.value = false
  showConfirmPassword.value = false
  errorMessage.value = ''
  attemptedSubmit.value = false
}

watch([nomeCompleto, cpf, email, whatsapp, endereco, senha, confirmarSenha], () => {
  errorMessage.value = ''
})

watch(() => props.isOpen, (isOpen) => {
  if (!isOpen) resetForm()
})

const formatCPF = (event: Event) => {
  const input = event.target as HTMLInputElement
  let v = input.value.replace(/\D/g, '')
  if (v.length > 11) v = v.slice(0, 11)
  v = v.replace(/(\d{3})(\d)/, '$1.$2')
  v = v.replace(/(\d{3})(\d)/, '$1.$2')
  v = v.replace(/(\d{3})(\d{1,2})$/, '$1-$2')
  cpf.value = v
}

const formatWhatsApp = (event: Event) => {
  const input = event.target as HTMLInputElement
  whatsapp.value = formatBrazilianPhone(input.value)
}

const handleSubmit = async () => {
  errorMessage.value = ''
  attemptedSubmit.value = true

  if (!nomeCompleto.value.trim() || !cpf.value.trim() || !email.value.trim() ||
      !whatsapp.value.trim() || !endereco.value.trim() || !senha.value || !confirmarSenha.value) {
    errorMessage.value = 'Por favor, preencha todos os campos obrigatórios'
    return
  }

  if (cpf.value.replace(/\D/g, '').length !== 11) {
    errorMessage.value = 'O CPF deve conter 11 dígitos.'
    return
  }

  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email.value.trim())) {
    errorMessage.value = 'Informe um e-mail válido.'
    return
  }

  if (senha.value !== confirmarSenha.value) {
    errorMessage.value = 'As senhas informadas não coincidem.'
    return
  }

  isSubmitting.value = true
  try {
    await cadastrarUsuario({
      nome: nomeCompleto.value.trim(),
      cpf: cpf.value.replace(/\D/g, ''),
      email: email.value.trim(),
      whatsapp: whatsapp.value,
      endereco: endereco.value.trim(),
      senha: senha.value
    })

    feedback.showToast('Cadastro realizado com sucesso', 'success')
    emit('close')
    emit('open-login')
  } catch (error) {
    const responseData = error instanceof AxiosError ? error.response?.data : undefined
    errorMessage.value = typeof responseData === 'string'
      ? responseData
      : 'Não foi possível concluir o cadastro. Verifique sua conexão e tente novamente.'
  } finally {
    isSubmitting.value = false
  }
}

const switchToLogin = () => {
  emit('close')
  emit('open-login')
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
    <div v-if="isOpen" class="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4 backdrop-blur-md">
      <!-- Container principal com overflow-visible para o botão "folhinha" não ser cortado -->
      <div class="relative w-full max-w-2xl bg-white border border-white/40 rounded-[2.5rem] p-8 md:p-10 shadow-[0_20px_50px_rgba(0,0,0,0.15)] flex flex-col items-center overflow-visible">
        
        <!-- Botão fechar (folhinha) flutuando para fora, visível e sem cortes -->
        <button 
          type="button"
          @click="emit('close')" 
          aria-label="Fechar cadastro"
          class="absolute -top-3.5 -right-3.5 w-14 h-14 bg-amma-green hover:bg-amma-green-dark transition-all duration-300 hover:scale-110 hover:shadow-lg active:scale-95 cursor-pointer flex items-center justify-center text-white focus:outline-none z-50" 
          style="border-radius: 40px 10px 40px 40px;"
        >
          <span class="material-symbols-outlined -mt-1 -mr-1 text-[24px]">close</span>
        </button>

        <!-- Selo do Portal do Cidadão -->
        <div class="inline-flex items-center gap-1.5 bg-[#e4efdc] text-emerald-800 text-xs font-semibold px-4 py-1.5 rounded-full mb-3">
          <span class="material-symbols-outlined text-[18px]">how_to_reg</span>
          <span class="leading-none pt-[2px]">Portal do Cidadão - AMMA Quixadá</span>
        </div>

        <h2 class="text-3.5xl font-extrabold text-amma-green mb-1 tracking-tight">Criar Conta</h2>
        <p class="text-xs text-grey-text/80 mb-6 text-center max-w-md">Cadastre-se para solicitar mudas, laudos e acompanhar processos ambientais de forma rápida e segura.</p>

        <!-- Mensagem de erro -->
        <div v-if="errorMessage" class="w-full bg-red-50 border border-red-200 text-red-700 text-xs font-medium rounded-btn p-3.5 mb-4 flex items-center gap-2" role="alert" aria-live="assertive">
          <span class="material-symbols-outlined text-[18px]">error</span>
          <span>{{ errorMessage }}</span>
        </div>

        <!-- Conteúdo Interno com Rolagem apenas vertical e espaço seguro para o layout -->
        <div class="w-full overflow-y-auto max-h-[60vh] md:max-h-[65vh] px-1 md:px-2 flex flex-col gap-1.5">
          <form @submit.prevent="handleSubmit" novalidate class="w-full flex flex-col gap-4">
            
            <!-- Nome Completo -->
            <div class="relative w-full">
              <label for="cadastro-nome" class="sr-only">Nome completo</label>
              <span class="material-symbols-outlined absolute left-4 top-1/2 -translate-y-1/2 text-amma-green-dark text-[22px]">badge</span>
              <input 
                v-model="nomeCompleto"
                id="cadastro-nome"
                name="nome"
                type="text" 
                autocomplete="name"
                aria-label="Nome completo"
                :aria-invalid="isMissing(nomeCompleto)"
                placeholder="Nome completo *"
                :class="['w-full bg-white focus:bg-amma-green-lighter border-2 rounded-btn py-3.5 pl-12 pr-4 text-grey-text hover:bg-amma-green-lighter placeholder-amma-green/70 outline-none focus-visible:ring-2 focus-visible:ring-amma-green transition-all text-sm font-medium shadow-inner', isMissing(nomeCompleto) ? 'border-red-500' : 'border-amma-green-light focus:border-amma-green']"
              />
            </div>

            <!-- CPF e WhatsApp lado a lado -->
            <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div class="relative w-full">
                <label for="cadastro-cpf" class="sr-only">CPF</label>
                <span class="material-symbols-outlined absolute left-4 top-1/2 -translate-y-1/2 text-amma-green-dark text-[22px]">fingerprint</span>
                <input 
                  :value="cpf"
                  @input="formatCPF"
                  id="cadastro-cpf"
                  name="cpf"
                  type="text" 
                  maxlength="14"
                  inputmode="numeric"
                  aria-label="CPF"
                  :aria-invalid="attemptedSubmit && cpf.replace(/\D/g, '').length !== 11"
                  placeholder="CPF *"
                  :class="['w-full bg-white focus:bg-amma-green-lighter border-2 rounded-btn py-3.5 pl-12 pr-4 text-grey-text hover:bg-amma-green-lighter placeholder-amma-green/70 outline-none focus-visible:ring-2 focus-visible:ring-amma-green transition-all text-sm font-medium shadow-inner', attemptedSubmit && cpf.replace(/\D/g, '').length !== 11 ? 'border-red-500' : 'border-amma-green-light focus:border-amma-green']"
                />
              </div>

              <div class="relative w-full">
                <label for="cadastro-whatsapp" class="sr-only">WhatsApp</label>
                <span class="material-symbols-outlined absolute left-4 top-1/2 -translate-y-1/2 text-amma-green-dark text-[22px]">call</span>
                <input 
                  :value="whatsapp"
                  @input="formatWhatsApp"
                  id="cadastro-whatsapp"
                  type="tel"
                  maxlength="15"
                  inputmode="tel"
                  aria-label="WhatsApp"
                  :aria-invalid="isMissing(whatsapp)"
                  placeholder="WhatsApp *"
                  :class="['w-full bg-white focus:bg-amma-green-lighter border-2 rounded-btn py-3.5 pl-12 pr-4 text-grey-text hover:bg-amma-green-lighter placeholder-amma-green/70 outline-none focus-visible:ring-2 focus-visible:ring-amma-green transition-all text-sm font-medium shadow-inner', isMissing(whatsapp) ? 'border-red-500' : 'border-amma-green-light focus:border-amma-green']"
                />
              </div>
            </div>

            <!-- E-mail -->
            <div class="relative w-full">
              <label for="cadastro-email" class="sr-only">E-mail</label>
              <span class="material-symbols-outlined absolute left-4 top-1/2 -translate-y-1/2 text-amma-green-dark text-[22px]">mail</span>
              <input 
                v-model="email"
                id="cadastro-email"
                name="email"
                type="email" 
                autocomplete="email"
                aria-label="E-mail"
                :aria-invalid="attemptedSubmit && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email.trim())"
                placeholder="E-mail *"
                :class="['w-full bg-white focus:bg-amma-green-lighter border-2 rounded-btn py-3.5 pl-12 pr-4 text-grey-text hover:bg-amma-green-lighter placeholder-amma-green/70 outline-none focus-visible:ring-2 focus-visible:ring-amma-green transition-all text-sm font-medium shadow-inner', attemptedSubmit && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email.trim()) ? 'border-red-500' : 'border-amma-green-light focus:border-amma-green']"
              />
            </div>

            <div class="relative w-full">
              <label for="cadastro-endereco" class="sr-only">Endereço</label>
              <span class="material-symbols-outlined absolute left-4 top-1/2 -translate-y-1/2 text-amma-green-dark text-[22px]">home</span>
              <input
                v-model="endereco"
                id="cadastro-endereco"
                name="endereco"
                type="text"
                autocomplete="street-address"
                aria-label="Endereço"
                :aria-invalid="isMissing(endereco)"
                placeholder="Endereço (Rua, Número, Bairro) *"
                :class="['w-full bg-white focus:bg-amma-green-lighter border-2 rounded-btn py-3.5 pl-12 pr-4 text-grey-text hover:bg-amma-green-lighter placeholder-amma-green/70 outline-none focus-visible:ring-2 focus-visible:ring-amma-green transition-all text-sm font-medium shadow-inner', isMissing(endereco) ? 'border-red-500' : 'border-amma-green-light focus:border-amma-green']"
              />
            </div>

            <!-- Senhas lado a lado -->
            <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div class="relative w-full">
                <label for="cadastro-senha" class="sr-only">Senha</label>
                <span class="material-symbols-outlined absolute left-4 top-1/2 -translate-y-1/2 text-amma-green-dark text-[22px]">lock</span>
                <input 
                  v-model="senha"
                  id="cadastro-senha"
                  name="senha"
                  :type="showPassword ? 'text' : 'password'" 
                  autocomplete="new-password"
                  aria-label="Senha"
                  :aria-invalid="isMissing(senha)"
                  placeholder="Senha *"
                  :class="['w-full bg-white focus:bg-amma-green-lighter border-2 rounded-btn py-3.5 pl-12 pr-11 text-grey-text hover:bg-amma-green-lighter placeholder-amma-green/70 outline-none focus-visible:ring-2 focus-visible:ring-amma-green transition-all text-sm font-medium shadow-inner', isMissing(senha) ? 'border-red-500' : 'border-amma-green-light focus:border-amma-green']"
                />
                <button 
                  type="button"
                  @click="showPassword = !showPassword"
                  :aria-label="showPassword ? 'Ocultar senha' : 'Mostrar senha'"
                  class="absolute right-3.5 top-1/2 -translate-y-1/2 text-amma-green-dark hover:text-amma-green transition-colors cursor-pointer"
                >
                  <span class="material-symbols-outlined text-[20px]">{{ showPassword ? 'visibility_off' : 'visibility' }}</span>
                </button>
              </div>

              <div class="relative w-full">
                <label for="cadastro-confirmar-senha" class="sr-only">Confirmar senha</label>
                <span class="material-symbols-outlined absolute left-4 top-1/2 -translate-y-1/2 text-amma-green-dark text-[22px]">lock_reset</span>
                <input 
                  v-model="confirmarSenha"
                  id="cadastro-confirmar-senha"
                  name="confirmar-senha"
                  :type="showConfirmPassword ? 'text' : 'password'" 
                  autocomplete="new-password"
                  aria-label="Confirmar senha"
                  :aria-invalid="isMissing(confirmarSenha) || passwordsDoNotMatch"
                  placeholder="Confirmar senha *"
                  :class="['w-full bg-white focus:bg-amma-green-lighter border-2 rounded-btn py-3.5 pl-12 pr-11 text-grey-text hover:bg-amma-green-lighter placeholder-amma-green/70 outline-none focus-visible:ring-2 focus-visible:ring-amma-green transition-all text-sm font-medium shadow-inner', isMissing(confirmarSenha) || passwordsDoNotMatch ? 'border-red-500' : 'border-amma-green-light focus:border-amma-green']"
                />
                <button 
                  type="button"
                  @click="showConfirmPassword = !showConfirmPassword"
                  :aria-label="showConfirmPassword ? 'Ocultar confirmação de senha' : 'Mostrar confirmação de senha'"
                  class="absolute right-3.5 top-1/2 -translate-y-1/2 text-amma-green-dark hover:text-amma-green transition-colors cursor-pointer"
                >
                  <span class="material-symbols-outlined text-[20px]">{{ showConfirmPassword ? 'visibility_off' : 'visibility' }}</span>
                </button>
              </div>
            </div>
            <p v-if="passwordsDoNotMatch" class="-mt-2 text-xs font-medium text-red-700" role="status">
              As senhas informadas não coincidem.
            </p>

            <!-- Botão submeter -->
            <button 
              type="submit" 
              :disabled="isSubmitting"
              class="w-full mt-3 bg-amma-green hover:bg-amma-green-dark text-white font-bold py-3.5 rounded-btn flex items-center justify-center gap-2 shadow-md shadow-emerald-900/10 efeito-zoom cursor-pointer disabled:opacity-60 disabled:cursor-wait"
            >
              <span class="material-symbols-outlined text-[22px]">person_add</span>
              <span class="leading-none pt-[2px]">{{ isSubmitting ? 'Cadastrando...' : 'Concluir Cadastro' }}</span>
            </button>
          </form>

          <!-- Links de apoio / alternância -->
          <div class="mt-4 pb-2 flex items-center justify-center gap-1.5 text-xs text-grey-text">
            <span>Já possui uma conta?</span>
            <button 
              type="button" 
              @click="switchToLogin" 
              class="font-bold text-amma-green hover:underline cursor-pointer"
            >
              Entrar
            </button>
          </div>
        </div>

      </div>
    </div>
  </Transition>
</template>

<style scoped>
/* Estilo suave para barra de rolagem dos campos em resoluções menores */
.overflow-y-auto::-webkit-scrollbar {
  width: 5px;
}
.overflow-y-auto::-webkit-scrollbar-track {
  background: transparent;
}
.overflow-y-auto::-webkit-scrollbar-thumb {
  background-color: #d2e0cc;
  border-radius: 10px;
}
</style>
