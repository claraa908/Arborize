<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AppBreadcrumb from '@/components/ui/AppBreadcrumb.vue'
import { useFeedbackStore } from '@/stores/feedbackStore'
import { useCompensacaoStore } from '@/stores/compensacaoStore'
import { compensacaoService } from '@/services/compensacaoService'
import type { CompensacaoStatus } from '@/types/compensacao'

const route = useRoute()
const router = useRouter()
const feedback = useFeedbackStore()
const store = useCompensacaoStore()
const isEditing = computed(() => Boolean(route.params.id))
const original = computed(() => store.termoAtual)
const form = ref({
  razaoSocial: '', cnpj: '', numeroTermo: '', dataAssinatura: '', prazoEntrega: '',
  quantidadeAcordada: null as number | null, status: 'EM_ABERTO' as CompensacaoStatus,
})
const arquivoTermo = ref<File | null>(null)
const reciboArquivo = ref<File | null>(null)
const quantidadeParcela = ref<number | null>(null)
const errorMessages = ref<Record<string, string>>({})
const isSubmitting = computed(() => store.isSubmitting)
const operationSuccess = ref(false)
const termoInput = ref<HTMLInputElement | null>(null)
const reciboInput = ref<HTMLInputElement | null>(null)
const termoMaxBytes = 10 * 1024 * 1024
const reciboMaxBytes = 2 * 1024 * 1024

onMounted(async () => {
  if (!isEditing.value) return
  const loaded = await store.carregarTermo(Number(route.params.id))
  if (loaded && store.termoAtual) {
    Object.assign(form.value, store.termoAtual, {
      dataAssinatura: store.termoAtual.dataAssinatura ?? '',
      prazoEntrega: store.termoAtual.prazoEntrega ?? '',
    })
  }
})

const breadcrumbItems = computed(() => [
  { label: 'Compensação Ambiental', routeName: 'compensacao-lista', icon: 'balance' },
  { label: isEditing.value ? 'Consultar / editar termo' : 'Novo termo' },
])

function validarArquivo(file: File, tipo: 'termo' | 'recibo'): string | null {
  const allowed = tipo === 'termo'
    ? ['application/pdf']
    : ['application/pdf', 'image/png', 'image/jpeg']
  const maxSize = tipo === 'termo' ? termoMaxBytes : reciboMaxBytes
  const extensionValid = tipo === 'termo'
    ? file.name.toLowerCase().endsWith('.pdf')
    : /\.(pdf|png|jpe?g)$/i.test(file.name)
  if (!extensionValid || !allowed.includes(file.type)) return tipo === 'termo' ? 'Anexe um arquivo PDF.' : 'Use recibo em PDF, PNG ou JPG.'
  if (file.size > maxSize) return tipo === 'termo' ? 'O PDF do termo deve ter até 10 MB.' : 'O recibo deve ter até 2 MB.'
  return null
}

function onFileChange(event: Event, tipo: 'termo' | 'recibo') {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return
  const message = validarArquivo(file, tipo)
  if (message) {
    errorMessages.value[tipo === 'termo' ? 'arquivoTermo' : 'reciboArquivo'] = message
    input.value = ''
    if (tipo === 'termo') arquivoTermo.value = null
    else reciboArquivo.value = null
    return
  }
  delete errorMessages.value[tipo === 'termo' ? 'arquivoTermo' : 'reciboArquivo']
  if (tipo === 'termo') arquivoTermo.value = file
  else reciboArquivo.value = file
}

function validarFormulario() {
  const errors: Record<string, string> = {}
  if (!form.value.razaoSocial.trim()) errors.razaoSocial = 'Informe a razão social da empresa.'
  if (form.value.cnpj.replace(/\D/g, '').length !== 14) errors.cnpj = 'Informe um CNPJ com 14 dígitos.'
  if (!form.value.quantidadeAcordada || form.value.quantidadeAcordada < 1 || !Number.isInteger(form.value.quantidadeAcordada)) {
    errors.quantidadeAcordada = 'Informe um quantitativo inteiro maior que zero.'
  }
  if (!isEditing.value && !arquivoTermo.value) errors.arquivoTermo = 'Anexe o PDF assinado do termo.'
  if (isEditing.value && quantidadeParcela.value !== null && (!Number.isInteger(quantidadeParcela.value) || quantidadeParcela.value <= 0)) {
    errors.quantidadeParcela = 'Informe uma quantidade inteira maior que zero.'
  }
  if (isEditing.value && quantidadeParcela.value && !reciboArquivo.value) errors.reciboArquivo = 'Anexe o recibo desta parcela.'
  errorMessages.value = errors
  return Object.keys(errors).length === 0
}

