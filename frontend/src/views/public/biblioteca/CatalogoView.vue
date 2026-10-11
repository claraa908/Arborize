<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import DocumentoCard from '@/components/biblioteca/DocumentoCard.vue'
import capaBanner from '@/assets/images/capa-biblioteca.png'

const router = useRouter()
const searchQuery = ref('')
const selectedCategory = ref('Todas')
const rootEl = ref<HTMLElement | null>(null)
const isScrolled = ref(false)

let scrollContainer: HTMLElement | Window | null = null
let isTicking = false

const categorias = ['Todas', 'Arborização Urbana', 'Caatinga', 'Educação Ambiental', 'Legislação']

const documentos = ref([
  { 
    id: '1', 
    titulo: 'Guia de Plantio Urbano 2026', 
    descricao: 'Manual prático para plantio de mudas em calçadas e praças, respeitando o espaçamento ideal e fiações.', 
    categoria: 'Arborização Urbana', 
    tipo: 'Manual', 
    data: '10/10/2026', 
    tamanho: '2.4 MB',
    capaUrl: 'https://images.unsplash.com/photo-1542601906990-b4d3fb778b09?w=600&auto=format&fit=crop&q=80'
  },
  { 
    id: '2', 
    titulo: 'Manual de Poda Segura', 
    descricao: 'Orientações técnicas para poda e manutenção preventiva sem risco à rede elétrica e à saúde vegetal.', 
    categoria: 'Arborização Urbana', 
    tipo: 'Manual', 
    data: '05/10/2026', 
    tamanho: '1.8 MB',
    capaUrl: 'https://images.unsplash.com/photo-1502082553048-f009c37129b9?w=600&auto=format&fit=crop&q=80'
  },
  { 
    id: '3', 
    titulo: 'Flora e Biodiversidade da Caatinga', 
    descricao: 'Catálogo ilustrado com as principais espécies nativas do semiárido e suas propriedades biológicas.', 
    categoria: 'Caatinga', 
    tipo: 'Cartilha', 
    data: '01/10/2026', 
    tamanho: '5.1 MB',
    capaUrl: 'https://images.unsplash.com/photo-1509316975850-ff9c5deb0cd9?w=600&auto=format&fit=crop&q=80'
  },
  { 
    id: '4', 
    titulo: 'Importância das Abelhas Nativas', 
    descricao: 'Material educativo sobre a polinização por abelhas sem ferrão no ecossistema e ambiente urbano.', 
    categoria: 'Educação Ambiental', 
    tipo: 'Cartilha', 
    data: '28/09/2026', 
    tamanho: '3.2 MB',
    capaUrl: 'https://images.unsplash.com/photo-1587049352846-4a222e784d38?w=600&auto=format&fit=crop&q=80'
  },
  { 
    id: '5', 
    titulo: 'Código Municipal de Meio Ambiente', 
    descricao: 'Texto integral consolidado da legislação ambiental municipal, normas de licenciamento e fiscalização.', 
    categoria: 'Legislação', 
    tipo: 'Relatório Técnico', 
    data: '22/09/2026', 
    tamanho: '4.7 MB'
  },
  { 
    id: '6', 
    titulo: 'Recuperação de Nascentes e Matas Ciliares', 
    descricao: 'Técnicas de bioengenharia e replantio para preservação de corpos hídricos e microbacias.', 
    categoria: 'Caatinga', 
    tipo: 'Guia', 
    data: '15/09/2026', 
    tamanho: '3.9 MB',
    capaUrl: 'https://images.unsplash.com/photo-1448375240586-882707db888b?w=600&auto=format&fit=crop&q=80'
  },
  { 
    id: '7', 
    titulo: 'Hortas Comunitárias e Agroecologia', 
    descricao: 'Passo a passo para implementação e manejo orgânico de hortas urbanas em escolas e bairros.', 
    categoria: 'Educação Ambiental', 
    tipo: 'Cartilha', 
    data: '10/09/2026', 
    tamanho: '2.1 MB',
    capaUrl: 'https://www.zapimoveis.com.br/blog/wp-content/uploads/2025/09/horta-no-quintal-de-casa-1.jpg'
  },
  { 
    id: '8', 
    titulo: 'Diretrizes de Arborização Viária', 
    descricao: 'Critérios botânicos para escolha de espécies adequadas ao canteiro central e passeios públicos.', 
    categoria: 'Arborização Urbana', 
    tipo: 'Manual', 
    data: '02/09/2026', 
    tamanho: '3.5 MB',
    capaUrl: 'https://images.unsplash.com/photo-1473448912268-2022ce9509d8?w=600&auto=format&fit=crop&q=80'
  },
  { 
    id: '9', 
    titulo: 'Manual de Compostagem Doméstica', 
    descricao: 'Como transformar resíduos orgânicos em adubo fértil utilizando composteiras caseiras simples.', 
    categoria: 'Educação Ambiental', 
    tipo: 'Guia', 
    data: '27/08/2026', 
    tamanho: '1.5 MB'
  },
  { 
    id: '10', 
    titulo: 'Normas de Compensação Ambiental', 
    descricao: 'Procedimentos legais e parâmetros de cálculo para mitigação e compensação de supressão vegetal.', 
    categoria: 'Legislação', 
    tipo: 'Relatório Técnico', 
    data: '20/08/2026', 
    tamanho: '2.8 MB',
    capaUrl: 'https://images.unsplash.com/photo-1464822759023-fed622ff2c3b?w=600&auto=format&fit=crop&q=80'
  },
  { 
    id: '11', 
    titulo: 'Sementes da Caatinga: Coleta e Beneficiamento', 
    descricao: 'Guia prático sobre época correta de colheita, quebra de dormência e armazenamento de sementes.', 
    categoria: 'Caatinga', 
    tipo: 'Guia', 
    data: '12/08/2026', 
    tamanho: '4.2 MB',
    capaUrl: 'https://images.unsplash.com/photo-1518531933037-91b2f5f229cc?w=600&auto=format&fit=crop&q=80'
  },
  { 
    id: '12', 
    titulo: 'Inventário Florestal Urbano', 
    descricao: 'Mapeamento fitossociológico e diagnóstico da cobertura vegetal das principais praças da cidade.', 
    categoria: 'Arborização Urbana', 
    tipo: 'Relatório Técnico', 
    data: '05/08/2026', 
    tamanho: '6.8 MB',
    capaUrl: 'https://images.unsplash.com/photo-1511497584788-87676104235f?w=600&auto=format&fit=crop&q=80'
  }
])

