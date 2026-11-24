<script setup lang="ts">
import { computed, onBeforeUnmount, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useFeedbackStore } from '@/stores/feedbackStore'
import { substituicaoBotanicaService } from '@/services/substituicaoBotanicaService'
import { formatCpf, isValidCpf } from '@/utils/cpf'
import { formatBrazilianPhone } from '@/utils/phone'

const router = useRouter()
const feedback = useFeedbackStore()
const form = reactive({
  nomeSolicitante: '',
  cpf: '',
  telefone: '',
  logradouro: '',
  numeroEndereco: '',
  bairro: '',
  complemento: '',
  pontoReferencia: '',
  descricaoArvore: ''
})
const foto = ref<File | null>(null)
const fotoErro = ref('')
const enviando = ref(false)
const erroEnvio = ref('')
const previewUrl = ref<string | null>(null)
const cpfTentado = ref(false)
const telefoneTentado = ref(false)
const fieldClass = 'mt-1 w-full min-w-0 rounded-btn border-2 border-amma-green-light bg-white px-4 py-3 text-sm font-medium text-grey-text outline-none transition-all focus:border-amma-green focus:bg-amma-green-lighter focus-visible:ring-2 focus-visible:ring-amma-green'
const telefoneValido = computed(() => {
  const digitos = form.telefone.replace(/\D/g, '')
  return [10, 11].includes(digitos.length) && Number(digitos.slice(0, 2)) >= 11
})
const telefoneErro = computed(() => telefoneTentado.value && !telefoneValido.value)

function atualizarTelefone(event: Event) {
  const input = event.target as HTMLInputElement
  form.telefone = formatBrazilianPhone(input.value)
  input.value = form.telefone
}

function selecionarFoto(event: Event) {
  const input = event.target as HTMLInputElement
  fotoErro.value = ''
  const arquivo = input.files?.[0]
  if (!arquivo) return
  const extensaoValida = /\.(jpe?g|png|webp)$/i.test(arquivo.name)
  const mimeValido = ['image/jpeg', 'image/png', 'image/webp'].includes(arquivo.type.toLowerCase())
  if (!extensaoValida || !mimeValido) {
    fotoErro.value = 'Escolha uma imagem JPG, PNG ou WebP.'
    input.value = ''
    return
  }
  if (arquivo.size > 2 * 1024 * 1024) {
    fotoErro.value = 'A fotografia deve ter no máximo 2 MB.'
    input.value = ''
    return
  }
  foto.value = arquivo
  if (previewUrl.value) URL.revokeObjectURL(previewUrl.value)
  previewUrl.value = URL.createObjectURL(arquivo)
}

function errosDaResposta(error: unknown): Record<string, string> {
  if (typeof error !== 'object' || error === null || !('response' in error)) return {}
  const body = (error as { response?: { data?: { errors?: unknown } } }).response?.data
  if (!body?.errors || typeof body.errors !== 'object') return {}
  if (Array.isArray(body.errors)) {
    return Object.fromEntries(body.errors.flatMap((item) => {
      if (typeof item !== 'object' || item === null || !('field' in item) || !('message' in item)) return []
      return [[String(item.field), String(item.message)]]
    }))
  }
  return Object.fromEntries(Object.entries(body.errors).map(([key, value]) => [key, String(value)]))
}

async function enviar() {
  erroEnvio.value = ''
  cpfTentado.value = true
  telefoneTentado.value = true
  if (!isValidCpf(form.cpf)) {
    feedback.showToast('Informe um CPF válido.', 'error')
    return
  }
  if (!telefoneValido.value) {
    feedback.showToast('Informe um telefone com DDD.', 'error')
    return
  }
  enviando.value = true
  const dados = new FormData()
  Object.entries(form).forEach(([key, value]) => dados.append(key, value))
  if (foto.value) dados.append('foto', foto.value)
  try {
    const resultado = await substituicaoBotanicaService.criar(dados)
    feedback.showToast('Requerimento enviado. Guarde o protocolo para acompanhar a análise da AMMA.', 'success', 6500)
    await router.push({ name: 'arborizacao-substituicao-acompanhar', query: { protocolo: resultado.protocolo } })
  } catch (error) {
    const errors = errosDaResposta(error)
    if (Object.keys(errors).length) {
      erroEnvio.value = Object.values(errors).join(' ')
      feedback.showToast('Revise os dados do requerimento e tente novamente.', 'error')
    } else {
      erroEnvio.value = 'Não foi possível enviar o requerimento. Seus dados continuam preenchidos.'
    }
  } finally {
    enviando.value = false
  }
}

