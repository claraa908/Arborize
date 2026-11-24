import { defineStore } from 'pinia';
import { ref } from 'vue';
import { eventoService } from '@/services/eventoService';
import { useFeedbackStore } from '@/stores/feedbackStore';
import type { EventoDTO, CreateEventoPayload, UpdateEventoPayload } from '@/types/eventos';

function extractHttpStatus(error: unknown): number | null {
  if (typeof error === 'object' && error !== null && 'response' in error) {
    const response = (error as { response?: { status?: unknown } }).response;
    if (typeof response?.status === 'number') {
      return response.status;
    }
  }
  return null;
}

export const useEventoStore = defineStore('evento', () => {
  const feedback = useFeedbackStore();

  const eventos = ref<EventoDTO[]>([]);
  const isLoading = ref(false);
  const error = ref<string | null>(null);

  const eventoAtual = ref<EventoDTO | null>(null);
  const isLoadingDetail = ref(false);
  const detailError = ref<string | null>(null);
  const detailStatus = ref<number | null>(null);
  let activeDetailRequestToken = 0;

  async function fetchEventos() {
    isLoading.value = true;
    error.value = null;
    try {
      const data = await eventoService.getAll();
      eventos.value = data;
    } catch {
      error.value = 'Não foi possível carregar os eventos socioambientais.';
      feedback.showToast('Erro ao carregar lista de eventos socioambientais.', 'error');
    } finally {
      isLoading.value = false;
    }
  }

  async function fetchEventoById(id: string | number): Promise<EventoDTO | null> {
    const requestToken = ++activeDetailRequestToken;
    isLoadingDetail.value = true;
    eventoAtual.value = null;
    detailError.value = null;
    detailStatus.value = null;

    try {
      const data = await eventoService.getById(id);
      if (requestToken !== activeDetailRequestToken) return null;
      eventoAtual.value = data;
      return data;
    } catch (err) {
      if (requestToken !== activeDetailRequestToken) return null;
      const status = extractHttpStatus(err);
      detailStatus.value = status;
      detailError.value =
        status === 404
          ? 'Evento não encontrado.'
          : 'Não foi possível carregar os detalhes do evento.';
      return null;
    } finally {
      if (requestToken === activeDetailRequestToken) {
        isLoadingDetail.value = false;
      }
    }
  }

  function clearEventoAtual() {
    activeDetailRequestToken += 1;
    eventoAtual.value = null;
    isLoadingDetail.value = false;
    detailError.value = null;
    detailStatus.value = null;
  }

  async function createEvento(payload: CreateEventoPayload): Promise<boolean> {
    isLoading.value = true;
    try {
      await eventoService.create(payload);
      feedback.showToast('Evento socioambiental cadastrado com sucesso!', 'success', 4000, 'Sucesso');
      await fetchEventos();
      return true;
    } catch {
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
    } catch {
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
    } catch {
      feedback.showToast('Não foi possível excluir o evento.', 'error');
      return false;
    } finally {
      isLoading.value = false;
    }
  }

  return {
    eventos,
    isLoading,
    error,
    eventoAtual,
    isLoadingDetail,
    detailError,
    detailStatus,
    fetchEventos,
    fetchEventoById,
    clearEventoAtual,
    createEvento,
    updateEvento,
    removeEvento,
  };
});
