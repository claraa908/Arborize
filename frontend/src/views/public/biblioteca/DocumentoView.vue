<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { materialService } from '@/services/materialService'
import { useFeedbackStore } from '@/stores/feedbackStore'

const router = useRouter()
const route = useRoute()
const feedback = useFeedbackStore()

const documento = ref<any>({
  id: '',
  titulo: 'Carregando...',
  descricao: '',
  categoria: '',
  tipo: '',
  dataPublicacao: '',
  tamanhoArquivo: '-',
  nomeArquivo: 'documento.pdf',
  capaUrl: ''
})

// Função para buscar os detalhes do material público pelo ID usando o materialService
const carregarDetalhesPublicos = async () => {
  try {
    const id = route.params.id as string
    const data = await materialService.getMaterialById(id)

    const firstDoc = data.documents?.[0]
    const hasCover = (data.documents?.length ?? 0) > 1

    documento.value = {
      id: data.id,
      titulo: data.title,
      descricao: data.description,
      categoria: data.category,
      tipo: data.materialType || data.type || '',
      dataPublicacao: data.uploadDate ? new Date(data.uploadDate).toLocaleDateString('pt-BR') : '-',
      tamanhoArquivo: firstDoc && firstDoc.sizeBytes ? `${(firstDoc.sizeBytes / 1024 / 1024).toFixed(1)} MB` : '2.0 MB',
      nomeArquivo: firstDoc ? (firstDoc.originalFilename || firstDoc.fileName || 'documento.pdf') : 'documento.pdf',
      capaUrl: hasCover ? materialService.getCoverUrl(data.id) : ''
    }
  } catch (error) {
    feedback.showToast('Erro ao carregar detalhes do documento.', 'error')
  }
}

onMounted(() => {
  carregarDetalhesPublicos()
})

const goToCatalogo = () => {
  router.push({ name: 'biblioteca-catalogo' })
}

// Função para abrir ou baixar o PDF usando a URL centralizada do service
const downloadPdf = () => {
  const id = route.params.id as string
  window.open(materialService.getDownloadUrl(id), '_blank')
}
</script>

<template>
  <div class="max-w-7xl mx-auto flex flex-col gap-4 pt-1 pb-6 w-full">

    <nav class="flex items-center gap-2 text-xs md:text-sm text-grey-text">
      <button
        @click="goToCatalogo"
        class="hover:text-amma-green flex items-center gap-1 font-semibold transition-colors cursor-pointer"
      >
        <span class="material-symbols-outlined text-[18px]">local_library</span>
        Biblioteca Digital
      </button>
      <span class="text-gray-300 select-none">/</span>
      <span class="text-amma-green-dark font-bold truncate max-w-xs md:max-w-md">
        {{ documento.titulo }}
      </span>
    </nav>

    <div class="bg-white rounded-box shadow-sm border border-amma-green-light overflow-hidden flex flex-col lg:flex-row min-h-[580px]">

      <div class="w-full lg:w-3/5 bg-amma-bg border-b lg:border-b-0 lg:border-r border-amma-green-light min-h-[420px] lg:min-h-[580px] flex flex-col items-center justify-center p-6 md:p-10 relative">
        <span class="absolute top-4 left-4 bg-white/80 text-amma-green-dark text-[11px] uppercase tracking-wider font-bold px-3 py-1 rounded-full border border-amma-green-light backdrop-blur-sm">
          Pré-visualização
        </span>

        <div class="w-20 h-20 rounded-full bg-white flex items-center justify-center shadow-sm border border-amma-green-light mb-4">
          <span class="material-symbols-outlined text-4xl text-amma-green">picture_as_pdf</span>
        </div>

        <h3 class="text-lg md:text-xl font-bold text-amma-green-dark mb-2 text-center">
          Visualizador de Documento
        </h3>
        <p class="text-xs md:text-sm text-grey-text text-center max-w-sm mb-6 leading-relaxed">
          O conteúdo completo deste documento está disponível para leitura interativa direta ou abertura em leitor dedicado.
        </p>

        <button
          @click="downloadPdf"
          class="bg-white hover:bg-amma-green-lighter text-amma-green-dark font-bold py-2.5 px-6 rounded-btn border-2 border-amma-green-light transition-colors text-xs md:text-sm flex items-center justify-center gap-2 shadow-sm efeito-zoom cursor-pointer"
        >
          <span class="material-symbols-outlined text-[18px]">open_in_new</span>
          Abrir em nova aba
        </button>
      </div>

      <div class="w-full lg:w-2/5 p-6 md:p-8 flex flex-col justify-between gap-6 bg-white">

        <div class="flex flex-col gap-4">
          <!-- Exibe a imagem apenas se a capaUrl estiver preenchida -->
          <div v-if="documento.capaUrl" class="w-full h-48 md:h-52 rounded-btn overflow-hidden border border-amma-green-light shadow-sm shrink-0">
            <img
              :src="documento.capaUrl"
              :alt="documento.titulo"
              class="w-full h-full object-cover"
            />
          </div>

          <div class="flex gap-2 flex-wrap items-center">
            <span class="text-[10px] font-bold text-white bg-amma-green uppercase tracking-wide px-3 py-1 rounded-full shadow-sm">
              {{ documento.tipo }}
            </span>
            <span class="text-[10px] font-bold text-amma-green-dark bg-amma-green-light/40 uppercase tracking-wide px-3 py-1 rounded-full border border-amma-green/20">
              {{ documento.categoria }}
            </span>
          </div>

          <h1 class="text-xl md:text-2xl font-black text-amma-green-dark leading-tight tracking-tight">
            {{ documento.titulo }}
          </h1>

          <p class="text-xs md:text-sm text-grey-text leading-relaxed">
            {{ documento.descricao }}
          </p>
        </div>

        <div class="flex flex-col gap-3 pt-4 border-t border-amma-green-light/60 mt-auto">
          <div class="bg-amma-bg p-4 rounded-btn border border-amma-green-light flex flex-col gap-2.5 text-xs">
            <div class="flex items-center justify-between">
              <span class="text-grey-text/70 font-medium">Publicado em:</span>
              <span class="text-amma-green-dark font-bold">{{ documento.dataPublicacao }}</span>
            </div>
            <div class="flex items-center justify-between">
              <span class="text-grey-text/70 font-medium">Tamanho:</span>
              <span class="text-amma-green-dark font-bold">{{ documento.tamanhoArquivo }}</span>
            </div>
          </div>

          <button
            @click="downloadPdf"
            class="w-full bg-amma-green hover:bg-amma-green-dark text-white font-bold py-3 px-6 rounded-btn shadow-md text-sm efeito-zoom flex items-center justify-center gap-2 transition-all cursor-pointer"
          >
            <span class="material-symbols-outlined text-[20px]">download</span>
            Baixar Documento
          </button>
        </div>

      </div>

    </div>
  </div>
</template>
