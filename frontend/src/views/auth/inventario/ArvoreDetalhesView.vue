<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import AppBreadcrumb from '@/components/ui/AppBreadcrumb.vue'
import InventarioEstado from '@/components/inventario/InventarioEstado.vue'
import InventarioMapa from '@/components/inventario/InventarioMapa.vue'
import ArvoreFotografias from '@/components/inventario/ArvoreFotografias.vue'
import { useArvoreFicha } from '@/components/inventario/useArvoreFicha'
import type { ClassificacaoTerritorial, CondicaoFitossanitaria } from '@/types/inventario'
import { useInventarioNavegacao } from '@/components/inventario/useInventarioNavegacao'

const route = useRoute()
const identificador = computed(() => String(route.params.id ?? ''))
const { arvore, carregando, erro, recarregar } = useArvoreFicha(identificador)
const arvoreSelecionadaNoMapa = ref<number | null>(null)
const identificadorInvalido = computed(
  () =>
    !/^\d+$/.test(identificador.value) ||
    Number(identificador.value) <= 0 ||
    !Number.isSafeInteger(Number(identificador.value)),
)
const tituloErro = computed(() => {
  if (erro.value?.status === 404) return 'Árvore não encontrada'
  if (erro.value?.acessoNegado) return 'Acesso ao inventário indisponível'
  if (identificadorInvalido.value) return 'Identificador inválido'
  return 'Não foi possível carregar a ficha técnica'
})
const mensagemErro = computed(() => {
  if (erro.value?.status === 404) {
    return `Não foi encontrada uma árvore com o identificador ${identificador.value}.`
  }
  if (erro.value?.acessoNegado) {
    return 'Não foi possível acessar esta ficha. Procure o responsável pelo acesso.'
  }
  return erro.value?.message ?? 'A consulta da ficha não foi concluída.'
})
const podeTentarNovamente = computed(
  () => !identificadorInvalido.value && erro.value?.status !== 404,
)
const centroMapa = computed<[number, number] | undefined>(() =>
  arvore.value ? [arvore.value.longitude, arvore.value.latitude] : undefined,
)
const arvoresDoMapa = computed(() => (arvore.value ? [arvore.value] : []))

const rotulosCondicao: Record<CondicaoFitossanitaria, string> = {
  NAO_AVALIADA: 'Não avaliada',
  APARENTEMENTE_SAUDAVEL: 'Aparentemente saudável',
  SINAIS_COMPROMETIMENTO: 'Com sinais de comprometimento',
  MORTA: 'Morta',
}

const rotulosTerritorio: Record<ClassificacaoTerritorial, string> = {
  BAIRRO_IDENTIFICADO: 'Bairro identificado',
  BAIRRO_NAO_IDENTIFICADO: 'Bairro não identificado',
  FORA_DA_AREA_URBANA: 'Fora da área urbana',
}

function formatarData(data: string | null): string {
  if (!data) return 'Não informado'
  const partes = /^(\d{4})-(\d{2})-(\d{2})$/.exec(data)
  return partes ? `${partes[3]}/${partes[2]}/${partes[1]}` : 'Não informado'
}

function formatarNumero(valor: number | null): string {
  return valor === null
    ? 'Não informado'
    : valor.toLocaleString('pt-BR', { maximumFractionDigits: 8 })
}

watch(arvore, () => {
  arvoreSelecionadaNoMapa.value = null
})

useInventarioNavegacao()
</script>

