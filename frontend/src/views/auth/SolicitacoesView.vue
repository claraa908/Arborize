<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AppBreadcrumb, { type BreadcrumbItem } from '@/components/ui/AppBreadcrumb.vue'
import AppDataTable, { type TableColumn, type TablePagination } from '@/components/ui/AppDataTable.vue'
import { useAuthStore } from '@/stores/authStore'
import { useFeedbackStore } from '@/stores/feedbackStore'
import { solicitacaoMudaService } from '@/services/solicitacaoMudaService'
import { SolicitacaoMudaError, solicitacaoStatus, type SolicitacaoMudaDetalhe, type SolicitacaoMudaPagina } from '@/types/solicitacaoMuda'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const feedback = useFeedbackStore()
const busca = ref('')
const filtro = ref('')
const pagina = ref(0)
const resultado = ref<SolicitacaoMudaPagina | null>(null)
const pedido = ref<SolicitacaoMudaDetalhe | null>(null)
const carregando = ref(false)
const enviando = ref(false)
const erro = ref('')
const erroCampo = ref('')
const quantidade = ref<number | ''>('')
const motivo = ref('')
let consultaAtual = 0

const lista = computed(() => route.name === 'solicitacoes')
const recebimento = computed(() => route.name === 'solicitacoes-recebimento')
const protocolo = computed(() => String(route.params.protocolo ?? ''))
const titulo = computed(() => lista.value ? 'Solicitações de mudas' : recebimento.value ? 'Registrar recebimento' : 'Detalhes da solicitação')
const podeRejeitar = computed(() => auth.role === 'ROLE_ADMINISTRADOR_AMMA' || auth.role === 'ROLE_FUNCIONARIO_AMMA')
const baixandoDocumento = ref(false)
const pendente = computed(() => pedido.value?.status === 'PENDENTE_RECICLAVEIS')
const detalhes = computed(() => ({ name: 'solicitacoes-detalhes', params: { protocolo: protocolo.value } }))
const breadcrumb = computed<BreadcrumbItem[]>(() => {
  const inicio: BreadcrumbItem = { label: 'Solicitações de mudas', icon: 'assignment', routeName: lista.value ? undefined : 'solicitacoes' }
  if (lista.value) return [inicio]
  const detalhe: BreadcrumbItem = { label: 'Detalhes da solicitação', routeName: recebimento.value ? 'solicitacoes-detalhes' : undefined, params: { protocolo: protocolo.value } }
  return recebimento.value ? [inicio, detalhe, { label: 'Registrar recebimento' }] : [inicio, detalhe]
})
const colunasSolicitacoes: TableColumn[] = [
  { key: 'protocolo', label: 'Protocolo / Solicitante' },
  { key: 'especie', label: 'Espécie / Mudas' },
  { key: 'contrapartida', label: 'Contrapartida' },
  { key: 'status', label: 'Status', align: 'center' },
  { key: 'acoes', label: 'Ações', align: 'right' },
]
const paginacaoTabela = computed<TablePagination | undefined>(() => {
  if (!resultado.value) return undefined
  return {
    page: resultado.value.page,
    size: 10,
    totalElements: resultado.value.totalPages <= 1 ? resultado.value.content.length : resultado.value.totalPages * 10,
    totalPages: resultado.value.totalPages,
  }
})
const campos = computed(() => pedido.value ? [
  ['Cidadão', pedido.value.nomeSolicitante],
  ['Solicitação', pedido.value.protocolo],
  ['Espécie', pedido.value.especie],
  ['Quantidade', pedido.value.quantidadeMudas + ' mudas'],
  ['Destino', pedido.value.destino],
  ['Local', pedido.value.localPlantio === 'CALCADA' ? 'Calçada' : pedido.value.localPlantio === 'QUINTAL' ? 'Quintal' : 'Não informado']
] : [])

const painel = 'min-w-0 rounded-box border border-amma-green-light bg-white p-5 md:p-6'
const campo = 'mt-1 w-full rounded-btn border-2 border-amma-green-light bg-white px-4 py-2.5 text-grey-text focus-visible:outline-2 focus-visible:outline-amma-green'
const primario = 'rounded-btn bg-amma-green-dark px-5 py-2.5 text-center font-bold text-white hover:bg-amma-green focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-amma-green-dark disabled:opacity-60'
const secundario = 'rounded-btn border-2 border-amma-green px-5 py-2.5 text-center font-bold text-amma-green-dark hover:bg-amma-green-lighter focus-visible:outline-2 focus-visible:outline-amma-green-dark disabled:opacity-60'
const selo = 'w-fit rounded-full border px-3 py-1 text-sm font-semibold'
const mensagensConsulta: Partial<Record<number, string>> = {
  401: 'Entre novamente para acessar as solicitações.',
  403: 'Você não tem permissão para acessar as solicitações.',
  404: 'Solicitação não encontrada.'
}

