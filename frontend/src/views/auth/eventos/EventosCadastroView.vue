<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import AppBreadcrumb from '@/components/ui/AppBreadcrumb.vue'

const router = useRouter()

const form = ref({
  nome: '',
  descricao: '',
  imagem: null as File | null,
  dataInicio: '',
  dataTermino: '',
  local: '',
  destinoFinal: '',
  horarioInicio: '',
  descricaoHorarioInicio: '',
  horarioTermino: '',
  descricaoHorarioTermino: '',
})

const previewImagem = ref<string | null>(null)
const imageInput = ref<HTMLInputElement | null>(null)
const mostrarPopup = ref(false)

const breadcrumbItems = [
  { label: 'Eventos Ambientais', icon: 'event', routeName: 'eventos' },
  { label: 'Cadastrar Evento' },
]

const goBack = () => {
  router.push({ name: 'eventos' })
}

const triggerImageInput = () => {
  if (imageInput.value) {
    imageInput.value.click()
  }
}

const handleFileUpload = (event: Event) => {
  const target = event.target as HTMLInputElement
  if (!target || !target.files || target.files.length === 0) {
    form.value.imagem = null
    previewImagem.value = null
    return
  }

  const file = target.files[0]
  if (file && file.type.startsWith('image/')) {
    form.value.imagem = file
    previewImagem.value = URL.createObjectURL(file)
  } else {
    alert('Por favor, selecione apenas arquivos de imagem (PNG, JPG, WEBP).')
    target.value = ''
    form.value.imagem = null
    previewImagem.value = null
  }
}

const formularioVazio = () => ({
  nome: '',
  descricao: '',
  imagem: null as File | null,
  dataInicio: '',
  dataTermino: '',
  local: '',
  destinoFinal: '',
  horarioInicio: '',
  descricaoHorarioInicio: '',
  horarioTermino: '',
  descricaoHorarioTermino: '',
})

const submeterEvento = async () => {
  // 1. Criar um objeto FormData em vez de um objeto JSON
  const formData = new FormData()

  // 2. Adicionar os campos de texto usando os nomes EXATOS do EventosRequestDTO no Java
  formData.append('nome', form.value.nome)
  formData.append('descricao', form.value.descricao)
  formData.append('dataInicio', form.value.dataInicio)
  formData.append('dataTermino', form.value.dataTermino)
  formData.append('localConcentracao', form.value.local) // mapeado do Vue
  formData.append('destinoFinal', form.value.destinoFinal)
  formData.append('horarioInicio', form.value.horarioInicio)
  formData.append('horarioTermino', form.value.horarioTermino)
  formData.append('descricaoPontoPartida', form.value.descricaoPontoPartida) // mapeado
  formData.append('descricaoEncerramento', form.value.descricaoEncerramento) // mapeado

  // 3. Adicionar o ficheiro da imagem (o nome 'banner' tem de bater com o DTO do Java)
  if (form.value.imagem) {
    formData.append('banner', form.value.imagem)
  }

  try {
    // 4. Fazer a requisição POST
    const response = await fetch('http://localhost:8080/eventos', {
      method: 'POST',
      // IMPORTANTE: NÃO coloque 'Content-Type' aqui.
      // Ao usar FormData, o navegador gere automaticamente o boundary correto do multipart.
      body: formData,
    })

    if (response.ok) {
      mostrarPopup.value = true
      form.value = { ...formularioVazio() }
      previewImagem.value = null

      setTimeout(() => {
        mostrarPopup.value = false
        goBack()
      }, 1500)
    } else {
      const errorText = await response.text()
      alert('Falha ao cadastrar evento. Verifique os dados preenchidos.')
      console.error('Erro retornado pelo servidor:', errorText)
    }
  } catch (error) {
    alert('Erro de conexão. Certifique-se de que o back-end está a rodar na porta 8080.')
    console.error('Falha na requisição:', error)
  }
}
</script>

<template>
  <div class="flex flex-col w-full">
    <AppBreadcrumb :items="breadcrumbItems" />

    <div class="px-6 md:px-10 pb-6 pt-6 flex flex-col gap-6 w-full">
      <div class="flex items-center gap-4">
        <div
          class="w-12 h-12 flex items-center justify-center bg-amma-green-lighter/50 text-amma-green rounded-full shadow-sm"
        >
          <span class="material-symbols-outlined text-[24px]">calendar_add_on</span>
        </div>
        <div>
          <h2 class="text-2xl font-bold text-amma-green-dark leading-none">Cadastrar Evento</h2>
          <p class="text-sm text-grey-text mt-1">
            Preencha os dados e a programação do evento socioambiental.
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
              <label class="text-sm font-semibold text-grey-text pl-1">Banner do Evento *</label>
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
                    Alterar Imagem
                  </div>
                </template>

                <template v-else>
                  <span class="material-symbols-outlined text-3xl text-amma-green mb-1">image</span>
                  <p class="text-xs font-medium text-grey-text text-center">
                    Clique para selecionar o banner *<br />
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
              Salvar Evento
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
        <span>Novo evento cadastrado com sucesso!</span>
      </div>
    </Transition>
  </div>
</template>
