import { defineStore } from 'pinia';
import { ref } from 'vue';
import { eventoService } from '@/services/eventoService';
import { useFeedbackStore } from '@/stores/feedbackStore';
import type { EventoDTO, CreateEventoPayload, UpdateEventoPayload } from '@/types/eventos';

export const useEventoStore = defineStore('evento', () => {
  const feedback = useFeedbackStore();

  const eventos = ref<EventoDTO[]>([]);
  const isLoading = ref(false);

  async function fetchEventos() {
    isLoading.value = true;
    try {
      const data = await eventoService.getAll();
      eventos.value = data;
    } catch (error) {
      feedback.showToast('Erro ao carregar lista de eventos socioambientais.', 'error');
    } finally {
      isLoading.value = false;
    }
  }

  async function createEvento(payload: CreateEventoPayload): Promise<boolean> {
    isLoading.value = true;
    try {
      await eventoService.create(payload);
      feedback.showToast('Evento socioambiental cadastrado com sucesso!', 'success', 4000, 'Sucesso');
      await fetchEventos();
      return true;
    } catch (error) {
      feedback.showToast('Falha ao cadastrar evento. Verifique os dados.', 'error', 5000, 'Erro');
      return false;
    } finally {
      isLoading.value = false;
    }
  }

  async function updateEvento(id: string | number, payload: UpdateEventoPayload): Promise<boolean> {
    isLoading.value = true;
    try {
      await eventoService.update(id, payload);
      feedback.showToast('Evento atualizado com sucesso!', 'success', 4000, 'Sucesso');
      await fetchEventos();
      return true;
    } catch (error) {
      feedback.showToast('Falha ao atualizar o evento. Verifique a conexão.', 'error', 5000, 'Erro');
      return false;
    } finally {
      isLoading.value = false;
    }
  }

  async function removeEvento(id: string | number): Promise<boolean> {
    const confirmed = await feedback.showConfirm({
      title: 'Excluir Evento',
      message: 'Tem certeza de que deseja remover este evento permanentemente?',
      confirmText: 'Excluir',
      cancelText: 'Voltar',
      variant: 'danger',
    });

    if (!confirmed) return false;

    isLoading.value = true;
    try {
      await eventoService.delete(id);
      eventos.value = eventos.value.filter((item) => item.id !== id);
      feedback.showToast('Evento excluído com sucesso!', 'success');
      return true;
    } catch (error) {
      feedback.showToast('Não foi possível excluir o evento.', 'error');
      return false;
    } finally {
      isLoading.value = false;
    }
  }

  return {
    eventos,
    isLoading,
    fetchEventos,
    createEvento,
    updateEvento,
    removeEvento,
  };
});