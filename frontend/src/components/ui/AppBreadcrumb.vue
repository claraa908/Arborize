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
  <div class="sticky top-0 z-20 bg-white px-6 md:px-10 py-4 flex items-center justify-between w-full shadow-[0_4px_12px_-4px_rgba(0,0,0,0.05)]">
    
    <div class="flex items-center flex-wrap gap-1.5 text-sm">
      <template v-for="(item, index) in items" :key="index">
        <span 
          @click="navigateTo(item)"
          :class="[
            'flex items-center transition-colors',
            item.routeName 
              ? 'text-grey-text hover:text-amma-green cursor-pointer' 
              : 'text-amma-green-dark cursor-default',
            item.icon ? '' : 'font-semibold text-sm'
          ]"
          :title="item.icon ? item.label : ''"
        >
          <span v-if="item.icon" class="material-symbols-outlined text-[22px]">{{ item.icon }}</span>
          <span v-else>{{ item.label }}</span>
        </span>
        
        <span 
          v-if="index < items.length - 1" 
          class="material-symbols-outlined text-grey-text/40 text-[18px] mx-1"
        >
          chevron_right
        </span>
      </template>
    </div>

    <button 
      v-if="showBack"
      @click="goBack" 
      class="flex items-center gap-1.5 text-grey-text hover:text-amma-green hover:border-amma-green bg-white hover:bg-amma-green-lighter/10 transition-colors font-bold text-sm outline-none border border-gray-200 rounded-[5px_20px_5px_20px] px-3 py-1.5 shadow-sm efeito-zoom"
    >
      <span class="material-symbols-outlined text-[18px]">arrow_back</span>
      Voltar
    </button>
    
  </div>
</template>