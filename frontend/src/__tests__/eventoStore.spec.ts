// @vitest-environment node
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { eventoService } from '@/services/eventoService'
import { useFeedbackStore } from '@/stores/feedbackStore'
import { useEventoStore } from '@/stores/eventoStore'
import type { EventoDTO } from '@/types/eventos'

const evento: EventoDTO = {
  id: 7,
  nome: 'Mutirão de teste',
  descricao: 'Ação demonstrativa',
  dataInicio: '2026-04-01',
  dataTermino: '2026-04-01',
  localConcentracao: 'Praça de teste',
  horarioInicio: '08:00',
  horarioTermino: '11:00',
  status: 'PLANEJADO',
}

beforeEach(() => {
  vi.restoreAllMocks()
  setActivePinia(createPinia())
})

describe('eventoStore', () => {
  it('carrega eventos e limpa o estado de carregamento', async () => {
    const store = useEventoStore()
    const getAll = vi.spyOn(eventoService, 'getAll').mockResolvedValue([evento])

    await store.fetchEventos()

    expect(getAll).toHaveBeenCalledExactlyOnceWith()
    expect(store.eventos).toEqual([evento])
    expect(store.isLoading).toBe(false)
  })

  it('notifica quando a listagem falha e mantém os eventos carregados', async () => {
    const store = useEventoStore()
    const feedback = useFeedbackStore()
    const toast = vi.spyOn(feedback, 'showToast')
    store.eventos = [evento]
    vi.spyOn(eventoService, 'getAll').mockRejectedValue(new Error('offline'))

    await store.fetchEventos()

    expect(store.eventos).toEqual([evento])
    expect(store.isLoading).toBe(false)
    expect(toast).toHaveBeenCalledWith(expect.stringContaining('Erro'), 'error')
  })

  it('cria e atualiza eventos, recarregando a lista após cada operação', async () => {
    const store = useEventoStore()
    const feedback = useFeedbackStore()
    const toast = vi.spyOn(feedback, 'showToast')
    const create = vi.spyOn(eventoService, 'create').mockResolvedValue(evento)
    const update = vi.spyOn(eventoService, 'update').mockResolvedValue({ ...evento, status: 'CONFIRMADO' })
    const getAll = vi.spyOn(eventoService, 'getAll').mockResolvedValue([evento])
    const payload = {
      nome: evento.nome,
      descricao: evento.descricao,
      imagem: null,
      dataInicio: evento.dataInicio,
      dataTermino: evento.dataTermino,
      local: evento.localConcentracao,
      horarioInicio: evento.horarioInicio,
      horarioTermino: evento.horarioTermino,
    }

    expect(await store.createEvento(payload)).toBe(true)
    expect(create).toHaveBeenCalledExactlyOnceWith(payload)
    expect(await store.updateEvento(7, { ...payload, imagem: null, status: 'CONFIRMADO' })).toBe(true)
    expect(update).toHaveBeenCalledExactlyOnceWith(7, { ...payload, imagem: null, status: 'CONFIRMADO' })
    expect(getAll).toHaveBeenCalledTimes(2)
    expect(toast).toHaveBeenCalledWith(expect.stringContaining('cadastrado'), 'success', 4000, 'Sucesso')
    expect(toast).toHaveBeenCalledWith(expect.stringContaining('atualizado'), 'success', 4000, 'Sucesso')
    expect(store.isLoading).toBe(false)
  })

  it('retorna falso e não recarrega a lista quando criar ou atualizar falha', async () => {
    const store = useEventoStore()
    const feedback = useFeedbackStore()
    const toast = vi.spyOn(feedback, 'showToast')
    const create = vi.spyOn(eventoService, 'create').mockRejectedValue(new Error('create failed'))
    const update = vi.spyOn(eventoService, 'update').mockRejectedValue(new Error('update failed'))
    const getAll = vi.spyOn(eventoService, 'getAll')
    const payload = {
      nome: evento.nome,
      descricao: evento.descricao,
      imagem: null,
      dataInicio: evento.dataInicio,
      dataTermino: evento.dataTermino,
      local: evento.localConcentracao,
      horarioInicio: evento.horarioInicio,
      horarioTermino: evento.horarioTermino,
    }

    expect(await store.createEvento(payload)).toBe(false)
    expect(await store.updateEvento(7, payload)).toBe(false)
    expect(create).toHaveBeenCalledOnce()
    expect(update).toHaveBeenCalledOnce()
    expect(getAll).not.toHaveBeenCalled()
    expect(toast).toHaveBeenCalledTimes(2)
    expect(store.isLoading).toBe(false)
  })

  it('exige confirmação antes de excluir e remove o evento da lista', async () => {
    const store = useEventoStore()
    const feedback = useFeedbackStore()
    const confirm = vi.spyOn(feedback, 'showConfirm')
    const remove = vi.spyOn(eventoService, 'delete').mockResolvedValue()
    store.eventos = [evento]

    confirm.mockResolvedValueOnce(false)
    expect(await store.removeEvento(7)).toBe(false)
    expect(remove).not.toHaveBeenCalled()
    expect(store.eventos).toHaveLength(1)

    confirm.mockResolvedValueOnce(true)
    expect(await store.removeEvento(7)).toBe(true)
    expect(remove).toHaveBeenCalledExactlyOnceWith(7)
    expect(store.eventos).toEqual([])
  })

  it('mantém o evento e exibe erro se a exclusão falhar', async () => {
    const store = useEventoStore()
    const feedback = useFeedbackStore()
    vi.spyOn(feedback, 'showConfirm').mockResolvedValue(true)
    const toast = vi.spyOn(feedback, 'showToast')
    vi.spyOn(eventoService, 'delete').mockRejectedValue(new Error('delete failed'))
    store.eventos = [evento]

    expect(await store.removeEvento(7)).toBe(false)
    expect(store.eventos).toEqual([evento])
    expect(store.isLoading).toBe(false)
    expect(toast).toHaveBeenCalledWith(expect.stringContaining('excluir'), 'error')
  })

  it('expõe estado de erro quando fetchEventos falha e limpa ao recuperar', async () => {
    const store = useEventoStore()
    vi.spyOn(eventoService, 'getAll')
      .mockRejectedValueOnce(new Error('offline'))
      .mockResolvedValueOnce([evento])

    await store.fetchEventos()
    expect(store.error).toContain('Não foi possível carregar')

    await store.fetchEventos()
    expect(store.error).toBeNull()
    expect(store.eventos).toEqual([evento])
  })

  it('carrega evento individual, diferencia 404 de erro genérico e ignora respostas obsoletas', async () => {
    const store = useEventoStore()
    const evento2: EventoDTO = { ...evento, id: 8, nome: 'Segundo evento' }

    let resolvePrimeiro!: (value: EventoDTO) => void
    const primeiraPromessa = new Promise<EventoDTO>((resolve) => {
      resolvePrimeiro = resolve
    })

    const getById = vi
      .spyOn(eventoService, 'getById')
      .mockReturnValueOnce(primeiraPromessa)
      .mockResolvedValueOnce(evento2)

    const req1 = store.fetchEventoById(7)
    const req2 = store.fetchEventoById(8)

    await req2
    resolvePrimeiro(evento)
    await req1

    expect(getById).toHaveBeenCalledTimes(2)
    expect(store.eventoAtual).toEqual(evento2)
    expect(store.isLoadingDetail).toBe(false)
    expect(store.detailError).toBeNull()

    getById.mockRejectedValueOnce({ response: { status: 404 } })
    expect(await store.fetchEventoById(999)).toBeNull()
    expect(store.eventoAtual).toBeNull()
    expect(store.detailStatus).toBe(404)
    expect(store.detailError).toContain('não encontrado')

    getById.mockRejectedValueOnce(new Error('network'))
    expect(await store.fetchEventoById(1000)).toBeNull()
    expect(store.detailStatus).toBeNull()
    expect(store.detailError).toContain('Não foi possível carregar')
  })
})
