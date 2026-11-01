<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import AppBreadcrumb from '@/components/ui/AppBreadcrumb.vue'
import { especieService } from '@/services/especieService'
import { useViveiroStore } from '@/stores/viveiroStore'
import { useFeedbackStore } from '@/stores/feedbackStore'
import type { EspecieDTO } from '@/types/viveiro'

const router = useRouter()
const route = useRoute()
const viveiroStore = useViveiroStore()
const feedback = useFeedbackStore()

const isLoading = ref(true)

const especie = ref<EspecieDTO>({
  id: '',
  nomePopular: '',
  nomeCientifico: '',
  familiaBotanica: '',
  categoria: '',
  porteArvore: '',
  recomendadaCalcadas: false,
  mudasDisponiveis: 0,
  tempoGerminacaoDias: '',
  descricaoCultivo: '',
  fotoUrl: '',
})

const breadcrumbItems = ref([
  { label: 'Viveiro', icon: 'psychiatry', routeName: 'viveiro' },
  { label: 'Carregando...' },
])

const carregarDetalhes = async () => {
  isLoading.value = true
  try {
    const id = route.params.id as string
    const data = await especieService.getById(id)

    especie.value = {
      ...data,
      fotoUrl: data.fotoUrl || especieService.getFotoUrl(data.id || id),
    }

    breadcrumbItems.value = [
      { label: 'Viveiro', icon: 'psychiatry', routeName: 'viveiro' },
      { label: especie.value.nomePopular },
    ]
  } catch (error) {
    feedback.showToast('Erro ao carregar os detalhes da espécie.', 'error')
    goBack()
  } finally {
    isLoading.value = false
  }
}

onMounted(() => {
  carregarDetalhes()
})

const goBack = () => router.push({ name: 'viveiro' })

const editEspecie = () => {
  router.push({ name: 'viveiro-editar', params: { id: especie.value.id } })
}

const deleteEspecie = async () => {
  if (!especie.value.id) return
  const removido = await viveiroStore.removeEspecie(especie.value.id)
  if (removido) {
    goBack()
  }
}
</script>

