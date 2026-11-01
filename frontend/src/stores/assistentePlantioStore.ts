import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { assistentePlantioQuestions } from '@/data/assistentePlantioQuestions'
import { buscarRecomendacoes } from '@/services/recomendacaoService'
import type { QuizAnswer, SpeciesRecommendation } from '@/types/assistentePlantio'

export const useAssistentePlantioStore = defineStore('assistentePlantio', () => {
  const respostas = ref<Record<string, string>>({})
  const currentIndex = ref(0)
  const recomendacoes = ref<SpeciesRecommendation[] | null>(null)
  const especieSelecionada = ref<SpeciesRecommendation | null>(null)
  const isLoading = ref(false)
  const errorMessage = ref<string | null>(null)
  const currentQuestion = computed(() => assistentePlantioQuestions[currentIndex.value])

  function setResposta(perguntaId: string, valor: string) {
    respostas.value[perguntaId] = valor
  }

  function avancar(): boolean {
    if (!currentQuestion.value || !respostas.value[currentQuestion.value.id]) return false
    if (currentIndex.value < assistentePlantioQuestions.length - 1) currentIndex.value += 1
    return true
  }

  function voltar() {
    if (currentIndex.value > 0) currentIndex.value -= 1
  }

  async function carregarRecomendacoes() {
    if (isLoading.value) return
    isLoading.value = true
    errorMessage.value = null
    try {
      const answers: QuizAnswer[] = assistentePlantioQuestions.map(({ id }) => ({
        perguntaId: id,
        valor: respostas.value[id] ?? ''
      }))
      recomendacoes.value = await buscarRecomendacoes(answers)
    } catch {
      recomendacoes.value = null
      errorMessage.value = 'Não foi possível buscar as recomendações agora. Suas respostas foram mantidas.'
    } finally {
      isLoading.value = false
    }
  }

  function selecionarEspecie(species: SpeciesRecommendation) {
    especieSelecionada.value = species
  }

  function refazerQuiz() {
    respostas.value = {}
    currentIndex.value = 0
    recomendacoes.value = null
    especieSelecionada.value = null
    errorMessage.value = null
    isLoading.value = false
  }

  return {
    respostas,
    currentIndex,
    currentQuestion,
    recomendacoes,
    especieSelecionada,
    isLoading,
    errorMessage,
    setResposta,
    avancar,
    voltar,
    carregarRecomendacoes,
    selecionarEspecie,
    refazerQuiz
  }
})
