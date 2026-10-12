<script setup lang="ts">
import { ref } from 'vue'

defineProps<{
  isOpen: boolean
}>()

const emit = defineEmits<{
  (e: 'close'): void
  (e: 'open-login'): void
}>()

const nomeCompleto = ref('')
const cpf = ref('')
const email = ref('')
const telefone = ref('')
const senha = ref('')
const confirmarSenha = ref('')

const showPassword = ref(false)
const showConfirmPassword = ref(false)
const errorMessage = ref('')

const formatCPF = (event: Event) => {
  const input = event.target as HTMLInputElement
  let v = input.value.replace(/\D/g, '')
  if (v.length > 11) v = v.slice(0, 11)
  v = v.replace(/(\d{3})(\d)/, '$1.$2')
  v = v.replace(/(\d{3})(\d)/, '$1.$2')
  v = v.replace(/(\d{3})(\d{1,2})$/, '$1-$2')
  cpf.value = v
}

const formatTelefone = (event: Event) => {
  const input = event.target as HTMLInputElement
  let v = input.value.replace(/\D/g, '')
  if (v.length > 11) v = v.slice(0, 11)
  if (v.length > 10) {
    v = v.replace(/(\d{2})(\d{5})(\d{4})/, '($1) $2-$3')
  } else if (v.length > 5) {
    v = v.replace(/(\d{2})(\d{4})(\d{0,4})/, '($1) $2-$3')
  } else if (v.length > 2) {
    v = v.replace(/(\d{2})(\d{0,5})/, '($1) $2')
  }
  telefone.value = v
}

