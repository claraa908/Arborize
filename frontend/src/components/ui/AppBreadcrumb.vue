<script setup lang="ts">
import { useRouter } from 'vue-router'

export interface BreadcrumbItem {
  label: string
  routeName?: string
  params?: Record<string, string | number>
  icon?: string
}

const props = withDefaults(defineProps<{
  items: BreadcrumbItem[]
  showBack?: boolean
}>(), {
  showBack: true
})

const router = useRouter()

const navigateTo = (item: BreadcrumbItem) => {
  if (item.routeName) {
    router.push({ name: item.routeName, params: item.params })
  }
}

const goBack = () => {
  router.back()
}
</script>

<template>
  <div class="sticky top-0 z-20 bg-white px-6 md:px-10 py-2.5 flex items-center justify-between w-full border-b border-amma-green-light/40 shadow-xs">
    
    <div class="flex items-center flex-wrap gap-1 text-xs md:text-sm">
      <template v-for="(item, index) in items" :key="index">
        <span 
          @click="navigateTo(item)"
          :class="[
            'flex items-center transition-colors',
            item.routeName 
              ? 'text-grey-text hover:text-amma-green cursor-pointer' 
              : 'text-amma-green-dark cursor-default',
            item.icon ? '' : 'font-semibold'
          ]"
          :title="item.icon ? item.label : ''"
        >
          <span v-if="item.icon" class="material-symbols-outlined text-[18px]">{{ item.icon }}</span>
          <span v-else>{{ item.label }}</span>
        </span>
        
        <span 
          v-if="index < items.length - 1" 
          class="material-symbols-outlined text-grey-text/40 text-[16px] mx-0.5"
        >
          chevron_right
        </span>
      </template>
    </div>

    <button 
      v-if="showBack"
      @click="goBack" 
      class="flex items-center gap-1 text-grey-text hover:text-amma-green hover:border-amma-green bg-white hover:bg-amma-green-lighter/10 transition-colors font-bold text-xs outline-none border border-gray-200 rounded-[4px_14px_4px_14px] px-2.5 py-1 shadow-xs efeito-zoom cursor-pointer"
    >
      <span class="material-symbols-outlined text-[16px]">arrow_back</span>
      Voltar
    </button>
    
  </div>
</template>