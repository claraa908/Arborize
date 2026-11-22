import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import MovimentacaoMudaForm from '@/components/viveiro/MovimentacaoMudaForm.vue'
import MovimentacoesTable from '@/components/viveiro/MovimentacoesTable.vue'
import type { EspecieDTO } from '@/types/viveiro'
import type { MovimentacaoMudaDTO } from '@/types/movimentacaoMuda'

const especies: EspecieDTO[] = [{
  id: 7,
  nomePopular: 'Ipê-amarelo',
  nomeCientifico: 'Handroanthus albus',
  categoria: 'Nativa da Caatinga',
  porteArvore: 'Médio',
  recomendadaCalcadas: true,
  mudasDisponiveis: 25,
}]

const movimento: MovimentacaoMudaDTO = {
  id: 13,
  tipo: 'SAIDA_PLANTIO',
  especieId: 7,
  especieNomePopular: 'Ipê-amarelo',
  especieNomeCientifico: 'Handroanthus albus',
  quantidade: 5,
  data: '2026-05-10',
  destino: 'Praça central',
  responsavel: 'Equipe Verde',
  observacoes: 'Ação comunitária',
  comprovanteNome: 'guia.pdf',
}

describe('formulário de movimentação de mudas', () => {
  it('exibe o destino como obrigatório apenas para saída de plantio', async () => {
    const wrapper = mount(MovimentacaoMudaForm, { props: { especies } })
    expect(wrapper.find('#movimentacao-destino').exists()).toBe(false)
    await wrapper.findAll('button[type="button"]')[1]?.trigger('click')
    expect(wrapper.find('#movimentacao-destino').exists()).toBe(true)

    await wrapper.find('form').trigger('submit')
    expect(wrapper.text()).toContain('Informe a finalidade ou o destino do plantio.')
    expect(wrapper.emitted('save')).toBeUndefined()

    await wrapper.get('#movimentacao-especie').setValue('7')
    await wrapper.get('#movimentacao-quantidade').setValue('4')
    await wrapper.get('#movimentacao-destino').setValue('Praça central')
    await wrapper.find('form').trigger('submit')
    const payload = wrapper.emitted('save')?.[0]?.[0] as { quantidade: number; destino: string; especieId: number }
    expect(payload).toMatchObject({ quantidade: 4, destino: 'Praça central', especieId: 7 })
  })

  it('rejeita quantidade fracionária e comprovante maior que 10 MB', async () => {
    const wrapper = mount(MovimentacaoMudaForm, { props: { especies } })
    await wrapper.get('#movimentacao-especie').setValue('7')
    await wrapper.get('#movimentacao-quantidade').setValue('2.5')
    await wrapper.find('form').trigger('submit')
    expect(wrapper.text()).toContain('Informe uma quantidade inteira maior que zero.')
    expect(wrapper.emitted('save')).toBeUndefined()

    const arquivoGrande = new File([new Uint8Array(10 * 1024 * 1024 + 1)], 'comprovante.pdf', { type: 'application/pdf' })
    const inputArquivo = wrapper.get('input[type="file"]')
    Object.defineProperty(inputArquivo.element, 'files', { value: [arquivoGrande], configurable: true })
    await inputArquivo.trigger('change')
    expect(wrapper.text()).toContain('O arquivo deve ter no máximo 10 MB.')
  })

  it('preenche os campos na edição e preserva o nome do comprovante atual', () => {
    const wrapper = mount(MovimentacaoMudaForm, { props: { especies, initialMovement: movimento } })
    expect((wrapper.get('#movimentacao-quantidade').element as HTMLInputElement).value).toBe('5')
    expect((wrapper.get('#movimentacao-destino').element as HTMLInputElement).value).toBe('Praça central')
    expect(wrapper.text()).toContain('guia.pdf')
    expect(wrapper.text()).toContain('Estoque atual: 25 mudas')
  })
})

describe('histórico de movimentações', () => {
  it('mostra estado vazio, emite filtros e oferece registro', async () => {
    const wrapper = mount(MovimentacoesTable, { props: { pagina: { content: [], number: 0, totalElements: 0, totalPages: 0 }, especies } })
    expect(wrapper.text()).toContain('Nenhuma movimentação encontrada.')
    await wrapper.get('input[type="search"]').setValue('Praça')
    await wrapper.get('form').trigger('submit')
    expect(wrapper.emitted('filter')?.[0]?.[0]).toMatchObject({ busca: 'Praça' })
    await wrapper.get('table tbody button').trigger('click')
    expect(wrapper.emitted('create')).toHaveLength(1)
  })

  it('apresenta carregamento, erro com nova tentativa e navegação entre páginas', async () => {
    const carregando = mount(MovimentacoesTable, {
      props: { pagina: null, especies, loading: true },
    })
    expect(carregando.find('[role="status"]').exists()).toBe(true)
    expect(carregando.find('table').exists()).toBe(false)

    const falha = mount(MovimentacoesTable, {
      props: { pagina: null, especies, error: 'Falha ao carregar.' },
    })
    expect(falha.get('[role="alert"]').text()).toContain('Falha ao carregar.')
    await falha.get('[role="alert"] button').trigger('click')
    expect(falha.emitted('retry')).toHaveLength(1)

    const paginada = mount(MovimentacoesTable, {
      props: {
        pagina: { content: [movimento], number: 0, totalElements: 11, totalPages: 2 },
        especies,
      },
    })
    await paginada.findAll('button').find((button) => button.text() === 'Próxima')?.trigger('click')
    expect(paginada.emitted('page')?.[0]).toEqual([
      expect.objectContaining({ tipo: '', especieId: '' }),
      1,
    ])
  })

  it('apresenta registro, comprovante e ação acessível de edição', async () => {
    const wrapper = mount(MovimentacoesTable, { props: { pagina: { content: [movimento], number: 0, totalElements: 1, totalPages: 1 }, especies } })
    expect(wrapper.text()).toContain('Saída para plantio')
    expect(wrapper.text()).toContain('10/05/2026')
    await wrapper.get('[aria-label="Abrir comprovante da movimentação de Ipê-amarelo"]').trigger('click')
    await wrapper.get('[aria-label="Editar movimentação de Ipê-amarelo"]').trigger('click')
    expect(wrapper.emitted('proof')?.[0]?.[0]).toBe(13)
    expect(wrapper.emitted('edit')?.[0]?.[0]).toEqual(movimento)
  })
})
