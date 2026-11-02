import { defineStore } from 'pinia';
import { ref } from 'vue';
import type { ToastItem, FeedbackType, ConfirmDialogOptions } from '@/types/feedback';

export const useFeedbackStore = defineStore('feedback', () => {
  const toasts = ref<ToastItem[]>([]);

  function showToast(message: string, type: FeedbackType = 'info', duration = 4000, title?: string) {
    const id = crypto.randomUUID();
    const newToast: ToastItem = { id, message, type, duration, title };
    toasts.value.push(newToast);

    if (duration > 0) {
      setTimeout(() => {
        removeToast(id);
      }, duration);
    }
  }

  function removeToast(id: string) {
    toasts.value = toasts.value.filter((toast) => toast.id !== id);
  }

  const isConfirmOpen = ref(false);
  const confirmOptions = ref<ConfirmDialogOptions>({
    message: '',
    confirmText: 'Confirmar',
    cancelText: 'Cancelar',
    variant: 'primary',
  });
  let confirmResolve: ((value: boolean) => void) | null = null;

  function showConfirm(options: ConfirmDialogOptions): Promise<boolean> {
    confirmOptions.value = {
      title: options.title || 'Confirmação',
      message: options.message,
      confirmText: options.confirmText || 'Confirmar',
      cancelText: options.cancelText || 'Cancelar',
      variant: options.variant || 'primary',
    };
    isConfirmOpen.value = true;

    return new Promise<boolean>((resolve) => {
      confirmResolve = resolve;
    });
  }

  function handleConfirmResponse(confirmed: boolean) {
    isConfirmOpen.value = false;
    if (confirmResolve) {
      confirmResolve(confirmed);
      confirmResolve = null;
    }
  }

  return {
    toasts,
    showToast,
    removeToast,
    isConfirmOpen,
    confirmOptions,
    showConfirm,
    handleConfirmResponse,
  };
});