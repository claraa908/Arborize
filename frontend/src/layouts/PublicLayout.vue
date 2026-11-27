<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { RouterView } from 'vue-router'
import { useRoute, useRouter } from 'vue-router'
import TopBar from '@/components/layout/TopBar.vue'
import LoginModal from '@/components/modals/LoginModal.vue'
import RegisterModal from '@/components/modals/RegisterModal.vue'
import AppToastContainer from '@/components/feedback/AppToastContainer.vue'
import { useAuthStore } from '@/stores/authStore'
import { postLoginTarget } from '@/router/sessionNavigation'

const isLoginModalOpen = ref(false)
const isRegisterModalOpen = ref(false)
const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const authNotice = computed(() => {
  const motivo = typeof route.query.motivo === 'string' ? route.query.motivo : ''
  const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : ''
  if (motivo === 'solicitar-muda' || redirect.startsWith('/arborizacao/solicitar-muda')) {
    return 'Para solicitar a muda selecionada, entre na sua conta ou cadastre-se. Sua escolha será mantida automaticamente.'
  }
  if (motivo === 'substituicao-nim' || redirect.startsWith('/arborizacao/substituicao-nim')) {
    return 'Para fazer um requerimento de retirada de Nim Indiano, entre na sua conta ou cadastre-se.'
  }
  return ''
})

const openLogin = () => {
  isRegisterModalOpen.value = false
  isLoginModalOpen.value = true
}

const openRegister = () => {
  isLoginModalOpen.value = false
  isRegisterModalOpen.value = true
}

const handleLoginSuccess = () => {
  isLoginModalOpen.value = false
  isRegisterModalOpen.value = false
  void router.replace(postLoginTarget(router, auth, route.query.redirect))
}

const closeAuthModals = () => {
  isLoginModalOpen.value = false
  isRegisterModalOpen.value = false

  if (route.query.auth !== 'required') return

  const query = { ...route.query }
  delete query.auth
  delete query.redirect
  delete query.motivo
  void router.replace({
    name: typeof route.name === 'string' ? route.name : 'home',
    params: route.params,
    query,
  })
}

watch(() => route.query.auth, (authRequired) => {
  if (authRequired === 'required' && !auth.isAuthenticated) openLogin()
}, { immediate: true })
</script>

<template>
  <div class="h-screen bg-amma-bg px-4 py-2 md:px-8 md:py-4 flex justify-center overflow-hidden">
    <div class="w-full max-w-7xl bg-white rounded-box shadow-xl flex flex-col h-full overflow-hidden relative">
      <AppToastContainer />

      <TopBar 
        class="shrink-0" 
        @open-login="openLogin" 
        @open-register="openRegister" 
      />
      
      <main class="flex-1 overflow-y-auto px-6 md:px-10 pt-2 pb-6 md:pb-10">
        <RouterView />
      </main>

    </div>

    <LoginModal 
      :isOpen="isLoginModalOpen" 
      :notice="authNotice"
      @close="closeAuthModals"
      @login-success="handleLoginSuccess"
      @open-register="openRegister"
    />

    <RegisterModal 
      :isOpen="isRegisterModalOpen" 
      :notice="authNotice"
      @close="closeAuthModals"
      @open-login="openLogin"
      @register-success="handleLoginSuccess"
    />
  </div>
</template>
