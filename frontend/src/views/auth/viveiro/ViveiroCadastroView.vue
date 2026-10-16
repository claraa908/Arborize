<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import AppBreadcrumb from '@/components/ui/AppBreadcrumb.vue'
import { useViveiroStore } from '@/stores/viveiroStore'
import { especieService } from '@/services/especieService'
import { useFeedbackStore } from '@/stores/feedbackStore'
import type { CreateEspeciePayload } from '@/types/viveiro'

const router = useRouter()
const route = useRoute()
const viveiroStore = useViveiroStore()
const feedback = useFeedbackStore()

const especieId = computed(() => route.params.id as string | undefined)
const isEditing = computed(() => !!especieId.value)

const isSubmitting = ref(false)
const isLoadingData = ref(false)

const form = ref<CreateEspeciePayload>({
  nomePopular: '',
  nomeCientifico: '',
  familiaBotanica: '',
  categoria: 'Nativa da Caatinga',
  porte: 'Médio',
  adequadoCalcada: 'Sim',
  tempoGerminacaoDias: '',
  descricao: '',
  imagem: null,
})

const previewImagem = ref<string | null>(null)
const imageInput = ref<HTMLInputElement | null>(null)

const categorias = ['Nativa da Caatinga', 'Frutífera', 'Ornamental', 'Medicinal', 'Exótica']
const portes = [
  { valor: 'Pequeno', rotulo: 'Pequeno (Até 4m)' },
  { valor: 'Médio', rotulo: 'Médio (4m a 8m)' },
  { valor: 'Grande', rotulo: 'Grande (Acima de 8m)' },
]

const breadcrumbItems = computed(() => [
  { label: 'Viveiro', icon: 'psychiatry', routeName: 'viveiro' },
  { label: isEditing.value ? 'Editar Espécie' : 'Cadastrar Espécie' },
])

const goBack = () => {
  router.push({ name: 'viveiro' })
}

onMounted(async () => {
  if (!isEditing.value || !especieId.value) return

  isLoadingData.value = true
  try {
    const data = await especieService.getById(especieId.value)
    form.value = {
      nomePopular: data.nomePopular || '',
      nomeCientifico: data.nomeCientifico || '',
      familiaBotanica: data.familiaBotanica || '',
      categoria: data.categoria || 'Nativa da Caatinga',
      porte: data.porteArvore || 'Médio',
      adequadoCalcada: data.recomendadaCalcadas ? 'Sim' : 'Não',
      tempoGerminacaoDias: data.tempoGerminacaoDias ? String(data.tempoGerminacaoDias) : '',
      descricao: data.descricaoCultivo || '',
      imagem: null,
    }

    previewImagem.value = data.fotoUrl || especieService.getFotoUrl(especieId.value)
  } catch (error) {
    feedback.showToast('Erro ao carregar os dados da espécie.', 'error')
    goBack()
  } finally {
    isLoadingData.value = false
  }
})

const triggerImageInput = () => {
  if (imageInput.value) {
    imageInput.value.click()
  }
}

const handleFileUpload = (event: Event) => {
  const target = event.target as HTMLInputElement
  if (!target || !target.files || target.files.length === 0) {
    if (!isEditing.value) {
      form.value.imagem = null
      previewImagem.value = null
    }
    return
  }

  const file = target.files[0]
  if (file && file.type.startsWith('image/')) {
    form.value.imagem = file
    previewImagem.value = URL.createObjectURL(file)
  } else {
    feedback.showToast('Por favor, selecione apenas arquivos de imagem (PNG, JPG, WEBP).', 'warning')
    target.value = ''
    if (!isEditing.value) {
      form.value.imagem = null
      previewImagem.value = null
    }
  }
}

