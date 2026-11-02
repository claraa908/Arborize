import { api } from '@/services/api'
import type { EventoDTO, CreateEventoPayload, UpdateEventoPayload } from '@/types/eventos'

export const eventoService = {
  async getAll(): Promise<EventoDTO[]> {
    const response = await api.get<EventoDTO[]>('/eventos')
    return response.data
  },

  async getById(id: string | number): Promise<EventoDTO> {
    const response = await api.get<EventoDTO>(`/eventos/${id}`)
    return response.data
  },

  async create(payload: CreateEventoPayload): Promise<EventoDTO> {
    const formData = new FormData()
    formData.append('nome', payload.nome)
    formData.append('descricao', payload.descricao)
    formData.append('dataInicio', payload.dataInicio)
    formData.append('dataTermino', payload.dataTermino)
    formData.append('localConcentracao', payload.local)

    if (payload.destinoFinal) {
      formData.append('destinoFinal', payload.destinoFinal)
    }

    formData.append('horarioInicio', payload.horarioInicio)
    formData.append('horarioTermino', payload.horarioTermino)

    if (payload.descricaoHorarioInicio) {
      formData.append('descricaoPontoPartida', payload.descricaoHorarioInicio)
    }

    if (payload.descricaoHorarioTermino) {
      formData.append('descricaoEncerramento', payload.descricaoHorarioTermino)
    }

    if (payload.imagem) {
      formData.append('banner', payload.imagem)
    }

    // CORREÇÃO: Removido o headers com Content-Type
    const response = await api.post<EventoDTO>('/eventos', formData)

    return response.data
  },

  async update(id: string | number, payload: UpdateEventoPayload): Promise<EventoDTO> {
    const formData = new FormData()
    formData.append('_method', 'PUT')

    formData.append('nome', payload.nome)
    formData.append('descricao', payload.descricao)
    formData.append('dataInicio', payload.dataInicio)
    formData.append('dataTermino', payload.dataTermino)
    formData.append('localConcentracao', payload.local)

    if (payload.destinoFinal) {
      formData.append('destinoFinal', payload.destinoFinal)
    }

    formData.append('horarioInicio', payload.horarioInicio)
    formData.append('horarioTermino', payload.horarioTermino)

    if (payload.descricaoHorarioInicio) {
      formData.append('descricaoPontoPartida', payload.descricaoHorarioInicio)
    }

    if (payload.descricaoHorarioTermino) {
      formData.append('descricaoEncerramento', payload.descricaoHorarioTermino)
    }

    if (payload.status) {
      formData.append('status', payload.status)
    }

    if (payload.imagem) {
      formData.append('banner', payload.imagem)
    }

    // CORREÇÃO 1: Removido o headers com Content-Type
    // CORREÇÃO 2: Alterado de api.put para api.post e ajustado a URL para contornar o erro 405
    // (Certifique-se de que o backend está usando @PostMapping("/editar/{id}"))
    const response = await api.put<EventoDTO>(`/eventos/editar/${id}`, formData)

    return response.data
  },

  async delete(id: string | number): Promise<void> {
    await api.delete(`/eventos/${id}`)
  },

  getBannerUrl(id: string | number): string {
    return `${api.defaults.baseURL}/eventos/${id}/banner`
  },
}
