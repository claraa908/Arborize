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
})
