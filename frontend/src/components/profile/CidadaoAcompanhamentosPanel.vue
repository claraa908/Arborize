<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import type {
  SolicitacaoMudaAcompanhamento,
  SubstituicaoAcompanhamento,
} from '@/services/usuarioService'

type FiltroTipo = 'TODOS' | 'MUDAS' | 'SUBSTITUICOES'
type EstadoEtapaKey = 'concluida' | 'atual' | 'futura' | 'rejeitada'

interface StatusVisual {
  label: string
  icon: string
  badgeClass: string
}

interface EtapaTimeline {
  titulo: string
  descricao: string
  data?: string
  estado: EstadoEtapaKey
}

interface ItemUnificado {
  uid: string
  tipo: 'MUDAS' | 'SUBSTITUICOES'
  protocolo: string
  titulo: string
  status: string
  criadaEm: string
  timeline: EtapaTimeline[]
}

const props = defineProps<{
  solicitacoesMuda: SolicitacaoMudaAcompanhamento[]
  substituicoesBotanicas: SubstituicaoAcompanhamento[]
  isLoading: boolean
  errorMessage: string
  initialFiltro?: FiltroTipo
  initialProtocolo?: string
}>()

const emit = defineEmits<{
  (e: 'retry'): void
}>()

const filtroAtivo = ref<FiltroTipo>(props.initialFiltro ?? 'TODOS')
const cardExpandidoUid = ref<string | null>(null)

watch(
  () => props.initialFiltro,
  (novoFiltro) => {
    if (novoFiltro) filtroAtivo.value = novoFiltro
  },
)

const toggleExpandir = (uid: string) => {
  cardExpandidoUid.value = cardExpandidoUid.value === uid ? null : uid
}

const statusVisualMap: Record<string, StatusVisual> = {
  PENDENTE_RECICLAVEIS: {
    label: 'Entrega de recicláveis',
    icon: 'recycling',
    badgeClass: 'bg-feedback-warning-bg text-feedback-warning border border-feedback-warning-border',
  },
  APROVADA: {
    label: 'Aprovada',
    icon: 'check_circle',
    badgeClass: 'bg-feedback-success-bg text-feedback-success border border-feedback-success-border',
  },
  RETIRADA_CONCLUIDA: {
    label: 'Retirada concluída',
    icon: 'task_alt',
    badgeClass: 'bg-amma-green text-white border border-amma-green',
  },
  REJEITADA: {
    label: 'Rejeitada',
    icon: 'cancel',
    badgeClass: 'bg-feedback-danger-bg text-feedback-danger border border-feedback-danger-border',
  },
  RECEBIDA: {
    label: 'Recebida pela AMMA',
    icon: 'inbox',
    badgeClass: 'bg-feedback-info-bg text-feedback-info border border-feedback-info-border',
  },
  EM_ANALISE: {
    label: 'Em análise',
    icon: 'manage_search',
    badgeClass: 'bg-feedback-info-bg text-feedback-info border border-feedback-info-border',
  },
  LAUDO_EMITIDO: {
    label: 'Laudo emitido',
    icon: 'description',
    badgeClass: 'bg-feedback-success-bg text-feedback-success border border-feedback-success-border',
  },
  RETIRADA_EM_ANDAMENTO: {
    label: 'Retirada em andamento',
    icon: 'engineering',
    badgeClass: 'bg-feedback-warning-bg text-feedback-warning border border-feedback-warning-border',
  },
  REPLANTIO_EM_ANDAMENTO: {
    label: 'Reposição em andamento',
    icon: 'park',
    badgeClass: 'bg-feedback-info-bg text-feedback-info border border-feedback-info-border',
  },
  CONCLUIDA: {
    label: 'Concluída',
    icon: 'verified',
    badgeClass: 'bg-amma-green text-white border border-amma-green',
  },
}

