import { defineStore } from 'pinia';
import { ref } from 'vue';
import { materialService } from '@/services/materialService';
import { useFeedbackStore } from '@/stores/feedbackStore';
import type {
  MaterialAdminItem,
  MaterialCatalogoItem,
  CreateMaterialPayload,
  UpdateMaterialPayload,
} from '@/types/library';

export const useLibraryStore = defineStore('library', () => {
  const feedback = useFeedbackStore();

  const publicMaterials = ref<MaterialCatalogoItem[]>([]);
  const adminMaterials = ref<MaterialAdminItem[]>([]);
  const isLoading = ref(false);

  async function fetchPublicCatalog(search?: string, category?: string) {
      isLoading.value = true;
      try {
        const data = await materialService.getPublicMaterials(search, category);
        publicMaterials.value = data.map((item) => {
          const firstDoc = item.documents?.[0];
          const tamanho = firstDoc && firstDoc.sizeBytes
            ? `${(firstDoc.sizeBytes / 1024 / 1024).toFixed(1)} MB`
            : '2.0 MB';

          return {
            id: String(item.id),
            titulo: item.title,
            descricao: item.description,
            categoria: item.category,
            tipo: item.materialType || '',
            data: item.uploadDate ? new Date(item.uploadDate).toLocaleDateString('pt-BR') : '-',
            tamanho,
            capaUrl: materialService.getCoverUrl(item.id),
          };
        });
      } catch (error) {
        feedback.showToast('Erro ao carregar o catálogo de materiais.', 'error');
      } finally {
        isLoading.value = false;
      }
    }

  async function fetchAdminMaterials() {
    isLoading.value = true;
    try {
      const data = await materialService.getAdminMaterials();
      adminMaterials.value = data.map((item) => ({
        id: String(item.id),
        titulo: item.title,
        categoria: item.category,
        status: item.status,
        data: item.uploadDate ? new Date(item.uploadDate).toLocaleDateString('pt-BR') : '-',
      }));
    } catch (error) {
      feedback.showToast('Erro ao carregar listagem administrativa.', 'error');
    } finally {
      isLoading.value = false;
    }
  }

  async function createDocument(payload: CreateMaterialPayload): Promise<boolean> {
    isLoading.value = true;
    try {
      await materialService.createMaterial(payload);
      feedback.showToast('Documento guardado com sucesso!', 'success', 4000, 'Sucesso');
      await fetchAdminMaterials();
      return true;
    } catch (error) {
      feedback.showToast('Falha ao salvar documento. Verifique a conexão.', 'error', 5000, 'Erro');
      return false;
    } finally {
      isLoading.value = false;
    }
  }

  async function updateDocument(id: string | number, payload: UpdateMaterialPayload): Promise<boolean> {
    isLoading.value = true;
    try {
      await materialService.updateMaterial(id, payload);
      feedback.showToast('Documento atualizado com sucesso!', 'success', 4000, 'Sucesso');
      await fetchAdminMaterials();
      return true;
    } catch (error) {
      feedback.showToast('Falha ao atualizar documento. Verifique a conexão.', 'error', 5000, 'Erro');
      return false;
    } finally {
      isLoading.value = false;
    }
  }

  async function removeDocument(id: string): Promise<boolean> {
    const confirmed = await feedback.showConfirm({
      title: 'Excluir Documento',
      message: 'Tem certeza de que deseja remover este material permanentemente?',
      confirmText: 'Excluir',
      cancelText: 'Voltar',
      variant: 'danger',
    });

    if (!confirmed) return false;

    isLoading.value = true;
    try {
      await materialService.deleteMaterial(id);
      adminMaterials.value = adminMaterials.value.filter((item) => item.id !== id);
      feedback.showToast('Documento removido com sucesso!', 'success');
      return true;
    } catch (error) {
      feedback.showToast('Não foi possível excluir o documento.', 'error');
      return false;
    } finally {
      isLoading.value = false;
    }
  }

  return {
    publicMaterials,
    adminMaterials,
    isLoading,
    fetchPublicCatalog,
    fetchAdminMaterials,
    createDocument,
    updateDocument,
    removeDocument,
  };
});