const submeterEspecie = async () => {
  if (!isEditing.value && !form.value.imagem) {
    feedback.showToast('Por favor, anexe uma imagem representativa da espécie.', 'warning')
    return
  }

  const actionTitle = isEditing.value ? 'Salvar Alterações' : 'Cadastrar Espécie Botânica'
  const actionMessage = isEditing.value
    ? `Deseja salvar as alterações da espécie "${form.value.nomePopular}"?`
    : `Confirma a inclusão de "${form.value.nomePopular}" no viveiro municipal?`

  const confirmed = await feedback.showConfirm({
    title: actionTitle,
    message: actionMessage,
    confirmText: isEditing.value ? 'Salvar' : 'Cadastrar',
    cancelText: 'Cancelar',
    variant: 'primary',
  })

  if (!confirmed) {
    return
  }

  isSubmitting.value = true

  let sucesso = false
  if (isEditing.value && especieId.value) {
    sucesso = await viveiroStore.updateEspecie(especieId.value, form.value)
  } else {
    sucesso = await viveiroStore.createEspecie(form.value)
  }

  isSubmitting.value = false

  if (sucesso) {
    goBack()
  }
}
</script>

<template>
  <div class="flex flex-col w-full">
    <AppBreadcrumb :items="breadcrumbItems" />

    <div
      v-if="isLoadingData"
      class="p-12 text-center text-grey-text font-semibold flex items-center justify-center gap-3"
    >
      <span class="material-symbols-outlined animate-spin text-amma-green text-2xl"
        >progress_activity</span
      >
      <span>Carregando dados da espécie...</span>
    </div>

    <div v-else class="px-6 md:px-10 pb-6 pt-6 flex flex-col gap-6 w-full">
      <div class="flex items-center gap-4">
        <div
          class="w-12 h-12 flex items-center justify-center bg-amma-green-lighter/50 text-amma-green rounded-full shadow-sm"
        >
          <span class="material-symbols-outlined text-[24px]">
            {{ isEditing ? 'edit_square' : 'add' }}
          </span>
        </div>
        <div>
          <h2 class="text-2xl font-bold text-amma-green-dark leading-none">
            {{ isEditing ? 'Editar Espécie Botânica' : 'Cadastrar Espécie Botânica' }}
          </h2>
          <p class="text-sm text-grey-text mt-1">
            {{
              isEditing
                ? 'Atualize as características, imagem ou orientações da espécie.'
                : 'Preencha os dados e características da espécie para o viveiro municipal.'
            }}
          </p>
        </div>
      </div>

      <div class="bg-white rounded-box shadow-sm border border-amma-green-light p-8">
        <form @submit.prevent="submeterEspecie" class="flex flex-col gap-6">
          <div class="grid grid-cols-1 md:grid-cols-2 gap-5">
            <div class="flex flex-col gap-1.5">
              <label class="text-sm font-semibold text-grey-text pl-1">Nome Popular *</label>
              <input
                v-model="form.nomePopular"
                type="text"
                required
                placeholder="Ex: Ipê-amarelo, Aroeira-do-sertão, Mulungu"
                class="w-full bg-white focus:bg-amma-green-lighter border-2 border-amma-green-light focus:border-amma-green rounded-btn py-3 px-4 text-grey-text outline-none transition-all text-sm font-medium"
              />
            </div>

            <div class="flex flex-col gap-1.5">
              <label class="text-sm font-semibold text-grey-text pl-1">Nome Científico *</label>
              <input
                v-model="form.nomeCientifico"
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
                v-model="form.familiaBotanica"
                type="text"
                placeholder="Ex: Bignoniaceae"
                class="w-full bg-white focus:bg-amma-green-lighter border-2 border-amma-green-light focus:border-amma-green rounded-btn py-3 px-4 text-grey-text outline-none transition-all text-sm font-medium"
              />
            </div>

            <div class="flex flex-col gap-1.5">
              <label class="text-sm font-semibold text-grey-text pl-1">Categoria *</label>
              <select
                v-model="form.categoria"
                required
                class="w-full bg-white focus:bg-amma-green-lighter border-2 border-amma-green-light focus:border-amma-green rounded-btn py-3 px-4 text-grey-text outline-none transition-all text-sm font-medium appearance-none"
              >
                <option v-for="cat in categorias" :key="cat" :value="cat">{{ cat }}</option>
              </select>
            </div>

            <div class="flex flex-col gap-1.5">
              <label class="text-sm font-semibold text-grey-text pl-1">Porte da Árvore *</label>
              <select
                v-model="form.porte"
                required
                class="w-full bg-white focus:bg-amma-green-lighter border-2 border-amma-green-light focus:border-amma-green rounded-btn py-3 px-4 text-grey-text outline-none transition-all text-sm font-medium appearance-none"
              >
                <option v-for="porte in portes" :key="porte.valor" :value="porte.valor">
                  {{ porte.rotulo }}
                </option>
              </select>
            </div>
          </div>

          <div class="grid grid-cols-1 md:grid-cols-2 gap-5">
            <div class="flex flex-col gap-1.5">
              <label class="text-sm font-semibold text-grey-text pl-1">
                Imagem da Espécie {{ isEditing ? '(Opcional para substituição)' : '*' }}
              </label>
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
                  <img
                    :src="previewImagem"
                    alt="Pré-visualização da Espécie"
                    class="absolute inset-0 w-full h-full object-cover rounded-box"
                  />
                  <div
                    class="absolute inset-0 bg-black/40 opacity-0 hover:opacity-100 transition-opacity flex items-center justify-center text-white text-xs font-semibold"
                  >
                    Substituir Imagem
                  </div>
                </template>

                <template v-else>
                  <span class="material-symbols-outlined text-3xl text-amma-green mb-1">image</span>
                  <p class="text-xs font-medium text-grey-text text-center">
                    Clique para selecionar a imagem {{ isEditing ? '' : '*' }}<br />
                    <span class="text-[11px] text-grey-text/60 font-normal">PNG, JPG ou WEBP</span>
                  </p>
                </template>
              </div>
            </div>

            <div class="flex flex-col justify-between gap-4">
              <div class="flex flex-col gap-1.5">
                <label class="text-sm font-semibold text-grey-text pl-1"
                  >Recomendada para Calçadas? *</label
                >
                <div class="flex flex-col sm:flex-row gap-4 pt-1">
                  <label class="flex items-center gap-2 cursor-pointer">
                    <input
                      type="radio"
                      v-model="form.adequadoCalcada"
                      value="Sim"
                      class="w-4 h-4 text-amma-green focus:ring-amma-green accent-amma-green"
                    />
                    <span class="text-sm font-medium text-grey-text"
                      >Sim (Raízes não agressivas)</span
                    >
                  </label>
                  <label class="flex items-center gap-2 cursor-pointer">
                    <input
                      type="radio"
                      v-model="form.adequadoCalcada"
                      value="Não"
                      class="w-4 h-4 text-amma-green focus:ring-amma-green accent-amma-green"
                    />
                    <span class="text-sm font-medium text-grey-text">Não</span>
                  </label>
                </div>
              </div>

              <div class="flex flex-col gap-1.5">
                <label class="text-sm font-semibold text-grey-text pl-1"
                  >Tempo médio de germinação (em dias)</label
                >
                <input
                  v-model="form.tempoGerminacaoDias"
                  type="number"
                  placeholder="Ex: 15"
                  class="w-full bg-white focus:bg-amma-green-lighter border-2 border-amma-green-light focus:border-amma-green rounded-btn py-3 px-4 text-grey-text outline-none transition-all text-sm font-medium"
                />
              </div>
            </div>
          </div>

          <div class="flex flex-col gap-1.5">
            <label class="text-sm font-semibold text-grey-text pl-1"
              >Descrição / Orientações de Cultivo</label
            >
            <textarea
              v-model="form.descricao"
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
              :disabled="isSubmitting"
            >
              Cancelar
            </button>
            <button
              type="submit"
              class="bg-amma-green hover:bg-amma-green-dark text-white font-bold py-2.5 px-6 rounded-btn shadow-md text-sm border-none outline-none efeito-zoom disabled:opacity-50 disabled:cursor-not-allowed"
              :disabled="isSubmitting"
            >
              {{ isSubmitting ? 'A salvar...' : isEditing ? 'Salvar Alterações' : 'Salvar Espécie' }}
            </button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>