<template>
  <div ref="pagina" class="flex w-full flex-col">
    <AppBreadcrumb
      :items="[
        { label: 'Início', routeName: 'dashboard', icon: 'home' },
        { label: 'Inventário', routeName: 'inventario' },
        { label: 'Ficha técnica' },
      ]"
    />
    <div class="flex min-w-0 flex-col gap-6 px-6 py-6 md:px-10">
      <header class="flex min-w-0 flex-col justify-between gap-4 sm:flex-row sm:items-center">
        <div class="min-w-0">
          <p class="mb-2 text-xs font-bold uppercase tracking-widest text-grey-text">
            Inventário Arbóreo
          </p>
          <h1 class="text-2xl font-bold text-amma-green-dark">Ficha técnica</h1>
          <template v-if="arvore">
            <p class="mt-2 text-sm font-semibold text-grey-text">{{ arvore.codigo }}</p>
            <h2 class="mt-1 break-words text-xl font-bold text-amma-green-dark">
              {{ arvore.especie.nomePopular }}
            </h2>
            <p class="break-words text-sm italic text-grey-text">
              {{ arvore.especie.nomeCientifico }}
            </p>
            <span
              class="mt-3 inline-flex rounded-full border border-amma-green-light bg-amma-green-lighter px-3 py-1 text-sm font-semibold text-amma-green-dark"
            >
              {{ rotulosCondicao[arvore.condicaoFitossanitaria] }}
            </span>
          </template>
          <p v-else class="mt-1 break-words text-sm text-grey-text">
            Identificador solicitado: {{ identificador }}
          </p>
        </div>
        <RouterLink
          v-if="arvore"
          :to="{ name: 'inventario-editar', params: { id: arvore.id } }"
          class="flex shrink-0 items-center justify-center gap-2 rounded-btn border border-amma-green px-5 py-3 text-sm font-bold text-amma-green-dark transition-colors hover:bg-amma-green-lighter focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-amma-green"
        >
          <span aria-hidden="true" class="material-symbols-outlined text-[20px]">edit</span>
          Ir para edição
        </RouterLink>
      </header>

      <InventarioEstado
        v-if="carregando"
        estado="carregando"
        titulo="Carregando ficha técnica"
        :mensagem="`Consultando a árvore ${identificador}…`"
      />

      <section v-else-if="erro" class="flex flex-col gap-4">
        <InventarioEstado estado="erro" :titulo="tituloErro" :mensagem="mensagemErro" />
        <div class="flex flex-col gap-3 sm:flex-row">
          <button
            v-if="podeTentarNovamente"
            type="button"
            class="rounded-btn border border-feedback-danger px-5 py-3 text-sm font-bold text-feedback-danger focus-visible:outline-2 focus-visible:outline-offset-2"
            @click="recarregar"
          >
            Tentar novamente
          </button>
          <RouterLink
            :to="{ name: 'inventario' }"
            class="rounded-btn border border-amma-green-light px-5 py-3 text-center text-sm font-bold text-amma-green-dark hover:bg-amma-green-lighter focus-visible:outline-2 focus-visible:outline-amma-green"
          >
            Voltar ao inventário
          </RouterLink>
        </div>
      </section>

      <template v-else-if="arvore">
        <section
          aria-labelledby="identificacao-titulo"
          class="min-w-0 rounded-box border border-amma-green-light bg-white p-5 shadow-sm md:p-8"
        >
          <h2
            id="identificacao-titulo"
            class="mb-5 flex items-center gap-2 text-lg font-bold text-amma-green-dark"
          >
            <span aria-hidden="true" class="material-symbols-outlined">eco</span>
            Identificação botânica
          </h2>
          <dl class="grid min-w-0 grid-cols-1 gap-5 sm:grid-cols-2 lg:grid-cols-3">
            <div>
              <dt class="text-sm font-semibold text-grey-text">Código da árvore</dt>
              <dd class="mt-1 break-words text-sm font-bold text-amma-green-dark">
                {{ arvore.codigo }}
              </dd>
            </div>
            <div>
              <dt class="text-sm font-semibold text-grey-text">Nome popular</dt>
              <dd class="mt-1 break-words text-sm text-grey-text">
                {{ arvore.especie.nomePopular }}
              </dd>
            </div>
            <div>
              <dt class="text-sm font-semibold text-grey-text">Nome científico</dt>
              <dd class="mt-1 break-words text-sm italic text-grey-text">
                {{ arvore.especie.nomeCientifico }}
              </dd>
            </div>
            <div>
              <dt class="text-sm font-semibold text-grey-text">Família botânica</dt>
              <dd class="mt-1 break-words text-sm text-grey-text">
                {{ arvore.especie.familiaBotanica || 'Não informado' }}
              </dd>
            </div>
          </dl>
        </section>

        <section
          aria-labelledby="localizacao-titulo"
          class="min-w-0 rounded-box border border-amma-green-light bg-white p-5 shadow-sm md:p-8"
        >
          <h2
            id="localizacao-titulo"
            class="mb-5 flex items-center gap-2 text-lg font-bold text-amma-green-dark"
          >
            <span aria-hidden="true" class="material-symbols-outlined">location_on</span>
            Localização
          </h2>
          <div class="grid min-w-0 grid-cols-1 gap-6 lg:grid-cols-2">
            <InventarioMapa
              v-model:selecionada-id="arvoreSelecionadaNoMapa"
              :arvores="arvoresDoMapa"
              :centro-inicial="centroMapa"
              :zoom-inicial="16"
              mensagem-vazia="Não há coordenadas válidas para exibir esta árvore no mapa."
            />
            <dl class="grid min-w-0 grid-cols-1 gap-5 sm:grid-cols-2">
              <div>
                <dt class="text-sm font-semibold text-grey-text">Latitude</dt>
                <dd class="mt-1 break-words text-sm text-grey-text">
                  {{ formatarNumero(arvore.latitude) }}
                </dd>
              </div>
              <div>
                <dt class="text-sm font-semibold text-grey-text">Longitude</dt>
                <dd class="mt-1 break-words text-sm text-grey-text">
                  {{ formatarNumero(arvore.longitude) }}
                </dd>
              </div>
              <div>
                <dt class="text-sm font-semibold text-grey-text">Situação territorial</dt>
                <dd class="mt-1 break-words text-sm text-grey-text">
                  {{ rotulosTerritorio[arvore.classificacaoTerritorial] }}
                </dd>
              </div>
              <div>
                <dt class="text-sm font-semibold text-grey-text">
                  {{ arvore.zonaRural === true || arvore.classificacaoTerritorial === 'FORA_DA_AREA_URBANA' ? 'Localidade rural' : 'Bairro' }}
                </dt>
                <dd class="mt-1 break-words text-sm text-grey-text">
                  {{ arvore.zonaRural === true || arvore.classificacaoTerritorial === 'FORA_DA_AREA_URBANA' ? arvore.localidadeRural || 'Não informado' : arvore.bairroNome || arvore.bairro?.nome || 'Não informado' }}
                </dd>
              </div>
              <div>
                <dt class="text-sm font-semibold text-grey-text">CEP</dt>
                <dd class="mt-1 break-words text-sm text-grey-text">
                  {{ arvore.cep || 'Não informado' }}
                </dd>
              </div>
              <div class="sm:col-span-2">
                <dt class="text-sm font-semibold text-grey-text">Logradouro ou referência</dt>
                <dd class="mt-1 break-words text-sm text-grey-text">
                  {{ arvore.referenciaLocalizacao || 'Não informado' }}
                </dd>
              </div>
            </dl>
          </div>
        </section>

        <section
          aria-labelledby="tecnicos-titulo"
          class="min-w-0 rounded-box border border-amma-green-light bg-white p-5 shadow-sm md:p-8"
        >
          <h2
            id="tecnicos-titulo"
            class="mb-5 flex items-center gap-2 text-lg font-bold text-amma-green-dark"
          >
            <span aria-hidden="true" class="material-symbols-outlined">straighten</span>
            Características técnicas
          </h2>
          <dl class="grid grid-cols-1 gap-5 sm:grid-cols-2 lg:grid-cols-3">
            <div>
              <dt class="text-sm font-semibold text-grey-text">Data de plantio</dt>
              <dd class="mt-1 text-sm text-grey-text">{{ formatarData(arvore.dataPlantio) }}</dd>
            </div>
            <div>
              <dt class="text-sm font-semibold text-grey-text">DAP</dt>
              <dd class="mt-1 text-sm text-grey-text">
                {{ formatarNumero(arvore.dapCm) }}<span v-if="arvore.dapCm !== null"> cm</span>
              </dd>
            </div>
            <div>
              <dt class="text-sm font-semibold text-grey-text">Altura estimada</dt>
              <dd class="mt-1 text-sm text-grey-text">
                {{ formatarNumero(arvore.alturaEstimadaM)
                }}<span v-if="arvore.alturaEstimadaM !== null"> m</span>
              </dd>
            </div>
            <div>
              <dt class="text-sm font-semibold text-grey-text">Condição fitossanitária</dt>
              <dd class="mt-1 text-sm text-grey-text">
                {{ rotulosCondicao[arvore.condicaoFitossanitaria] }}
              </dd>
            </div>
          </dl>
        </section>

        <section
          aria-labelledby="observacoes-titulo"
          class="min-w-0 rounded-box border border-amma-green-light bg-white p-5 shadow-sm md:p-8"
        >
          <h2 id="observacoes-titulo" class="mb-3 text-lg font-bold text-amma-green-dark">
            Observações
          </h2>
          <p class="whitespace-pre-wrap break-words text-sm text-grey-text">
            {{ arvore.observacoes || 'Não informado' }}
          </p>
        </section>

        <ArvoreFotografias :arvore-id="arvore.id" permitir-upload />
      </template>

      <RouterLink
        :to="{ name: 'inventario' }"
        class="flex items-center justify-center gap-2 self-start rounded-btn border border-amma-green-light px-5 py-3 text-sm font-bold text-amma-green-dark transition-colors hover:bg-amma-green-lighter focus-visible:outline-2 focus-visible:outline-amma-green"
      >
        <span aria-hidden="true" class="material-symbols-outlined text-[18px]">arrow_back</span>
        Voltar ao inventário
      </RouterLink>
    </div>
  </div>
</template>
