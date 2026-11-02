<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import AppBreadcrumb from '@/components/ui/AppBreadcrumb.vue'
import { useLibraryStore } from '@/stores/libraryStore'
import { materialService } from '@/services/materialService'
import { useFeedbackStore } from '@/stores/feedbackStore'
import type { MaterialTypeUI, MaterialCategoryUI } from '@/types/library'

const router = useRouter()
const route = useRoute()
const libraryStore = useLibraryStore()
const feedback = useFeedbackStore()

const docId = computed(() => route.params.id as string | undefined)
const isEditing = computed(() => !!docId.value)

const isSubmitting = ref(false)
const isLoadingData = ref(false)

const titulo = ref('')
const descricao = ref('')
const tipoMaterial = ref<MaterialTypeUI | ''>('')
const categoria = ref<MaterialCategoryUI | ''>('')
const statusPublicacao = ref('RASCUNHO')
const fileName = ref('')
const fileInput = ref<HTMLInputElement | null>(null)

const coverName = ref('')
const coverPreview = ref<string | null>(null)
const coverInput = ref<HTMLInputElement | null>(null)

const tiposMaterial: MaterialTypeUI[] = ['Cartilha', 'Manual', 'Relatório Técnico', 'Guia', 'Outros']
const categorias: MaterialCategoryUI[] = ['Arborização Urbana', 'Caatinga', 'Educação Ambiental', 'Legislação']

const breadcrumbItems = computed(() => [
  { label: 'Biblioteca Digital', icon: 'local_library', routeName: 'biblioteca-lista' },
  { label: isEditing.value ? 'Editar Documento' : 'Novo Documento' }
])

const goBack = () => {
  router.push({ name: 'biblioteca-lista' })
}

const reverseMapCategory = (cat: string): MaterialCategoryUI => {
  const map: Record<string, MaterialCategoryUI> = {
    'ARBORIZACAO_URBANA': 'Arborização Urbana',
    'Arborização Urbana': 'Arborização Urbana',
    'CAATINGA': 'Caatinga',
    'Caatinga': 'Caatinga',
    'EDUCACAO_AMBIENTAL': 'Educação Ambiental',
    'Educação Ambiental': 'Educação Ambiental',
    'LEGISLACAO': 'Legislação',
    'Legislação': 'Legislação',
  }
  return map[cat] || 'Arborização Urbana'
}

const reverseMapType = (tipo: string): MaterialTypeUI => {
  const map: Record<string, MaterialTypeUI> = {
    'CARTILHA': 'Cartilha',
    'Cartilha': 'Cartilha',
    'MANUAL': 'Manual',
    'Manual': 'Manual',
    'RELATORIO_TECNICO': 'Relatório Técnico',
    'Relatório Técnico': 'Relatório Técnico',
    'GUIA': 'Guia',
    'Guia': 'Guia',
    'OUTROS': 'Outros',
    'Outros': 'Outros',
  }
  return map[tipo] || 'Outros'
}

onMounted(async () => {
  if (!isEditing.value || !docId.value) return

  isLoadingData.value = true
  try {
    const data = await materialService.getMaterialById(docId.value)
    titulo.value = data.title || ''
    descricao.value = data.description || ''
    statusPublicacao.value = data.status || 'RASCUNHO'

    if (data.type || data.materialType) {
      tipoMaterial.value = reverseMapType((data.type || data.materialType) as string)
    }

    if (data.category) {
      categoria.value = reverseMapCategory(data.category)
    }

    const firstDoc = data.documents?.[0]
    if (firstDoc) {
      fileName.value = firstDoc.originalFilename || firstDoc.fileName
    }

    const hasCover = (data.documents?.length ?? 0) > 1
    if (hasCover) {
      coverPreview.value = materialService.getCoverUrl(docId.value)
      coverName.value = 'capa_existente.jpg'
    }
  } catch (error) {
    feedback.showToast('Erro ao carregar os dados para edição.', 'error')
    goBack()
  } finally {
    isLoadingData.value = false
  }
})

