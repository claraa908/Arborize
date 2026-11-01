<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import DashboardReportModal from '@/components/dashboard/DashboardReportModal.vue'
import { useDashboardStore } from '@/stores/dashboardStore'
import type { DashboardGoal, DashboardReportRequest } from '@/types/dashboard'

const dashboardStore = useDashboardStore()
const zone = ref('Todas')
const today = new Date()
const year = ref(today.getFullYear())
const quarter = ref(Math.floor(today.getMonth() / 3) + 1)
const isReportModalOpen = ref(false)
const isGeneratingReport = ref(false)

const yearOptions = computed(() =>
  [...new Set([year.value, ...(dashboardStore.goals?.anosDisponiveis ?? [])])].sort((a, b) => b - a),
)
const quarterOptions = [1, 2, 3, 4]
const hasEvolutionData = computed(
  () =>
    dashboardStore.overview?.evolucao.some((month) => month.mudasDoadas > 0 || month.mudasPlantadas > 0) ??
    false,
)
const hasSpeciesStock = computed(() => (dashboardStore.overview?.estoquePorEspecie.length ?? 0) > 0)
const speciesColors = [
  'var(--color-amma-green-dark)',
  'var(--color-amma-green)',
  'var(--color-feedback-info)',
  'var(--color-feedback-warning)',
  'var(--color-amma-green-light)',
]

const speciesGradient = computed(() => {
  const species = dashboardStore.overview?.estoquePorEspecie ?? []
  const total = species.reduce((sum, item) => sum + item.quantidade, 0)
  if (total === 0) return 'conic-gradient(var(--color-amma-green-light) 0deg 360deg)'

  let start = 0
  const segments = species.map((item, index) => {
    const end = start + (item.quantidade / total) * 360
    const segment = `${speciesColors[index % speciesColors.length]} ${start}deg ${end}deg`
    start = end
    return segment
  })
  return `conic-gradient(${segments.join(', ')})`
})

const chartPoints = (key: 'mudasDoadas' | 'mudasPlantadas') => {
  const rows = dashboardStore.overview?.evolucao ?? []
  const maxValue = Math.max(1, ...rows.map((row) => row[key]))
  return rows
    .map((row, index) => {
      const x = rows.length <= 1 ? 350 : 42 + (index * 616) / (rows.length - 1)
      const y = 190 - (row[key] / maxValue) * 160
      return `${x},${y}`
    })
    .join(' ')
}

const chartLabels = computed(() =>
  (dashboardStore.overview?.evolucao ?? []).filter((_, index) => index % 2 === 0),
)

const progressPercent = (goal: DashboardGoal) =>
  goal.metaPlantio === 0 ? 0 : Math.min(100, (goal.realizado / goal.metaPlantio) * 100)

const formatNumber = (value: number) => new Intl.NumberFormat('pt-BR').format(value)

const fetchGoals = () =>
  dashboardStore.fetchGoals({
    zone: zone.value,
    year: year.value,
    quarter: quarter.value,
  })

watch([zone, year, quarter], () => {
  void fetchGoals()
})

onMounted(() => {
  void dashboardStore.fetchOverview()
  void fetchGoals()
})

const generateReport = async (request: DashboardReportRequest) => {
  isGeneratingReport.value = true
  try {
    const file = await dashboardStore.generateReport(request)
    if (!file) return

    const fileUrl = URL.createObjectURL(file)
    const link = document.createElement('a')
    link.href = fileUrl
    link.download = `relatorio-impacto-ambiental_${request.dataInicial}_${request.dataFinal}.${request.formato.toLowerCase()}`
    document.body.append(link)
    link.click()
    link.remove()
    window.setTimeout(() => URL.revokeObjectURL(fileUrl), 1000)
    isReportModalOpen.value = false
  } finally {
    isGeneratingReport.value = false
  }
}
</script>

