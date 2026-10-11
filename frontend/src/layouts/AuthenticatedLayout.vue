<script setup lang="ts">
import { RouterView } from 'vue-router'
import AppSidebar from '@/components/layout/AppSidebar.vue'
import AppHeader from '@/components/layout/AppHeader.vue'
</script>

<template>
  <div class="h-screen w-full bg-amma-bg px-4 py-2 md:px-8 md:py-4 flex justify-center items-center overflow-hidden">
    <div class="w-full max-w-7xl bg-white rounded-box shadow-xl flex flex-row h-full overflow-hidden">
      
      <AppSidebar />

      <div class="flex-1 flex flex-col min-w-0 bg-white h-full">
        
        <AppHeader />

        <main class="flex-1 pb-6 md:pb-7 overflow-y-auto">
          <RouterView />
        </main>
      </div>

    </div>
  </div>
</template>