<script setup lang="ts">
import { useInventarioNavegacao } from '@/components/inventario/useInventarioNavegacao'
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import AppBreadcrumb, { type BreadcrumbItem } from '@/components/ui/AppBreadcrumb.vue'
import ArvoreFormulario from '@/components/inventario/ArvoreFormulario.vue'

const route = useRoute()
const editando = computed(() => route.name === 'inventario-editar')
const identificador = computed(() => String(route.params.id ?? ''))
const breadcrumbItems = computed<BreadcrumbItem[]>(() => [
  { label: 'Início', routeName: 'dashboard', icon: 'home' },
  { label: 'Inventário', routeName: 'inventario' },
  ...(editando.value
    ? [
        {
          label: `Árvore ${identificador.value}`,
          routeName: 'inventario-detalhes',
          params: { id: identificador.value },
        },
      ]
    : []),
  { label: editando.value ? 'Editar árvore' : 'Nova árvore' },
])

useInventarioNavegacao()
</script>

<template>
  <div ref="pagina" class="flex w-full flex-col">
    <AppBreadcrumb :items="breadcrumbItems" />
    <div class="flex flex-col gap-4 px-4 py-4 md:px-6">
      <header class="flex items-start gap-3">
        <span
          aria-hidden="true"
          class="material-symbols-outlined flex h-12 w-12 shrink-0 items-center justify-center rounded-full bg-amma-green-lighter text-amma-green"
          >{{ editando ? 'edit_square' : 'add' }}</span
        >
        <div>
          <h1 class="text-2xl font-bold text-amma-green-dark">
            {{ editando ? 'Editar árvore' : 'Nova árvore' }}
          </h1>
          <p class="mt-1 text-sm text-grey-text">
            {{
              editando
                ? 'Localização, identificação e características do indivíduo arbóreo.'
                : 'Registre a espécie, a localização e as informações disponíveis da árvore.'
            }}
          </p>
        </div>
      </header>
      <!-- A key impede manter campos de outra rota ao alternar cadastro/edição. -->
      <ArvoreFormulario :key="route.fullPath" :editando="editando" :identificador="identificador" />
    </div>
  </div>
</template>
