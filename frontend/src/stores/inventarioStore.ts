import { defineStore } from 'pinia'
import { reactive, ref } from 'vue'
import { inventarioService, interpretarErroInventario } from '@/services/inventarioService'
import type {
  ArvoreAtualizacaoRequestDTO,
  ArvoreCadastroRequestDTO,
  ArvorePaginaResponseDTO,
  ArvoreResponseDTO,
  ArvoreResumoResponseDTO,
  BairroResumoResponseDTO,
  ErroInventario,
  InventarioFiltros,
  EspecieIdentificacaoResponseDTO,
} from '@/types/inventario'

type Operacao = 'listagem' | 'mapa' | 'ficha' | 'salvamento' | 'bairros'

export const useInventarioStore = defineStore('inventario', () => {
  // null distingue dados não consultados/indisponíveis de uma consulta vazia bem-sucedida.
  const pagina = ref<ArvorePaginaResponseDTO | null>(null)
  const registrosMapa = ref<ArvoreResumoResponseDTO[] | null>(null)
  const arvoreSelecionada = ref<ArvoreResponseDTO | null>(null)
  const filtros = ref<InventarioFiltros>({})
  const opcoesEspecies = ref<EspecieIdentificacaoResponseDTO[]>([])
  const opcoesBairros = ref<BairroResumoResponseDTO[]>([])
  const opcoesCarregadas = ref(false)
  const carregando = reactive<Record<Operacao, boolean>>({
    listagem: false,
    mapa: false,
    ficha: false,
    salvamento: false,
    bairros: false,
  })
  const erros = reactive<Record<Operacao, ErroInventario | null>>({
    listagem: null,
    mapa: null,
    ficha: null,
    salvamento: null,
    bairros: null,
  })

  async function executar<T>(operacao: Operacao, acao: () => Promise<T>): Promise<T> {
    carregando[operacao] = true
    erros[operacao] = null
    try {
      return await acao()
    } catch (error) {
      const falha = interpretarErroInventario(error)
      erros[operacao] = {
        status: falha.status,
        message: falha.message,
        acessoNegado: falha.acessoNegado,
        problem: falha.problem,
      }
      throw falha
    } finally {
      carregando[operacao] = false
    }
  }

  function invalidarResultados() {
    pagina.value = null
    registrosMapa.value = null
    erros.listagem = null
    erros.mapa = null
  }

  // Usar esta ação ao aplicar critérios. Nenhuma ação/watch dispara consultas automáticas.
  function setFiltros(novos: InventarioFiltros) {
    filtros.value = { ...novos }
    invalidarResultados()
    arvoreSelecionada.value = null
    erros.ficha = null
  }

  function limparFiltros() {
    setFiltros({})
  }

  async function fetchArvores(page = 0, size = 20): Promise<ArvorePaginaResponseDTO> {
    pagina.value = null
    return executar('listagem', async () => {
      const resultado = await inventarioService.getAll({ ...filtros.value, page, size })
      pagina.value = resultado
      return resultado
    })
  }

  async function fetchMapa(): Promise<ArvoreResumoResponseDTO[]> {
    registrosMapa.value = null
    return executar('mapa', async () => {
      const criterios = { ...filtros.value }
      const resultado = await inventarioService.getMapa(criterios)
      registrosMapa.value = resultado
      if (
        !criterios.search?.trim() &&
        !criterios.bairro?.trim() &&
        criterios.especieId == null &&
        criterios.condicao == null
      ) {
        const especies = new Map<number, EspecieIdentificacaoResponseDTO>()
        for (const arvore of resultado) {
          especies.set(arvore.especie.id, arvore.especie)
        }
        opcoesEspecies.value = [...especies.values()].sort((a, b) =>
          a.nomePopular.localeCompare(b.nomePopular, 'pt-BR'),
        )
        opcoesCarregadas.value = true
      }
      return resultado
    })
  }

  async function fetchArvore(id: number): Promise<ArvoreResponseDTO> {
    arvoreSelecionada.value = null
    return executar('ficha', async () => {
      const resultado = await inventarioService.getById(id)
      arvoreSelecionada.value = resultado
      return resultado
    })
  }

  async function fetchBairros(): Promise<BairroResumoResponseDTO[]> {
    opcoesBairros.value = []
    return executar('bairros', async () => {
      const resultado = await inventarioService.getBairros()
      opcoesBairros.value = resultado
        .filter((bairro) => bairro.ativo)
        .sort((a, b) => a.nome.localeCompare(b.nome, 'pt-BR'))
      return opcoesBairros.value
    })
  }

  async function createArvore(payload: ArvoreCadastroRequestDTO): Promise<ArvoreResponseDTO> {
    return executar('salvamento', async () => {
      const criada = await inventarioService.create(payload)
      arvoreSelecionada.value = criada
      invalidarResultados()
      return criada
    })
  }

  async function updateArvore(
    id: number,
    payload: ArvoreAtualizacaoRequestDTO,
  ): Promise<ArvoreResponseDTO> {
    return executar('salvamento', async () => {
      const atualizada = await inventarioService.update(id, payload)
      arvoreSelecionada.value = atualizada
      invalidarResultados()
      return atualizada
    })
  }

  return {
    pagina,
    registrosMapa,
    arvoreSelecionada,
    filtros,
    opcoesEspecies,
    opcoesBairros,
    opcoesCarregadas,
    carregando,
    erros,
    setFiltros,
    limparFiltros,
    fetchArvores,
    fetchMapa,
    fetchArvore,
    fetchBairros,
    createArvore,
    updateArvore,
  }
})
