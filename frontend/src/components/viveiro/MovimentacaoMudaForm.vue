<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import type { EspecieDTO } from '@/types/viveiro'
import type { MovimentacaoMudaDTO, MovimentacaoMudaPayload, TipoMovimentacao } from '@/types/movimentacaoMuda'

const props = defineProps<{
  especies: EspecieDTO[]
  initialMovement?: MovimentacaoMudaDTO | null
  loading?: boolean
  saving?: boolean
  error?: string
}>()

const emit = defineEmits<{
  save: [payload: MovimentacaoMudaPayload]
  cancel: []
  proof: [id: number]
}>()

const tipos: { id: TipoMovimentacao; titulo: string; descricao: string; icone: string }[] = [
  { id: 'ENTRADA_GERMINACAO', titulo: 'Entrada / Germinação', descricao: 'Novas mudas incorporadas ao estoque', icone: 'south_west' },
  { id: 'SAIDA_PLANTIO', titulo: 'Saída para plantio', descricao: 'Mudas destinadas a uma ação de plantio', icone: 'park' },
  { id: 'BAIXA_PERDA', titulo: 'Baixa por perda', descricao: 'Mudas perdidas ou sem condição de uso', icone: 'remove_circle' },
]

function dataHoje() {
  const agora = new Date()
  const local = new Date(agora.getTime() - agora.getTimezoneOffset() * 60_000)
  return local.toISOString().slice(0, 10)
}

const arquivoInput = ref<HTMLInputElement | null>(null)
const arquivoError = ref('')
const errors = reactive<Record<string, string>>({})
const form = reactive<MovimentacaoMudaPayload>({
  tipo: 'ENTRADA_GERMINACAO',
  especieId: 0,
  quantidade: 1,
  data: dataHoje(),
  destino: '',
  responsavel: '',
  observacoes: '',
  comprovante: null,
})

watch(
  () => props.initialMovement,
  (movimento) => {
    if (!movimento) {
      Object.assign(form, { tipo: 'ENTRADA_GERMINACAO', especieId: 0, quantidade: 1, data: dataHoje(), destino: '', responsavel: '', observacoes: '', comprovante: null })
      return
    }
    Object.assign(form, {
      tipo: movimento.tipo,
      especieId: movimento.especieId,
      quantidade: movimento.quantidade,
      data: movimento.data,
      destino: movimento.destino ?? '',
      responsavel: movimento.responsavel ?? '',
      observacoes: movimento.observacoes ?? '',
      comprovante: null,
    })
  },
  { immediate: true },
)

const especieSelecionada = computed(() => props.especies.find((item) => Number(item.id) === Number(form.especieId)))
const exigeDestino = computed(() => form.tipo === 'SAIDA_PLANTIO')

function validar(): boolean {
  Object.keys(errors).forEach((key) => delete errors[key])
  if (!form.tipo) errors.tipo = 'Selecione o tipo de movimentação.'
  if (!form.especieId) errors.especieId = 'Selecione uma espécie.'
  if (!Number.isInteger(Number(form.quantidade)) || Number(form.quantidade) <= 0) errors.quantidade = 'Informe uma quantidade inteira maior que zero.'
  if (!form.data || Number.isNaN(Date.parse(`${form.data}T00:00:00`))) errors.data = 'Informe uma data válida.'
  if (exigeDestino.value && !form.destino.trim()) errors.destino = 'Informe a finalidade ou o destino do plantio.'
  return Object.keys(errors).length === 0 && !arquivoError.value
}

function selecionarArquivo(event: Event) {
  arquivoError.value = ''
  const arquivo = (event.target as HTMLInputElement).files?.[0] ?? null
  if (!arquivo) {
    form.comprovante = null
    return
  }
  const extensao = arquivo.name.split('.').pop()?.toLowerCase()
  const mimePermitidos = ['application/pdf', 'image/png', 'image/jpeg']
  if (arquivo.size > 10 * 1024 * 1024) {
    arquivoError.value = 'O arquivo deve ter no máximo 10 MB.'
    form.comprovante = null
  } else if (!['pdf', 'png', 'jpg', 'jpeg'].includes(extensao ?? '') || !mimePermitidos.includes(arquivo.type)) {
    arquivoError.value = 'Envie um arquivo PDF, PNG ou JPG válido.'
    form.comprovante = null
  } else {
    form.comprovante = arquivo
  }
}

function enviar() {
  if (validar()) emit('save', { ...form, quantidade: Number(form.quantidade) })
}
</script>

