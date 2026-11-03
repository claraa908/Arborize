<script setup lang="ts">
import { computed, onBeforeUnmount, reactive, ref, watch } from 'vue'
import { inventarioService, interpretarErroInventario } from '@/services/inventarioService'
import { useFeedbackStore } from '@/stores/feedbackStore'
import type { ErroInventario, FotoArvoreResponseDTO } from '@/types/inventario'
import InventarioEstado from './InventarioEstado.vue'
import {
  LIMITE_FOTOGRAFIAS_ARVORE,
  limiteFotografiasExcedido,
  validarArquivoFotografia,
} from './arvoreFotografias'

interface ArquivoPendente {
  arquivo: File
  url: string
}

const props = withDefaults(
  defineProps<{
    arvoreId: number
    permitirUpload?: boolean
    permitirRemocao?: boolean
  }>(),
  { permitirUpload: false, permitirRemocao: false },
)

const feedback = useFeedbackStore()
const fotografias = ref<FotoArvoreResponseDTO[]>([])
const urlsFotografias = reactive(new Map<number, string>())
const errosConteudo = reactive(new Map<number, string>())
const arquivosPendentes = ref<ArquivoPendente[]>([])
const carregando = ref(false)
const enviando = ref(false)
const removendoId = ref<number | null>(null)
const erroCarregamento = ref<ErroInventario | null>(null)
const erroOperacao = ref('')
const mensagemSucesso = ref('')
const mensagensSelecao = ref<string[]>([])
const arquivoInput = ref<HTMLInputElement | null>(null)
const quantidadeAtual = computed(() => fotografias.value.length + arquivosPendentes.value.length)
const vagasRestantes = computed(() =>
  Math.max(0, LIMITE_FOTOGRAFIAS_ARVORE - quantidadeAtual.value),
)
const acessoNegado = computed(() => erroCarregamento.value?.acessoNegado ?? false)
let sequenciaConsulta = 0

function liberarUrl(url: string) {
  URL.revokeObjectURL(url)
}

function liberarPreviews() {
  for (const url of urlsFotografias.values()) liberarUrl(url)
  urlsFotografias.clear()
  errosConteudo.clear()
  for (const pendente of arquivosPendentes.value) liberarUrl(pendente.url)
  arquivosPendentes.value = []
}

function mensagemErro(error: unknown, acao: 'consultar' | 'enviar' | 'remover'): string {
  const falha = interpretarErroInventario(error)
  if (falha.acessoNegado) {
    return 'Não foi possível acessar as fotografias. Nenhuma alteração foi confirmada. Procure o responsável pelo acesso.'
  }
  if (falha.status === 404) return 'A árvore ou a fotografia não foi encontrada.'
  if (falha.status === 413) return 'A fotografia excede o limite de 2 MB.'
  if (falha.status === 400 && falha.problem?.errors?.some((item) => item.field === 'fotografias')) {
    return (
      falha.problem.detail || 'A fotografia não foi aceita. Confira formato e limite de imagens.'
    )
  }
  if (falha.status === null) {
    return `Falha de rede ao ${acao === 'consultar' ? 'carregar' : acao === 'enviar' ? 'enviar' : 'remover'} fotografia. Tente novamente.`
  }
  return falha.problem?.detail || falha.message || 'A operação de fotografia não foi concluída.'
}

async function carregarGaleria() {
  const consulta = ++sequenciaConsulta
  const arvoreId = props.arvoreId
  liberarPreviews()
  fotografias.value = []
  erroCarregamento.value = null
  erroOperacao.value = ''
  mensagemSucesso.value = ''
  enviando.value = false
  removendoId.value = null
  carregando.value = true

  try {
    const metadados = await inventarioService.listarFotos(arvoreId)
    if (consulta !== sequenciaConsulta) return
    if (metadados.some((foto) => foto.arvoreId !== arvoreId)) {
      throw new Error('A resposta contém fotografias associadas a outra árvore.')
    }
    fotografias.value = metadados

    const resultados = await Promise.allSettled(
      metadados.map(async (foto) => ({
        foto,
        conteudo: await inventarioService.recuperarFoto(arvoreId, foto.id),
      })),
    )
    if (consulta !== sequenciaConsulta) return
    for (const [index, resultado] of resultados.entries()) {
      const foto = metadados[index]
      if (!foto) continue
      if (resultado.status === 'fulfilled') {
        urlsFotografias.set(foto.id, URL.createObjectURL(resultado.value.conteudo))
      } else {
        const falha = interpretarErroInventario(resultado.reason)
        if (falha.acessoNegado) erroCarregamento.value = falha
        errosConteudo.set(foto.id, mensagemErro(resultado.reason, 'consultar'))
      }
    }
  } catch (error) {
    if (consulta === sequenciaConsulta) erroCarregamento.value = interpretarErroInventario(error)
  } finally {
    if (consulta === sequenciaConsulta) carregando.value = false
  }
}

