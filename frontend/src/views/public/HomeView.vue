<script setup lang="ts">
import CaatingaTipsCarousel from '@/components/home/CaatingaTipsCarousel.vue'
import HomeJourney from '@/components/home/HomeJourney.vue'
import { plantingSteps } from '@/data/homeContent'
</script>

<template>
  <div class="mx-auto min-w-0 max-w-5xl space-y-10 py-5 md:space-y-14 md:py-8">
    <section
      aria-labelledby="boas-vindas-titulo"
      class="welcome-card rounded-box bg-amma-green-dark px-6 py-8 text-center text-white shadow-md sm:px-9 sm:py-10 lg:py-12"
    >
      <p class="text-xs font-semibold uppercase tracking-wider">AMMA de Quixadá</p>
      <h1
        id="boas-vindas-titulo"
        class="mt-2 text-3xl font-black leading-tight tracking-tight sm:text-4xl lg:text-5xl"
      >
        Bem-vindo ao Arborize
      </h1>
      <p class="mx-auto mt-2 max-w-md text-base leading-snug sm:text-lg">
        Encontre a árvore certa para seu espaço, solicite sua muda e saiba como plantar e cuidar
        dela.
      </p>
    </section>

    <HomeJourney />

    <section
      class="rounded-box bg-amma-green-lighter px-5 py-6 sm:px-7 sm:py-7 lg:px-9"
      aria-labelledby="como-plantar"
    >
      <h2
        id="como-plantar"
        tabindex="-1"
        class="scroll-mt-4 text-2xl font-bold text-amma-green-dark focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-amma-green-dark sm:text-3xl"
      >
        Como plantar sua muda
      </h2>
      <ol class="mt-2 grid gap-x-6 sm:grid-cols-2 lg:grid-cols-3" role="list">
        <li
          v-for="(step, index) in plantingSteps"
          :key="step.id"
          class="grid min-w-0 grid-cols-[auto_minmax(0,1fr)] items-start gap-3 border-t border-amma-green-light py-4"
        >
          <span
            class="grid size-9 place-items-center rounded-btn bg-white text-amma-green-dark"
            aria-hidden="true"
          >
            <component :is="step.icon" class="size-5" />
          </span>
          <div>
            <h3 class="text-base font-bold leading-tight text-amma-green-dark">
              {{ index + 1 }}. {{ step.title }}
            </h3>
            <p class="mt-1 text-sm leading-snug text-grey-text">{{ step.text }}</p>
          </div>
        </li>
      </ol>
    </section>

    <CaatingaTipsCarousel />
  </div>
</template>

<style scoped>
.welcome-card {
  background-image:
    radial-gradient(ellipse at 100% 0, rgb(255 255 255 / 9%) 22%, transparent 22.4%),
    radial-gradient(ellipse at 0 100%, rgb(255 255 255 / 7%) 26%, transparent 26.4%),
    linear-gradient(
      120deg,
      color-mix(in srgb, var(--color-amma-green-dark), black 24%),
      var(--color-amma-green-dark)
    );
}
</style>
