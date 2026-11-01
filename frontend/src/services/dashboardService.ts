import { api } from '@/services/api'
import type {
  DashboardGoals,
  DashboardOverview,
  DashboardReportRequest,
} from '@/types/dashboard'

export const dashboardService = {
  async getOverview(): Promise<DashboardOverview> {
    const response = await api.get<DashboardOverview>('/api/dashboard/overview')
    return response.data
  },

  async getGoals(filters: { zone: string; year: number; quarter: number }): Promise<DashboardGoals> {
    const response = await api.get<DashboardGoals>('/api/dashboard/goals', {
      params: {
        zone: filters.zone === 'Todas' ? undefined : filters.zone,
        year: filters.year,
        quarter: filters.quarter,
      },
    })
    return response.data
  },

  async generateReport(request: DashboardReportRequest): Promise<Blob> {
    const response = await api.post<Blob>('/api/dashboard/reports', request, {
      responseType: 'blob',
    })
    return response.data
  },
}