const fluxoPadraoSubstituicao: Array<{ status: string; titulo: string; descricaoPadrao: string }> = [
  {
    status: 'RECEBIDA',
    titulo: 'Recebida pela AMMA',
    descricaoPadrao: 'Requerimento registrado no portal Arborize.',
  },
  {
    status: 'EM_ANALISE',
    titulo: 'Em análise',
    descricaoPadrao: 'Avaliação técnica e vistoria pela equipe da AMMA.',
  },
  {
    status: 'APROVADA',
    titulo: 'Aprovada',
    descricaoPadrao: 'Substituição autorizada após análise técnica.',
  },
  {
    status: 'LAUDO_EMITIDO',
    titulo: 'Laudo emitido',
    descricaoPadrao: 'Emissão do laudo técnico de supressão e definição da espécie nativa.',
  },
  {
    status: 'RETIRADA_EM_ANDAMENTO',
    titulo: 'Retirada em andamento',
    descricaoPadrao: 'Remoção controlada da espécie invasora no endereço informado.',
  },
  {
    status: 'REPLANTIO_EM_ANDAMENTO',
    titulo: 'Reposição em andamento',
    descricaoPadrao: 'Plantio da muda nativa da Caatinga em substituição.',
  },
  {
    status: 'CONCLUIDA',
    titulo: 'Concluída',
    descricaoPadrao: 'Substituição botânica finalizada pela AMMA.',
  },
]

const estadoEtapaMeta: Record<
  EstadoEtapaKey,
  { label: string; icon: string; markerClass: string; titleClass: string }
> = {
  concluida: {
    label: 'concluída',
    icon: 'check',
    markerClass: 'bg-amma-green text-white border-amma-green',
    titleClass: 'text-amma-green-dark',
  },
  atual: {
    label: 'em andamento',
    icon: 'hourglass_top',
    markerClass: 'bg-feedback-warning-bg text-feedback-warning border-feedback-warning',
    titleClass: 'text-amma-green-dark',
  },
  futura: {
    label: 'próxima etapa',
    icon: 'lock',
    markerClass: 'bg-amma-bg text-grey-text/60 border-amma-green-light',
    titleClass: 'text-grey-text/75',
  },
  rejeitada: {
    label: 'encerrada',
    icon: 'close',
    markerClass: 'bg-feedback-danger-bg text-feedback-danger border-feedback-danger',
    titleClass: 'text-feedback-danger',
  },
}

const obterStatusVisual = (status: string): StatusVisual =>
  statusVisualMap[status] ?? {
    label: status.replace(/_/g, ' '),
    icon: 'info',
    badgeClass: 'bg-amma-bg text-grey-text border border-amma-green-light',
  }

const formatarData = (iso?: string): string => {
  if (!iso) return 'Data não informada'
  const date = new Date(iso)
  if (Number.isNaN(date.getTime())) return iso
  return date.toLocaleDateString('pt-BR', { day: '2-digit', month: '2-digit', year: 'numeric' })
}

const formatarDataHora = (iso?: string): string => {
  if (!iso) return ''
  const date = new Date(iso)
  if (Number.isNaN(date.getTime())) return iso
  return date.toLocaleString('pt-BR', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  })
}

const construirTimelineMuda = (m: SolicitacaoMudaAcompanhamento): EtapaTimeline[] => {
  const enviada: EtapaTimeline = {
    titulo: 'Solicitação enviada',
    descricao: `Pedido de ${m.quantidadeMudas} ${m.quantidadeMudas === 1 ? 'muda' : 'mudas'} de ${m.especieNome} registrado no Arborize.`,
    data: formatarDataHora(m.criadaEm),
    estado: 'concluida',
  }

  if (m.status === 'REJEITADA') {
    return [
      enviada,
      {
        titulo: 'Solicitação rejeitada',
        descricao: m.motivoRejeicao || 'A solicitação foi encerrada após análise da AMMA.',
        estado: 'rejeitada',
      },
    ]
  }

  const recebidos = m.quantidadeRecebida ?? 0
  const entregue = m.status === 'RETIRADA_CONCLUIDA'
  const aprovada = m.status === 'APROVADA' || entregue

  return [
    enviada,
    {
      titulo: 'Entrega de recicláveis',
      descricao: aprovada
        ? `${recebidos}/${m.totalRecicaveis} garrafas PET conferidas pela AMMA.`
        : `Entregue ${m.totalRecicaveis} garrafas PET na AMMA (${recebidos}/${m.totalRecicaveis} conferidas).`,
      estado: aprovada ? 'concluida' : 'atual',
    },
    {
      titulo: 'Aprovação do pedido',
      descricao: aprovada
        ? 'A AMMA aprovou o pedido e liberou a retirada da muda.'
        : 'A AMMA aprova o pedido após conferir os recicláveis.',
      estado: aprovada ? 'concluida' : 'futura',
    },
    {
      titulo: 'Retirada da muda',
      descricao: entregue
        ? 'Entrega da muda registrada no laboratório do IFCE.'
        : aprovada
          ? 'Pronta para retirada. Apresente no IFCE a autorização emitida pela AMMA.'
          : 'Disponível após a aprovação da AMMA.',
      estado: entregue ? 'concluida' : aprovada ? 'atual' : 'futura',
    },
  ]
}