const documentosFiltrados = computed(() => {
  return documentos.value.filter(doc => {
    const matchPesquisa = doc.titulo.toLowerCase().includes(searchQuery.value.toLowerCase()) || doc.descricao.toLowerCase().includes(searchQuery.value.toLowerCase())
    const matchCategoria = selectedCategory.value === 'Todas' || doc.categoria === selectedCategory.value
    return matchPesquisa && matchCategoria
  })
})

const viewDocument = (id: string) => {
  router.push({ name: 'biblioteca-documento', params: { id } })
}

const handleScroll = (e: Event) => {
  if (isTicking) return
  isTicking = true

  requestAnimationFrame(() => {
    const target = e.target as HTMLElement
    const currentScroll = target ? target.scrollTop : window.scrollY

    if (currentScroll > 60 && !isScrolled.value) {
      isScrolled.value = true
    } else if (currentScroll < 20 && isScrolled.value) {
      isScrolled.value = false
    }

    isTicking = false
  })
}

onMounted(() => {
  const main = rootEl.value?.closest('main')
  if (main) {
    scrollContainer = main
    main.style.overflowAnchor = 'none'
    main.addEventListener('scroll', handleScroll, { passive: true })
  } else {
    scrollContainer = window
    window.addEventListener('scroll', handleScroll, { passive: true })
  }
})

onUnmounted(() => {
  if (scrollContainer) {
    scrollContainer.removeEventListener('scroll', handleScroll as EventListener)
  }
})
</script>

