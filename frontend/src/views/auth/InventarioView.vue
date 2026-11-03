<script setup lang="ts">
import { computed, onMounted, useTemplateRef } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import { useInventarioNavegacao } from '@/components/inventario/useInventarioNavegacao'
import { useInventarioConsulta } from '@/components/inventario/useInventarioConsulta'
import AppBreadcrumb from '@/components/ui/AppBreadcrumb.vue'
import AppDataTable, { type TableColumn } from '@/components/ui/AppDataTable.vue'
import InventarioEstado from '@/components/inventario/InventarioEstado.vue'
import InventarioMapa from '@/components/inventario/InventarioMapa.vue'
import ArvoreFotoMiniatura from '@/components/inventario/ArvoreFotoMiniatura.vue'
import { condicoesMapa, temCoordenadasValidas } from '@/components/inventario/inventarioMapaConfig'
import type { ArvoreResumoResponseDTO } from '@/types/inventario'

const router = useRouter()
const mapa = useTemplateRef<InstanceType<typeof InventarioMapa>>('mapa')
const {
  store,
  criterios,
  tamanhoPagina,
  idSelecionado,
  consultando,
  filtrosAtivos,
  acessoNegado,
  consultar,
  aplicarFiltros,
  limparFiltros,
  mudarPagina,
  mudarTamanho,
} = useInventarioConsulta()
const columns: TableColumn[] = [
  { key: 'fotografia', label: 'Fotografia' },
  { key: 'codigo', label: 'Código' },
  { key: 'especie', label: 'Espécie' },
  { key: 'localizacao', label: 'Localização' },
  { key: 'condicaoFitossanitaria', label: 'Condição' },
  { key: 'acoes', label: 'Ações', align: 'right' },
]
const campoClass =
  'w-full min-w-0 rounded-btn border-2 border-amma-green-light bg-white px-4 py-2.5 text-sm text-grey-text outline-none focus:border-amma-green disabled:cursor-not-allowed disabled:bg-amma-green-lighter'
const mensagemMapaVazio = computed(() =>
  store.registrosMapa === null
    ? 'Os registros do mapa ainda não estão disponíveis.'
    : filtrosAtivos.value
      ? 'Nenhuma árvore corresponde aos filtros aplicados.'
      : 'Nenhuma árvore cadastrada.',
)
function podeLocalizar(arvore: ArvoreResumoResponseDTO) {
  return (
    temCoordenadasValidas(arvore) &&
    Boolean(store.registrosMapa?.some((item) => item.id === arvore.id))
  )
}
function abrirFicha(id: number) {
  router.push({ name: 'inventario-detalhes', params: { id } })
}
useInventarioNavegacao()
onMounted(() => {
  void consultar()
  void store.fetchBairros().catch(() => {})
})
</script>