function selecionarArquivos(evento: Event) {
  const input = evento.target as HTMLInputElement
  const selecionados = Array.from(input.files ?? [])
  const avisos: string[] = []

  for (const arquivo of selecionados) {
    if (limiteFotografiasExcedido(quantidadeAtual.value)) {
      avisos.push('Cada árvore pode possuir até três fotografias.')
      break
    }
    const erro = validarArquivoFotografia(arquivo)
    if (erro) {
      avisos.push(erro)
      continue
    }
    arquivosPendentes.value.push({ arquivo, url: URL.createObjectURL(arquivo) })
  }
  mensagensSelecao.value = avisos
  erroOperacao.value = ''
  mensagemSucesso.value = ''
  input.value = ''
}

function removerPendente(index: number) {
  const [removido] = arquivosPendentes.value.splice(index, 1)
  if (removido) liberarUrl(removido.url)
}

async function enviarSelecionadas() {
  if (!props.permitirUpload || enviando.value || arquivosPendentes.value.length === 0) return
  const consulta = sequenciaConsulta
  const arvoreId = props.arvoreId
  enviando.value = true
  erroOperacao.value = ''
  mensagemSucesso.value = ''
  mensagensSelecao.value = []
  let enviadas = 0

  while (arquivosPendentes.value.length > 0) {
    const pendente = arquivosPendentes.value[0]
    if (!pendente) break
    try {
      const criada = await inventarioService.adicionarFoto(arvoreId, pendente.arquivo)
      if (consulta !== sequenciaConsulta || arvoreId !== props.arvoreId) return
      if (criada.arvoreId !== arvoreId) {
        erroOperacao.value = 'A resposta do servidor não corresponde à árvore solicitada.'
        break
      }
      fotografias.value = [...fotografias.value, criada]
      arquivosPendentes.value.shift()
      liberarUrl(pendente.url)
      enviadas += 1
      try {
        const conteudo = await inventarioService.recuperarFoto(arvoreId, criada.id)
        if (consulta !== sequenciaConsulta || arvoreId !== props.arvoreId) return
        urlsFotografias.set(criada.id, URL.createObjectURL(conteudo))
      } catch (error) {
        if (consulta === sequenciaConsulta && arvoreId === props.arvoreId) {
          const falha = interpretarErroInventario(error)
          errosConteudo.set(
            criada.id,
            falha.acessoNegado
              ? 'Não foi possível exibir esta fotografia.'
              : mensagemErro(error, 'consultar'),
          )
          erroOperacao.value = `O envio da fotografia foi confirmado, mas não foi possível carregar sua pré-visualização. ${mensagemErro(error, 'consultar')}`
        }
      }
    } catch (error) {
      if (consulta === sequenciaConsulta && arvoreId === props.arvoreId) {
        erroOperacao.value = mensagemErro(error, 'enviar')
      }
      break
    }
  }

  enviando.value = false
  if (enviadas > 0) {
    mensagemSucesso.value = `${enviadas} ${enviadas === 1 ? 'fotografia enviada' : 'fotografias enviadas'} com confirmação do servidor.`
    feedback.showToast(mensagemSucesso.value, 'success', 4000, 'Sucesso')
  }
}

