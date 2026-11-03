<script setup lang="ts">
import { computed } from 'vue'
import { RouterLink } from 'vue-router'

export interface BreadcrumbItem {
  label: string
  routeName?: string
  params?: Record<string, string | number>
  icon?: string
}

const props = withDefaults(
  defineProps<{
    items: BreadcrumbItem[]
    showBack?: boolean
  }>(),
  {
    showBack: true,
  },
)

const destinoAnterior = computed(() =>
  [...props.items]
    .slice(0, -1)
    .reverse()
    .find((item) => item.routeName),
)
</script>

<template>
  <nav
    aria-label="Caminho de navegação"
    class="sticky top-0 z-20 flex w-full items-center justify-between border-b border-amma-green-light/40 bg-white px-6 py-2.5 shadow-xs md:px-10"
  >
    <ol class="flex flex-wrap items-center gap-1 text-xs md:text-sm">
      <template v-for="(item, index) in items" :key="index">
        <li class="flex items-center gap-1">
          <RouterLink
            v-if="item.routeName && index < items.length - 1"
            :to="{ name: item.routeName, params: item.params }"
            class="flex items-center transition-colors hover:text-amma-green focus-visible:rounded-sm focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-amma-green"
            :class="item.icon ? 'text-grey-text' : 'font-semibold text-grey-text'"
            :aria-label="item.icon ? item.label : undefined"
            :title="item.icon ? item.label : undefined"
          >
            <span
              v-if="item.icon"
              aria-hidden="true"
              class="material-symbols-outlined text-[18px]"
              >{{ item.icon }}</span
            >
            <span v-else>{{ item.label }}</span>
          </RouterLink>
          <span
            v-else
            :aria-current="index === items.length - 1 ? 'page' : undefined"
            :class="[
              'flex items-center',
              index === items.length - 1 ? 'font-semibold text-amma-green-dark' : 'text-grey-text',
            ]"
            :title="item.icon ? item.label : undefined"
          >
            <span
              v-if="item.icon"
              aria-hidden="true"
              class="material-symbols-outlined text-[18px]"
              >{{ item.icon }}</span
            >
            <span v-else>{{ item.label }}</span>
          </span>
          <span
            v-if="index < items.length - 1"
            aria-hidden="true"
            class="material-symbols-outlined mx-0.5 text-[16px] text-grey-text/40"
            >chevron_right</span
          >
        </li>
      </template>
    </ol>

    <RouterLink
      v-if="showBack && destinoAnterior?.routeName"
      :to="{ name: destinoAnterior.routeName, params: destinoAnterior.params }"
      :aria-label="`Voltar para ${destinoAnterior.label}`"
      class="flex cursor-pointer items-center gap-1 rounded-[4px_14px_4px_14px] border border-gray-200 bg-white px-2.5 py-1 text-xs font-bold text-grey-text shadow-xs transition-colors hover:border-amma-green hover:bg-amma-green-lighter/10 hover:text-amma-green focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-amma-green"
    >
      <span aria-hidden="true" class="material-symbols-outlined text-[16px]">arrow_back</span>
      Voltar
    </RouterLink>
  </nav>
</template>
