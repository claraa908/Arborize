import { createSSRApp } from 'vue'
import { renderToString } from '@vue/server-renderer'
import { describe, expect, it } from 'vitest'
import CompensacaoListaEstado from '@/components/compensacao/CompensacaoListaEstado.vue'
import CompensacaoStatusBadge from '@/components/compensacao/CompensacaoStatusBadge.vue'
import { calcularStatusCompensacao, statusLabel, type CompensacaoStatus } from '@/types/compensacao'

describe('estados visuais de compensação', () => {
  it.each([
    [0, 500, 'EM_ABERTO', 'Em aberto'],
    [120, 500, 'PARCIALMENTE_DOADO', 'Parcialmente doado'],
    [500, 500, 'TOTALMENTE_DOADO', 'Totalmente doado/Fechado'],
    [520, 500, 'TOTALMENTE_DOADO', 'Totalmente doado/Fechado'],
  ] as const)('calcula o status para %i de %i mudas', (entregue, acordado, status, label) => {
    expect(calcularStatusCompensacao(entregue, acordado)).toBe(status)
    expect(statusLabel[status]).toBe(label)
  })

  it.each([
    ['EM_ABERTO', 'Em aberto'],
    ['PARCIALMENTE_DOADO', 'Parcialmente doado'],
    ['TOTALMENTE_DOADO', 'Totalmente doado/Fechado'],
  ] as const)('mostra texto explícito para %s', async (status, label) => {
    const html = await renderToString(createSSRApp(CompensacaoStatusBadge, { status: status as CompensacaoStatus }))
    expect(html).toContain(label)
    expect(html).toContain('role="status"')
  })

  it('distingue carregamento, lista vazia e erro com ação de nova tentativa', async () => {
    const loading = await renderToString(createSSRApp(CompensacaoListaEstado, {
      loading: true, error: null, empty: false,
    }))
    expect(loading).toContain('role="status"')
    expect(loading).toContain('Carregando termos')

    const empty = await renderToString(createSSRApp(CompensacaoListaEstado, {
      loading: false, error: null, empty: true,
    }, { default: () => 'Lista de termos' }))
    expect(empty).toContain('Nenhum termo encontrado')
    expect(empty).not.toContain('Lista de termos')

    const error = await renderToString(createSSRApp(CompensacaoListaEstado, {
      loading: false, error: 'Não foi possível carregar os termos.', empty: false,
    }))
    expect(error).toContain('role="alert"')
    expect(error).toContain('Não foi possível carregar')
    expect(error).toContain('Tentar novamente')
  })
})