async function removerFotografia(foto: FotoArvoreResponseDTO) {
  if (!props.permitirRemocao || removendoId.value !== null) return
  const confirmado = await feedback.showConfirm({
    title: 'Remover fotografia',
    message: `Confirma a remoção da fotografia ${foto.id}? Esta ação não altera os demais arquivos associados à árvore.`,
    confirmText: 'Remover fotografia',
    cancelText: 'Cancelar',
    variant: 'danger',
  })
  if (!confirmado) return

  const arvoreId = props.arvoreId
  const consulta = sequenciaConsulta
  removendoId.value = foto.id
  erroOperacao.value = ''
  mensagemSucesso.value = ''
  try {
    await inventarioService.removerFoto(arvoreId, foto.id)
    if (consulta !== sequenciaConsulta || arvoreId !== props.arvoreId) return
    fotografias.value = fotografias.value.filter((item) => item.id !== foto.id)
    const url = urlsFotografias.get(foto.id)
    if (url) liberarUrl(url)
    urlsFotografias.delete(foto.id)
    errosConteudo.delete(foto.id)
    mensagemSucesso.value = 'Fotografia removida com confirmação do servidor.'
    feedback.showToast(mensagemSucesso.value, 'success', 4000, 'Sucesso')
  } catch (error) {
    if (consulta === sequenciaConsulta && arvoreId === props.arvoreId) {
      erroOperacao.value = mensagemErro(error, 'remover')
    }
  } finally {
    if (consulta === sequenciaConsulta) removendoId.value = null
  }
}

function formatarTamanho(bytes: number): string {
  return bytes < 1024 * 1024
    ? `${Math.ceil(bytes / 1024)} KB`
    : `${(bytes / (1024 * 1024)).toFixed(1)} MB`
}

watch(
  () => props.arvoreId,
  () => void carregarGaleria(),
  { immediate: true },
)
onBeforeUnmount(() => {
  sequenciaConsulta += 1
  liberarPreviews()
})
</script>

