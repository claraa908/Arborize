<script setup lang="ts">
import { ref, watch } from 'vue'
import { RouterView } from 'vue-router'
import { useRoute, useRouter } from 'vue-router'
import TopBar from '@/components/layout/TopBar.vue'
import LoginModal from '@/components/modals/LoginModal.vue'
import RegisterModal from '@/components/modals/RegisterModal.vue'
import AppToastContainer from '@/components/feedback/AppToastContainer.vue'

const isLoginModalOpen = ref(false)
const isRegisterModalOpen = ref(false)
const route = useRoute()
const router = useRouter()

const openLogin = () => {
  isRegisterModalOpen.value = false
  isLoginModalOpen.value = true
}

const openRegister = () => {
  isLoginModalOpen.value = false
  isRegisterModalOpen.value = true
}

const handleLoginSuccess = () => {
  const redirect = route.query.redirect
  if (typeof redirect === 'string' && redirect.startsWith('/') && !redirect.startsWith('//')) {
    void router.replace(redirect)
    return
  }
  void router.replace({ name: 'perfil' })
}

watch(() => route.query.auth, (authRequired) => {
  if (authRequired === 'required') openLogin()
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
      @close="isLoginModalOpen = false"
      @login-success="handleLoginSuccess"
    />

    <RegisterModal 
      :isOpen="isRegisterModalOpen" 
      @close="isRegisterModalOpen = false"
      @open-login="openLogin"
    />
  </div>
</template>
