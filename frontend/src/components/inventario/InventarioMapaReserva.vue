<script setup lang="ts">
withDefaults(defineProps<{ mensagem?: string }>(), {
  mensagem:
    'A distribuição geográfica das árvores será exibida aqui quando o mapa estiver disponível.',
})
</script>

<template>
  <!-- Sem MapLibre, tiles ou representação de uma geografia fictícia nesta etapa. -->
  <div
    class="flex min-h-56 md:min-h-72 flex-col items-center justify-center gap-3 rounded-btn border border-dashed border-amma-green/40 bg-amma-green-lighter px-5 py-8 text-center"
  >
    <span
      aria-hidden="true"
      class="material-symbols-outlined flex h-14 w-14 items-center justify-center rounded-full bg-amma-green-light text-3xl text-amma-green-dark"
      >map</span
    >
    <p class="font-bold text-amma-green-dark">Mapa ainda indisponível</p>
    <p class="max-w-md text-sm leading-relaxed text-grey-text">{{ mensagem }}</p>
  </div>
</template>