<template>
  <section
    aria-labelledby="fotografias-arvore-titulo"
    class="min-w-0 rounded-box border border-amma-green-light bg-white p-5 shadow-sm md:p-8"
  >
    <header class="mb-5 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
      <div>
        <h2
          id="fotografias-arvore-titulo"
          class="flex items-center gap-2 text-lg font-bold text-amma-green-dark"
        >
          <span aria-hidden="true" class="material-symbols-outlined">photo_library</span>
          Fotografias
        </h2>
        <p class="mt-1 text-sm text-grey-text">
          {{ fotografias.length }} de {{ LIMITE_FOTOGRAFIAS_ARVORE }} fotografias associadas. JPG,
          PNG ou WebP; até 2 MB cada.
        </p>
      </div>
      <label
        v-if="permitirUpload && vagasRestantes > 0"
        for="arvore-fotografias-seletor"
        class="inline-flex cursor-pointer items-center justify-center gap-2 self-start rounded-btn border border-amma-green px-4 py-2 text-sm font-bold text-amma-green-dark transition-colors hover:bg-amma-green-lighter focus-within:outline-2 focus-within:outline-amma-green"
        :class="{ 'pointer-events-none opacity-60': enviando || carregando }"
      >
        <span aria-hidden="true" class="material-symbols-outlined text-[18px]"
          >add_photo_alternate</span
        >
        Selecionar imagens
        <input
          id="arvore-fotografias-seletor"
          ref="arquivoInput"
          class="sr-only"
          type="file"
          accept="image/jpeg,image/png,image/webp,.jpg,.jpeg,.png,.webp"
          multiple
          :disabled="enviando || carregando"
          @change="selecionarArquivos"
        />
      </label>
    </header>

    <InventarioEstado
      v-if="carregando"
      estado="carregando"
      titulo="Carregando fotografias"
      mensagem="Consultando os arquivos associados à árvore."
    />
    <div v-else-if="erroCarregamento" class="space-y-3">
      <InventarioEstado
        estado="erro"
        :titulo="
          acessoNegado
            ? 'Acesso às fotografias indisponível'
            : 'Não foi possível carregar as fotografias'
        "
        :mensagem="mensagemErro(erroCarregamento, 'consultar')"
      />
      <button
        type="button"
        class="rounded-btn border border-feedback-danger px-4 py-2 text-sm font-bold text-feedback-danger focus-visible:outline-2"
        @click="carregarGaleria"
      >
        Tentar novamente
      </button>
    </div>

    <template v-else>
      <p
        v-if="erroOperacao"
        role="alert"
        class="mb-4 rounded-btn border border-feedback-danger-border bg-feedback-danger-bg p-3 text-sm text-feedback-danger"
      >
        {{ erroOperacao }}
      </p>
      <p
        v-if="mensagemSucesso"
        role="status"
        class="mb-4 rounded-btn border border-amma-green-light bg-amma-green-lighter p-3 text-sm text-amma-green-dark"
      >
        {{ mensagemSucesso }}
      </p>
      <ul v-if="mensagensSelecao.length" class="mb-4 space-y-1" role="alert">
        <li
          v-for="(mensagem, index) in mensagensSelecao"
          :key="index"
          class="text-sm text-feedback-danger"
        >
          {{ mensagem }}
        </li>
      </ul>

      <div v-if="fotografias.length" class="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
        <article
          v-for="foto in fotografias"
          :key="foto.id"
          class="min-w-0 overflow-hidden rounded-btn border border-amma-green-light bg-amma-green-lighter/30"
        >
          <img
            v-if="urlsFotografias.has(foto.id)"
            :src="urlsFotografias.get(foto.id)"
            :alt="`Fotografia ${foto.id} da árvore ${arvoreId}`"
            class="aspect-[4/3] w-full object-cover"
            loading="lazy"
          />
          <div
            v-else
            class="flex aspect-[4/3] items-center justify-center p-4 text-center text-sm text-grey-text"
          >
            {{ errosConteudo.get(foto.id) ?? 'Pré-visualização indisponível.' }}
          </div>
          <div class="flex items-center justify-between gap-2 p-3">
            <div class="min-w-0 text-xs text-grey-text">
              <p class="truncate font-semibold">Fotografia {{ foto.id }}</p>
              <p>{{ foto.contentType }} · {{ formatarTamanho(foto.sizeBytes) }}</p>
            </div>
            <button
              v-if="permitirRemocao"
              type="button"
              class="shrink-0 rounded-btn border border-feedback-danger px-3 py-2 text-xs font-bold text-feedback-danger hover:bg-feedback-danger-bg focus-visible:outline-2"
              :disabled="removendoId !== null || enviando"
              :aria-label="`Remover fotografia ${foto.id}`"
              @click="removerFotografia(foto)"
            >
              {{ removendoId === foto.id ? 'Removendo…' : 'Remover' }}
            </button>
          </div>
        </article>
      </div>
      <p v-else-if="arquivosPendentes.length === 0" class="text-sm text-grey-text">
        Nenhuma fotografia associada a esta árvore.
      </p>

      <div v-if="arquivosPendentes.length" class="mt-5 space-y-4">
        <h3 class="text-sm font-bold text-amma-green-dark">Pré-visualização antes do envio</h3>
        <ul class="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
          <li
            v-for="(pendente, index) in arquivosPendentes"
            :key="`${pendente.arquivo.name}-${pendente.arquivo.lastModified}-${index}`"
            class="min-w-0 overflow-hidden rounded-btn border border-dashed border-amma-green-light"
          >
            <img
              :src="pendente.url"
              :alt="`Pré-visualização de ${pendente.arquivo.name}`"
              class="aspect-[4/3] w-full object-cover"
            />
            <div class="flex items-center justify-between gap-2 p-3">
              <p class="min-w-0 truncate text-xs text-grey-text">
                {{ pendente.arquivo.name }} · {{ formatarTamanho(pendente.arquivo.size) }}
              </p>
              <button
                type="button"
                class="shrink-0 rounded-btn border border-amma-green-light px-3 py-2 text-xs font-bold text-grey-text hover:bg-amma-green-lighter focus-visible:outline-2"
                :disabled="enviando"
                :aria-label="`Remover ${pendente.arquivo.name} da seleção`"
                @click="removerPendente(index)"
              >
                Remover da seleção
              </button>
            </div>
          </li>
        </ul>
        <button
          type="button"
          class="rounded-btn bg-amma-green px-5 py-3 text-sm font-bold text-white transition-colors hover:bg-amma-green-dark focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-amma-green disabled:cursor-wait disabled:opacity-60"
          :disabled="enviando || carregando"
          @click="enviarSelecionadas"
        >
          {{
            enviando
              ? 'Enviando…'
              : `Enviar ${arquivosPendentes.length} ${arquivosPendentes.length === 1 ? 'fotografia' : 'fotografias'}`
          }}
        </button>
      </div>
      <p v-if="permitirUpload && vagasRestantes === 0" class="mt-4 text-sm text-grey-text">
        O limite de três fotografias foi atingido.
      </p>
    </template>
  </section>
</template>