async function carregar() {
  const consulta = ++consultaAtual
  carregando.value = true
  erro.value = ''
  resultado.value = null
  pedido.value = null
  try {
    if (lista.value) {
      const dados = await solicitacaoMudaService.listar(filtro.value, pagina.value)
      if (consulta === consultaAtual) resultado.value = dados
    } else {
      const dados = await solicitacaoMudaService.detalhar(protocolo.value)
      if (consulta === consultaAtual) pedido.value = dados
    }
  } catch (falha) {
    if (consulta !== consultaAtual) return
    erro.value = falha instanceof SolicitacaoMudaError
      ? mensagensConsulta[falha.status ?? 0] ?? falha.message
      : 'Não foi possível carregar a solicitação.'
  } finally {
    if (consulta === consultaAtual) carregando.value = false
  }
}

function pesquisar() {
  filtro.value = busca.value.trim().toUpperCase()
  pagina.value = 0
  void carregar()
}

function mudarPagina(nova: number) {
  pagina.value = nova
  void carregar()
}

async function decidir(rejeitar: boolean) {
  const atual = pedido.value
  if (!atual || !pendente.value || enviando.value || (rejeitar && !podeRejeitar.value)) return
  erroCampo.value = ''
  const recebido = Number(quantidade.value)
  const justificativa = motivo.value.trim()
  if (rejeitar && (!justificativa || justificativa.length > 255)) {
    erroCampo.value = 'Informe um motivo de até 255 caracteres.'
    return
  }
  if (!rejeitar && (quantidade.value === '' || !Number.isInteger(recebido) || recebido < atual.totalRecicaveis || recebido > 2147483647)) {
    erroCampo.value = 'Informe uma quantidade inteira igual ou superior à esperada.'
    return
  }
  const origem = route.fullPath
  enviando.value = true
  try {
    const confirmou = await feedback.showConfirm({
      title: rejeitar ? 'Rejeitar solicitação' : 'Confirmar recebimento e aprovar',
      message: rejeitar ? 'A solicitação será rejeitada com o motivo informado.' : 'A aprovação libera o documento que a AMMA deve entregar ao cidadão.',
      confirmText: rejeitar ? 'Rejeitar' : 'Confirmar',
      cancelText: 'Voltar',
      variant: rejeitar ? 'danger' : 'primary'
    })
    if (!confirmou || route.fullPath !== origem) return
    const atualizado = rejeitar
      ? await solicitacaoMudaService.rejeitar(atual.protocolo, justificativa)
      : await solicitacaoMudaService.receber(atual.protocolo, recebido)
    if (route.fullPath !== origem) return
    pedido.value = atualizado
    feedback.showToast(rejeitar ? 'Solicitação rejeitada.' : 'Solicitação aprovada. Baixe o documento de retirada para entregar ao cidadão.', 'success')
    if (!rejeitar) await router.push(detalhes.value)
  } catch (falha) {
    if (route.fullPath !== origem) return
    if (falha instanceof SolicitacaoMudaError && falha.status === 409) {
      feedback.showToast('Esta solicitação já foi processada. Os dados foram atualizados.', 'warning')
      await carregar()
    } else if (falha instanceof SolicitacaoMudaError) {
      erroCampo.value = falha.problem?.errors?.[rejeitar ? 'motivo' : 'quantidadeRecebida'] ?? falha.problem?.detail ?? falha.message
    } else {
      erroCampo.value = 'Não foi possível concluir a operação. Tente novamente.'
    }
  } finally {
    enviando.value = false
  }
}

async function baixarDocumento() {
  const atual = pedido.value
  if (!atual || atual.status !== 'APROVADA' || baixandoDocumento.value) return
  baixandoDocumento.value = true
  try {
    const blob = await solicitacaoMudaService.documento(atual.protocolo)
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = atual.protocolo + '.pdf'
    link.click()
    setTimeout(() => URL.revokeObjectURL(url), 1000)
  } catch {
    feedback.showToast('Não foi possível baixar o documento de retirada. Tente novamente.', 'error')
  } finally {
    baixandoDocumento.value = false
  }
}

watch(() => route.fullPath, () => {
  erroCampo.value = ''
  quantidade.value = ''
  motivo.value = ''
  void carregar()
}, { immediate: true })
</script>

