<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import AppBreadcrumb from '@/components/ui/AppBreadcrumb.vue'
import { materialService } from '@/services/materialService'
import { useLibraryStore } from '@/stores/libraryStore'
import { useFeedbackStore } from '@/stores/feedbackStore'

const router = useRouter()
const route = useRoute()
const libraryStore = useLibraryStore()
const feedback = useFeedbackStore()

const isLoading = ref(true)

const documento = ref({
  id: '',
  titulo: '',
  descricao: '',
  tipoMaterial: '',
  categoria: '',
  status: '',
  dataCriacao: '',
  cadastradoPor: 'Romênia Barros',
  nomeArquivo: 'documento.pdf',
  tamanhoArquivo: '-',
  capaUrl: ''
})

const breadcrumbItems = ref([
  { label: 'Biblioteca Digital', icon: 'local_library', routeName: 'biblioteca-lista' },
  { label: 'Carregando...' }
])

const carregarDetalhes = async () => {
  isLoading.value = true
  try {
    const id = route.params.id as string
    const data = await materialService.getMaterialById(id)

    const firstDoc = data.documents?.[0]
    const hasCover = (data.documents?.length ?? 0) > 1

    documento.value = {
      id: String(data.id),
      titulo: data.title || 'Sem Título',
      descricao: data.description,
      tipoMaterial: data.materialType || '',
      categoria: data.category,
      status: data.status,
      dataCriacao: data.uploadDate ? new Date(data.uploadDate).toLocaleDateString('pt-BR') : '-',
      cadastradoPor: 'Romênia Barros',
      nomeArquivo: firstDoc ? (firstDoc.originalFilename || firstDoc.fileName) : 'documento.pdf',
      tamanhoArquivo: firstDoc ? `${(firstDoc.sizeBytes / 1024 / 1024).toFixed(1)} MB` : '2.0 MB',
      capaUrl: hasCover ? materialService.getCoverUrl(data.id) : ''
    }

    breadcrumbItems.value = [
      { label: 'Biblioteca Digital', icon: 'local_library', routeName: 'biblioteca-lista' },
      { label: documento.value.titulo }
    ]
  } catch (error) {
    feedback.showToast('Erro ao carregar os detalhes do documento.', 'error')
  } finally {
    isLoading.value = false
  }
}

onMounted(() => {
  carregarDetalhes()
})

const goBack = () => router.push({ name: 'biblioteca-lista' })

const editDocument = () => {
  router.push({ name: 'biblioteca-editar', params: { id: documento.value.id } })
}

const deleteDocument = async () => {
  const confirmed = await feedback.showConfirm({
    title: 'Excluir Documento',
    message: `Tem certeza de que deseja excluir permanentemente "${documento.value.titulo}"? Esta ação removerá o arquivo do servidor e não poderá ser desfeita.`,
    confirmText: 'Excluir',
    cancelText: 'Cancelar',
    variant: 'danger',
  })

  if (!confirmed) return

  try {
    await materialService.deleteMaterial(documento.value.id)
    feedback.showToast('Documento excluído com sucesso!', 'success')
    goBack()
  } catch (error) {
    feedback.showToast('Erro ao excluir o documento.', 'error')
  }
}

const togglePublishStatus = async () => {
  const isPublished = documento.value.status === 'PUBLICADO'
  const newStatus = isPublished ? 'RASCUNHO' : 'PUBLICADO'

  const confirmed = await feedback.showConfirm({
    title: isPublished ? 'Arquivar Documento' : 'Publicar Documento',
    message: isPublished
      ? `Deseja arquivar "${documento.value.titulo}"? Ele deixará de ser visível no catálogo público e passará para Rascunho.`
      : `Deseja publicar "${documento.value.titulo}" no catálogo público agora?`,
    confirmText: isPublished ? 'Arquivar' : 'Publicar',
    cancelText: 'Cancelar',
    variant: isPublished ? 'warning' : 'primary',
  })

  if (!confirmed) return

  try {
    await materialService.updateStatus(documento.value.id, newStatus)
    documento.value.status = newStatus
    feedback.showToast(`Documento ${newStatus === 'PUBLICADO' ? 'publicado' : 'arquivado'} com sucesso!`, 'success')
  } catch (error) {
    feedback.showToast('Erro ao alterar o status do documento.', 'error')
  }
}

const baixarPdf = () => {
  const id = route.params.id as string
  window.open(materialService.getDownloadUrl(id), '_blank')
}
</script>

