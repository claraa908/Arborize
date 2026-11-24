import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createMemoryHistory } from 'vue-router'
import TopBar from '@/components/layout/TopBar.vue'
import { createAppRouter } from '@/router'
import { api } from '@/services/api'
import { sobreService } from '@/services/sobreService'
import SobreNosView from '@/views/public/SobreNosView.vue'
import type { SobreIndicadoresDTO } from '@/types/sobre'

const indicadoresMock: SobreIndicadoresDTO = {
  mudasDoadas: 1240,
  estoqueViveiro: 3850,
  garrafasPetArrecadadas: 14500,
  ninsSubstituidos: 310,
}

beforeEach(() => {
  vi.restoreAllMocks()
  setActivePinia(createPinia())
})

describe('sobreService', () => {
  it('consulta GET /api/sobre/indicadores e retorna os dados tipados', async () => {
    const getSpy = vi.spyOn(api, 'get').mockResolvedValue({ data: indicadoresMock })

    const data = await sobreService.getIndicadores()

    expect(getSpy).toHaveBeenCalledExactlyOnceWith('/api/sobre/indicadores')
    expect(data).toEqual(indicadoresMock)
  })
})

describe('SobreNosView (/sobre)', () => {
  it('exibe blocos institucionais, carrega e formata os 4 indicadores reais em pt-BR', async () => {
    const getIndicadores = vi.spyOn(sobreService, 'getIndicadores').mockResolvedValue(indicadoresMock)
    const pinia = createPinia()
    setActivePinia(pinia)
    const router = createAppRouter(createMemoryHistory())
    await router.push('/sobre')
    await router.isReady()

    const wrapper = mount(SobreNosView, {
      global: { plugins: [pinia, router] },
    })

    expect(wrapper.text()).toContain('Autarquia Municipal do Meio Ambiente de Quixadá')
    expect(wrapper.text()).toContain('Nossa missão')
    expect(wrapper.text()).toContain('O que fazemos')
    expect(wrapper.text()).toContain('Parceiros')
    expect(wrapper.text()).toContain('Fale com a AMMA')
    expect(wrapper.find('[role="status"]').exists()).toBe(true)

    await flushPromises()

    expect(getIndicadores).toHaveBeenCalledOnce()
    expect(wrapper.text()).toContain('1.240')
    expect(wrapper.text()).toContain('3.850')
    expect(wrapper.text()).toContain('14.500')
    expect(wrapper.text()).toContain('310')

    wrapper.unmount()
  })

  it('mantém os blocos institucionais visíveis em erro de API e permite tentar novamente', async () => {
    const getIndicadores = vi
      .spyOn(sobreService, 'getIndicadores')
      .mockRejectedValueOnce(new Error('offline'))
      .mockResolvedValueOnce(indicadoresMock)

    const pinia = createPinia()
    setActivePinia(pinia)
    const router = createAppRouter(createMemoryHistory())
    await router.push('/sobre')
    await router.isReady()

    const wrapper = mount(SobreNosView, {
      global: { plugins: [pinia, router] },
    })
    await flushPromises()

    expect(wrapper.text()).toContain('Autarquia Municipal do Meio Ambiente de Quixadá')
    expect(wrapper.text()).toContain('Indicadores temporariamente indisponíveis')

    const retryBtn = wrapper.findAll('button').find((b) => b.text().includes('Tentar novamente'))
    expect(retryBtn).toBeDefined()
    await retryBtn!.trigger('click')
    await flushPromises()

    expect(getIndicadores).toHaveBeenCalledTimes(2)
    expect(wrapper.text()).toContain('3.850')
    wrapper.unmount()
  })

  it('navega para /sobre ao clicar em Sobre Nós na TopBar e destaca o item como ativo', async () => {
    const pinia = createPinia()
    setActivePinia(pinia)
    const router = createAppRouter(createMemoryHistory())
    await router.push('/')
    await router.isReady()

    const wrapper = mount(TopBar, {
      global: { plugins: [pinia, router] },
    })

    const btnSobre = wrapper.findAll('nav button').find((b) => b.text().trim() === 'Sobre Nós')
    expect(btnSobre).toBeDefined()
    await btnSobre!.trigger('click')
    await flushPromises()

    expect(router.currentRoute.value.name).toBe('sobre')
    expect(btnSobre!.classes()).toContain('bg-amma-green-light')
    wrapper.unmount()
  })
})
