<script setup lang="ts">
import { RouterLink } from 'vue-router'
import { useAuthStore } from '@/stores/authStore'

const auth = useAuthStore()
const links = [
  { name: 'perfil', title: 'Meu perfil', text: 'Confira seus dados pessoais e mantenha seu contato atualizado.', icon: 'person' },
  { name: 'arborizacao', title: 'Arborização', text: 'Conheça as orientações para plantar e solicitar uma muda.', icon: 'psychiatry' },
  { name: 'biblioteca-catalogo', title: 'Biblioteca Digital', text: 'Acesse materiais sobre arborização e educação ambiental.', icon: 'local_library' },
]
</script>

<template>
  <section class="py-6 md:py-10" aria-labelledby="cidadao-dashboard-title">
    <header class="mb-8">
      <p class="text-xs font-semibold uppercase tracking-wider text-grey-text">Seu espaço no Arborize</p>
      <h1 id="cidadao-dashboard-title" class="mt-2 text-2xl font-bold text-amma-green-dark">Olá{{ auth.nome ? `, ${auth.nome}` : '' }}!</h1>
      <p class="mt-2 text-sm text-grey-text">Cuide dos seus dados e explore os serviços e conteúdos do Arborize.</p>
    </header>
    <nav aria-label="Serviços do cidadão" class="grid grid-cols-1 gap-5 md:grid-cols-3">
      <RouterLink v-for="link in links" :key="link.name" :to="{ name: link.name }"
        class="flex flex-col items-start gap-3 rounded-box border-2 border-amma-green-light bg-amma-green-lighter p-6 text-grey-text transition-colors hover:border-amma-green focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-amma-green">
        <span class="material-symbols-outlined text-3xl text-amma-green" aria-hidden="true">{{ link.icon }}</span>
        <h2 class="text-lg font-bold text-amma-green-dark">{{ link.title }}</h2>
        <p class="text-sm">{{ link.text }}</p>
      </RouterLink>
    </nav>
  </section>
</template>
