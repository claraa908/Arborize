<script setup lang="ts">
import { ref } from 'vue'

const nome = ref('Romênia Barros')
const email = ref('romenia.barros@amma.quixada.ce.gov.br')
const cpf = ref('000.000.000-00')
const currentPassword = ref('')
const newPassword = ref('')
const confirmPassword = ref('')
</script>

<template>
  <div class="max-w-5xl mx-auto pt-8">
    <div class="grid grid-cols-1 md:grid-cols-3 gap-8">
      
      <div class="md:col-span-1 flex flex-col gap-6">
        <div class="bg-white rounded-box shadow-sm border border-amma-green-light p-8 flex flex-col items-center text-center">
          <div class="relative mb-6">
            <div 
              class="w-32 h-32 bg-amma-green-light text-amma-green flex items-center justify-center font-bold text-4xl shadow-lg border-4 border-white"
              style="border-radius: 60px 15px 60px 60px;"
            >
              RB
            </div>
            <button class="absolute bottom-0 right-0 w-10 h-10 bg-white border border-amma-green-light text-amma-green hover:text-amma-green rounded-full flex items-center justify-center shadow-sm efeito-zoom">
              <span class="material-symbols-outlined text-[20px]">photo_camera</span>
            </button>
          </div>
          
          <h3 class="text-xl font-extrabold text-amma-green-dark mb-1">{{ nome }}</h3>
          <span class="text-sm font-semibold text-amma-green-dark bg-amma-green-lighter px-3 py-1 rounded-full mb-4">Superintendente</span>
          <p class="text-sm text-grey-text font-medium">AMMA - Autarquia Municipal de Meio Ambiente de Quixadá</p>
        </div>
      </div>

      <div class="md:col-span-2 flex flex-col gap-6">
        <div class="bg-white rounded-box shadow-sm border border-amma-green-light p-8">
          <h4 class="text-lg font-bold text-amma-green-dark mb-6 flex items-center gap-2">
            <span class="material-symbols-outlined text-amma-green">person</span>
            Dados Pessoais
          </h4>
          
          <div class="grid grid-cols-1 md:grid-cols-2 gap-5 mb-6">
            <div class="flex flex-col gap-1.5 md:col-span-2">
              <label class="text-sm font-semibold text-grey-text pl-1">Nome Completo</label>
              <input v-model="nome" type="text" class="w-full bg-white focus:bg-amma-green-lighter border-2 border-amma-green-light focus:border-amma-green rounded-btn py-3 px-4 text-grey-text outline-none transition-all text-sm font-medium" />
            </div>
            
            <div class="flex flex-col gap-1.5">
              <label class="text-sm font-semibold text-grey-text pl-1">E-mail</label>
              <input v-model="email" type="email" class="w-full bg-white focus:bg-amma-green-lighter border-2 border-amma-green-light focus:border-amma-green rounded-btn py-3 px-4 text-grey-text outline-none transition-all text-sm font-medium" />
            </div>

            <div class="flex flex-col gap-1.5">
              <label class="text-sm font-semibold text-grey-text pl-1">CPF</label>
              <input v-model="cpf" type="text" class="w-full bg-amma-green-lighter border-2 border-transparent rounded-btn py-3 px-4 text-grey-text/60 outline-none text-sm font-medium cursor-not-allowed" disabled />
            </div>
          </div>

          <div class="flex justify-end">
            <button class="bg-amma-green hover:bg-amma-green-dark text-white font-bold py-2.5 px-6 rounded-btn shadow-md text-sm border-none outline-none efeito-zoom">
              Salvar Alterações
            </button>
          </div>
        </div>

        <div class="bg-white rounded-box shadow-sm border border-amma-green-light p-8">
          <h4 class="text-lg font-bold text-amma-green-dark mb-6 flex items-center gap-2">
            <span class="material-symbols-outlined text-amma-green">lock</span>
            Segurança e Senha
          </h4>
          
          <div class="grid grid-cols-1 gap-5 mb-6">
            <div class="flex flex-col gap-1.5">
              <label class="text-sm font-semibold text-grey-text pl-1">Senha Atual</label>
              <input v-model="currentPassword" type="password" class="w-full bg-white focus:bg-amma-green-lighter border-2 border-amma-green-light focus:border-amma-green rounded-btn py-3 px-4 text-grey-text outline-none transition-all text-sm font-medium" />
            </div>
            
            <div class="grid grid-cols-1 md:grid-cols-2 gap-5">
              <div class="flex flex-col gap-1.5">
                <label class="text-sm font-semibold text-grey-text pl-1">Nova Senha</label>
                <input v-model="newPassword" type="password" class="w-full bg-white focus:bg-amma-green-lighter border-2 border-amma-green-light focus:border-amma-green rounded-btn py-3 px-4 text-grey-text outline-none transition-all text-sm font-medium" />
              </div>
              <div class="flex flex-col gap-1.5">
                <label class="text-sm font-semibold text-grey-text pl-1">Confirmar Nova Senha</label>
                <input v-model="confirmPassword" type="password" class="w-full bg-white focus:bg-amma-green-lighter border-2 border-amma-green-light focus:border-amma-green rounded-btn py-3 px-4 text-grey-text outline-none transition-all text-sm font-medium" />
              </div>
            </div>
          </div>

          <div class="flex justify-end">
            <button class="bg-amma-green-dark hover:bg-amma-green text-white font-bold py-2.5 px-6 rounded-btn shadow-md text-sm border-none outline-none efeito-zoom">
              Atualizar Senha
            </button>
          </div>
        </div>
      </div>
      
    </div>
  </div>
</template>