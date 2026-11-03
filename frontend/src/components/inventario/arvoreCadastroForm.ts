import type {
  ArvoreAtualizacaoRequestDTO,
  ArvoreCadastroRequestDTO,
  ArvoreResponseDTO,
  BairroResumoResponseDTO,
  CondicaoFitossanitaria,
} from '@/types/inventario'

export interface ArvoreCadastroFormulario {
  especieId: string
  latitude: string
  longitude: string
  zonaRural: boolean
  bairroId: string
  localidadeRural: string
  cep: string
  referenciaLocalizacao: string
  dataPlantio: string
  dapCm: string
  alturaEstimadaM: string
  condicaoFitossanitaria: CondicaoFitossanitaria
  observacoes: string
}

export type ErrosCadastroArvore = Partial<Record<keyof ArvoreCadastroFormulario, string>>

export type CampoEditadoDuranteGeocodificacao =
  | 'referenciaLocalizacao'
  | 'zonaRural'
  | 'bairroId'
  | 'localidadeRural'
  | 'cep'

export function normalizarNomeTerritorial(valor: string): string {
  return valor
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .trim()
    .replace(/\s+/g, ' ')
    .toLocaleLowerCase('pt-BR')
}

export function localizarBairroUnico(
  sugestao: string,
  bairros: readonly BairroResumoResponseDTO[],
): BairroResumoResponseDTO | null {
  const chave = normalizarNomeTerritorial(sugestao)
  if (!chave) return null
  const correspondencias = bairros.filter(
    (bairro) => bairro.ativo && normalizarNomeTerritorial(bairro.nome) === chave,
  )
  return correspondencias.length === 1 ? correspondencias[0]! : null
}

export function criarControleEdicaoDuranteGeocodificacao() {
  const revisoes: Record<CampoEditadoDuranteGeocodificacao, number> = {
    referenciaLocalizacao: 0,
    zonaRural: 0,
    bairroId: 0,
    localidadeRural: 0,
    cep: 0,
  }
  let revisoesDaSolicitacao: Record<CampoEditadoDuranteGeocodificacao, number> | null = null

  return {
    iniciarSolicitacao() {
      revisoesDaSolicitacao = { ...revisoes }
    },
    registrarEdicao(campo: CampoEditadoDuranteGeocodificacao) {
      revisoes[campo] += 1
    },
    podeAtualizar(campo: CampoEditadoDuranteGeocodificacao) {
      return revisoesDaSolicitacao !== null && revisoesDaSolicitacao[campo] === revisoes[campo]
    },
  }
}

export function novoFormularioArvore(): ArvoreCadastroFormulario {
  return {
    especieId: '',
    latitude: '',
    longitude: '',
    zonaRural: false,
    bairroId: '',
    localidadeRural: '',
    cep: '',
    referenciaLocalizacao: '',
    dataPlantio: '',
    dapCm: '',
    alturaEstimadaM: '',
    condicaoFitossanitaria: 'NAO_AVALIADA',
    observacoes: '',
  }
}

export function preencherFormularioArvore(arvore: ArvoreResponseDTO): ArvoreCadastroFormulario {
  return {
    especieId: String(arvore.especie.id),
    latitude: String(arvore.latitude),
    longitude: String(arvore.longitude),
    zonaRural: arvore.zonaRural ?? arvore.classificacaoTerritorial === 'FORA_DA_AREA_URBANA',
    bairroId: '',
    localidadeRural: arvore.localidadeRural ?? '',
    cep: arvore.cep ?? '',
    referenciaLocalizacao: arvore.referenciaLocalizacao ?? '',
    dataPlantio: arvore.dataPlantio ?? '',
    dapCm: arvore.dapCm === null ? '' : String(arvore.dapCm),
    alturaEstimadaM: arvore.alturaEstimadaM === null ? '' : String(arvore.alturaEstimadaM),
    condicaoFitossanitaria: arvore.condicaoFitossanitaria,
    observacoes: arvore.observacoes ?? '',
  }
}

