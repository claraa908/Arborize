import type { EventoDTO, EventoStatus } from '@/types/eventos'

export interface EventoStatusVisual {
  label: string
  icon: string
  badgeClass: string
}

const ISO_DATE_PATTERN = /^(\d{4})-(\d{2})-(\d{2})(?:T.*)?$/

export function formatarDataEvento(valor?: string | null): string {
  const texto = valor?.trim()
  if (!texto) return 'Não informado'

  const isoMatch = ISO_DATE_PATTERN.exec(texto)
  if (isoMatch) {
    const [, ano, mes, dia] = isoMatch
    return `${dia}/${mes}/${ano}`
  }

  return texto
}

export function formatarPeriodoEvento(dataInicio?: string | null, dataTermino?: string | null): string {
  const inicio = formatarDataEvento(dataInicio)
  const termino = formatarDataEvento(dataTermino)

  if (inicio === 'Não informado' && termino === 'Não informado') {
    return 'Não informado'
  }
  if (termino === 'Não informado' || inicio === termino) {
    return inicio
  }
  if (inicio === 'Não informado') {
    return termino
  }

  return `${inicio} a ${termino}`
}

export function formatarHorarioEvento(horarioInicio?: string | null, horarioTermino?: string | null): string {
  const inicio = horarioInicio?.trim()
  const termino = horarioTermino?.trim()

  if (!inicio && !termino) return 'Não informado'
  if (inicio && !termino) return inicio
  if (!inicio && termino) return termino
  return `${inicio} às ${termino}`
}

export function obterStatusVisualEvento(status: EventoStatus): EventoStatusVisual {
  switch (status) {
    case 'CONFIRMADO':
      return {
        label: 'Confirmado',
        icon: 'event_available',
        badgeClass: 'bg-amma-green text-white border border-amma-green',
      }
    case 'ENCERRADO':
      return {
        label: 'Encerrado',
        icon: 'history',
        badgeClass: 'bg-white/95 text-grey-text border border-gray-300',
      }
    case 'CANCELADO':
      return {
        label: 'Cancelado',
        icon: 'event_busy',
        badgeClass: 'bg-feedback-danger-bg text-feedback-danger border border-feedback-danger-border',
      }
    case 'PLANEJADO':
    default:
      return {
        label: 'Planejado',
        icon: 'schedule',
        badgeClass: 'bg-feedback-info-bg text-feedback-info border border-feedback-info-border',
      }
  }
}

export function eventoPossuiBanner(evento: Pick<EventoDTO, 'bannerUrl' | 'documentos'>): boolean {
  if (evento.bannerUrl && evento.bannerUrl.trim() !== '') return true
  return Array.isArray(evento.documentos) && evento.documentos.length > 0
}

export function isEventoVisivelPublicamente(evento: Pick<EventoDTO, 'status'>): boolean {
  return evento.status === 'CONFIRMADO' || evento.status === 'ENCERRADO'
}

export function ordenarEventosPublicos(eventos: EventoDTO[]): EventoDTO[] {
  const pesoStatus: Record<EventoStatus, number> = {
    CONFIRMADO: 0,
    ENCERRADO: 1,
    PLANEJADO: 2,
    CANCELADO: 3,
  }

  return [...eventos].sort((a, b) => pesoStatus[a.status] - pesoStatus[b.status])
}
