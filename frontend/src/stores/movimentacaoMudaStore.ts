import { defineStore } from 'pinia'
import { ref } from 'vue'
import { isAxiosError } from 'axios'
import { movimentacaoMudaService } from '@/services/movimentacaoMudaService'
import { useFeedbackStore } from '@/stores/feedbackStore'
import type {
  MovimentacaoMudaDTO,
  MovimentacaoMudaFiltros,
  MovimentacaoMudaPayload,
  PaginaMovimentacoes,
  ResumoMovimentacoes,
} from '@/types/movimentacaoMuda'

export const useMovimentacaoMudaStore = defineStore('movimentacaoMuda', () => {
  const feedback = useFeedbackStore()
  const pagina = ref<PaginaMovimentacoes | null>(null)
  const resumo = ref<ResumoMovimentacoes | null>(null)
  const movimentacaoAtual = ref<MovimentacaoMudaDTO | null>(null)
  const isLoadingListagem = ref(false)
  const isLoadingResumo = ref(false)
  const isLoadingDetalhe = ref(false)
  const isSaving = ref(false)
  const listagemError = ref('')
  const formError = ref('')
  const resumoError = ref('')

  async function buscarResumo() {
    isLoadingResumo.value = true
    resumoError.value = ''
    try {
      resumo.value = await movimentacaoMudaService.resumo()
    } catch {
      resumo.value = null
      resumoError.value = 'Não foi possível carregar os indicadores mensais.'
    } finally {
      isLoadingResumo.value = false
    }
  }

  async function listar(filtros: MovimentacaoMudaFiltros = {}, page = 0) {
    isLoadingListagem.value = true
    listagemError.value = ''
    try {
      pagina.value = await movimentacaoMudaService.listar(filtros, page)
    } catch {
      listagemError.value = 'Não foi possível carregar o histórico de movimentações.'
    } finally {
      isLoadingListagem.value = false
    }
  }

  async function buscar(id: number): Promise<MovimentacaoMudaDTO | null> {
    isLoadingDetalhe.value = true
    formError.value = ''
    movimentacaoAtual.value = null
    try {
      movimentacaoAtual.value = await movimentacaoMudaService.buscar(id)
      return movimentacaoAtual.value
    } catch {
      formError.value = 'Não foi possível carregar esta movimentação.'
      return null
    } finally {
      isLoadingDetalhe.value = false
    }
  }

  async function salvar(payload: MovimentacaoMudaPayload, id?: number): Promise<boolean> {
    isSaving.value = true
    formError.value = ''
    try {
      if (id) await movimentacaoMudaService.atualizar(id, payload)
      else await movimentacaoMudaService.criar(payload)
      feedback.showToast(
        id ? 'Movimentação atualizada com sucesso.' : 'Movimentação registrada com sucesso.',
        'success',
      )
      await buscarResumo()
      return true
    } catch (error: unknown) {
      const detail =
        typeof error === 'object' && error !== null && 'response' in error
          ? (error as { response?: { data?: { detail?: unknown } } }).response?.data?.detail
          : null
      const status = isAxiosError(error) ? error.response?.status : undefined
      formError.value =
        typeof detail === 'string'
          ? detail
          : status === 401
            ? 'Sua sessão expirou ou não é válida. Saia da conta e entre novamente para registrar a movimentação.'
            : status === 403
              ? 'Seu perfil não tem permissão para registrar movimentações.'
          : 'Não foi possível salvar. Confira o saldo disponível e os dados informados.'
      feedback.showToast(formError.value, 'error')
      return false
    } finally {
      isSaving.value = false
    }
  }

  async function abrirComprovante(id: number) {
    try {
      const arquivo = await movimentacaoMudaService.comprovante(id)
      const url = URL.createObjectURL(arquivo)
      const link = document.createElement('a')
      link.href = url
      link.target = '_blank'
      link.rel = 'noopener'
      link.click()
      window.setTimeout(() => URL.revokeObjectURL(url), 60_000)
    } catch {
      feedback.showToast('Não foi possível abrir o comprovante.', 'error')
    }
  }

  return {
    pagina,
    resumo,
    movimentacaoAtual,
    isLoadingListagem,
    isLoadingResumo,
    isLoadingDetalhe,
    isSaving,
    listagemError,
    formError,
    resumoError,
    buscarResumo,
    listar,
    buscar,
    salvar,
    abrirComprovante,
  }
})
