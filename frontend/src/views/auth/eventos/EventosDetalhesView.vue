<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import AppBreadcrumb from '@/components/ui/AppBreadcrumb.vue'
import { eventoService } from '@/services/eventoService'
import { useEventoStore } from '@/stores/eventoStore'
import { useFeedbackStore } from '@/stores/feedbackStore'
import type { EventoDTO } from '@/types/eventos'

const router = useRouter()
const route = useRoute()
const eventoStore = useEventoStore()
const feedback = useFeedbackStore()

const isLoading = ref(true)

const evento = ref<EventoDTO>({
  id: '',
  nome: '',
  descricao: '',
  bannerUrl: '',
  dataInicio: '',
  dataTermino: '',
  localConcentracao: '',
  destinoFinal: '',
  horarioInicio: '',
  descricaoPontoPartida: '',
  horarioTermino: '',
  descricaoEncerramento: '',
  status: 'PLANEJADO',
})

const breadcrumbItems = ref([
  { label: 'Eventos Ambientais', icon: 'event', routeName: 'eventos' },
  { label: 'Carregando...' },
])

const carregarDetalhes = async () => {
  isLoading.value = true
  try {
    const id = route.params.id as string
    const data = await eventoService.getById(id)

    evento.value = {
      ...data,
      bannerUrl: data.bannerUrl || eventoService.getBannerUrl(data.id || id),
    }

    breadcrumbItems.value = [
      { label: 'Eventos Ambientais', icon: 'event', routeName: 'eventos' },
      { label: evento.value.nome },
    ]
  } catch (error) {
    feedback.showToast('Erro ao carregar os detalhes do evento.', 'error')
    goBack()
  } finally {
    isLoading.value = false
  }
}

onMounted(() => {
  carregarDetalhes()
})

const goBack = () => router.push({ name: 'eventos' })

const editEvento = () => {
  router.push({ name: 'eventos-editar', params: { id: evento.value.id } })
}

const deleteEvento = async () => {
  if (!evento.value.id) return
  const removido = await eventoStore.removeEvento(evento.value.id)
  if (removido) {
    goBack()
  }
}

