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
  <header class="flex items-center justify-between px-10 py-5 bg-white border-b border-emerald-900/10 shadow-[0_4px_20px_-15px_rgba(0,0,0,0.05)] z-10 relative">
    
    <div class="flex items-center h-full">
      <span class="text-amma-green font-bold text-2xl tracking-tight leading-none pt-1">{{ pageTitle }}</span>
    </div>

    <div class="flex items-center gap-6">
      
      <div class="flex items-center gap-4">
        <div class="flex flex-col text-right">
          <span class="text-sm font-extrabold text-emerald-950">Romênia Barros</span>
          <span class="text-xs font-medium text-emerald-900/60">Superintendente • AMMA</span>
        </div>
        
        <div 
          class="w-12 h-12 bg-[#4D6E1F] text-white flex items-center justify-center font-bold text-[15px] shadow-md border-2 border-white"
          style="border-radius: 24px 6px 24px 24px;"
        >
          RB
        </div>
      </div>

      <div class="w-px h-8 bg-emerald-900/15"></div>

      <div class="flex items-center gap-2">
        <button 
          @click="navigateToProfile"
          class="w-10 h-10 flex items-center justify-center text-emerald-800/60 hover:text-amma-green hover:bg-amma-green-lighter/50 rounded-full transition-all efeito-zoom" 
          title="Meu Perfil"
        >
          <span class="material-symbols-outlined text-[24px]">person</span>
        </button>
        
        <button 
          class="w-10 h-10 flex items-center justify-center text-emerald-800/60 hover:text-red-500 hover:bg-red-50 rounded-full transition-all efeito-zoom" 
          title="Sair"
        >
          <span class="material-symbols-outlined text-[24px]">logout</span>
        </button>
      </div>

    </div>
  </header>
</template>