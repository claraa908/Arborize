<script setup lang="ts">
import { ref } from 'vue'

const props = defineProps<{
  isOpen: boolean
}>()

const emit = defineEmits<{
  (e: 'close'): void
}>()

const showPassword = ref(false)
const email = ref('')
const password = ref('')

const handleSubmit = () => {
  // preencher depois com a lógica de autenticação
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
      <!-- card do modal -->
      <div class="relative w-full max-w-md bg-[#ffffff] backdrop-blur-xl border border-white/40 rounded-[2.5rem] p-8 md:p-10 shadow-[0_20px_50px_rgba(0,0,0,0.15)] flex flex-col items-center">
        
        <!-- O botão de fechar modal-->
        <button @click="emit('close')" class="absolute -top-3 -right-3 w-14 h-14 bg-amma-green hover:bg-amma-green-dark transition-all duration-300 hover:scale-115 hover:shadow-lg active:scale-95 cursor-pointer flex items-center justify-center text-white focus:outline-none" style="border-radius: 40px 10px 40px 40px;">
            <span class="material-symbols-outlined -mt-1 -mr-1">close</span>
        </button>

        <!-- Badge de Área Restrita -->
        <div class="inline-flex items-center gap-1.5 bg-[#e4efdc] text-emerald-800 text-xs font-semibold px-4 py-1.5 rounded-full mb-4">
            <span class="material-symbols-outlined">verified_user</span>
            <span class="leading-none pt-[2px]">Área restrita - AMMA de Quixadá</span>
        </div>

        <h2 class="text-3xl font-extrabold text-amma-green mb-8 tracking-tight">Entrar</h2>

            <form @submit.prevent="handleSubmit" class="w-full flex flex-col gap-4">
            <!-- Usuário -->
            <div class="relative">
                <span class="material-symbols-outlined absolute left-4 top-1/2 -translate-y-1/2 text-amma-green-dark text-[22px]">person</span>
                <input 
                v-model="email"
                type="text" 
                placeholder="Usuário, e-mail ou CPF"
                class="w-full bg-white/70 focus:bg-amma-green-lighter border-2 border-amma-green-light focus:border-amma-green rounded-btn py-4 pl-12 pr-4 text-amma-green-dark hover:bg-amma-green-lighter placeholder-amma-green outline-none transition-all text-base font-medium shadow-inner"/>
            </div>

            <!-- Senha -->
            <div class="relative">
                <span class="material-symbols-outlined absolute left-4 top-1/2 -translate-y-1/2 text-amma-green-dark text-[22px]">lock</span>
                <input 
                v-model="password"
                :type="showPassword ? 'text' : 'password'" 
                placeholder="Senha"
                class="w-full bg-white/70 focus:bg-amma-green-lighter border-2 border-amma-green-light focus:border-amma-green rounded-btn py-4 pl-12 pr-12 text-amma-green-dark hover:bg-amma-green-lighter placeholder-amma-green outline-none transition-all text-base font-medium shadow-inner"
                />
                <button 
                type="button"
                @click="showPassword = !showPassword"
                class="absolute right-4 top-1/2 -translate-y-1/2 text-amma-green-dark hover:text-amma-green transition-colors cursor-pointer"
                >
                <span class="material-symbols-outlined text-[22px]">{{ showPassword ? 'visibility_off' : 'visibility' }}</span>
                </button>
            </div>

                <!-- Botão  de entrar -->
                <button type="submit" class="w-full mt-2 bg-amma-green hover:bg-amma-green-dark text-white font-bold py-4 rounded-btn flex items-center justify-center gap-2 shadow-lg shadow-emerald-900/10 efeito-zoom">
                    <span class="material-symbols-outlined text-[22px]">login</span>
                    <span class="leading-none pt-[2px]">Entrar</span>
                </button>
            </form>
      </div>
    </div>
  </Transition>
</template>