<template>
  <div class="flex flex-col">
    <div
      v-if="isLoading"
      class="p-12 text-center text-grey-text font-semibold flex items-center justify-center gap-3"
    >
      <span class="material-symbols-outlined animate-spin text-amma-green text-2xl"
        >progress_activity</span
      >
      <span>Carregando informações da espécie...</span>
    </div>

    <template v-else>
      <AppBreadcrumb :items="breadcrumbItems" />

      <div
        class="flex flex-col sm:flex-row justify-between items-start sm:items-center px-6 md:px-10 pb-6 pt-6"
      >
        <div class="flex items-center gap-4">
          <div
            class="w-12 h-12 shrink-0 flex items-center justify-center bg-amma-green-lighter/50 text-amma-green rounded-full shadow-sm"
          >
            <span class="material-symbols-outlined text-[24px]">psychiatry</span>
          </div>
          <div>
            <h2
              class="text-2xl font-bold text-amma-green-dark leading-tight line-clamp-1 max-w-[500px]"
              :title="especie.nomePopular"
            >
              {{ especie.nomePopular }}
            </h2>
            <div class="flex items-center gap-3 mt-1">
              <span class="text-xs italic font-medium text-grey-text">
                {{ especie.nomeCientifico }}
              </span>
              <span
                class="text-[10px] uppercase tracking-wider font-bold px-2.5 py-0.5 rounded-full bg-amma-green/20 text-amma-green-dark"
              >
                {{ especie.categoria }}
              </span>
            </div>
          </div>
        </div>

        <div class="flex items-center gap-3 w-full sm:w-auto mt-4 sm:mt-0">
          <button
            type="button"
            @click="deleteEspecie"
            class="flex-1 sm:flex-none bg-feedback-danger-bg hover:opacity-90 text-feedback-danger font-bold py-2 px-4 rounded-btn border border-feedback-danger-border transition-colors shadow-sm text-sm flex items-center justify-center gap-2 efeito-zoom cursor-pointer"
          >
            <span class="material-symbols-outlined text-[18px]">delete</span>
            Excluir
          </button>
          <button
            type="button"
            @click="editEspecie"
            class="flex-1 sm:flex-none bg-amma-green hover:bg-amma-green-dark text-white font-bold py-2.5 px-6 rounded-btn shadow-md text-sm border-none outline-none efeito-zoom flex items-center justify-center gap-2 cursor-pointer"
          >
            <span class="material-symbols-outlined text-[18px]">edit</span>
            Editar
          </button>
        </div>
      </div>

      <div class="grid grid-cols-1 lg:grid-cols-3 gap-6 px-6 md:px-10 pb-6 pt-6 items-stretch">
        <div class="lg:col-span-1 flex flex-col h-full">
          <div
            class="bg-white rounded-box shadow-sm border border-amma-green-light p-6 flex flex-col items-center justify-between text-center h-full"
          >
            <div
              v-if="especie.fotoUrl"
              class="w-full h-52 rounded-btn overflow-hidden border border-amma-green-light shadow-sm mb-4"
            >
              <img
                :src="especie.fotoUrl"
                alt="Foto da Espécie"
                class="w-full h-full object-cover"
              />
            </div>

            <div
              v-else
              class="w-20 h-20 bg-amma-green-lighter rounded-full flex items-center justify-center mb-4"
            >
              <span class="material-symbols-outlined text-4xl text-amma-green">psychiatry</span>
            </div>

            <h4
              class="text-base font-bold text-amma-green-dark mb-1 w-full truncate px-2"
              :title="especie.nomePopular"
            >
              {{ especie.nomePopular }}
            </h4>
            <span class="text-xs italic text-grey-text/70 mb-4">{{ especie.nomeCientifico }}</span>

            <div class="w-full bg-amma-green-lighter/60 rounded-btn p-4 border border-amma-green-light mt-auto">
              <span class="text-xs font-semibold text-grey-text block mb-1">Mudas Disponíveis</span>
              <span class="text-2xl font-black text-amma-green-dark">
                {{ especie.mudasDisponiveis || 0 }}
                <span class="text-xs font-medium text-grey-text">unidades</span>
              </span>
            </div>
          </div>
        </div>

        <div class="lg:col-span-2 flex flex-col h-full">
          <div
            class="bg-white rounded-box shadow-sm border border-amma-green-light p-8 h-full flex flex-col justify-center"
          >
            <h3
              class="text-lg font-bold text-amma-green-dark mb-6 border-b border-amma-green-light pb-4 flex items-center gap-2"
            >
              <span class="material-symbols-outlined text-amma-green">eco</span>
              Características Botânicas
            </h3>

            <div class="grid grid-cols-1 sm:grid-cols-2 gap-y-6 gap-x-8">
              <div class="flex flex-col gap-1">
                <span class="text-xs font-bold text-grey-text/60 uppercase tracking-wide"
                  >Família Botânica</span
                >
                <span class="text-sm font-semibold text-grey-text bg-black/5 w-fit px-3 py-1.5 rounded-btn mt-1">
                  {{ especie.familiaBotanica || 'Não informada' }}
                </span>
              </div>

              <div class="flex flex-col gap-1">
                <span class="text-xs font-bold text-grey-text/60 uppercase tracking-wide"
                  >Porte da Árvore</span
                >
                <span class="text-sm font-semibold text-grey-text mt-1">
                  {{ especie.porteArvore }}
                </span>
              </div>

              <div class="flex flex-col gap-1">
                <span class="text-xs font-bold text-grey-text/60 uppercase tracking-wide"
                  >Recomendada para Calçadas</span
                >
                <span
                  :class="[
                    'text-xs font-bold px-3 py-1.5 rounded-full inline-block w-fit mt-1',
                    especie.recomendadaCalcadas
                      ? 'bg-amma-green/20 text-amma-green-dark'
                      : 'bg-feedback-danger-bg text-feedback-danger border border-feedback-danger-border',
                  ]"
                >
                  {{ especie.recomendadaCalcadas ? 'Sim (Raízes não agressivas)' : 'Não' }}
                </span>
              </div>

              <div class="flex flex-col gap-1">
                <span class="text-xs font-bold text-grey-text/60 uppercase tracking-wide"
                  >Tempo Médio de Germinação</span
                >
                <span class="text-sm font-semibold text-grey-text mt-1">
                  {{ especie.tempoGerminacaoDias ? `${especie.tempoGerminacaoDias} dias` : 'Não especificado' }}
                </span>
              </div>

              <div class="flex flex-col gap-1 sm:col-span-2 mt-2">
                <span class="text-xs font-bold text-grey-text/60 uppercase tracking-wide"
                  >Descrição e Orientações de Cultivo</span
                >
                <p
                  class="text-sm font-medium text-grey-text mt-1 leading-relaxed border-l-2 border-amma-green pl-3"
                >
                  {{ especie.descricaoCultivo || 'Nenhuma orientação de cultivo registrada.' }}
                </p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>