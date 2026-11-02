<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import AppBreadcrumb from '@/components/ui/AppBreadcrumb.vue'
import { useEventoStore } from '@/stores/eventoStore'
import { eventoService } from '@/services/eventoService'
import { useFeedbackStore } from '@/stores/feedbackStore'
import type { CreateEventoPayload, EventoStatus } from '@/types/eventos'

const router = useRouter()
const route = useRoute()
const eventoStore = useEventoStore()
const feedback = useFeedbackStore()

const eventoId = computed(() => route.params.id as string | undefined)
const isEditing = computed(() => !!eventoId.value)

const isSubmitting = ref(false)
const isLoadingData = ref(false)

const form = ref<CreateEventoPayload>({
  nome: '',
  descricao: '',
  imagem: null,
  dataInicio: '',
  dataTermino: '',
  local: '',
  destinoFinal: '',
  horarioInicio: '',
  descricaoHorarioInicio: '',
  horarioTermino: '',
  descricaoHorarioTermino: '',
})

const statusEvento = ref<EventoStatus>('PLANEJADO')
const statusOpcoes: EventoStatus[] = ['PLANEJADO', 'CONFIRMADO', 'ENCERRADO', 'CANCELADO']

const previewImagem = ref<string | null>(null)
const imageInput = ref<HTMLInputElement | null>(null)

const breadcrumbItems = computed(() => [
  { label: 'Eventos Ambientais', icon: 'event', routeName: 'eventos' },
  { label: isEditing.value ? 'Editar Evento' : 'Cadastrar Evento' },
])

const goBack = () => {
  router.push({ name: 'eventos' })
}

