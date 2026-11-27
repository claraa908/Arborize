<script setup lang="ts">
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AppBreadcrumb from '@/components/ui/AppBreadcrumb.vue'
import { especieService } from '@/services/especieService'
import { buscarMeuPerfil, type UsuarioResponse } from '@/services/usuarioService'
import { useAssistentePlantioStore } from '@/stores/assistentePlantioStore'
import { useSolicitacaoMudaStore } from '@/stores/solicitacaoMudaStore'
import type { SpeciesRecommendation } from '@/types/assistentePlantio'

const route = useRoute()
const router = useRouter()
const quizStore = useAssistentePlantioStore()
const store = useSolicitacaoMudaStore()
const selectedSpecies = ref<SpeciesRecommendation | null>(null)
const perfilUsuario = ref<UsuarioResponse | null>(null)
const isLoadingSpecies = ref(false)
const isLoadingProfile = ref(false)
const speciesError = ref<string | null>(null)
const profileError = ref<string | null>(null)
const validationErrors = ref<Record<string, string>>({})
const formElement = ref<HTMLFormElement | null>(null)
const speciesImageFailed = ref(false)

const speciesId = computed(() => {
  const value = route.query.especieId
  return typeof value === 'string' ? Number(value) : NaN
})
const maxQuantity = 3
const quantityFromQuiz: Record<string, number> = { UMA: 1, DUAS: 2, TRES: 3 }

function aplicarIdentificacaoPerfil() {
  if (!perfilUsuario.value) return
  store.form.nomeSolicitante = perfilUsuario.value.nome
  store.form.cpf = perfilUsuario.value.cpf
  store.form.telefone = perfilUsuario.value.whatsapp
}

async function carregarPerfil() {
  isLoadingProfile.value = true
  profileError.value = null
  try {
    perfilUsuario.value = await buscarMeuPerfil()
    aplicarIdentificacaoPerfil()
  } catch {
    profileError.value = 'Não foi possível carregar os seus dados cadastrais.'
  } finally {
    isLoadingProfile.value = false
  }
}

watch(speciesId, () => {
  speciesImageFailed.value = false
  void loadSpecies()
}, { immediate: true })

onMounted(() => {
  void carregarPerfil()
})

async function loadSpecies() {
  speciesError.value = null
  if (!Number.isSafeInteger(speciesId.value) || speciesId.value <= 0) {
    speciesError.value = 'Escolha uma espécie para continuar.'
    return
  }

  const selected = quizStore.especieSelecionada
  validationErrors.value = { ...store.serverErrors }
  if (selected?.id === speciesId.value) {
    selectedSpecies.value = selected
    const isNewSelection = store.form.especieId !== speciesId.value
    store.preencherEspecie(speciesId.value)
    aplicarIdentificacaoPerfil()
    if (isNewSelection) {
      const quizQuantity = quizStore.respostas.quantidade_mudas
      store.form.quantidadeMudas = quizQuantity ? quantityFromQuiz[quizQuantity] ?? 1 : 1
    }
    return
  }

  isLoadingSpecies.value = true
  try {
    const catalog = await especieService.getAll()
    const especie = catalog.find((item) => Number(item.id) === speciesId.value)
    if (!especie?.id) {
      speciesError.value = 'Não encontramos a espécie escolhida. Volte ao assistente para selecionar outra muda.'
      return
    }
    selectedSpecies.value = {
      id: Number(especie.id),
      nomePopular: especie.nomePopular,
      nomeCientifico: especie.nomeCientifico,
      porte: especie.porteArvore,
      fotoUrl: especie.fotoUrl
    }
    const isNewSelection = store.form.especieId !== speciesId.value
    store.preencherEspecie(speciesId.value)
    aplicarIdentificacaoPerfil()
    if (isNewSelection) {
      const quizQuantity = quizStore.respostas.quantidade_mudas
      store.form.quantidadeMudas = quizQuantity ? quantityFromQuiz[quizQuantity] ?? 1 : 1
    }
  } catch {
    speciesError.value = 'Não foi possível carregar a espécie escolhida. Tente novamente.'
  } finally {
    isLoadingSpecies.value = false
  }
}

function voltarAoQuiz() {
  quizStore.irParaPergunta(0)
  void router.push({ name: 'arborizacao-assistente' })
}

