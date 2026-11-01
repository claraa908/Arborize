<script setup lang="ts">
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { assistentePlantioQuestions } from '@/data/assistentePlantioQuestions'
import { useAssistentePlantioStore } from '@/stores/assistentePlantioStore'

const router = useRouter()
const route = useRoute()
const store = useAssistentePlantioStore()
const questionHeading = ref<HTMLElement | null>(null)
const showValidation = ref(false)
const handoffMessage = ref<string | null>(null)
const question = computed(() => store.currentQuestion)
const stepNumber = computed(() => store.currentIndex + 1)
const isLastQuestion = computed(() => store.currentIndex === assistentePlantioQuestions.length - 1)

onMounted(() => {
  if (route.query.aviso === 'especie-obrigatoria') {
    handoffMessage.value = 'Escolha uma espécie para continuar.'
  }
})

watch(() => store.currentIndex, async () => {
  showValidation.value = false
  await nextTick()
  questionHeading.value?.focus()
})

function continueQuiz() {
  if (!store.avancar()) {
    showValidation.value = true
    return
  }
  if (isLastQuestion.value) return
}

async function finishQuiz() {
  if (!question.value || !store.respostas[question.value.id]) {
    showValidation.value = true
    return
  }
  await store.carregarRecomendacoes()
  await router.push({ name: 'arborizacao-recomendacoes' })
}

function handleContinue() {
  if (isLastQuestion.value) void finishQuiz()
  else continueQuiz()
}
</script>

<template>
  <section class="mx-auto max-w-4xl py-8 md:py-12" aria-labelledby="assistente-title">
    <header class="mb-6 md:mb-8">
      <p class="text-xs font-bold uppercase tracking-wider text-amma-green">Serviço ao Cidadão</p>
      <h1 id="assistente-title" class="mt-2 text-2xl font-bold text-amma-green-dark">Assistente de Plantio</h1>
      <p class="mt-2 max-w-3xl text-sm leading-6 text-grey-text">
        Responda algumas perguntas rápidas sobre o local do plantio. Com base nas informações fornecidas, o Arborize indicará espécies adequadas para a sua solicitação.
      </p>
    </header>

    <p v-if="handoffMessage" class="mb-4 rounded-btn border border-feedback-info-border bg-feedback-info-bg p-3 text-sm text-feedback-info" role="status">
      {{ handoffMessage }}
    </p>

    <div class="rounded-box bg-white p-5 shadow-sm md:p-8">
      <div class="mb-7" role="progressbar" :aria-valuenow="stepNumber" aria-valuemin="1" aria-valuemax="10" :aria-label="`Pergunta ${stepNumber} de 10`">
        <div class="mb-2 flex items-center justify-between text-xs font-bold uppercase tracking-wider text-grey-text">
          <span>Pergunta {{ stepNumber }} de 10</span>
          <span>{{ stepNumber }} de 10 etapas previstas</span>
        </div>
        <div class="grid grid-cols-10 gap-1.5" aria-hidden="true">
          <span v-for="step in 10" :key="step" class="h-2 rounded-full" :class="step <= stepNumber ? 'bg-amma-green' : 'bg-amma-green-light'"></span>
        </div>
      </div>

      <fieldset :aria-describedby="showValidation ? 'answer-error' : undefined">
        <legend ref="questionHeading" tabindex="-1" class="mb-5 text-lg font-bold text-amma-green-dark outline-none focus-visible:ring-2 focus-visible:ring-amma-green">
          {{ question?.prompt }}
        </legend>
        <p class="mb-5 text-sm text-grey-text">{{ question?.helpText }}</p>

        <div class="grid grid-cols-1 gap-3 md:grid-cols-2">
          <label
            v-for="(option, index) in question?.options"
            :key="option.value"
            class="group relative flex min-h-20 cursor-pointer items-start gap-3 rounded-btn border-2 p-4 transition-colors focus-within:ring-2 focus-within:ring-amma-green"
            :class="store.respostas[question!.id] === option.value ? 'border-amma-green bg-amma-green-lighter' : 'border-amma-green-light bg-white hover:bg-amma-green-lighter'"
          >
            <input
              :id="`answer-${question?.id}-${option.value}`"
              type="radio"
              class="mt-1 h-4 w-4 accent-amma-green"
              :name="`question-${question?.id}`"
              :value="option.value"
              :checked="store.respostas[question!.id] === option.value"
              :aria-invalid="showValidation"
              :aria-describedby="showValidation ? 'answer-error' : undefined"
              @change="store.setResposta(question!.id, option.value); showValidation = false"
            />
            <span class="flex-1">
              <span class="block text-sm font-bold text-amma-green-dark">{{ option.label }}</span>
              <span class="mt-1 block text-sm leading-5 text-grey-text">{{ option.description }}</span>
            </span>
            <span v-if="index === 0" class="sr-only">Opção 1</span>
          </label>
        </div>
        <p v-if="showValidation" id="answer-error" class="mt-4 flex items-center gap-2 rounded-btn border border-feedback-danger-border bg-feedback-danger-bg p-3 text-sm font-semibold text-feedback-danger" role="alert">
          <span aria-hidden="true" class="material-symbols-outlined">error</span>
          Escolha uma opção para continuar.
        </p>
      </fieldset>

      <div class="mt-8 flex flex-col-reverse gap-3 border-t border-amma-green-light pt-5 sm:flex-row sm:justify-between">
        <button
          v-if="store.currentIndex > 0"
          type="button"
          class="rounded-btn border-2 border-amma-green bg-white px-6 py-2.5 font-bold text-amma-green transition-colors hover:bg-amma-green-lighter focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-amma-green"
          @click="store.voltar()"
        >Voltar</button>
        <span v-else></span>
        <button
          type="button"
          :disabled="store.isLoading"
          :aria-busy="store.isLoading"
          class="efeito-zoom rounded-btn bg-amma-green px-6 py-2.5 font-bold text-white hover:bg-amma-green-dark focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-amma-green"
          @click="handleContinue"
        >
          {{ store.isLoading ? 'Buscando recomendações…' : isLastQuestion ? 'Finalizar' : 'Próxima' }}
        </button>
      </div>

    </div>
  </section>
</template>
