import { onMounted, useTemplateRef, watch } from 'vue'
import { useRoute } from 'vue-router'

// O painel rola o <main>, não a janela. Limita o ajuste às novas telas do inventário.
export function useInventarioNavegacao() {
  const pagina = useTemplateRef<HTMLElement>('pagina')
  const route = useRoute()
  const voltarAoTopo = () => pagina.value?.closest('main')?.scrollTo({ top: 0 })
  onMounted(voltarAoTopo)
  watch(() => route.fullPath, voltarAoTopo, { flush: 'post' })
}