const statusClass = (status: string) => {
  switch (status) {
    case 'CONFIRMADO':
      return 'bg-amma-green/20 text-amma-green-dark'
    case 'PLANEJADO':
      return 'bg-feedback-info-bg text-feedback-info border border-feedback-info-border'
    case 'ENCERRADO':
      return 'bg-black/10 text-grey-text'
    default:
      return 'bg-feedback-warning-bg text-feedback-warning border border-feedback-warning-border'
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
      <span>Carregando informações do evento...</span>
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
            <span class="material-symbols-outlined text-[24px]">event</span>
          </div>
          <div>
            <h2
              class="text-2xl font-bold text-amma-green-dark leading-tight line-clamp-1 max-w-[500px]"
              :title="evento.nome"
            >
              {{ evento.nome }}
            </h2>
            <div class="flex items-center gap-3 mt-1">
              <span
                :class="[
                  'text-[10px] uppercase tracking-wider font-bold px-2.5 py-0.5 rounded-full inline-block',
                  statusClass(evento.status),
                ]"
              >
                {{ evento.status }}
              </span>
              <span class="text-xs font-medium text-grey-text/70 flex items-center gap-1">
                <span class="material-symbols-outlined !text-[14px] leading-none">calendar_today</span>
                {{ evento.dataInicio === evento.dataTermino ? evento.dataInicio : `${evento.dataInicio} até ${evento.dataTermino}` }}
              </span>
            </div>
          </div>
        </div>

        <div class="flex items-center gap-3 w-full sm:w-auto mt-4 sm:mt-0">
          <button
            type="button"
            @click="deleteEvento"
            class="flex-1 sm:flex-none bg-feedback-danger-bg hover:opacity-90 text-feedback-danger font-bold py-2 px-4 rounded-btn border border-feedback-danger-border transition-colors shadow-sm text-sm flex items-center justify-center gap-2 efeito-zoom cursor-pointer"
          >
            <span class="material-symbols-outlined text-[18px]">delete</span>
            Excluir
          </button>
          <button
            type="button"
            @click="editEvento"
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
              v-if="evento.bannerUrl"
              class="w-full h-52 rounded-btn overflow-hidden border border-amma-green-light shadow-sm mb-4"
            >
              <img
                :src="evento.bannerUrl"
                alt="Banner do Evento"
                class="w-full h-full object-cover"
              />
            </div>

            <div
              v-else
              class="w-20 h-20 bg-amma-green-lighter rounded-full flex items-center justify-center mb-4"
            >
              <span class="material-symbols-outlined text-4xl text-amma-green">event</span>
            </div>

            <h4
              class="text-base font-bold text-amma-green-dark mb-1 w-full truncate px-2"
              :title="evento.nome"
            >
              {{ evento.nome }}
            </h4>
            <span class="text-xs font-semibold text-grey-text/70 mb-4">
              {{ evento.localConcentracao }}
            </span>

            <div class="w-full bg-amma-green-lighter/60 rounded-btn p-4 border border-amma-green-light mt-auto text-left flex flex-col gap-2">
              <div>
                <span class="text-[11px] font-bold text-grey-text/60 uppercase block">Horário Previsto</span>
                <span class="text-sm font-bold text-amma-green-dark">
                  {{ evento.horarioInicio }} - {{ evento.horarioTermino }}
                </span>
              </div>
              <div v-if="evento.destinoFinal">
                <span class="text-[11px] font-bold text-grey-text/60 uppercase block">Destino de Chegada</span>
                <span class="text-xs font-semibold text-grey-text">
                  {{ evento.destinoFinal }}
                </span>
              </div>
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
              <span class="material-symbols-outlined text-amma-green">schedule</span>
              Programação e Itinerário
            </h3>

            <div class="grid grid-cols-1 sm:grid-cols-2 gap-y-6 gap-x-8">
              <div class="flex flex-col gap-1">
                <span class="text-xs font-bold text-grey-text/60 uppercase tracking-wide"
                  >Local de Concentração</span
                >
                <span class="text-sm font-semibold text-grey-text mt-1">
                  {{ evento.localConcentracao }}
                </span>
                <span v-if="evento.descricaoPontoPartida" class="text-xs text-grey-text/70">
                  {{ evento.descricaoPontoPartida }}
                </span>
              </div>

              <div class="flex flex-col gap-1">
                <span class="text-xs font-bold text-grey-text/60 uppercase tracking-wide"
                  >Ponto de Encerramento</span
                >
                <span class="text-sm font-semibold text-grey-text mt-1">
                  {{ evento.destinoFinal || 'Mesmo local da concentração' }}
                </span>
                <span v-if="evento.descricaoEncerramento" class="text-xs text-grey-text/70">
                  {{ evento.descricaoEncerramento }}
                </span>
              </div>

              <div class="flex flex-col gap-1">
                <span class="text-xs font-bold text-grey-text/60 uppercase tracking-wide"
                  >Data de Início</span
                >
                <span class="text-sm font-semibold text-grey-text mt-1">
                  {{ evento.dataInicio }} às {{ evento.horarioInicio }}
                </span>
              </div>

              <div class="flex flex-col gap-1">
                <span class="text-xs font-bold text-grey-text/60 uppercase tracking-wide"
                  >Data de Término</span
                >
                <span class="text-sm font-semibold text-grey-text mt-1">
                  {{ evento.dataTermino }} às {{ evento.horarioTermino }}
                </span>
              </div>

              <div class="flex flex-col gap-1 sm:col-span-2 mt-2">
                <span class="text-xs font-bold text-grey-text/60 uppercase tracking-wide"
                  >Descrição e Orientações</span
                >
                <p
                  class="text-sm font-medium text-grey-text mt-1 leading-relaxed border-l-2 border-amma-green pl-3"
                >
                  {{ evento.descricao }}
                </p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>