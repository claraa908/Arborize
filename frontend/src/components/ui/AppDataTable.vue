<script setup lang="ts" generic="T extends object">
import { ref } from 'vue'

export interface TablePagination {
  page: number
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

defineEmits<{
  (e: 'update:search', value: string): void
  (e: 'action-click'): void
  (e: 'prev-page'): void
  (e: 'next-page'): void
}>()

const currentView = ref<'table' | 'grid'>(props.defaultView)
function valor(item: T, key: string): unknown {
  return (item as Record<string, unknown>)[key]
}
function chave(item: T, index: number): string | number {
  const value = valor(item, props.itemKey)
  return typeof value === 'string' || typeof value === 'number' ? value : index
}
</script>

<template>
  <div class="flex flex-col gap-6 w-full">
    <div
      v-if="showSearch || showSummary || allowViewToggle || buttonLabel || $slots['header-actions']"
      class="flex flex-col lg:flex-row justify-between items-stretch lg:items-center gap-4"
    >
      <div v-if="showSearch" class="relative w-full lg:w-96">
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
      <div v-else></div>

      <div
        class="flex items-center flex-wrap justify-between lg:justify-end gap-3 w-full lg:w-auto"
      >
        <div
          v-if="showSummary"
          class="flex items-center gap-2 bg-white border border-amma-green-light/70 px-3 py-1.5 rounded-btn shadow-xs"
        >
          <span class="text-xs font-semibold text-grey-text/70 whitespace-nowrap">
            {{ pagination?.totalElements ?? items.length }} registros
          </span>
          <span v-if="pagination" class="text-xs text-grey-text">
            Página {{ pagination.totalPages ? pagination.page + 1 : 0 }} de
            {{ pagination.totalPages }}
          </span>
          <div class="flex items-center ml-1">
            <button
              type="button"
              @click="$emit('prev-page')"
              class="w-6 h-6 flex items-center justify-center rounded-l border border-amma-green-light bg-amma-bg text-grey-text hover:text-amma-green hover:bg-amma-green-lighter/30 disabled:opacity-40 transition-colors cursor-pointer"
              :disabled="!pagination || loading || pagination.page <= 0"
              aria-label="Página anterior"
              title="Página anterior"
            >
              <span class="material-symbols-outlined text-[15px]">chevron_left</span>
            </button>
            <button
              type="button"
              @click="$emit('next-page')"
              class="w-6 h-6 flex items-center justify-center rounded-r border-y border-r border-amma-green-light bg-amma-bg text-grey-text hover:text-amma-green hover:bg-amma-green-lighter/30 disabled:opacity-40 transition-colors cursor-pointer"
              :disabled="!pagination || loading || pagination.page + 1 >= pagination.totalPages"
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

    <div v-if="currentView === 'grid'" class="flex flex-col gap-6">
      <div v-if="items.length > 0" class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-5">
        <div
          v-for="(item, index) in items"
          :key="chave(item, index)"
          class="bg-white rounded-box border border-amma-green-light p-5 shadow-xs flex flex-col justify-between hover:shadow-md transition-shadow"
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
      <div class="overflow-x-auto">
        <table class="w-full text-left border-collapse">
          <caption v-if="caption" class="sr-only">
            {{
              caption
            }}
          </caption>
          <thead>
            <tr class="bg-amma-bg border-b border-amma-green-light text-amma-green-dark text-sm">
              <th
                v-for="col in columns"
                :key="col.key"
                :class="[
                  'py-4 px-6 font-bold',
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

          <tbody v-if="items.length > 0">
            <tr
              v-for="(item, index) in items"
              :key="chave(item, index)"
              class="border-b border-amma-green-light/50 hover:bg-amma-green-lighter/30 transition-colors"
              :class="{
                'bg-amma-green-lighter': selectedId != null && valor(item, itemKey) === selectedId,
              }"
            >
              <td
                v-for="col in columns"
                :key="col.key"
                :class="[
                  'py-4 px-6',
                  col.align === 'center'
                    ? 'text-center'
                    : col.align === 'right'
                      ? 'text-right'
                      : 'text-left',
                ]"
              >
                <slot :name="col.key" :item="item" :value="valor(item, col.key)">
                  <span class="text-sm font-medium text-grey-text">{{ valor(item, col.key) }}</span>
                </slot>
              </td>
            </tr>
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
  </div>
</template>
