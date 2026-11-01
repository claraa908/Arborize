<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import type {
  DashboardReportDataset,
  DashboardReportFormat,
  DashboardReportRequest,
} from '@/types/dashboard'

defineProps<{
  isGenerating: boolean
}>()

const emit = defineEmits<{
  close: []
  generate: [request: DashboardReportRequest]
}>()

const toInputDate = (date: Date) => {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

const today = new Date()
const startDateInput = ref<HTMLInputElement | null>(null)
const dataInicial = ref(`${today.getFullYear()}-01-01`)
const dataFinal = ref(toInputDate(today))
const dadosSelecionados = ref<DashboardReportDataset[]>(['MUDAS_DOADAS', 'BALANCO_RECICLAVEIS'])
const formato = ref<DashboardReportFormat>('PDF')
const submitted = ref(false)
const formatOptions: { label: string; value: DashboardReportFormat }[] = [
  { label: 'PDF Oficial', value: 'PDF' },
  { label: 'CSV', value: 'CSV' },
  { label: 'Excel (XLSX)', value: 'XLSX' },
]

const validationMessage = computed(() => {
  if (!dataInicial.value || !dataFinal.value) return 'Informe as duas datas do período.'
  if (dataFinal.value < dataInicial.value) return 'A data final não pode ser anterior à data inicial.'
  if (dadosSelecionados.value.length === 0) return 'Selecione ao menos um conjunto de dados.'
  return ''
})

const toggleDataset = (dataset: DashboardReportDataset, checked: boolean) => {
  if (checked && !dadosSelecionados.value.includes(dataset)) {
    dadosSelecionados.value = [...dadosSelecionados.value, dataset]
  } else if (!checked) {
    dadosSelecionados.value = dadosSelecionados.value.filter((item) => item !== dataset)
  }
}

const handleDatasetChange = (dataset: DashboardReportDataset, event: Event) => {
  const target = event.target
  if (target instanceof HTMLInputElement) {
    toggleDataset(dataset, target.checked)
  }
}

const generate = () => {
  submitted.value = true
  if (validationMessage.value) return

  emit('generate', {
    dataInicial: dataInicial.value,
    dataFinal: dataFinal.value,
    dados: dadosSelecionados.value,
    formato: formato.value,
  })
}

onMounted(() => startDateInput.value?.focus())
</script>

<template>
  <div
    class="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4"
    @click.self="emit('close')"
    @keydown.esc="emit('close')"
  >
    <section
      role="dialog"
      aria-modal="true"
      aria-labelledby="dashboard-report-title"
      aria-describedby="dashboard-report-description"
      class="max-h-[calc(100vh-2rem)] w-full max-w-xl overflow-y-auto rounded-box border border-amma-green-light bg-white p-6 shadow-2xl md:p-8"
    >
      <header class="mb-6 flex items-start justify-between gap-4">
        <div>
          <h2 id="dashboard-report-title" class="text-xl font-bold text-amma-green-dark">
            Gerar Relatório de Impacto Ambiental
          </h2>
          <p id="dashboard-report-description" class="mt-1 text-sm text-grey-text">
            Selecione o período, os dados e o formato do arquivo.
          </p>
        </div>
        <button
          type="button"
          class="flex h-9 w-9 shrink-0 items-center justify-center rounded-full text-grey-text hover:bg-amma-green-lighter focus-visible:outline-2 focus-visible:outline-amma-green"
          aria-label="Fechar relatório"
          :disabled="isGenerating"
          @click="emit('close')"
        >
          <span class="material-symbols-outlined" aria-hidden="true">close</span>
        </button>
      </header>

      <form class="space-y-5" @submit.prevent="generate">
        <fieldset>
          <legend class="mb-2 text-sm font-semibold text-grey-text">Período do relatório</legend>
          <div class="grid gap-4 sm:grid-cols-2">
            <div>
              <label for="report-start" class="mb-1.5 block text-sm font-semibold text-grey-text">
                Data inicial
              </label>
              <input
                id="report-start"
                ref="startDateInput"
                v-model="dataInicial"
                type="date"
                required
                class="w-full rounded-btn border-2 border-amma-green-light bg-white px-3 py-2.5 text-sm text-grey-text outline-none transition focus:border-amma-green focus:bg-amma-green-lighter focus-visible:ring-2 focus-visible:ring-amma-green/30"
              />
            </div>
            <div>
              <label for="report-end" class="mb-1.5 block text-sm font-semibold text-grey-text">
                Data final
              </label>
              <input
                id="report-end"
                v-model="dataFinal"
                type="date"
                required
                :min="dataInicial"
                class="w-full rounded-btn border-2 border-amma-green-light bg-white px-3 py-2.5 text-sm text-grey-text outline-none transition focus:border-amma-green focus:bg-amma-green-lighter focus-visible:ring-2 focus-visible:ring-amma-green/30"
              />
            </div>
          </div>
        </fieldset>

        <fieldset>
          <legend class="mb-2 text-sm font-semibold text-grey-text">Dados a incluir</legend>
          <div class="grid gap-2 sm:grid-cols-2">
            <label class="flex items-center gap-2 text-sm text-grey-text">
              <input
                type="checkbox"
                :checked="dadosSelecionados.includes('MUDAS_DOADAS')"
                class="h-4 w-4 accent-amma-green focus-visible:outline-2 focus-visible:outline-amma-green"
                @change="handleDatasetChange('MUDAS_DOADAS', $event)"
              />
              Mudas doadas
            </label>
            <label class="flex items-center gap-2 text-sm text-grey-text">
              <input
                type="checkbox"
                :checked="dadosSelecionados.includes('BALANCO_RECICLAVEIS')"
                class="h-4 w-4 accent-amma-green focus-visible:outline-2 focus-visible:outline-amma-green"
                @change="handleDatasetChange('BALANCO_RECICLAVEIS', $event)"
              />
              Balanço de recicláveis
            </label>
            <label class="flex items-center gap-2 text-sm text-grey-text/60">
              <input type="checkbox" disabled class="h-4 w-4 accent-amma-green" />
              Laudos emitidos
              <span class="text-xs">(fonte indisponível)</span>
            </label>
          </div>
        </fieldset>

        <fieldset>
          <legend class="mb-2 text-sm font-semibold text-grey-text">Formato</legend>
          <div class="flex flex-wrap gap-x-6 gap-y-2">
            <label
              v-for="option in formatOptions"
              :key="option.value"
              class="flex items-center gap-2 text-sm text-grey-text"
            >
              <input
                v-model="formato"
                type="radio"
                name="report-format"
                :value="option.value"
                class="h-4 w-4 accent-amma-green focus-visible:outline-2 focus-visible:outline-amma-green"
              />
              {{ option.label }}
            </label>
          </div>
        </fieldset>

        <p v-if="submitted && validationMessage" role="alert" class="text-sm font-medium text-feedback-danger">
          {{ validationMessage }}
        </p>

        <footer class="flex flex-col-reverse gap-3 pt-1 sm:flex-row sm:justify-end">
          <button
            type="button"
            class="rounded-btn border-2 border-amma-green-light px-5 py-2.5 text-sm font-bold text-grey-text transition hover:bg-amma-green-lighter focus-visible:outline-2 focus-visible:outline-amma-green disabled:cursor-not-allowed disabled:opacity-60"
            :disabled="isGenerating"
            @click="emit('close')"
          >
            Cancelar
          </button>
          <button
            type="submit"
            class="efeito-zoom inline-flex items-center justify-center gap-2 rounded-btn bg-amma-green px-5 py-2.5 text-sm font-bold text-white transition hover:bg-amma-green-dark focus-visible:outline-2 focus-visible:outline-amma-green disabled:cursor-wait disabled:opacity-60"
            :disabled="isGenerating"
          >
            <span v-if="isGenerating" class="material-symbols-outlined animate-spin" aria-hidden="true">progress_activity</span>
            {{ isGenerating ? 'Gerando...' : 'Gerar relatório' }}
          </button>
        </footer>
      </form>
    </section>
  </div>
</template>
