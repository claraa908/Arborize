export interface DashboardOverview {
  mudasDoadas: number
  mudasPlantadas: number
  estoqueAtual: number
  garrafasPetArrecadadas: number
  substituicoesNim: number
  evolucao: DashboardMonthlyMetric[]
  estoquePorEspecie: DashboardSpeciesStock[]
}

export interface DashboardMonthlyMetric {
  mes: string
  mudasDoadas: number
  mudasPlantadas: number
}

export interface DashboardSpeciesStock {
  especie: string
  quantidade: number
}

export interface DashboardGoal {
  bairro: string
  zona: string
  metaPlantio: number
  realizado: number
  saldoLiquido: number
  deficitPercentual: number
  prioridade: string
}

export interface DashboardGoals {
  ano: number
  trimestre: number
  zona: string
  zonasDisponiveis: string[]
  anosDisponiveis: number[]
  metas: DashboardGoal[]
}

export type DashboardReportDataset = 'MUDAS_DOADAS' | 'BALANCO_RECICLAVEIS' | 'LAUDOS_EMITIDOS'
export type DashboardReportFormat = 'PDF' | 'CSV' | 'XLSX'

export interface DashboardReportRequest {
  dataInicial: string
  dataFinal: string
  dados: DashboardReportDataset[]
  formato: DashboardReportFormat
}
