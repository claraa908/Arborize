<script setup lang="ts">
import {
  computed,
  nextTick,
  onMounted,
  reactive,
  ref,
  shallowRef,
  useTemplateRef,
  watch,
} from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import { Hash, MapPin } from 'lucide-vue-next'
import type { AxiosError } from 'axios'
import type { EspecieDTO } from '@/types/viveiro'
import { especieService } from '@/services/especieService'
import { interpretarErroInventario, inventarioService } from '@/services/inventarioService'
import { useInventarioStore } from '@/stores/inventarioStore'
import { useFeedbackStore } from '@/stores/feedbackStore'
import type { ArvoreResponseDTO, ErroInventario } from '@/types/inventario'
import ArvoreFotografias from './ArvoreFotografias.vue'
import ArvoreFotografiasCadastro from './ArvoreFotografiasCadastro.vue'
import InventarioEstado from './InventarioEstado.vue'
import ArvoreLocalizacaoMapa from './ArvoreLocalizacaoMapa.vue'
import { diagnosticarMapa } from './inventarioMapaDiagnostico'
import { useArvoreFicha } from './useArvoreFicha'
import {
  criarPayloadAtualizacaoArvore,
  criarPayloadCadastroArvore,
  criarControleEdicaoDuranteGeocodificacao,
  localizarBairroUnico,
  novoFormularioArvore,
  preencherFormularioArvore,
  validarCadastroArvore,
  type ArvoreCadastroFormulario,
  type ErrosCadastroArvore,
} from './arvoreCadastroForm'

const props = defineProps<{ editando: boolean; identificador?: string }>()
const formularioElemento = useTemplateRef<HTMLFormElement>('formularioElemento')
const alertaFormulario = useTemplateRef<HTMLDivElement>('alertaFormulario')
const mapaLocalizacao =
  useTemplateRef<InstanceType<typeof ArvoreLocalizacaoMapa>>('mapaLocalizacao')
const router = useRouter()
const inventario = useInventarioStore()
const feedback = useFeedbackStore()
const identificadorRota = computed(() => props.identificador ?? '')
const ficha = props.editando ? useArvoreFicha(identificadorRota) : null
const arvore = ficha?.arvore ?? ref<ArvoreResponseDTO | null>(null)
const carregandoArvore = computed(() => ficha?.carregando.value ?? false)
const erroCarregamento = computed(() => ficha?.erro.value ?? null)
const formulario = reactive(novoFormularioArvore())
const controleEdicaoGeocodificacao = criarControleEdicaoDuranteGeocodificacao()
const bairroGeocodificado = ref('')
const bairroFoiSugerido = ref(false)
const bairroConfirmadoManualmente = ref(false)
const especies = ref<EspecieDTO[]>([])
const carregandoEspecies = ref(false)
const erroCatalogo = ref('')
const erroCatalogoBairros = ref('')
const errosCampo = ref<ErrosCadastroArvore>({})
const erroGeral = ref('')
const fotografiasSelecionadas = shallowRef<File[]>([])
const arvoreCriadaId = ref<number | null>(null)
const fotografiasConfirmadas = ref(0)
const enviandoFotografias = ref(false)
const erroCoordenadas = computed(() =>
  errosCampo.value.latitude || errosCampo.value.longitude
    ? 'Marque a localização da árvore no mapa.'
    : '',
)
const salvando = computed(() => inventario.carregando.salvamento)
const idsEspecies = computed(() => {
  const ids = especies.value
    .map((especie) => Number(especie.id))
    .filter((id) => Number.isSafeInteger(id) && id > 0)
  if (arvore.value) ids.push(arvore.value.especie.id)
  return new Set(ids)
})
const especieAtualForaDoCatalogo = computed(() => {
  const especieAtual = arvore.value?.especie
  return especieAtual && !especies.value.some((especie) => Number(especie.id) === especieAtual.id)
    ? especieAtual
    : null
})
const bairrosAtivos = computed(() => inventario.opcoesBairros)
const idsBairrosAtivos = computed(() => new Set(bairrosAtivos.value.map((bairro) => bairro.id)))
const arvoreTemBairroLegadoSemOpcao = computed(() => {
  const nome = arvore.value?.bairroNome ?? arvore.value?.bairro?.nome
  return Boolean(
    nome &&
      !bairrosAtivos.value.some((bairro) => bairro.id === arvore.value?.bairro?.id),
  )
})
const centroInicialMapa = computed<[number, number] | undefined>(() =>
  arvore.value ? [arvore.value.longitude, arvore.value.latitude] : undefined,
)
const tituloErroCarregamento = computed(() => {
  if (erroCarregamento.value?.status === 404) return 'Árvore não encontrada'
  if (erroCarregamento.value?.acessoNegado) return 'Acesso ao inventário indisponível'
  return 'Não foi possível carregar a árvore para edição'
})
const mensagemErroCarregamento = computed(() => {
  if (erroCarregamento.value?.status === 404) {
    return `Não foi encontrada uma árvore com o identificador ${identificadorRota.value}.`
  }
  if (erroCarregamento.value?.acessoNegado) {
    return 'Não foi possível acessar esta árvore para edição. Procure o responsável pelo acesso.'
  }
  return erroCarregamento.value?.message ?? 'A consulta da árvore não foi concluída.'
})
const podeTentarCarregarNovamente = computed(() => erroCarregamento.value?.status !== 404)

