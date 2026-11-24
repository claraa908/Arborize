<script setup lang="ts">
defineProps<{
  loading: boolean
  error: string | null
  empty: boolean
}>()

const emit = defineEmits<{ retry: [] }>()
</script>

<template>
  <div v-if="loading" class="flex items-center justify-center gap-3 rounded-btn bg-amma-bg p-10 text-sm font-semibold text-grey-text" role="status">
    <span class="material-symbols-outlined animate-spin text-amma-green" aria-hidden="true">progress_activity</span>
    Carregando termos…
  </div>
  <div v-else-if="error" class="rounded-btn border border-feedback-danger-border bg-feedback-danger-bg p-6 text-center" role="alert">
    <p class="font-semibold text-feedback-danger">{{ error }}</p>
    <button type="button" class="mt-4 min-h-10 rounded-btn border-2 border-amma-green px-4 py-2 font-bold text-amma-green focus-visible:outline focus-visible:outline-2 focus-visible:outline-amma-green" @click="emit('retry')">Tentar novamente</button>
  </div>
  <div v-else-if="empty" class="rounded-btn border border-amma-green-light bg-amma-bg p-8 text-center">
    <span class="material-symbols-outlined text-3xl text-amma-green" aria-hidden="true">description</span>
    <p class="mt-2 text-sm text-grey-text">Nenhum termo encontrado. Cadastre um novo termo para iniciar o acompanhamento.</p>
  </div>
  <slot v-else />
</template>
