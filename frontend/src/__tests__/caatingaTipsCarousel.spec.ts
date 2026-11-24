import { mount } from '@vue/test-utils'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import CaatingaTipsCarousel from '@/components/home/CaatingaTipsCarousel.vue'

describe('CaatingaTipsCarousel', () => {
  beforeEach(() => {
    vi.stubGlobal(
      'ResizeObserver',
      class {
        observe() {}
        disconnect() {}
      },
    )
    vi.stubGlobal(
      'matchMedia',
      vi.fn().mockImplementation((query: string) => ({
        matches: query.includes('reduce'),
        media: query,
        onchange: null,
        addListener: vi.fn(),
        removeListener: vi.fn(),
        addEventListener: vi.fn(),
        removeEventListener: vi.fn(),
        dispatchEvent: vi.fn(),
      })),
    )
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('remove o filtro Todas e filtra sem alterar os nomes das categorias', async () => {
    const wrapper = mount(CaatingaTipsCarousel, { global: { stubs: { RouterLink: true } } })
    const buttons = wrapper.findAll('[aria-pressed]')

    expect(wrapper.text()).not.toContain('Todas')
    expect(buttons.map((button) => button.text())).toEqual([
      'Cuidados com a muda',
      'Conheça seu solo',
      'Clima e época',
      'Árvores nativas',
    ])
    expect(wrapper.findAll('[aria-roledescription="slide"]')).toHaveLength(12)

    const soilFilter = wrapper.get('button:nth-child(2)[aria-pressed]')
    await soilFilter.trigger('click')

    expect(wrapper.findAll('[aria-roledescription="slide"]')).toHaveLength(3)
    const firstTip = wrapper.get('[aria-label="1 de 3"]')
    expect(firstTip.get('h3').text()).toBe('Solo arenoso e solto')
    expect(firstTip.text()).not.toContain('Conheça seu solo')

    await soilFilter.trigger('click')

    expect(wrapper.findAll('[aria-roledescription="slide"]')).toHaveLength(12)
  })
})
