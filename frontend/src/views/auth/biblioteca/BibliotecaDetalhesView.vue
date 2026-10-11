<script setup lang="ts">
import { ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import AppBreadcrumb from '@/components/ui/AppBreadcrumb.vue'

const router = useRouter()
const route = useRoute()

const documento = ref({
  id: route.params.id,
  titulo: 'Guia de Plantio Urbano 2026',
  descricao: 'Manual completo sobre técnicas de plantio adequadas para o ambiente urbano de Quixadá, com foco em espécies nativas da Caatinga.',
  tipoMaterial: 'Manual',
  categoria: 'Arborização Urbana',
  status: 'PUBLICADO', 
  dataCriacao: '11/10/2026 às 09:30',
  cadastradoPor: 'Romênia Barros',
  nomeArquivo: 'guia_plantio_urbano_2026.pdf',
  tamanhoArquivo: '2.4 MB'
})

const breadcrumbItems = [
  { label: 'Biblioteca Digital', icon: 'local_library', routeName: 'biblioteca-lista' },
  { label: documento.value.titulo }
]

const goBack = () => router.push({ name: 'biblioteca-lista' })

const editDocument = () => {
  console.log('Navegar para edição')
}

const archiveDocument = () => {
  if (confirm('Tem a certeza que deseja arquivar este documento?')) {
    goBack()
  }
}
</script>

<template>
  <div class="flex flex-col">
    
    <AppBreadcrumb :items="breadcrumbItems" />

    <div class="flex flex-col sm:flex-row justify-between items-start sm:items-center px-6 md:px-10 pb-6 pt-6">
      
      <div class="flex items-center gap-4">
        <div class="w-12 h-12 shrink-0 flex items-center justify-center bg-amma-green-lighter/50 text-amma-green rounded-full shadow-sm">
          <span class="material-symbols-outlined text-[24px]">description</span>
        </div>
        <div>
          <h2 class="text-2xl font-bold text-amma-green-dark leading-tight line-clamp-1 max-w-[500px]" :title="documento.titulo">
            {{ documento.titulo }}
          </h2>
          <div class="flex items-center gap-3 mt-1">
            <span 
              :class="[ 
                'text-[10px] uppercase tracking-wider font-bold px-2.5 py-0.5 rounded-full',
                documento.status === 'PUBLICADO' ? 'bg-amma-green/20 text-amma-green-dark' : 'bg-orange-100 text-orange-700'
              ]"
            >
              {{ documento.status }}
            </span>
            <span class="text-xs font-medium text-grey-text/70 flex items-center gap-1">
                <span class="material-symbols-outlined !text-[14px] leading-none">calendar_today</span>
                    Adicionado em {{ documento.dataCriacao }}
                </span>
          </div>
        </div>
      </div>

      <div class="flex items-center gap-3 w-full sm:w-auto mt-4 sm:mt-0">
        <button 
          @click="archiveDocument"
          class="flex-1 sm:flex-none bg-white hover:bg-gray-100 text-gray-600 font-bold py-2 px-4 rounded-btn border border-gray-300 hover:border-gray-400 transition-colors shadow-sm text-sm flex items-center justify-center gap-2 efeito-zoom"
        >
          <span class="material-symbols-outlined text-[18px]">archive</span>
          Arquivar
        </button>
        <button 
          @click="editDocument"
          class="flex-1 sm:flex-none bg-amma-green hover:bg-amma-green-dark text-white font-bold py-2.5 px-6 rounded-btn shadow-md text-sm border-none outline-none efeito-zoom flex items-center justify-center gap-2 efeito-zoom"
        >
          <span class="material-symbols-outlined text-[18px]">edit</span>
          Editar
        </button>
      </div>
    </div>

    <div class="grid grid-cols-1 lg:grid-cols-3 gap-6 px-6 md:px-10 pb-6 pt-6 items-stretch">
      
      <div class="lg:col-span-1 flex flex-col h-full">
        <div class="bg-white rounded-box shadow-sm border border-amma-green-light p-8 flex flex-col items-center justify-center text-center h-full">
          <div class="w-20 h-20 bg-amma-green-lighter rounded-full flex items-center justify-center mb-4">
            <span class="material-symbols-outlined text-4xl text-amma-green">picture_as_pdf</span>
          </div>
          
          <h4 class="text-sm font-bold text-amma-green-dark mb-1 w-full truncate px-2" :title="documento.nomeArquivo">
            {{ documento.nomeArquivo }}
          </h4>
          <span class="text-xs font-semibold text-grey-text/60 mb-6">{{ documento.tamanhoArquivo }}</span>
          
          <div class="w-full flex flex-col gap-3 mt-auto">
            <button class="w-full bg-amma-green-lighter hover:bg-amma-green hover:text-white text-amma-green-dark font-bold py-2.5 px-4 rounded-btn transition-colors text-sm flex items-center justify-center gap-2 border border-amma-green/20 efeito-zoom">
              <span class="material-symbols-outlined text-[20px]">download</span>
              Baixar PDF
            </button>
            
            <button class="w-full bg-white hover:bg-amma-bg text-grey-text font-bold py-2.5 px-4 rounded-btn border border-amma-green-light transition-colors text-sm flex items-center justify-center gap-2 efeito-zoom">
              <span class="material-symbols-outlined text-[20px]">visibility</span>
              Pré-visualizar
            </button>
          </div>
        </div>
      </div>

      <div class="lg:col-span-2 flex flex-col h-full">
        <div class="bg-white rounded-box shadow-sm border border-amma-green-light p-8 h-full flex flex-col justify-center">
          <h3 class="text-lg font-bold text-amma-green-dark mb-6 border-b border-amma-green-light pb-4 flex items-center gap-2">
            <span class="material-symbols-outlined text-amma-green">info</span>
            Metadados do Registo
          </h3>
          
          <div class="grid grid-cols-1 sm:grid-cols-2 gap-y-6 gap-x-8">
            <div class="flex flex-col gap-1">
              <span class="text-xs font-bold text-grey-text/60 uppercase tracking-wide">Tipo de Material</span>
              <span class="text-sm font-semibold text-grey-text bg-black/5 w-fit px-3 py-1.5 rounded-btn mt-1">{{ documento.tipoMaterial }}</span>
            </div>
            
            <div class="flex flex-col gap-1">
              <span class="text-xs font-bold text-grey-text/60 uppercase tracking-wide">Categoria</span>
              <span class="text-sm font-semibold text-grey-text">{{ documento.categoria }}</span>
            </div>
            
            <div class="flex flex-col gap-1">
              <span class="text-xs font-bold text-grey-text/60 uppercase tracking-wide">Cadastrado Por</span>
              <span class="text-sm font-semibold text-grey-text flex items-center gap-2 mt-1">
                <span class="w-6 h-6 rounded-full bg-amma-green-light text-amma-green-dark flex items-center justify-center text-[10px]">RB</span>
                {{ documento.cadastradoPor }}
              </span>
            </div>
            
            <div class="flex flex-col gap-1 sm:col-span-2 mt-2">
              <span class="text-xs font-bold text-grey-text/60 uppercase tracking-wide">Descrição do Documento</span>
              <p class="text-sm font-medium text-grey-text mt-1 leading-relaxed border-l-2 border-amma-green pl-3">
                {{ documento.descricao || 'Nenhuma descrição fornecida.' }}
              </p>
            </div>
          </div>
        </div>
      </div>

    </div>
  </div>
</template>