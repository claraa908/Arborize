<script setup lang="ts">
import { RouterLink } from 'vue-router'
</script>

<template>
  <section class="mx-auto max-w-5xl py-8 md:py-12" aria-labelledby="arborizacao-title">
    <header class="mb-7">
      <p class="text-xs font-bold uppercase tracking-wider text-amma-green">Serviços ao cidadão</p>
      <h1 id="arborizacao-title" class="mt-2 text-2xl font-bold text-amma-green-dark">Arborização</h1>
      <p class="mt-2 max-w-3xl text-sm leading-6 text-grey-text">
        Escolha o serviço adequado. Pedidos de retirada de Nim são analisados pela AMMA; recomendações de plantio continuam no assistente.
      </p>
    </header>

    <div class="grid grid-cols-1 gap-5 md:grid-cols-2">
      <article class="flex min-w-0 flex-col rounded-box border border-amma-green-light bg-white p-5 shadow-sm md:p-7">
        <span class="flex h-12 w-12 shrink-0 items-center justify-center rounded-btn bg-amma-green-lighter text-amma-green" aria-hidden="true"><span class="material-symbols-outlined block text-[28px] leading-none">park</span></span>
        <h2 class="mt-4 text-lg font-bold text-amma-green-dark">Retirada de Nim Indiano</h2>
        <p class="mt-2 flex-1 text-sm leading-6 text-grey-text">Solicite uma avaliação da AMMA para retirar um Nim e acompanhar a análise, o laudo e as próximas etapas.</p>
        <RouterLink :to="{ name: 'arborizacao-substituicao-nim' }" class="efeito-zoom mt-5 inline-flex min-h-11 items-center justify-center gap-2 self-start rounded-btn bg-amma-green px-5 py-2.5 text-sm font-bold text-white hover:bg-amma-green-dark focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-amma-green">
          <span class="material-symbols-outlined" aria-hidden="true">description</span>
          Fazer requerimento
        </RouterLink>
        <RouterLink :to="{ name: 'arborizacao-substituicao-acompanhar' }" class="mt-3 inline-flex min-h-11 items-center self-start rounded-btn px-2 text-sm font-semibold text-amma-green-dark underline decoration-amma-green-light underline-offset-4 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-amma-green">
          Acompanhar requerimento
        </RouterLink>
      </article>

      <article class="flex min-w-0 flex-col rounded-box border border-amma-green-light bg-white p-5 shadow-sm md:p-7">
        <span class="flex h-12 w-12 shrink-0 items-center justify-center rounded-btn bg-amma-green-lighter text-amma-green" aria-hidden="true"><span class="material-symbols-outlined block text-[28px] leading-none">psychiatry</span></span>
        <h2 class="mt-4 text-lg font-bold text-amma-green-dark">Assistente de plantio</h2>
        <p class="mt-2 flex-1 text-sm leading-6 text-grey-text">Responda sobre o local e receba recomendações de espécies adequadas para o plantio e o pedido de mudas.</p>
        <RouterLink :to="{ name: 'arborizacao-assistente' }" class="efeito-zoom mt-5 inline-flex min-h-11 items-center justify-center gap-2 self-start rounded-btn border-2 border-amma-green bg-white px-5 py-2.5 text-sm font-bold text-amma-green hover:bg-amma-green-lighter focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-amma-green">
          <span class="material-symbols-outlined" aria-hidden="true">eco</span>
          Começar assistente
        </RouterLink>
        <RouterLink :to="{ name: 'arborizacao-acompanhar' }" class="mt-3 inline-flex min-h-11 items-center self-start rounded-btn px-2 text-sm font-semibold text-amma-green-dark underline decoration-amma-green-light underline-offset-4 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-amma-green">
          Acompanhar pedido de muda
        </RouterLink>
      </article>
    </div>
  </section>
</template>
