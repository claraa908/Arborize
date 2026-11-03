<script setup lang="ts">
import { onBeforeUnmount, ref, shallowRef, watch } from 'vue'
import {
  LIMITE_FOTOGRAFIAS_ARVORE,
  limiteFotografiasExcedido,
  validarArquivoFotografia,
} from './arvoreFotografias'

interface PreviaFotografia {
  arquivo: File
  url: string
}

const props = withDefaults(
  defineProps<{
    modelValue: File[]
    quantidadeExistente?: number
    desativado?: boolean
  }>(),
  { quantidadeExistente: 0, desativado: false },
)

const emit = defineEmits<{ 'update:modelValue': [arquivos: File[]] }>()
const previas = shallowRef<PreviaFotografia[]>([])
const mensagens = ref<string[]>([])
const seletor = ref<HTMLInputElement | null>(null)

function liberar(url: string) {
  URL.revokeObjectURL(url)
}

function sincronizarPrevias(arquivos: File[]): File[] {
  const atuais = new Map(previas.value.map((previa) => [previa.arquivo, previa]))
  const novas: PreviaFotografia[] = []
  const aceitos: File[] = []
  const usados = new Set<File>()

  for (const arquivo of arquivos) {
    if (usados.has(arquivo)) continue
    usados.add(arquivo)
    const previaExistente = atuais.get(arquivo)
    if (previaExistente) {
      novas.push(previaExistente)
      aceitos.push(arquivo)
      atuais.delete(arquivo)
      continue
    }
    try {
      novas.push({ arquivo, url: URL.createObjectURL(arquivo) })
      aceitos.push(arquivo)
    } catch {
      mensagens.value = [
        ...mensagens.value,
        `${arquivo.name}: não foi possível preparar a pré-visualização.`,
      ]
    }
  }

  for (const previa of atuais.values()) liberar(previa.url)
  previas.value = novas
  return aceitos
}

function publicar(arquivos: File[]) {
  emit('update:modelValue', arquivos)
}

function selecionarArquivos(evento: Event) {
  const input = evento.target as HTMLInputElement
  const aceitos = [...props.modelValue]
  const avisos: string[] = []

  for (const arquivo of Array.from(input.files ?? [])) {
    if (limiteFotografiasExcedido(props.quantidadeExistente + aceitos.length)) {
      avisos.push(`Cada árvore pode possuir até ${LIMITE_FOTOGRAFIAS_ARVORE} fotografias.`)
      break
    }
    const erro = validarArquivoFotografia(arquivo)
    if (erro) {
      avisos.push(erro)
      continue
    }
    aceitos.push(arquivo)
  }

  mensagens.value = avisos
  publicar(sincronizarPrevias(aceitos))
  input.value = ''
}

function removerArquivo(arquivo: File) {
  mensagens.value = []
  publicar(props.modelValue.filter((item) => item !== arquivo))
}

watch(() => props.modelValue, sincronizarPrevias, { immediate: true })

onBeforeUnmount(() => {
  for (const previa of previas.value) liberar(previa.url)
  previas.value = []
})
</script>

<template>
  <section
    aria-labelledby="fotos-cadastro-titulo"
    class="min-w-0 rounded-box border border-amma-green-light bg-white p-4 shadow-sm md:p-5"
  >
    <div class="flex flex-wrap items-center justify-between gap-3">
      <div>
        <h2 id="fotos-cadastro-titulo" class="text-lg font-bold text-amma-green-dark">
          Fotografias <span class="text-sm font-normal">(opcionais)</span>
        </h2>
        <p class="mt-1 text-sm text-grey-text">
          {{ quantidadeExistente + modelValue.length }} de
          {{ LIMITE_FOTOGRAFIAS_ARVORE }} selecionadas. JPG, PNG ou WebP; até 2 MB cada.
        </p>
      </div>
      <label
        v-if="quantidadeExistente + modelValue.length < LIMITE_FOTOGRAFIAS_ARVORE"
        for="arvore-fotografias-cadastro-seletor"
        class="inline-flex cursor-pointer items-center justify-center gap-2 rounded-btn border border-amma-green px-4 py-2 text-sm font-bold text-amma-green-dark transition-colors hover:bg-amma-green-lighter focus-within:outline-2 focus-within:outline-amma-green"
        :class="{ 'pointer-events-none opacity-60': desativado }"
      >
        <span aria-hidden="true" class="material-symbols-outlined text-[18px]"
          >add_photo_alternate</span
        >
        Selecionar imagens
        <input
          id="arvore-fotografias-cadastro-seletor"
          ref="seletor"
          class="sr-only"
          type="file"
          accept="image/jpeg,image/png,image/webp,.jpg,.jpeg,.png,.webp"
          multiple
          :disabled="desativado"
          @change="selecionarArquivos"
        />
      </label>
    </div>

    <ul v-if="mensagens.length" class="mt-3 space-y-1" role="alert">
      <li
        v-for="(mensagem, indice) in mensagens"
        :key="indice"
        class="text-sm text-feedback-danger"
      >
        {{ mensagem }}
      </li>
    </ul>

    <p v-if="previas.length === 0" class="mt-3 text-sm text-grey-text">
      Selecione imagens para pré-visualizá-las antes de cadastrar. Você também poderá adicionar
      fotografias depois, pela ficha técnica.
    </p>

    <ul v-else class="mt-4 grid min-w-0 grid-cols-1 gap-3 sm:grid-cols-2">
      <li
        v-for="previa in previas"
        :key="`${previa.arquivo.name}-${previa.arquivo.lastModified}-${previa.url}`"
        class="min-w-0 overflow-hidden rounded-btn border border-dashed border-amma-green-light"
      >
        <img
          :src="previa.url"
          :alt="`Pré-visualização de ${previa.arquivo.name}`"
          class="aspect-[4/3] w-full object-cover"
        />
        <div class="flex min-w-0 items-center justify-between gap-2 p-3">
          <p class="min-w-0 truncate text-xs text-grey-text">
            {{ previa.arquivo.name }} · {{ Math.max(1, Math.ceil(previa.arquivo.size / 1024)) }} KB
          </p>
          <button
            type="button"
            class="shrink-0 rounded-btn border border-amma-green-light px-3 py-2 text-xs font-bold text-grey-text hover:bg-amma-green-lighter focus-visible:outline-2 focus-visible:outline-amma-green"
            :disabled="desativado"
            :aria-label="`Remover ${previa.arquivo.name} da seleção`"
            @click="removerArquivo(previa.arquivo)"
          >
            Remover
          </button>
        </div>
      </li>
    </ul>
  </section>
</template>