<template>
  <form class="mx-auto flex w-full max-w-5xl flex-col gap-6" @submit.prevent="enviar" novalidate>
    <section class="rounded-box border border-amma-green-light bg-white p-5 shadow-sm sm:p-7" aria-labelledby="tipo-movimentacao-titulo">
      <div class="mb-5">
        <h2 id="tipo-movimentacao-titulo" class="text-lg font-bold text-amma-green-dark">Tipo de movimentação <span class="text-feedback-danger" aria-hidden="true">*</span></h2>
        <p class="mt-1 text-sm text-grey-text">Escolha a operação que será aplicada ao estoque.</p>
      </div>
      <div class="grid grid-cols-1 gap-3 md:grid-cols-3">
        <button
          v-for="tipo in tipos"
          :key="tipo.id"
          type="button"
          :aria-pressed="form.tipo === tipo.id"
          @click="form.tipo = tipo.id"
          :class="[
            'flex min-h-28 items-start gap-3 rounded-btn border-2 p-4 text-left transition-colors focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-amma-green',
            form.tipo === tipo.id ? 'border-amma-green bg-amma-green-lighter' : 'border-amma-green-light bg-white hover:bg-amma-bg',
          ]"
        >
          <span class="material-symbols-outlined mt-0.5 text-amma-green-dark" aria-hidden="true">{{ tipo.icone }}</span>
          <span>
            <span class="block font-bold text-grey-text">{{ tipo.titulo }}</span>
            <span class="mt-1 block text-xs leading-5 text-grey-text/70">{{ tipo.descricao }}</span>
          </span>
        </button>
      </div>
      <p v-if="errors.tipo" class="mt-2 text-sm font-medium text-feedback-danger" role="alert">{{ errors.tipo }}</p>
    </section>

    <section class="rounded-box border border-amma-green-light bg-white p-5 shadow-sm sm:p-7" aria-labelledby="dados-movimentacao-titulo">
      <div class="mb-5">
        <h2 id="dados-movimentacao-titulo" class="text-lg font-bold text-amma-green-dark">Dados da movimentação</h2>
        <p class="mt-1 text-sm text-grey-text">Informe a espécie, a quantidade e a data do registro.</p>
      </div>

      <div class="grid grid-cols-1 gap-5 md:grid-cols-2">
        <div class="md:col-span-2">
          <label for="movimentacao-especie" class="mb-1.5 block text-sm font-semibold text-grey-text">Espécie <span class="text-feedback-danger" aria-hidden="true">*</span></label>
          <select id="movimentacao-especie" v-model.number="form.especieId" :disabled="loading || saving" :aria-invalid="Boolean(errors.especieId)" aria-describedby="especie-erro" class="w-full rounded-btn border border-amma-green-light bg-white px-3 py-3 text-sm text-grey-text outline-none focus:border-amma-green focus:ring-2 focus:ring-amma-green/20 disabled:opacity-60">
            <option :value="0" disabled>Selecione uma espécie</option>
            <option v-for="especie in especies" :key="especie.id" :value="Number(especie.id)">{{ especie.nomePopular }} — {{ especie.nomeCientifico }}</option>
          </select>
          <p v-if="errors.especieId" id="especie-erro" class="mt-1 text-sm text-feedback-danger" role="alert">{{ errors.especieId }}</p>
          <div v-if="especieSelecionada" class="mt-3 flex flex-wrap items-center justify-between gap-2 rounded-btn bg-amma-bg px-4 py-3 text-sm">
            <span class="font-semibold text-grey-text">{{ especieSelecionada.nomePopular }} <span class="font-normal italic text-grey-text/70">{{ especieSelecionada.nomeCientifico }}</span></span>
            <span class="font-bold text-amma-green-dark">Estoque atual: {{ especieSelecionada.mudasDisponiveis ?? 0 }} mudas</span>
          </div>
        </div>

        <div>
          <label for="movimentacao-quantidade" class="mb-1.5 block text-sm font-semibold text-grey-text">Quantidade <span class="text-feedback-danger" aria-hidden="true">*</span></label>
          <input id="movimentacao-quantidade" v-model.number="form.quantidade" type="number" min="1" step="1" inputmode="numeric" :disabled="saving" :aria-invalid="Boolean(errors.quantidade)" aria-describedby="quantidade-erro" class="w-full rounded-btn border border-amma-green-light px-3 py-3 text-sm outline-none focus:border-amma-green focus:ring-2 focus:ring-amma-green/20 disabled:opacity-60" />
          <p v-if="errors.quantidade" id="quantidade-erro" class="mt-1 text-sm text-feedback-danger" role="alert">{{ errors.quantidade }}</p>
        </div>
        <div>
          <label for="movimentacao-data" class="mb-1.5 block text-sm font-semibold text-grey-text">Data <span class="text-feedback-danger" aria-hidden="true">*</span></label>
          <input id="movimentacao-data" v-model="form.data" type="date" :disabled="saving" :aria-invalid="Boolean(errors.data)" aria-describedby="data-erro" class="w-full rounded-btn border border-amma-green-light px-3 py-3 text-sm outline-none focus:border-amma-green focus:ring-2 focus:ring-amma-green/20 disabled:opacity-60" />
          <p v-if="errors.data" id="data-erro" class="mt-1 text-sm text-feedback-danger" role="alert">{{ errors.data }}</p>
        </div>

        <div v-if="exigeDestino" class="md:col-span-2">
          <label for="movimentacao-destino" class="mb-1.5 block text-sm font-semibold text-grey-text">Finalidade / destino <span class="text-feedback-danger" aria-hidden="true">*</span></label>
          <input id="movimentacao-destino" v-model="form.destino" type="text" maxlength="180" placeholder="Ex.: Praça José de Barros, mutirão de plantio..." :disabled="saving" :aria-invalid="Boolean(errors.destino)" aria-describedby="destino-erro" class="w-full rounded-btn border border-amma-green-light px-3 py-3 text-sm outline-none focus:border-amma-green focus:ring-2 focus:ring-amma-green/20 disabled:opacity-60" />
          <p v-if="errors.destino" id="destino-erro" class="mt-1 text-sm text-feedback-danger" role="alert">{{ errors.destino }}</p>
        </div>
        <div class="md:col-span-2">
          <label for="movimentacao-responsavel" class="mb-1.5 block text-sm font-semibold text-grey-text">Responsável / equipe <span class="font-normal text-grey-text/60">(opcional)</span></label>
          <input id="movimentacao-responsavel" v-model="form.responsavel" type="text" maxlength="160" placeholder="Nome do responsável ou equipe" :disabled="saving" class="w-full rounded-btn border border-amma-green-light px-3 py-3 text-sm outline-none focus:border-amma-green focus:ring-2 focus:ring-amma-green/20 disabled:opacity-60" />
        </div>
        <div class="md:col-span-2">
          <label for="movimentacao-observacoes" class="mb-1.5 block text-sm font-semibold text-grey-text">Observações <span class="font-normal text-grey-text/60">(opcional)</span></label>
          <textarea id="movimentacao-observacoes" v-model="form.observacoes" rows="4" maxlength="1000" placeholder="Adicione informações relevantes sobre esta movimentação" :disabled="saving" class="w-full resize-y rounded-btn border border-amma-green-light px-3 py-3 text-sm outline-none focus:border-amma-green focus:ring-2 focus:ring-amma-green/20 disabled:opacity-60"></textarea>
        </div>
      </div>
    </section>

    <section class="rounded-box border border-amma-green-light bg-white p-5 shadow-sm sm:p-7" aria-labelledby="comprovante-titulo">
      <h2 id="comprovante-titulo" class="text-lg font-bold text-amma-green-dark">Comprovante <span class="font-normal text-grey-text/60">(opcional)</span></h2>
      <p class="mt-1 text-sm text-grey-text">Anexe PDF, PNG ou JPG de até 10 MB.</p>
      <div v-if="initialMovement?.comprovanteNome && !form.comprovante" class="mt-4 flex flex-wrap items-center justify-between gap-3 rounded-btn bg-amma-bg px-4 py-3">
        <span class="flex items-center gap-2 text-sm font-medium text-grey-text"><span class="material-symbols-outlined text-amma-green" aria-hidden="true">attach_file</span>{{ initialMovement.comprovanteNome }}</span>
        <button type="button" class="text-sm font-bold text-amma-green-dark underline" @click="emit('proof', initialMovement.id)">Abrir comprovante</button>
      </div>
      <div class="mt-4 flex flex-col gap-3 sm:flex-row sm:items-center">
        <input ref="arquivoInput" type="file" accept=".pdf,.png,.jpg,.jpeg,application/pdf,image/png,image/jpeg" class="sr-only" aria-label="Selecionar comprovante" @change="selecionarArquivo" />
        <button type="button" :disabled="saving" class="inline-flex min-h-11 items-center justify-center gap-2 rounded-btn border border-amma-green px-4 py-2 text-sm font-bold text-amma-green-dark hover:bg-amma-green-lighter focus-visible:outline focus-visible:outline-2 focus-visible:outline-amma-green disabled:opacity-50" @click="arquivoInput?.click()">
          <span class="material-symbols-outlined text-[19px]" aria-hidden="true">upload_file</span>Selecionar arquivo
        </button>
        <span class="text-sm text-grey-text/70">{{ form.comprovante?.name ?? 'Nenhum arquivo selecionado' }}</span>
      </div>
      <p v-if="arquivoError" class="mt-2 text-sm text-feedback-danger" role="alert">{{ arquivoError }}</p>
    </section>

    <p v-if="error" class="rounded-btn border border-feedback-danger-border bg-feedback-danger-bg p-4 text-sm font-medium text-feedback-danger" role="alert">{{ error }}</p>

    <div class="flex flex-col-reverse justify-end gap-3 pb-4 sm:flex-row">
      <button type="button" :disabled="saving" class="min-h-11 rounded-btn border border-amma-green-light bg-white px-6 py-2 text-sm font-bold text-grey-text hover:bg-amma-bg disabled:opacity-50" @click="emit('cancel')">Cancelar</button>
      <button type="submit" :disabled="saving || loading" class="inline-flex min-h-11 items-center justify-center gap-2 rounded-btn bg-amma-green px-6 py-2 text-sm font-bold text-white hover:bg-amma-green-dark focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-amma-green disabled:cursor-wait disabled:opacity-60">
        <span v-if="saving" class="h-4 w-4 animate-spin rounded-full border-2 border-white border-r-transparent" aria-hidden="true"></span>
        <span v-else class="material-symbols-outlined text-[19px]" aria-hidden="true">check</span>
        {{ saving ? 'Salvando...' : initialMovement ? 'Salvar alterações' : 'Registrar movimentação' }}
      </button>
    </div>
  </form>
</template>