<template>
  <div ref="pagina" class="flex w-full flex-col">
    <AppBreadcrumb
      :items="[{ label: 'Início', routeName: 'dashboard', icon: 'home' }, { label: 'Inventário' }]"
      :show-back="false"
    />
    <div class="flex min-w-0 flex-col gap-8 px-6 py-8 md:px-10">
      <header class="flex flex-col justify-between gap-4 sm:flex-row sm:items-center">
        <div>
          <p class="mb-2 text-xs font-bold uppercase tracking-widest text-grey-text">
            Gestão ambiental · AMMA
          </p>
          <h1 class="text-2xl font-bold text-amma-green-dark">Inventário Arbóreo</h1>
          <p class="mt-1 text-sm text-grey-text">
            Indivíduos arbóreos, localização e características técnicas.
          </p>
        </div>
        <RouterLink
          :to="{ name: 'inventario-nova' }"
          class="flex shrink-0 items-center justify-center gap-2 rounded-btn bg-amma-green px-5 py-3 text-sm font-bold text-white shadow-sm hover:bg-amma-green-dark focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-amma-green"
        >
          <span aria-hidden="true" class="material-symbols-outlined text-[20px]">add</span>Nova
          árvore
        </RouterLink>
      </header>

      <InventarioEstado
        v-if="acessoNegado"
        estado="erro"
        titulo="Acesso ao inventário indisponível"
        mensagem="Não foi possível acessar o inventário. Procure o responsável pelo acesso."
      />

      <section
        aria-labelledby="mapa-titulo"
        class="min-w-0 rounded-box border border-amma-green-light bg-white p-4 shadow-sm md:p-6"
      >
        <h2
          id="mapa-titulo"
          class="mb-3 flex items-center gap-2 text-lg font-bold text-amma-green-dark"
        >
          <span aria-hidden="true" class="material-symbols-outlined">pin_drop</span>Distribuição das
          árvores
        </h2>
        <form
          class="mb-4 flex min-w-0 flex-wrap items-end gap-3 rounded-btn border border-amma-green-light bg-amma-green-lighter/60 p-3"
          @submit.prevent="aplicarFiltros"
        >
          <fieldset :disabled="consultando" class="flex min-w-0 w-full flex-wrap items-end gap-3">
            <legend class="sr-only">Pesquisar e filtrar árvores do inventário</legend>
            <div class="min-w-0 w-full flex-[2_1_18rem]">
              <label
                for="inventario-pesquisa"
                class="mb-1 block text-xs font-semibold text-grey-text"
              >
                Pesquisar árvores
              </label>
              <input
                id="inventario-pesquisa"
                v-model="criterios.search"
                type="search"
                autocomplete="off"
                placeholder="Código, nome popular ou científico"
                :class="campoClass"
              />
            </div>
            <div class="min-w-0 w-full flex-[1_1_13rem]">
              <label
                for="inventario-bairro"
                class="mb-1 block text-xs font-semibold text-grey-text"
              >
                Bairro
              </label>
              <select
                id="inventario-bairro"
                v-model="criterios.bairro"
                :disabled="store.carregando.bairros || Boolean(store.erros.bairros)"
                :class="campoClass"
                aria-describedby="opcoes-ajuda bairro-filtro-status"
              >
                <option value="">Todos os bairros</option>
                <option v-for="bairro in store.opcoesBairros" :key="bairro.id" :value="bairro.nome">
                  {{ bairro.nome }}
                </option>
              </select>
            </div>
            <div class="min-w-0 w-full flex-[1_1_13rem]">
              <label
                for="inventario-especie"
                class="mb-1 block text-xs font-semibold text-grey-text"
              >
                Espécie
              </label>
              <select
                id="inventario-especie"
                v-model="criterios.especieId"
                :disabled="!store.opcoesCarregadas || !store.opcoesEspecies.length"
                :class="campoClass"
              >
                <option :value="undefined">Todas as espécies</option>
                <option
                  v-for="especie in store.opcoesEspecies"
                  :key="especie.id"
                  :value="especie.id"
                >
                  {{ especie.nomePopular }} — {{ especie.nomeCientifico }}
                </option>
              </select>
            </div>
            <div class="min-w-0 w-full flex-[1_1_13rem]">
              <label
                for="inventario-condicao"
                class="mb-1 block text-xs font-semibold text-grey-text"
              >
                Condição fitossanitária
              </label>
              <select id="inventario-condicao" v-model="criterios.condicao" :class="campoClass">
                <option :value="undefined">Todas as condições</option>
                <option v-for="(condicao, valor) in condicoesMapa" :key="valor" :value="valor">
                  {{ condicao.rotulo }}
                </option>
              </select>
            </div>
            <div class="flex w-full flex-wrap gap-2 sm:w-auto">
              <button
                type="button"
                class="min-h-11 w-full rounded-btn border border-amma-green-light bg-white px-4 py-2 text-sm font-bold text-grey-text hover:bg-amma-green-lighter disabled:opacity-50 sm:w-auto"
                :disabled="consultando"
                @click="limparFiltros"
              >
                Limpar
              </button>
              <button
                type="submit"
                class="min-h-11 w-full rounded-btn bg-amma-green px-4 py-2 text-sm font-bold text-white hover:bg-amma-green-dark focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-amma-green disabled:cursor-wait disabled:opacity-50 sm:w-auto"
                :disabled="consultando"
              >
                Aplicar
              </button>
            </div>
            <div
              id="opcoes-ajuda"
              class="flex w-full min-w-0 flex-col gap-1 text-xs leading-relaxed text-grey-text"
            >
              <p>Combine pesquisa e filtros para atualizar o mapa e a listagem.</p>
            </div>
          </fieldset>
        </form>
        <div
          v-if="store.erros.bairros"
          id="bairro-filtro-status"
          class="mb-3 flex flex-wrap items-center gap-2 text-sm text-feedback-danger"
          role="alert"
        >
          <span>A lista de bairros não está disponível.</span>
          <button
            type="button"
            class="rounded-btn border border-amma-green-light px-3 py-1.5 font-semibold text-amma-green-dark hover:bg-amma-green-lighter"
            :disabled="store.carregando.bairros"
            @click="store.fetchBairros().catch(() => {})"
          >
            Tentar novamente
          </button>
        </div>
        <InventarioEstado
          v-if="store.carregando.mapa"
          estado="carregando"
          titulo="Carregando árvores do mapa"
        />
        <InventarioEstado
          v-else-if="store.erros.mapa && !acessoNegado"
          estado="erro"
          titulo="Não foi possível consultar as árvores do mapa"
          :mensagem="store.erros.mapa.message"
        />
        <InventarioMapa
          ref="mapa"
          v-model:selecionada-id="idSelecionado"
          :arvores="store.registrosMapa ?? []"
          :mensagem-vazia="mensagemMapaVazio"
          @abrir-ficha="abrirFicha"
        />
      </section>

      <section aria-labelledby="arvores-titulo" class="min-w-0">
        <div class="mb-4 flex flex-col justify-between gap-3 sm:flex-row sm:items-center">
          <h2 id="arvores-titulo" class="text-lg font-bold text-amma-green-dark">
            Árvores do inventário
          </h2>
          <div class="flex items-center gap-2 text-sm text-grey-text">
            <label for="inventario-tamanho">Registros por página</label>
            <select
              id="inventario-tamanho"
              v-model="tamanhoPagina"
              :disabled="consultando"
              class="rounded-btn border border-amma-green-light bg-white px-3 py-2"
              @change="mudarTamanho"
            >
              <option v-for="size in [10, 20, 50, 100]" :key="size" :value="size">
                {{ size }}
              </option>
            </select>
          </div>
        </div>
        <InventarioEstado v-if="store.carregando.listagem" estado="carregando" />
        <InventarioEstado
          v-else-if="store.erros.listagem && !acessoNegado"
          estado="erro"
          :mensagem="store.erros.listagem.message"
        />
        <p v-else-if="store.erros.listagem" role="status" class="text-sm text-grey-text">
          Seu acesso não permite consultar a listagem.
        </p>
        <AppDataTable
          v-else-if="store.pagina"
          :columns="columns"
          :items="store.pagina.content"
          :pagination="store.pagina"
          :loading="consultando"
          :selected-id="idSelecionado"
          caption="Árvores do inventário"
          :show-search="false"
          :allow-view-toggle="false"
          default-view="table"
          :empty-message="
            filtrosAtivos
              ? 'Nenhum resultado para os filtros aplicados.'
              : 'Nenhuma árvore cadastrada.'
          "
          @prev-page="mudarPagina(store.pagina.page - 1)"
          @next-page="mudarPagina(store.pagina.page + 1)"
        >
          <template #fotografia="{ item }">
            <ArvoreFotoMiniatura :arvore-id="item.id" :foto-capa="item.fotoCapa" />
          </template>
          <template #codigo="{ item }"
            ><span class="font-bold text-amma-green-dark">{{ item.codigo }}</span
            ><span v-if="item.id === idSelecionado" class="block text-xs text-amma-green-dark"
              >Selecionada no mapa</span
            ></template
          >
          <template #especie="{ item }"
            ><p class="font-semibold text-grey-text">{{ item.especie.nomePopular }}</p>
            <p class="text-xs italic text-grey-text">{{ item.especie.nomeCientifico }}</p></template
          >
          <template #localizacao="{ item }"
            ><span class="text-sm text-grey-text">{{
              item.zonaRural === true || item.classificacaoTerritorial === 'FORA_DA_AREA_URBANA'
                ? item.localidadeRural || 'Localidade rural não informada'
                : item.bairroNome ?? item.bairro?.nome ?? 'Bairro não identificado'
            }}</span>
            <p v-if="item.referenciaLocalizacao" class="text-xs text-grey-text">
              {{ item.referenciaLocalizacao }}
            </p></template
          >
          <template #condicaoFitossanitaria="{ item }"
            ><span class="text-sm text-grey-text">{{
              condicoesMapa[item.condicaoFitossanitaria].rotulo
            }}</span></template
          >
          <template #acoes="{ item }">
            <div class="flex flex-wrap justify-end gap-2">
              <RouterLink
                :to="{ name: 'inventario-detalhes', params: { id: item.id } }"
                :aria-label="`Ver ficha da árvore ${item.codigo}`"
                class="rounded-btn border border-amma-green-light px-3 py-2 text-sm font-semibold text-amma-green-dark hover:bg-amma-green-lighter"
                >Ver ficha</RouterLink
              >
              <button
                type="button"
                :disabled="!podeLocalizar(item)"
                :aria-label="`Localizar árvore ${item.codigo} no mapa`"
                class="rounded-btn border border-amma-green-light px-3 py-2 text-sm font-semibold text-amma-green-dark hover:bg-amma-green-lighter disabled:cursor-not-allowed disabled:opacity-40"
                @click="mapa?.localizarArvore(item.id)"
              >
                Localizar
              </button>
            </div>
          </template>
        </AppDataTable>
        <p v-else role="status" class="text-sm text-grey-text">Nenhuma consulta concluída.</p>
        <button
          v-if="store.erros.listagem || store.erros.mapa"
          type="button"
          :disabled="consultando"
          class="mt-4 rounded-btn border border-amma-green px-5 py-2.5 text-sm font-bold text-amma-green-dark disabled:opacity-50"
          @click="consultar"
        >
          Consultar novamente
        </button>
      </section>
    </div>
  </div>
</template>