const triggerFileInput = () => {
  if (fileInput.value) {
    fileInput.value.click()
  }
}

const handleFileUpload = (event: Event) => {
  const inputElement = event.target as HTMLInputElement

  if (!inputElement || !inputElement.files || inputElement.files.length === 0) {
    if (!isEditing.value) fileName.value = ''
    return
  }

  const uploadedFile = inputElement.files[0]

  if (uploadedFile && uploadedFile.type === 'application/pdf') {
    fileName.value = uploadedFile.name
  } else {
    feedback.showToast('Por favor, selecione apenas ficheiros PDF.', 'warning')
    inputElement.value = ''
    if (!isEditing.value) fileName.value = ''
  }
}

const triggerCoverInput = () => {
  if (coverInput.value) {
    coverInput.value.click()
  }
}

const handleCoverUpload = (event: Event) => {
  const inputElement = event.target as HTMLInputElement

  if (!inputElement || !inputElement.files || inputElement.files.length === 0) {
    if (!isEditing.value) {
      coverName.value = ''
      coverPreview.value = null
    }
    return
  }

  const uploadedCover = inputElement.files[0]

  if (uploadedCover && uploadedCover.type.startsWith('image/')) {
    coverName.value = uploadedCover.name
    coverPreview.value = URL.createObjectURL(uploadedCover)
  } else {
    feedback.showToast('Por favor, selecione apenas ficheiros de imagem (PNG, JPG, WEBP).', 'warning')
    inputElement.value = ''
    if (!isEditing.value) {
      coverName.value = ''
      coverPreview.value = null
    }
  }
}

const saveDocument = async () => {
  if (!isEditing.value && !fileInput.value?.files?.[0]) {
    feedback.showToast('É necessário anexar um arquivo PDF antes de prosseguir.', 'warning')
    return
  }

  const actionTitle = isEditing.value ? 'Salvar Alterações' : 'Guardar Documento'
  const actionMessage = isEditing.value
    ? `Deseja salvar as alterações realizadas no documento "${titulo.value}"?`
    : `Deseja cadastrar o documento "${titulo.value}"?`

  const confirmed = await feedback.showConfirm({
    title: actionTitle,
    message: actionMessage,
    confirmText: isEditing.value ? 'Salvar' : 'Guardar',
    cancelText: 'Voltar',
    variant: 'primary',
  })

  if (!confirmed) {
    return
  }

  isSubmitting.value = true

  const fileToUpload = fileInput.value?.files?.[0] || null
  const coverToUpload = coverInput.value?.files?.[0] || null

  let success = false

  if (isEditing.value && docId.value) {
    success = await libraryStore.updateDocument(docId.value, {
      title: titulo.value,
      description: descricao.value,
      status: statusPublicacao.value as any,
      type: tipoMaterial.value as MaterialTypeUI,
      category: categoria.value as MaterialCategoryUI,
      file: fileToUpload,
      coverPhoto: coverToUpload,
    })
  } else if (fileToUpload) {
    success = await libraryStore.createDocument({
      title: titulo.value,
      description: descricao.value,
      status: statusPublicacao.value as any,
      type: tipoMaterial.value as MaterialTypeUI,
      category: categoria.value as MaterialCategoryUI,
      file: fileToUpload,
      coverPhoto: coverToUpload,
    })
  }

  isSubmitting.value = false

  if (success) {
    goBack()
  }
}
</script>

