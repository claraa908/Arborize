import axios, { type AxiosResponse } from 'axios'
import { api, type ApiRequestConfig } from '@/services/api'
import type {
  ArvoreAtualizacaoRequestDTO,
  ArvoreCadastroRequestDTO,
  ArvorePaginaResponseDTO,
  ArvoreResponseDTO,
  ArvoreResumoResponseDTO,
  BairroResumoResponseDTO,
  ErroCampoInventario,
  ErroInventario,
  FotoArvoreResponseDTO,
  InventarioFiltros,
  InventarioListagemParams,
  ProblemDetailInventario,
} from '@/types/inventario'

function objeto(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null && !Array.isArray(value)
}

function lerProblemDetail(value: unknown): ProblemDetailInventario | null {
  if (
    !objeto(value) ||
    typeof value.type !== 'string' ||
    typeof value.title !== 'string' ||
    typeof value.status !== 'number' ||
    typeof value.detail !== 'string'
  )
    return null

  const problem: ProblemDetailInventario = {
    type: value.type,
    title: value.title,
    status: value.status,
    detail: value.detail,
  }
  if (typeof value.code === 'string') problem.code = value.code
  if (typeof value.instance === 'string') problem.instance = value.instance
  if (Array.isArray(value.errors)) {
    problem.errors = value.errors
      .filter(
        (item): item is ErroCampoInventario =>
          objeto(item) && typeof item.field === 'string' && typeof item.message === 'string',
      )
      .map(({ field, message }) => ({ field, message }))
  }
  return problem
}

export class InventarioApiError extends Error implements ErroInventario {
  readonly acessoNegado: boolean

  constructor(
    readonly status: number | null,
    readonly problem: ProblemDetailInventario | null,
    cause: unknown,
  ) {
    const acessoNegado = status === 401 || status === 403
    const fallback =
      status === null
        ? 'Não foi possível conectar ao inventário.'
        : status === 404
          ? 'O recurso solicitado não foi encontrado.'
          : 'Não foi possível concluir a operação no inventário.'
    super(acessoNegado ? 'Acesso negado ao inventário.' : problem?.detail || fallback, { cause })
    this.name = 'InventarioApiError'
    this.acessoNegado = acessoNegado
  }
}

// Mantém o status HTTP real e o erro original como cause; nunca transforma falha em sucesso.
export function interpretarErroInventario(error: unknown): InventarioApiError {
  if (error instanceof InventarioApiError) return error
  if (axios.isAxiosError<unknown>(error)) {
    return new InventarioApiError(
      error.response?.status ?? null,
      lerProblemDetail(error.response?.data),
      error,
    )
  }
  return new InventarioApiError(null, null, error)
}

async function requisitar<T>(request: () => Promise<AxiosResponse<T>>): Promise<T> {
  try {
    return (await request()).data
  } catch (error) {
    throw interpretarErroInventario(error)
  }
}

// Não usa teste de truthiness para IDs: valores inválidos são enviados à validação da API.
function filtrosHttp(filtros: InventarioFiltros) {
  return {
    ...(filtros.search?.trim() ? { search: filtros.search.trim() } : {}),
    ...(filtros.bairro?.trim() ? { bairro: filtros.bairro.trim() } : {}),
    ...(filtros.especieId != null ? { especieId: filtros.especieId } : {}),
    ...(filtros.condicao != null ? { condicao: filtros.condicao } : {}),
  }
}

const recurso = '/api/arvores'
const feedbackLocalDeErro: ApiRequestConfig = {
  suppressGlobalErrorToast: true,
}

export const inventarioService = {
  getBairros(): Promise<BairroResumoResponseDTO[]> {
    return requisitar(() => api.get<BairroResumoResponseDTO[]>('/api/bairros', feedbackLocalDeErro))
  },

  getAll(params: InventarioListagemParams = {}): Promise<ArvorePaginaResponseDTO> {
    return requisitar(() =>
      api.get<ArvorePaginaResponseDTO>(recurso, {
        params: { ...filtrosHttp(params), page: params.page ?? 0, size: params.size ?? 20 },
        ...feedbackLocalDeErro,
      }),
    )
  },

  getMapa(filtros: InventarioFiltros = {}): Promise<ArvoreResumoResponseDTO[]> {
    return requisitar(() =>
      api.get<ArvoreResumoResponseDTO[]>(`${recurso}/mapa`, {
        params: filtrosHttp(filtros),
        ...feedbackLocalDeErro,
      }),
    )
  },

  getById(id: number): Promise<ArvoreResponseDTO> {
    return requisitar(() => api.get<ArvoreResponseDTO>(`${recurso}/${id}`, feedbackLocalDeErro))
  },

  create(payload: ArvoreCadastroRequestDTO): Promise<ArvoreResponseDTO> {
    return requisitar(() => api.post<ArvoreResponseDTO>(recurso, payload, feedbackLocalDeErro))
  },

  update(id: number, payload: ArvoreAtualizacaoRequestDTO): Promise<ArvoreResponseDTO> {
    return requisitar(() =>
      api.put<ArvoreResponseDTO>(`${recurso}/${id}`, payload, feedbackLocalDeErro),
    )
  },

  listarFotos(arvoreId: number): Promise<FotoArvoreResponseDTO[]> {
    return requisitar(() => api.get<FotoArvoreResponseDTO[]>(`${recurso}/${arvoreId}/fotos`))
  },

  recuperarFoto(
    arvoreId: number,
    fotoId: number,
    opcoes: { suppressGlobalErrorToast?: boolean } = {},
  ): Promise<Blob> {
    return requisitar(() =>
      api.get<Blob>(`${recurso}/${arvoreId}/fotos/${fotoId}`, {
        responseType: 'blob',
        ...(opcoes.suppressGlobalErrorToast ? feedbackLocalDeErro : {}),
      }),
    )
  },

  adicionarFoto(arvoreId: number, file: File): Promise<FotoArvoreResponseDTO> {
    const formData = new FormData()
    formData.append('file', file)
    return requisitar(() =>
      api.post<FotoArvoreResponseDTO>(`${recurso}/${arvoreId}/fotos`, formData),
    )
  },

  removerFoto(arvoreId: number, fotoId: number): Promise<void> {
    return requisitar(() => api.delete<void>(`${recurso}/${arvoreId}/fotos/${fotoId}`))
  },
}
