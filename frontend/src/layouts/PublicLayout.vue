<script setup lang="ts">
import { ref } from 'vue'
import { RouterView } from 'vue-router'
import TopBar from '@/components/layout/TopBar.vue'
import LoginModal from '@/components/modals/LoginModal.vue'
import RegisterModal from '@/components/modals/RegisterModal.vue'
import AppToastContainer from '@/components/feedback/AppToastContainer.vue'

const isLoginModalOpen = ref(false)
const isRegisterModalOpen = ref(false)

const openLogin = () => {
  isRegisterModalOpen.value = false
  isLoginModalOpen.value = true
}

const openRegister = () => {
  isLoginModalOpen.value = false
  isRegisterModalOpen.value = true
}
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
    />

    <RegisterModal 
      :isOpen="isRegisterModalOpen" 
      @close="isRegisterModalOpen = false"
      @open-login="openLogin"
    />
  </div>
</template>