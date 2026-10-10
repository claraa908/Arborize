import { createRouter, createWebHistory } from 'vue-router'
import PublicLayout from '@/layouts/PublicLayout.vue'
import AuthenticatedLayout from '@/layouts/AuthenticatedLayout.vue'
import HomeView from '@/views/public/HomeView.vue'

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
        }
      ]
    },
    {
      path: '/sistema',
      component: AuthenticatedLayout,
      children: [
        {
          path: '',
          name: 'dashboard',
          component: () => import('@/views/auth/DashboardView.vue'),
          meta: { title: 'Dashboard e Relatórios' }
        },
        {
          path: 'solicitacoes',
          name: 'solicitacoes',
          component: () => import('@/views/auth/SolicitacoesView.vue'),
          meta: { title: 'Solicitações de Mudas' }
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
          component: () => import('@/views/auth/ViveiroView.vue'),
          meta: { title: 'Gestão do Viveiro' }
        },
        {
          path: 'laudos',
          name: 'laudos',
          component: () => import('@/views/auth/LaudosView.vue'),
          meta: { title: 'Laudos Técnicos' }
        },
        {
          path: 'podas',
          name: 'podas',
          component: () => import('@/views/auth/PodasView.vue'),
          meta: { title: 'Cronograma de Podas' }
        },
        {
          path: 'eventos',
          name: 'eventos',
          component: () => import('@/views/auth/EventosView.vue'),
          meta: { title: 'Eventos Ambientais' }
        },
        {
          path: 'perfil',
          name: 'perfil',
          component: () => import('@/views/auth/PerfilView.vue'),
          meta: { title: 'Meu Perfil' }
        }
      ]
    }
  ]
})

export default router