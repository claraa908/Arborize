<script setup lang="ts">
import { useFeedbackStore } from '@/stores/feedbackStore'
import type { FeedbackType } from '@/types/feedback'

const feedbackStore = useFeedbackStore()

interface StyleConfig {
  bg: string
  border: string
  textTitle: string
  textMessage: string
  iconName: string
  iconColor: string
}

const stylesMap: Record<FeedbackType, StyleConfig> = {
  success: {
    bg: 'bg-feedback-success-bg',
    border: 'border-feedback-success-border',
    textTitle: 'text-feedback-success',
    textMessage: 'text-amma-green-dark',
    iconName: 'eco',
    iconColor: 'text-feedback-success',
  },
  error: {
    bg: 'bg-feedback-danger-bg',
    border: 'border-feedback-danger-border',
    textTitle: 'text-feedback-danger',
    textMessage: 'text-feedback-danger',
    iconName: 'report',
    iconColor: 'text-feedback-danger',
  },
  warning: {
    bg: 'bg-feedback-warning-bg',
    border: 'border-feedback-warning-border',
    textTitle: 'text-feedback-warning',
    textMessage: 'text-feedback-warning',
    iconName: 'warning',
    iconColor: 'text-feedback-warning',
  },
  info: {
    bg: 'bg-feedback-info-bg',
    border: 'border-feedback-info-border',
    textTitle: 'text-feedback-info',
    textMessage: 'text-feedback-info',
    iconName: 'info',
    iconColor: 'text-feedback-info',
  },
}
</script>

<template>
  <div
    class="absolute top-5 left-1/2 -translate-x-1/2 z-50 flex flex-col items-center gap-2.5 max-w-md w-[calc(100%-2.5rem)] pointer-events-none"
    aria-live="polite"
  >
    <TransitionGroup
      enter-active-class="transition duration-300 ease-out"
      enter-from-class="opacity-0 -translate-y-8 scale-95"
      enter-to-class="opacity-100 translate-y-0 scale-100"
      leave-active-class="transition duration-200 ease-in"
      leave-from-class="opacity-100 scale-100"
      leave-to-class="opacity-0 -translate-y-4 scale-95"
    >
      <div
        v-for="toast in feedbackStore.toasts"
        :key="toast.id"
        class="pointer-events-auto flex items-start gap-3 p-3.5 sm:p-4 rounded-btn border-2 shadow-sm transition-all w-full"
        :class="[stylesMap[toast.type].bg, stylesMap[toast.type].border]"
        role="alert"
      >
        <div class="mt-0.5 shrink-0 flex items-center justify-center">
          <span
            class="material-symbols-outlined text-[22px]"
            :class="stylesMap[toast.type].iconColor"
          >
            {{ stylesMap[toast.type].iconName }}
          </span>
        </div>

        <div class="flex-1 min-w-0">
          <h4
            v-if="toast.title"
            class="text-sm font-bold leading-tight mb-0.5"
            :class="stylesMap[toast.type].textTitle"
          >
            {{ toast.title }}
          </h4>
          <p
            class="text-xs font-medium leading-relaxed"
            :class="stylesMap[toast.type].textMessage"
          >
            {{ toast.message }}
          </p>
        </div>

        <button
          type="button"
          class="shrink-0 text-grey-text/60 hover:text-grey-text p-1 cursor-pointer transition-colors"
          aria-label="Fechar notificação"
          @click="feedbackStore.removeToast(toast.id)"
        >
          <span class="material-symbols-outlined text-[18px] leading-none">close</span>
        </button>
      </div>
    </TransitionGroup>
  </div>
</template>