const handleSubmit = () => {
  errorMessage.value = ''

  if (senha.value !== confirmarSenha.value) {
    errorMessage.value = 'As senhas informadas não coincidem.'
    return
  }

  if (senha.value.length < 6) {
    errorMessage.value = 'A senha deve possuir pelo menos 6 caracteres.'
    return
  }

  console.log('Cadastro de cidadão submetido:', {
    nomeCompleto: nomeCompleto.value,
    cpf: cpf.value,
    email: email.value,
    telefone: telefone.value,
    senha: senha.value
  })

  emit('close')
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
        <div v-if="errorMessage" class="w-full bg-red-50 border border-red-200 text-red-700 text-xs font-medium rounded-btn p-3.5 mb-4 flex items-center gap-2">
          <span class="material-symbols-outlined text-[18px]">error</span>
          <span>{{ errorMessage }}</span>
        </div>

        <!-- Conteúdo Interno com Rolagem apenas vertical e espaço seguro para o layout -->
        <div class="w-full overflow-y-auto max-h-[60vh] md:max-h-[65vh] px-1 md:px-2 flex flex-col gap-1.5">
          <form @submit.prevent="handleSubmit" class="w-full flex flex-col gap-4">
            
            <!-- Nome Completo -->
            <div class="relative w-full">
              <span class="material-symbols-outlined absolute left-4 top-1/2 -translate-y-1/2 text-amma-green-dark text-[22px]">badge</span>
              <input 
                v-model="nomeCompleto"
                type="text" 
                required
                placeholder="Nome completo *"
                class="w-full bg-white focus:bg-amma-green-lighter border-2 border-amma-green-light focus:border-amma-green rounded-btn py-3.5 pl-12 pr-4 text-grey-text hover:bg-amma-green-lighter placeholder-amma-green/70 outline-none transition-all text-sm font-medium shadow-inner"
              />
            </div>

            <!-- CPF e WhatsApp lado a lado -->
            <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div class="relative w-full">
                <span class="material-symbols-outlined absolute left-4 top-1/2 -translate-y-1/2 text-amma-green-dark text-[22px]">fingerprint</span>
                <input 
                  :value="cpf"
                  @input="formatCPF"
                  type="text" 
                  required
                  maxlength="14"
                  placeholder="CPF *"
                  class="w-full bg-white focus:bg-amma-green-lighter border-2 border-amma-green-light focus:border-amma-green rounded-btn py-3.5 pl-12 pr-4 text-grey-text hover:bg-amma-green-lighter placeholder-amma-green/70 outline-none transition-all text-sm font-medium shadow-inner"
                />
              </div>

              <div class="relative w-full">
                <span class="material-symbols-outlined absolute left-4 top-1/2 -translate-y-1/2 text-amma-green-dark text-[22px]">call</span>
                <input 
                  :value="telefone"
                  @input="formatTelefone"
                  type="text" 
                  required
                  maxlength="15"
                  placeholder="WhatsApp *"
                  class="w-full bg-white focus:bg-amma-green-lighter border-2 border-amma-green-light focus:border-amma-green rounded-btn py-3.5 pl-12 pr-4 text-grey-text hover:bg-amma-green-lighter placeholder-amma-green/70 outline-none transition-all text-sm font-medium shadow-inner"
                />
              </div>
            </div>

            <!-- E-mail -->
            <div class="relative w-full">
              <span class="material-symbols-outlined absolute left-4 top-1/2 -translate-y-1/2 text-amma-green-dark text-[22px]">mail</span>
              <input 
                v-model="email"
                type="email" 
                required
                placeholder="E-mail *"
                class="w-full bg-white focus:bg-amma-green-lighter border-2 border-amma-green-light focus:border-amma-green rounded-btn py-3.5 pl-12 pr-4 text-grey-text hover:bg-amma-green-lighter placeholder-amma-green/70 outline-none transition-all text-sm font-medium shadow-inner"
              />
            </div>

            <!-- Senhas lado a lado -->
            <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div class="relative w-full">
                <span class="material-symbols-outlined absolute left-4 top-1/2 -translate-y-1/2 text-amma-green-dark text-[22px]">lock</span>
                <input 
                  v-model="senha"
                  :type="showPassword ? 'text' : 'password'" 
                  required
                  placeholder="Senha *"
                  class="w-full bg-white focus:bg-amma-green-lighter border-2 border-amma-green-light focus:border-amma-green rounded-btn py-3.5 pl-12 pr-11 text-grey-text hover:bg-amma-green-lighter placeholder-amma-green/70 outline-none transition-all text-sm font-medium shadow-inner"
                />
                <button 
                  type="button"
                  @click="showPassword = !showPassword"
                  class="absolute right-3.5 top-1/2 -translate-y-1/2 text-amma-green-dark hover:text-amma-green transition-colors cursor-pointer"
                >
                  <span class="material-symbols-outlined text-[20px]">{{ showPassword ? 'visibility_off' : 'visibility' }}</span>
                </button>
              </div>

              <div class="relative w-full">
                <span class="material-symbols-outlined absolute left-4 top-1/2 -translate-y-1/2 text-amma-green-dark text-[22px]">lock_reset</span>
                <input 
                  v-model="confirmarSenha"
                  :type="showConfirmPassword ? 'text' : 'password'" 
                  required
                  placeholder="Confirmar senha *"
                  class="w-full bg-white focus:bg-amma-green-lighter border-2 border-amma-green-light focus:border-amma-green rounded-btn py-3.5 pl-12 pr-11 text-grey-text hover:bg-amma-green-lighter placeholder-amma-green/70 outline-none transition-all text-sm font-medium shadow-inner"
                />
                <button 
                  type="button"
                  @click="showConfirmPassword = !showConfirmPassword"
                  class="absolute right-3.5 top-1/2 -translate-y-1/2 text-amma-green-dark hover:text-amma-green transition-colors cursor-pointer"
                >
                  <span class="material-symbols-outlined text-[20px]">{{ showConfirmPassword ? 'visibility_off' : 'visibility' }}</span>
                </button>
              </div>
            </div>

            <!-- Botão submeter -->
            <button 
              type="submit" 
              class="w-full mt-3 bg-amma-green hover:bg-amma-green-dark text-white font-bold py-3.5 rounded-btn flex items-center justify-center gap-2 shadow-md shadow-emerald-900/10 efeito-zoom cursor-pointer"
            >
              <span class="material-symbols-outlined text-[22px]">person_add</span>
              <span class="leading-none pt-[2px]">Concluir Cadastro</span>
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