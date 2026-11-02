// @vitest-environment node
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { defineComponent, h, nextTick, reactive } from 'vue'
import { inventarioService } from '@/services/inventarioService'
import type { FotoCapaResponseDTO } from '@/types/inventario'
import ArvoreFotoMiniatura from '@/components/inventario/ArvoreFotoMiniatura.vue'
import { ElementoTeste, encontrar, renderer } from './mapaTestHost'

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
    this.callback([{ isIntersecting: true } as IntersectionObserverEntry], this as unknown as IntersectionObserver)
  }
}

const apps: Array<{ unmount: () => void }> = []
const capa: FotoCapaResponseDTO = { id: 8, contentType: 'image/jpeg' }

function montar(props: {
  arvoreId: number
  fotoCapa: FotoCapaResponseDTO | null
  carregarImediatamente?: boolean
}) {
  const raiz = new ElementoTeste('ROOT')
  const valores = reactive({ ...props })
  const app = renderer.createApp(
    defineComponent({
      setup() {
        return () => h(ArvoreFotoMiniatura, valores)
      },
    }),
  )
  app.mount(raiz)
  apps.push(app)
  return { raiz, app, atualizar: (proximos: Partial<typeof props>) => Object.assign(valores, proximos) }
}

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
  apps.splice(0).forEach((app) => app.unmount())
  vi.restoreAllMocks()
  vi.unstubAllGlobals()
})

describe('T27: miniaturas protegidas', () => {
  it('mostra ilustração identificada sem consultar conteúdo quando não há capa', () => {
    const recuperar = vi.spyOn(inventarioService, 'recuperarFoto')
    const { raiz } = montar({ arvoreId: 42, fotoCapa: null })
    expect(raiz.textContent).toContain('Sem fotografia')
    expect(recuperar).not.toHaveBeenCalled()
  })

  it('busca a imagem somente quando a miniatura da linha entra na área visível', async () => {
    const recuperar = vi.spyOn(inventarioService, 'recuperarFoto').mockResolvedValue(new Blob(['foto']))
    const { raiz } = montar({ arvoreId: 42, fotoCapa: capa })
    expect(recuperar).not.toHaveBeenCalled()
    expect(ObservadorTeste.instancias).toHaveLength(1)
    ObservadorTeste.instancias[0]!.revelar()
    await estabilizar()
    expect(recuperar).toHaveBeenCalledExactlyOnceWith(42, 8, { suppressGlobalErrorToast: true })
    expect(encontrar(raiz, (no) => no.tagName === 'IMG').props.src).toBe('blob:foto-teste')
    expect(encontrar(raiz, (no) => no.tagName === 'IMG').props.alt).toBe('Fotografia da árvore 42')
  })

  it.each([401, 403, 404])(
    'separa erro HTTP %s de ausência de fotografia',
    async (status) => {
      const recuperar = vi.spyOn(inventarioService, 'recuperarFoto').mockRejectedValue({
        status,
        acessoNegado: status === 401 || status === 403,
      })
      const { raiz } = montar({ arvoreId: 42, fotoCapa: capa, carregarImediatamente: true })
      await estabilizar()
      expect(recuperar).toHaveBeenCalledExactlyOnceWith(42, 8, { suppressGlobalErrorToast: true })
      expect(raiz.textContent).toContain('Fotografia indisponível')
      expect(raiz.textContent).not.toContain('Sem fotografia')
      expect(URL.revokeObjectURL).not.toHaveBeenCalled()
    },
  )

  it('trata imagem ilegível como indisponível e revoga o objeto temporário', async () => {
    vi.spyOn(inventarioService, 'recuperarFoto').mockResolvedValue(new Blob(['não é uma imagem']))
    const { raiz } = montar({ arvoreId: 42, fotoCapa: capa, carregarImediatamente: true })
    await estabilizar()
    encontrar(raiz, (no) => no.tagName === 'IMG').dispatch('error')
    await nextTick()
    expect(raiz.textContent).toContain('Fotografia indisponível')
    expect(URL.revokeObjectURL).toHaveBeenCalledWith('blob:foto-teste')
  })

  it('ignora resposta atrasada após desmontagem e revoga seu Blob URL', async () => {
    let concluir!: (conteudo: Blob) => void
    vi.spyOn(inventarioService, 'recuperarFoto').mockReturnValue(
      new Promise((resolve) => {
        concluir = resolve
      }),
    )
    const { app, raiz } = montar({ arvoreId: 42, fotoCapa: capa, carregarImediatamente: true })
    app.unmount()
    apps.splice(apps.indexOf(app), 1)
    concluir(new Blob(['foto']))
    await estabilizar()
    expect(raiz.textContent).not.toContain('Fotografia indisponível')
    expect(URL.revokeObjectURL).toHaveBeenCalledWith('blob:foto-teste')
  })

  it('revoga URL substituída quando a árvore muda de capa', async () => {
    const recuperar = vi.spyOn(inventarioService, 'recuperarFoto').mockResolvedValue(new Blob(['foto']))
    const { raiz, atualizar } = montar({ arvoreId: 42, fotoCapa: capa, carregarImediatamente: true })
    await estabilizar()
    atualizar({ arvoreId: 43, fotoCapa: { id: 9, contentType: 'image/png' } })
    await estabilizar()
    expect(recuperar).toHaveBeenCalledWith(43, 9, { suppressGlobalErrorToast: true })
    expect(URL.revokeObjectURL).toHaveBeenCalledWith('blob:foto-teste')
    expect(encontrar(raiz, (no) => no.tagName === 'IMG').props.src).toBe('blob:foto-teste')
  })

  it('rotula indisponível quando o navegador não oferece IntersectionObserver', () => {
    vi.stubGlobal('IntersectionObserver', undefined)
    const { raiz } = montar({ arvoreId: 42, fotoCapa: capa })
    expect(raiz.textContent).toContain('Fotografia indisponível')
    expect(raiz.textContent).not.toContain('Sem fotografia')
  })
})
