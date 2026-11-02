export type EventoStatus = 'PLANEJADO' | 'CONFIRMADO' | 'ENCERRADO' | 'CANCELADO';

export interface EventoDTO {
  id?: number | string;
  nome: string;
  descricao: string;
  bannerUrl?: string;
  dataInicio: string;
  dataTermino: string;
  localConcentracao: string;
  destinoFinal?: string;
  horarioInicio: string;
  descricaoPontoPartida?: string;
  horarioTermino: string;
  descricaoEncerramento?: string;
  status: EventoStatus;
}

export interface CreateEventoPayload {
  nome: string;
  descricao: string;
  imagem: File | null;
  dataInicio: string;
  dataTermino: string;
  local: string;
  destinoFinal?: string;
  horarioInicio: string;
  descricaoHorarioInicio?: string;
  horarioTermino: string;
  descricaoHorarioTermino?: string;
}

export interface UpdateEventoPayload {
  nome: string;
  descricao: string;
  imagem?: File | null;
  dataInicio: string;
  dataTermino: string;
  local: string;
  destinoFinal?: string;
  horarioInicio: string;
  descricaoHorarioInicio?: string;
  horarioTermino: string;
  descricaoHorarioTermino?: string;
  status?: EventoStatus;
}