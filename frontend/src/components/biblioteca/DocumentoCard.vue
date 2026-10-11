<script setup lang="ts">
export interface Documento {
  id: string
  titulo: string
  descricao: string
  categoria: string
  tipo: string
  data: string
  tamanho?: string
  capaUrl?: string
}

defineProps<{
  documento: Documento
}>()

const emit = defineEmits<{
  (e: 'click', id: string): void
}>()
</script>

<template>
  <div 
    class="bg-white rounded-box border border-amma-green-light shadow-sm hover:shadow-xl transition-all duration-300 flex flex-col h-[380px] overflow-hidden group cursor-pointer efeito-zoom"
    @click="emit('click', documento.id)"
  >
    <div class="bg-amma-green-lighter h-44 w-full flex items-center justify-center relative overflow-hidden shrink-0 group-hover:bg-amma-green-light transition-colors">
      <img 
        v-if="documento.capaUrl" 
        :src="documento.capaUrl" 
        :alt="documento.titulo" 
        class="w-full h-full object-cover transition-all duration-500 ease-out group-hover:scale-110 group-hover:blur-[2px]"
      />
      <div 
        v-else 
        class="w-full h-full flex items-center justify-center transition-all duration-500 ease-out group-hover:scale-110 group-hover:blur-[2px]"
      >
        <span class="material-symbols-outlined text-6xl text-amma-green/40 select-none">
          menu_book
        </span>
      </div>

      <span class="absolute top-3 left-3 bg-white/95 backdrop-blur-sm text-amma-green-dark text-[10px] font-bold px-2.5 py-1 rounded-full shadow-sm border border-amma-green-light uppercase tracking-wide z-10">
        {{ documento.tipo }}
      </span>

      <div class="absolute inset-0 bg-black/25 opacity-0 group-hover:opacity-100 transition-all duration-300 flex items-center justify-center p-3 z-20">
        <span class="bg-white/95 text-amma-green-dark text-xs md:text-sm font-bold px-4 py-2 rounded-full shadow-lg border border-white/80 flex items-center gap-1.5 translate-y-2 group-hover:translate-y-0 transition-all duration-300 select-none">
          <span class="material-symbols-outlined text-[18px]">book_5</span>
          Abrir {{ documento.tipo }}
        </span>
      </div>
    </div>
    
    <div class="p-5 flex flex-col flex-1 overflow-hidden">
      <span class="text-[11px] font-bold text-amma-green tracking-wider uppercase mb-1.5 shrink-0">
        {{ documento.categoria }}
      </span>

      <h3 class="text-base font-bold text-amma-green-dark leading-snug mb-2 group-hover:text-amma-green transition-colors line-clamp-2 shrink-0">
        {{ documento.titulo }}
      </h3>

      <p class="text-xs md:text-sm text-grey-text line-clamp-3 mb-3 leading-relaxed">
        {{ documento.descricao }}
      </p>
      
      <div class="mt-auto pt-1 flex items-center gap-1.5 text-xs font-medium text-grey-text/70 shrink-0">
        <span class="material-symbols-outlined text-[15px]">calendar_today</span>
        <span>{{ documento.data }}</span>
      </div>
    </div>
  </div>
</template>