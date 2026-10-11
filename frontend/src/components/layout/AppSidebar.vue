<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()

const menuItems = [
  { name: 'Dashboard e Relatórios', icon: 'insert_chart', routeName: 'dashboard' },
  { name: 'Biblioteca Digital', icon: 'local_library', routeName: 'biblioteca-lista' },
  { name: 'Inventário', icon: 'pin_drop', routeName: 'inventario' },
  { name: 'Viveiro', icon: 'psychiatry', routeName: 'viveiro' },
  { name: 'Solicitações', icon: 'content_paste', routeName: 'solicitacoes' },
  { name: 'Laudos', icon: 'description', routeName: 'laudos' },
  { name: 'Eventos', icon: 'event', routeName: 'eventos' }
]

const isExpanded = ref(true)
const isMobileOpen = ref(false)
const windowWidth = ref(window.innerWidth)

const handleResize = () => {
  windowWidth.value = window.innerWidth
  if (windowWidth.value < 1024) {
    isExpanded.value = false 
  } else {
    isExpanded.value = true 
  }
}

onMounted(() => {
  window.addEventListener('resize', handleResize)
  handleResize()
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
})

const navigateTo = (routeName: string) => {
  router.push({ name: routeName })
  isMobileOpen.value = false
}

const isActive = (routeName: string) => {
  return route.name === routeName || route.meta.activeMenu === routeName
}
</script>

<template>
  <aside 
    :class="[ 
      'hidden md:flex flex-col bg-amma-green text-white rounded-box shadow-2xl z-20 transition-all duration-300 flex-shrink-0 relative',
      isExpanded ? 'w-64' : 'w-[88px]'
    ]"
  >
    <button 
      v-if="windowWidth >= 1024"
      @click="isExpanded = !isExpanded"
      class="absolute -right-3.5 top-1/2 -translate-y-1/2 bg-white text-amma-green-dark border-2 border-amma-green-light rounded-full w-7 h-7 flex items-center justify-center shadow-md hover:scale-110 transition-transform z-30 cursor-pointer"
    >
      <span class="material-symbols-outlined text-[20px]">
        {{ isExpanded ? 'chevron_left' : 'chevron_right' }}
      </span>
    </button>

    <div class="p-6 flex flex-col items-center min-h-[140px] justify-center transition-all duration-300">
      <template v-if="isExpanded">
        <img src="@/assets/images/logoBranca.png" alt="Logo Arborize" class="h-14 w-auto mb-2" />
        <span class="text-xl font-bold leading-none tracking-wide text-white">ARBORIZE</span>
        <span class="text-[0.6rem] font-medium tracking-widest text-white/80 mt-1">Gestão Ambiental</span>
      </template>
      <template v-else>
        <img src="@/assets/images/logoBranca.png" alt="Logo Arborize" class="h-10 w-auto" />
      </template>
    </div>

    <nav class="flex-1 px-3 py-4 flex flex-col gap-2 overflow-y-auto overflow-x-hidden mt-2">
      <a
        v-for="item in menuItems"
        :key="item.name"
        href="#"
        :title="!isExpanded ? item.name : ''"
        :class="[
          'flex items-center py-3.5 transition-all cursor-pointer rounded-btn-sidebar',
          isExpanded ? 'px-4 gap-4 justify-start' : 'px-0 justify-center',
          isActive(item.routeName)
            ? 'bg-amma-green-dark text-white shadow-inner'
            : 'text-white/80 hover:bg-black/10 hover:text-white'
        ]"
        @click.prevent="navigateTo(item.routeName)"
      >
        <span class="material-symbols-outlined text-[24px]" :class="{ 'text-[26px]': !isExpanded }">{{ item.icon }}</span>
        <span v-if="isExpanded" class="text-sm font-semibold whitespace-nowrap">{{ item.name }}</span>
      </a>
    </nav>
  </aside>

  <button 
    v-if="windowWidth < 768"
    @click="isMobileOpen = true"
    class="fixed top-5 left-5 w-12 h-12 bg-amma-green text-white shadow-lg flex items-center justify-center z-40 efeito-zoom border-2 border-white"
    style="border-radius: 10px 30px 10px 30px;"
  >
    <span class="material-symbols-outlined text-[26px]">menu</span>
  </button>

  <Transition
    enter-active-class="transition duration-300 ease-out"
    enter-from-class="opacity-0 -translate-x-4"
    enter-to-class="opacity-100 translate-x-0"
    leave-active-class="transition duration-200 ease-in"
    leave-from-class="opacity-100 translate-x-0"
    leave-to-class="opacity-0 -translate-x-4"
  >
    <div v-if="isMobileOpen && windowWidth < 768" class="fixed inset-0 z-50 flex">
      
      <div class="absolute inset-0 bg-black/40 backdrop-blur-sm" @click="isMobileOpen = false"></div>
      
      <div class="relative w-4/5 max-w-[280px] bg-amma-green h-full shadow-2xl flex flex-col rounded-r-box">
        
        <div class="p-6 flex items-center justify-between border-b border-white/10">
          <button 
            @click="isMobileOpen = false" 
            class="w-12 h-12 bg-amma-green-dark border-2 border-white text-white flex items-center justify-center shadow-lg efeito-zoom"
            style="border-radius: 30px 10px 30px 30px;"
          >
            <span class="material-symbols-outlined text-[24px]">close</span>
          </button>
          
          <div class="flex flex-col items-end">
            <img src="@/assets/images/logoBranca.png" alt="Logo Arborize" class="h-10 w-auto" />
            <span class="text-xs font-bold tracking-wide text-white mt-1">ARBORIZE</span>
          </div>
        </div>

        <nav class="flex-1 px-4 py-4 flex flex-col gap-2 overflow-y-auto">
          <a
            v-for="item in menuItems"
            :key="item.name"
            href="#"
            :class="[
              'flex items-center gap-4 px-4 py-3.5 text-sm font-semibold transition-all cursor-pointer rounded-btn',
              isActive(item.routeName)
                ? 'bg-amma-green-dark text-white shadow-inner'
                : 'text-white/80 hover:bg-black/10 hover:text-white'
            ]"
            @click.prevent="navigateTo(item.routeName)"
          >
            <span class="material-symbols-outlined text-[24px]">{{ item.icon }}</span>
            {{ item.name }}
          </a>
        </nav>
      </div>
    </div>
  </Transition>
</template>