const campoClass =
  'w-full min-w-0 rounded-btn border-2 border-amma-green-light bg-white px-4 py-3 text-sm font-medium text-grey-text outline-none transition-colors focus:border-amma-green focus:bg-amma-green-lighter aria-invalid:border-feedback-danger aria-invalid:bg-feedback-danger-bg aria-invalid:focus:border-feedback-danger disabled:cursor-not-allowed disabled:bg-amma-green-lighter'
const labelClass = 'mb-1.5 block text-sm font-semibold text-grey-text'

function formatarCoordenada(valor: string) {
  return valor !== '' && Number.isFinite(Number(valor))
    ? Number(valor).toFixed(6)
    : 'Aguardando mapa'
}

const controles: Array<[keyof ArvoreCadastroFormulario, string]> = [
  ['especieId', 'arvore-especie'],
  ['latitude', ''],
  ['longitude', ''],
  ['zonaRural', 'arvore-zona-rural'],
  ['bairroId', 'arvore-bairro'],
  ['localidadeRural', 'arvore-localidade-rural'],
  ['cep', 'arvore-cep'],
  ['referenciaLocalizacao', 'arvore-referencia'],
  ['dataPlantio', 'arvore-plantio'],
  ['condicaoFitossanitaria', 'arvore-condicao'],
  ['dapCm', 'arvore-dap'],
  ['alturaEstimadaM', 'arvore-altura'],
  ['observacoes', 'arvore-observacoes'],
]

async function focarPrimeiroErro() {
  await nextTick()
  const controle = controles.find(([campo]) => errosCampo.value[campo])
  if (controle?.[0] === 'latitude' || controle?.[0] === 'longitude') {
    mapaLocalizacao.value?.focarMapa()
    return
  }
  const elemento = controle
    ? formularioElemento.value?.querySelector<HTMLElement>(`#${controle[1]}`)
    : alertaFormulario.value
  elemento?.scrollIntoView({ behavior: 'smooth', block: 'center' })
  elemento?.focus({ preventScroll: true })
}

function idDaEspecie(especie: EspecieDTO): number | null {
  const id = Number(especie.id)
  return Number.isSafeInteger(id) && id > 0 ? id : null
}

async function carregarEspecies() {
  carregandoEspecies.value = true
  erroCatalogo.value = ''
  try {
    const catalogo = await especieService.getAll()
    especies.value = catalogo.filter((especie) => idDaEspecie(especie) !== null)
    if (especies.value.length === 0) {
      erroCatalogo.value = 'Nenhuma espécie está disponível para seleção.'
    }
  } catch (erro) {
    const status = (erro as AxiosError).response?.status
    erroCatalogo.value =
      status === 401 || status === 403
        ? 'Seu acesso não permite carregar as espécies disponíveis.'
        : 'Não foi possível carregar as espécies disponíveis. Tente novamente.'
  } finally {
    carregandoEspecies.value = false
  }
}

function carregarFormulario(arvoreCarregada: ArvoreResponseDTO | null) {
  Object.assign(formulario, arvoreCarregada ? preencherFormularioArvore(arvoreCarregada) : novoFormularioArvore())
  bairroGeocodificado.value = ''
  bairroFoiSugerido.value = false
  bairroConfirmadoManualmente.value = false
  if (arvoreCarregada && !formulario.zonaRural && arvoreCarregada.bairro?.ativo) {
    const associadoAtivo = bairrosAtivos.value.find(
      (bairro) => bairro.id === arvoreCarregada.bairro?.id,
    )
    if (associadoAtivo) {
      formulario.bairroId = String(associadoAtivo.id)
    }
  }
  errosCampo.value = {}
  erroGeral.value = ''
}

function carregarSelecaoLegada(arvoreCarregada: ArvoreResponseDTO | null) {
  if (!arvoreCarregada || formulario.zonaRural || formulario.bairroId || !arvoreCarregada.bairro) return
  const associadoAtivo = bairrosAtivos.value.find(
    (bairro) => bairro.ativo && bairro.id === arvoreCarregada.bairro?.id,
  )
  if (associadoAtivo) {
    formulario.bairroId = String(associadoAtivo.id)
  }
}

function atualizarLatitude(latitude: number) {
  formulario.latitude = String(latitude)
  diagnosticarMapa('formulario.latitude', { coordenadasValidas: Number.isFinite(latitude) })
  bairroGeocodificado.value = ''
  if (bairroFoiSugerido.value && !bairroConfirmadoManualmente.value) formulario.bairroId = ''
  bairroFoiSugerido.value = false
  delete errosCampo.value.latitude
}

function atualizarLongitude(longitude: number) {
  formulario.longitude = String(longitude)
  diagnosticarMapa('formulario.longitude', { coordenadasValidas: Number.isFinite(longitude) })
  delete errosCampo.value.longitude
}

function iniciarIdentificacaoLocalizacao() {
  controleEdicaoGeocodificacao.iniciarSolicitacao()
}

