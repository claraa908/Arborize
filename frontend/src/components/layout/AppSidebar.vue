<script setup lang="ts">
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()

const menuItems = [
  { name: 'Dashboard e Relatórios', icon: 'insert_chart', routeName: 'dashboard' },
  { name: 'Inventário', icon: 'pin_drop', routeName: 'inventario' },
  { name: 'Viveiro', icon: 'psychiatry', routeName: 'viveiro' },
  { name: 'Solicitações', icon: 'content_paste', routeName: 'solicitacoes' },
  { name: 'Laudos', icon: 'description', routeName: 'laudos' },
  { name: 'Podas', icon: 'content_cut', routeName: 'podas' },
  { name: 'Eventos', icon: 'event', routeName: 'eventos' }
]

const navigateTo = (routeName: string) => {
  router.push({ name: routeName })
}
</script>

<template>
  <aside class="w-64 flex-shrink-0 bg-amma-green text-white flex flex-col rounded-box shadow-2xl z-20">
    <div class="p-6 flex flex-col items-center relative">
      <button class="absolute left-6 top-6 text-white hover:scale-110 transition-transform cursor-pointer">
        <span class="material-symbols-outlined text-[28px]">menu</span>
      </button>

      <div class="mt-10 flex flex-col items-center">
        <img src="@/assets/images/logoBranca.png" alt="Logo Arborize" class="h-16 w-auto" />
        <span class="text-xl font-bold leading-none tracking-wide text-white">ARBORIZE</span>
        <span class="text-[0.6rem] font-medium tracking-widest text-white/80 mt-1">Gestão Ambiental</span>
      </div>
    </div>

    <nav class="flex-1 px-4 py-4 flex flex-col gap-1 overflow-y-auto mt-4">
      <a
        v-for="item in menuItems"
        :key="item.name"
        href="#"
        :class="[
          'flex items-center gap-4 px-4 py-3.5 text-sm font-semibold transition-all cursor-pointer',
          route.name === item.routeName
            ? 'bg-amma-green-dark text-white rounded-btn shadow-inner'
            : 'text-white/80 hover:bg-black/10 hover:text-white rounded-btn'
        ]"
        @click.prevent="navigateTo(item.routeName)"
      >
        <span class="material-symbols-outlined text-[22px]">{{ item.icon }}</span>
        {{ item.name }}
      </a>
    </nav>
  </aside>
</template>