onMounted(async () => {
  if (!isEditing.value || !eventoId.value) return

  isLoadingData.value = true
  try {
    const data = await eventoService.getById(eventoId.value)
    form.value = {
      nome: data.nome || '',
      descricao: data.descricao || '',
      imagem: null,
      dataInicio: data.dataInicio || '',
      dataTermino: data.dataTermino || '',
      local: data.localConcentracao || '',
      destinoFinal: data.destinoFinal || '',
      horarioInicio: data.horarioInicio || '',
      descricaoHorarioInicio: data.descricaoPontoPartida || '',
      horarioTermino: data.horarioTermino || '',
      descricaoHorarioTermino: data.descricaoEncerramento || '',
    }

    statusEvento.value = data.status || 'PLANEJADO'
    previewImagem.value = data.bannerUrl || eventoService.getBannerUrl(eventoId.value)
  } catch (error) {
    feedback.showToast('Erro ao carregar dados do evento.', 'error')
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

const submeterEvento = async () => {
  if (!isEditing.value && !form.value.imagem) {
    feedback.showToast('Por favor, anexe uma imagem de banner para o evento.', 'warning')
    return
  }

  const actionTitle = isEditing.value ? 'Salvar Alterações' : 'Cadastrar Evento Socioambiental'
  const actionMessage = isEditing.value
    ? `Deseja salvar as alterações do evento "${form.value.nome}"?`
    : `Confirma o agendamento do evento "${form.value.nome}"?`

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
  if (isEditing.value && eventoId.value) {
    sucesso = await eventoStore.updateEvento(eventoId.value, {
      ...form.value,
      status: statusEvento.value,
    })
  } else {
    sucesso = await eventoStore.createEvento(form.value)
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
      <span>Carregando dados do evento...</span>
    </div>

    <div v-else class="px-6 md:px-10 pb-6 pt-6 flex flex-col gap-6 w-full">
      <div class="flex items-center gap-4">
        <div
          class="w-12 h-12 flex items-center justify-center bg-amma-green-lighter/50 text-amma-green rounded-full shadow-sm"
        >
          <span class="material-symbols-outlined text-[24px]">
            {{ isEditing ? 'edit_calendar' : 'calendar_add_on' }}
          </span>
        </div>
        <div>
          <h2 class="text-2xl font-bold text-amma-green-dark leading-none">
            {{ isEditing ? 'Editar Evento' : 'Cadastrar Evento' }}
          </h2>
          <p class="text-sm text-grey-text mt-1">
            {{
              isEditing
                ? 'Atualize a programação, localização ou banner do evento.'
                : 'Preencha os dados e a programação do evento socioambiental.'
            }}
          </p>
        </div>
      </div>

      <div class="bg-white rounded-box shadow-sm border border-amma-green-light p-8">
        <form @submit.prevent="submeterEvento" class="flex flex-col gap-6">
          <div class="flex flex-col gap-1.5">
            <label class="text-sm font-semibold text-grey-text pl-1">Nome do Evento *</label>
            <input
              v-model="form.nome"
              type="text"
              required
              placeholder="Ex: Caminhada ecológica no Cedro para preservação da Caatinga"
              class="w-full bg-white focus:bg-amma-green-lighter border-2 border-amma-green-light focus:border-amma-green rounded-btn py-3 px-4 text-grey-text outline-none transition-all text-sm font-medium"
            />
          </div>

          <div class="flex flex-col gap-1.5">
            <label class="text-sm font-semibold text-grey-text pl-1">Descrição do Evento *</label>
            <textarea
              v-model="form.descricao"
              rows="3"
              required
              placeholder="Descreva os objetivos, público-alvo e recomendações aos participantes..."
              class="w-full bg-white focus:bg-amma-green-lighter border-2 border-amma-green-light focus:border-amma-green rounded-btn py-3 px-4 text-grey-text outline-none transition-all text-sm font-medium resize-none"
            ></textarea>
          </div>

          <div class="grid grid-cols-1 md:grid-cols-2 gap-5">
            <div class="flex flex-col gap-1.5">
              <label class="text-sm font-semibold text-grey-text pl-1">
                Banner do Evento {{ isEditing ? '(Opcional para substituição)' : '*' }}
              </label>
              <div
                @click="triggerImageInput"
                class="relative w-full border-2 border-dashed border-amma-green-light hover:border-amma-green bg-amma-green-lighter/30 rounded-box p-4 flex flex-col items-center justify-center cursor-pointer transition-colors h-[170px] overflow-hidden"
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
                    alt="Pré-visualização do Banner"
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
                    Clique para selecionar o banner {{ isEditing ? '' : '*' }}<br />
                    <span class="text-[11px] text-grey-text/60 font-normal">PNG, JPG ou WEBP</span>
                  </p>
                </template>
              </div>
            </div>

            <div class="flex flex-col justify-between gap-4">
              <div class="flex flex-col gap-1.5">
                <label class="text-sm font-semibold text-grey-text pl-1">Data de Início *</label>
                <input
                  v-model="form.dataInicio"
                  type="date"
                  required
                  class="w-full bg-white focus:bg-amma-green-lighter border-2 border-amma-green-light focus:border-amma-green rounded-btn py-3 px-4 text-grey-text outline-none transition-all text-sm font-medium"
                />
              </div>

              <div class="flex flex-col gap-1.5">
                <label class="text-sm font-semibold text-grey-text pl-1">Data de Término *</label>
                <input
                  v-model="form.dataTermino"
                  type="date"
                  required
                  class="w-full bg-white focus:bg-amma-green-lighter border-2 border-amma-green-light focus:border-amma-green rounded-btn py-3 px-4 text-grey-text outline-none transition-all text-sm font-medium"
                />
              </div>
            </div>
          </div>

          <div class="border-t border-amma-green-light pt-6 flex flex-col gap-5">
            <h3 class="text-base font-bold text-amma-green-dark">Localização e Horários</h3>

            <div class="grid grid-cols-1 md:grid-cols-2 gap-5">
              <div class="flex flex-col gap-1.5">
                <label class="text-sm font-semibold text-grey-text pl-1"
                  >Local de Concentração *</label
                >
                <input
                  v-model="form.local"
                  type="text"
                  required
                  placeholder="Ex: UFC Campus Quixadá - Entrada Principal"
                  class="w-full bg-white focus:bg-amma-green-lighter border-2 border-amma-green-light focus:border-amma-green rounded-btn py-3 px-4 text-grey-text outline-none transition-all text-sm font-medium"
                />
              </div>

              <div class="flex flex-col gap-1.5">
                <label class="text-sm font-semibold text-grey-text pl-1"
                  >Destino Final (Opcional)</label
                >
                <input
                  v-model="form.destinoFinal"
                  type="text"
                  placeholder="Ex: Açude do Cedro"
                  class="w-full bg-white focus:bg-amma-green-lighter border-2 border-amma-green-light focus:border-amma-green rounded-btn py-3 px-4 text-grey-text outline-none transition-all text-sm font-medium"
                />
              </div>
            </div>

            <div class="grid grid-cols-1 md:grid-cols-2 gap-5">
              <div class="flex flex-col gap-1.5">
                <label class="text-sm font-semibold text-grey-text pl-1">Horário de Início *</label>
                <input
                  v-model="form.horarioInicio"
                  type="time"
                  required
                  class="w-full bg-white focus:bg-amma-green-lighter border-2 border-amma-green-light focus:border-amma-green rounded-btn py-3 px-4 text-grey-text outline-none transition-all text-sm font-medium"
                />
              </div>

              <div class="flex flex-col gap-1.5">
                <label class="text-sm font-semibold text-grey-text pl-1"
                  >Descrição do Ponto de Partida</label
                >
                <input
                  v-model="form.descricaoHorarioInicio"
                  type="text"
                  placeholder="Ex: Saída pontual dos participantes"
                  class="w-full bg-white focus:bg-amma-green-lighter border-2 border-amma-green-light focus:border-amma-green rounded-btn py-3 px-4 text-grey-text outline-none transition-all text-sm font-medium"
                />
              </div>
            </div>

            <div class="grid grid-cols-1 md:grid-cols-2 gap-5">
              <div class="flex flex-col gap-1.5">
                <label class="text-sm font-semibold text-grey-text pl-1"
                  >Horário de Término *</label
                >
                <input
                  v-model="form.horarioTermino"
                  type="time"
                  required
                  class="w-full bg-white focus:bg-amma-green-lighter border-2 border-amma-green-light focus:border-amma-green rounded-btn py-3 px-4 text-grey-text outline-none transition-all text-sm font-medium"
                />
              </div>

              <div class="flex flex-col gap-1.5">
                <label class="text-sm font-semibold text-grey-text pl-1"
                  >Descrição do Encerramento</label
                >
                <input
                  v-model="form.descricaoHorarioTermino"
                  type="text"
                  placeholder="Ex: Previsão de chegada e retorno"
                  class="w-full bg-white focus:bg-amma-green-lighter border-2 border-amma-green-light focus:border-amma-green rounded-btn py-3 px-4 text-grey-text outline-none transition-all text-sm font-medium"
                />
              </div>
            </div>

            <div v-if="isEditing" class="grid grid-cols-1 md:grid-cols-2 gap-5 pt-2">
              <div class="flex flex-col gap-1.5">
                <label class="text-sm font-semibold text-grey-text pl-1">Status do Evento</label>
                <select
                  v-model="statusEvento"
                  class="w-full bg-white focus:bg-amma-green-lighter border-2 border-amma-green-light focus:border-amma-green rounded-btn py-3 px-4 text-grey-text outline-none transition-all text-sm font-medium appearance-none"
                >
                  <option v-for="st in statusOpcoes" :key="st" :value="st">{{ st }}</option>
                </select>
              </div>
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
              {{ isSubmitting ? 'A salvar...' : isEditing ? 'Salvar Alterações' : 'Salvar Evento' }}
            </button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>