export interface EspecieDTO {
  id?: number | string;
  nomePopular: string;
  nomeCientifico: string;
  familiaBotanica?: string;
  categoria: string;
  porteArvore: string;
  recomendadaCalcadas: boolean;
  mudasDisponiveis?: number;
  tempoGerminacaoDias?: number | string;
  descricaoCultivo?: string;
  fotoUrl?: string;
}

export interface CreateEspeciePayload {
  nomePopular: string;
  nomeCientifico: string;
  familiaBotanica?: string;
  categoria: string;
  porte: string;
  adequadoCalcada: 'Sim' | 'Não';
  mudasDisponiveis: number;
  tempoGerminacaoDias?: string | number;
  descricao?: string;
  imagem: File | null;
}

export interface UpdateEspeciePayload {
  nomePopular: string;
  nomeCientifico: string;
  familiaBotanica?: string;
  categoria: string;
  porte: string;
  adequadoCalcada: 'Sim' | 'Não';
  mudasDisponiveis: number;
  tempoGerminacaoDias?: string | number;
  descricao?: string;
  imagem?: File | null;
}