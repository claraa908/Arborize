// @vitest-environment node
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { effectScope, nextTick, ref } from 'vue'
import { inventarioService } from '@/services/inventarioService'
import { useArvoreFicha } from '@/components/inventario/useArvoreFicha'
import { arvore, falhaHttp } from './inventarioFixtures'

vi.mock('@/services/inventarioService', async (importOriginal) => {
  const original = await importOriginal<typeof import('@/services/inventarioService')>()
  return {
    ...original,
    inventarioService: {
      getAll: vi.fn(),
      getMapa: vi.fn(),
      getById: vi.fn(),
      create: vi.fn(),
      update: vi.fn(),
    },
  }
})

let scope: ReturnType<typeof effectScope>

beforeEach(() => {
  setActivePinia(createPinia())
  vi.resetAllMocks()
  scope = effectScope()
})

afterEach(() => scope.stop())

async function deixarPromisesResolverem() {
  await new Promise((resolve) => setTimeout(resolve, 0))
}

describe('useArvoreFicha: consulta por ID e estados', () => {
  it('consulta o ID da rota pela store e expõe o registro retornado', async () => {
    vi.mocked(inventarioService.getById).mockResolvedValue(arvore)
    const id = ref('42')
    const ficha = scope.run(() => useArvoreFicha(id))!

    expect(ficha.carregando.value).toBe(true)
    await deixarPromisesResolverem()

    expect(inventarioService.getById).toHaveBeenCalledExactlyOnceWith(42)
    expect(ficha.arvore.value).toEqual(arvore)
    expect(ficha.erro.value).toBeNull()
    expect(ficha.carregando.value).toBe(false)
  })

  it.each([401, 403])(
    'mantém a ficha vazia e registra acesso negado em HTTP %s',
    async (status) => {
      vi.mocked(inventarioService.getById).mockRejectedValue(falhaHttp(status))
      const ficha = scope.run(() => useArvoreFicha(ref('42')))

      await deixarPromisesResolverem()

      expect(ficha?.arvore.value).toBeNull()
      expect(ficha?.erro.value).toMatchObject({ status, acessoNegado: true })
      expect(ficha?.carregando.value).toBe(false)
    },
  )

  it('mantém o 404 distinto de acesso negado para um registro inexistente', async () => {
    vi.mocked(inventarioService.getById).mockRejectedValue(falhaHttp(404))
    const ficha = scope.run(() => useArvoreFicha(ref('999')))

    await deixarPromisesResolverem()

    expect(ficha?.arvore.value).toBeNull()
    expect(ficha?.erro.value).toMatchObject({ status: 404, acessoNegado: false })
  })

  it('não chama a API para um identificador inválido na rota', async () => {
    const ficha = scope.run(() => useArvoreFicha(ref('abc')))

    expect(ficha?.erro.value?.message).toContain('identificador')
    expect(inventarioService.getById).not.toHaveBeenCalled()
  })

  it('limpa a árvore anterior e ignora respostas atrasadas de outro ID', async () => {
    let resolverAntiga!: (value: typeof arvore) => void
    vi.mocked(inventarioService.getById).mockImplementation((id) => {
      if (id === 42) return new Promise((resolve) => (resolverAntiga = resolve))
      return Promise.resolve({ ...arvore, id: Number(id) })
    })
    const id = ref('42')
    const ficha = scope.run(() => useArvoreFicha(id))!
    expect(ficha.carregando.value).toBe(true)

    id.value = '7'
    await nextTick()
    expect(ficha.arvore.value).toBeNull()
    await deixarPromisesResolverem()
    expect(ficha.arvore.value?.id).toBe(7)

    resolverAntiga(arvore)
    await deixarPromisesResolverem()
    expect(ficha.arvore.value?.id).toBe(7)
  })
})
