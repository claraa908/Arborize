<script setup lang="ts">
import { ref } from 'vue'

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

function handleFileUpload(event: Event) {
  const target = event.target as HTMLInputElement
  if (target.files && target.files[0]) {
    const file = target.files[0]
    formEspecie.value.imagem = file
    previewImagem.value = URL.createObjectURL(file)
  }
}

const mostrarPopup = ref(false)
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

function submeterEspecie() {
  mostrarPopup.value = true
  formEspecie.value = { ...formVazio() }
  previewImagem.value = null

  setTimeout(() => {
    mostrarPopup.value = false
  }, 2000)
}
</script>

<template>
  <div class="bg-white p-8 rounded-2xl shadow-sm max-w-5xl mx-auto text-gray-800">
    <h1 class="text-2xl font-bold text-[#2d4a22] mb-1">Cadastrar Espécie Botânica</h1>
    <p class="text-xs text-gray-500 mb-6">(*) indica campos obrigatórios</p>

      <form @submit.prevent="submeterEspecie" class="space-y-6">
        <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
          <div>
            <label class="block text-sm font-semibold mb-2">Nome Popular *</label>
            <input
              v-model="formEspecie.nomePopular"
              type="text"
              placeholder="Ex: Ipê-amarelo, Aroeira-do-sertão, Mulungu"
              class="w-full bg-[#f4f7f2] border border-[#d2e0cc] rounded-xl p-3 text-sm focus:outline-none focus:ring-2 focus:ring-[#5b8246]"
              required
            />
          </div>

          <div>
            <label class="block text-sm font-semibold mb-2">Nome Científico *</label>
            <input
              v-model="formEspecie.nomeCientifico"
              type="text"
              placeholder="Ex: Handroanthus chrysotrichus"
              class="w-full bg-[#f4f7f2] border border-[#d2e0cc] rounded-xl p-3 text-sm italic focus:outline-none focus:ring-2 focus:ring-[#5b8246]"
              required
            />
          </div>
        </div>

        <div class="grid grid-cols-1 md:grid-cols-3 gap-6">
          <div>
            <label class="block text-sm font-semibold mb-2">Família Botânica</label>
            <input
              v-model="formEspecie.familiaBotanica"
              type="text"
              placeholder="Ex: Bignoniaceae"
              class="w-full bg-[#f4f7f2] border border-[#d2e0cc] rounded-xl p-3 text-sm focus:outline-none focus:ring-2 focus:ring-[#5b8246]"
            />
          </div>

          <div>
            <label class="block text-sm font-semibold mb-2">Categoria *</label>
            <select
              v-model="formEspecie.categoria"
              class="w-full bg-[#f4f7f2] border border-[#d2e0cc] text-gray-700 rounded-xl p-3 text-sm focus:outline-none focus:ring-2 focus:ring-[#5b8246]"
              required
            >
              <option value="Nativa da Caatinga">Nativa da Caatinga</option>
              <option value="Frutífera">Frutífera</option>
              <option value="Ornamental">Ornamental</option>
              <option value="Medicinal">Medicinal</option>
              <option value="Exótica">Exótica</option>
            </select>
          </div>

          <div>
            <label class="block text-sm font-semibold mb-2">Porte da Árvore *</label>
            <select
              v-model="formEspecie.porte"
              class="w-full bg-[#f4f7f2] border border-[#d2e0cc] text-gray-700 rounded-xl p-3 text-sm focus:outline-none focus:ring-2 focus:ring-[#5b8246]"
              required
            >
              <option value="Pequeno">Pequeno (Até 4m)</option>
              <option value="Médio">Médio (4m a 8m)</option>
              <option value="Grande">Grande (Acima de 8m)</option>
            </select>
          </div>
        </div>

        <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
          <div>
            <label class="block text-sm font-semibold mb-2">Imagem da Espécie *</label>
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
                <button type="button" @click="previewImagem = null" class="text-xs text-red-600 underline cursor-pointer">
                  Remover imagem
                </button>
              </template>
            </div>
          </div>

          <div class="space-y-4">
            <div>
              <label class="block text-sm font-semibold mb-2">Recomendada para Calçadas? *</label>
              <div class="flex gap-6 pt-2">
                <label class="flex items-center gap-2 cursor-pointer text-sm">
                  <input type="radio" v-model="formEspecie.adequadoCalcada" value="Sim" class="accent-[#6b8e53]" />
                  Sim (Raízes profundas/não agressivas)
                </label>
                <label class="flex items-center gap-2 cursor-pointer text-sm">
                  <input type="radio" v-model="formEspecie.adequadoCalcada" value="Não" class="accent-[#6b8e53]" />
                  Não
                </label>
              </div>
            </div>

            <div>
              <label class="block text-sm font-semibold mb-2">Tempo médio de germinação (em dias)</label>
              <input
                v-model="formEspecie.tempoGerminacaoDias"
                type="number"
                placeholder="Ex: 15"
                class="w-full bg-[#f4f7f2] border border-[#d2e0cc] rounded-xl p-3 text-sm focus:outline-none focus:ring-2 focus:ring-[#5b8246]"
              />
            </div>
          </div>
        </div>

        <div>
          <label class="block text-sm font-semibold mb-2">Descrição / Orientações de Cultivo</label>
          <textarea
            v-model="formEspecie.descricao"
            rows="3"
            placeholder="Descreva as características da espécie, época de florada, recomendações de solo e rega..."
            class="w-full bg-[#f4f7f2] border border-[#d2e0cc] rounded-xl p-3 text-sm focus:outline-none focus:ring-2 focus:ring-[#5b8246]"
          ></textarea>
        </div>

        <div class="flex justify-center pt-4">
          <button
            type="submit"
            class="bg-[#6b8e53] hover:bg-[#567542] text-white px-12 py-3 rounded-xl font-bold tracking-wide transition shadow-md cursor-pointer"
          >
            SALVAR ESPÉCIE
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
        <span>Espécie cadastrada com sucesso!</span>
      </div>
    </Transition>
    </form>
  </div>
</template>