<template>
  <div class="flex flex-col">

    <div v-if="isLoading" class="p-12 text-center text-grey-text font-semibold flex items-center justify-center gap-3">
      <span class="material-symbols-outlined animate-spin text-amma-green text-2xl">progress_activity</span>
      <span>Carregando informações do documento...</span>
    </div>

    <template v-else>
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
                  documento.status === 'PUBLICADO'
                    ? 'bg-amma-green/20 text-amma-green-dark'
                    : 'bg-feedback-warning-bg text-feedback-warning border border-feedback-warning-border'
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
            type="button"
            @click="deleteDocument"
            class="flex-1 sm:flex-none bg-feedback-danger-bg hover:opacity-90 text-feedback-danger font-bold py-2 px-4 rounded-btn border border-feedback-danger-border transition-colors shadow-sm text-sm flex items-center justify-center gap-2 efeito-zoom cursor-pointer"
          >
            <span class="material-symbols-outlined text-[18px]">delete</span>
            Excluir
          </button>
          <button
  type="button"
  @click="togglePublishStatus"
  class="flex-1 sm:flex-none bg-white hover:bg-amma-bg text-grey-text font-bold py-2 px-4 rounded-btn border-2 border-amma-green-light transition-colors shadow-sm text-sm flex items-center justify-center gap-2 efeito-zoom cursor-pointer"
>
  <span class="material-symbols-outlined text-[18px]">
    {{ documento.status === 'PUBLICADO' ? 'archive' : 'publish' }}
  </span>
  {{ documento.status === 'PUBLICADO' ? 'Arquivar' : 'Publicar' }}
</button>
          <button
            type="button"
            @click="editDocument"
            class="flex-1 sm:flex-none bg-amma-green hover:bg-amma-green-dark text-white font-bold py-2.5 px-6 rounded-btn shadow-md text-sm border-none outline-none efeito-zoom flex items-center justify-center gap-2 cursor-pointer"
          >
            <span class="material-symbols-outlined text-[18px]">edit</span>
            Editar
          </button>
        </div>
      </div>

      <div class="grid grid-cols-1 lg:grid-cols-3 gap-6 px-6 md:px-10 pb-6 pt-6 items-stretch">

        <div class="lg:col-span-1 flex flex-col h-full">
          <div class="bg-white rounded-box shadow-sm border border-amma-green-light p-6 flex flex-col items-center justify-between text-center h-full">

            <div v-if="documento.capaUrl" class="w-full h-48 rounded-btn overflow-hidden border border-amma-green-light shadow-sm mb-4">
              <img :src="documento.capaUrl" alt="Capa do Material" class="w-full h-full object-cover" />
            </div>

            <div v-else class="w-20 h-20 bg-amma-green-lighter rounded-full flex items-center justify-center mb-4">
              <span class="material-symbols-outlined text-4xl text-amma-green">picture_as_pdf</span>
            </div>

            <h4 class="text-sm font-bold text-amma-green-dark mb-1 w-full truncate px-2" :title="documento.nomeArquivo">
              {{ documento.nomeArquivo }}
            </h4>
            <span class="text-xs font-semibold text-grey-text/60 mb-6">{{ documento.tamanhoArquivo }}</span>

            <div class="w-full flex flex-col gap-3 mt-auto">
              <button
                type="button"
                @click="baixarPdf"
                class="w-full bg-amma-green-lighter hover:bg-amma-green hover:text-white text-amma-green-dark font-bold py-2.5 px-4 rounded-btn transition-colors text-sm flex items-center justify-center gap-2 border border-amma-green/20 efeito-zoom cursor-pointer"
              >
                <span class="material-symbols-outlined text-[20px]">download</span>
                Baixar PDF
              </button>

              <button
                type="button"
                @click="baixarPdf"
                class="w-full bg-white hover:bg-amma-bg text-grey-text font-bold py-2.5 px-4 rounded-btn border border-amma-green-light transition-colors text-sm flex items-center justify-center gap-2 efeito-zoom cursor-pointer"
              >
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
                <span class="text-sm font-semibold text-grey-text bg-black/5 w-fit px-3 py-1.5 rounded-btn mt-1">
                  {{ documento.tipoMaterial }}
                </span>
              </div>

              <div class="flex flex-col gap-1">
                <span class="text-xs font-bold text-grey-text/60 uppercase tracking-wide">Categoria</span>
                <span class="text-sm font-semibold text-grey-text">
                  {{ documento.categoria }}
                </span>
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
    </template>
  </div>
</template>
