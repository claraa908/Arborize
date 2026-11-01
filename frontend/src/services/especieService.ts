import { api } from '@/services/api';
import type { EspecieDTO, CreateEspeciePayload, UpdateEspeciePayload } from '@/types/viveiro';

export const especieService = {
  async getAll(): Promise<EspecieDTO[]> {
    const response = await api.get<EspecieDTO[]>('/especies');
    return response.data;
  },

  async getById(id: string | number): Promise<EspecieDTO> {
    const response = await api.get<EspecieDTO>(`/especies/${id}`);
    return response.data;
  },

  async create(payload: CreateEspeciePayload): Promise<EspecieDTO> {
    const formData = new FormData();
    formData.append('nomePopular', payload.nomePopular);
    formData.append('nomeCientifico', payload.nomeCientifico);

    if (payload.familiaBotanica) {
      formData.append('familiaBotanica', payload.familiaBotanica);
    }

    formData.append('categoria', payload.categoria);
    formData.append('porteArvore', payload.porte);
    formData.append('descricaoCultivo', payload.descricao || '');

    const calcadaBoolean = payload.adequadoCalcada === 'Sim' ? 'true' : 'false';
    formData.append('recomendadaCalcadas', calcadaBoolean);

    if (payload.tempoGerminacaoDias) {
      formData.append('tempoGerminacaoDias', payload.tempoGerminacaoDias.toString());
    }

    if (payload.imagem) {
      formData.append('foto', payload.imagem);
    }

    const response = await api.post<EspecieDTO>('/especies', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    });

    return response.data;
  },

  async update(id: string | number, payload: UpdateEspeciePayload): Promise<EspecieDTO> {
    const formData = new FormData();
    formData.append('_method', 'PUT')

    formData.append('nomePopular', payload.nomePopular);
    formData.append('nomeCientifico', payload.nomeCientifico);

    if (payload.familiaBotanica) {
      formData.append('familiaBotanica', payload.familiaBotanica);
    }

    formData.append('categoria', payload.categoria);
    formData.append('porteArvore', payload.porte);
    formData.append('descricaoCultivo', payload.descricao || '');

    const calcadaBoolean = payload.adequadoCalcada === 'Sim' ? 'true' : 'false';
    formData.append('recomendadaCalcadas', calcadaBoolean);

    if (payload.tempoGerminacaoDias) {
      formData.append('tempoGerminacaoDias', payload.tempoGerminacaoDias.toString());
    }

    if (payload.imagem) {
      formData.append('foto', payload.imagem);
    }

    const response = await api.put<EspecieDTO>(`/especies/editar/${id}`, formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    });

    return response.data;
  },

  async delete(id: string | number): Promise<void> {
    await api.delete(`/especies/${id}`);
  },

  getFotoUrl(id: string | number): string {
    return `${api.defaults.baseURL}/especies/${id}/imagem`;
  },
};
