<script setup lang="ts">
import { formatCpf } from '@/utils/cpf'

defineProps<{
  protocol: string
  cpf: string
  loading: boolean
  copied: boolean
  protocolPlaceholder: string
  submitLabel: string
}>()

const emit = defineEmits<{
  'update:protocol': [value: string]
  'update:cpf': [value: string]
  submit: []
  'copy-protocol': []
}>()
</script>

<template>
  <form
    class="grid min-w-0 grid-cols-1 gap-4 rounded-box border border-amma-green-light bg-white p-5 shadow-sm md:grid-cols-[minmax(0,1fr)_minmax(0,1fr)_auto] md:items-start md:p-6"
    @submit.prevent="emit('submit')"
  >
    <div class="min-w-0">
      <label for="tracking-protocol" class="text-sm font-semibold text-grey-text">Protocolo *</label>
      <div class="relative mt-1">
        <input
          id="tracking-protocol"
          :value="protocol"
          required
          maxlength="32"
          autocomplete="off"
          :placeholder="protocolPlaceholder"
          class="w-full min-w-0 rounded-btn border-2 border-amma-green-light bg-white py-3 pl-4 pr-12 text-sm text-grey-text outline-none focus:border-amma-green focus:bg-amma-green-lighter focus-visible:ring-2 focus-visible:ring-amma-green"
          @input="emit('update:protocol', ($event.target as HTMLInputElement).value)"
        />
        <button
          type="button"
          :disabled="!protocol"
          :aria-label="copied ? 'Protocolo copiado' : 'Copiar protocolo'"
          class="absolute right-2 top-1/2 inline-flex h-9 w-9 -translate-y-1/2 items-center justify-center rounded-full text-amma-green hover:bg-amma-green-lighter focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-amma-green disabled:cursor-not-allowed disabled:opacity-40"
          @click="emit('copy-protocol')"
        >
          <span class="material-symbols-outlined text-xl" aria-hidden="true">{{ copied ? 'check' : 'content_copy' }}</span>
        </button>
        <span class="sr-only" aria-live="polite">{{ copied ? 'Protocolo copiado para a área de transferência.' : '' }}</span>
      </div>
      <p class="mt-1 min-h-4 text-xs text-grey-text">Informe o protocolo recebido ao enviar o pedido.</p>
    </div>

    <div class="min-w-0">
      <label for="tracking-cpf" class="text-sm font-semibold text-grey-text">CPF do solicitante *</label>
      <input
        id="tracking-cpf"
        :value="cpf"
        required
        inputmode="numeric"
        maxlength="14"
        autocomplete="off"
        placeholder="000.000.000-00"
        class="mt-1 w-full min-w-0 rounded-btn border-2 border-amma-green-light bg-white px-4 py-3 text-sm text-grey-text outline-none focus:border-amma-green focus:bg-amma-green-lighter focus-visible:ring-2 focus-visible:ring-amma-green"
        @input="emit('update:cpf', formatCpf(($event.target as HTMLInputElement).value))"
      />
      <p class="mt-1 min-h-4 text-xs text-grey-text">Use o mesmo CPF informado no pedido.</p>
    </div>

    <button
      type="submit"
      :disabled="loading"
      :aria-busy="loading"
      class="mt-0 inline-flex min-h-11 items-center justify-center gap-2 rounded-btn bg-amma-green px-5 py-2.5 text-sm font-bold text-white hover:bg-amma-green-dark focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-amma-green disabled:cursor-wait disabled:opacity-70 md:mt-7"
    >
      <span v-if="loading" class="material-symbols-outlined animate-spin" aria-hidden="true">progress_activity</span>
      {{ loading ? 'Consultando…' : submitLabel }}
    </button>
  </form>
</template>