<template>
  <div class="flex flex-col w-full">

    <AppBreadcrumb :items="breadcrumbItems" />

    <div v-if="isLoadingData" class="p-12 text-center text-grey-text font-semibold flex items-center justify-center gap-3">
      <span class="material-symbols-outlined animate-spin text-amma-green text-2xl">progress_activity</span>
      <span>Carregando dados do documento...</span>
    </div>

    <div v-else class="px-6 md:px-10 pb-6 pt-6 flex flex-col gap-6 w-full">
      <div class="flex items-center gap-4">
        <div class="w-12 h-12 flex items-center justify-center bg-amma-green-lighter/50 text-amma-green rounded-full shadow-sm">
          <span class="material-symbols-outlined text-[24px]">
            {{ isEditing ? 'edit_note' : 'note_add' }}
          </span>
        </div>
        <div>
          <h2 class="text-2xl font-bold text-amma-green-dark leading-none">
            {{ isEditing ? 'Editar Documento' : 'Novo Documento' }}
          </h2>
          <p class="text-sm text-grey-text mt-1">
            {{ isEditing ? 'Atualize as informações, capa ou substitua o ficheiro PDF do documento.' : 'Preencha os dados e anexe a capa e o ficheiro PDF para o catálogo.' }}
          </p>
        </div>
      </div>

      <div class="bg-white rounded-box shadow-sm border border-amma-green-light p-8">
        <form @submit.prevent="saveDocument" class="flex flex-col gap-6">

          <div class="grid grid-cols-1 md:grid-cols-2 gap-5">

            <div class="flex flex-col gap-1.5">
              <label class="text-sm font-semibold text-grey-text pl-1">Foto de Capa (Opcional)</label>
              <div
                @click="triggerCoverInput"
                class="relative w-full border-2 border-dashed border-amma-green-light hover:border-amma-green bg-amma-green-lighter/30 rounded-box p-4 flex flex-col items-center justify-center cursor-pointer transition-colors h-[150px] overflow-hidden"
              >
                <input
                  type="file"
                  ref="coverInput"
                  accept="image/*"
                  class="hidden"
                  @change="handleCoverUpload"
                />

                <template v-if="coverPreview">
                  <img :src="coverPreview" alt="Pré-visualização da Capa" class="absolute inset-0 w-full h-full object-cover rounded-box" />
                  <div class="absolute inset-0 bg-black/40 opacity-0 hover:opacity-100 transition-opacity flex items-center justify-center text-white text-xs font-semibold">
                    Substituir Capa
                  </div>
                </template>

                <template v-else>
                  <span class="material-symbols-outlined text-3xl text-amma-green mb-1">image</span>
                  <p class="text-xs font-medium text-grey-text text-center">
                    Clique para selecionar a capa<br/>
                    <span class="text-[11px] text-grey-text/60 font-normal">PNG, JPG ou WEBP</span>
                  </p>
                </template>
              </div>
            </div>

            <div class="flex flex-col gap-1.5">
              <label class="text-sm font-semibold text-grey-text pl-1">
                Arquivo PDF {{ isEditing ? '(Opcional para substituição)' : '*' }}
              </label>
              <div
                @click="triggerFileInput"
                class="w-full border-2 border-dashed border-amma-green-light hover:border-amma-green bg-amma-green-lighter/30 rounded-box p-4 flex flex-col items-center justify-center cursor-pointer transition-colors h-[150px]"
              >
                <input
                  type="file"
                  ref="fileInput"
                  accept=".pdf"
                  class="hidden"
                  @change="handleFileUpload"
                />
                <span class="material-symbols-outlined text-3xl text-amma-green mb-1">picture_as_pdf</span>
                <p v-if="!fileName" class="text-xs font-medium text-grey-text text-center">
                  Clique para selecionar o PDF {{ isEditing ? '' : '*' }}<br/>
                  <span class="text-[11px] text-grey-text/60 font-normal">Tamanho máximo: 10MB</span>
                </p>
                <div v-else class="text-center px-2">
                  <p class="text-xs font-bold text-amma-green-dark truncate max-w-full">
                    {{ fileName }}
                  </p>
                  <span v-if="isEditing" class="text-[11px] text-grey-text/70">
                    Clique para substituir o PDF atual
                  </span>
                </div>
              </div>
            </div>

          </div>

          <div class="flex flex-col gap-1.5">
            <label class="text-sm font-semibold text-grey-text pl-1">Título do Documento *</label>
            <input
              v-model="titulo"
              type="text"
              required
              placeholder="Ex: Guia de Plantio Urbano 2026"
              class="w-full bg-white focus:bg-amma-green-lighter border-2 border-amma-green-light focus:border-amma-green rounded-btn py-3 px-4 text-grey-text outline-none transition-all text-sm font-medium"
            />
          </div>

          <div class="grid grid-cols-1 md:grid-cols-2 gap-5">
            <div class="flex flex-col gap-1.5">
              <label class="text-sm font-semibold text-grey-text pl-1">Tipo de Material *</label>
              <select
                v-model="tipoMaterial"
                required
                class="w-full bg-white focus:bg-amma-green-lighter border-2 border-amma-green-light focus:border-amma-green rounded-btn py-3 px-4 text-grey-text outline-none transition-all text-sm font-medium appearance-none"
              >
                <option value="" disabled>Selecione o tipo</option>
                <option v-for="tipo in tiposMaterial" :key="tipo" :value="tipo">{{ tipo }}</option>
              </select>
            </div>

            <div class="flex flex-col gap-1.5">
              <label class="text-sm font-semibold text-grey-text pl-1">Categoria *</label>
              <select
                v-model="categoria"
                required
                class="w-full bg-white focus:bg-amma-green-lighter border-2 border-amma-green-light focus:border-amma-green rounded-btn py-3 px-4 text-grey-text outline-none transition-all text-sm font-medium appearance-none"
              >
                <option value="" disabled>Selecione a categoria</option>
                <option v-for="cat in categorias" :key="cat" :value="cat">{{ cat }}</option>
              </select>
            </div>
          </div>

          <div class="flex flex-col gap-1.5">
            <label class="text-sm font-semibold text-grey-text pl-1">Descrição</label>
            <textarea
              v-model="descricao"
              rows="3"
              placeholder="Breve resumo sobre o conteúdo do documento..."
              class="w-full bg-white focus:bg-amma-green-lighter border-2 border-amma-green-light focus:border-amma-green rounded-btn py-3 px-4 text-grey-text outline-none transition-all text-sm font-medium resize-none"
            ></textarea>
          </div>

          <div class="flex flex-col gap-1.5 border-t border-amma-green-light pt-6">
            <label class="text-sm font-semibold text-grey-text pl-1">Status de Publicação *</label>
            <div class="flex gap-4">
              <label class="flex items-center gap-2 cursor-pointer">
                <input type="radio" v-model="statusPublicacao" value="RASCUNHO" class="w-4 h-4 text-amma-green focus:ring-amma-green accent-amma-green" />
                <span class="text-sm font-medium text-grey-text">Salvar como Rascunho (Não visível ao público)</span>
              </label>
              <label class="flex items-center gap-2 cursor-pointer">
                <input type="radio" v-model="statusPublicacao" value="PUBLICADO" class="w-4 h-4 text-amma-green focus:ring-amma-green accent-amma-green" />
                <span class="text-sm font-medium text-grey-text">Publicar Imediatamente</span>
              </label>
            </div>
          </div>

          <div class="flex justify-end gap-3 mt-4">
            <button
              type="button"
              @click="goBack"
              class="bg-white hover:bg-amma-bg text-grey-text font-bold py-2.5 px-6 rounded-btn border-2 border-amma-green-light transition-colors shadow-sm text-sm"
              :disabled="isSubmitting"
            >
              Cancelar
            </button>
            <button
              type="submit"
              class="bg-amma-green hover:bg-amma-green-dark text-white font-bold py-2.5 px-6 rounded-btn shadow-md text-sm border-none outline-none efeito-zoom disabled:opacity-50 disabled:cursor-not-allowed"
              :disabled="isSubmitting"
            >
              {{ isSubmitting ? 'A salvar...' : (isEditing ? 'Atualizar Documento' : 'Guardar Documento') }}
            </button>
          </div>

        </form>
      </div>
    </div>
  </div>
</template>