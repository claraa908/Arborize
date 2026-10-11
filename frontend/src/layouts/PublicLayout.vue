<script setup lang="ts">
import { ref } from 'vue'
import { RouterView } from 'vue-router'
import TopBar from '@/components/layout/TopBar.vue'
import LoginModal from '@/components/modals/LoginModal.vue'

const isLoginModalOpen = ref(false)
</script>

<template>
  <div class="h-screen bg-amma-bg px-4 py-2 md:px-8 md:py-4 flex justify-center overflow-hidden">
    <div class="w-full max-w-7xl bg-white rounded-box shadow-xl flex flex-col h-full overflow-hidden">
      
      <TopBar class="shrink-0" @open-login="isLoginModalOpen = true" />
      
      <main class="flex-1 overflow-y-auto px-6 md:px-10 pt-2 pb-6 md:pb-10">
        <RouterView />
      </main>

    </div>

    <LoginModal :isOpen="isLoginModalOpen" @close="isLoginModalOpen = false"/>
  </div>
</template>