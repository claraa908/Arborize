import { defineStore } from 'pinia'
import { ref } from 'vue'
import { dashboardService } from '@/services/dashboardService'
import { useFeedbackStore } from '@/stores/feedbackStore'
import type {
  DashboardGoals,
  DashboardOverview,
  DashboardReportRequest,
} from '@/types/dashboard'

function getErrorMessage(error: unknown, fallback: string): string {
  if (typeof error === 'object' && error !== null && 'response' in error) {
    const response = error.response
    if (typeof response === 'object' && response !== null && 'data' in response) {
      const data = response.data
      if (typeof data === 'object' && data !== null && 'detail' in data && typeof data.detail === 'string') {
        return data.detail
      }
    }
  }
  return fallback
}

export const useDashboardStore = defineStore('dashboard', () => {
  const feedback = useFeedbackStore()
  const overview = ref<DashboardOverview | null>(null)
  const goals = ref<DashboardGoals | null>(null)
  const isLoadingOverview = ref(false)
  const isLoadingGoals = ref(false)
  const overviewError = ref<string | null>(null)
  const goalsError = ref<string | null>(null)
  let goalsRequestId = 0

  async function fetchOverview(): Promise<boolean> {
    isLoadingOverview.value = true
    overviewError.value = null
    try {
      overview.value = await dashboardService.getOverview()
      return true
    } catch (error) {
      overviewError.value = getErrorMessage(error, 'Não foi possível carregar os indicadores.')
      feedback.showToast(overviewError.value, 'error')
      return false
    } finally {
      isLoadingOverview.value = false
    }
  }

  async function fetchGoals(filters: { zone: string; year: number; quarter: number }): Promise<boolean> {
    const requestId = ++goalsRequestId
    isLoadingGoals.value = true
    goalsError.value = null
    try {
      const result = await dashboardService.getGoals(filters)
      if (requestId === goalsRequestId) {
        goals.value = result
      }
      return true
    } catch (error) {
      if (requestId === goalsRequestId) {
        goalsError.value = getErrorMessage(error, 'Não foi possível carregar as metas deste período.')
        feedback.showToast(goalsError.value, 'error')
      }
      return false
    } finally {
      if (requestId === goalsRequestId) {
        isLoadingGoals.value = false
      }
    }
  }

  async function generateReport(request: DashboardReportRequest): Promise<Blob | null> {
    try {
      const file = await dashboardService.generateReport(request)
      feedback.showToast('Relatório gerado e baixado com sucesso.', 'success')
      return file
    } catch (error) {
      feedback.showToast(
        getErrorMessage(error, 'Não foi possível gerar o relatório. Tente novamente.'),
        'error',
      )
      return null
    }
  }

  return {
    overview,
    goals,
    isLoadingOverview,
    isLoadingGoals,
    overviewError,
    goalsError,
    fetchOverview,
    fetchGoals,
    generateReport,
  }
})
