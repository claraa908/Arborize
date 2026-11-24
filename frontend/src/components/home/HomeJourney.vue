<script setup lang="ts">
import { ArrowDown, ArrowRight } from 'lucide-vue-next'
import { RouterLink } from 'vue-router'
import { requestSteps } from '@/data/homeContent'
</script>

<template>
  <section aria-labelledby="jornada-titulo">
    <h2 id="jornada-titulo" class="text-2xl font-bold text-amma-green-dark sm:text-3xl">
      Como funciona
    </h2>
    <ol
      role="list"
      class="relative mt-4 grid gap-4 before:pointer-events-none before:absolute before:inset-y-5 before:left-5 before:w-px before:bg-amma-green-light lg:grid-cols-4 lg:gap-5 lg:before:inset-x-[12.5%] lg:before:inset-y-auto lg:before:top-5 lg:before:h-px lg:before:w-auto"
    >
      <li
        v-for="step in requestSteps"
        :key="step.id"
        class="relative grid min-w-0 grid-cols-[auto_minmax(0,1fr)] items-start gap-3 lg:flex lg:flex-col lg:items-center lg:text-center"
      >
        <span
          class="grid size-10 shrink-0 place-items-center rounded-full bg-amma-bg text-amma-green-dark"
          aria-hidden="true"
        >
          <component :is="step.icon" class="size-5" />
        </span>
        <div class="min-w-0 rounded-btn bg-white">
          <h3 class="text-lg font-bold leading-tight text-amma-green-dark">{{ step.title }}</h3>
          <p class="mt-1 text-sm leading-snug text-grey-text">{{ step.text }}</p>
          <RouterLink
            v-if="step.id === 'descubra'"
            :to="{ name: 'arborizacao-assistente' }"
            class="mt-2 inline-flex min-h-11 items-center gap-2 rounded-btn bg-amma-green-dark px-4 text-sm font-semibold text-white transition-colors hover:bg-amma-green-dark/90 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-amma-green-dark"
          >
            Começar o quiz <ArrowRight class="size-4 shrink-0" aria-hidden="true" />
          </RouterLink>
          <a
            v-if="step.id === 'retire'"
            href="#como-plantar"
            class="mt-2 inline-flex min-h-11 items-center gap-1.5 rounded-btn text-sm font-semibold text-amma-green-dark underline decoration-amma-green-light decoration-2 underline-offset-4 hover:decoration-current focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-amma-green-dark"
          >
            Ver guia de plantio <ArrowDown class="size-4 shrink-0" aria-hidden="true" />
          </a>
        </div>
      </li>
    </ol>
  </section>
</template>
