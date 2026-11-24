import { createRouter, createWebHistory, type RouterHistory, type RouteRecordRaw } from 'vue-router'
import { sessionNavigation } from './sessionNavigation'
import PublicLayout from '@/layouts/PublicLayout.vue'
import AuthenticatedLayout from '@/layouts/AuthenticatedLayout.vue'
import HomeView from '@/views/public/HomeView.vue'
import { useAuthStore } from '@/stores/authStore'

const routes: RouteRecordRaw[] = [
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
          path: 'eventos',
          name: 'eventos-publico',
          component: () => import('@/views/public/eventos/EventosPublicoView.vue')
        },
        {
          path: 'eventos/:id',
          name: 'eventos-publico-detalhes',
          component: () => import('@/views/public/eventos/EventoPublicoDetalhesView.vue')
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
          component: () => import('@/views/public/arborizacao/ArborizacaoInicioView.vue')
        },
        {
          path: 'arborizacao/substituicao-nim',
          name: 'arborizacao-substituicao-nim',
          component: () => import('@/views/public/arborizacao/SolicitarSubstituicaoView.vue')
        },
        {
          path: 'arborizacao/substituicao-nim/acompanhar',
          name: 'arborizacao-substituicao-acompanhar',
          component: () => import('@/views/public/arborizacao/AcompanharSubstituicaoView.vue')
        },
        {
          path: 'arborizacao/assistente',
          name: 'arborizacao-assistente',
          component: () => import('@/views/public/arborizacao/AssistentePlantioView.vue'),
          meta: { requiresAuth: true }
        },
        {
          path: 'arborizacao/recomendacoes',
          name: 'arborizacao-recomendacoes',
          component: () => import('@/views/public/arborizacao/RecomendacoesView.vue'),
          meta: { requiresAuth: true }
        },
        {
          path: 'arborizacao/solicitar-muda',
          name: 'solicitar-muda',
          component: () => import('@/views/public/arborizacao/SolicitarMudaView.vue'),
          meta: { requiresAuth: true }
        },
        {
          path: 'arborizacao/conversao',
          name: 'arborizacao-conversao',
          component: () => import('@/views/public/arborizacao/ConversaoMudaView.vue'),
          meta: { requiresAuth: true }
        },
        {
          path: 'arborizacao/acompanhar',
          name: 'arborizacao-acompanhar',
          component: () => import('@/views/public/arborizacao/AcompanharSolicitacaoView.vue'),
          meta: { requiresAuth: true }
        },
        {
          path: 'dashboard',
          name: 'cidadao-dashboard',
          component: () => import('@/views/public/CidadaoDashboardView.vue'),
          meta: { requiresAuth: true, requiresCitizen: true, title: 'Dashboard' }
        },
        {
          path: 'perfil',
          name: 'perfil',
          component: () => import('@/views/public/PerfilView.vue'),
          meta: { requiresAuth: true, requiresCitizen: true, title: 'Meu Perfil' }
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
          path: 'perfil',
          name: 'perfil-institucional',
          component: () => import('@/views/auth/PerfilInstitucionalView.vue'),
          meta: { title: 'Meu Perfil' }
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
          meta: { title: 'Inventário Arbóreo', activeMenu: 'inventario' }
        },
        {
          path: 'inventario/nova',
          name: 'inventario-nova',
          component: () => import('@/views/auth/inventario/ArvoreFormView.vue'),
          meta: { title: 'Inventário Arbóreo', activeMenu: 'inventario' }
        },
        {
          path: 'inventario/:id/editar',
          name: 'inventario-editar',
          component: () => import('@/views/auth/inventario/ArvoreFormView.vue'),
          meta: { title: 'Inventário Arbóreo', activeMenu: 'inventario' }
        },
        {
          path: 'inventario/:id',
          name: 'inventario-detalhes',
          component: () => import('@/views/auth/inventario/ArvoreDetalhesView.vue'),
          meta: { title: 'Inventário Arbóreo', activeMenu: 'inventario' }
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
          path: 'compensacao',
          name: 'compensacao-lista',
          component: () => import('@/views/auth/compensacao/CompensacaoListaView.vue'),
          meta: { title: 'Compensação Ambiental', activeMenu: 'compensacao-lista', requiresAmmaStaff: true }
        },
        {
          path: 'compensacao/novo',
          name: 'compensacao-novo',
          component: () => import('@/views/auth/compensacao/CompensacaoFormView.vue'),
          meta: { title: 'Compensação Ambiental', activeMenu: 'compensacao-lista', requiresAmmaStaff: true }
        },
        {
          path: 'compensacao/:id',
          name: 'compensacao-detalhes',
          component: () => import('@/views/auth/compensacao/CompensacaoDetalheView.vue'),
          meta: { title: 'Compensação Ambiental', activeMenu: 'compensacao-lista', requiresAmmaStaff: true }
        },
        {
          path: 'compensacao/:id/editar',
          name: 'compensacao-editar',
          component: () => import('@/views/auth/compensacao/CompensacaoFormView.vue'),
          meta: { title: 'Compensação Ambiental', activeMenu: 'compensacao-lista', requiresAmmaStaff: true }
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

export function createAppRouter(history: RouterHistory = createWebHistory(import.meta.env.BASE_URL)) {
  const router = createRouter({ history, routes })
  router.beforeEach((to) => sessionNavigation(to, useAuthStore()))
  return router
}

export default createAppRouter()
