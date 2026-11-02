// Contratos de ArvoreController e dos records Java; não são modelos de formulário.
export type ClassificacaoTerritorial =
  'BAIRRO_IDENTIFICADO' | 'BAIRRO_NAO_IDENTIFICADO' | 'FORA_DA_AREA_URBANA'

// Classificação provisória já adotada pelo backend, pendente de validação da AMMA.
export type CondicaoFitossanitaria =
  'NAO_AVALIADA' | 'APARENTEMENTE_SAUDAVEL' | 'SINAIS_COMPROMETIMENTO' | 'MORTA'

export interface EspecieIdentificacaoResponseDTO {
  id: number
  nomePopular: string
  nomeCientifico: string
  familiaBotanica: string | null
}

export interface BairroResumoResponseDTO {
  id: number
  nome: string
  ativo: boolean
}

export interface FotoCapaResponseDTO {
  id: number
  contentType: 'image/jpeg' | 'image/png' | 'image/webp'
}

export interface ArvoreResumoResponseDTO {
  id: number
  codigo: string
  especie: EspecieIdentificacaoResponseDTO
  latitude: number
  longitude: number
  classificacaoTerritorial: ClassificacaoTerritorial
  bairro: BairroResumoResponseDTO | null
  bairroNome?: string | null
  cep?: string | null
  zonaRural?: boolean | null
  localidadeRural?: string | null
  referenciaLocalizacao: string | null
  condicaoFitossanitaria: CondicaoFitossanitaria
  fotoCapa: FotoCapaResponseDTO | null
}

export interface ArvoreResponseDTO extends ArvoreResumoResponseDTO {
  // LocalDate: YYYY-MM-DD; LocalDateTime: ISO sem fuso. Não converter para Date aqui.
  dataPlantio: string | null
  dapCm: number | null
  alturaEstimadaM: number | null
  observacoes: string | null
  criadoEm: string
  atualizadoEm: string
}

export interface FotoArvoreResponseDTO {
  id: number
  arvoreId: number
  contentType: 'image/jpeg' | 'image/png' | 'image/webp'
  sizeBytes: number
  criadoEm: string
}

export interface ArvoreCadastroRequestDTO {
  especieId: number
  latitude: number
  longitude: number
  zonaRural: boolean
  bairroId: number | null
  localidadeRural: string | null
  classificacaoTerritorial?: ClassificacaoTerritorial | null
  bairroNome?: string | null
  cep?: string | null
  referenciaLocalizacao?: string | null
  dataPlantio?: string | null
  dapCm?: number | null
  alturaEstimadaM?: number | null
  condicaoFitossanitaria?: CondicaoFitossanitaria | null
  observacoes?: string | null
}

// PUT completo, não Partial: campos opcionais explicitamente nulos significam limpeza.
export type ArvoreAtualizacaoRequestDTO = Omit<
  ArvoreCadastroRequestDTO,
  'condicaoFitossanitaria'
> & {
  condicaoFitossanitaria: CondicaoFitossanitaria
}

export interface ArvorePaginaResponseDTO {
  content: ArvoreResumoResponseDTO[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export interface InventarioFiltros {
  search?: string
  bairro?: string
  especieId?: number | null
  condicao?: CondicaoFitossanitaria | null
}

export interface InventarioListagemParams extends InventarioFiltros {
  // A API inicia a paginação em zero; padrões do Controller: page=0, size=20.
  page?: number
  size?: number
}

export interface ErroCampoInventario {
  field: string
  message: string
}

export interface ProblemDetailInventario {
  type: string
  title: string
  status: number
  detail: string
  code?: string
  instance?: string
  errors?: ErroCampoInventario[]
}

// Estado público de erro: não armazena configuração Axios, credenciais ou corpo arbitrário.
export interface ErroInventario {
  status: number | null
  message: string
  acessoNegado: boolean
  problem: ProblemDetailInventario | null
}
