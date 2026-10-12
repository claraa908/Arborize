<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import AppBreadcrumb from '@/components/ui/AppBreadcrumb.vue'

const router = useRouter()

const formEspecie = ref({
  nomePopular: '',
  nomeCientifico: '',
  familiaBotanica: '',
  categoria: 'Nativa da Caatinga',
  porte: 'Médio',
  adequadoCalcada: 'Sim',
  tempoGerminacaoDias: '',
  descricao: '',
  imagem: null as File | null
})

const previewImagem = ref<string | null>(null)
const imageInput = ref<HTMLInputElement | null>(null)
const mostrarPopup = ref(false)

const categorias = ['Nativa da Caatinga', 'Frutífera', 'Ornamental', 'Medicinal', 'Exótica']
const portes = [
  { valor: 'Pequeno', rotulo: 'Pequeno (Até 4m)' },
  { valor: 'Médio', rotulo: 'Médio (4m a 8m)' },
  { valor: 'Grande', rotulo: 'Grande (Acima de 8m)' }
]

const breadcrumbItems = [
  { label: 'Viveiro', icon: 'psychiatry', routeName: 'viveiro' },
  { label: 'Cadastrar Espécie' }
]

const goBack = () => {
  router.push({ name: 'viveiro' })
}

const triggerImageInput = () => {
  if (imageInput.value) {
    imageInput.value.click()
  }
}

const handleFileUpload = (event: Event) => {
  const target = event.target as HTMLInputElement
  if (!target || !target.files || target.files.length === 0) {
    formEspecie.value.imagem = null
    previewImagem.value = null
    return
  }

  const file = target.files[0]
  if (file && file.type.startsWith('image/')) {
    formEspecie.value.imagem = file
    previewImagem.value = URL.createObjectURL(file)
  } else {
    alert('Por favor, selecione apenas arquivos de imagem (PNG, JPG, WEBP).')
    target.value = ''
    formEspecie.value.imagem = null
    previewImagem.value = null
  }
}

const formVazio = () => ({
  nomePopular: '',
  nomeCientifico: '',
  familiaBotanica: '',
  categoria: 'Nativa da Caatinga',
  porte: 'Médio',
  adequadoCalcada: 'Sim',
  tempoGerminacaoDias: '',
  descricao: '',
  imagem: null as File | null
})

const submeterEspecie = () => {
  mostrarPopup.value = true
  formEspecie.value = { ...formVazio() }
  previewImagem.value = null

  setTimeout(() => {
    mostrarPopup.value = false
    goBack()
  }, 1500)
}
</script>

