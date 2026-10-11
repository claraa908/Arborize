<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()
const pageTitle = computed(() => route.meta.title as string || 'Arborize')

const navigateToProfile = () => {
  router.push({ name: 'perfil' })
}
</script>

<template>
  <header class="flex items-center justify-end md:justify-between px-6 md:px-10 py-5 bg-white border-b border-amma-green-light shadow-[0_4px_20px_-15px_rgba(0,0,0,0.05)] z-10 relative">
    
    <div class="hidden md:flex items-center h-full">
      <span class="text-amma-green-dark font-bold text-2xl tracking-tight leading-none pt-1">{{ pageTitle }}</span>
    </div>

    <div class="flex items-center gap-4 md:gap-6">
      
      <div class="hidden sm:flex items-center gap-4">
        <div class="flex flex-col text-right">
          <span class="text-sm font-extrabold text-amma-green-dark">Romênia Barros</span>
          <span class="text-xs font-medium text-grey-text">Superintendente • AMMA</span>
        </div>
        
        <div 
          class="w-12 h-12 bg-amma-green-light text-dark flex items-center justify-center font-bold text-[15px] shadow-md border-2 border-white"
          style="border-radius: 24px 6px 24px 24px;"
        >
          RB
        </div>
      </div>

      <div class="hidden sm:block w-px h-8 bg-amma-green-light"></div>

      <div class="flex items-center gap-2">
        <button 
          @click="navigateToProfile"
          :class="[
            'w-10 h-10 flex items-center justify-center transition-all efeito-zoom',
            route.name === 'perfil'
              ? 'bg-amma-green-dark text-white shadow-inner rounded-bl-[10px] rounded-tr-[10px] rounded-tl-[20px] rounded-br-[20px]'
              : 'text-grey-text hover:text-white hover:bg-amma-green rounded-bl-[10px] rounded-tr-[10px] rounded-tl-[20px] rounded-br-[20px]'
          ]"
          title="Meu Perfil"
        >
          <span class="material-symbols-outlined text-[24px]">person</span>
        </button>
        
        <button 
          class="w-10 h-10 flex items-center justify-center text-grey-text hover:text-red-500 hover:bg-red-50 rounded-bl-[10px] rounded-tr-[10px] rounded-tl-[20px] rounded-br-[20px] transition-all efeito-zoom" 
          title="Sair"
        >
          <span class="material-symbols-outlined text-[24px]">logout</span>
        </button>
      </div>

    </div>
  </header>
</template>