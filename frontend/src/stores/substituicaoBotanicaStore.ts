import { defineStore } from 'pinia'
import { ref } from 'vue'
import { substituicaoBotanicaService } from '@/services/substituicaoBotanicaService'
import type {
  LaudoSubstituicaoPayload,
  SubstituicaoBotanicaStatus,
  SubstituicaoGestao
} from '@/types/substituicaoBotanica'

export const useSubstituicaoBotanicaStore = defineStore('substituicaoBotanica', () => {
  const requests = ref<SubstituicaoGestao[]>([])
  const selected = ref<SubstituicaoGestao | null>(null)
  const isLoading = ref(false)
  const isSubmitting = ref(false)
  const error = ref<string | null>(null)

  async function carregar() {
    isLoading.value = true
    error.value = null
    try {
      requests.value = await substituicaoBotanicaService.listarGestao()
      return true
    } catch {
      error.value = 'Não foi possível carregar os requerimentos. Tente novamente.'
      return false
    } finally {
      isLoading.value = false
    }
  }

  async function atualizar(id: number, action: () => Promise<SubstituicaoGestao>) {
    isSubmitting.value = true
    error.value = null
    try {
      const result = await action()
      const index = requests.value.findIndex((item) => item.id === id)
      if (index >= 0) requests.value[index] = result
      selected.value = result
      return true
    } catch {
      error.value = 'Não foi possível atualizar o requerimento. Revise os dados e tente novamente.'
      return false
    } finally {
      isSubmitting.value = false
    }
  }

  const iniciarAnalise = (id: number) => atualizar(id, () => substituicaoBotanicaService.iniciarAnalise(id))
  const decidir = (id: number, status: 'APROVADA' | 'REJEITADA', justificativa?: string) =>
    atualizar(id, () => substituicaoBotanicaService.decidir(id, status, justificativa))
  const registrarProgresso = (id: number, status: SubstituicaoBotanicaStatus, descricao: string) =>
    atualizar(id, () => substituicaoBotanicaService.registrarProgresso(id, status, descricao))
  const emitirLaudo = (id: number, payload: LaudoSubstituicaoPayload) =>
    atualizar(id, () => substituicaoBotanicaService.emitirLaudo(id, payload))

  async function carregarDetalhe(id: number) {
    isLoading.value = true
    error.value = null
    try {
      selected.value = await substituicaoBotanicaService.buscarGestao(id)
      return selected.value
    } catch {
      error.value = 'Não foi possível carregar os dados do requerimento.'
      return null
    } finally {
      isLoading.value = false
    }
  }

  return {
    requests,
    selected,
    isLoading,
    isSubmitting,
    error,
    carregar,
    carregarDetalhe,
    iniciarAnalise,
    decidir,
    registrarProgresso,
    emitirLaudo
  }
})
