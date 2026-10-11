<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const searchQuery = ref('')

const documentos = ref([
  { id: '1', titulo: 'Guia de Plantio Urbano', categoria: 'Manuais', status: 'PUBLICADO', data: '10/10/2026' },
  { id: '2', titulo: 'Cartilha de Preservação da Caatinga', categoria: 'Cartilhas', status: 'RASCUNHO', data: '11/10/2026' },
  { id: '3', titulo: 'Manual de Poda Segura', categoria: 'Manuais', status: 'PUBLICADO', data: '05/10/2026' }
])

const documentosFiltrados = computed(() => {
  if (!searchQuery.value) return documentos.value
  return documentos.value.filter(doc => 
    doc.titulo.toLowerCase().includes(searchQuery.value.toLowerCase()) ||
    doc.categoria.toLowerCase().includes(searchQuery.value.toLowerCase())
  )
})

const navigateToNew = () => router.push({ name: 'biblioteca-nova' })
const viewDetails = (id: string) => router.push({ name: 'biblioteca-detalhes', params: { id } })
</script>

<template>
  <div class="px-6 md:px-10 pt-6 md:pt-6 flex flex-col gap-6">
    <div class="flex flex-col md:flex-row justify-between items-start md:items-center gap-4">
      <div class="relative w-full md:w-96">
        <span class="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-grey-text/60">search</span>
        <input 
          v-model="searchQuery" 
          type="text" 
          placeholder="Pesquisar por título ou categoria..." 
          class="w-full bg-white focus:bg-amma-green-lighter border-2 border-amma-green-light focus:border-amma-green rounded-btn py-2.5 pl-10 pr-4 text-grey-text outline-none transition-all text-sm font-medium"
        />
      </div>
      
      <button @click="navigateToNew" class="w-full md:w-auto bg-white hover:bg-amma-green hover:text-white text-amma-green font-bold py-2.5 px-6 rounded-btn text-sm border-2 border-amma-green outline-none efeito-zoom flex items-center justify-center gap-2">
        <span class="material-symbols-outlined text-[20px]">add</span>
        Novo Documento
      </button>
    </div>
    
    <div class="bg-white rounded-btn shadow-sm border border-amma-green-light overflow-hidden">
      <div class="overflow-x-auto">
        <table class="w-full text-left border-collapse">
          <thead>
            <tr class="bg-amma-bg border-b border-amma-green-light text-amma-green-dark text-sm">
              <th class="py-4 px-6 font-bold w-2/5">Título do Documento</th>
              <th class="py-4 px-6 font-bold">Categoria</th>
              <th class="py-4 px-6 font-bold text-center">Status</th>
              <th class="py-4 px-6 font-bold text-center">Data</th>
              <th class="py-4 px-6 font-bold text-center">Ações</th>
            </tr>
          </thead>
          
          <tbody v-if="documentosFiltrados.length > 0">
            <tr 
              v-for="doc in documentosFiltrados" 
              :key="doc.id"
              class="border-b border-amma-green-light/50 hover:bg-amma-green-lighter/30 transition-colors"
            >
              <td class="py-4 px-6">
                <span class="font-semibold text-grey-text block">{{ doc.titulo }}</span>
              </td>
              <td class="py-4 px-6">
                <span class="text-sm font-medium text-grey-text/80 bg-black/5 px-3 py-1 rounded-full">{{ doc.categoria }}</span>
              </td>
              <td class="py-4 px-6 text-center">
                <span 
                  :class="[ 
                    'text-xs font-bold px-3 py-1.5 rounded-full inline-block min-w-[100px]',
                    doc.status === 'PUBLICADO' ? 'bg-amma-green/20 text-amma-green-dark' : 'bg-orange-100 text-orange-700'
                  ]"
                >
                  {{ doc.status }}
                </span>
              </td>
              <td class="py-4 px-6 text-center">
                <span class="text-sm font-medium text-grey-text/80">{{ doc.data }}</span>
              </td>
              <td class="py-4 px-6 text-center">
                <div class="flex items-center justify-center gap-2">
                  <button 
                    @click="viewDetails(doc.id)"
                    class="w-8 h-8 flex items-center justify-center text-amma-green hover:bg-amma-green-lighter rounded-full transition-colors efeito-zoom" 
                    title="Visualizar Detalhes"
                  >
                    <span class="material-symbols-outlined">list_alt</span>
                  </button>
                  <button 
                    class="w-8 h-8 flex items-center justify-center text-grey-text hover:text-amma-green-dark hover:bg-amma-green-lighter rounded-full transition-colors efeito-zoom" 
                    title="Editar Documento"
                  >
                    <span class="material-symbols-outlined text-[20px]">edit</span>
                  </button>
                </div>
              </td>
            </tr>
          </tbody>

          <tbody v-else>
            <tr>
              <td colspan="5" class="py-12 px-6 text-center">
                <span class="material-symbols-outlined text-4xl text-grey-text/30 mb-2">find_in_page</span>
                <p class="text-grey-text/60 font-medium">Nenhum documento encontrado.</p>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
      
      <div class="flex items-center justify-between px-6 py-4 border-t border-amma-green-light bg-white">
        <span class="text-sm font-medium text-grey-text/70">Mostrando {{ documentosFiltrados.length }} registos</span>
        <div class="flex gap-2">
          <button class="w-8 h-8 flex items-center justify-center rounded-btn-l-small border border-amma-green text-amma-green hover:bg-amma-green-light transition-colors disabled:opacity-50 efeito-zoom" disabled>
            <span class="material-symbols-outlined text-[18px]">chevron_left</span>
          </button>
          <button class="w-8 h-8 flex items-center justify-center rounded-btn-r-small border border-amma-green text-amma-green hover:bg-amma-green-light transition-colors disabled:opacity-50 efeito-zoom" disabled>
            <span class="material-symbols-outlined text-[18px]">chevron_right</span>
          </button>
        </div>
      </div>
    </div>
  </div>
</template>