<template>
  <div ref="rootEl" class="max-w-7xl mx-auto flex flex-col gap-6 relative pb-10" style="overflow-anchor: none;">
    
    <div class="sticky top-0 -mt-2 -mx-6 md:-mx-10 px-6 md:px-10 z-30 pt-1 pb-4 bg-white/70 backdrop-blur-md" style="overflow-anchor: none;">
      
      <div 
        class="relative overflow-hidden shadow-md transition-all duration-300 ease-out"
        :class="isScrolled ? 'rounded-btn p-2.5 md:p-3' : 'rounded-box py-4 px-5 md:py-5 md:px-6'"
        style="isolation: isolate;"
      >
        <img 
          :src="capaBanner" 
          alt="Capa Biblioteca Digital" 
          class="absolute inset-0 w-full h-full object-cover rounded-[inherit] pointer-events-none select-none blur-[2px] scale-105"
        />

        <div 
          class="absolute inset-0 rounded-[inherit] pointer-events-none transition-all duration-300 ease-out"
          :class="isScrolled ? 'bg-white/80 backdrop-blur-xl border border-white/70 shadow-sm' : 'bg-transparent border-transparent'"
        ></div>

        <div 
          class="relative z-10 transition-all duration-300 ease-out flex"
          :class="isScrolled ? 'flex-col lg:flex-row lg:items-center lg:justify-between gap-3' : 'flex-col items-center gap-3 md:gap-4'"
        >
          <div 
            class="transition-all duration-300 ease-out flex"
            :class="isScrolled ? 'items-center gap-3 shrink-0' : 'flex-col items-center text-center'"
          >
            <div 
              class="bg-white/95 rounded-full flex items-center justify-center border-2 border-white shadow-md transition-all duration-300 ease-out shrink-0"
              :class="isScrolled ? 'w-10 h-10 shadow-sm' : 'w-12 h-12 md:w-14 md:h-14 mb-1'"
            >
              <span 
                class="material-symbols-outlined text-amma-green transition-all duration-300 ease-out select-none !leading-none"
                :style="{ fontSize: isScrolled ? '22px' : '32px' }"
              >
                local_library
              </span>
            </div>

            <div :class="isScrolled ? 'flex flex-col' : 'flex flex-col items-center'">
              <h1 
                class="font-black text-amma-green-dark transition-all duration-300 ease-out leading-tight drop-shadow-sm tracking-tight"
                :class="isScrolled ? 'text-lg md:text-xl' : 'text-xl md:text-2xl lg:text-3xl mb-1'"
              >
                Biblioteca Digital
              </h1>
              
              <div 
                class="transition-all duration-300 ease-out"
                :class="isScrolled ? 'hidden' : 'block max-w-2xl mx-auto text-center'"
              >
                <p class="inline-block bg-white/30 backdrop-blur-md px-4 py-1.5 rounded-full text-gray-800 text-xs md:text-sm font-semibold shadow-sm border border-white/50">
                  Explore cartilhas, manuais e materiais educativos sobre a arborização e a Caatinga disponibilizados publicamente pela AMMA.
                </p>
              </div>
            </div>
          </div>

          <div 
            class="transition-all duration-300 ease-out flex flex-col md:flex-row items-center gap-2.5 rounded-btn"
            :class="[
              isScrolled 
                ? 'p-0 flex-1 lg:max-w-3xl lg:justify-end bg-transparent border-transparent shadow-none backdrop-blur-none' 
                : 'p-2.5 md:p-3 w-full justify-between bg-white/30 backdrop-blur-md border border-white/50 shadow-[0_8px_24px_rgba(0,0,0,0.06)]'
            ]"
          >
            <div 
              class="relative w-full transition-all duration-300 ease-out"
              :class="isScrolled ? 'md:w-64 lg:w-72' : 'md:w-1/2'"
            >
              <span class="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-amma-green-dark font-bold text-[20px] select-none">
                search
              </span>
              <input 
                v-model="searchQuery" 
                type="text" 
                placeholder="O que procura aprender hoje?" 
                class="w-full bg-white text-gray-800 placeholder-gray-500 border-2 border-white focus:border-amma-green rounded-btn pl-10 pr-4 outline-none transition-all text-sm font-medium shadow-sm"
                :class="isScrolled ? 'py-1.5' : 'py-2'"
              />
            </div>
            
            <div class="w-full md:w-auto overflow-x-auto pb-1 md:pb-0 hide-scrollbar flex gap-1.5 shrink-0">
              <button 
                v-for="cat in categorias" 
                :key="cat"
                @click="selectedCategory = cat"
                :class="[
                  'whitespace-nowrap rounded-btn font-bold transition-all border-2 text-xs shadow-sm',
                  isScrolled ? 'px-3 py-1.5' : 'px-3.5 py-1.5 text-xs',
                  selectedCategory === cat 
                    ? 'bg-amma-green text-white border-amma-green shadow-md' 
                    : 'bg-white text-gray-700 border-white hover:border-amma-green hover:text-amma-green'
                ]"
              >
                {{ cat }}
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
    
    <div v-if="documentosFiltrados.length > 0" class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6 pt-4">
      <DocumentoCard 
        v-for="doc in documentosFiltrados" 
        :key="doc.id"
        :documento="doc"
        @click="viewDocument"
      />
    </div>
    
    <div v-else class="bg-white rounded-box shadow-sm border border-amma-green-light p-12 flex flex-col items-center justify-center text-center mt-4">
      <span class="material-symbols-outlined text-6xl text-grey-text/30 mb-4">search_off</span>
      <h3 class="text-xl font-bold text-amma-green-dark mb-2">Nenhum documento encontrado</h3>
      <p class="text-grey-text font-medium">Tente alterar os termos da pesquisa ou selecione outra categoria.</p>
      <button 
        @click="searchQuery = ''; selectedCategory = 'Todas'"
        class="mt-6 bg-amma-green-lighter hover:bg-amma-green text-amma-green-dark hover:text-white font-bold py-2.5 px-6 rounded-btn transition-colors text-sm"
      >
        Limpar Filtros
      </button>
    </div>

  </div>
</template>

<style scoped>
.hide-scrollbar::-webkit-scrollbar {
  display: none;
}
.hide-scrollbar {
  -ms-overflow-style: none;
  scrollbar-width: none;
}
</style>