function validateForm() {
  aplicarIdentificacaoPerfil()
  const values = store.form
  const errors: Record<string, string> = {}
  const required: Array<[keyof typeof values, string]> = [
    ['logradouro', 'Preencha este campo.'],
    ['numeroEndereco', 'Preencha este campo.'],
    ['bairro', 'Preencha este campo.'],
    ['pontoReferencia', 'Preencha este campo.']
  ]
  for (const [field, message] of required) {
    if (!String(values[field]).trim()) errors[field] = message
  }
  validationErrors.value = errors
  if (Object.keys(errors).length) {
    void nextTick(() => {
      const firstInvalid = formElement.value?.querySelector<HTMLElement>('[aria-invalid="true"]')
      firstInvalid?.focus()
    })
    return false
  }
  for (const key of ['logradouro', 'numeroEndereco', 'bairro', 'complemento', 'pontoReferencia'] as const) {
    values[key] = values[key].trim().replace(/\s+/g, ' ')
  }
  return true
}

async function continuar() {
  if (!selectedSpecies.value) return
  if (!perfilUsuario.value && (!store.form.nomeSolicitante.trim() || !store.form.cpf.trim() || !store.form.telefone.trim())) {
    await carregarPerfil()
    if (!perfilUsuario.value) return
  }
  if (!validateForm()) return
  store.form.especieId = speciesId.value
  const local = quizStore.especieSelecionada?.id === speciesId.value ? quizStore.respostas.local_plantio : undefined
  store.form.localPlantio = local === 'CALCADA' || local === 'QUINTAL' ? local : undefined
  if (await store.calcularConversao()) {
    await router.push({ name: 'arborizacao-conversao', query: { especieId: speciesId.value } })
  }
}
</script>

