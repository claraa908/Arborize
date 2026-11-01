import { defineStore } from 'pinia';
import { ref } from 'vue';
import { especieService } from '@/services/especieService';
import { useFeedbackStore } from '@/stores/feedbackStore';
import type { EspecieDTO, CreateEspeciePayload, UpdateEspeciePayload } from '@/types/viveiro';

export const useViveiroStore = defineStore('viveiro', () => {
  const feedback = useFeedbackStore();

  const especies = ref<EspecieDTO[]>([]);
  const isLoading = ref(false);

  async function fetchEspecies() {
    isLoading.value = true;
    try {
      const data = await especieService.getAll();
      especies.value = data;
    } catch (error) {
      feedback.showToast('Erro ao carregar catálogo de espécies do viveiro.', 'error');
    } finally {
      isLoading.value = false;
    }
  }

  async function createEspecie(payload: CreateEspeciePayload): Promise<boolean> {
    isLoading.value = true;
    try {
      await especieService.create(payload);
      feedback.showToast('Espécie cadastrada com sucesso!', 'success', 4000, 'Sucesso');
      await fetchEspecies();
      return true;
    } catch (error) {
      feedback.showToast('Não foi possível cadastrar a espécie. Verifique os dados.', 'error', 5000, 'Erro');
      return false;
    } finally {
      isLoading.value = false;
    }
  }

  async function updateEspecie(id: string | number, payload: UpdateEspeciePayload): Promise<boolean> {
    isLoading.value = true;
    try {
      await especieService.update(id, payload);
      feedback.showToast('Espécie atualizada com sucesso!', 'success', 4000, 'Sucesso');
      await fetchEspecies();
      return true;
    } catch (error) {
      feedback.showToast('Falha ao atualizar a espécie. Verifique a conexão.', 'error', 5000, 'Erro');
      return false;
    } finally {
      isLoading.value = false;
    }
  }

  async function removeEspecie(id: string | number): Promise<boolean> {
    const confirmed = await feedback.showConfirm({
      title: 'Excluir Espécie',
      message: 'Tem certeza de que deseja remover esta espécie do viveiro?',
      confirmText: 'Excluir',
      cancelText: 'Voltar',
      variant: 'danger',
    });

    if (!confirmed) return false;

    isLoading.value = true;
    try {
      await especieService.delete(id);
      especies.value = especies.value.filter((item) => item.id !== id);
      feedback.showToast('Espécie removida com sucesso!', 'success');
      return true;
    } catch (error) {
      feedback.showToast('Erro ao excluir espécie.', 'error');
      return false;
    } finally {
      isLoading.value = false;
    }
  }

  return {
    especies,
    isLoading,
    fetchEspecies,
    createEspecie,
    updateEspecie,
    removeEspecie,
  };
});