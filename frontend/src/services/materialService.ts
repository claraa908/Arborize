import { api } from '@/services/api';
import type { MaterialBackendDTO, CreateMaterialPayload, UpdateMaterialPayload } from '@/types/library';

const categoryMapToBackend: Record<string, string> = {
  'Arborização Urbana': 'ARBORIZACAO_URBANA',
  'Caatinga': 'CAATINGA',
  'Educação Ambiental': 'EDUCACAO_AMBIENTAL',
  'Legislação': 'LEGISLACAO',
};

const typeMapToBackend: Record<string, string> = {
  'Cartilha': 'CARTILHA',
  'Manual': 'MANUAL',
  'Relatório Técnico': 'RELATORIO_TECNICO',
  'Guia': 'GUIA',
  'Outros': 'OUTROS',
};

export const materialService = {
  async getPublicMaterials(search?: string, category?: string): Promise<MaterialBackendDTO[]> {
    const params = new URLSearchParams();
    if (search) params.append('search', search);
    if (category) {
      const mappedCategory = categoryMapToBackend[category] || category;
      params.append('category', mappedCategory);
    }

    const response = await api.get<MaterialBackendDTO[]>('/api/materials', { params });
    return response.data;
  },

  async getAdminMaterials(): Promise<MaterialBackendDTO[]> {
    const response = await api.get<MaterialBackendDTO[]>('/api/materials/admin');
    return response.data;
  },

  async getMaterialById(id: string | number): Promise<MaterialBackendDTO> {
    const response = await api.get<MaterialBackendDTO>(`/api/materials/${id}`);
    return response.data;
  },

  async createMaterial(payload: CreateMaterialPayload): Promise<MaterialBackendDTO> {
    const formData = new FormData();
    formData.append('title', payload.title);
    formData.append('description', payload.description);
    formData.append('status', payload.status);

    formData.append('type', typeMapToBackend[payload.type] || 'OUTROS');
    formData.append('category', categoryMapToBackend[payload.category] || 'ARBORIZACAO_URBANA');
    formData.append('file', payload.file);

    if (payload.coverPhoto) {
      formData.append('coverPhoto', payload.coverPhoto);
    }

    const response = await api.post<MaterialBackendDTO>('/api/materials', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    });
    return response.data;
  },

  async updateMaterial(id: string | number, payload: UpdateMaterialPayload): Promise<MaterialBackendDTO> {
    const formData = new FormData();
    formData.append('title', payload.title);
    formData.append('description', payload.description);
    formData.append('status', payload.status);

    formData.append('type', typeMapToBackend[payload.type] || 'OUTROS');
    formData.append('category', categoryMapToBackend[payload.category] || 'ARBORIZACAO_URBANA');

    if (payload.file) {
      formData.append('file', payload.file);
    }

    if (payload.coverPhoto) {
      formData.append('coverPhoto', payload.coverPhoto);
    }

    const response = await api.put<MaterialBackendDTO>(`/api/materials/${id}`, formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    });
    return response.data;
  },

  async updateStatus(id: string | number, status: 'PUBLICADO' | 'RASCUNHO'): Promise<MaterialBackendDTO> {
    const response = await api.patch<MaterialBackendDTO>(`/api/materials/${id}/status`, { status });
    return response.data;
  },

  async deleteMaterial(id: string | number): Promise<void> {
    await api.delete(`/api/materials/${id}`);
  },

  getCoverUrl(id: string | number): string {
    return `${api.defaults.baseURL}/api/materials/${id}/cover`;
  },

  getDownloadUrl(id: string | number): string {
    return `${api.defaults.baseURL}/api/materials/${id}/download`;
  },
};
