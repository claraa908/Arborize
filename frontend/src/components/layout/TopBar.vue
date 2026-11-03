<script setup lang="ts">
import { ref } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/authStore'
import { useFeedbackStore } from '@/stores/feedbackStore'

interface NavLink {
  label: string
  routeName?: string
}

const emit = defineEmits<{
  (e: 'open-login'): void
  (e: 'open-register'): void
}>()

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const feedback = useFeedbackStore()
const isMobileMenuOpen = ref(false)

const navLinks: NavLink[] = [
  { label: 'Início', routeName: 'home' },
  { label: 'Eventos' },
  { label: 'Arborização', routeName: 'arborizacao' },
  { label: 'Biblioteca Digital', routeName: 'biblioteca-catalogo' },
  { label: 'Sobre Nós', routeName: 'sobre' }
]

const isLinkActive = (link: NavLink) => {
  if (!link.routeName) return false
  if (route.name === link.routeName) return true
  if (link.routeName === 'arborizacao' && route.path.startsWith('/arborizacao')) return true
  if (link.routeName === 'home' && (route.name === 'home' || route.path === '/')) return true
  return false
}

const navigateTo = (routeName?: string) => {
  if (!routeName) return
  isMobileMenuOpen.value = false
  router.push({ name: routeName }).catch(() => {
    if (routeName === 'home') {
      router.push('/')
    }
  })
}

const navigateToProfile = () => {
  isMobileMenuOpen.value = false
  void router.push({ name: 'perfil' })
}

const navigateToManagement = () => {
  isMobileMenuOpen.value = false
  void router.push({ name: 'dashboard' })
}

const handleLogout = () => {
  auth.logout()
  feedback.showToast('Sessão encerrada com sucesso.', 'info')
  isMobileMenuOpen.value = false
  void router.push({ name: 'home' })
}
</script>

