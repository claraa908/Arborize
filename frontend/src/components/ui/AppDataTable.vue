<script setup lang="ts" generic="T extends object">
import { computed, ref, watch } from 'vue'

export interface TablePagination {
  page: number
  size?: number
  totalElements: number
  totalPages: number
}

export interface TableColumn {
  key: string
  label: string
  align?: 'left' | 'center' | 'right'
  width?: string
  class?: string
}

const props = withDefaults(
  defineProps<{
    columns: TableColumn[]
    items: T[]
    pagination?: TablePagination
    pageSize?: number
    maxBodyHeightClass?: string
    fixedLayout?: boolean
    loading?: boolean
    selectedId?: string | number | null
    caption?: string
    itemKey?: string
    search?: string
    searchPlaceholder?: string
    buttonLabel?: string
    buttonIcon?: string
    emptyMessage?: string
    showSearch?: boolean
    showSummary?: boolean
    allowViewToggle?: boolean
    defaultView?: 'table' | 'grid'
  }>(),
  {
    pageSize: 10,
    maxBodyHeightClass: 'max-h-[460px]',
    fixedLayout: false,
    loading: false,
    selectedId: null,
    itemKey: 'id',
    search: '',
    searchPlaceholder: 'Pesquisar...',
    buttonIcon: 'add',
    emptyMessage: 'Nenhum registo encontrado.',
    showSearch: true,
    showSummary: true,
    allowViewToggle: true,
    defaultView: 'grid',
  },
)

const emit = defineEmits<{
  (e: 'update:search', value: string): void
  (e: 'action-click'): void
  (e: 'prev-page'): void
  (e: 'next-page'): void
  (e: 'page-change', page: number): void
}>()

const currentView = ref<'table' | 'grid'>(props.defaultView)
const localPage = ref(0)

const isServerPaginated = computed(() => Boolean(props.pagination))
const effectivePageSize = computed(() => props.pagination?.size ?? props.pageSize ?? 10)
const effectivePage = computed(() => props.pagination?.page ?? localPage.value)
const effectiveTotalElements = computed(
  () => props.pagination?.totalElements ?? props.items.length,
)
const effectiveTotalPages = computed(() => {
  if (props.pagination) return props.pagination.totalPages
  return props.items.length === 0
    ? 0
    : Math.max(1, Math.ceil(props.items.length / effectivePageSize.value))
})

const displayedItems = computed(() => {
  if (isServerPaginated.value) return props.items
  const start = localPage.value * effectivePageSize.value
  return props.items.slice(start, start + effectivePageSize.value)
})

watch(
  () => props.search,
  () => {
    localPage.value = 0
  },
)

watch(
  () => [props.items.length, effectivePageSize.value],
  () => {
    if (isServerPaginated.value) return
    const maxPage = Math.max(0, effectiveTotalPages.value - 1)
    if (localPage.value > maxPage) localPage.value = 0
  },
)

function goPrevPage() {
  if (props.loading || effectivePage.value <= 0) return
  if (isServerPaginated.value) {
    emit('prev-page')
    emit('page-change', effectivePage.value - 1)
    return
  }
  localPage.value = Math.max(0, localPage.value - 1)
}

function goNextPage() {
  if (props.loading || effectivePage.value + 1 >= effectiveTotalPages.value) return
  if (isServerPaginated.value) {
    emit('next-page')
    emit('page-change', effectivePage.value + 1)
    return
  }
  localPage.value = Math.min(effectiveTotalPages.value - 1, localPage.value + 1)
}

function valor(item: T, key: string): unknown {
  return (item as Record<string, unknown>)[key]
}
function chave(item: T, index: number): string | number {
  const value = valor(item, props.itemKey)
  return typeof value === 'string' || typeof value === 'number' ? value : index
}
</script>

