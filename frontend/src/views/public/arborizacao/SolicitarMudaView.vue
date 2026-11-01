<script setup lang="ts">
import { computed, nextTick, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { especieService } from '@/services/especieService'
import { useAssistentePlantioStore } from '@/stores/assistentePlantioStore'
import { useSolicitacaoMudaStore } from '@/stores/solicitacaoMudaStore'
import type { SpeciesRecommendation } from '@/types/assistentePlantio'

const route = useRoute()
const router = useRouter()
const quizStore = useAssistentePlantioStore()
const store = useSolicitacaoMudaStore()
const selectedSpecies = ref<SpeciesRecommendation | null>(null)
const isLoadingSpecies = ref(false)
const speciesError = ref<string | null>(null)
const validationErrors = ref<Record<string, string>>({})
const formElement = ref<HTMLFormElement | null>(null)
const speciesImageFailed = ref(false)

const speciesId = computed(() => {
  const value = route.query.especieId
  return typeof value === 'string' ? Number(value) : NaN
})
const maxQuantity = 3
const quantityFromQuiz: Record<string, number> = { UMA: 1, DUAS: 2, TRES: 3 }

watch(speciesId, () => {
  speciesImageFailed.value = false
  void loadSpecies()
}, { immediate: true })

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

function formatCpf(value: string) {
  const digits = value.replace(/\D/g, '').slice(0, 11)
  return digits.replace(/^(\d{3})(\d)/, '$1.$2').replace(/^(\d{3}\.\d{3})(\d)/, '$1.$2').replace(/(\d{3})(\d{1,2})$/, '$1-$2')
}

function formatPhone(value: string) {
  const digits = value.replace(/\D/g, '').slice(0, 11)
  if (digits.length <= 2) return digits ? `(${digits}` : ''
  const area = digits.slice(0, 2)
  const phone = digits.slice(2)
  if (digits.length <= 10) return `(${area}) ${phone.slice(0, 4)}${phone.length > 4 ? `-${phone.slice(4)}` : ''}`
  return `(${area}) ${phone.slice(0, 5)}${phone.length > 5 ? `-${phone.slice(5)}` : ''}`
}

function isValidCpf(value: string) {
  const digits = value.replace(/\D/g, '')
  if (digits.length !== 11 || /^([0-9])\1{10}$/.test(digits)) return false
  const calculate = (length: number) => {
    const sum = digits.slice(0, length).split('').reduce((total, digit, index) => total + Number(digit) * (length + 1 - index), 0)
    const remainder = (sum * 10) % 11
    return remainder === 10 ? 0 : remainder
  }
  return calculate(9) === Number(digits[9]) && calculate(10) === Number(digits[10])
}

function validateForm() {
  const values = store.form
  const errors: Record<string, string> = {}
  const required: Array<[keyof typeof values, string]> = [
    ['nomeSolicitante', 'Preencha este campo.'],
    ['logradouro', 'Preencha este campo.'],
    ['numeroEndereco', 'Preencha este campo.'],
    ['bairro', 'Preencha este campo.'],
    ['pontoReferencia', 'Preencha este campo.']
  ]
  for (const [field, message] of required) {
    if (!String(values[field]).trim()) errors[field] = message
  }
  if (values.nomeSolicitante.trim() && values.nomeSolicitante.trim().split(/\s+/).length < 2) {
    errors.nomeSolicitante = 'Informe seu nome completo.'
  }
  if (!values.cpf || !isValidCpf(values.cpf)) errors.cpf = 'Informe um CPF válido.'
  const phoneDigits = values.telefone.replace(/\D/g, '')
  const ddd = Number(phoneDigits.slice(0, 2))
  if (![10, 11].includes(phoneDigits.length) || ddd < 11 || ddd > 99) {
    errors.telefone = 'Informe um telefone com DDD.'
  }
  validationErrors.value = errors
  if (Object.keys(errors).length) {
    void nextTick(() => {
      const firstInvalid = formElement.value?.querySelector<HTMLElement>('[aria-invalid="true"]')
      firstInvalid?.focus()
    })
    return false
  }
  for (const key of ['nomeSolicitante', 'logradouro', 'numeroEndereco', 'bairro', 'complemento', 'pontoReferencia'] as const) {
    values[key] = values[key].trim().replace(/\s+/g, ' ')
  }
  return true
}

async function continuar() {
  if (!selectedSpecies.value || !validateForm()) return
  store.form.especieId = speciesId.value
  if (await store.calcularConversao()) {
    await router.push({ name: 'arborizacao-conversao', query: { especieId: speciesId.value } })
  }
}
</script>

<template>
  <section class="mx-auto max-w-6xl py-8 md:py-12" aria-labelledby="request-title">
    <header class="mb-7">
      <p class="text-xs font-bold uppercase tracking-wider text-amma-green">Solicitação</p>
      <h1 id="request-title" class="mt-2 text-2xl font-bold text-amma-green-dark">Solicitar muda</h1>
      <p class="mt-2 text-sm text-grey-text">Confirme a espécie, informe a quantidade e o endereço de destino e preencha seus dados de identificação.</p>
    </header>

    <div v-if="speciesError" class="rounded-box border border-feedback-info-border bg-feedback-info-bg p-5 text-feedback-info" role="status">
      <p>{{ speciesError }}</p>
      <div class="mt-4 flex flex-wrap gap-3">
        <button v-if="speciesId > 0" type="button" class="rounded-btn border-2 border-amma-green bg-white px-5 py-2.5 font-bold text-amma-green focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-amma-green" @click="loadSpecies">Tentar novamente</button>
        <button type="button" class="rounded-btn bg-amma-green px-5 py-2.5 font-bold text-white focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-amma-green" @click="router.push({ name: 'arborizacao-assistente' })">Voltar ao assistente</button>
      </div>
    </div>

    <div v-else-if="isLoadingSpecies" class="rounded-box bg-white p-8 text-center text-grey-text" role="status" aria-live="polite">
      Carregando a espécie escolhida…
    </div>

    <form v-else-if="selectedSpecies" ref="formElement" class="grid grid-cols-1 gap-5 lg:grid-cols-2" novalidate @submit.prevent="continuar">
      <section class="rounded-box border border-amma-green-light bg-white p-5 shadow-sm md:p-7" aria-labelledby="request-card-title">
        <h2 id="request-card-title" class="text-lg font-bold text-amma-green-dark">Solicitação</h2>
        <div class="mt-4 flex items-center gap-4 rounded-btn border border-amma-green-light bg-amma-green-lighter p-4">
          <img v-if="!speciesImageFailed" :src="selectedSpecies.fotoUrl?.trim() || selectedSpecies.imagemUrl?.trim() || selectedSpecies.urlFoto?.trim() || especieService.getFotoUrl(selectedSpecies.id)" :alt="`Foto de ${selectedSpecies.nomePopular}`" class="h-16 w-16 rounded-btn object-cover" @error="speciesImageFailed = true" />
          <div v-else class="flex h-16 w-16 items-center justify-center rounded-btn bg-white text-amma-green" aria-label="Foto indisponível"><span class="material-symbols-outlined text-3xl" aria-hidden="true">park</span></div>
          <div class="min-w-0">
            <p class="font-bold text-amma-green-dark">{{ selectedSpecies.nomePopular }}</p>
            <p class="text-sm italic text-grey-text">{{ selectedSpecies.nomeCientifico }}</p>
            <p class="text-xs text-grey-text">Porte {{ selectedSpecies.porte }}</p>
          </div>
        </div>

        <div class="mt-5">
          <span class="mb-2 block text-sm font-semibold text-grey-text">Quantidade de mudas <span aria-hidden="true">*</span></span>
          <div class="flex items-center gap-4">
            <button type="button" aria-label="Diminuir quantidade" class="flex h-11 w-11 items-center justify-center rounded-full border-2 border-amma-green text-amma-green focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-amma-green disabled:opacity-50" :disabled="store.form.quantidadeMudas <= 1" @click="store.form.quantidadeMudas = Math.max(1, store.form.quantidadeMudas - 1)">
              <span aria-hidden="true" class="text-xl">−</span>
            </button>
            <output class="min-w-10 text-center text-xl font-bold text-amma-green-dark" aria-live="polite">{{ store.form.quantidadeMudas }}</output>
            <button type="button" aria-label="Aumentar quantidade" class="flex h-11 w-11 items-center justify-center rounded-full border-2 border-amma-green text-amma-green focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-amma-green disabled:opacity-50" :disabled="store.form.quantidadeMudas >= maxQuantity" @click="store.form.quantidadeMudas = Math.min(maxQuantity, store.form.quantidadeMudas + 1)">
              <span aria-hidden="true" class="text-xl">+</span>
            </button>
            <span class="text-xs text-grey-text">Máximo de {{ maxQuantity }} mudas por solicitação.</span>
          </div>
          <p v-if="validationErrors.quantidadeMudas" class="mt-1 text-sm text-feedback-danger">{{ validationErrors.quantidadeMudas }}</p>
        </div>

        <div class="mt-5 grid grid-cols-1 gap-4 sm:grid-cols-2">
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
      </section>

      <section class="rounded-box border border-amma-green-light bg-white p-5 shadow-sm md:p-7" aria-labelledby="identity-title">
        <h2 id="identity-title" class="text-lg font-bold text-amma-green-dark">Identificação</h2>
        <div class="mt-5 flex flex-col gap-4">
          <div>
            <label for="nomeSolicitante" class="text-sm font-semibold text-grey-text">Nome Completo <span aria-hidden="true">*</span></label>
            <input id="nomeSolicitante" v-model="store.form.nomeSolicitante" autocomplete="name" required :aria-invalid="!!validationErrors.nomeSolicitante" :aria-describedby="validationErrors.nomeSolicitante ? 'nome-error' : undefined" class="mt-1 w-full rounded-btn border-2 border-amma-green-light bg-white px-4 py-3 text-sm text-grey-text outline-none transition-all focus:border-amma-green focus:bg-amma-green-lighter focus-visible:ring-2 focus-visible:ring-amma-green" />
            <p v-if="validationErrors.nomeSolicitante" id="nome-error" class="mt-1 text-sm text-feedback-danger">{{ validationErrors.nomeSolicitante }}</p>
          </div>
          <div>
            <label for="cpf" class="text-sm font-semibold text-grey-text">CPF <span aria-hidden="true">*</span></label>
            <input id="cpf" :value="store.form.cpf" inputmode="numeric" autocomplete="off" maxlength="14" required :aria-describedby="validationErrors.cpf ? 'cpf-help cpf-error' : 'cpf-help'" :aria-invalid="!!validationErrors.cpf" @input="store.form.cpf = formatCpf(($event.target as HTMLInputElement).value)" class="mt-1 w-full rounded-btn border-2 border-amma-green-light bg-white px-4 py-3 text-sm text-grey-text outline-none transition-all focus:border-amma-green focus:bg-amma-green-lighter focus-visible:ring-2 focus-visible:ring-amma-green" />
            <p id="cpf-help" class="mt-1 text-xs text-grey-text">Usado para identificar a solicitação. Não será exibido no comprovante.</p>
            <p v-if="validationErrors.cpf" id="cpf-error" class="mt-1 text-sm text-feedback-danger">{{ validationErrors.cpf }}</p>
          </div>
          <div>
            <label for="telefone" class="text-sm font-semibold text-grey-text">Telefone de contato <span aria-hidden="true">*</span></label>
            <input id="telefone" :value="store.form.telefone" inputmode="tel" autocomplete="tel" maxlength="15" required :aria-invalid="!!validationErrors.telefone" :aria-describedby="validationErrors.telefone ? 'telefone-error' : undefined" @input="store.form.telefone = formatPhone(($event.target as HTMLInputElement).value)" class="mt-1 w-full rounded-btn border-2 border-amma-green-light bg-white px-4 py-3 text-sm text-grey-text outline-none transition-all focus:border-amma-green focus:bg-amma-green-lighter focus-visible:ring-2 focus-visible:ring-amma-green" />
            <p v-if="validationErrors.telefone" id="telefone-error" class="mt-1 text-sm text-feedback-danger">{{ validationErrors.telefone }}</p>
          </div>
        </div>
      </section>

      <div class="lg:col-span-2 flex flex-col-reverse gap-3 border-t border-amma-green-light pt-5 sm:flex-row sm:items-center sm:justify-between">
        <button type="button" class="rounded-btn border-2 border-amma-green bg-white px-5 py-2.5 font-bold text-amma-green hover:bg-amma-green-lighter focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-amma-green" @click="voltarAoQuiz">Voltar ao Quiz</button>
        <button type="submit" :disabled="store.isCalculating" class="efeito-zoom rounded-btn bg-amma-green px-6 py-2.5 font-bold text-white hover:bg-amma-green-dark focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-amma-green disabled:cursor-not-allowed disabled:opacity-60">
          {{ store.isCalculating ? 'Calculando conversão…' : 'Continuar' }}
        </button>
      </div>
      <p v-if="store.conversionError" class="lg:col-span-2 rounded-btn border border-feedback-danger-border bg-feedback-danger-bg p-4 text-sm text-feedback-danger" role="alert">
        {{ store.conversionError }}
        <button type="button" class="ml-2 font-bold underline" @click="continuar">Tentar novamente</button>
      </p>
    </form>
  </section>
</template>