async function submitForm() {
  operationSuccess.value = false
  if (!validarFormulario()) {
    feedback.showToast('Revise os campos destacados antes de continuar.', 'warning')
    return
  }
  const payload = {
    razaoSocial: form.value.razaoSocial.trim(),
    cnpj: form.value.cnpj.replace(/\D/g, ''),
    numeroTermo: form.value.numeroTermo.trim() || undefined,
    dataAssinatura: form.value.dataAssinatura || undefined,
    prazoEntrega: form.value.prazoEntrega || undefined,
    quantidadeAcordada: form.value.quantidadeAcordada as number,
  }
  let success = false
  if (!isEditing.value) {
    success = Boolean(arquivoTermo.value && await store.criar(payload, arquivoTermo.value))
  } else {
    const id = Number(route.params.id)
    success = await store.atualizar(id, payload)
    if (success && store.termoAtual && store.termoAtual.status !== form.value.status) {
      success = await store.atualizarStatus(id, form.value.status)
    }
    if (success && quantidadeParcela.value && reciboArquivo.value) {
      success = await store.registrarEntrega(id, quantidadeParcela.value, reciboArquivo.value)
    }
  }
  if (!success) {
    errorMessages.value = { ...errorMessages.value, ...store.serverErrors }
    feedback.showToast(store.error ?? 'Não foi possível concluir a operação. Seus dados foram mantidos.', 'error')
    return
  }
  if (store.termoAtual) form.value.status = store.termoAtual.status
  if (quantidadeParcela.value && reciboArquivo.value) {
    quantidadeParcela.value = null
    reciboArquivo.value = null
    if (reciboInput.value) reciboInput.value.value = ''
  }
  operationSuccess.value = true
  feedback.showToast(isEditing.value ? 'Termo atualizado com sucesso.' : 'Termo cadastrado com sucesso.', 'success')
  if (!isEditing.value) await router.push({ name: 'compensacao-lista' })
}

async function baixarDocumento(entregaId?: number) {
  try {
    const blob = await compensacaoService.baixarDocumento(Number(route.params.id), entregaId)
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = entregaId == null ? original.value?.arquivoTermo ?? 'termo.pdf' : 'recibo'
    link.click()
    URL.revokeObjectURL(url)
  } catch (error) {
    feedback.showToast(error instanceof Error ? error.message : 'Não foi possível acessar o documento.', 'error')
  }
}

onUnmounted(() => {
  if (termoInput.value) termoInput.value.value = ''
  if (reciboInput.value) reciboInput.value.value = ''
})
</script>

