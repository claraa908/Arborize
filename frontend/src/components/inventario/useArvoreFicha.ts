import { onScopeDispose, ref, watch, type Ref } from 'vue'
import { interpretarErroInventario } from '@/services/inventarioService'
import { useInventarioStore } from '@/stores/inventarioStore'
import type { ArvoreResponseDTO, ErroInventario } from '@/types/inventario'

export function useArvoreFicha(identificador: Ref<string>) {
  const inventario = useInventarioStore()
  const arvore = ref<ArvoreResponseDTO | null>(null)
  const carregando = ref(false)
  const erro = ref<ErroInventario | null>(null)
  let sequencia = 0

  async function carregar(id = identificador.value) {
    const requisicao = ++sequencia
    arvore.value = null
    erro.value = null
    carregando.value = false

    const idNumerico = Number(id)
    if (!/^\d+$/.test(id) || !Number.isSafeInteger(idNumerico) || idNumerico <= 0) {
      erro.value = {
        status: null,
        message: 'O identificador da árvore na rota é inválido.',
        acessoNegado: false,
        problem: null,
      }
      return
    }

    carregando.value = true
    try {
      const resultado = await inventario.fetchArvore(idNumerico)
      if (requisicao !== sequencia) return
      if (resultado.id !== idNumerico) {
        erro.value = {
          status: null,
          message: 'A resposta recebida não corresponde ao identificador solicitado.',
          acessoNegado: false,
          problem: null,
        }
        return
      }
      arvore.value = resultado
    } catch (falha) {
      if (requisicao === sequencia) erro.value = interpretarErroInventario(falha)
    } finally {
      if (requisicao === sequencia) carregando.value = false
    }
  }

  watch(identificador, (id) => void carregar(id), { immediate: true })
  onScopeDispose(() => {
    // Ignora qualquer resposta que chegue depois que a ficha saiu da tela.
    sequencia += 1
  })

  return { arvore, carregando, erro, recarregar: () => carregar() }
}
