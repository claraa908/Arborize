<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import capaSobreNos from '@/assets/images/capa-sobre-nos.jpeg'
import { sobreService } from '@/services/sobreService'
import type { SobreIndicadoresDTO } from '@/types/sobre'

interface PilarInstitucional {
  id: string
  titulo: string
  descricao: string
  icone: string
}

interface IndicadorCard {
  id: keyof SobreIndicadoresDTO
  titulo: string
  unidade: string
  icone: string
  valor: number
  percentualBarra: number
}

const pilares: PilarInstitucional[] = [
  {
    id: 'missao',
    titulo: 'Nossa missão',
    descricao:
      'Ampliar a cobertura arbórea de Quixadá e proteger o bioma Caatinga com a participação ativa da população.',
    icone: 'flag',
  },
  {
    id: 'atuacao',
    titulo: 'O que fazemos',
    descricao:
      'Doação de mudas pelo Eco Ponto, mutirões de plantio, inventário arbóreo georreferenciado, laudos técnicos e substituição de espécies invasoras.',
    icone: 'psychiatry',
  },
  {
    id: 'parceiros',
    titulo: 'Parceiros',
    descricao:
      'Laboratório e viveiro do IFCE na produção e entrega de mudas nativas, além de associações de catadores no recebimento de recicláveis.',
    icone: 'account_balance',
  },
]

const atalhosServicos = [
  {
    label: 'Arborização',
    routeName: 'arborizacao',
    icone: 'park',
  },
  {
    label: 'Eventos',
    routeName: 'eventos-publico',
    icone: 'calendar_month',
  },
  {
    label: 'Biblioteca Digital',
    routeName: 'biblioteca-catalogo',
    icone: 'local_library',
  },
]

const formatadorNumero = new Intl.NumberFormat('pt-BR')

const indicadores = ref<SobreIndicadoresDTO | null>(null)
const isLoadingIndicadores = ref(true)
const erroIndicadores = ref<string | null>(null)

const calcularPercentual = (valor: number, maximo: number): number => {
  if (valor <= 0 || maximo <= 0) return 0
  const percentual = Math.round((valor / maximo) * 100)
  return Math.max(12, Math.min(100, percentual))
}

const cardsIndicadores = computed<IndicadorCard[]>(() => {
  const dados = indicadores.value
  if (!dados) return []

  const maiorValor = Math.max(
    dados.mudasDoadas,
    dados.estoqueViveiro,
    dados.garrafasPetArrecadadas,
    dados.ninsSubstituidos,
    1,
  )

  return [
    {
      id: 'mudasDoadas',
      titulo: 'Mudas doadas',
      unidade: 'plantadas',
      icone: 'psychiatry',
      valor: dados.mudasDoadas,
      percentualBarra: calcularPercentual(dados.mudasDoadas, maiorValor),
    },
    {
      id: 'estoqueViveiro',
      titulo: 'Estoque do viveiro',
      unidade: 'mudas disponíveis',
      icone: 'inventory_2',
      valor: dados.estoqueViveiro,
      percentualBarra: calcularPercentual(dados.estoqueViveiro, maiorValor),
    },
    {
      id: 'garrafasPetArrecadadas',
      titulo: 'Garrafas PET',
      unidade: 'arrecadadas',
      icone: 'recycling',
      valor: dados.garrafasPetArrecadadas,
      percentualBarra: calcularPercentual(dados.garrafasPetArrecadadas, maiorValor),
    },
    {
      id: 'ninsSubstituidos',
      titulo: 'Nins substituídos',
      unidade: 'árvores',
      icone: 'verified',
      valor: dados.ninsSubstituidos,
      percentualBarra: calcularPercentual(dados.ninsSubstituidos, maiorValor),
    },
  ]
})

const formatarValor = (valor: number): string => formatadorNumero.format(valor)

const carregarIndicadores = async () => {
  isLoadingIndicadores.value = true
  erroIndicadores.value = null

  try {
    indicadores.value = await sobreService.getIndicadores()
  } catch {
    indicadores.value = null
    erroIndicadores.value = 'Não foi possível carregar os indicadores ambientais no momento.'
  } finally {
    isLoadingIndicadores.value = false
  }
}

onMounted(() => {
  void carregarIndicadores()
})
</script>

