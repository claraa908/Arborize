import { computed, reactive, ref } from 'vue'
import { useInventarioStore } from '@/stores/inventarioStore'
import type { InventarioFiltros } from '@/types/inventario'

// Interações da consulta, sem HTTP direto e sem efeitos de montagem implícitos.
export function useInventarioConsulta() {
  const store = useInventarioStore()
  const criterios = reactive<InventarioFiltros>({ ...store.filtros })
  const tamanhoPagina = ref(store.pagina?.size ?? 20)
  const idSelecionado = ref<number | null>(null)
  const consultando = computed(() => store.carregando.listagem || store.carregando.mapa)
  const filtrosAtivos = computed(() =>
    Boolean(
      store.filtros.search?.trim() ||
      store.filtros.bairro?.trim() ||
      store.filtros.especieId != null ||
      store.filtros.condicao != null,
    ),
  )
  const acessoNegado = computed(() =>
    [store.erros.listagem, store.erros.mapa].find((erro) => erro?.acessoNegado),
  )

  async function consultar() {
    if (consultando.value) return
    // Mesmos critérios, endpoints distintos. Só a tabela recebe page/size.
    await Promise.allSettled([store.fetchArvores(0, tamanhoPagina.value), store.fetchMapa()])
  }

  async function aplicarFiltros() {
    if (consultando.value) return
    store.setFiltros({ ...criterios })
    idSelecionado.value = null
    await consultar()
  }

  async function limparFiltros() {
    if (consultando.value) return
    Object.assign(criterios, {
      search: '',
      bairro: '',
      especieId: undefined,
      condicao: undefined,
    })
    store.limparFiltros()
    idSelecionado.value = null
    await consultar()
  }

  async function mudarPagina(page: number) {
    const atual = store.pagina
    if (consultando.value || !atual || page < 0 || page >= atual.totalPages) return
    try {
      await store.fetchArvores(page, tamanhoPagina.value)
    } catch {
      /* Erro explícito na store; não converter em resultado vazio. */
    }
  }

  async function mudarTamanho() {
    if (consultando.value) return
    try {
      await store.fetchArvores(0, tamanhoPagina.value)
    } catch {
      /* O mapa mantém o conjunto completo dos mesmos filtros. */
    }
  }

  return {
    store,
    criterios,
    tamanhoPagina,
    idSelecionado,
    consultando,
    filtrosAtivos,
    acessoNegado,
    consultar,
    aplicarFiltros,
    limparFiltros,
    mudarPagina,
    mudarTamanho,
  }
}