<template>
  <div class="flex flex-col w-full">
    <AppBreadcrumb :items="breadcrumbItems" />

    <div class="px-6 md:px-10 pb-6 pt-6 flex flex-col gap-6 w-full">
      <div class="flex items-center gap-4">
        <div class="w-12 h-12 flex items-center justify-center bg-amma-green-lighter/50 text-amma-green rounded-full shadow-sm">
          <span class="material-symbols-outlined text-[24px]">add</span>
        </div>
        <div>
          <h2 class="text-2xl font-bold text-amma-green-dark leading-none">Cadastrar Espécie Botânica</h2>
          <p class="text-sm text-grey-text mt-1">Preencha os dados e características da espécie para o viveiro municipal.</p>
        </div>
      </div>

      <div class="bg-white rounded-box shadow-sm border border-amma-green-light p-8">
        <form @submit.prevent="submeterEspecie" class="flex flex-col gap-6">
          <div class="grid grid-cols-1 md:grid-cols-2 gap-5">
            <div class="flex flex-col gap-1.5">
              <label class="text-sm font-semibold text-grey-text pl-1">Nome Popular *</label>
              <input
                v-model="formEspecie.nomePopular"
                type="text"
                required
                placeholder="Ex: Ipê-amarelo, Aroeira-do-sertão, Mulungu"
                class="w-full bg-white focus:bg-amma-green-lighter border-2 border-amma-green-light focus:border-amma-green rounded-btn py-3 px-4 text-grey-text outline-none transition-all text-sm font-medium"
              />
            </div>

            <div class="flex flex-col gap-1.5">
              <label class="text-sm font-semibold text-grey-text pl-1">Nome Científico *</label>
              <input
                v-model="formEspecie.nomeCientifico"
                type="text"
                required
                placeholder="Ex: Handroanthus chrysotrichus"
                class="w-full bg-white focus:bg-amma-green-lighter border-2 border-amma-green-light focus:border-amma-green rounded-btn py-3 px-4 text-grey-text italic outline-none transition-all text-sm font-medium"
              />
            </div>
          </div>

          <div class="grid grid-cols-1 md:grid-cols-3 gap-5">
            <div class="flex flex-col gap-1.5">
              <label class="text-sm font-semibold text-grey-text pl-1">Família Botânica</label>
              <input
                v-model="formEspecie.familiaBotanica"
                type="text"
                placeholder="Ex: Bignoniaceae"
                class="w-full bg-white focus:bg-amma-green-lighter border-2 border-amma-green-light focus:border-amma-green rounded-btn py-3 px-4 text-grey-text outline-none transition-all text-sm font-medium"
              />
            </div>

            <div class="flex flex-col gap-1.5">
              <label class="text-sm font-semibold text-grey-text pl-1">Categoria *</label>
              <select
                v-model="formEspecie.categoria"
                required
                class="w-full bg-white focus:bg-amma-green-lighter border-2 border-amma-green-light focus:border-amma-green rounded-btn py-3 px-4 text-grey-text outline-none transition-all text-sm font-medium appearance-none"
              >
                <option v-for="cat in categorias" :key="cat" :value="cat">{{ cat }}</option>
              </select>
            </div>

            <div class="flex flex-col gap-1.5">
              <label class="text-sm font-semibold text-grey-text pl-1">Porte da Árvore *</label>
              <select
                v-model="formEspecie.porte"
                required
                class="w-full bg-white focus:bg-amma-green-lighter border-2 border-amma-green-light focus:border-amma-green rounded-btn py-3 px-4 text-grey-text outline-none transition-all text-sm font-medium appearance-none"
              >
                <option v-for="porte in portes" :key="porte.valor" :value="porte.valor">{{ porte.rotulo }}</option>
              </select>
            </div>
          </div>

          <div class="grid grid-cols-1 md:grid-cols-2 gap-5">
            <div class="flex flex-col gap-1.5">
              <label class="text-sm font-semibold text-grey-text pl-1">Imagem da Espécie *</label>
              <div
                @click="triggerImageInput"
                class="relative w-full border-2 border-dashed border-amma-green-light hover:border-amma-green bg-amma-green-lighter/30 rounded-box p-4 flex flex-col items-center justify-center cursor-pointer transition-colors h-[150px] overflow-hidden"
              >
                <input
                  type="file"
                  ref="imageInput"
                  accept="image/*"
                  class="hidden"
                  @change="handleFileUpload"
                />

                <template v-if="previewImagem">
                  <img :src="previewImagem" alt="Pré-visualização da Espécie" class="absolute inset-0 w-full h-full object-cover rounded-box" />
                  <div class="absolute inset-0 bg-black/40 opacity-0 hover:opacity-100 transition-opacity flex items-center justify-center text-white text-xs font-semibold">
                    Alterar Imagem
                  </div>
                </template>

                <template v-else>
                  <span class="material-symbols-outlined text-3xl text-amma-green mb-1">image</span>
                  <p class="text-xs font-medium text-grey-text text-center">
                    Clique para selecionar a imagem *<br/>
                    <span class="text-[11px] text-grey-text/60 font-normal">PNG, JPG ou WEBP</span>
                  </p>
                </template>
              </div>
            </div>

            <div class="flex flex-col justify-between gap-4">
              <div class="flex flex-col gap-1.5">
                <label class="text-sm font-semibold text-grey-text pl-1">Recomendada para Calçadas? *</label>
                <div class="flex flex-col sm:flex-row gap-4 pt-1">
                  <label class="flex items-center gap-2 cursor-pointer">
                    <input
                      type="radio"
                      v-model="formEspecie.adequadoCalcada"
                      value="Sim"
                      class="w-4 h-4 text-amma-green focus:ring-amma-green accent-amma-green"
                    />
                    <span class="text-sm font-medium text-grey-text">Sim (Raízes não agressivas)</span>
                  </label>
                  <label class="flex items-center gap-2 cursor-pointer">
                    <input
                      type="radio"
                      v-model="formEspecie.adequadoCalcada"
                      value="Não"
                      class="w-4 h-4 text-amma-green focus:ring-amma-green accent-amma-green"
                    />
                    <span class="text-sm font-medium text-grey-text">Não</span>
                  </label>
                </div>
              </div>

              <div class="flex flex-col gap-1.5">
                <label class="text-sm font-semibold text-grey-text pl-1">Tempo médio de germinação (em dias)</label>
                <input
                  v-model="formEspecie.tempoGerminacaoDias"
                  type="number"
                  placeholder="Ex: 15"
                  class="w-full bg-white focus:bg-amma-green-lighter border-2 border-amma-green-light focus:border-amma-green rounded-btn py-3 px-4 text-grey-text outline-none transition-all text-sm font-medium"
                />
              </div>
            </div>
          </div>

          <div class="flex flex-col gap-1.5">
            <label class="text-sm font-semibold text-grey-text pl-1">Descrição / Orientações de Cultivo</label>
            <textarea
              v-model="formEspecie.descricao"
              rows="3"
              placeholder="Descreva as características da espécie, época de florada, recomendações de solo e rega..."
              class="w-full bg-white focus:bg-amma-green-lighter border-2 border-amma-green-light focus:border-amma-green rounded-btn py-3 px-4 text-grey-text outline-none transition-all text-sm font-medium resize-none"
            ></textarea>
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
              Salvar Espécie
            </button>
          </div>
        </form>
      </div>
    </div>

    <Transition
      enter-active-class="transition duration-300 ease-out"
      enter-from-class="opacity-0 -translate-y-6"
      enter-to-class="opacity-100 translate-y-0"
      leave-active-class="transition duration-200 ease-in"
      leave-from-class="opacity-100 translate-y-0"
      leave-to-class="opacity-0 -translate-y-6"
    >
      <div
        v-if="mostrarPopup"
        class="fixed top-6 left-1/2 -translate-x-1/2 z-50 bg-amma-green text-white font-bold py-3 px-8 rounded-btn shadow-xl border border-white/20 text-center text-sm md:text-base flex items-center gap-3"
      >
        <span class="material-symbols-outlined text-2xl">check_circle</span>
        <span>Espécie cadastrada com sucesso!</span>
      </div>
    </Transition>
  </div>
</template>