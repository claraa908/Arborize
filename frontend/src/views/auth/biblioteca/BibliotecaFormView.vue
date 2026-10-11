<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import AppBreadcrumb from '@/components/ui/AppBreadcrumb.vue'

const router = useRouter()

const titulo = ref('')
const descricao = ref('')
const tipoMaterial = ref('')
const categoria = ref('')
const statusPublicacao = ref('RASCUNHO')
const fileName = ref('')
const fileInput = ref<HTMLInputElement | null>(null)

// Variáveis para a Foto de Capa e Pré-visualização
const coverName = ref('')
const coverPreview = ref<string | null>(null)
const coverInput = ref<HTMLInputElement | null>(null)

const tiposMaterial = ['Cartilha', 'Manual', 'Relatório Técnico', 'Guia', 'Outros']
const categorias = ['Arborização Urbana', 'Caatinga', 'Educação Ambiental', 'Legislação']

const breadcrumbItems = [
  { label: 'Biblioteca Digital', icon: 'local_library', routeName: 'biblioteca-lista' },
  { label: 'Novo Documento' }
]

const goBack = () => {
  router.push({ name: 'biblioteca-lista' })
}

const triggerFileInput = () => {
  if (fileInput.value) {
    fileInput.value.click()
  }
}

const handleFileUpload = (event: Event) => {
  const inputElement = event.target as HTMLInputElement
  
  if (!inputElement || !inputElement.files || inputElement.files.length === 0) {
    fileName.value = ''
    return
  }

  const uploadedFile = inputElement.files[0]
  
  if (uploadedFile && uploadedFile.type === 'application/pdf') {
    fileName.value = uploadedFile.name
  } else {
    alert('Por favor, selecione apenas ficheiros PDF.')
    inputElement.value = ''
    fileName.value = ''
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
    coverName.value = ''
    coverPreview.value = null
    return
  }

  const uploadedCover = inputElement.files[0]
  
  if (uploadedCover && uploadedCover.type.startsWith('image/')) {
    coverName.value = uploadedCover.name
    // Cria uma URL temporária para pré-visualizar a imagem escolhida
    coverPreview.value = URL.createObjectURL(uploadedCover)
  } else {
    alert('Por favor, selecione apenas ficheiros de imagem (PNG, JPG, WEBP).')
    inputElement.value = ''
    coverName.value = ''
    coverPreview.value = null
  }
}

const saveDocument = () => {
  console.log('Guardando documento...', {
    titulo: titulo.value,
    descricao: descricao.value,
    tipoMaterial: tipoMaterial.value,
    categoria: categoria.value,
    statusPublicacao: statusPublicacao.value,
    arquivo: fileName.value,
    capa: coverName.value
  })
  goBack()
}

</script>

<template>
  <div class="flex flex-col w-full">
    
    <AppBreadcrumb :items="breadcrumbItems" />

    <div class="px-6 md:px-10 pb-6 pt-6 flex flex-col gap-6 w-full">
      <div class="flex items-center gap-4">
        <div class="w-12 h-12 flex items-center justify-center bg-amma-green-lighter/50 text-amma-green rounded-full shadow-sm">
            <span class="material-symbols-outlined text-[24px]">note_add</span>
        </div>
        <div>
            <h2 class="text-2xl font-bold text-amma-green-dark leading-none">Novo Documento</h2>
            <p class="text-sm text-grey-text mt-1">Preencha os dados e anexe a capa e o ficheiro PDF para o catálogo.</p>
        </div>
      </div>

      <div class="bg-white rounded-box shadow-sm border border-amma-green-light p-8">
        <form @submit.prevent="saveDocument" class="flex flex-col gap-6">
          
          <div class="grid grid-cols-1 md:grid-cols-2 gap-5">
            
            <!-- Upload de Capa com Pré-visualização -->
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
                
                <!-- Se houver imagem selecionada, mostra a pré-visualização em miniatura -->
                <template v-if="coverPreview">
                  <img :src="coverPreview" alt="Pré-visualização da Capa" class="absolute inset-0 w-full h-full object-cover rounded-box" />
                  <div class="absolute inset-0 bg-black/40 opacity-0 hover:opacity-100 transition-opacity flex items-center justify-center text-white text-xs font-semibold">
                    Alterar Capa
                  </div>
                </template>

                <!-- Estado padrão (sem imagem) -->
                <template v-else>
                  <span class="material-symbols-outlined text-3xl text-amma-green mb-1">image</span>
                  <p class="text-xs font-medium text-grey-text text-center">
                    Clique para selecionar a capa<br/>
                    <span class="text-[11px] text-grey-text/60 font-normal">PNG, JPG ou WEBP</span>
                  </p>
                </template>
              </div>
            </div>

            <!-- Upload de PDF -->
            <div class="flex flex-col gap-1.5">
              <label class="text-sm font-semibold text-grey-text pl-1">Arquivo PDF *</label>
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
                  Clique para selecionar o PDF *<br/>
                  <span class="text-[11px] text-grey-text/60 font-normal">Tamanho máximo: 10MB</span>
                </p>
                <p v-else class="text-xs font-bold text-amma-green-dark text-center truncate max-w-full px-2">
                  {{ fileName }}
                </p>
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
            >
              Cancelar
            </button>
            <button 
              type="submit"
              class="bg-amma-green hover:bg-amma-green-dark text-white font-bold py-2.5 px-6 rounded-btn shadow-md text-sm border-none outline-none efeito-zoom"
            >
              Guardar Documento
            </button>
          </div>
          
        </form>
      </div>
    </div>
  </div>
</template>