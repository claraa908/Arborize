export interface QuizOption {
  label: string
  description: string
  value: string
}

export interface QuizQuestion {
  id: string
  prompt: string
  helpText: string
  options: QuizOption[]
}

export interface SpeciesRecommendation {
  id: number
  nomePopular: string
  nomeCientifico: string
  porte: string
  fotoUrl?: string | null
  imagemUrl?: string | null
  urlFoto?: string | null
}

export interface QuizAnswer {
  perguntaId: string
  valor: string
}

export interface SpeciesRecord {
  id: number
  nomePopular: string
  nomeCientifico: string
  categoria: string
  porteArvore: string
  fotoUrl?: string
}

export interface ProblemDetailResponse {
  type?: string
  title?: string
  status?: number
  detail?: string
  code?: string
  errors?: Array<{ field: string; message: string }>
}

export class RecommendationError extends Error {
  constructor(
    message: string,
    readonly status?: number,
    readonly problem?: ProblemDetailResponse
  ) {
    super(message)
    this.name = 'RecommendationError'
  }
}
