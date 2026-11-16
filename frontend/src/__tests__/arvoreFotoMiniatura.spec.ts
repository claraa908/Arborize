import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { nextTick } from 'vue'
import { mount } from '@vue/test-utils'
import { inventarioService } from '@/services/inventarioService'
import type { FotoCapaResponseDTO } from '@/types/inventario'
import ArvoreFotoMiniatura from '@/components/inventario/ArvoreFotoMiniatura.vue'

class ObservadorTeste {
  static instancias: ObservadorTeste[] = []
  desconectado = false
  private readonly callback: IntersectionObserverCallback

  constructor(callback: IntersectionObserverCallback) {
    this.callback = callback
    ObservadorTeste.instancias.push(this)
  }

  observe = vi.fn()
  disconnect = vi.fn(() => {
    this.desconectado = true
  })
  unobserve = vi.fn()
  root = null
  rootMargin = '0px'
  thresholds = [0]

  revelar() {
    this.callback(
      [{ isIntersecting: true } as IntersectionObserverEntry],
      this as unknown as IntersectionObserver,
    )
  }
}

const capa: FotoCapaResponseDTO = { id: 8, contentType: 'image/jpeg' }

async function estabilizar() {
  for (let indice = 0; indice < 5; indice++) {
    await Promise.resolve()
    await nextTick()
  }
}

beforeEach(() => {
  ObservadorTeste.instancias = []
  vi.stubGlobal('IntersectionObserver', ObservadorTeste)
  vi.spyOn(URL, 'createObjectURL').mockImplementation(() => 'blob:foto-teste')
  vi.spyOn(URL, 'revokeObjectURL').mockImplementation(() => undefined)
})

afterEach(() => {
  vi.restoreAllMocks()
  vi.unstubAllGlobals()
})

describe('T27: miniaturas protegidas', () => {
  it('mostra ilustração identificada sem consultar conteúdo quando não há capa', () => {
    const recuperar = vi.spyOn(inventarioService, 'recuperarFoto')
    const wrapper = mount(ArvoreFotoMiniatura, { props: { arvoreId: 42, fotoCapa: null } })
    expect(wrapper.text()).toContain('Sem fotografia')
    expect(recuperar).not.toHaveBeenCalled()
  })

  it('busca a imagem somente quando a miniatura da linha entra na área visível', async () => {
    const recuperar = vi
      .spyOn(inventarioService, 'recuperarFoto')
      .mockResolvedValue(new Blob(['foto']))
    const wrapper = mount(ArvoreFotoMiniatura, { props: { arvoreId: 42, fotoCapa: capa } })
    expect(recuperar).not.toHaveBeenCalled()
    ObservadorTeste.instancias[0]!.revelar()
    await estabilizar()
    expect(recuperar).toHaveBeenCalledExactlyOnceWith(42, 8, { suppressGlobalErrorToast: true })
    expect(wrapper.find('img').attributes('src')).toBe('blob:foto-teste')
    expect(wrapper.find('img').attributes('alt')).toBe('Fotografia da árvore 42')
  })

  it.each([401, 403, 404])('separa erro HTTP %s de ausência de fotografia', async (status) => {
    const recuperar = vi.spyOn(inventarioService, 'recuperarFoto').mockRejectedValue({
      status,
      acessoNegado: status === 401 || status === 403,
    })
    const wrapper = mount(ArvoreFotoMiniatura, {
      props: { arvoreId: 42, fotoCapa: capa, carregarImediatamente: true },
    })
    await estabilizar()
    expect(recuperar).toHaveBeenCalledExactlyOnceWith(42, 8, { suppressGlobalErrorToast: true })
    expect(wrapper.text()).toContain('Fotografia indisponível')
    expect(wrapper.text()).not.toContain('Sem fotografia')
    expect(URL.revokeObjectURL).not.toHaveBeenCalled()
  })

  it('trata imagem ilegível como indisponível e revoga o objeto temporário', async () => {
    vi.spyOn(inventarioService, 'recuperarFoto').mockResolvedValue(new Blob(['não é uma imagem']))
    const wrapper = mount(ArvoreFotoMiniatura, {
      props: { arvoreId: 42, fotoCapa: capa, carregarImediatamente: true },
    })
    await estabilizar()
    await wrapper.find('img').trigger('error')
    expect(wrapper.text()).toContain('Fotografia indisponível')
    expect(URL.revokeObjectURL).toHaveBeenCalledWith('blob:foto-teste')
  })

  it('ignora resposta atrasada após desmontagem e revoga seu Blob URL', async () => {
    let concluir!: (conteudo: Blob) => void
    vi.spyOn(inventarioService, 'recuperarFoto').mockReturnValue(
      new Promise((resolve) => {
        concluir = resolve
      }),
    )
    const wrapper = mount(ArvoreFotoMiniatura, {
      props: { arvoreId: 42, fotoCapa: capa, carregarImediatamente: true },
    })
    wrapper.unmount()
    concluir(new Blob(['foto']))
    await estabilizar()
    expect(URL.revokeObjectURL).toHaveBeenCalledWith('blob:foto-teste')
  })

  it('revoga URL substituída quando a árvore muda de capa', async () => {
    const recuperar = vi
      .spyOn(inventarioService, 'recuperarFoto')
      .mockResolvedValue(new Blob(['foto']))
    const wrapper = mount(ArvoreFotoMiniatura, {
      props: { arvoreId: 42, fotoCapa: capa, carregarImediatamente: true },
    })
    await estabilizar()
    await wrapper.setProps({ arvoreId: 43, fotoCapa: { id: 9, contentType: 'image/png' } })
    await estabilizar()
    expect(recuperar).toHaveBeenCalledWith(43, 9, { suppressGlobalErrorToast: true })
    expect(URL.revokeObjectURL).toHaveBeenCalledWith('blob:foto-teste')
    expect(wrapper.find('img').attributes('src')).toBe('blob:foto-teste')
  })

  it('rotula indisponível quando o navegador não oferece IntersectionObserver', () => {
    vi.stubGlobal('IntersectionObserver', undefined)
    const wrapper = mount(ArvoreFotoMiniatura, { props: { arvoreId: 42, fotoCapa: capa } })
    expect(wrapper.text()).toContain('Fotografia indisponível')
    expect(wrapper.text()).not.toContain('Sem fotografia')
  })
})