<template>
  <div class="flex w-full flex-col">
    <AppBreadcrumb class="-mx-6 -mt-2 mb-2 md:-mx-10" :items="[{ label: 'Arborização', icon: 'park', routeName: 'arborizacao' }, { label: 'Espécies recomendadas', routeName: 'arborizacao-recomendacoes' }, { label: 'Solicitar muda' }]" />
    <section class="mx-auto w-full max-w-4xl py-8 md:py-12" aria-labelledby="request-title">
      <header class="mb-7">
        <p class="text-xs font-bold uppercase tracking-wider text-amma-green">Solicitação</p>
        <h1 id="request-title" class="mt-2 text-2xl font-bold text-amma-green-dark">Solicitar muda</h1>
        <p class="mt-2 text-sm text-grey-text">Confirme a espécie escolhida, informe a quantidade de mudas e o endereço onde será realizado o plantio.</p>
      </header>

      <div v-if="speciesError" class="rounded-box border border-feedback-info-border bg-feedback-info-bg p-5 text-feedback-info" role="status">
        <p>{{ speciesError }}</p>
        <div class="mt-4 flex flex-wrap gap-3">
          <button v-if="speciesId > 0" type="button" class="rounded-btn border-2 border-amma-green bg-white px-5 py-2.5 font-bold text-amma-green focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-amma-green" @click="loadSpecies">Tentar novamente</button>
          <button type="button" class="rounded-btn bg-amma-green px-5 py-2.5 font-bold text-white focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-amma-green" @click="router.push({ name: 'arborizacao-assistente' })">Voltar ao assistente</button>
        </div>
      </div>

      <div v-else-if="isLoadingSpecies || isLoadingProfile" class="rounded-box bg-white p-8 text-center text-grey-text" role="status" aria-live="polite">
        Carregando dados da solicitação…
      </div>

      <div v-else-if="profileError" class="rounded-box border border-feedback-danger-border bg-feedback-danger-bg p-5 text-sm text-feedback-danger" role="alert">
        <p>{{ profileError }}</p>
        <button type="button" class="mt-3 rounded-btn border-2 border-amma-green bg-white px-4 py-2 font-bold text-amma-green" @click="carregarPerfil">Tentar novamente</button>
      </div>

      <form v-else-if="selectedSpecies" ref="formElement" class="flex flex-col gap-6" novalidate @submit.prevent="continuar">
        <section class="rounded-box border border-amma-green-light bg-white p-5 shadow-sm md:p-7" aria-labelledby="request-card-title">
          <h2 id="request-card-title" class="text-lg font-bold text-amma-green-dark">Detalhes da solicitação</h2>
          <div class="mt-4 grid grid-cols-1 gap-5 md:grid-cols-[minmax(0,1.25fr)_minmax(0,1fr)] md:items-center">
            <div class="flex items-center gap-4 rounded-btn border border-amma-green-light bg-amma-green-lighter p-4">
              <img v-if="!speciesImageFailed" :src="selectedSpecies.fotoUrl?.trim() || selectedSpecies.imagemUrl?.trim() || selectedSpecies.urlFoto?.trim() || especieService.getFotoUrl(selectedSpecies.id)" :alt="`Foto de ${selectedSpecies.nomePopular}`" class="h-16 w-16 shrink-0 rounded-btn object-cover" @error="speciesImageFailed = true" />
              <div v-else class="flex h-16 w-16 shrink-0 items-center justify-center rounded-btn bg-white text-amma-green" aria-label="Foto indisponível"><span class="material-symbols-outlined text-3xl" aria-hidden="true">park</span></div>
              <div class="min-w-0">
                <p class="font-bold text-amma-green-dark">{{ selectedSpecies.nomePopular }}</p>
                <p class="text-sm italic text-grey-text">{{ selectedSpecies.nomeCientifico }}</p>
                <p class="text-xs text-grey-text">Porte {{ selectedSpecies.porte }}</p>
              </div>
            </div>

            <div class="rounded-btn border border-amma-green-light/70 bg-amma-bg p-4">
              <span class="mb-2 block text-sm font-semibold text-grey-text">Quantidade de mudas <span aria-hidden="true">*</span></span>
              <div class="flex items-center gap-4">
                <button type="button" aria-label="Diminuir quantidade" class="flex h-11 w-11 items-center justify-center rounded-full border-2 border-amma-green bg-white text-amma-green focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-amma-green disabled:opacity-50" :disabled="store.form.quantidadeMudas <= 1" @click="store.form.quantidadeMudas = Math.max(1, store.form.quantidadeMudas - 1)">
                  <span aria-hidden="true" class="text-xl">−</span>
                </button>
                <output class="min-w-10 text-center text-xl font-bold text-amma-green-dark" aria-live="polite">{{ store.form.quantidadeMudas }}</output>
                <button type="button" aria-label="Aumentar quantidade" class="flex h-11 w-11 items-center justify-center rounded-full border-2 border-amma-green bg-white text-amma-green focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-amma-green disabled:opacity-50" :disabled="store.form.quantidadeMudas >= maxQuantity" @click="store.form.quantidadeMudas = Math.min(maxQuantity, store.form.quantidadeMudas + 1)">
                  <span aria-hidden="true" class="text-xl">+</span>
                </button>
              </div>
              <p class="mt-2 text-xs text-grey-text">Máximo de {{ maxQuantity }} mudas por solicitação.</p>
              <p v-if="validationErrors.quantidadeMudas" class="mt-1 text-sm text-feedback-danger">{{ validationErrors.quantidadeMudas }}</p>
            </div>
          </div>

          <div class="mt-6 border-t border-amma-green-light pt-6">
            <h3 class="mb-4 text-base font-bold text-amma-green-dark">Endereço do local de plantio</h3>
            <div class="grid grid-cols-1 gap-4 sm:grid-cols-2">
              <div class="sm:col-span-2">
                <label for="logradouro" class="text-sm font-semibold text-grey-text">Logradouro <span aria-hidden="true">*</span></label>
                <input id="logradouro" v-model="store.form.logradouro" autocomplete="street-address" required :aria-invalid="!!validationErrors.logradouro" :aria-describedby="validationErrors.logradouro ? 'logradouro-error' : undefined" class="mt-1 w-full rounded-btn border-2 border-amma-green-light bg-white px-4 py-3 text-sm text-grey-text outline-none transition-all focus:border-amma-green focus:bg-amma-green-lighter focus-visible:ring-2 focus-visible:ring-amma-green" />
                <p v-if="validationErrors.logradouro" id="logradouro-error" class="mt-1 text-sm text-feedback-danger">{{ validationErrors.logradouro }}</p>
              </div>
              <div>
                <label for="numeroEndereco" class="text-sm font-semibold text-grey-text">Número <span aria-hidden="true">*</span></label>
                <input id="numeroEndereco" v-model="store.form.numeroEndereco" required :aria-invalid="!!validationErrors.numeroEndereco" :aria-describedby="validationErrors.numeroEndereco ? 'numero-error' : undefined" class="mt-1 w-full rounded-btn border-2 border-amma-green-light bg-white px-4 py-3 text-sm text-grey-text outline-none transition-all focus:border-amma-green focus:bg-amma-green-lighter focus-visible:ring-2 focus-visible:ring-amma-green" />
                <p v-if="validationErrors.numeroEndereco" id="numero-error" class="mt-1 text-sm text-feedback-danger">{{ validationErrors.numeroEndereco }}</p>
              </div>
              <div>
                <label for="bairro" class="text-sm font-semibold text-grey-text">Bairro (Quixadá) <span aria-hidden="true">*</span></label>
                <input id="bairro" v-model="store.form.bairro" autocomplete="address-level3" required :aria-invalid="!!validationErrors.bairro" :aria-describedby="validationErrors.bairro ? 'bairro-error' : undefined" class="mt-1 w-full rounded-btn border-2 border-amma-green-light bg-white px-4 py-3 text-sm text-grey-text outline-none transition-all focus:border-amma-green focus:bg-amma-green-lighter focus-visible:ring-2 focus-visible:ring-amma-green" />
                <p v-if="validationErrors.bairro" id="bairro-error" class="mt-1 text-sm text-feedback-danger">{{ validationErrors.bairro }}</p>
              </div>
              <div class="sm:col-span-2">
                <label for="pontoReferencia" class="text-sm font-semibold text-grey-text">Ponto de Referência <span aria-hidden="true">*</span></label>
                <input id="pontoReferencia" v-model="store.form.pontoReferencia" required :aria-invalid="!!validationErrors.pontoReferencia" :aria-describedby="validationErrors.pontoReferencia ? 'ponto-error' : undefined" class="mt-1 w-full rounded-btn border-2 border-amma-green-light bg-white px-4 py-3 text-sm text-grey-text outline-none transition-all focus:border-amma-green focus:bg-amma-green-lighter focus-visible:ring-2 focus-visible:ring-amma-green" />
                <p v-if="validationErrors.pontoReferencia" id="ponto-error" class="mt-1 text-sm text-feedback-danger">{{ validationErrors.pontoReferencia }}</p>
              </div>
              <div class="sm:col-span-2">
                <label for="complemento" class="text-sm font-semibold text-grey-text">Complemento (opcional)</label>
                <input id="complemento" v-model="store.form.complemento" class="mt-1 w-full rounded-btn border-2 border-amma-green-light bg-white px-4 py-3 text-sm text-grey-text outline-none transition-all focus:border-amma-green focus:bg-amma-green-lighter focus-visible:ring-2 focus-visible:ring-amma-green" />
              </div>
            </div>
          </div>
        </section>

        <div class="flex flex-col-reverse gap-3 border-t border-amma-green-light pt-5 sm:flex-row sm:items-center sm:justify-between">
          <button type="button" class="rounded-btn border-2 border-amma-green bg-white px-5 py-2.5 font-bold text-amma-green hover:bg-amma-green-lighter focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-amma-green" @click="voltarAoQuiz">Voltar ao Quiz</button>
          <button type="submit" :disabled="store.isCalculating" class="efeito-zoom rounded-btn bg-amma-green px-6 py-2.5 font-bold text-white hover:bg-amma-green-dark focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-amma-green disabled:cursor-not-allowed disabled:opacity-60">
            {{ store.isCalculating ? 'Calculando conversão…' : 'Continuar' }}
          </button>
        </div>
        <p v-if="store.conversionError" class="rounded-btn border border-feedback-danger-border bg-feedback-danger-bg p-4 text-sm text-feedback-danger" role="alert">
          {{ store.conversionError }}
          <button type="button" class="ml-2 font-bold underline" @click="continuar">Tentar novamente</button>
        </p>
      </form>
    </section>
  </div>
</template>