const construirTimelineSubstituicao = (s: SubstituicaoAcompanhamento): EtapaTimeline[] => {
  const eventosReais = s.historico ?? []
  const etapas: EtapaTimeline[] = []

  if (eventosReais.length > 0) {
    eventosReais.forEach((ev, index) => {
      const isUltimo = index === eventosReais.length - 1
      const estado: EstadoEtapaKey =
        ev.status === 'REJEITADA'
          ? 'rejeitada'
          : ev.status === 'CONCLUIDA' || !isUltimo
            ? 'concluida'
            : 'atual'

      etapas.push({
        titulo: obterStatusVisual(ev.status).label,
        descricao: ev.descricao,
        data: formatarDataHora(ev.criadoEm),
        estado,
      })
    })
  } else {
    const idxAtualSemHistorico = fluxoPadraoSubstituicao.findIndex((e) => e.status === s.status)
    if (s.status === 'REJEITADA') {
      etapas.push(
        {
          titulo: 'Recebida pela AMMA',
          descricao: 'Requerimento registrado no portal Arborize.',
          data: formatarDataHora(s.criadaEm),
          estado: 'concluida',
        },
        {
          titulo: 'Não aprovada',
          descricao: 'A solicitação foi encerrada após análise da AMMA.',
          estado: 'rejeitada',
        },
      )
    } else if (idxAtualSemHistorico >= 0) {
      fluxoPadraoSubstituicao.slice(0, idxAtualSemHistorico + 1).forEach((step, idx) => {
        const isUltimo = idx === idxAtualSemHistorico
        etapas.push({
          titulo: step.titulo,
          descricao: step.descricaoPadrao,
          data: idx === 0 ? formatarDataHora(s.criadaEm) : undefined,
          estado: s.status === 'CONCLUIDA' || !isUltimo ? 'concluida' : 'atual',
        })
      })
    } else {
      etapas.push({
        titulo: obterStatusVisual(s.status).label,
        descricao: s.descricaoArvore,
        data: formatarDataHora(s.criadaEm),
        estado: 'atual',
      })
    }
  }

  // Adiciona as próximas etapas bloqueadas quando o processo ainda está em andamento
  if (s.status !== 'CONCLUIDA' && s.status !== 'REJEITADA') {
    const idxAtual = fluxoPadraoSubstituicao.findIndex((e) => e.status === s.status)
    if (idxAtual >= 0) {
      for (const futura of fluxoPadraoSubstituicao.slice(idxAtual + 1)) {
        etapas.push({
          titulo: futura.titulo,
          descricao: futura.descricaoPadrao,
          estado: 'futura',
        })
      }
    }
  }

  return etapas
}

const itensUnificados = computed<ItemUnificado[]>(() => {
  const mudas: ItemUnificado[] = props.solicitacoesMuda.map((m) => ({
    uid: `muda-${m.id}`,
    tipo: 'MUDAS',
    protocolo: m.protocolo,
    titulo: `${m.especieNome} (${m.quantidadeMudas} ${m.quantidadeMudas === 1 ? 'muda' : 'mudas'} • ${m.totalRecicaveis} PET)`,
    status: m.status,
    criadaEm: m.criadaEm,
    timeline: construirTimelineMuda(m),
  }))

  const substituicoes: ItemUnificado[] = props.substituicoesBotanicas.map((s) => ({
    uid: `sub-${s.id}`,
    tipo: 'SUBSTITUICOES',
    protocolo: s.protocolo,
    titulo: `${s.logradouro}, ${s.numeroEndereco} — ${s.bairro}`,
    status: s.status,
    criadaEm: s.criadaEm,
    timeline: construirTimelineSubstituicao(s),
  }))

  return [...mudas, ...substituicoes].sort(
    (a, b) => new Date(b.criadaEm).getTime() - new Date(a.criadaEm).getTime(),
  )
})