onBeforeUnmount(() => {
  if (previewUrl.value) URL.revokeObjectURL(previewUrl.value)
})
</script>

<template>
  <section class="mx-auto w-full max-w-5xl min-w-0 py-6 md:py-10" aria-labelledby="nim-request-title">
    <header class="mb-6">
      <RouterLink :to="{ name: 'arborizacao' }" class="inline-flex min-h-11 items-center gap-2 rounded-btn px-2 text-sm font-semibold text-amma-green-dark focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-amma-green">
        <span class="material-symbols-outlined" aria-hidden="true">arrow_back</span> Arborização
      </RouterLink>
      <p class="mt-4 text-xs font-bold uppercase tracking-wider text-amma-green">Requerimento do cidadão</p>
      <h1 id="nim-request-title" class="mt-2 text-2xl font-bold text-amma-green-dark">Solicitar retirada de Nim Indiano</h1>
      <p class="mt-2 max-w-3xl text-sm leading-6 text-grey-text">Informe onde a árvore está. A AMMA vai analisar o pedido e, se aprovado, conduzir o laudo e as etapas seguintes.</p>
    </header>

    <form class="min-w-0 space-y-5" @submit.prevent="enviar">
      <fieldset class="min-w-0 rounded-box border border-amma-green-light bg-white p-5 shadow-sm md:p-7">
        <legend class="px-2 text-lg font-bold text-amma-green-dark">Localização da árvore</legend>
        <div class="grid min-w-0 grid-cols-1 gap-4 sm:grid-cols-2">
          <div class="min-w-0 sm:col-span-2"><label for="nim-street" class="text-sm font-semibold text-grey-text">Rua ou localidade *</label><input id="nim-street" v-model.trim="form.logradouro" required maxlength="180" autocomplete="street-address" :class="fieldClass" /></div>
          <div class="min-w-0"><label for="nim-number" class="text-sm font-semibold text-grey-text">Número *</label><input id="nim-number" v-model.trim="form.numeroEndereco" required maxlength="30" :class="fieldClass" /></div>
          <div class="min-w-0"><label for="nim-neighborhood" class="text-sm font-semibold text-grey-text">Bairro *</label><input id="nim-neighborhood" v-model.trim="form.bairro" required maxlength="100" :class="fieldClass" /></div>
          <div class="min-w-0"><label for="nim-complement" class="text-sm font-semibold text-grey-text">Complemento</label><input id="nim-complement" v-model.trim="form.complemento" maxlength="180" :class="fieldClass" /></div>
          <div class="min-w-0"><label for="nim-reference" class="text-sm font-semibold text-grey-text">Ponto de referência *</label><input id="nim-reference" v-model.trim="form.pontoReferencia" required maxlength="255" :class="fieldClass" placeholder="Ex.: em frente à praça" /></div>
          <div class="min-w-0 sm:col-span-2"><label for="nim-description" class="text-sm font-semibold text-grey-text">Descreva a árvore e a situação *</label><textarea id="nim-description" v-model.trim="form.descricaoArvore" required maxlength="2000" rows="4" :class="fieldClass" placeholder="Conte como identificar o Nim e descreva o motivo do pedido." /></div>
          <div class="min-w-0 sm:col-span-2">
            <label for="nim-photo" class="text-sm font-semibold text-grey-text">Fotografia da árvore (opcional)</label>
            <div class="mt-1 flex min-w-0 flex-col gap-4 rounded-box border-2 border-dashed border-amma-green-light bg-amma-green-lighter p-4 sm:flex-row sm:items-center">
              <img v-if="previewUrl" :src="previewUrl" alt="Prévia da fotografia selecionada" class="h-28 w-28 shrink-0 rounded-btn object-cover" />
              <div class="min-w-0 flex-1"><input id="nim-photo" type="file" accept="image/jpeg,image/png,image/webp,.jpg,.jpeg,.png,.webp" class="block w-full min-w-0 text-sm text-grey-text file:mr-4 file:rounded-btn file:border-0 file:bg-white file:px-4 file:py-2 file:font-bold file:text-amma-green-dark focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-amma-green" aria-describedby="nim-photo-help nim-photo-error" @change="selecionarFoto" /><p id="nim-photo-help" class="mt-2 text-xs text-grey-text">JPG, PNG ou WebP · até 2 MB.</p><p v-if="fotoErro" id="nim-photo-error" class="mt-1 text-sm text-feedback-danger" role="alert">{{ fotoErro }}</p></div>
            </div>
          </div>
        </div>
      </fieldset>

      <fieldset class="min-w-0 rounded-box border border-amma-green-light bg-white p-5 shadow-sm md:p-7">
        <legend class="px-2 text-lg font-bold text-amma-green-dark">Identificação e contato</legend>
        <div class="grid min-w-0 grid-cols-1 gap-4 sm:grid-cols-2">
          <div class="min-w-0 sm:col-span-2"><label for="nim-name" class="text-sm font-semibold text-grey-text">Nome completo *</label><input id="nim-name" v-model.trim="form.nomeSolicitante" required maxlength="180" autocomplete="name" :class="fieldClass" /></div>
          <div class="min-w-0"><label for="nim-cpf" class="text-sm font-semibold text-grey-text">CPF *</label><input id="nim-cpf" :value="form.cpf" required inputmode="numeric" autocomplete="off" maxlength="14" :class="fieldClass" :aria-invalid="cpfTentado && !isValidCpf(form.cpf)" :aria-describedby="cpfTentado && !isValidCpf(form.cpf) ? 'nim-cpf-help nim-cpf-error' : 'nim-cpf-help'" placeholder="000.000.000-00" @input="form.cpf = formatCpf(($event.target as HTMLInputElement).value)" /><p id="nim-cpf-help" class="mt-1 text-xs text-grey-text">Usado para proteger a consulta do protocolo.</p><p v-if="cpfTentado && !isValidCpf(form.cpf)" id="nim-cpf-error" class="mt-1 text-sm text-feedback-danger" role="alert">Informe um CPF válido.</p></div>
          <div class="min-w-0">
            <label for="nim-phone" class="text-sm font-semibold text-grey-text">Telefone com DDD *</label>
            <input
              id="nim-phone"
              :value="form.telefone"
              type="tel"
              required
              autocomplete="tel-national"
              :class="fieldClass"
              :aria-invalid="telefoneErro"
              :aria-describedby="telefoneErro ? 'nim-phone-error' : undefined"
              @input="atualizarTelefone"
              @blur="telefoneTentado = true"
            />
            <p v-if="telefoneErro" id="nim-phone-error" class="mt-1 text-sm text-feedback-danger" role="alert">
              Informe um telefone com DDD.
            </p>
          </div>
        </div>
      </fieldset>

      <div v-if="erroEnvio" class="rounded-btn border border-feedback-danger-border bg-feedback-danger-bg px-4 py-3 text-sm text-feedback-danger" role="alert">{{ erroEnvio }}</div>
      <div class="flex flex-col-reverse gap-3 sm:flex-row sm:justify-end">
        <RouterLink :to="{ name: 'arborizacao' }" class="inline-flex min-h-11 items-center justify-center rounded-btn border-2 border-amma-green bg-white px-5 py-2.5 text-sm font-bold text-amma-green hover:bg-amma-green-lighter focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-amma-green">Cancelar</RouterLink>
        <button type="submit" :disabled="enviando" :aria-busy="enviando" class="efeito-zoom inline-flex min-h-11 items-center justify-center gap-2 rounded-btn bg-amma-green px-6 py-2.5 text-sm font-bold text-white hover:bg-amma-green-dark focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-amma-green disabled:cursor-wait disabled:opacity-70">
          <span v-if="enviando" class="material-symbols-outlined animate-spin" aria-hidden="true">progress_activity</span>{{ enviando ? 'Enviando…' : 'Enviar requerimento' }}
        </button>
      </div>
    </form>
  </section>
</template>
