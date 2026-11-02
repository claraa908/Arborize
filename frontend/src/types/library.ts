export type MaterialStatus = 'RASCUNHO' | 'PUBLICADO';

export type MaterialTypeBackend = 'CARTILHA' | 'MANUAL' | 'RELATORIO_TECNICO' | 'GUIA' | 'OUTROS';
export type MaterialTypeUI = 'Cartilha' | 'Manual' | 'Relatório Técnico' | 'Guia' | 'Outros';

export type MaterialCategoryBackend = 'ARBORIZACAO_URBANA' | 'CAATINGA' | 'EDUCACAO_AMBIENTAL' | 'LEGISLACAO';
export type MaterialCategoryUI = 'Arborização Urbana' | 'Caatinga' | 'Educação Ambiental' | 'Legislação';

export interface MaterialBackendDTO {
  id: number | string;
  title: string;
  description: string;
  type?: MaterialTypeBackend;
  materialType?: string;
  category: string;
  status: MaterialStatus;
  uploadDate?: string;
  documents?: Array<{
    id: number | string;
    fileName?: string;
    originalFilename?: string;
    filePath?: string;
    sizeBytes?: number;
  }>;
}

export interface MaterialAdminItem {
  id: string;
  titulo: string;
  categoria: string;
  status: MaterialStatus;
  data: string;
}

export interface MaterialCatalogoItem {
  id: string;
  titulo: string;
  descricao: string;
  categoria: string;
  tipo: string;
  data: string;
  tamanho: string;
  capaUrl: string;
}

export interface CreateMaterialPayload {
  title: string;
  description: string;
  status: MaterialStatus;
  type: MaterialTypeUI;
  category: MaterialCategoryUI;
  file: File;
  coverPhoto?: File | null;
}

export interface UpdateMaterialPayload {
  title: string;
  description: string;
  status: MaterialStatus;
  type: MaterialTypeUI;
  category: MaterialCategoryUI;
  file?: File | null;
  coverPhoto?: File | null;
}
