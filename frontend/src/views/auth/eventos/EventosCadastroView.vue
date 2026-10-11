<script setup lang="ts">
import { ref } from 'vue'


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
  descricaoHorarioTermino: ''
})

const previewImagem = ref<string | null>(null)

function handleFileUpload(event: Event) {
  const target = event.target as HTMLInputElement
  if (target.files && target.files[0]) {
    const file = target.files[0]
    form.value.imagem = file
    previewImagem.value = URL.createObjectURL(file)
  }
}

const mostrarPopup = ref(false)
const formularioVazio = {
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
  descricaoHorarioTermino: ''
}

function submeterEvento() {
  mostrarPopup.value = true
  form.value = { ...formularioVazio }
  previewImagem.value = null

  setTimeout(() => {
    mostrarPopup.value = false
  }, 4000)
}


</script>

<template>
  <div class="bg-white p-8 rounded-2xl shadow-sm max-w-5xl mx-auto text-gray-800">
    <h1 class="text-2xl font-bold text-[#2d4a22] mb-1">Cadastrar Eventos</h1>
    <p class="text-xs text-gray-500 mb-6">(*) indica campos obrigatórios</p>

    <form @submit.prevent="submeterEvento" class="space-y-6">
      <div>
        <label class="block text-sm font-semibold mb-2">Nome do Evento *</label>
        <input
          v-model="form.nome"
          type="text"
          placeholder="Ex: Caminhada até o Cedro para boas vivências com a natureza"
          class="w-full bg-[#f4f7f2] border border-[#d2e0cc] rounded-xl p-3 text-sm focus:outline-none focus:ring-2 focus:ring-[#5b8246]"
          required
        />
      </div>

      <div>
        <label class="block text-sm font-semibold mb-2">Descrição do Evento *</label>
        <textarea
          v-model="form.descricao"
          rows="3"
          placeholder="Escreva a descrição do evento..."
          class="w-full bg-[#f4f7f2] border border-[#d2e0cc] rounded-xl p-3 text-sm focus:outline-none focus:ring-2 focus:ring-[#5b8246]"
          required
        ></textarea>
      </div>

      <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
        <div>
          <label class="block text-sm font-semibold mb-2">Adicionar imagem *</label>
          <div class="border-2 border-dashed border-gray-300 bg-[#e8e8e8] rounded-xl p-6 text-center flex flex-col items-center justify-center min-h-[160px] relative">
            <template v-if="!previewImagem">
              <p class="text-xs text-gray-500 mb-3">Selecione a imagem</p>
              <label class="bg-[#6b8e53] hover:bg-[#567542] text-white px-6 py-1.5 rounded-lg text-xs cursor-pointer font-medium transition">
                upload
                <input type="file" accept="image/*" class="hidden" @change="handleFileUpload" />
              </label>
            </template>
            <template v-else>
              <img :src="previewImagem" class="max-h-32 rounded-lg object-cover mb-2" />
              <button type="button" @click="previewImagem = null" class="text-xs text-red-600 underline">
                Remover imagem
              </button>
            </template>
          </div>
        </div>

        <div class="space-y-4">
          <div>
            <label class="block text-sm font-semibold mb-2">Data de início *</label>
            <input
              v-model="form.dataInicio"
              type="date"
class="w-full bg-[#f4f7f2] border border-[#d2e0cc] text-gray-700 rounded-xl p-3 text-sm focus:outline-none focus:ring-2 focus:ring-[#5b8246] accent-[#6b8e53]"              required
            />
          </div>
          <div>
            <label class="block text-sm font-semibold mb-2">Data de término *</label>
            <input
              v-model="form.dataTermino"
              type="date"
class="w-full bg-[#f4f7f2] border border-[#d2e0cc] text-gray-700 rounded-xl p-3 text-sm focus:outline-none focus:ring-2 focus:ring-[#5b8246] accent-[#6b8e53]"              required
            />
          </div>
        </div>
      </div>

      <div class="border-t border-gray-200 pt-6 space-y-4">
        <h2 class="text-base font-bold text-gray-800 mb-4">Detalhes do Evento</h2>

        <div>
          <label class="block text-sm font-semibold mb-2">Defina o local *</label>
          <input
            v-model="form.local"
            type="text"
            placeholder="Ex: UFC Quixadá Av., José de Freitas Queiroz, 5003 - Cedro Novo"
            class="w-full bg-[#f4f7f2] border border-[#d2e0cc] rounded-xl p-3 text-sm focus:outline-none focus:ring-2 focus:ring-[#5b8246]"
            required
          />
        </div>

        <div>
          <label class="block text-sm font-semibold mb-2">Defina o destino final (opcional)</label>
          <input
            v-model="form.destinoFinal"
            type="text"
            placeholder="Ex: Açude do Cedro Estr. p/ Açude do Cedro - Quixadá"
            class="w-full bg-[#f4f7f2] border border-[#d2e0cc] rounded-xl p-3 text-sm focus:outline-none focus:ring-2 focus:ring-[#5b8246]"
          />
        </div>

        <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div>
            <label class="block text-sm font-semibold mb-2">Horário de início *</label>
            <input
              v-model="form.horarioInicio"
              type="time"
              class="w-full bg-[#f4f7f2] border border-[#d2e0cc] text-gray-700 rounded-xl p-3 text-sm focus:outline-none focus:ring-2 focus:ring-[#5b8246] accent-[#6b8e53]"
              required
            />
          </div>
          <div>
            <label class="block text-sm font-semibold mb-2">Descrição do horário de início *</label>
            <input
              v-model="form.descricaoHorarioInicio"
              type="text"
              placeholder="Ex: Saída da UFC até o Cedro"
              class="w-full bg-[#f4f7f2] border border-[#d2e0cc] rounded-xl p-3 text-sm focus:outline-none focus:ring-2 focus:ring-[#5b8246] accent-[#6b8e53]"
            />
          </div>

          <div>
            <label class="block text-sm font-semibold mb-2">Horário de término *</label>
            <input
              v-model="form.horarioTermino"
              type="time"
              class="w-full bg-[#f4f7f2] border border-[#d2e0cc] text-gray-700 rounded-xl p-3 text-sm focus:outline-none focus:ring-2 focus:ring-[#5b8246] accent-[#6b8e53]"
              required
            />
          </div>
          <div>
            <label class="block text-sm font-semibold mb-2">Descrição do horário de término *</label>
            <input
              v-model="form.descricaoHorarioTermino"
              type="text"
              placeholder="Ex: Saída do Cedro até a UFC"
              class="w-full bg-[#f4f7f2] border border-[#d2e0cc] rounded-xl p-3 text-sm focus:outline-none focus:ring-2 focus:ring-[#5b8246] accent-[#6b8e53]"
            />
          </div>
        </div>
      </div>

      <div class="flex justify-center pt-4">
        <button
          type="submit"
          class="bg-[#6b8e53] hover:bg-[#567542] text-white px-12 py-3 rounded-xl font-bold tracking-wide transition shadow-md"
        >
          ENVIAR
        </button>
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
        class="fixed top-6 left-1/2 -translate-x-1/2 z-50 bg-[#8cb158] text-white font-bold py-3 px-10 rounded-2xl shadow-xl border border-white/20 text-center text-lg flex items-center gap-3"
      >
        <span class="material-symbols-outlined text-2xl">check_circle</span>
        <span>Novo evento foi criado!</span>
      </div>
    </Transition>
    </form>
  </div>
</template>
