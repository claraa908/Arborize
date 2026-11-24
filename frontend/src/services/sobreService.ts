import { api } from '@/services/api'
import type { SobreIndicadoresDTO } from '@/types/sobre'

export const sobreService = {
  async getIndicadores(): Promise<SobreIndicadoresDTO> {
    const response = await api.get<SobreIndicadoresDTO>('/api/sobre/indicadores')
    return response.data
  },
}
