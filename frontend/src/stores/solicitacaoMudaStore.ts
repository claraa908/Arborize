import { defineStore } from 'pinia'
import { ref } from 'vue'
import { solicitacaoMudaService } from '@/services/solicitacaoMudaService'
import { SolicitacaoMudaError } from '@/types/solicitacaoMuda'
import type {
  ConversaoReciclagem,
  SolicitacaoMudaForm,
  SolicitacaoMudaResponse
} from '@/types/solicitacaoMuda'

const emptyForm = (): SolicitacaoMudaForm => ({
  especieId: 0,
  quantidadeMudas: 1,
  logradouro: '',
  numeroEndereco: '',
  bairro: '',
  complemento: '',
  pontoReferencia: '',
  nomeSolicitante: '',
  cpf: '',
  telefone: ''
})

export const useSolicitacaoMudaStore = defineStore('solicitacaoMuda', () => {
  const form = ref<SolicitacaoMudaForm>(emptyForm())
  const conversao = ref<ConversaoReciclagem | null>(null)
  const resultado = ref<SolicitacaoMudaResponse | null>(null)
  const isCalculating = ref(false)
  const isSubmitting = ref(false)
  const conversionError = ref<string | null>(null)
  const submitError = ref<string | null>(null)
  const serverErrors = ref<Record<string, string>>({})

  async function calcularConversao() {
    isCalculating.value = true
    conversionError.value = null
    try {
      conversao.value = await solicitacaoMudaService.calcularConversao(form.value.quantidadeMudas)
      return true
    } catch {
      conversionError.value = 'Não foi possível calcular os recicláveis necessários. Tente novamente.'
      return false
    } finally {
      isCalculating.value = false
    }
  }

  async function enviarSolicitacao() {
    if (isSubmitting.value) return false
    isSubmitting.value = true
    submitError.value = null
    serverErrors.value = {}
    try {
      resultado.value = await solicitacaoMudaService.criarSolicitacao(form.value)
      return true
    } catch (error) {
      if (error instanceof SolicitacaoMudaError) serverErrors.value = error.problem?.errors ?? {}
      submitError.value = 'Não foi possível enviar sua solicitação agora. Seus dados foram mantidos.'
      return false
    } finally {
      isSubmitting.value = false
    }
  }

  function preencherEspecie(especieId: number) {
    if (form.value.especieId !== especieId) {
      form.value = { ...emptyForm(), especieId }
      conversao.value = null
      resultado.value = null
    }
  }

  return {
    form,
    conversao,
    resultado,
    isCalculating,
    isSubmitting,
    conversionError,
    submitError,
    serverErrors,
    calcularConversao,
    enviarSolicitacao,
    preencherEspecie
  }
})