function registrarEdicaoLocalizacao(
  campo: 'referenciaLocalizacao' | 'zonaRural' | 'bairroId' | 'localidadeRural' | 'cep',
) {
  controleEdicaoGeocodificacao.registrarEdicao(campo)
  if (campo === 'bairroId') {
    bairroConfirmadoManualmente.value = true
    bairroFoiSugerido.value = false
    const bairroId = Number(String(formulario.bairroId ?? '').trim())
    if (Number.isSafeInteger(bairroId) && idsBairrosAtivos.value.has(bairroId)) {
      delete errosCampo.value.bairroId
    }
  }
}

function atualizarLocalizacaoIdentificada(localizacao: {
  logradouro: string | null
  bairro: string | null
  localidade: string | null
  cep: string | null
}) {
  if (controleEdicaoGeocodificacao.podeAtualizar('referenciaLocalizacao')) {
    formulario.referenciaLocalizacao = localizacao.logradouro ?? ''
  }
  if (controleEdicaoGeocodificacao.podeAtualizar('cep')) {
    formulario.cep = localizacao.cep ?? ''
  }
  bairroGeocodificado.value = localizacao.bairro ?? ''
  if (formulario.zonaRural) {
    const sugestaoLocalidade = localizacao.localidade ?? localizacao.bairro
    if (sugestaoLocalidade && controleEdicaoGeocodificacao.podeAtualizar('localidadeRural')) {
      formulario.localidadeRural = sugestaoLocalidade
    }
    return
  }
  if (!bairroConfirmadoManualmente.value && controleEdicaoGeocodificacao.podeAtualizar('bairroId')) {
    aplicarSugestaoBairro()
  }
}

function aplicarSugestaoBairro() {
  if (
    !bairroGeocodificado.value ||
    formulario.zonaRural ||
    bairroConfirmadoManualmente.value ||
    !controleEdicaoGeocodificacao.podeAtualizar('bairroId')
  ) return
  const correspondencia = localizarBairroUnico(bairroGeocodificado.value, bairrosAtivos.value)
  if (correspondencia) {
    formulario.bairroId = String(correspondencia.id)
    bairroFoiSugerido.value = true
    delete errosCampo.value.bairroId
  } else if (bairroFoiSugerido.value) {
    formulario.bairroId = ''
    bairroFoiSugerido.value = false
  }
}

function tratarErro(erro: unknown, operacao: 'cadastro' | 'atualizacao' = 'cadastro') {
  const falha = erro as ErroInventario
  erroGeral.value = falha.acessoNegado
    ? `Esta operação não pôde ser concluída. Nenhuma alteração foi confirmada; os dados preenchidos foram preservados. Procure o responsável pelo acesso.`
    : falha.message ||
      `Não foi possível concluir a ${operacao === 'cadastro' ? 'operação de cadastro' : 'atualização'}. Os dados preenchidos foram preservados.`
  const errosServidor: ErrosCadastroArvore = {}
  for (const campo of falha.problem?.errors ?? []) {
    const chave = campo.field as keyof ArvoreCadastroFormulario
    if (chave in formulario) errosServidor[chave] = campo.message
  }
  errosCampo.value = { ...errosCampo.value, ...errosServidor }
  void focarPrimeiroErro()
}

async function cadastrar() {
  if (salvando.value || enviandoFotografias.value) return
  if (arvoreCriadaId.value !== null) {
    if (fotografiasSelecionadas.value.length === 0) {
      await abrirFicha(arvoreCriadaId.value)
      return
    }
    if (await enviarFotografias(arvoreCriadaId.value)) {
      await abrirFicha(arvoreCriadaId.value)
    }
    return
  }
  erroGeral.value = ''
  errosCampo.value = {}
  const erros = validarCadastroArvore(formulario, idsEspecies.value, idsBairrosAtivos.value)
  errosCampo.value = erros
  if (Object.keys(erros).length > 0) {
    erroGeral.value = 'Revise os campos destacados antes de cadastrar.'
    await focarPrimeiroErro()
    return
  }
  if (carregandoEspecies.value || erroCatalogo.value) {
    erroGeral.value = 'Aguarde o carregamento das espécies antes de cadastrar.'
    await focarPrimeiroErro()
    return
  }

  try {
    const criada = await inventario.createArvore(criarPayloadCadastroArvore(formulario))
    if (!Number.isSafeInteger(criada.id) || criada.id <= 0) {
      erroGeral.value =
        'O cadastro foi confirmado, mas não foi possível identificar a árvore para enviar as fotografias. Procure o responsável pelo sistema.'
      return
    }
    arvoreCriadaId.value = criada.id
    if (fotografiasSelecionadas.value.length > 0) {
      if (!(await enviarFotografias(criada.id))) return
    }
    await abrirFicha(criada.id)
  } catch (erro) {
    tratarErro(erro)
  }
}

