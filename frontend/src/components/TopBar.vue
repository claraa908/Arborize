<script setup lang="ts">
</script>

<template>
  <header class="w-full flex items-center justify-between px-8 py-6 border-b border-gray-200 ">
    <div class="flex items-center gap-2">
      <div class="text-[#5D8A3F] flex items-center gap-2">
        <img src="@/assets/images/logo.png" alt="Logo Arborize" class="h-16 w-auto" />
        <div class="flex flex-col">
          <span class="text-xl font-bold leading-none tracking-wide text-[#5D8A3F]">ARBORIZE</span>
          <span class="text-[0.6rem] font-medium tracking-widest text-[#5D8A3F]">Gestão Ambiental</span>
        </div>
      </div>
    </div>

    <nav class="hidden md:flex items-center gap-8 text-sm font-medium text-gray-500">
      <a href="#" class="hover:text-gray-900 transition-colors">Início</a>
      <a href="#" class="hover:text-gray-900 transition-colors">Eventos</a>
      <a href="#" class="bg-amma-green-light text-[#4D6E1F] px-5 py-2 rounded-tl-[10px] rounded-tr-[20px] rounded-bl-[20px] rounded-br-[10px] font-bold">Arborização</a>
      <a href="#" class="hover:text-gray-900 transition-colors">Sobre Nós</a>
    </nav>

    <div class="flex items-center gap-4 text-sm font-bold">
      <button class="text-[#5D8A3F] hover:text-[#3E5F29] transition-colors">Cadastre-se</button>
      <button class="bg-amma-green text-white px-6 py-2.5 rounded-tl-[10px] rounded-tr-[20px] rounded-bl-[20px] rounded-br-[10px] hover:bg-[#527536] transition-colors">
        Entrar
      </button>
    </div>
  </header>
</template>