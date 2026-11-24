<script setup lang="ts">
import { computed } from 'vue'
import { statusLabel, type CompensacaoStatus } from '@/types/compensacao'

const props = defineProps<{ status: CompensacaoStatus }>()
const icon = computed(() => props.status === 'EM_ABERTO'
  ? 'radio_button_unchecked'
  : props.status === 'PARCIALMENTE_DOADO' ? 'timelapse' : 'task_alt')
</script>

<template>
  <span class="inline-flex items-center gap-1.5 rounded-full border border-amma-green-light bg-amma-green-lighter px-3 py-1.5 text-xs font-bold text-amma-green-dark" role="status">
    <span class="material-symbols-outlined text-base" aria-hidden="true">{{ icon }}</span>
    {{ statusLabel[status] }}
  </span>
</template>
