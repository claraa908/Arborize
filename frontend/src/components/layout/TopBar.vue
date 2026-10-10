<script setup lang="ts">
import { ref } from 'vue'

const emit = defineEmits<{
    (e: 'open-login'): void
}>()

const isMobileMenuOpen = ref(false)
</script>

<template>
  <header class="relative w-full flex items-center justify-between px-8 py-6 border-b border-gray-200 z-40 bg-white rounded-tl-[15px] rounded-tr-[60px]">
    <div class="flex items-center gap-2">
      <div class="text-amma-grey-text flex items-center gap-2">
        <img src="@/assets/images/logoVerde.png" alt="Logo Arborize" class="h-16 w-auto" />
        <div class="hidden sm:flex flex-col">
          <span class="text-xl font-bold leading-none tracking-wide text-amma-green">ARBORIZE</span>
          <span class="text-[0.6rem] font-medium tracking-widest text-amma-green">Gestão Ambiental</span>
        </div>
      </div>
    </div>

    <nav class="hidden md:flex items-center gap-8 text-sm font-medium text-gray-500">
      <a href="#" class="hover:text-gray-900 transition-colors">Início</a>
      <a href="#" class="hover:text-gray-900 transition-colors">Eventos</a>
      <a href="#" class="bg-amma-green-light text-[#4D6E1F] px-5 py-2 rounded-tl-[10px] rounded-tr-[20px] rounded-bl-[20px] rounded-br-[10px] font-bold">Arborização</a>
      <a href="#" class="hover:text-gray-900 transition-colors">Sobre Nós</a>
    </nav>

    <div class="flex items-center gap-4 text-sm font-bold">
        <button @click="emit('open-login')" class="bg-amma-green hover:bg-amma-green-dark text-white px-6 py-2.5 rounded-btn efeito-zoom">Entrar</button>
        <button @click="isMobileMenuOpen = !isMobileMenuOpen" class="md:hidden flex items-center text-amma-green ml-2">
            <span class="material-symbols-outlined text-[32px]">{{ isMobileMenuOpen ? 'close' : 'menu' }}
            </span>
        </button>
    </div>

    <Transition
        enter-active-class="transition-all duration-300 ease-out"
        enter-from-class="opacity-0 -translate-y-4"
        enter-to-class="opacity-100 translate-y-0"
        leave-active-class="transition-all duration-200 ease-in"
        leave-from-class="opacity-100 translate-y-0"
        leave-to-class="opacity-0 -translate-y-4">
        <div v-if="isMobileMenuOpen" class="md:hidden absolute top-full left-0 w-full bg-white border-b border-gray-200 px-8 py-6 flex flex-col gap-4 shadow-lg">
        <a href="#" class="text-gray-600 font-medium py-2">Início</a>
        <a href="#" class="text-gray-600 font-medium py-2">Eventos</a>
        <a href="#" class="bg-amma-green-light text-[#4D6E1F] px-5 py-3 rounded-btn font-bold text-center mt-2">Arborização</a>
        <a href="#" class="text-gray-600 font-medium py-2">Sobre Nós</a>
        </div>
    </Transition>
  </header>
</template>