<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { inventarioService } from '@/services/inventarioService'
import type { FotoCapaResponseDTO } from '@/types/inventario'

const props = withDefaults(
  defineProps<{
    arvoreId: number
    fotoCapa: FotoCapaResponseDTO | null
    carregarImediatamente?: boolean
  }>(),
  { carregarImediatamente: false },
)

type Estado = 'sem-fotografia' | 'aguardando' | 'carregando' | 'disponivel' | 'indisponivel'

const raiz = ref<HTMLElement | null>(null)
const url = ref<string | null>(null)
const estado = ref<Estado>('sem-fotografia')
let observador: IntersectionObserver | null = null
let requisicaoAtual = 0

function revogarUrl() {
  if (!url.value) return
  URL.revokeObjectURL(url.value)
  url.value = null
}

function pararObservacao() {
  observador?.disconnect()
  observador = null
}

async function carregar() {
  if (!props.fotoCapa || estado.value === 'carregando' || estado.value === 'disponivel') return
  pararObservacao()
  estado.value = 'carregando'
  const idDaRequisicao = ++requisicaoAtual
  try {
    const conteudo = await inventarioService.recuperarFoto(props.arvoreId, props.fotoCapa.id, {
      suppressGlobalErrorToast: true,
    })
    const novaUrl = URL.createObjectURL(conteudo)
    if (idDaRequisicao !== requisicaoAtual) {
      URL.revokeObjectURL(novaUrl)
      return
    }
    url.value = novaUrl
    estado.value = 'disponivel'
  } catch {
    if (idDaRequisicao === requisicaoAtual) estado.value = 'indisponivel'
  }
}

function observarVisibilidade() {
  if (!props.fotoCapa || props.carregarImediatamente || estado.value !== 'aguardando') return
  if (typeof IntersectionObserver === 'undefined') {
    estado.value = 'indisponivel'
    return
  }
  if (!raiz.value) return
  observador = new IntersectionObserver(
    (entradas) => {
      if (entradas.some((entrada) => entrada.isIntersecting)) void carregar()
    },
    { rootMargin: '0px' },
  )
  observador.observe(raiz.value)
}

function marcarIndisponivel() {
  estado.value = 'indisponivel'
  revogarUrl()
}

watch(
  () => [props.arvoreId, props.fotoCapa?.id, props.carregarImediatamente] as const,
  () => {
    requisicaoAtual++
    pararObservacao()
    revogarUrl()
    if (!props.fotoCapa) {
      estado.value = 'sem-fotografia'
      return
    }
    estado.value = 'aguardando'
    if (props.carregarImediatamente) void carregar()
    else observarVisibilidade()
  },
  { immediate: true, flush: 'post' },
)

onMounted(observarVisibilidade)
onBeforeUnmount(() => {
  requisicaoAtual++
  pararObservacao()
  revogarUrl()
})
</script>

<template>
  <figure
    ref="raiz"
    class="flex h-16 w-20 shrink-0 items-center justify-center overflow-hidden rounded-btn border border-amma-green-light bg-amma-green-lighter"
    :aria-label="estado === 'sem-fotografia' ? 'Sem fotografia' : 'Fotografia da árvore'"
  >
    <img
      v-if="estado === 'disponivel' && url"
      :src="url"
      :alt="`Fotografia da árvore ${arvoreId}`"
      class="h-full w-full object-cover"
      @error="marcarIndisponivel"
    />
    <div v-else-if="estado === 'sem-fotografia'" class="flex flex-col items-center text-amma-green-dark">
      <svg aria-hidden="true" viewBox="0 0 48 36" class="mb-0.5 h-7 w-9" fill="none">
        <rect x="3" y="3" width="42" height="30" rx="4" stroke="currentColor" stroke-width="2" />
        <circle cx="16" cy="13" r="3" fill="currentColor" />
        <path d="m6 29 11-10 7 6 6-5 12 9" stroke="currentColor" stroke-width="2" />
      </svg>
      <span class="text-[10px] font-semibold leading-tight">Sem fotografia</span>
    </div>
    <span v-else-if="estado === 'carregando'" role="status" class="px-1 text-center text-[10px] text-grey-text">
      Carregando…
    </span>
    <span v-else-if="estado === 'indisponivel'" role="status" class="px-1 text-center text-[10px] leading-tight text-grey-text">
      Fotografia indisponível
    </span>
    <span v-else class="text-[10px] text-grey-text" aria-hidden="true">Fotografia</span>
  </figure>
</template>
