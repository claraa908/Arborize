// @vitest-environment node
import { describe, expect, it } from 'vitest'
import {
  eventoPossuiBanner,
  formatarDataEvento,
  formatarHorarioEvento,
  formatarPeriodoEvento,
  isEventoVisivelPublicamente,
  obterStatusVisualEvento,
  ordenarEventosPublicos,
} from '@/components/eventos/eventoPresentation'
import type { EventoDTO } from '@/types/eventos'

const criarEvento = (overrides: Partial<EventoDTO>): EventoDTO => ({
  id: 1,
  nome: 'Mutirão',
  descricao: 'Descrição',
  dataInicio: '2026-06-10',
  dataTermino: '2026-06-10',
  localConcentracao: 'Praça José de Barros',
  horarioInicio: '08:00',
  horarioTermino: '11:00',
  status: 'CONFIRMADO',
  ...overrides,
})

describe('eventoPresentation', () => {
  it('formata datas ISO para pt-BR, preserva formato existente e trata ausência', () => {
    expect(formatarDataEvento('2026-06-10')).toBe('10/06/2026')
    expect(formatarDataEvento('2026-06-10T08:00:00')).toBe('10/06/2026')
    expect(formatarDataEvento('15/07/2026')).toBe('15/07/2026')
    expect(formatarDataEvento('')).toBe('Não informado')
    expect(formatarDataEvento(undefined)).toBe('Não informado')
  })

  it('formata período de um dia e de múltiplos dias', () => {
    expect(formatarPeriodoEvento('2026-06-10', '2026-06-10')).toBe('10/06/2026')
    expect(formatarPeriodoEvento('2026-06-10', '2026-06-12')).toBe('10/06/2026 a 12/06/2026')
    expect(formatarPeriodoEvento('', '')).toBe('Não informado')
  })

  it('formata horários corretamente', () => {
    expect(formatarHorarioEvento('08:00', '11:30')).toBe('08:00 às 11:30')
    expect(formatarHorarioEvento('08:00', '')).toBe('08:00')
    expect(formatarHorarioEvento('', '')).toBe('Não informado')
  })

  it('retorna rótulos textuais e ícones para cada status', () => {
    expect(obterStatusVisualEvento('CONFIRMADO')).toMatchObject({ label: 'Confirmado', icon: 'event_available' })
    expect(obterStatusVisualEvento('ENCERRADO')).toMatchObject({ label: 'Encerrado', icon: 'history' })
    expect(obterStatusVisualEvento('CANCELADO')).toMatchObject({ label: 'Cancelado', icon: 'event_busy' })
    expect(obterStatusVisualEvento('PLANEJADO')).toMatchObject({ label: 'Planejado', icon: 'schedule' })
  })

  it('identifica presença de banner via bannerUrl ou documentos', () => {
    expect(eventoPossuiBanner({ bannerUrl: 'http://example.com/banner.jpg' })).toBe(true)
    expect(eventoPossuiBanner({ documentos: [{ id: 1, contentType: 'image/png' }] })).toBe(true)
    expect(eventoPossuiBanner({ documentos: [] })).toBe(false)
    expect(eventoPossuiBanner({})).toBe(false)
  })

  it('filtra visibilidade pública e ordena CONFIRMADO antes de ENCERRADO', () => {
    const encerrado = criarEvento({ id: 1, status: 'ENCERRADO' })
    const confirmado = criarEvento({ id: 2, status: 'CONFIRMADO' })
    const planejado = criarEvento({ id: 3, status: 'PLANEJADO' })
    const cancelado = criarEvento({ id: 4, status: 'CANCELADO' })

    expect(isEventoVisivelPublicamente(confirmado)).toBe(true)
    expect(isEventoVisivelPublicamente(encerrado)).toBe(true)
    expect(isEventoVisivelPublicamente(planejado)).toBe(false)
    expect(isEventoVisivelPublicamente(cancelado)).toBe(false)

    const ordenados = ordenarEventosPublicos([encerrado, confirmado])
    expect(ordenados.map((e) => e.id)).toEqual([2, 1])
  })
})