<template>
  <div class="max-w-7xl mx-auto flex flex-col gap-6 pt-2 pb-10">
    <!-- Hero Institucional -->
    <section
      aria-labelledby="sobre-nos-titulo"
      class="relative overflow-hidden rounded-box py-6 px-5 md:py-8 md:px-8 text-center shadow-md"
      style="isolation: isolate;"
    >
      <img
        :src="capaSobreNos"
        alt=""
        aria-hidden="true"
        class="absolute inset-0 w-full h-full object-cover rounded-[inherit] pointer-events-none select-none blur-[2px] scale-105"
      />

      <div class="relative z-10 flex flex-col items-center gap-3 md:gap-4">
        <div
          class="bg-white/95 rounded-full w-12 h-12 md:w-14 md:h-14 flex items-center justify-center border-2 border-white shadow-md shrink-0"
        >
          <span
            class="material-symbols-outlined text-amma-green select-none !leading-none text-[30px] md:text-[32px]"
            aria-hidden="true"
          >
            eco
          </span>
        </div>

        <div class="flex flex-col items-center gap-2.5 max-w-3xl mx-auto">
          <h1
            id="sobre-nos-titulo"
            class="inline-block bg-white/30 backdrop-blur-md px-5 py-2 rounded-btn border border-white/50 shadow-sm text-xl sm:text-2xl lg:text-3xl font-black text-amma-green-dark tracking-tight leading-tight drop-shadow-sm"
          >
            Autarquia Municipal do Meio Ambiente de Quixadá
          </h1>

          <p
            class="inline-block max-w-2xl bg-white/30 backdrop-blur-md px-5 py-2 rounded-full text-gray-800 text-xs md:text-sm font-semibold shadow-sm border border-white/50 leading-relaxed"
          >
            Planejamos, fiscalizamos e executamos as ações ambientais do município, com foco nas espécies nativas da Caatinga.
          </p>
        </div>
      </div>
    </section>

    <!-- Pilares Institucionais -->
    <section aria-label="Pilares institucionais da AMMA" class="grid grid-cols-1 md:grid-cols-3 gap-5">
      <article
        v-for="pilar in pilares"
        :key="pilar.id"
        class="bg-white rounded-box border border-amma-green-light p-6 shadow-xs hover:shadow-md hover:border-amma-green/60 transition-all duration-300 flex flex-col gap-3"
      >
        <div
          class="w-11 h-11 rounded-btn bg-amma-green-lighter border border-amma-green-light flex items-center justify-center text-amma-green shrink-0"
        >
          <span class="material-symbols-outlined text-[22px]" aria-hidden="true">{{ pilar.icone }}</span>
        </div>

        <h2 class="text-lg font-bold text-amma-green-dark leading-snug">
          {{ pilar.titulo }}
        </h2>

        <p class="text-sm text-grey-text leading-relaxed">
          {{ pilar.descricao }}
        </p>
      </article>
    </section>

    <!-- Indicadores de Impacto Ambiental -->
    <section aria-labelledby="indicadores-ambientais-titulo" class="flex flex-col gap-3">
      <div class="flex flex-wrap items-center justify-between gap-2 px-1">
        <h2 id="indicadores-ambientais-titulo" class="text-base font-bold text-amma-green-dark flex items-center gap-2">
          <span class="material-symbols-outlined text-[20px] text-amma-green" aria-hidden="true">monitoring</span>
          Impacto Ambiental em Tempo Real
        </h2>
        <span class="text-xs font-medium text-grey-text">
          Dados atualizados diretamente da operação da AMMA e do viveiro municipal
        </span>
      </div>

      <!-- Loading Skeleton -->
      <div
        v-if="isLoadingIndicadores"
        role="status"
        aria-live="polite"
        class="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-4 gap-5"
      >
        <p class="sr-only">Carregando indicadores ambientais...</p>
        <div
          v-for="n in 4"
          :key="n"
          class="bg-white rounded-box border border-amma-green-light p-5 shadow-xs animate-pulse flex flex-col gap-3"
        >
          <div class="flex items-center justify-between">
            <div class="h-3.5 w-28 bg-amma-green-light rounded-full"></div>
            <div class="h-7 w-7 bg-amma-green-lighter rounded-full"></div>
          </div>
          <div class="h-8 w-24 bg-amma-green-light rounded-full"></div>
          <div class="h-3 w-20 bg-amma-bg rounded-full"></div>
          <div class="h-2 w-full bg-amma-green-light/70 rounded-full mt-1"></div>
        </div>
      </div>

      <!-- Estado de Erro Isolado -->
      <div
        v-else-if="erroIndicadores"
        role="alert"
        class="bg-feedback-danger-bg rounded-box border border-feedback-danger-border p-6 flex flex-col sm:flex-row items-center justify-between gap-4"
      >
        <div class="flex items-center gap-3 text-center sm:text-left">
          <span class="material-symbols-outlined text-3xl text-feedback-danger shrink-0" aria-hidden="true">
            error_outline
          </span>
          <div>
            <p class="text-sm font-bold text-feedback-danger">Indicadores temporariamente indisponíveis</p>
            <p class="text-xs text-grey-text mt-0.5">{{ erroIndicadores }}</p>
          </div>
        </div>

        <button
          type="button"
          class="bg-amma-green hover:bg-amma-green-dark text-white font-bold py-2 px-5 rounded-btn transition-colors text-xs inline-flex items-center gap-1.5 cursor-pointer efeito-zoom shrink-0 focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-amma-green"
          @click="carregarIndicadores"
        >
          <span class="material-symbols-outlined text-[16px]" aria-hidden="true">refresh</span>
          Tentar novamente
        </button>
      </div>

      <!-- Cards de Indicadores Reais -->
      <div v-else class="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-4 gap-5">
        <article
          v-for="card in cardsIndicadores"
          :key="card.id"
          class="bg-white rounded-box border border-amma-green-light p-5 shadow-xs hover:shadow-md hover:border-amma-green/60 transition-all duration-300 flex flex-col justify-between gap-3"
        >
          <div class="flex items-start justify-between gap-2">
            <h3 class="text-xs font-bold uppercase tracking-wider text-grey-text">
              {{ card.titulo }}
            </h3>
            <span
              class="w-8 h-8 rounded-full bg-amma-green-lighter text-amma-green flex items-center justify-center shrink-0"
              aria-hidden="true"
            >
              <span class="material-symbols-outlined text-[18px]">{{ card.icone }}</span>
            </span>
          </div>

          <div>
            <p class="text-3xl font-black text-amma-green-dark tracking-tight leading-none">
              {{ formatarValor(card.valor) }}
            </p>
            <p class="text-xs font-medium text-grey-text mt-1.5">
              {{ card.unidade }}
            </p>
          </div>

          <div
            class="w-full h-2 rounded-full bg-amma-green-light/70 overflow-hidden"
            role="progressbar"
            :aria-label="`${card.titulo}: ${formatarValor(card.valor)} ${card.unidade}`"
            :aria-valuenow="card.percentualBarra"
            aria-valuemin="0"
            aria-valuemax="100"
          >
            <div
              class="h-full rounded-full bg-amma-green transition-all duration-500"
              :style="{ width: `${card.percentualBarra}%` }"
            ></div>
          </div>
        </article>
      </div>
    </section>

    <!-- Bloco Fale com a AMMA -->
    <section
      aria-labelledby="fale-com-amma-titulo"
      class="bg-white rounded-box border border-amma-green-light p-6 md:p-7 shadow-xs flex flex-col lg:flex-row lg:items-center lg:justify-between gap-5"
    >
      <div class="flex items-start gap-4">
        <div
          class="w-12 h-12 rounded-btn bg-amma-green-lighter border border-amma-green-light flex items-center justify-center text-amma-green shrink-0"
        >
          <span class="material-symbols-outlined text-[24px]" aria-hidden="true">location_on</span>
        </div>

        <div>
          <h2 id="fale-com-amma-titulo" class="text-lg font-bold text-amma-green-dark">
            Fale com a AMMA
          </h2>
          <p class="text-sm text-grey-text mt-1 leading-relaxed">
            Quixadá, Ceará — atendimento presencial de segunda a sexta-feira, em horário comercial.
          </p>
        </div>
      </div>

      <nav aria-label="Atalhos para serviços públicos" class="flex flex-wrap items-center gap-2.5 shrink-0">
        <RouterLink
          v-for="atalho in atalhosServicos"
          :key="atalho.routeName"
          :to="{ name: atalho.routeName }"
          class="inline-flex items-center gap-1.5 rounded-btn border-2 border-amma-green-light bg-amma-green-lighter px-4 py-2 text-xs font-bold text-amma-green-dark hover:border-amma-green hover:bg-amma-green hover:text-white transition-colors focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-amma-green"
        >
          <span class="material-symbols-outlined text-[16px]" aria-hidden="true">{{ atalho.icone }}</span>
          {{ atalho.label }}
        </RouterLink>
      </nav>
    </section>
  </div>
</template>
