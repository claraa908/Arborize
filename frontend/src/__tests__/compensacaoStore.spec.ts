import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { useCompensacaoStore } from '@/stores/compensacaoStore'
import { compensacaoService } from '@/services/compensacaoService'

vi.mock('@/services/compensacaoService', () => ({
  compensacaoService: {
    listar: vi.fn(), consultar: vi.fn(), criar: vi.fn(), atualizar: vi.fn(), atualizarStatus: vi.fn(), registrarEntrega: vi.fn()
  }
}))

const termo = {
  id: 41, razaoSocial: 'Empresa de demonstração', cnpj: '12345678000190', numeroTermo: 'TC-41',
  dataAssinatura: '', prazoEntrega: '', quantidadeAcordada: 100, quantidadeEntregue: 20,
  status: 'PARCIALMENTE_DOADO' as const, arquivoTermo: 'termo.pdf', entregas: []
}

describe('store de compensação ambiental', () => {
  beforeEach(() => { setActivePinia(createPinia()); vi.resetAllMocks() })

  it('carrega termos e encerra o estado de carregamento', async () => {
    vi.mocked(compensacaoService.listar).mockResolvedValue([termo])
    const store = useCompensacaoStore()
    expect(await store.carregarLista()).toBe(true)
    expect(store.termos).toEqual([termo])
    expect(store.isLoading).toBe(false)
    expect(store.error).toBeNull()
  })

  it('mantém erro explícito sem converter falha em lista vazia bem-sucedida', async () => {
    vi.mocked(compensacaoService.listar).mockRejectedValue(new Error('API indisponível'))
    const store = useCompensacaoStore()
    expect(await store.carregarLista()).toBe(false)
    expect(store.termos).toEqual([])
    expect(store.error).toBe('API indisponível')
    expect(store.isLoading).toBe(false)
  })

  it('guarda o novo estado devolvido pela API', async () => {
    vi.mocked(compensacaoService.atualizarStatus).mockResolvedValue({ ...termo, status: 'TOTALMENTE_DOADO' })
    const store = useCompensacaoStore()
    expect(await store.atualizarStatus(41, 'TOTALMENTE_DOADO')).toBe(true)
    expect(store.termoAtual?.status).toBe('TOTALMENTE_DOADO')
  })
})