<template>
  <div class="flex w-full flex-col">
    <AppBreadcrumb v-if="!lista" :items="breadcrumb" />
    <div class="flex min-w-0 flex-col gap-5 px-6 py-8 md:px-10">
      <header v-if="lista">
        <h1 class="text-2xl font-bold text-amma-green-dark md:sr-only">{{ titulo }}</h1>
        <p class="text-grey-text">Acompanhe os pedidos de mudas e a entrega de recicláveis.</p>
      </header>
      <header v-else class="flex items-center gap-4">
        <div
          aria-hidden="true"
          class="flex h-12 w-12 shrink-0 items-center justify-center rounded-full bg-amma-green-lighter/50 text-amma-green shadow-sm"
        >
          <span class="material-symbols-outlined text-[24px]">{{ recebimento ? 'recycling' : 'assignment' }}</span>
        </div>
        <div>
          <h1 class="text-2xl font-bold leading-none text-amma-green-dark">{{ titulo }}</h1>
          <p class="mt-1 text-sm text-grey-text">{{ recebimento ? 'Confirme os recicláveis entregues e aprove a solicitação.' : 'Confira os dados do cidadão, da muda e da contrapartida.' }}</p>
        </div>
      </header>

      <form v-if="lista" class="flex flex-wrap items-end gap-3" @submit.prevent="pesquisar">
        <label class="min-w-0 flex-1 font-semibold text-grey-text">
          Buscar por protocolo
          <input v-model="busca" :class="campo" type="search" placeholder="SOL-2026-0011" />
        </label>
        <button :class="secundario" type="submit">Buscar</button>
        <p class="rounded-btn border border-amma-green-light px-4 py-2.5 text-sm text-grey-text">Mais recentes</p>
      </form>

      <p v-if="carregando" role="status" :class="painel">
        <span aria-hidden="true" class="mr-2 inline-block size-4 animate-spin rounded-full border-2 border-amma-green border-r-transparent"></span>
        Carregando solicitações…
      </p>
      <div v-else-if="erro" :class="painel">
        <p class="mb-3 text-feedback-danger" role="alert">{{ erro }}</p>
        <button :class="secundario" type="button" @click="carregar">Tentar novamente</button>
      </div>
      <template v-else-if="lista && resultado">
        <p v-if="!resultado.content.length" :class="painel">{{ filtro ? 'Nenhum pedido corresponde a esse protocolo.' : 'Ainda não há solicitações.' }}</p>
        <AppDataTable
          v-else
          :columns="colunasSolicitacoes"
          :items="resultado.content"
          :pagination="paginacaoTabela"
          item-key="protocolo"
          caption="Solicitações de mudas"
          :show-search="false"
          :show-summary="true"
          :allow-view-toggle="false"
          default-view="table"
          @page-change="mudarPagina"
        >
          <template #protocolo="{ item }">
            <p class="font-bold text-amma-green-dark">{{ item.protocolo }}</p>
            <p class="text-xs text-grey-text">{{ item.nomeSolicitante }}</p>
          </template>
          <template #especie="{ item }">
            <p class="text-sm font-medium text-grey-text">{{ item.especie }}</p>
            <p class="text-xs text-grey-text">{{ item.quantidadeMudas }} muda(s)</p>
          </template>
          <template #contrapartida="{ item }">
            <span class="whitespace-nowrap text-sm font-semibold text-amma-green-dark">{{ item.totalRecicaveis }} garrafas PET</span>
          </template>
          <template #status="{ item }">
            <span :class="[selo, solicitacaoStatus[item.status].badge]">{{ solicitacaoStatus[item.status].label }}</span>
          </template>
          <template #acoes="{ item }">
            <RouterLink :to="{ name: 'solicitacoes-detalhes', params: { protocolo: item.protocolo } }" :class="['inline-block text-sm', primario]" :aria-label="'Abrir solicitação ' + item.protocolo">Abrir</RouterLink>
          </template>
        </AppDataTable>
      </template>

    <div v-else-if="pedido" class="grid min-w-0 gap-5 lg:grid-cols-[minmax(0,3fr)_minmax(0,2fr)]">
      <template v-if="recebimento">
        <section :class="painel">
          <h2 class="mb-3 text-lg font-bold text-amma-green-dark">Conferência da entrega</h2>
          <form v-if="pendente" class="grid gap-4 sm:grid-cols-2" novalidate @submit.prevent="decidir(false)">
            <label>Quantidade esperada<input :value="pedido.totalRecicaveis" :class="campo" readonly /></label>
            <label>Quantidade recebida
              <input v-model="quantidade" :class="campo" inputmode="numeric" type="number" :min="pedido.totalRecicaveis" step="1" required :disabled="enviando" :aria-invalid="!!erroCampo" :aria-describedby="erroCampo ? 'erro-recebimento' : undefined" />
            </label>
            <p v-if="erroCampo" id="erro-recebimento" role="alert" class="text-feedback-danger sm:col-span-2">{{ erroCampo }}</p>
            <label class="sm:col-span-2">Material<input value="Garrafas PET" :class="campo" readonly /></label>
            <div class="mt-2 flex flex-wrap justify-between gap-3 sm:col-span-2">
              <RouterLink :to="detalhes" :class="secundario">Voltar</RouterLink>
              <button :class="primario" type="submit" :disabled="enviando">{{ enviando ? 'Aguarde…' : 'Confirmar recebimento e aprovar' }}</button>
            </div>
          </form>
          <div v-else>
            <p role="status" class="mb-4 text-grey-text">Somente solicitações pendentes podem receber recicláveis.</p>
            <RouterLink :to="detalhes" :class="secundario">Voltar</RouterLink>
          </div>
        </section>
        <section :class="painel">
          <h2 class="mb-3 break-words text-lg font-bold text-amma-green-dark">Solicitação {{ pedido.protocolo }}</h2>
          <dl class="space-y-3">
            <div v-for="([rotulo, valor]) in campos.filter(([nome]) => nome !== 'Solicitação' && nome !== 'Destino' && nome !== 'Local')" :key="rotulo" class="flex justify-between gap-3 border-b border-amma-green-light pb-2">
              <dt>{{ rotulo }}</dt><dd class="break-words text-right font-bold">{{ valor }}</dd>
            </div>
          </dl>
        </section>
      </template>
      <template v-else>
        <section :class="painel">
          <div class="mb-3 flex flex-wrap items-center justify-between gap-2">
            <h2 class="text-lg font-bold text-amma-green-dark">Dados do pedido</h2>
            <span :class="[selo, solicitacaoStatus[pedido.status].badge]">{{ solicitacaoStatus[pedido.status].label }}</span>
          </div>
          <dl class="grid gap-3 sm:grid-cols-2">
            <div v-for="([rotulo, valor]) in campos" :key="rotulo" class="min-w-0 rounded-btn border border-amma-green-light px-3 py-2">
              <dt class="text-sm text-grey-text">{{ rotulo }}</dt><dd class="break-words font-bold text-amma-green-dark">{{ valor }}</dd>
            </div>
          </dl>
          <p v-if="pedido.motivoRejeicao" class="mt-4 break-words text-grey-text"><strong>Motivo da rejeição</strong><br />{{ pedido.motivoRejeicao }}</p>
          <form v-if="pendente && podeRejeitar" class="mt-5 border-t border-amma-green-light pt-4" novalidate @submit.prevent="decidir(true)">
            <label class="font-semibold text-grey-text">Motivo da rejeição
              <textarea v-model="motivo" :class="campo" rows="2" maxlength="255" required :disabled="enviando" :aria-invalid="!!erroCampo" :aria-describedby="erroCampo ? 'erro-rejeicao' : undefined"></textarea>
            </label>
            <p v-if="erroCampo" id="erro-rejeicao" role="alert" class="mb-2 text-feedback-danger">{{ erroCampo }}</p>
            <button type="submit" :disabled="enviando" class="mt-2 rounded-btn border border-feedback-danger-border bg-feedback-danger-bg px-4 py-2 font-bold text-feedback-danger focus-visible:outline-2">{{ enviando ? 'Aguarde…' : 'Rejeitar solicitação' }}</button>
          </form>
        </section>
        <section :class="painel">
          <h2 class="mb-3 text-lg font-bold text-amma-green-dark">Contrapartida</h2>
          <div class="rounded-btn bg-amma-bg p-5">
            <p class="text-sm text-grey-text">Recicláveis esperados</p>
            <p class="text-3xl font-bold text-amma-green-dark">{{ pedido.totalRecicaveis }} PET</p>
            <p v-if="pedido.quantidadeRecebida !== null" class="mt-2 text-grey-text">Quantidade recebida: {{ pedido.quantidadeRecebida }} garrafas PET</p>
          </div>
          <RouterLink v-if="pendente" :to="{ name: 'solicitacoes-recebimento', params: { protocolo: pedido.protocolo } }" :class="['mt-5 inline-block', primario]">Registrar Recebimento</RouterLink>
          <button v-if="pedido.status === 'APROVADA'" type="button" :class="['mt-5', primario]" :disabled="baixandoDocumento" @click="baixarDocumento">
            {{ baixandoDocumento ? 'Baixando…' : 'Baixar documento de retirada' }}
          </button>
        </section>
      </template>
    </div>
    </div>
  </div>
</template>