<template>
  <div class="px-6 md:px-10 pt-5 pb-8 flex flex-col gap-5">
    <AppBreadcrumb :items="breadcrumbItems" :show-back="false" />
    <section class="bg-white border border-amma-green-light rounded-box p-5 md:p-8">
      <header class="mb-7">
        <h1 class="text-2xl md:text-3xl font-bold text-amma-green-dark">{{ isEditing ? 'Consultar / editar termo' : 'Cadastrar termo de compensação' }}</h1>
        <p class="text-sm md:text-base text-grey-text mt-2">Registre a empresa, as mudas acordadas e anexe o Termo de Compromisso assinado.</p>
      </header>

      <p v-if="store.isLoading" class="rounded-btn border border-amma-green-light bg-amma-bg px-4 py-3 text-sm text-grey-text mb-6" role="status">
        Carregando os dados do termo…
      </p>
      <div v-if="store.error" class="rounded-btn border border-feedback-danger-border bg-feedback-danger-bg px-4 py-3 text-sm text-feedback-danger mb-6" role="alert">
        <p>{{ store.error }}</p>
        <button v-if="isEditing" type="button" class="mt-2 font-bold underline" @click="store.carregarTermo(Number(route.params.id))">Tentar novamente</button>
      </div>

      <form v-if="!isEditing || original" class="flex flex-col gap-7" novalidate @submit.prevent="submitForm">
        <section aria-labelledby="empresa-heading" class="flex flex-col gap-5">
          <h2 id="empresa-heading" class="text-lg font-bold text-amma-green-dark">Dados da empresa</h2>
          <div class="grid grid-cols-1 lg:grid-cols-2 gap-5">
            <div class="flex flex-col gap-1.5">
              <label for="razao-social" class="text-sm font-semibold text-grey-text">Razão social *</label>
              <input id="razao-social" v-model="form.razaoSocial" required maxlength="180" autocomplete="organization" :aria-invalid="Boolean(errorMessages.razaoSocial)" aria-describedby="razao-social-error" class="w-full bg-white focus:bg-amma-green-lighter border-2 border-amma-green-light focus:border-amma-green rounded-btn py-3 px-4 text-grey-text outline-none transition-all text-sm font-medium">
              <p v-if="errorMessages.razaoSocial" id="razao-social-error" class="text-sm text-feedback-danger" role="alert">{{ errorMessages.razaoSocial }}</p>
            </div>
            <div class="flex flex-col gap-1.5">
              <label for="cnpj" class="text-sm font-semibold text-grey-text">CNPJ *</label>
              <input id="cnpj" v-model="form.cnpj" required inputmode="numeric" autocomplete="off" placeholder="00.000.000/0000-00" :aria-invalid="Boolean(errorMessages.cnpj)" aria-describedby="cnpj-error" class="w-full bg-white focus:bg-amma-green-lighter border-2 border-amma-green-light focus:border-amma-green rounded-btn py-3 px-4 text-grey-text outline-none transition-all text-sm font-medium">
              <p v-if="errorMessages.cnpj" id="cnpj-error" class="text-sm text-feedback-danger" role="alert">{{ errorMessages.cnpj }}</p>
            </div>
          </div>
        </section>

        <section aria-labelledby="acordo-heading" class="border-t border-amma-green-light pt-6 flex flex-col gap-5">
          <h2 id="acordo-heading" class="text-lg font-bold text-amma-green-dark">Dados do compromisso</h2>
          <div class="grid grid-cols-1 md:grid-cols-2 gap-5">
            <div class="flex flex-col gap-1.5">
              <label for="numero-termo" class="text-sm font-semibold text-grey-text">Número do termo <span class="font-normal">(se disponível)</span></label>
              <input id="numero-termo" v-model="form.numeroTermo" maxlength="60" placeholder="Ex.: TC-2026-010" class="w-full bg-white focus:bg-amma-green-lighter border-2 border-amma-green-light focus:border-amma-green rounded-btn py-3 px-4 text-grey-text outline-none transition-all text-sm font-medium">
            </div>
            <div class="flex flex-col gap-1.5">
              <label for="quantidade-acordada" class="text-sm font-semibold text-grey-text">Mudas acordadas *</label>
              <input id="quantidade-acordada" v-model.number="form.quantidadeAcordada" required min="1" step="1" type="number" inputmode="numeric" :aria-invalid="Boolean(errorMessages.quantidadeAcordada)" aria-describedby="quantidade-acordada-error" placeholder="Ex.: 500" class="w-full bg-white focus:bg-amma-green-lighter border-2 border-amma-green-light focus:border-amma-green rounded-btn py-3 px-4 text-grey-text outline-none transition-all text-sm font-medium">
              <p v-if="errorMessages.quantidadeAcordada" id="quantidade-acordada-error" class="text-sm text-feedback-danger" role="alert">{{ errorMessages.quantidadeAcordada }}</p>
            </div>
            <div class="flex flex-col gap-1.5">
              <label for="data-assinatura" class="text-sm font-semibold text-grey-text">Data de assinatura <span class="font-normal">(se disponível)</span></label>
              <input id="data-assinatura" v-model="form.dataAssinatura" type="date" class="w-full bg-white focus:bg-amma-green-lighter border-2 border-amma-green-light focus:border-amma-green rounded-btn py-3 px-4 text-grey-text outline-none transition-all text-sm font-medium">
            </div>
            <div class="flex flex-col gap-1.5">
              <label for="prazo-entrega" class="text-sm font-semibold text-grey-text">Prazo de entrega <span class="font-normal">(se disponível)</span></label>
              <input id="prazo-entrega" v-model="form.prazoEntrega" type="date" class="w-full bg-white focus:bg-amma-green-lighter border-2 border-amma-green-light focus:border-amma-green rounded-btn py-3 px-4 text-grey-text outline-none transition-all text-sm font-medium">
            </div>
          </div>
        </section>

        <section v-if="!isEditing" class="border-t border-amma-green-light pt-6 flex flex-col gap-2">
          <label for="termo-pdf" class="text-sm font-semibold text-grey-text">Termo de Compromisso em PDF *</label>
          <button type="button" class="min-h-36 border-2 border-dashed border-amma-green-light hover:border-amma-green bg-amma-green-lighter/30 rounded-box p-6 flex flex-col items-center justify-center gap-2 text-center text-amma-green-dark focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-amma-green" @click="termoInput?.click()">
            <span class="material-symbols-outlined text-3xl" aria-hidden="true">upload_file</span>
            <span class="font-bold">{{ arquivoTermo?.name ?? original?.arquivoTermo ?? 'Clique para anexar o PDF assinado' }}</span>
            <span class="text-xs text-grey-text">PDF até 10 MB</span>
          </button>
          <input id="termo-pdf" ref="termoInput" type="file" accept="application/pdf,.pdf" :required="!isEditing" class="sr-only" :aria-invalid="Boolean(errorMessages.arquivoTermo)" aria-describedby="termo-error" @change="onFileChange($event, 'termo')">
          <p v-if="errorMessages.arquivoTermo" id="termo-error" class="text-sm text-feedback-danger" role="alert">{{ errorMessages.arquivoTermo }}</p>
        </section>

        <section v-else-if="original" class="border-t border-amma-green-light pt-6 flex flex-col gap-2" aria-labelledby="termo-atual-heading">
          <h2 id="termo-atual-heading" class="text-lg font-bold text-amma-green-dark">Termo de Compromisso anexado</h2>
          <button type="button" class="inline-flex w-fit min-h-10 items-center gap-2 text-sm font-semibold text-amma-green underline focus-visible:outline focus-visible:outline-2 focus-visible:outline-amma-green" @click="baixarDocumento()">
            <span class="material-symbols-outlined" aria-hidden="true">picture_as_pdf</span>{{ original.arquivoTermo }} — baixar PDF
          </button>
        </section>

        <section v-if="isEditing && original" class="border-t border-amma-green-light pt-6 flex flex-col gap-4" aria-labelledby="entregas-heading">
          <div class="max-w-xl flex flex-col gap-1.5">
            <label for="estado-acordo" class="text-sm font-semibold text-grey-text">Estado do acordo</label>
            <select id="estado-acordo" v-model="form.status" class="w-full bg-white border-2 border-amma-green-light focus:border-amma-green rounded-btn py-3 px-4 text-grey-text outline-none focus-visible:ring-2 focus-visible:ring-amma-green">
              <option value="EM_ABERTO">○ Em aberto</option>
              <option value="PARCIALMENTE_DOADO">◐ Parcialmente doado</option>
              <option value="TOTALMENTE_DOADO">✓ Totalmente doado/Fechado</option>
            </select>
            <p class="text-xs text-grey-text">Ajuste o estado quando o acordo incluir insumos além de mudas. Uma nova entrega de mudas recalcula o estado pelos recibos acumulados.</p>
          </div>
          <div class="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3">
            <div>
              <h2 id="entregas-heading" class="text-lg font-bold text-amma-green-dark">Recibos de entrega</h2>
              <p class="text-sm text-grey-text mt-1">As parcelas comprovadas são acumuladas. O status é calculado pelo total entregue.</p>
            </div>
            <span class="inline-flex w-fit items-center gap-2 rounded-full border border-amma-green-light bg-amma-green-lighter px-3 py-1.5 text-sm font-bold text-amma-green-dark">
              <span class="material-symbols-outlined" aria-hidden="true">task_alt</span>
              {{ original.quantidadeEntregue }} / {{ original.quantidadeAcordada }} mudas entregues
            </span>
          </div>
          <div v-if="original.entregas.length" class="flex flex-col divide-y divide-amma-green-light rounded-btn border border-amma-green-light">
            <div v-for="entrega in original.entregas" :key="entrega.id" class="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-2 p-4">
              <div>
                <p class="font-semibold text-grey-text">{{ entrega.quantidade }} mudas — recibo {{ entrega.arquivoNome }}</p>
                <p class="text-xs text-grey-text mt-1">Registrado em {{ entrega.registradaEm }}</p>
              </div>
              <button type="button" class="inline-flex min-h-10 items-center gap-2 text-sm font-semibold text-amma-green underline focus-visible:outline focus-visible:outline-2 focus-visible:outline-amma-green" @click="baixarDocumento(entrega.id)">
                <span class="material-symbols-outlined" aria-hidden="true">attach_file</span>Baixar recibo
              </button>
            </div>
          </div>
          <p v-else class="rounded-btn border border-amma-green-light bg-amma-bg p-4 text-sm text-grey-text">Nenhuma parcela registrada.</p>

          <div class="grid grid-cols-1 md:grid-cols-2 gap-5">
            <div class="flex flex-col gap-1.5">
              <label for="quantidade-parcela" class="text-sm font-semibold text-grey-text">Quantidade desta parcela</label>
              <input id="quantidade-parcela" v-model.number="quantidadeParcela" type="number" min="1" step="1" inputmode="numeric" :aria-invalid="Boolean(errorMessages.quantidadeParcela)" aria-describedby="parcela-error" class="w-full bg-white focus:bg-amma-green-lighter border-2 border-amma-green-light focus:border-amma-green rounded-btn py-3 px-4 text-grey-text outline-none transition-all text-sm font-medium">
              <p v-if="errorMessages.quantidadeParcela" id="parcela-error" class="text-sm text-feedback-danger" role="alert">{{ errorMessages.quantidadeParcela }}</p>
            </div>
            <div class="flex flex-col gap-1.5">
              <label for="recibo" class="text-sm font-semibold text-grey-text">Recibo da parcela <span class="font-normal">(PDF, PNG ou JPG; até 2 MB)</span></label>
              <input id="recibo" ref="reciboInput" type="file" accept="application/pdf,image/png,image/jpeg,.pdf,.png,.jpg,.jpeg" class="w-full text-sm text-grey-text file:mr-3 file:rounded-btn file:border-0 file:bg-amma-green-lighter file:px-4 file:py-2 file:font-bold file:text-amma-green-dark focus-visible:outline focus-visible:outline-2 focus-visible:outline-amma-green" :aria-invalid="Boolean(errorMessages.reciboArquivo)" aria-describedby="recibo-error" @change="onFileChange($event, 'recibo')">
              <p v-if="reciboArquivo" class="text-xs text-grey-text">Selecionado: {{ reciboArquivo.name }}</p>
              <p v-if="errorMessages.reciboArquivo" id="recibo-error" class="text-sm text-feedback-danger" role="alert">{{ errorMessages.reciboArquivo }}</p>
            </div>
          </div>
        </section>

        <div v-if="operationSuccess" class="rounded-btn border border-feedback-success-border bg-feedback-success-bg p-4 text-sm text-feedback-success" role="status">
          Alterações salvas com sucesso.
        </div>

        <footer class="border-t border-amma-green-light pt-5 flex flex-col-reverse sm:flex-row sm:justify-end gap-3">
          <button type="button" class="min-h-11 rounded-btn border-2 border-amma-green-light bg-white px-6 py-2.5 font-bold text-grey-text hover:bg-amma-bg focus-visible:outline focus-visible:outline-2 focus-visible:outline-amma-green" @click="router.push({ name: 'compensacao-lista' })">Voltar</button>
          <button type="submit" :disabled="isSubmitting || (isEditing && !original)" class="min-h-11 rounded-btn bg-amma-green px-6 py-2.5 font-bold text-white hover:bg-amma-green-dark disabled:cursor-not-allowed disabled:opacity-60 focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-amma-green">
            {{ isSubmitting ? 'Enviando…' : isEditing ? 'Salvar alterações' : 'Salvar termo' }}
          </button>
        </footer>
      </form>
    </section>
  </div>
</template>