<template>
  <div class="flex flex-col gap-6 px-6 pb-8 pt-6 md:px-10">
    <header class="flex flex-col justify-between gap-4 sm:flex-row sm:items-center">
      <div>
        <p class="text-xs font-bold uppercase tracking-wider text-amma-green">Indicadores da AMMA</p>
        <h1 class="mt-1 text-2xl font-bold text-amma-green-dark">Dashboard Gerencial</h1>
        <p class="mt-1 text-sm text-grey-text">Acompanhe os resultados operacionais e as metas de arborização.</p>
      </div>
      <button
        type="button"
        class="efeito-zoom inline-flex min-h-11 items-center justify-center gap-2 rounded-btn bg-amma-green px-5 py-2.5 font-bold text-white hover:bg-amma-green-dark focus-visible:outline-2 focus-visible:outline-amma-green"
        @click="isReportModalOpen = true"
      >
        <span class="material-symbols-outlined" aria-hidden="true">download</span>
        Exportar relatório
      </button>
    </header>

    <section aria-labelledby="operational-metrics-heading" class="space-y-4">
      <div class="flex items-center justify-between gap-3">
        <h2 id="operational-metrics-heading" class="text-lg font-bold text-amma-green-dark">
          Métricas operacionais
        </h2>
        <span class="text-xs text-grey-text">Dados consolidados confirmados</span>
      </div>

      <div v-if="dashboardStore.isLoadingOverview" role="status" aria-live="polite" class="grid gap-4 sm:grid-cols-2 xl:grid-cols-4" aria-label="Carregando indicadores">
        <div v-for="card in 4" :key="card" class="h-36 animate-pulse rounded-box border border-amma-green-light bg-amma-green-lighter" />
      </div>
      <div
        v-else-if="dashboardStore.overviewError"
        role="alert"
        class="flex flex-col gap-3 rounded-box border border-feedback-danger-border bg-feedback-danger-bg p-5 text-feedback-danger sm:flex-row sm:items-center sm:justify-between"
      >
        <p class="text-sm font-medium">{{ dashboardStore.overviewError }}</p>
        <button
          type="button"
          class="rounded-btn border border-feedback-danger-border px-4 py-2 text-sm font-bold focus-visible:outline-2 focus-visible:outline-feedback-danger"
          @click="dashboardStore.fetchOverview()"
        >
          Tentar novamente
        </button>
      </div>
      <div v-else-if="dashboardStore.overview" class="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <article class="rounded-box border border-amma-green-light bg-white p-5 shadow-sm">
          <div class="flex items-center justify-between">
            <p class="text-sm font-semibold text-grey-text">Mudas doadas / plantadas</p>
            <span class="material-symbols-outlined text-amma-green" aria-hidden="true">psychiatry</span>
          </div>
          <div class="mt-5 grid grid-cols-2 gap-3">
            <div>
              <p class="text-2xl font-bold text-amma-green-dark">{{ formatNumber(dashboardStore.overview.mudasDoadas) }}</p>
              <p class="mt-1 text-xs text-grey-text">Doadas</p>
            </div>
            <div>
              <p class="text-2xl font-bold text-amma-green-dark">{{ formatNumber(dashboardStore.overview.mudasPlantadas) }}</p>
              <p class="mt-1 text-xs text-grey-text">Plantadas</p>
            </div>
          </div>
        </article>

        <article class="rounded-box border border-amma-green-light bg-white p-5 shadow-sm">
          <div class="flex items-center justify-between">
            <p class="text-sm font-semibold text-grey-text">Estoque do viveiro</p>
            <span class="material-symbols-outlined text-amma-green" aria-hidden="true">inventory_2</span>
          </div>
          <p class="mt-5 text-3xl font-bold text-amma-green-dark">{{ formatNumber(dashboardStore.overview.estoqueAtual) }}</p>
          <p class="mt-1 text-xs text-grey-text">Mudas disponíveis</p>
        </article>

        <article class="rounded-box border border-amma-green-light bg-white p-5 shadow-sm">
          <div class="flex items-center justify-between">
            <p class="text-sm font-semibold text-grey-text">Garrafas PET arrecadadas</p>
            <span class="material-symbols-outlined text-amma-green" aria-hidden="true">recycling</span>
          </div>
          <p class="mt-5 text-3xl font-bold text-amma-green-dark">{{ formatNumber(dashboardStore.overview.garrafasPetArrecadadas) }}</p>
          <p class="mt-1 text-xs text-grey-text">Transações confirmadas</p>
        </article>

        <article class="rounded-box border border-amma-green-light bg-white p-5 shadow-sm">
          <div class="flex items-center justify-between">
            <p class="text-sm font-semibold text-grey-text">Substituições de Nim Indiano</p>
            <span class="material-symbols-outlined text-amma-green" aria-hidden="true">forest</span>
          </div>
          <p class="mt-5 text-3xl font-bold text-amma-green-dark">{{ formatNumber(dashboardStore.overview.substituicoesNim) }}</p>
          <p class="mt-1 text-xs text-grey-text">Removido e substituído por espécie nativa</p>
        </article>
      </div>
    </section>

    <section class="grid gap-5 xl:grid-cols-2" aria-label="Gráficos analíticos">
      <article class="min-w-0 rounded-box border border-amma-green-light bg-white p-5 shadow-sm md:p-6">
        <div class="mb-4">
          <h2 class="text-lg font-bold text-amma-green-dark">Evolução de doações e plantios</h2>
          <p class="mt-1 text-sm text-grey-text">Últimos 12 meses · registros confirmados</p>
        </div>
        <div v-if="dashboardStore.isLoadingOverview" role="status" aria-label="Carregando gráfico de evolução" class="h-56 animate-pulse rounded-btn bg-amma-green-lighter" />
        <p v-else-if="!hasEvolutionData" class="flex min-h-56 items-center justify-center rounded-btn bg-amma-green-lighter px-5 text-center text-sm text-grey-text">
          Ainda não há registros confirmados para exibir nesta evolução.
        </p>
        <div v-else class="min-w-0">
          <div class="overflow-x-auto">
            <svg
              viewBox="0 0 700 230"
              role="img"
              aria-label="Gráfico de linhas da evolução mensal de mudas doadas e plantadas"
              class="h-56 min-w-[560px] w-full"
            >
              <line x1="42" y1="190" x2="658" y2="190" stroke="var(--color-amma-green-light)" stroke-width="2" />
              <polyline :points="chartPoints('mudasDoadas')" fill="none" stroke="var(--color-amma-green-dark)" stroke-width="3" stroke-linecap="round" stroke-linejoin="round" />
              <polyline :points="chartPoints('mudasPlantadas')" fill="none" stroke="var(--color-feedback-info)" stroke-width="3" stroke-linecap="round" stroke-linejoin="round" />
              <text
                v-for="(month, index) in chartLabels"
                :key="month.mes"
                :x="42 + (dashboardStore.overview!.evolucao.indexOf(month) * 616) / 11"
                y="216"
                text-anchor="middle"
                fill="var(--color-grey-text)"
                font-size="11"
              >
                {{ month.mes.slice(5) }}/{{ month.mes.slice(2, 4) }}
              </text>
            </svg>
          </div>
          <div class="mt-2 flex flex-wrap gap-4 text-xs text-grey-text">
            <span class="inline-flex items-center gap-2"><i class="h-2.5 w-2.5 rounded-full bg-amma-green-dark" />Mudas doadas</span>
            <span class="inline-flex items-center gap-2"><i class="h-2.5 w-2.5 rounded-full bg-feedback-info" />Mudas plantadas</span>
          </div>
        </div>
      </article>

      <article class="min-w-0 rounded-box border border-amma-green-light bg-white p-5 shadow-sm md:p-6">
        <div class="mb-4">
          <h2 class="text-lg font-bold text-amma-green-dark">Espécies disponíveis no viveiro</h2>
          <p class="mt-1 text-sm text-grey-text">Distribuição do estoque atual por espécie</p>
        </div>
        <div v-if="dashboardStore.isLoadingOverview" role="status" aria-label="Carregando gráfico de espécies" class="h-56 animate-pulse rounded-btn bg-amma-green-lighter" />
        <p v-else-if="!hasSpeciesStock" class="flex min-h-56 items-center justify-center rounded-btn bg-amma-green-lighter px-5 text-center text-sm text-grey-text">
          Não há estoque de mudas cadastrado para compor o gráfico.
        </p>
        <div v-else class="flex min-h-56 flex-col items-center justify-center gap-5 sm:flex-row">
          <div
            class="relative h-40 w-40 shrink-0 rounded-full"
            :style="{ background: speciesGradient }"
            role="img"
            aria-label="Gráfico de rosca do estoque por espécie"
          >
            <div class="absolute inset-5 flex items-center justify-center rounded-full bg-white text-center">
              <div>
                <span class="block text-2xl font-bold text-amma-green-dark">{{ formatNumber(dashboardStore.overview!.estoqueAtual) }}</span>
                <span class="text-xs text-grey-text">mudas</span>
              </div>
            </div>
          </div>
          <ul class="w-full space-y-2">
            <li
              v-for="(item, index) in dashboardStore.overview!.estoquePorEspecie"
              :key="item.especie"
              class="flex items-center justify-between gap-3 text-sm"
            >
              <span class="flex min-w-0 items-center gap-2 text-grey-text">
                <i class="h-2.5 w-2.5 shrink-0 rounded-full" :style="{ backgroundColor: speciesColors[index % speciesColors.length] }" />
                <span class="truncate">{{ item.especie }}</span>
              </span>
              <strong class="shrink-0 text-amma-green-dark">{{ formatNumber(item.quantidade) }}</strong>
            </li>
          </ul>
        </div>
      </article>
    </section>

    <section aria-labelledby="goals-heading" class="space-y-4">
      <div class="flex flex-col justify-between gap-4 lg:flex-row lg:items-end">
        <div>
          <h2 id="goals-heading" class="text-lg font-bold text-amma-green-dark">Acompanhamento por zona</h2>
          <p class="mt-1 text-sm text-grey-text">Metas de plantio, execução e saldo líquido no período.</p>
        </div>
        <div class="grid gap-3 sm:grid-cols-3">
          <div>
            <label for="dashboard-zone" class="mb-1.5 block text-sm font-semibold text-grey-text">Bairro/Zona</label>
            <select
              id="dashboard-zone"
              v-model="zone"
              class="min-h-11 w-full rounded-btn border-2 border-amma-green-light bg-white px-3 text-sm text-grey-text outline-none focus:border-amma-green focus:bg-amma-green-lighter focus-visible:ring-2 focus-visible:ring-amma-green/30"
            >
              <option value="Todas">Todas as zonas</option>
              <option v-for="item in dashboardStore.goals?.zonasDisponiveis ?? []" :key="item" :value="item">
                {{ item }}
              </option>
            </select>
          </div>
          <div>
            <label for="dashboard-year" class="mb-1.5 block text-sm font-semibold text-grey-text">Ano</label>
            <select
              id="dashboard-year"
              v-model.number="year"
              class="min-h-11 w-full rounded-btn border-2 border-amma-green-light bg-white px-3 text-sm text-grey-text outline-none focus:border-amma-green focus:bg-amma-green-lighter focus-visible:ring-2 focus-visible:ring-amma-green/30"
            >
              <option v-for="item in yearOptions" :key="item" :value="item">{{ item }}</option>
            </select>
          </div>
          <div>
            <label for="dashboard-quarter" class="mb-1.5 block text-sm font-semibold text-grey-text">Trimestre</label>
            <select
              id="dashboard-quarter"
              v-model.number="quarter"
              class="min-h-11 w-full rounded-btn border-2 border-amma-green-light bg-white px-3 text-sm text-grey-text outline-none focus:border-amma-green focus:bg-amma-green-lighter focus-visible:ring-2 focus-visible:ring-amma-green/30"
            >
              <option v-for="item in quarterOptions" :key="item" :value="item">{{ item }}º trimestre</option>
            </select>
          </div>
        </div>
      </div>

      <div
        v-if="dashboardStore.isLoadingGoals"
        role="status"
        aria-live="polite"
        class="grid gap-4 md:grid-cols-2"
        aria-label="Carregando metas do período"
      >
        <div v-for="item in 2" :key="item" class="h-24 animate-pulse rounded-box bg-amma-green-lighter" />
      </div>
      <div
        v-else-if="dashboardStore.goalsError"
        role="alert"
        class="flex flex-col gap-3 rounded-box border border-feedback-danger-border bg-feedback-danger-bg p-5 text-feedback-danger sm:flex-row sm:items-center sm:justify-between"
      >
        <p class="text-sm font-medium">{{ dashboardStore.goalsError }}</p>
        <button
          type="button"
          class="rounded-btn border border-feedback-danger-border px-4 py-2 text-sm font-bold focus-visible:outline-2 focus-visible:outline-feedback-danger"
          @click="fetchGoals()"
        >
          Tentar novamente
        </button>
      </div>
      <div v-else-if="dashboardStore.goals" class="space-y-4">
        <div v-if="dashboardStore.goals.metas.length" class="grid gap-4 md:grid-cols-2">
          <article
            v-for="goal in dashboardStore.goals.metas"
            :key="`${goal.zona}-${goal.bairro}`"
            class="rounded-box border border-amma-green-light bg-white p-5 shadow-sm"
          >
            <div class="mb-3 flex items-start justify-between gap-3">
              <div>
                <h3 class="font-bold text-amma-green-dark">{{ goal.bairro }}</h3>
                <p class="text-xs text-grey-text">{{ goal.zona }}</p>
              </div>
              <span class="text-sm font-bold text-amma-green-dark">
                {{ goal.metaPlantio === 0 ? 'Sem meta' : `${Math.round(progressPercent(goal))}%` }}
              </span>
            </div>
            <div
              class="h-3 overflow-hidden rounded-full bg-amma-bg"
              role="progressbar"
              :aria-label="`Progresso de plantio em ${goal.bairro}`"
              :aria-valuenow="Math.round(progressPercent(goal))"
              aria-valuemin="0"
              aria-valuemax="100"
            >
              <div
                class="h-full rounded-full bg-amma-green transition-[width]"
                :style="{ width: `${progressPercent(goal)}%` }"
              />
            </div>
            <p class="mt-2 text-xs text-grey-text">
              {{ formatNumber(goal.realizado) }} de {{ formatNumber(goal.metaPlantio) }} mudas realizadas
            </p>
          </article>
        </div>

        <div class="overflow-hidden rounded-box border border-amma-green-light bg-white shadow-sm">
          <div class="overflow-x-auto">
            <table class="w-full min-w-[760px] border-collapse text-left">
              <caption class="sr-only">
                Metas de plantio e saldo líquido por bairro no {{ dashboardStore.goals.trimestre }}º trimestre de {{ dashboardStore.goals.ano }}
              </caption>
              <thead class="bg-amma-green-lighter text-xs uppercase tracking-wide text-amma-green-dark">
                <tr>
                  <th scope="col" class="px-5 py-4 font-bold">Bairro</th>
                  <th scope="col" class="px-5 py-4 font-bold">Meta de plantio</th>
                  <th scope="col" class="px-5 py-4 font-bold">Realizado</th>
                  <th scope="col" class="px-5 py-4 font-bold">Saldo líquido</th>
                  <th scope="col" class="px-5 py-4 font-bold">Prioridade</th>
                </tr>
              </thead>
              <tbody v-if="dashboardStore.goals.metas.length" class="divide-y divide-amma-green-light">
                <tr v-for="goal in dashboardStore.goals.metas" :key="`${goal.zona}-${goal.bairro}-table`">
                  <th scope="row" class="px-5 py-4 text-sm font-semibold text-grey-text">
                    {{ goal.bairro }}
                    <span class="mt-0.5 block text-xs font-normal">{{ goal.zona }}</span>
                  </th>
                  <td class="px-5 py-4 text-sm text-grey-text">{{ formatNumber(goal.metaPlantio) }}</td>
                  <td class="px-5 py-4 text-sm text-grey-text">{{ formatNumber(goal.realizado) }}</td>
                  <td class="px-5 py-4 text-sm font-semibold text-grey-text">
                    {{ goal.saldoLiquido > 0 ? '+' : '' }}{{ formatNumber(goal.saldoLiquido) }}
                  </td>
                  <td class="px-5 py-4">
                    <span
                      :class="[
                        'inline-flex rounded-full px-3 py-1 text-xs font-bold',
                        goal.prioridade === 'Alta'
                          ? 'bg-feedback-danger-bg text-feedback-danger'
                          : goal.prioridade === 'Média'
                            ? 'bg-feedback-warning-bg text-feedback-warning'
                            : goal.prioridade === 'Baixa'
                              ? 'bg-feedback-success-bg text-feedback-success'
                              : 'bg-amma-bg text-grey-text',
                      ]"
                    >
                      {{ goal.prioridade }}
                    </span>
                    <span class="sr-only">, déficit de {{ goal.deficitPercentual.toLocaleString('pt-BR', { maximumFractionDigits: 1 }) }}%</span>
                  </td>
                </tr>
              </tbody>
              <tbody v-else>
                <tr>
                  <td colspan="5" class="px-5 py-12 text-center">
                    <p class="font-semibold text-amma-green-dark">Nenhuma meta encontrada neste recorte.</p>
                    <p class="mt-1 text-sm text-grey-text">Altere a zona ou o período para consultar outros registros.</p>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
        <p class="text-xs text-grey-text">
          Saldo líquido = plantios realizados − supressões autorizadas na zona. A prioridade deriva do déficit percentual da meta.
        </p>
      </div>
    </section>
  </div>

  <DashboardReportModal
    v-if="isReportModalOpen"
    :is-generating="isGeneratingReport"
    @close="isReportModalOpen = false"
    @generate="generateReport"
  />
</template>