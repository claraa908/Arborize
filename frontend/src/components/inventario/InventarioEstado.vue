<script setup lang="ts">
// Apenas apresentação; a integração futura controlará o estado real da consulta.
withDefaults(
  defineProps<{
    estado?: 'pendente' | 'carregando' | 'erro' | 'vazio' | 'sem-resultados'
    titulo?: string
    mensagem?: string
  }>(),
  { estado: 'pendente' },
)

const textos = {
  pendente: {
    icon: 'info',
    titulo: 'Consulta ainda indisponível',
    mensagem:
      'Os registros não foram carregados. Nenhuma informação do inventário está sendo consultada nesta tela.',
  },
  carregando: {
    icon: 'progress_activity',
    titulo: 'Carregando inventário',
    mensagem: 'Aguarde enquanto os registros são consultados.',
  },
  erro: {
    icon: 'error',
    titulo: 'Não foi possível carregar o inventário',
    mensagem:
      'Ocorreu uma falha na consulta. Isso não significa que não existam árvores cadastradas.',
  },
  vazio: {
    icon: 'eco',
    titulo: 'Nenhuma árvore cadastrada',
    mensagem: 'Cadastre o primeiro indivíduo arbóreo para iniciar o inventário.',
  },
  'sem-resultados': {
    icon: 'search_off',
    titulo: 'Nenhum resultado encontrado',
    mensagem: 'Revise a pesquisa e os filtros para encontrar outros registros.',
  },
}
</script>

<template>
  <div
    :role="estado === 'erro' ? 'alert' : 'status'"
    :aria-busy="estado === 'carregando'"
    class="flex items-start gap-3 rounded-btn border p-4 text-sm"
    :class="
      estado === 'erro'
        ? 'border-feedback-danger-border bg-feedback-danger-bg text-feedback-danger'
        : 'border-amma-green-light bg-amma-green-lighter text-amma-green-dark'
    "
  >
    <span
      aria-hidden="true"
      class="material-symbols-outlined shrink-0 text-[22px]"
      :class="{ 'animate-spin motion-reduce:animate-none': estado === 'carregando' }"
      >{{ textos[estado].icon }}</span
    >
    <div class="min-w-0">
      <p class="font-bold">{{ titulo ?? textos[estado].titulo }}</p>
      <p class="mt-1 leading-relaxed">{{ mensagem ?? textos[estado].mensagem }}</p>
    </div>
  </div>
</template>
