import axios from 'axios'
import { api } from '@/services/api'
import type { ProblemDetailResponse, QuizAnswer, SpeciesRecommendation } from '@/types/assistentePlantio'
import { RecommendationError } from '@/types/assistentePlantio'

export async function buscarRecomendacoes(respostas: QuizAnswer[]): Promise<SpeciesRecommendation[]> {
  try {
    const response = await api.post<SpeciesRecommendation[]>('/api/assistente-plantio/recomendar', { respostas })
    return response.data
  } catch (error) {
    if (axios.isAxiosError<ProblemDetailResponse>(error)) {
      throw new RecommendationError(
        'Não foi possível buscar as recomendações agora. Suas respostas foram mantidas.',
        error.response?.status,
        error.response?.data
      )
    }
    throw new RecommendationError('Não foi possível buscar as recomendações agora. Suas respostas foram mantidas.')
  }
}
