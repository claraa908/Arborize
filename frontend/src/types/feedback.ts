export type FeedbackType = 'success' | 'error' | 'warning' | 'info';

export interface ToastItem {
  id: string;
  type: FeedbackType;
  title?: string;
  message: string;
  duration?: number;
}

export interface ConfirmDialogOptions {
  title?: string;
  message: string;
  confirmText?: string;
  cancelText?: string;
  variant?: 'danger' | 'primary' | 'warning';
}