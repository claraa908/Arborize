import { afterEach, describe, expect, it, vi } from 'vitest'
import { createPinia } from 'pinia'
import { flushPromises, mount } from '@vue/test-utils'
import { createMemoryHistory, createRouter } from 'vue-router'
import { api } from '@/services/api'
import { especieService } from '@/services/especieService'
import ViveiroCadastroView from '@/views/auth/viveiro/ViveiroCadastroView.vue'
import type { EspecieDTO } from '@/types/viveiro'

function criarRota(path: string) {
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/viveiro', name: 'viveiro', component: { template: '<div />' } },
      { path: '/viveiro/cadastro', name: 'viveiro-cadastrar', component: ViveiroCadastroView },
      { path: '/viveiro/:id/editar', name: 'viveiro-editar', component: ViveiroCadastroView },
    ],
  })

  return router.push(path).then(() => router)
}

async function montarFormulario(path: string) {
  const router = await criarRota(path)
  const wrapper = mount(ViveiroCadastroView, {
    global: { plugins: [createPinia(), router] },
  })

  return { wrapper, router }
}

const especieExistente: EspecieDTO = {
  id: 7,
  nomePopular: 'Ipê-amarelo',
  nomeCientifico: 'Handroanthus albus',
  categoria: 'Nativa da Caatinga',
  porteArvore: 'Médio',
  recomendadaCalcadas: true,
  mudasDisponiveis: 42,
}

afterEach(() => vi.restoreAllMocks())

describe('formulário de espécies do Viveiro', () => {
  it('não apresenta controle de estoque no cadastro', async () => {
    const { wrapper } = await montarFormulario('/viveiro/cadastro')

    expect(wrapper.find('#mudas-disponiveis').exists()).toBe(false)
    expect(wrapper.text()).not.toContain('Estoque atual de mudas')
    wrapper.unmount()
  })

  it('não apresenta controle de estoque na edição, mesmo ao carregar o saldo existente', async () => {
    vi.spyOn(especieService, 'getById').mockResolvedValue(especieExistente)
    const { wrapper } = await montarFormulario('/viveiro/7/editar')
    await flushPromises()

    expect((wrapper.get('input').element as HTMLInputElement).value).toBe('Ipê-amarelo')
    expect(wrapper.find('#mudas-disponiveis').exists()).toBe(false)
    expect(wrapper.text()).not.toContain('Estoque atual de mudas')
    wrapper.unmount()
  })

  it('envia cadastro sem quantidade de mudas', async () => {
    const post = vi.spyOn(api, 'post').mockResolvedValue({ data: especieExistente } as never)

    await especieService.create({
      nomePopular: 'Ipê-amarelo',
      nomeCientifico: 'Handroanthus albus',
      categoria: 'Nativa da Caatinga',
      porte: 'Médio',
      adequadoCalcada: 'Sim',
      imagem: null,
    })

    const payload = post.mock.calls[0]?.[1] as FormData
    expect(payload.has('mudasDisponiveis')).toBe(false)
  })

  it('envia edição sem quantidade de mudas', async () => {
    const put = vi.spyOn(api, 'put').mockResolvedValue({ data: especieExistente } as never)

    await especieService.update(7, {
      nomePopular: 'Ipê-amarelo',
      nomeCientifico: 'Handroanthus albus',
      categoria: 'Nativa da Caatinga',
      porte: 'Médio',
      adequadoCalcada: 'Sim',
    })

    const payload = put.mock.calls[0]?.[1] as FormData
    expect(payload.has('mudasDisponiveis')).toBe(false)
  })
})