watch(
  [itensUnificados, () => props.initialProtocolo],
  ([itens, protocolo]) => {
    if (!protocolo) return
    const alvo = itens.find((item) => item.protocolo.toLowerCase() === protocolo.toLowerCase())
    if (alvo) {
      cardExpandidoUid.value = alvo.uid
    }
  },
  { immediate: true },
)

const totalGeral = computed(() => itensUnificados.value.length)

const itensFiltrados = computed(() => {
  if (filtroAtivo.value === 'TODOS') return itensUnificados.value
  return itensUnificados.value.filter((item) => item.tipo === filtroAtivo.value)
})

const filtros = computed<Array<{ key: FiltroTipo; label: string; count: number }>>(() => [
  { key: 'TODOS', label: 'Todos', count: totalGeral.value },
  { key: 'MUDAS', label: 'Solicitações de Mudas', count: props.solicitacoesMuda.length },
  { key: 'SUBSTITUICOES', label: 'Substituição de Nim', count: props.substituicoesBotanicas.length },
])
</script>

<template>
  <section aria-labelledby="acompanhamentos-titulo" class="flex flex-col gap-5 h-full min-h-0">
    <header class="shrink-0 border-b border-amma-green-light pb-4 flex flex-col gap-4">
      <div>
        <h2 id="acompanhamentos-titulo" class="text-xl font-bold text-amma-green-dark flex items-center gap-2">
          <span class="material-symbols-outlined text-amma-green text-[22px]" aria-hidden="true">assignment</span>
          Meus Acompanhamentos
        </h2>
        <p class="text-sm text-grey-text mt-1">
          Clique em uma solicitação para visualizar todas as etapas do processo até o momento.
        </p>
      </div>

      <div
        role="group"
        aria-label="Filtrar acompanhamentos por tipo"
        class="flex flex-wrap gap-2"
      >
        <button
          v-for="filtro in filtros"
          :key="filtro.key"
          type="button"
          :aria-pressed="filtroAtivo === filtro.key"
          :class="[
            'rounded-btn border-2 px-3.5 py-1.5 text-xs font-bold transition-colors inline-flex items-center gap-1.5 cursor-pointer focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-amma-green',
            filtroAtivo === filtro.key
              ? 'bg-amma-green text-white border-amma-green shadow-2xs'
              : 'bg-white text-grey-text border-amma-green-light hover:border-amma-green hover:text-amma-green-dark',
          ]"
          @click="filtroAtivo = filtro.key"
        >
          <span>{{ filtro.label }}</span>
          <span
            :class="[
              'px-1.5 py-0.5 rounded-full text-[10px]',
              filtroAtivo === filtro.key ? 'bg-white/20 text-white' : 'bg-amma-green-lighter text-amma-green-dark',
            ]"
          >
            {{ filtro.count }}
          </span>
        </button>
      </div>
    </header>

    <!-- Skeleton -->
    <div v-if="isLoading" role="status" aria-live="polite" class="flex-1 min-h-0 overflow-y-auto space-y-3.5 pr-1">
      <p class="sr-only">Carregando seus acompanhamentos...</p>
      <div
        v-for="n in 3"
        :key="n"
        class="shrink-0 rounded-btn border border-amma-green-light bg-amma-green-lighter/30 p-5 animate-pulse flex flex-col gap-3"
      >
        <div class="flex items-center justify-between">
          <div class="h-5 w-36 bg-amma-green-light rounded-full"></div>
          <div class="h-4 w-24 bg-amma-bg rounded-full"></div>
        </div>
        <div class="h-5 w-2/3 bg-amma-green-light rounded-full"></div>
        <div class="h-4 w-1/2 bg-amma-bg rounded-full"></div>
      </div>
    </div>

    <!-- Erro isolado -->
    <div
      v-else-if="errorMessage"
      role="alert"
      class="rounded-btn border border-feedback-danger-border bg-feedback-danger-bg p-6 flex flex-col sm:flex-row items-center justify-between gap-4"
    >
      <div class="flex items-center gap-3 text-center sm:text-left">
        <span class="material-symbols-outlined text-3xl text-feedback-danger shrink-0" aria-hidden="true">
          error_outline
        </span>
        <div>
          <p class="text-sm font-bold text-feedback-danger">Erro ao carregar acompanhamentos</p>
          <p class="text-xs text-grey-text mt-0.5">{{ errorMessage }}</p>
        </div>
      </div>
      <button
        type="button"
        class="rounded-btn bg-amma-green hover:bg-amma-green-dark text-white font-bold px-4 py-2 text-xs inline-flex items-center gap-1.5 cursor-pointer efeito-zoom shrink-0 focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-amma-green"
        @click="emit('retry')"
      >
        <span class="material-symbols-outlined text-[16px]" aria-hidden="true">refresh</span>
        Tentar novamente
      </button>
    </div>

    <!-- Vazio geral -->
    <div
      v-else-if="totalGeral === 0"
      class="flex-1 rounded-btn border border-amma-green-light bg-amma-green-lighter/40 p-10 text-center flex flex-col items-center justify-center gap-3"
    >
      <div class="w-14 h-14 rounded-full bg-white border border-amma-green-light flex items-center justify-center text-amma-green shadow-2xs">
        <span class="material-symbols-outlined text-3xl" aria-hidden="true">assignment</span>
      </div>
      <h3 class="text-lg font-bold text-amma-green-dark">Você ainda não possui solicitações</h3>
      <p class="text-sm text-grey-text max-w-md leading-relaxed">
        Quando você registrar uma solicitação de mudas ou de substituição botânica pelo menu Arborização, o histórico e o status aparecerão automaticamente aqui.
      </p>
    </div>

    <!-- Vazio por filtro -->
    <div
      v-else-if="itensFiltrados.length === 0"
      class="flex-1 rounded-btn border border-amma-green-light bg-amma-green-lighter/30 p-8 text-center flex flex-col items-center justify-center gap-2"
    >
      <span class="material-symbols-outlined text-4xl text-grey-text/40" aria-hidden="true">filter_list_off</span>
      <p class="text-sm font-bold text-amma-green-dark">Nenhum registro nesta categoria</p>
      <button
        type="button"
        class="mt-2 rounded-btn bg-white border border-amma-green-light hover:border-amma-green hover:bg-amma-green-lighter text-amma-green-dark font-bold px-4 py-2 text-xs transition-colors cursor-pointer"
        @click="filtroAtivo = 'TODOS'"
      >
        Ver todos os acompanhamentos
      </button>
    </div>

    <!-- Lista de acompanhamentos expansíveis (rolagem interna mantendo o cabeçalho e o container fixos) -->
    <div v-else class="flex-1 min-h-0 overflow-y-auto pr-1.5 space-y-3.5 pb-1">
      <article
        v-for="item in itensFiltrados"
        :key="item.uid"
        :class="[
          'shrink-0 rounded-btn border transition-all overflow-hidden',
          cardExpandidoUid === item.uid
            ? 'border-amma-green bg-white shadow-sm'
            : 'border-amma-green-light bg-amma-green-lighter/25 hover:bg-white hover:border-amma-green/70',
        ]"
      >
        <!-- Visão simplificada: identificação + etapa atual -->
        <button
          type="button"
          :aria-expanded="cardExpandidoUid === item.uid"
          :aria-controls="`processo-${item.uid}`"
          class="w-full text-left p-4 sm:p-5 flex flex-col gap-2.5 cursor-pointer focus-visible:outline focus-visible:outline-2 focus-visible:outline-inset focus-visible:outline-amma-green"
          @click="toggleExpandir(item.uid)"
        >
          <div class="flex flex-wrap items-center justify-between gap-2">
            <div class="flex flex-wrap items-center gap-2">
              <span
                class="inline-flex items-center gap-1 rounded-full px-2.5 py-0.5 text-[11px] font-bold uppercase tracking-wide bg-white text-amma-green-dark border border-amma-green-light"
              >
                <span class="material-symbols-outlined text-[14px] text-amma-green" aria-hidden="true">
                  {{ item.tipo === 'MUDAS' ? 'psychiatry' : 'swap_horiz' }}
                </span>
                {{ item.tipo === 'MUDAS' ? 'Mudas' : 'Substituição de Nim' }}
              </span>

              <span class="text-xs font-bold tracking-wider text-amma-green">
                {{ item.protocolo }}
              </span>
            </div>

            <div class="flex items-center gap-2">
              <span class="text-xs font-semibold text-grey-text flex items-center gap-1">
                <span class="material-symbols-outlined text-[14px]" aria-hidden="true">calendar_today</span>
                {{ formatarData(item.criadaEm) }}
              </span>
              <span
                class="material-symbols-outlined text-[20px] text-amma-green transition-transform duration-200"
                :class="{ 'rotate-180': cardExpandidoUid === item.uid }"
                aria-hidden="true"
              >
                expand_more
              </span>
            </div>
          </div>

          <h3 class="text-base font-bold text-amma-green-dark leading-snug">
            {{ item.titulo }}
          </h3>

          <div class="flex flex-wrap items-center gap-2 pt-0.5">
            <span class="text-xs font-semibold text-grey-text">Etapa atual:</span>
            <span
              :class="[
                'inline-flex items-center gap-1 rounded-full px-2.5 py-0.5 text-[11px] font-bold',
                obterStatusVisual(item.status).badgeClass,
              ]"
            >
              <span class="material-symbols-outlined text-[14px]" aria-hidden="true">
                {{ obterStatusVisual(item.status).icon }}
              </span>
              {{ obterStatusVisual(item.status).label }}
            </span>
          </div>
        </button>

        <!-- Visão expandida: todo o processo etapa por etapa (concluídas, atual e bloqueadas) -->
        <div
          v-if="cardExpandidoUid === item.uid"
          :id="`processo-${item.uid}`"
          class="border-t border-amma-green-light bg-amma-green-lighter/25 px-5 py-4"
        >
          <h4 class="text-xs font-bold uppercase tracking-wider text-amma-green-dark mb-3.5 flex items-center gap-1.5">
            <span class="material-symbols-outlined text-[16px] text-amma-green" aria-hidden="true">timeline</span>
            Etapas do processo
          </h4>

          <ol class="relative flex flex-col gap-3.5 pl-1">
            <li
              v-for="(etapa, idx) in item.timeline"
              :key="`${item.uid}-etapa-${idx}`"
              class="relative flex items-start gap-3"
              :class="{ 'opacity-70': etapa.estado === 'futura' }"
            >
              <span
                v-if="idx < item.timeline.length - 1"
                aria-hidden="true"
                :class="[
                  'absolute left-[11px] top-6 bottom-[-18px] w-0.5',
                  etapa.estado === 'concluida' ? 'bg-amma-green/50' : 'bg-amma-green-light',
                ]"
              ></span>

              <span
                :class="[
                  'relative z-10 w-6 h-6 rounded-full border-2 flex items-center justify-center shrink-0 mt-0.5',
                  estadoEtapaMeta[etapa.estado].markerClass,
                ]"
                aria-hidden="true"
              >
                <span class="material-symbols-outlined text-[13px]">
                  {{ estadoEtapaMeta[etapa.estado].icon }}
                </span>
              </span>

              <div class="min-w-0 flex-1 pb-0.5">
                <div class="flex flex-wrap items-center justify-between gap-2">
                  <p class="text-xs font-bold" :class="estadoEtapaMeta[etapa.estado].titleClass">
                    {{ etapa.titulo }}
                    <span class="font-semibold text-grey-text">
                      — {{ estadoEtapaMeta[etapa.estado].label }}
                    </span>
                  </p>
                  <span v-if="etapa.data" class="text-[11px] font-medium text-grey-text/80">
                    {{ etapa.data }}
                  </span>
                </div>
                <p class="text-xs text-grey-text mt-0.5 leading-relaxed">
                  {{ etapa.descricao }}
                </p>
              </div>
            </li>
          </ol>
        </div>
      </article>
    </div>
  </section>
</template>