<template>
  <div class="flex min-w-0 w-full flex-col gap-6">
    <div
      v-if="showSearch || showSummary || allowViewToggle || buttonLabel || $slots['header-actions']"
      class="flex flex-wrap items-center justify-between gap-3"
    >
      <div v-if="showSearch" class="relative w-full min-w-0 sm:min-w-60 sm:flex-1 lg:max-w-96">
        <span
          class="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-grey-text/60"
          >search</span
        >
        <input
          :value="search"
          @input="$emit('update:search', ($event.target as HTMLInputElement).value)"
          type="text"
          :placeholder="searchPlaceholder"
          class="w-full bg-white focus:bg-amma-green-lighter border-2 border-amma-green-light focus:border-amma-green rounded-btn py-2.5 pl-10 pr-4 text-grey-text outline-none transition-all text-sm font-medium"
        />
      </div>
      <div
        class="flex items-center flex-wrap justify-between lg:justify-end gap-3 w-full lg:w-auto"
      >
        <div
          v-if="showSummary"
          class="flex items-center gap-2 bg-white border border-amma-green-light/70 px-3 py-1.5 rounded-btn shadow-xs"
        >
          <span class="text-xs font-semibold text-grey-text/70 whitespace-nowrap">
            {{ effectiveTotalElements }} registros
          </span>
          <span class="text-xs text-grey-text whitespace-nowrap">
            Página {{ effectiveTotalPages ? effectivePage + 1 : 0 }} de
            {{ effectiveTotalPages }}
          </span>
          <div class="flex items-center ml-1">
            <button
              type="button"
              @click="goPrevPage"
              class="w-6 h-6 flex items-center justify-center rounded-l border border-amma-green-light bg-amma-bg text-grey-text hover:text-amma-green hover:bg-amma-green-lighter/30 disabled:opacity-40 transition-colors cursor-pointer"
              :disabled="loading || effectivePage <= 0"
              aria-label="Página anterior"
              title="Página anterior"
            >
              <span class="material-symbols-outlined text-[15px]">chevron_left</span>
            </button>
            <button
              type="button"
              @click="goNextPage"
              class="w-6 h-6 flex items-center justify-center rounded-r border-y border-r border-amma-green-light bg-amma-bg text-grey-text hover:text-amma-green hover:bg-amma-green-lighter/30 disabled:opacity-40 transition-colors cursor-pointer"
              :disabled="loading || effectivePage + 1 >= effectiveTotalPages"
              aria-label="Próxima página"
              title="Próxima página"
            >
              <span class="material-symbols-outlined text-[15px]">chevron_right</span>
            </button>
          </div>
        </div>

        <div
          v-if="allowViewToggle"
          class="flex items-center bg-amma-bg p-1 rounded-btn-l-small border border-amma-green-light/60"
        >
          <button
            type="button"
            @click="currentView = 'grid'"
            :class="[
              'p-1.5 rounded-btn-l-small transition-all cursor-pointer flex items-center justify-center',
              currentView === 'grid'
                ? 'bg-amma-green text-white shadow-xs'
                : 'text-grey-text hover:text-amma-green',
            ]"
            aria-label="Visualização em grade"
            title="Visualização em Grade"
          >
            <span class="material-symbols-outlined text-[18px]">grid_view</span>
          </button>

          <button
            type="button"
            @click="currentView = 'table'"
            :class="[
              'p-1.5 rounded-btn-l-small transition-all cursor-pointer flex items-center justify-center',
              currentView === 'table'
                ? 'bg-amma-green text-white shadow-xs'
                : 'text-grey-text hover:text-amma-green',
            ]"
            aria-label="Visualização em tabela"
            title="Visualização em Tabela"
          >
            <span class="material-symbols-outlined text-[18px]">table_rows</span>
          </button>
        </div>

        <slot name="header-actions">
          <button
            v-if="buttonLabel"
            type="button"
            @click="$emit('action-click')"
            class="bg-white hover:bg-amma-green hover:text-white text-amma-green font-bold py-2 px-5 rounded-btn text-sm border-2 border-amma-green outline-none efeito-zoom flex items-center justify-center gap-1.5 cursor-pointer ml-auto lg:ml-0"
          >
            <span v-if="buttonIcon" class="material-symbols-outlined text-[18px]">{{
              buttonIcon
            }}</span>
            {{ buttonLabel }}
          </button>
        </slot>
      </div>
    </div>

    <div v-if="currentView === 'grid'" class="flex min-w-0 flex-col gap-6">
      <div
        v-if="displayedItems.length > 0"
        :class="['grid min-w-0 grid-cols-1 gap-5 overflow-y-auto pr-1 sm:grid-cols-2 lg:grid-cols-3', maxBodyHeightClass]"
      >
        <div
          v-for="(item, index) in displayedItems"
          :key="chave(item, index)"
          class="flex min-w-0 flex-col justify-between rounded-box border border-amma-green-light bg-white p-5 shadow-xs transition-shadow hover:shadow-md"
        >
          <slot name="card" :item="item">
            <div class="flex flex-col gap-2">
              <div v-for="col in columns" :key="col.key" class="flex flex-col">
                <span class="text-xs font-semibold text-grey-text/60 uppercase">{{
                  col.label
                }}</span>
                <slot :name="col.key" :item="item" :value="valor(item, col.key)">
                  <span class="text-sm font-medium text-grey-text">{{ valor(item, col.key) }}</span>
                </slot>
              </div>
            </div>
          </slot>
          <div
            v-if="$slots['row-actions']"
            class="mt-4 border-t border-amma-green-light/60 pt-3"
          >
            <slot name="row-actions" :item="item" />
          </div>
        </div>
      </div>

      <div v-else class="bg-white rounded-btn border border-amma-green-light p-12 text-center">
        <span class="material-symbols-outlined text-4xl text-grey-text/30 mb-2">find_in_page</span>
        <p class="text-grey-text/60 font-medium">{{ emptyMessage }}</p>
      </div>
    </div>

    <div
      v-else
      class="bg-white rounded-btn shadow-sm border border-amma-green-light overflow-hidden"
    >
      <div :class="['overflow-auto', maxBodyHeightClass]">
        <table :class="['w-full text-left border-collapse', fixedLayout ? 'lg:table-fixed' : '']">
          <caption v-if="caption" class="sr-only">
            {{
              caption
            }}
          </caption>
          <thead class="sticky top-0 z-10 bg-amma-bg shadow-[inset_0_-1px_0_0_var(--color-amma-green-light)]">
            <tr class="text-amma-green-dark text-sm">
              <th
                v-for="col in columns"
                :key="col.key"
                :class="[
                  'bg-amma-bg py-3.5 px-4 font-bold whitespace-nowrap',
                  col.align === 'center'
                    ? 'text-center'
                    : col.align === 'right'
                      ? 'text-right'
                      : 'text-left',
                  col.class || '',
                ]"
                :style="col.width ? { width: col.width } : {}"
              >
                {{ col.label }}
              </th>
            </tr>
          </thead>

          <tbody v-if="displayedItems.length > 0">
            <template v-for="(item, index) in displayedItems" :key="chave(item, index)">
              <tr
                class="hover:bg-amma-green-lighter/30 transition-colors"
                :class="[
                  $slots['row-actions'] ? '' : 'border-b border-amma-green-light/50',
                  { 'bg-amma-green-lighter': selectedId != null && valor(item, itemKey) === selectedId },
                ]"
              >
                <td
                  v-for="col in columns"
                  :key="col.key"
                  :class="[
                    $slots['row-actions'] ? 'pt-3.5 pb-2 px-4' : 'py-3.5 px-4',
                    col.align === 'center'
                      ? 'text-center'
                      : col.align === 'right'
                        ? 'text-right'
                        : 'text-left',
                    fixedLayout ? 'overflow-hidden' : '',
                    col.class || '',
                  ]"
                >
                  <slot :name="col.key" :item="item" :value="valor(item, col.key)">
                    <span class="text-sm font-medium text-grey-text">{{ valor(item, col.key) }}</span>
                  </slot>
                </td>
              </tr>
              <tr
                v-if="$slots['row-actions']"
                class="border-b border-amma-green-light/60 bg-amma-bg/25"
              >
                <td :colspan="columns.length" class="px-4 pb-3 pt-1">
                  <slot name="row-actions" :item="item" />
                </td>
              </tr>
            </template>
          </tbody>

          <tbody v-else>
            <tr>
              <td :colspan="columns.length" class="py-12 px-6 text-center">
                <span class="material-symbols-outlined text-4xl text-grey-text/30 mb-2"
                  >find_in_page</span
                >
                <p class="text-grey-text/60 font-medium">{{ emptyMessage }}</p>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <div
      v-if="effectiveTotalPages > 1"
      class="flex flex-wrap items-center justify-between gap-3 rounded-btn border border-amma-green-light/70 bg-white px-4 py-2.5 text-xs text-grey-text shadow-xs"
    >
      <span class="font-semibold">
        {{ effectiveTotalElements }} registros · Página {{ effectivePage + 1 }} de {{ effectiveTotalPages }}
      </span>
      <div class="flex items-center gap-2">
        <button
          type="button"
          class="inline-flex min-h-9 items-center gap-1 rounded-btn border border-amma-green-light bg-white px-3 py-1.5 font-bold text-amma-green-dark hover:bg-amma-green-lighter disabled:cursor-not-allowed disabled:opacity-40"
          :disabled="loading || effectivePage <= 0"
          @click="goPrevPage"
        >
          <span class="material-symbols-outlined text-[16px]" aria-hidden="true">chevron_left</span>
          Anterior
        </button>
        <button
          type="button"
          class="inline-flex min-h-9 items-center gap-1 rounded-btn border border-amma-green-light bg-white px-3 py-1.5 font-bold text-amma-green-dark hover:bg-amma-green-lighter disabled:cursor-not-allowed disabled:opacity-40"
          :disabled="loading || effectivePage + 1 >= effectiveTotalPages"
          @click="goNextPage"
        >
          Próxima
          <span class="material-symbols-outlined text-[16px]" aria-hidden="true">chevron_right</span>
        </button>
      </div>
    </div>
  </div>
</template>
