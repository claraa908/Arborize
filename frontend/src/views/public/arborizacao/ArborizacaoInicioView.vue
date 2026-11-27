<script setup lang="ts">
import { computed } from 'vue'
import { RouterLink } from 'vue-router'
import { useAuthStore } from '@/stores/authStore'
import capaRetiradaNim from '@/assets/images/capa-retirada-nim.png'
import capaAssistentePlantio from '@/assets/images/capa-assistente-plantio.png'

const auth = useAuthStore()

const acompanharSubstituicaoDestino = computed(() =>
  auth.canAccessManagement
    ? { name: 'arborizacao-substituicao-acompanhar' }
    : { name: 'perfil', query: { filtro: 'SUBSTITUICOES' } },
)

const acompanharMudaDestino = computed(() =>
  auth.canAccessManagement
    ? { name: 'arborizacao-acompanhar' }
    : { name: 'perfil', query: { filtro: 'MUDAS' } },
)
</script>

<template>
  <section class="mx-auto max-w-5xl py-8 md:py-12" aria-labelledby="arborizacao-title">
    <header class="mb-8 flex flex-col gap-2">
      <span class="inline-flex w-fit items-center gap-1.5 rounded-full bg-amma-green-lighter px-3 py-1 text-xs font-bold uppercase tracking-wider text-amma-green-dark">
        <span class="material-symbols-outlined text-[15px]" aria-hidden="true">nature_people</span>
        Serviços ao cidadão
      </span>
      <h1 id="arborizacao-title" class="mt-1 text-3xl font-extrabold tracking-tight text-amma-green-dark">
        Arborização Urbana
      </h1>
      <p class="max-w-3xl text-sm leading-6 text-grey-text md:text-base">
        Solicite a substituição de espécies exóticas invasoras com segurança ou utilize o assistente de plantio para escolher mudas nativas adequadas ao seu espaço.
      </p>
    </header>

    <div class="grid grid-cols-1 gap-6 md:grid-cols-2">
      <article class="group flex min-w-0 flex-col overflow-hidden rounded-box border border-amma-green-light bg-white shadow-sm transition-all duration-300 hover:-translate-y-0.5 hover:shadow-lg">
        <div class="relative h-48 overflow-hidden bg-gradient-to-br from-amma-green-dark to-amma-green sm:h-52">
          <img
            :src="capaRetiradaNim"
            alt="Substituição sustentável de árvore invasora por muda nativa da Caatinga em calçada urbana"
            class="h-full w-full object-cover transition-transform duration-500 group-hover:scale-105"
          />
          <div class="absolute inset-0 bg-gradient-to-t from-black/60 via-black/15 to-transparent" aria-hidden="true"></div>
          <span class="absolute left-4 top-4 inline-flex items-center gap-1.5 rounded-full bg-white/95 px-3 py-1 text-[11px] font-bold uppercase tracking-wider text-amma-green-dark shadow-xs backdrop-blur-xs">
            <span class="h-1.5 w-1.5 rounded-full bg-amma-green" aria-hidden="true"></span>
            Substituição Botânica
          </span>
          <div class="absolute bottom-4 left-5 flex items-center gap-3">
            <span class="flex h-11 w-11 shrink-0 items-center justify-center rounded-full border border-white/60 bg-white text-amma-green shadow-md" aria-hidden="true">
              <span class="material-symbols-outlined text-[24px]">park</span>
            </span>
            <h2 class="text-xl font-bold text-white drop-shadow-xs">Retirada de Nim Indiano</h2>
          </div>
        </div>

        <div class="flex flex-1 flex-col p-6 md:p-7">
          <p class="text-sm leading-6 text-grey-text">
            Solicite uma avaliação da AMMA para retirar um Nim e acompanhe a análise, o laudo técnico e as próximas etapas da substituição.
          </p>

          <ul class="mt-4 space-y-2 text-xs font-medium text-grey-text">
            <li class="flex items-center gap-2">
              <span class="material-symbols-outlined text-[16px] text-amma-green" aria-hidden="true">check_circle</span>
              Vistoria técnica realizada pela equipe da AMMA
            </li>
            <li class="flex items-center gap-2">
              <span class="material-symbols-outlined text-[16px] text-amma-green" aria-hidden="true">check_circle</span>
              Emissão e acompanhamento do laudo oficial
            </li>
            <li class="flex items-center gap-2">
              <span class="material-symbols-outlined text-[16px] text-amma-green" aria-hidden="true">check_circle</span>
              Indicação de reposição por espécie nativa
            </li>
          </ul>

          <div class="mt-6 flex flex-wrap items-center justify-between gap-3 border-t border-amma-green-light/70 pt-5">
            <RouterLink
              :to="{ name: 'arborizacao-substituicao-nim' }"
              class="efeito-zoom inline-flex min-h-11 items-center justify-center gap-2 rounded-btn bg-amma-green px-5 py-2.5 text-sm font-bold text-white shadow-sm hover:bg-amma-green-dark focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-amma-green"
            >
              <span class="material-symbols-outlined text-[19px]" aria-hidden="true">description</span>
              Fazer requerimento
            </RouterLink>
            <RouterLink
              :to="acompanharSubstituicaoDestino"
              class="inline-flex min-h-11 items-center gap-1 rounded-btn px-2 text-sm font-semibold text-amma-green-dark underline decoration-amma-green-light underline-offset-4 transition-colors hover:text-amma-green focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-amma-green"
            >
              Acompanhar requerimento
              <span class="material-symbols-outlined text-[16px]" aria-hidden="true">arrow_forward</span>
            </RouterLink>
          </div>
        </div>
      </article>

      <article class="group flex min-w-0 flex-col overflow-hidden rounded-box border border-amma-green-light bg-white shadow-sm transition-all duration-300 hover:-translate-y-0.5 hover:shadow-lg">
        <div class="relative h-48 overflow-hidden bg-gradient-to-br from-amma-green-dark to-amma-green sm:h-52">
          <img
            :src="capaAssistentePlantio"
            alt="Mãos cuidando de muda jovem de espécie nativa da Caatinga para plantio urbano"
            class="h-full w-full object-cover transition-transform duration-500 group-hover:scale-105"
          />
          <div class="absolute inset-0 bg-gradient-to-t from-black/60 via-black/15 to-transparent" aria-hidden="true"></div>
          <span class="absolute left-4 top-4 inline-flex items-center gap-1.5 rounded-full bg-white/95 px-3 py-1 text-[11px] font-bold uppercase tracking-wider text-amma-green-dark shadow-xs backdrop-blur-xs">
            <span class="h-1.5 w-1.5 rounded-full bg-amma-green" aria-hidden="true"></span>
            Assistente Interativo
          </span>
          <div class="absolute bottom-4 left-5 flex items-center gap-3">
            <span class="flex h-11 w-11 shrink-0 items-center justify-center rounded-full border border-white/60 bg-white text-amma-green shadow-md" aria-hidden="true">
              <span class="material-symbols-outlined text-[24px]">psychiatry</span>
            </span>
            <h2 class="text-xl font-bold text-white drop-shadow-xs">Assistente de plantio</h2>
          </div>
        </div>

        <div class="flex flex-1 flex-col p-6 md:p-7">
          <p class="text-sm leading-6 text-grey-text">
            Responda sobre o local do plantio e receba recomendações de espécies nativas adequadas antes de solicitar suas mudas.
          </p>

          <ul class="mt-4 space-y-2 text-xs font-medium text-grey-text">
            <li class="flex items-center gap-2">
              <span class="material-symbols-outlined text-[16px] text-amma-green" aria-hidden="true">check_circle</span>
              Questionário rápido sobre calçada, rede elétrica e espaço
            </li>
            <li class="flex items-center gap-2">
              <span class="material-symbols-outlined text-[16px] text-amma-green" aria-hidden="true">check_circle</span>
              Recomendações ideais de espécies da Caatinga
            </li>
            <li class="flex items-center gap-2">
              <span class="material-symbols-outlined text-[16px] text-amma-green" aria-hidden="true">check_circle</span>
              Retirada organizada junto ao viveiro do IFCE
            </li>
          </ul>

          <div class="mt-6 flex flex-wrap items-center justify-between gap-3 border-t border-amma-green-light/70 pt-5">
            <RouterLink
              :to="{ name: 'arborizacao-assistente' }"
              class="efeito-zoom inline-flex min-h-11 items-center justify-center gap-2 rounded-btn border-2 border-amma-green bg-white px-5 py-2.5 text-sm font-bold text-amma-green hover:bg-amma-green-lighter focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-amma-green"
            >
              <span class="material-symbols-outlined text-[19px]" aria-hidden="true">eco</span>
              Começar assistente
            </RouterLink>
            <RouterLink
              :to="acompanharMudaDestino"
              class="inline-flex min-h-11 items-center gap-1 rounded-btn px-2 text-sm font-semibold text-amma-green-dark underline decoration-amma-green-light underline-offset-4 transition-colors hover:text-amma-green focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-amma-green"
            >
              Acompanhar pedido de muda
              <span class="material-symbols-outlined text-[16px]" aria-hidden="true">arrow_forward</span>
            </RouterLink>
          </div>
        </div>
      </article>
    </div>
  </section>
</template>
