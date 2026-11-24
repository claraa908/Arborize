import { defineStore } from 'pinia'
import { ref } from 'vue'
import { compensacaoService, type TermoCompensacaoPayload } from '@/services/compensacaoService'
import type { CompensacaoStatus, TermoCompensacao } from '@/types/compensacao'

export const useCompensacaoStore = defineStore('compensacao', () => {
  const termos = ref<TermoCompensacao[]>([])
  const termoAtual = ref<TermoCompensacao | null>(null)
  const isLoading = ref(false)
  const isSubmitting = ref(false)
  const error = ref<string | null>(null)
  const serverErrors = ref<Record<string, string>>({})

  function guardarErro(cause: unknown, fallback: string) {
    error.value = cause instanceof Error ? cause.message : fallback
    serverErrors.value = cause && typeof cause === 'object' && 'fields' in cause
      && typeof cause.fields === 'object' && cause.fields !== null
      ? cause.fields as Record<string, string> : {}
  }

  async function carregarLista() {
    isLoading.value = true
    error.value = null
    serverErrors.value = {}
    try { termos.value = await compensacaoService.listar(); return true }
    catch (cause) { guardarErro(cause, 'Não foi possível carregar os termos.'); return false }
    finally { isLoading.value = false }
  }
  async function carregarTermo(id: number) {
    isLoading.value = true
    error.value = null
    serverErrors.value = {}
    try { termoAtual.value = await compensacaoService.consultar(id); return true }
    catch (cause) { guardarErro(cause, 'Não foi possível carregar o termo.'); return false }
    finally { isLoading.value = false }
  }
  async function criar(dados: TermoCompensacaoPayload, arquivo: File) {
    isSubmitting.value = true
    error.value = null
    serverErrors.value = {}
    try { termoAtual.value = await compensacaoService.criar(dados, arquivo); return true }
    catch (cause) { guardarErro(cause, 'Não foi possível cadastrar o termo.'); return false }
    finally { isSubmitting.value = false }
  }
  async function atualizar(id: number, dados: TermoCompensacaoPayload) {
    isSubmitting.value = true
    error.value = null
    serverErrors.value = {}
    try { termoAtual.value = await compensacaoService.atualizar(id, dados); return true }
    catch (cause) { guardarErro(cause, 'Não foi possível atualizar o termo.'); return false }
    finally { isSubmitting.value = false }
  }
  async function atualizarStatus(id: number, status: CompensacaoStatus) {
    isSubmitting.value = true
    error.value = null
    serverErrors.value = {}
    try { termoAtual.value = await compensacaoService.atualizarStatus(id, status); return true }
    catch (cause) { guardarErro(cause, 'Não foi possível atualizar o estado.'); return false }
    finally { isSubmitting.value = false }
  }
  async function registrarEntrega(id: number, quantidade: number, recibo: File) {
    isSubmitting.value = true
    error.value = null
    serverErrors.value = {}
    try { termoAtual.value = await compensacaoService.registrarEntrega(id, quantidade, recibo); return true }
    catch (cause) { guardarErro(cause, 'Não foi possível registrar a entrega.'); return false }
    finally { isSubmitting.value = false }
  }
  function limparAtual() { termoAtual.value = null; error.value = null; serverErrors.value = {} }

  return { termos, termoAtual, isLoading, isSubmitting, error, serverErrors, carregarLista, carregarTermo, criar, atualizar, atualizarStatus, registrarEntrega, limparAtual }
})