async function enviarFotografias(arvoreId: number): Promise<boolean> {
  enviandoFotografias.value = true
  erroGeral.value = ''
  try {
    while (fotografiasSelecionadas.value.length > 0) {
      const arquivo = fotografiasSelecionadas.value[0]
      if (!arquivo) break
      try {
        const fotografia = await inventarioService.adicionarFoto(arvoreId, arquivo)
        if (fotografia.arvoreId !== arvoreId) {
          throw new Error('A resposta da fotografia não corresponde à árvore cadastrada.')
        }
        fotografiasSelecionadas.value = fotografiasSelecionadas.value.filter(
          (selecionada) => selecionada !== arquivo,
        )
        fotografiasConfirmadas.value += 1
      } catch (erro) {
        const falha = interpretarErroInventario(erro)
        erroGeral.value = falha.acessoNegado
          ? `A árvore ${arvoreId} foi cadastrada, mas o acesso não permitiu enviar ${fotografiasSelecionadas.value.length === 1 ? 'a fotografia pendente' : 'as fotografias pendentes'}. Você pode tentar novamente ou adicionar fotografias depois pela ficha técnica.`
          : `A árvore ${arvoreId} foi cadastrada, mas não foi possível enviar ${fotografiasSelecionadas.value.length === 1 ? 'a fotografia pendente' : 'todas as fotografias pendentes'}. ${falha.message} Tente novamente sem cadastrar a árvore outra vez.`
        return false
      }
    }
    return true
  } finally {
    enviandoFotografias.value = false
  }
}

async function abrirFicha(arvoreId: number) {
  try {
    await router.push({ name: 'inventario-detalhes', params: { id: arvoreId } })
  } catch {
    erroGeral.value =
      'A árvore foi cadastrada. Não foi possível abrir a ficha técnica agora; use o Inventário para localizá-la.'
  }
}

async function atualizar() {
  if (salvando.value || !arvore.value) return
  erroGeral.value = ''
  errosCampo.value = {}
  const erros = validarCadastroArvore(formulario, idsEspecies.value, idsBairrosAtivos.value)
  errosCampo.value = erros
  if (Object.keys(erros).length > 0) {
    erroGeral.value = 'Revise os campos destacados antes de salvar.'
    await focarPrimeiroErro()
    return
  }

  const id = Number(identificadorRota.value)
  if (!Number.isSafeInteger(id) || id <= 0) {
    erroGeral.value = 'O identificador da árvore na rota é inválido.'
    return
  }

  let atualizada: ArvoreResponseDTO
  try {
    atualizada = await inventario.updateArvore(id, criarPayloadAtualizacaoArvore(formulario))
  } catch (erro) {
    if (Number(identificadorRota.value) === id) tratarErro(erro, 'atualizacao')
    return
  }
  if (Number(identificadorRota.value) !== id) return
  if (atualizada.id !== id) {
    erroGeral.value =
      'Não foi possível confirmar a atualização. Os dados preenchidos foram preservados.'
    return
  }

  feedback.showToast('Árvore atualizada com sucesso.', 'success', 4000, 'Sucesso')
  try {
    await router.push({ name: 'inventario-detalhes', params: { id } })
  } catch {
    erroGeral.value = 'Árvore atualizada, mas não foi possível abrir a ficha agora.'
  }
}

function alterarZonaRural() {
  registrarEdicaoLocalizacao('zonaRural')
  delete errosCampo.value.bairroId
  delete errosCampo.value.localidadeRural
}

watch(arvore, carregarFormulario, { immediate: true })
watch(bairrosAtivos, () => {
  carregarSelecaoLegada(arvore.value)
  aplicarSugestaoBairro()
})

onMounted(() => {
  void carregarEspecies()
  void inventario.fetchBairros().catch((erro: unknown) => {
    const falha = interpretarErroInventario(erro)
    erroCatalogoBairros.value = falha.acessoNegado
      ? 'Não foi possível carregar os bairros disponíveis.'
      : 'A lista de bairros não está disponível no momento.'
  })
})
</script>