<template>
  <header class="relative z-40 flex w-full items-center justify-between border-b border-gray-200 bg-white px-4 py-5 sm:px-8 sm:py-6 rounded-tl-[15px] rounded-tr-[60px]">
    <RouterLink :to="{ name: 'home' }" aria-label="Arborize — Início" class="flex shrink-0 items-center gap-2 rounded-btn focus-visible:outline focus-visible:outline-2 focus-visible:outline-amma-green">
      <div class="text-amma-grey-text flex items-center gap-2">
        <img src="@/assets/images/logoVerde.png" alt="Logo Arborize" class="h-12 w-auto sm:h-16" />
        <div class="hidden sm:flex flex-col">
          <span class="text-xl font-bold leading-none tracking-wide text-amma-green">ARBORIZE</span>
          <span class="text-[0.6rem] font-medium tracking-widest text-amma-green">Gestão Ambiental</span>
        </div>
      </div>
    </RouterLink>

    <nav aria-label="Seções públicas" class="hidden xl:flex items-center gap-4 text-sm">
      <button
        v-for="link in navLinks"
        :key="link.label"
        type="button"
        @click="navigateTo(link.routeName)"
        :class="[
          'rounded-btn transition-colors focus-visible:outline focus-visible:outline-2 focus-visible:outline-amma-green',
          isLinkActive(link)
            ? 'bg-amma-green-light text-[#4D6E1F] px-5 py-2 rounded-tl-[10px] rounded-tr-[20px] rounded-bl-[20px] rounded-br-[10px] font-bold'
            : 'text-gray-500 hover:text-gray-900 font-medium'
        ]"
      >
        {{ link.label }}
      </button>
    </nav>

    <div class="flex shrink-0 items-center gap-2 text-sm font-bold">
      <template v-if="auth.isAuthenticated">
        <button
          v-if="auth.isCitizen"
          type="button"
          @click="navigateTo('cidadao-dashboard')"
          class="inline-flex min-h-11 min-w-11 items-center justify-center gap-2 rounded-btn border-2 border-amma-green px-2.5 py-2.5 text-amma-green hover:bg-amma-green-lighter focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-amma-green lg:px-4"
          aria-label="Dashboard"
        >
          <span class="material-symbols-outlined text-[20px]" aria-hidden="true">dashboard</span>
          <span class="hidden lg:inline">Dashboard</span>
        </button>
        <button
          type="button"
          v-if="auth.isCitizen"
          @click="navigateToProfile"
          :class="[
            'inline-flex items-center gap-2 border-2 px-2.5 py-2.5 transition-colors efeito-zoom cursor-pointer focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-amma-green lg:px-5',
            route.name === 'perfil'
              ? 'border-transparent bg-amma-green-light text-amma-green-dark rounded-full'
              : 'border-amma-green text-amma-green hover:bg-amma-green-lighter rounded-btn'
          ]"
          aria-label="Meu perfil"
        >
          <span class="material-symbols-outlined text-[20px]" aria-hidden="true">person</span>
          <span class="hidden lg:inline">Meu perfil</span>
        </button>
        <button
          v-if="auth.canAccessManagement"
          type="button"
          @click="navigateToManagement"
          class="inline-flex items-center gap-2 rounded-btn border-2 border-amma-green px-2.5 py-2.5 text-amma-green transition-colors hover:bg-amma-green-lighter efeito-zoom cursor-pointer focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-amma-green lg:px-5"
          aria-label="Voltar ao painel"
        >
          <span class="material-symbols-outlined text-[20px]" aria-hidden="true">dashboard</span>
          <span class="hidden lg:inline">Voltar ao painel</span>
        </button>
        <button
          type="button"
          @click="handleLogout"
          class="inline-flex items-center gap-2 rounded-btn px-2.5 py-2.5 text-amma-green-dark hover:bg-amma-green-lighter efeito-zoom cursor-pointer focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-amma-green lg:px-4"
          aria-label="Sair"
        >
          <span class="material-symbols-outlined text-[20px]" aria-hidden="true">logout</span>
          <span class="hidden lg:inline">Sair</span>
        </button>
      </template>
      <template v-else>
        <button
          type="button"
          @click="emit('open-register')"
          class="border-2 border-amma-green text-amma-green hover:bg-amma-green-lighter px-3 py-2.5 rounded-btn text-xs transition-colors efeito-zoom cursor-pointer focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-amma-green sm:px-5 sm:text-sm"
        >
          Cadastrar-se
        </button>
        <button
          type="button"
          @click="emit('open-login')"
          class="bg-amma-green hover:bg-amma-green-dark text-white px-4 py-2.5 rounded-btn text-xs efeito-zoom cursor-pointer focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-amma-green sm:px-6 sm:text-sm"
        >
          Entrar
        </button>
      </template>
      <button 
        type="button"
        @click="isMobileMenuOpen = !isMobileMenuOpen" 
        class="xl:hidden flex min-h-11 min-w-11 items-center justify-center rounded-btn text-amma-green focus-visible:outline focus-visible:outline-2 focus-visible:outline-amma-green"
        :aria-expanded="isMobileMenuOpen"
        aria-controls="public-mobile-menu"
        :aria-label="isMobileMenuOpen ? 'Fechar menu' : 'Abrir menu'"
      >
        <span class="material-symbols-outlined text-[32px]">
          {{ isMobileMenuOpen ? 'close' : 'menu' }}
        </span>
      </button>
    </div>

    <Transition
      enter-active-class="transition-all duration-300 ease-out"
      enter-from-class="opacity-0 -translate-y-4"
      enter-to-class="opacity-100 translate-y-0"
      leave-active-class="transition-all duration-200 ease-in"
      leave-from-class="opacity-100 translate-y-0"
      leave-to-class="opacity-0 -translate-y-4"
    >
      <div v-if="isMobileMenuOpen" id="public-mobile-menu" class="xl:hidden absolute top-full left-0 z-50 flex w-full flex-col gap-3 border-b border-gray-200 bg-white px-5 py-6 shadow-lg sm:px-8">
        <button
          v-for="link in navLinks"
          :key="link.label"
          type="button"
          @click="navigateTo(link.routeName)"
          :class="[
            'py-2 font-medium transition-colors text-left',
            isLinkActive(link)
              ? 'bg-amma-green-light text-[#4D6E1F] px-5 py-3 rounded-btn font-bold text-center'
              : 'text-gray-600 hover:text-gray-900'
          ]"
        >
          {{ link.label }}
        </button>
        <div v-if="auth.isAuthenticated" class="mt-2 flex flex-col gap-2 border-t border-amma-green-light pt-4">
          <button v-if="auth.isCitizen" type="button" class="flex min-h-11 items-center gap-3 rounded-btn px-4 py-2 text-left font-semibold text-amma-green-dark hover:bg-amma-green-lighter focus-visible:outline focus-visible:outline-2 focus-visible:outline-amma-green" @click="navigateTo('cidadao-dashboard')">
            <span class="material-symbols-outlined" aria-hidden="true">dashboard</span>
            Dashboard
          </button>
          <button v-if="auth.isCitizen" type="button" class="flex min-h-11 items-center gap-3 rounded-btn px-4 py-2 text-left font-semibold text-amma-green-dark hover:bg-amma-green-lighter focus-visible:outline focus-visible:outline-2 focus-visible:outline-amma-green" @click="navigateToProfile">
            <span class="material-symbols-outlined" aria-hidden="true">person</span>
            Meu Perfil
          </button>
          <button v-if="auth.canAccessManagement" type="button" class="flex min-h-11 items-center gap-3 rounded-btn px-4 py-2 text-left font-semibold text-amma-green-dark hover:bg-amma-green-lighter focus-visible:outline focus-visible:outline-2 focus-visible:outline-amma-green" @click="navigateToManagement">
            <span class="material-symbols-outlined" aria-hidden="true">dashboard</span>
            Voltar ao painel
          </button>
        </div>
      </div>
    </Transition>
  </header>
</template>
