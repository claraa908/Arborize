<script setup lang="ts">
import { onMounted, onUnmounted, computed } from 'vue'
import { useFeedbackStore } from '@/stores/feedbackStore'

const feedbackStore = useFeedbackStore()

const variantConfig = computed(() => {
  const variant = feedbackStore.confirmOptions.variant || 'primary'

  if (variant === 'danger') {
    return {
      icon: 'delete',
      iconBg: 'bg-feedback-danger-bg text-feedback-danger border border-feedback-danger-border',
      btnClass: 'bg-feedback-danger hover:opacity-90 text-white',
      titleClass: 'text-feedback-danger',
    }
  }

  if (variant === 'warning') {
    return {
      icon: 'warning',
      iconBg: 'bg-feedback-warning-bg text-feedback-warning border border-feedback-warning-border',
      btnClass: 'bg-feedback-warning hover:opacity-90 text-white',
      titleClass: 'text-feedback-warning',
    }
  }

  return {
    icon: 'task_alt',
    iconBg: 'bg-amma-green-lighter text-amma-green border border-amma-green-light',
    btnClass: 'bg-amma-green hover:bg-amma-green-dark text-white',
    titleClass: 'text-amma-green-dark',
  }
})

function handleKeydown(event: KeyboardEvent) {
  if (event.key === 'Escape' && feedbackStore.isConfirmOpen) {
    feedbackStore.handleConfirmResponse(false)
  }
}

onMounted(() => {
  window.addEventListener('keydown', handleKeydown)
})

onUnmounted(() => {
  window.removeEventListener('keydown', handleKeydown)
})
</script>

<template>
  <Transition
    enter-active-class="transition duration-200 ease-out"
    enter-from-class="opacity-0"
    enter-to-class="opacity-100"
    leave-active-class="transition duration-150 ease-in"
    leave-from-class="opacity-100"
    leave-to-class="opacity-0"
  >
    <div
      v-if="feedbackStore.isConfirmOpen"
      class="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/40 backdrop-blur-xs"
      @click.self="feedbackStore.handleConfirmResponse(false)"
    >
      <div
        class="bg-white rounded-box shadow-xl border-2 border-amma-green-light max-w-md w-full p-6 sm:p-7 transform transition-all flex flex-col gap-5"
        role="dialog"
        aria-modal="true"
        aria-labelledby="confirm-modal-title"
      >
        <div class="flex items-start gap-4">
          <div
            class="w-12 h-12 rounded-full flex items-center justify-center shrink-0 shadow-xs"
            :class="variantConfig.iconBg"
          >
            <span class="material-symbols-outlined text-[26px]">
              {{ variantConfig.icon }}
            </span>
          </div>

          <div class="flex-1 min-w-0">
            <h3
              id="confirm-modal-title"
              class="text-xl font-bold leading-tight"
              :class="variantConfig.titleClass"
            >
              {{ feedbackStore.confirmOptions.title }}
            </h3>
            <p class="text-sm text-grey-text mt-1.5 leading-relaxed">
              {{ feedbackStore.confirmOptions.message }}
            </p>
          </div>
        </div>

        <div class="flex justify-end gap-3 pt-2 border-t border-amma-green-light">
          <button
            type="button"
            class="bg-white hover:bg-amma-bg text-grey-text font-bold py-2 px-5 rounded-btn border-2 border-amma-green-light transition-colors text-sm cursor-pointer shadow-xs"
            @click="feedbackStore.handleConfirmResponse(false)"
          >
            {{ feedbackStore.confirmOptions.cancelText }}
          </button>

          <button
            type="button"
            class="font-bold py-2 px-6 rounded-btn shadow-md text-sm border-none outline-none efeito-zoom"
            :class="variantConfig.btnClass"
            @click="feedbackStore.handleConfirmResponse(true)"
          >
            {{ feedbackStore.confirmOptions.confirmText }}
          </button>
        </div>
      </div>
    </div>
  </Transition>
</template>