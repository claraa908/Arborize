<script setup lang="ts">
import { ArrowRight, ChevronLeft, ChevronRight } from 'lucide-vue-next'
import { computed, nextTick, onMounted, onUnmounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { caatingaTips, tipCategories, type TipCategory } from '@/data/homeContent'

const track = ref<HTMLElement | null>(null)
const category = ref<TipCategory | null>(null)
const atStart = ref(true)
const atEnd = ref(true)
const tips = computed(() =>
  category.value === null
    ? caatingaTips
    : caatingaTips.filter((tip) => tip.category === category.value),
)
const tolerance = 2
let observer: ResizeObserver | undefined

function cardOffsets(element: HTMLElement) {
  const origin = element.getBoundingClientRect().left
  return Array.from(
    element.children,
    (card) => card.getBoundingClientRect().left - origin + element.scrollLeft,
  )
}

function updateScrollLimits() {
  const element = track.value
  if (!element) return
  const { scrollLeft, scrollWidth, clientWidth } = element
  atStart.value = scrollLeft <= tolerance
  atEnd.value = scrollWidth - clientWidth - scrollLeft <= tolerance
}

async function selectCategory(value: TipCategory) {
  category.value = value === category.value ? null : value
  await nextTick()
  if (track.value) track.value.scrollLeft = 0
  updateScrollLimits()
}

function move(direction: -1 | 1) {
  const element = track.value
  if (!element || (direction === -1 ? atStart.value : atEnd.value)) return
  const end = element.scrollWidth - element.clientWidth
  const offsets = cardOffsets(element)
  const target =
    direction === 1
      ? (offsets.find((offset) => offset > element.scrollLeft + tolerance) ?? end)
      : (offsets.reverse().find((offset) => offset < element.scrollLeft - tolerance) ?? 0)
  const reduced = window.matchMedia('(prefers-reduced-motion: reduce)').matches
  element.scrollTo({
    left: Math.min(end, Math.max(0, target)),
    behavior: reduced ? 'instant' : 'smooth',
  })
  if (reduced) updateScrollLimits()
}

function onKeydown(event: KeyboardEvent) {
  if (event.key !== 'ArrowLeft' && event.key !== 'ArrowRight') return
  event.preventDefault()
  move(event.key === 'ArrowLeft' ? -1 : 1)
}

onMounted(() => {
  updateScrollLimits()
  if (track.value) {
    observer = new ResizeObserver(updateScrollLimits)
    observer.observe(track.value)
  }
})
onUnmounted(() => observer?.disconnect())
</script>

<template>
  <section class="min-w-0" aria-labelledby="dicas-titulo" aria-roledescription="carrossel">
    <h2 id="dicas-titulo" class="text-2xl font-bold text-amma-green-dark sm:text-3xl">
      Dicas da Caatinga
    </h2>

    <div role="group" aria-label="Filtrar dicas por categoria" class="my-4 flex flex-wrap gap-2">
      <button
        v-for="(option, key) in tipCategories"
        :key="key"
        type="button"
        :aria-pressed="category === key"
        aria-controls="dicas-faixa"
        class="min-h-11 cursor-pointer rounded-btn border border-amma-green-light px-3 py-2 text-sm font-semibold text-amma-green-dark transition-colors hover:border-amma-green-dark focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-amma-green-dark aria-pressed:border-amma-green-dark aria-pressed:bg-amma-green-dark aria-pressed:text-white"
        @click="selectCategory(key)"
      >
        {{ option.label }}
      </button>
    </div>

    <div
      id="dicas-faixa"
      ref="track"
      tabindex="0"
      role="group"
      aria-label="Cartões de dicas"
      class="flex min-w-0 max-w-full snap-x snap-mandatory gap-4 overflow-x-auto overscroll-x-contain rounded-btn pb-3 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-amma-green-dark"
      @scroll.passive="updateScrollLimits"
      @keydown.self="onKeydown"
    >
      <article
        v-for="(tip, index) in tips"
        :key="tip.id"
        role="group"
        aria-roledescription="slide"
        :aria-label="`${index + 1} de ${tips.length}`"
        class="tip-card flex min-w-0 shrink-0 snap-start flex-col rounded-box border border-amma-green-light bg-amma-green-lighter p-4 sm:p-5"
      >
        <div class="flex items-center gap-2 text-amma-green-dark">
          <component
            :is="tipCategories[tip.category].icon"
            class="size-5 shrink-0"
            aria-hidden="true"
          />
          <h3 class="min-w-0 text-lg font-bold leading-tight [overflow-wrap:anywhere]">
            {{ tip.title }}
          </h3>
        </div>
        <p class="mt-2 grow text-sm leading-snug text-grey-text">{{ tip.text }}</p>
        <RouterLink
          v-if="tip.link"
          :to="{ name: tip.link.route }"
          class="mt-2 inline-flex min-h-11 items-center gap-1.5 self-start rounded-btn text-sm font-semibold text-amma-green-dark underline decoration-amma-green-light decoration-2 underline-offset-4 hover:decoration-current focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-amma-green-dark"
        >
          {{ tip.link.label }} <ArrowRight class="size-4 shrink-0" aria-hidden="true" />
        </RouterLink>
      </article>
    </div>

    <div class="mt-3 flex justify-center gap-2">
      <button
        type="button"
        aria-label="Anterior"
        aria-controls="dicas-faixa"
        :disabled="atStart"
        class="grid size-11 place-items-center rounded-btn border border-amma-green-light text-amma-green-dark enabled:hover:bg-amma-green-lighter focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-amma-green-dark disabled:opacity-45"
        @click="move(-1)"
      >
        <ChevronLeft class="size-5" aria-hidden="true" />
      </button>
      <button
        type="button"
        aria-label="Próximo"
        aria-controls="dicas-faixa"
        :disabled="atEnd"
        class="grid size-11 place-items-center rounded-btn border border-amma-green-light text-amma-green-dark enabled:hover:bg-amma-green-lighter focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-amma-green-dark disabled:opacity-45"
        @click="move(1)"
      >
        <ChevronRight class="size-5" aria-hidden="true" />
      </button>
    </div>
  </section>
</template>

<style scoped>
.tip-card {
  flex-basis: calc((100% - 1rem) / 1.5);
}

@media (min-width: 48rem) {
  .tip-card {
    flex-basis: calc((100% - 1rem) / 2);
  }
}

@media (min-width: 64rem) {
  .tip-card {
    flex-basis: calc((100% - 2rem) / 3);
  }
}
</style>
