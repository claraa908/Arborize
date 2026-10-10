<script setup lang="ts">
    import TopBar from '@/components/TopBar.vue';
    import LoginModal from '@/components/LoginModal.vue'
    import { ref } from 'vue'

    const isLoginModalOpen = ref(false)
</script>

<template>
  <main class="min-h-screen bg-amma-bg p-4 md:p-8 flex flex-col">
    <div class="flex-1 w-full max-w-7xl mx-auto bg-white rounded-tl-[15px] rounded-tr-[40px] rounded-bl-[40px] rounded-br-[15px] border border-gray-200 shadow-[0px_4px_7px_1px_rgba(0,0,0,0.13)] flex flex-col">
        <TopBar @open-login="isLoginModalOpen = true" />
    </div>
    <LoginModal :is-open="isLoginModalOpen" @close="isLoginModalOpen = false" />
  </main>
</template>