<template>
  <form
    ref="formularioElemento"
    class="grid min-w-0 grid-cols-1 gap-4 xl:h-[calc(100dvh-14rem)] xl:min-h-[30rem] xl:grid-cols-[minmax(0,2fr)_minmax(20rem,1fr)] xl:gap-0"
    novalidate
    :aria-busy="salvando || carregandoArvore || enviandoFotografias"
    @submit.prevent="editando ? atualizar() : cadastrar()"
  >
    <section
      aria-label="Localização da árvore no mapa"
      class="h-[65dvh] min-h-[28rem] min-w-0 rounded-box border border-amma-green-light bg-amma-green-lighter p-2 sm:p-3 xl:h-full xl:min-h-0 xl:rounded-r-none"
      :inert="arvoreCriadaId !== null"
    >
      <ArvoreLocalizacaoMapa
        v-if="!editando || arvore"
        ref="mapaLocalizacao"
        class="h-full"
        :latitude="formulario.latitude === '' ? null : Number(formulario.latitude)"
        :longitude="formulario.longitude === '' ? null : Number(formulario.longitude)"
        :centro-inicial="centroInicialMapa"
        :zoom-inicial="editando ? 16 : undefined"
        :erro-localizacao="erroCoordenadas"
        @update:latitude="atualizarLatitude"
        @update:longitude="atualizarLongitude"
        @localizacao-identificacao-iniciada="iniciarIdentificacaoLocalizacao"
        @localizacao-identificada="atualizarLocalizacaoIdentificada"
      />
      <div
        v-else
        role="status"
        class="flex h-full items-center justify-center rounded-btn border border-amma-green-light bg-white p-6 text-center text-sm text-grey-text"
      >
        {{
          carregandoArvore
            ? `Consultando a localização da árvore ${identificadorRota}…`
            : 'A localização não está disponível até que a árvore seja carregada.'
        }}
      </div>
    </section>

    <aside
      aria-label="Dados da árvore"
      class="flex min-w-0 flex-col bg-white xl:h-full xl:min-h-0 xl:overflow-hidden xl:border-l xl:border-amma-green-light xl:shadow-[-8px_0_20px_rgba(77,110,31,0.08)]"
    >
      <div class="min-h-0 flex-none space-y-4 p-4 xl:flex-1 xl:overflow-y-auto">
        <div class="flex flex-wrap items-center justify-between gap-2">
          <p class="text-xs text-grey-text">
            <span class="font-semibold">*</span> Campo obrigatório
          </p>
        </div>

        <InventarioEstado
          v-if="editando && carregandoArvore"
          estado="carregando"
          titulo="Carregando árvore para edição"
          :mensagem="`Consultando a árvore ${identificadorRota}…`"
        />
        <section v-else-if="editando && erroCarregamento" class="flex flex-col gap-4">
          <InventarioEstado
            estado="erro"
            :titulo="tituloErroCarregamento"
            :mensagem="mensagemErroCarregamento"
          />
          <div class="flex flex-col gap-3 sm:flex-row">
            <button
              v-if="podeTentarCarregarNovamente"
              type="button"
              class="rounded-btn border border-feedback-danger px-5 py-3 text-sm font-bold text-feedback-danger focus-visible:outline-2 focus-visible:outline-offset-2"
              @click="ficha?.recarregar"
            >
              Tentar novamente
            </button>
            <RouterLink
              :to="{ name: 'inventario' }"
              class="rounded-btn border border-amma-green-light px-5 py-3 text-center text-sm font-bold text-amma-green-dark hover:bg-amma-green-lighter focus-visible:outline-2 focus-visible:outline-amma-green"
            >
              Voltar ao inventário
            </RouterLink>
          </div>
        </section>

        <div
          v-if="(!editando || arvore) && erroGeral"
          ref="alertaFormulario"
          role="alert"
          tabindex="-1"
          class="rounded-btn border border-feedback-danger-border bg-feedback-danger-bg p-3 text-sm text-feedback-danger focus-visible:outline-2 focus-visible:outline-feedback-danger"
        >
          {{ erroGeral }}
        </div>

        <div v-if="!editando || arvore" class="space-y-4">
          <fieldset
            :disabled="salvando || arvoreCriadaId !== null"
            class="min-w-0 rounded-box border border-amma-green-light bg-white p-4 shadow-sm md:p-5"
          >
            <legend class="px-2 text-lg font-bold text-amma-green-dark">
              Identificação botânica
            </legend>
            <div class="grid grid-cols-1 gap-5">
              <div
                v-if="editando"
                role="group"
                aria-labelledby="arvore-codigo-titulo"
                class="flex flex-wrap items-center justify-between gap-2 rounded-btn bg-amma-green-lighter px-3 py-2.5"
              >
                <div class="flex items-center gap-2 text-amma-green-dark">
                  <Hash :size="16" aria-hidden="true" />
                  <p id="arvore-codigo-titulo" class="text-sm font-semibold">Código da árvore</p>
                </div>
                <p class="text-sm font-bold text-grey-text" aria-live="polite">
                  {{ editando ? (arvore?.codigo ?? identificador) : 'Gerado após o cadastro' }}
                </p>
              </div>
              <div>
                <label for="arvore-especie" :class="labelClass">Espécie botânica *</label>
                <select
                  id="arvore-especie"
                  v-model="formulario.especieId"
                  :disabled="carregandoEspecies || salvando"
                  :class="campoClass"
                  :aria-invalid="Boolean(errosCampo.especieId)"
                  aria-describedby="especie-ajuda especie-erro"
                  required
                >
                  <option value="" disabled>
                    {{ carregandoEspecies ? 'Carregando espécies…' : 'Selecione uma espécie' }}
                  </option>
                  <option
                    v-for="especie in especies"
                    :key="idDaEspecie(especie)!"
                    :value="String(idDaEspecie(especie))"
                  >
                    {{ especie.nomePopular }} — {{ especie.nomeCientifico }}
                  </option>
                  <option
                    v-if="especieAtualForaDoCatalogo"
                    :value="String(especieAtualForaDoCatalogo.id)"
                  >
                    {{ especieAtualForaDoCatalogo.nomePopular }} —
                    {{ especieAtualForaDoCatalogo.nomeCientifico }}
                  </option>
                </select>
                <p id="especie-ajuda" class="mt-1.5 text-xs text-grey-text">
                  Selecione a espécie da árvore.
                </p>
                <p v-if="erroCatalogo" role="alert" class="mt-1.5 text-sm text-feedback-danger">
                  {{ erroCatalogo }}
                </p>
                <p
                  v-if="errosCampo.especieId"
                  id="especie-erro"
                  class="mt-1.5 text-sm text-feedback-danger"
                >
                  {{ errosCampo.especieId }}
                </p>
              </div>
            </div>
            <button
              v-if="erroCatalogo"
              type="button"
              class="mt-4 rounded-btn border border-amma-green-light px-4 py-2 text-sm font-bold text-amma-green-dark hover:bg-amma-green-lighter"
              :disabled="carregandoEspecies || salvando"
              @click="carregarEspecies"
            >
              Tentar carregar espécies novamente
            </button>
          </fieldset>

          <fieldset
            :disabled="salvando || arvoreCriadaId !== null"
            class="min-w-0 rounded-box border border-amma-green-light bg-white p-4 shadow-sm md:p-5"
          >
            <legend class="px-2 text-lg font-bold text-amma-green-dark">Localização</legend>
            <div class="grid grid-cols-1 gap-4">
              <div
                role="group"
                aria-label="Coordenadas selecionadas"
                aria-describedby="arvore-localizacao-estado"
                class="rounded-btn border border-amma-green-light bg-amma-green-lighter p-3"
                :class="erroCoordenadas ? 'border-feedback-danger' : ''"
              >
                <div class="grid grid-cols-2 gap-3">
                  <div :aria-invalid="Boolean(errosCampo.latitude)">
                    <div class="flex items-center gap-1.5 text-amma-green-dark">
                      <MapPin :size="15" aria-hidden="true" />
                      <p id="arvore-latitude-titulo" class="text-xs font-semibold">Latitude *</p>
                    </div>
                    <output
                      class="mt-1 block min-h-6 text-xs font-semibold tabular-nums text-grey-text"
                      :title="formulario.latitude || undefined"
                      aria-live="polite"
                      aria-labelledby="arvore-latitude-titulo"
                    >
                      {{ formatarCoordenada(formulario.latitude) }}
                    </output>
                  </div>
                  <div :aria-invalid="Boolean(errosCampo.longitude)">
                    <div class="flex items-center gap-1.5 text-amma-green-dark">
                      <MapPin :size="15" aria-hidden="true" />
                      <p id="arvore-longitude-titulo" class="text-xs font-semibold">Longitude *</p>
                    </div>
                    <output
                      class="mt-1 block min-h-6 text-xs font-semibold tabular-nums text-grey-text"
                      :title="formulario.longitude || undefined"
                      aria-live="polite"
                      aria-labelledby="arvore-longitude-titulo"
                    >
                      {{ formatarCoordenada(formulario.longitude) }}
                    </output>
                  </div>
                </div>
              </div>
              <div>
                <label
                  for="arvore-zona-rural"
                  class="flex min-h-11 items-center gap-3 rounded-btn border border-amma-green-light bg-amma-green-lighter px-3 py-2 text-sm font-semibold text-grey-text focus-within:outline-2 focus-within:outline-offset-2 focus-within:outline-amma-green"
                >
                  <input
                    id="arvore-zona-rural"
                    v-model="formulario.zonaRural"
                    type="checkbox"
                    class="h-5 w-5 accent-amma-green focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-amma-green"
                    :aria-invalid="Boolean(errosCampo.zonaRural)"
                    aria-describedby="zona-rural-ajuda"
                    @change="alterarZonaRural"
                  />
                  <span>A árvore está localizada em zona rural</span>
                </label>
                <p id="zona-rural-ajuda" class="mt-1.5 text-xs text-grey-text">
                  A localização é informada pelo operador; o mapa não define a zona.
                </p>
              </div>
              <div v-if="!formulario.zonaRural" class="space-y-2">
                <label for="arvore-bairro" :class="labelClass">Bairro *</label>
                <select
                  id="arvore-bairro"
                  v-model="formulario.bairroId"
                  :disabled="salvando || inventario.carregando.bairros || Boolean(erroCatalogoBairros)"
                  :class="campoClass"
                  :aria-invalid="Boolean(errosCampo.bairroId)"
                  aria-describedby="bairro-ajuda bairro-erro"
                  required
                  @change="registrarEdicaoLocalizacao('bairroId')"
                >
                  <option value="" disabled>Selecione o bairro</option>
                  <option v-for="bairro in bairrosAtivos" :key="bairro.id" :value="String(bairro.id)">
                    {{ bairro.nome }}
                  </option>
                </select>
                <p id="bairro-ajuda" class="text-xs text-grey-text">
                  Selecione um bairro ativo. Uma sugestão do mapa só é aplicada quando corresponder a uma opção única.
                </p>
                <p v-if="erroCatalogoBairros" role="alert" class="text-sm text-feedback-danger">
                  {{ erroCatalogoBairros }}
                </p>
                <p
                  v-else-if="bairroGeocodificado"
                  role="status"
                  class="text-xs leading-relaxed text-amma-green-dark"
                >
                  Sugestão do mapa: “{{ bairroGeocodificado }}”. Confira a seleção.
                </p>
                <p
                  v-if="arvoreTemBairroLegadoSemOpcao && !formulario.bairroId"
                  role="status"
                  class="text-xs text-feedback-warning"
                >
                  Registro anterior: {{ arvore?.bairroNome ?? arvore?.bairro?.nome }}. Selecione um bairro ativo para atualizar.
                </p>
                <p v-if="errosCampo.bairroId" id="bairro-erro" class="text-sm text-feedback-danger">
                  {{ errosCampo.bairroId }}
                </p>
                <button
                  v-if="erroCatalogoBairros"
                  type="button"
                  class="rounded-btn border border-amma-green-light px-4 py-2 text-sm font-bold text-amma-green-dark hover:bg-amma-green-lighter"
                  :disabled="inventario.carregando.bairros || salvando"
                  @click="inventario.fetchBairros().then(() => (erroCatalogoBairros = '')).catch(() => {})"
                >
                  Tentar carregar bairros novamente
                </button>
              </div>
              <div v-else>
                <label for="arvore-localidade-rural" :class="labelClass">Localidade rural *</label>
                <input
                  id="arvore-localidade-rural"
                  v-model="formulario.localidadeRural"
                  type="text"
                  autocomplete="address-level2"
                  placeholder="Informe sítio, comunidade ou outra referência rural"
                  :disabled="salvando"
                  :class="campoClass"
                  :aria-invalid="Boolean(errosCampo.localidadeRural)"
                  aria-describedby="localidade-rural-ajuda localidade-rural-erro"
                  required
                  @input="registrarEdicaoLocalizacao('localidadeRural')"
                />
                <p id="localidade-rural-ajuda" class="mt-1.5 text-xs text-grey-text">
                  Informe a localidade conhecida. O sistema não presume distritos ou limites.
                </p>
                <p
                  v-if="bairroGeocodificado"
                  role="status"
                  class="mt-1.5 text-xs leading-relaxed text-amma-green-dark"
                >
                  Nome territorial sugerido pelo mapa: “{{ bairroGeocodificado }}”. Confira os dados.
                </p>
                <p
                  v-if="errosCampo.localidadeRural"
                  id="localidade-rural-erro"
                  class="mt-1.5 text-sm text-feedback-danger"
                >
                  {{ errosCampo.localidadeRural }}
                </p>
              </div>
              <div>
                <label for="arvore-cep" :class="labelClass">
                  CEP <span class="font-normal">(opcional)</span>
                </label>
                <input
                  id="arvore-cep"
                  v-model="formulario.cep"
                  type="text"
                  inputmode="numeric"
                  autocomplete="postal-code"
                  placeholder="Informe ou revise o CEP"
                  :disabled="salvando"
                  :class="campoClass"
                  :aria-invalid="Boolean(errosCampo.cep)"
                  aria-describedby="cep-ajuda cep-erro"
                  @input="registrarEdicaoLocalizacao('cep')"
                />
                <p id="cep-ajuda" class="mt-1.5 text-xs text-grey-text">
                  Será preenchido quando disponível; zeros à esquerda são preservados.
                </p>
                <p v-if="errosCampo.cep" id="cep-erro" class="mt-1.5 text-sm text-feedback-danger">
                  {{ errosCampo.cep }}
                </p>
              </div>
              <div>
                <label for="arvore-referencia" :class="labelClass">
                  Logradouro ou ponto de referência <span class="font-normal">(opcional)</span>
                </label>
                <input
                  id="arvore-referencia"
                  v-model="formulario.referenciaLocalizacao"
                  type="text"
                  :class="campoClass"
                  :aria-invalid="Boolean(errosCampo.referenciaLocalizacao)"
                  aria-describedby="referencia-erro"
                  @input="registrarEdicaoLocalizacao('referenciaLocalizacao')"
                />
                <p
                  v-if="errosCampo.referenciaLocalizacao"
                  id="referencia-erro"
                  class="mt-1.5 text-sm text-feedback-danger"
                >
                  {{ errosCampo.referenciaLocalizacao }}
                </p>
              </div>
            </div>
          </fieldset>

          <fieldset
            :disabled="salvando || arvoreCriadaId !== null"
            class="min-w-0 rounded-box border border-amma-green-light bg-white p-4 shadow-sm md:p-5"
          >
            <legend class="px-2 text-lg font-bold text-amma-green-dark">
              Características técnicas
            </legend>
            <p class="mb-4 text-sm leading-relaxed text-grey-text">
              Preencha apenas as informações disponíveis. Os campos opcionais podem ficar vazios.
            </p>
            <div class="grid grid-cols-1 gap-5">
              <div>
                <label for="arvore-plantio" :class="labelClass"
                  >Data de plantio <span class="font-normal">(opcional)</span></label
                >
                <input
                  id="arvore-plantio"
                  v-model="formulario.dataPlantio"
                  type="date"
                  :class="campoClass"
                  :aria-invalid="Boolean(errosCampo.dataPlantio)"
                  aria-describedby="plantio-erro"
                />
                <p
                  v-if="errosCampo.dataPlantio"
                  id="plantio-erro"
                  class="mt-1.5 text-sm text-feedback-danger"
                >
                  {{ errosCampo.dataPlantio }}
                </p>
              </div>
              <div>
                <label for="arvore-condicao" :class="labelClass">Condição fitossanitária *</label>
                <select
                  id="arvore-condicao"
                  v-model="formulario.condicaoFitossanitaria"
                  :class="campoClass"
                  :aria-invalid="Boolean(errosCampo.condicaoFitossanitaria)"
                  aria-describedby="condicao-erro"
                  required
                >
                  <option value="NAO_AVALIADA">Não avaliada</option>
                  <option value="APARENTEMENTE_SAUDAVEL">Aparentemente saudável</option>
                  <option value="SINAIS_COMPROMETIMENTO">Com sinais de comprometimento</option>
                  <option value="MORTA">Morta</option>
                </select>
                <p class="mt-1.5 text-xs text-grey-text">Selecione a condição observada.</p>
                <p
                  v-if="errosCampo.condicaoFitossanitaria"
                  id="condicao-erro"
                  class="mt-1.5 text-sm text-feedback-danger"
                >
                  {{ errosCampo.condicaoFitossanitaria }}
                </p>
              </div>
              <div>
                <label for="arvore-dap" :class="labelClass"
                  >DAP (cm) <span class="font-normal">(opcional)</span></label
                >
                <input
                  id="arvore-dap"
                  v-model="formulario.dapCm"
                  type="number"
                  min="0"
                  step="any"
                  inputmode="decimal"
                  :class="campoClass"
                  :aria-invalid="Boolean(errosCampo.dapCm)"
                  aria-describedby="dap-erro"
                />
                <p
                  v-if="errosCampo.dapCm"
                  id="dap-erro"
                  class="mt-1.5 text-sm text-feedback-danger"
                >
                  {{ errosCampo.dapCm }}
                </p>
              </div>
              <div>
                <label for="arvore-altura" :class="labelClass"
                  >Altura estimada (m) <span class="font-normal">(opcional)</span></label
                >
                <input
                  id="arvore-altura"
                  v-model="formulario.alturaEstimadaM"
                  type="number"
                  min="0"
                  step="any"
                  inputmode="decimal"
                  :class="campoClass"
                  :aria-invalid="Boolean(errosCampo.alturaEstimadaM)"
                  aria-describedby="altura-erro"
                />
                <p
                  v-if="errosCampo.alturaEstimadaM"
                  id="altura-erro"
                  class="mt-1.5 text-sm text-feedback-danger"
                >
                  {{ errosCampo.alturaEstimadaM }}
                </p>
              </div>
              <p class="text-xs leading-relaxed text-grey-text">
                Quando informadas, as medidas devem ser positivas.
              </p>
              <div>
                <label for="arvore-observacoes" :class="labelClass"
                  >Observações <span class="font-normal">(opcional)</span></label
                >
                <textarea
                  id="arvore-observacoes"
                  v-model="formulario.observacoes"
                  rows="4"
                  :class="campoClass"
                  :aria-invalid="Boolean(errosCampo.observacoes)"
                  aria-describedby="observacoes-erro"
                />
                <p
                  v-if="errosCampo.observacoes"
                  id="observacoes-erro"
                  class="mt-1.5 text-sm text-feedback-danger"
                >
                  {{ errosCampo.observacoes }}
                </p>
              </div>
            </div>
          </fieldset>

          <ArvoreFotografias
            v-if="editando && arvore"
            :arvore-id="arvore.id"
            permitir-upload
            permitir-remocao
          />
          <ArvoreFotografiasCadastro
            v-else-if="!editando"
            v-model="fotografiasSelecionadas"
            :quantidade-existente="fotografiasConfirmadas"
            :desativado="enviandoFotografias"
          />
          <p
            v-if="arvoreCriadaId !== null && fotografiasSelecionadas.length > 0"
            class="text-xs leading-relaxed text-grey-text"
          >
            Se continuar para a ficha técnica sem reenviar, os arquivos pendentes desta seleção
            serão descartados. Você poderá selecioná-los novamente pela ficha.
          </p>
          <RouterLink
            v-if="arvoreCriadaId !== null && fotografiasSelecionadas.length > 0"
            :to="{ name: 'inventario-detalhes', params: { id: arvoreCriadaId } }"
            class="text-sm font-semibold text-amma-green-dark underline focus-visible:outline-2 focus-visible:outline-offset-2"
          >
            Continuar sem estes arquivos e abrir a ficha técnica
          </RouterLink>
        </div>
      </div>
      <footer
        v-if="!editando || arvore"
        class="flex shrink-0 flex-col-reverse gap-3 border-t border-amma-green-light bg-white p-4 sm:flex-row sm:justify-end"
      >
        <RouterLink
          :to="
            editando
              ? { name: 'inventario-detalhes', params: { id: identificador } }
              : { name: 'inventario' }
          "
          class="rounded-btn border border-amma-green-light px-6 py-3 text-center text-sm font-bold text-grey-text transition-colors hover:bg-amma-green-lighter focus-visible:outline-2 focus-visible:outline-amma-green"
          >Cancelar</RouterLink
        >
        <button
          type="submit"
          :disabled="
            salvando ||
            enviandoFotografias ||
            (editando && carregandoArvore) ||
            (!editando && carregandoEspecies)
          "
          class="rounded-btn bg-amma-green px-6 py-3 text-sm font-bold text-white transition-opacity hover:bg-amma-green-dark focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-amma-green disabled:cursor-wait disabled:opacity-60"
        >
          {{
            editando
              ? salvando
                ? 'Salvando…'
                : 'Salvar alterações'
              : enviandoFotografias
                ? 'Enviando fotografias…'
                : arvoreCriadaId !== null
                  ? fotografiasSelecionadas.length > 0
                    ? 'Tentar envio novamente'
                    : 'Abrir ficha técnica'
                  : salvando
                    ? 'Cadastrando…'
                    : 'Cadastrar árvore'
          }}
        </button>
      </footer>
    </aside>
  </form>
</template>