function numero(texto: unknown): number | null {
  const entrada = String(texto ?? '').trim()
  if (!entrada) return null
  const valor = Number(entrada)
  return Number.isFinite(valor) ? valor : null
}

function textoNormalizado(valor: unknown): string {
  return String(valor ?? '').trim()
}

export function validarCadastroArvore(
  formulario: ArvoreCadastroFormulario,
  especiesDisponiveis: ReadonlySet<number>,
  bairrosAtivos: ReadonlySet<number>,
): ErrosCadastroArvore {
  const erros: ErrosCadastroArvore = {}
  const especieId = numero(formulario.especieId)
  if (especieId === null || !Number.isInteger(especieId) || !especiesDisponiveis.has(especieId)) {
    erros.especieId = 'Selecione uma espécie disponível no catálogo.'
  }

  const latitude = numero(formulario.latitude)
  if (latitude === null || latitude < -90 || latitude > 90) {
    erros.latitude = 'Informe uma latitude entre -90 e 90 graus decimais.'
  }
  const longitude = numero(formulario.longitude)
  if (longitude === null || longitude < -180 || longitude > 180) {
    erros.longitude = 'Informe uma longitude entre -180 e 180 graus decimais.'
  }

  if (formulario.zonaRural) {
    if (!textoNormalizado(formulario.localidadeRural)) {
      erros.localidadeRural = 'Informe a localidade rural.'
    }
  } else {
    const bairroId = numero(formulario.bairroId)
    if (bairroId === null || !Number.isInteger(bairroId) || !bairrosAtivos.has(bairroId)) {
      erros.bairroId = 'Selecione um bairro ativo no catálogo.'
    }
  }

  for (const [campo, rotulo] of [
    ['dapCm', 'O DAP'],
    ['alturaEstimadaM', 'A altura estimada'],
  ] as const) {
    const texto = formulario[campo]
    if (texto && (numero(texto) === null || numero(texto)! <= 0)) {
      erros[campo] = `${rotulo} deve ser um valor positivo.`
    }
  }

  return erros
}

function criarPayloadTerritorial(formulario: ArvoreCadastroFormulario) {
  const dap = textoNormalizado(formulario.dapCm)
  const altura = textoNormalizado(formulario.alturaEstimadaM)
  return {
    zonaRural: formulario.zonaRural,
    bairroId: formulario.zonaRural ? null : Number(formulario.bairroId),
    bairroNome: null,
    localidadeRural: formulario.zonaRural ? textoNormalizado(formulario.localidadeRural) : null,
    cep: textoNormalizado(formulario.cep) || null,
    referenciaLocalizacao: textoNormalizado(formulario.referenciaLocalizacao) || null,
    dataPlantio: formulario.dataPlantio || null,
    dapCm: dap ? Number(dap) : null,
    alturaEstimadaM: altura ? Number(altura) : null,
    condicaoFitossanitaria: formulario.condicaoFitossanitaria,
    observacoes: textoNormalizado(formulario.observacoes) || null,
  }
}

export function criarPayloadCadastroArvore(
  formulario: ArvoreCadastroFormulario,
): ArvoreCadastroRequestDTO {
  return {
    especieId: Number(formulario.especieId),
    latitude: Number(formulario.latitude),
    longitude: Number(formulario.longitude),
    ...criarPayloadTerritorial(formulario),
  }
}

export function criarPayloadAtualizacaoArvore(
  formulario: ArvoreCadastroFormulario,
): ArvoreAtualizacaoRequestDTO {
  return {
    especieId: Number(formulario.especieId),
    latitude: Number(formulario.latitude),
    longitude: Number(formulario.longitude),
    ...criarPayloadTerritorial(formulario),
    condicaoFitossanitaria: formulario.condicaoFitossanitaria,
  }
}
