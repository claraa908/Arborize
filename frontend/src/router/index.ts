import { createRouter, createWebHistory } from 'vue-router'
import PublicLayout from '@/layouts/PublicLayout.vue'
import AuthenticatedLayout from '@/layouts/AuthenticatedLayout.vue'
import HomeView from '@/views/public/HomeView.vue'
import { useAssistentePlantioStore } from '@/stores/assistentePlantioStore'
import { useSolicitacaoMudaStore } from '@/stores/solicitacaoMudaStore'
import { useAuthStore } from '@/stores/authStore'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      component: PublicLayout,
      children: [
        {
          path: '',
          name: 'home',
          component: HomeView
        },
        {
          path: 'biblioteca',
          name: 'biblioteca-catalogo',
          component: () => import('@/views/public/biblioteca/CatalogoView.vue')
        },
        {
          path: 'biblioteca/:id',
          name: 'biblioteca-documento',
          component: () => import('@/views/public/biblioteca/DocumentoView.vue')
        },
        {
          path: 'arborizacao',
          name: 'arborizacao',
          redirect: () => {
            const quizStore = useAssistentePlantioStore()
            const requestStore = useSolicitacaoMudaStore()

            if (requestStore.resultado) return { name: 'arborizacao-acompanhar' }
            if (requestStore.form.especieId > 0) {
              return { name: 'solicitar-muda', query: { especieId: requestStore.form.especieId } }
            }
            if (quizStore.recomendacoes !== null) return { name: 'arborizacao-recomendacoes' }
            return { name: 'arborizacao-assistente' }
          }
        },
        {
          path: 'arborizacao/assistente',
          name: 'arborizacao-assistente',
          component: () => import('@/views/public/arborizacao/AssistentePlantioView.vue')
        },
        {
          path: 'arborizacao/recomendacoes',
          name: 'arborizacao-recomendacoes',
          component: () => import('@/views/public/arborizacao/RecomendacoesView.vue')
        },
        {
          path: 'arborizacao/solicitar-muda',
          name: 'solicitar-muda',
          component: () => import('@/views/public/arborizacao/SolicitarMudaView.vue')
        },
        {
          path: 'arborizacao/conversao',
          name: 'arborizacao-conversao',
          component: () => import('@/views/public/arborizacao/ConversaoMudaView.vue')
        },
        {
          path: 'arborizacao/acompanhar',
          name: 'arborizacao-acompanhar',
          component: () => import('@/views/public/arborizacao/AcompanharSolicitacaoView.vue')
        },
        {
          path: 'perfil',
          name: 'perfil',
          component: () => import('@/views/public/PerfilView.vue'),
          meta: { requiresAuth: true, title: 'Meu Perfil' }
        }
      ]
    },
    {
      path: '/sistema',
      component: AuthenticatedLayout,
      meta: { requiresManagement: true },
      children: [
        {
          path: '',
          name: 'dashboard',
          component: () => import('@/views/auth/DashboardView.vue'),
          meta: { title: 'Dashboard e Relatórios' }
        },
        {
          path: 'biblioteca',
          name: 'biblioteca-lista',
          component: () => import('@/views/auth/biblioteca/BibliotecaListaView.vue'),
          meta: { title: 'Biblioteca Digital', activeMenu: 'biblioteca-lista' }
        },
        {
          path: 'biblioteca/nova',
          name: 'biblioteca-nova',
          component: () => import('@/views/auth/biblioteca/BibliotecaFormView.vue'),
          meta: { title: 'Biblioteca Digital', activeMenu: 'biblioteca-lista' }
        },
        {
          path: 'biblioteca/:id/editar',
          name: 'biblioteca-editar',
          component: () => import('@/views/auth/biblioteca/BibliotecaFormView.vue'),
          meta: { title: 'Biblioteca Digital', activeMenu: 'biblioteca-lista' }
        },
        {
          path: 'biblioteca/:id',
          name: 'biblioteca-detalhes',
          component: () => import('@/views/auth/biblioteca/BibliotecaDetalhesView.vue'),
          meta: { title: 'Biblioteca Digital', activeMenu: 'biblioteca-lista' }
        },
        {
          path: 'inventario',
          name: 'inventario',
          component: () => import('@/views/auth/InventarioView.vue'),
          meta: { title: 'Inventário Arbóreo' }
        },
        {
  path: 'viveiro',
  name: 'viveiro',
  component: () => import('@/views/auth/viveiro/ViveiroView.vue'),
  meta: { title: 'Gestão do Viveiro' }
},
{
  path: 'viveiro/cadastrar-especie',
  name: 'viveiro-cadastrar',
  component: () => import('@/views/auth/viveiro/ViveiroCadastroView.vue'),
  meta: { title: 'Gestão do Viveiro', activeMenu: 'viveiro' }
},
{
  path: 'viveiro/:id/editar',
  name: 'viveiro-editar',
  component: () => import('@/views/auth/viveiro/ViveiroCadastroView.vue'),
  meta: { title: 'Gestão do Viveiro', activeMenu: 'viveiro' }
},
{
  path: 'viveiro/:id',
  name: 'viveiro-detalhes',
  component: () => import('@/views/auth/viveiro/ViveiroDetalhesView.vue'),
  meta: { title: 'Gestão do Viveiro', activeMenu: 'viveiro' }
},
        {
          path: 'solicitacoes',
          name: 'solicitacoes',
          component: () => import('@/views/auth/SolicitacoesView.vue'),
          meta: { title: 'Solicitações de Mudas' }
        },
        {
          path: 'laudos',
          name: 'laudos',
          component: () => import('@/views/auth/LaudosView.vue'),
          meta: { title: 'Laudos Técnicos' }
        },
        {
          path: 'eventos',
          name: 'eventos',
          component: () => import('@/views/auth/eventos/EventosView.vue'),
          meta: { title: 'Eventos Ambientais' }
        },
        {
          path: 'eventos/cadastrar',
          name: 'eventos-cadastrar',
          component: () => import('@/views/auth/eventos/EventosCadastroView.vue'),
          meta: { title: 'Eventos Ambientais', activeMenu: 'eventos' }
        },
        {
          path: 'eventos/:id',
          name: 'eventos-detalhes',
          component: () => import('@/views/auth/eventos/EventosDetalhesView.vue'),
          meta: { title: 'Eventos Ambientais', activeMenu: 'eventos' }
        },
        {
          path: 'eventos/:id/editar',
          name: 'eventos-editar',
          component: () => import('@/views/auth/eventos/EventosCadastroView.vue'),
          meta: { title: 'Eventos Ambientais', activeMenu: 'eventos' }
        },
      ]
    },
    {
      path: '/gestao',
      component: AuthenticatedLayout,
      meta: { requiresAdmin: true },
      children: [
        {
          path: '',
          name: 'gestao',
          component: () => import('@/views/auth/GestaoView.vue'),
          meta: { title: 'Painel de Gestão' }
        }
      ]
    },
    {
      path: '/ifce',
      component: AuthenticatedLayout,
      meta: { requiresIfceTeam: true },
      children: [
        {
          path: 'equipe',
          name: 'ifce-equipe',
          component: () => import('@/views/auth/GestaoIfceView.vue'),
          meta: { title: 'Equipe do Laboratório' }
        }
      ]
    }
  ]
})

router.beforeEach((to) => {
  const auth = useAuthStore()

  if (to.matched.some((record) => record.meta.requiresAdmin) && !auth.isAmmaAdmin) {
    return { name: 'home' }
  }

  if (to.matched.some((record) => record.meta.requiresIfceTeam) && !auth.canManageIfceTeam) {
    return { name: 'home' }
  }

  if (to.matched.some((record) => record.meta.requiresManagement)) {
    if (!auth.isAuthenticated) {
      return {
        name: 'home',
        query: { auth: 'required', redirect: to.fullPath }
      }
    }
    if (!auth.canAccessManagement) return { name: 'home' }
  }

  if (to.matched.some((record) => record.meta.requiresAuth) && !auth.isAuthenticated) {
    return {
      name: 'home',
      query: { auth: 'required', redirect: to.fullPath }
    }
  }
  return true
})

export default router
