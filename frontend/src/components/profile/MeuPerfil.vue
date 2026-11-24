<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { AxiosError } from 'axios'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/authStore'
import { useFeedbackStore } from '@/stores/feedbackStore'
import {
  atualizarMeuPerfil,
  buscarMeuPerfil,
  buscarMeusAcompanhamentos,
  type SolicitacaoMudaAcompanhamento,
  type SubstituicaoAcompanhamento,
} from '@/services/usuarioService'
import CidadaoAcompanhamentosPanel from '@/components/profile/CidadaoAcompanhamentosPanel.vue'
import ModalAlterarSenha from '@/components/modals/ModalAlterarSenha.vue'
import { formatBrazilianPhone } from '@/utils/phone'

type SecaoPerfil = 'acompanhamentos' | 'dados' | 'seguranca'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()
const feedback = useFeedbackStore()

const secaoAtiva = ref<SecaoPerfil>('acompanhamentos')

const nome = ref('')
const email = ref('')
const cpf = ref('')
const whatsapp = ref('')
const endereco = ref('')

const isLoading = ref(true)
const isProfileLoaded = ref(false)
const isSaving = ref(false)
const isPasswordModalOpen = ref(false)
const errorMessage = ref('')

const solicitacoesMuda = ref<SolicitacaoMudaAcompanhamento[]>([])
const substituicoesBotanicas = ref<SubstituicaoAcompanhamento[]>([])
const isLoadingAcompanhamentos = ref(true)
const acompanhamentosError = ref('')

const userInitials = computed(() => {
  const source = (nome.value || auth.nome || 'Cidadão').trim()
  return (
    source
      .split(/\s+/)
      .slice(0, 2)
      .map((part) => part.charAt(0).toUpperCase())
      .join('') || 'C'
  )
})

const filtroInicial = computed<'TODOS' | 'MUDAS' | 'SUBSTITUICOES'>(() => {
  const raw = typeof route.query.filtro === 'string' ? route.query.filtro.toUpperCase() : ''
  if (raw === 'MUDAS' || raw === 'SUBSTITUICOES') return raw
  return 'TODOS'
})

const protocoloInicial = computed(() =>
  typeof route.query.protocolo === 'string' ? route.query.protocolo.trim() : '',
)

const totalAcompanhamentos = computed(
  () => solicitacoesMuda.value.length + substituicoesBotanicas.value.length,
)

const menuSecoes = computed<Array<{ key: SecaoPerfil; label: string; icon: string; badge?: number }>>(() => [
  {
    key: 'acompanhamentos',
    label: 'Meus Acompanhamentos',
    icon: 'assignment',
    badge: totalAcompanhamentos.value,
  },
  {
    key: 'dados',
    label: 'Dados Pessoais',
    icon: 'person',
  },
  {
    key: 'seguranca',
    label: 'Segurança e Senha',
    icon: 'lock',
  },
])

const handleWhatsAppInput = (event: Event) => {
  whatsapp.value = formatBrazilianPhone((event.target as HTMLInputElement).value)
}

const sendToLogin = () => {
  auth.logout()
  void router.replace({ name: 'home', query: { auth: 'required', redirect: route.fullPath } })
}

const handleLoadError = (error: unknown) => {
  if (error instanceof AxiosError && error.response?.status === 401) {
    sendToLogin()
    return
  }
  errorMessage.value =
    error instanceof AxiosError && error.response?.status === 403
      ? 'Você não tem permissão para acessar estes dados.'
      : 'Não foi possível carregar seus dados. Tente novamente.'
}

const loadProfile = async () => {
  isLoading.value = true
  isProfileLoaded.value = false
  errorMessage.value = ''
  try {
    const usuario = await buscarMeuPerfil()
    nome.value = usuario.nome
    email.value = usuario.email
    cpf.value = usuario.cpf.replace(/(\d{3})(\d{3})(\d{3})(\d{2})/, '$1.$2.$3-$4')
    whatsapp.value = formatBrazilianPhone(usuario.whatsapp)
    endereco.value = usuario.endereco
    auth.updateName(usuario.nome)
    isProfileLoaded.value = true
  } catch (error) {
    handleLoadError(error)
  } finally {
    isLoading.value = false
  }
}

const loadAcompanhamentos = async () => {
  isLoadingAcompanhamentos.value = true
  acompanhamentosError.value = ''
  try {
    const data = await buscarMeusAcompanhamentos()
    solicitacoesMuda.value = data.solicitacoesMuda ?? []
    substituicoesBotanicas.value = data.substituicoesBotanicas ?? []
  } catch (error) {
    if (error instanceof AxiosError && error.response?.status === 401) {
      sendToLogin()
      return
    }
    acompanhamentosError.value = 'Não foi possível carregar suas solicitações no momento.'
  } finally {
    isLoadingAcompanhamentos.value = false
  }
}

const handleSubmit = async () => {
  errorMessage.value = ''
  if (!nome.value.trim()) {
    errorMessage.value = 'Por favor, informe seu nome completo.'
    return
  }

  isSaving.value = true
  try {
    const usuario = await atualizarMeuPerfil({
      nome: nome.value.trim(),
      whatsapp: whatsapp.value,
      endereco: endereco.value.trim(),
    })
    nome.value = usuario.nome
    whatsapp.value = formatBrazilianPhone(usuario.whatsapp)
    endereco.value = usuario.endereco
    auth.updateName(usuario.nome)
    feedback.showToast('Perfil atualizado com sucesso', 'success')
  } catch (error) {
    if (error instanceof AxiosError && error.response?.status === 401) {
      sendToLogin()
    } else if (error instanceof AxiosError && error.response?.status === 403) {
      errorMessage.value = 'Você não tem permissão para atualizar estes dados.'
    } else if (error instanceof AxiosError && typeof error.response?.data === 'string' && error.response.data.trim()) {
      errorMessage.value = error.response.data
    } else {
      errorMessage.value = 'Não foi possível salvar as alterações. Tente novamente.'
    }
  } finally {
    isSaving.value = false
  }
}

const handleForgotPassword = () => {
  feedback.showToast('A recuperação de senha por e-mail estará disponível em breve.', 'info')
}

onMounted(() => {
  void loadProfile()
  void loadAcompanhamentos()
})
</script>

<template>
  <div class="max-w-6xl mx-auto py-3 md:py-4 lg:h-full lg:overflow-hidden flex flex-col gap-6">
    <!-- Loading inicial do perfil -->
    <div
      v-if="isLoading"
      role="status"
      aria-live="polite"
      class="grid grid-cols-1 lg:grid-cols-12 gap-6 items-stretch lg:flex-1 lg:min-h-0 animate-pulse"
    >
      <p class="sr-only">Carregando seus dados...</p>
      <div class="lg:col-span-4 lg:h-full rounded-box border border-amma-green-light bg-white p-8 flex flex-col items-center gap-4">
        <div class="w-28 h-28 bg-amma-green-light" style="border-radius: 60px 15px 60px 60px;"></div>
        <div class="h-5 w-2/3 bg-amma-green-light rounded-full"></div>
        <div class="h-4 w-1/2 bg-amma-bg rounded-full"></div>
        <div class="w-full h-10 bg-amma-green-lighter rounded-btn mt-4"></div>
        <div class="w-full h-10 bg-amma-bg rounded-btn"></div>
        <div class="w-full h-10 bg-amma-bg rounded-btn"></div>
      </div>
      <div class="lg:col-span-8 lg:h-full rounded-box border border-amma-green-light bg-white p-8 min-h-[440px] flex flex-col gap-4">
        <div class="h-6 w-1/3 bg-amma-green-light rounded-full"></div>
        <div class="h-24 w-full bg-amma-bg rounded-btn"></div>
        <div class="h-24 w-full bg-amma-bg rounded-btn"></div>
      </div>
    </div>

    <!-- Erro fatal ao carregar perfil -->
    <div
      v-else-if="!isProfileLoaded"
      role="alert"
      class="rounded-box border border-feedback-danger-border bg-feedback-danger-bg p-8 text-center flex flex-col items-center gap-3"
    >
      <span class="material-symbols-outlined text-5xl text-feedback-danger" aria-hidden="true">error_outline</span>
      <h1 class="text-xl font-bold text-feedback-danger">Não foi possível exibir seu perfil</h1>
      <p class="text-sm text-grey-text max-w-md">
        {{ errorMessage || 'Não foi possível carregar seus dados. Tente novamente.' }}
      </p>
      <button
        type="button"
        class="mt-2 rounded-btn bg-amma-green hover:bg-amma-green-dark px-6 py-2.5 text-sm font-bold text-white transition-colors efeito-zoom cursor-pointer focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-amma-green"
        @click="loadProfile"
      >
        Tentar novamente
      </button>
    </div>

    <!-- Layout Principal Simétrico em 2 Colunas (ocupa 100% da altura visível do container principal em desktop) -->
    <div v-else class="grid grid-cols-1 lg:grid-cols-12 gap-6 items-stretch lg:flex-1 lg:min-h-0">
      <!-- Coluna Esquerda Fixa (Identidade + Navegação do Perfil) -->
      <aside
        aria-label="Resumo e navegação do perfil"
        class="lg:col-span-4 lg:h-full lg:min-h-0"
      >
        <div class="h-full rounded-box border border-amma-green-light bg-white p-6 md:p-7 shadow-sm flex flex-col items-center text-center gap-4 overflow-y-auto">
          <!-- Avatar no padrão do Perfil Institucional -->
          <div class="flex flex-col items-center shrink-0">
            <div
              class="w-24 h-24 bg-amma-green-light text-amma-green flex items-center justify-center font-bold text-3xl shadow-lg border-4 border-white select-none mb-3"
              style="border-radius: 60px 15px 60px 60px;"
              aria-hidden="true"
            >
              {{ userInitials }}
            </div>

            <h1 class="text-lg font-extrabold text-amma-green-dark leading-snug line-clamp-2">
              {{ nome }}
            </h1>
            <span
              class="mt-1.5 inline-flex items-center gap-1 text-xs font-semibold text-amma-green-dark bg-amma-green-lighter border border-amma-green-light px-3 py-0.5 rounded-full"
            >
              <span class="material-symbols-outlined text-[14px] text-amma-green" aria-hidden="true">person</span>
              Cidadão
            </span>

            <p class="mt-2.5 text-xs font-medium text-grey-text break-all">{{ email }}</p>
            <p class="mt-0.5 text-xs font-medium text-grey-text">
              CPF: <strong class="text-amma-green-dark">{{ cpf }}</strong>
            </p>
          </div>

          <!-- Resumo simétrico -->
          <div class="w-full shrink-0 grid grid-cols-2 gap-3 rounded-btn bg-amma-green-lighter/70 border border-amma-green-light p-3">
            <div class="flex flex-col items-center">
              <span class="text-lg font-black text-amma-green-dark leading-none">{{ solicitacoesMuda.length }}</span>
              <span class="text-[11px] font-semibold text-grey-text mt-1">Mudas</span>
            </div>
            <div class="flex flex-col items-center border-l border-amma-green-light">
              <span class="text-lg font-black text-amma-green-dark leading-none">{{ substituicoesBotanicas.length }}</span>
              <span class="text-[11px] font-semibold text-grey-text mt-1">Substituições</span>
            </div>
          </div>

          <!-- Menu de navegação interna -->
          <nav aria-label="Seções do perfil" class="w-full shrink-0 flex flex-col gap-1.5 border-t border-amma-green-light pt-4 mt-auto">
            <button
              v-for="secao in menuSecoes"
              :key="secao.key"
              type="button"
              :aria-current="secaoAtiva === secao.key ? 'page' : undefined"
              :class="[
                'w-full flex items-center justify-between gap-3 rounded-btn px-4 py-2.5 text-sm font-bold transition-colors cursor-pointer focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-amma-green',
                secaoAtiva === secao.key
                  ? 'bg-amma-green text-white shadow-xs'
                  : 'text-grey-text hover:bg-amma-green-lighter hover:text-amma-green-dark',
              ]"
              @click="secaoAtiva = secao.key"
            >
              <span class="flex items-center gap-2.5">
                <span class="material-symbols-outlined text-[20px]" aria-hidden="true">{{ secao.icon }}</span>
                <span>{{ secao.label }}</span>
              </span>
              <span
                v-if="secao.badge !== undefined"
                :class="[
                  'px-2 py-0.5 rounded-full text-[11px] font-bold',
                  secaoAtiva === secao.key
                    ? 'bg-white/20 text-white'
                    : 'bg-amma-green-lighter text-amma-green-dark',
                ]"
              >
                {{ secao.badge }}
              </span>
            </button>
          </nav>
        </div>
      </aside>

      <!-- Coluna Direita Interativa (altura fixa ocupando 100% da área visível com rolagem interna) -->
      <div class="lg:col-span-8 lg:h-full lg:min-h-0 min-w-0 rounded-box border border-amma-green-light bg-white p-6 md:p-7 shadow-sm flex flex-col overflow-hidden">
        <!-- Seção 1: Meus Acompanhamentos -->
        <CidadaoAcompanhamentosPanel
          v-if="secaoAtiva === 'acompanhamentos'"
          :solicitacoes-muda="solicitacoesMuda"
          :substituicoes-botanicas="substituicoesBotanicas"
          :is-loading="isLoadingAcompanhamentos"
          :error-message="acompanhamentosError"
          :initial-filtro="filtroInicial"
          :initial-protocolo="protocoloInicial"
          @retry="loadAcompanhamentos"
        />

        <!-- Seção 2: Dados Pessoais -->
        <section v-else-if="secaoAtiva === 'dados'" aria-labelledby="dados-pessoais-titulo" class="flex flex-col gap-5 h-full min-h-0">
          <header class="border-b border-amma-green-light pb-4 shrink-0">
            <h2 id="dados-pessoais-titulo" class="text-xl font-bold text-amma-green-dark flex items-center gap-2">
              <span class="material-symbols-outlined text-amma-green text-[22px]" aria-hidden="true">person</span>
              Dados Pessoais
            </h2>
            <p class="text-sm text-grey-text mt-1">
              Mantenha seu contato e endereço atualizados para facilitar o atendimento da AMMA.
            </p>
          </header>

          <div
            v-if="errorMessage"
            role="alert"
            aria-live="assertive"
            class="shrink-0 rounded-btn border border-feedback-danger-border bg-feedback-danger-bg p-4 text-sm font-medium text-feedback-danger"
          >
            {{ errorMessage }}
          </div>

          <form class="flex flex-col gap-5 flex-1 min-h-0" @submit.prevent="handleSubmit">
            <div class="grid grid-cols-1 sm:grid-cols-2 gap-4 flex-1 min-h-0 overflow-y-auto pr-1 content-start">
              <div class="flex flex-col gap-1.5">
                <label for="perfil-nome" class="text-sm font-semibold text-grey-text">Nome Completo *</label>
                <input
                  id="perfil-nome"
                  v-model="nome"
                  name="nome"
                  type="text"
                  autocomplete="name"
                  required
                  class="w-full bg-white focus:bg-amma-green-lighter border-2 border-amma-green-light focus:border-amma-green rounded-btn py-3 px-4 text-grey-text outline-none transition-all text-sm font-medium"
                />
              </div>

              <div class="flex flex-col gap-1.5">
                <label for="perfil-whatsapp" class="text-sm font-semibold text-grey-text">WhatsApp *</label>
                <input
                  id="perfil-whatsapp"
                  :value="whatsapp"
                  name="whatsapp"
                  type="tel"
                  inputmode="tel"
                  autocomplete="tel"
                  required
                  placeholder="(00) 00000-0000"
                  class="w-full bg-white focus:bg-amma-green-lighter border-2 border-amma-green-light focus:border-amma-green rounded-btn py-3 px-4 text-grey-text outline-none transition-all text-sm font-medium"
                  @input="handleWhatsAppInput"
                />
              </div>

              <div class="flex flex-col gap-1.5">
                <label for="perfil-cpf" class="text-sm font-semibold text-grey-text">CPF (somente leitura)</label>
                <div class="relative">
                  <input
                    id="perfil-cpf"
                    :value="cpf"
                    type="text"
                    disabled
                    class="w-full cursor-not-allowed bg-amma-bg border-2 border-amma-green-light rounded-btn py-3 px-4 pr-11 text-grey-text/80 outline-none text-sm font-medium"
                  />
                  <span
                    class="material-symbols-outlined pointer-events-none absolute right-3.5 top-1/2 -translate-y-1/2 text-[18px] text-grey-text/60"
                    aria-hidden="true"
                  >
                    lock
                  </span>
                </div>
              </div>

              <div class="flex flex-col gap-1.5">
                <label for="perfil-email" class="text-sm font-semibold text-grey-text">E-mail (somente leitura)</label>
                <div class="relative">
                  <input
                    id="perfil-email"
                    :value="email"
                    type="email"
                    disabled
                    class="w-full cursor-not-allowed bg-amma-bg border-2 border-amma-green-light rounded-btn py-3 px-4 pr-11 text-grey-text/80 outline-none text-sm font-medium"
                  />
                  <span
                    class="material-symbols-outlined pointer-events-none absolute right-3.5 top-1/2 -translate-y-1/2 text-[18px] text-grey-text/60"
                    aria-hidden="true"
                  >
                    lock
                  </span>
                </div>
              </div>

              <div class="flex flex-col gap-1.5 sm:col-span-2">
                <label for="perfil-endereco" class="text-sm font-semibold text-grey-text">Endereço Completo *</label>
                <input
                  id="perfil-endereco"
                  v-model="endereco"
                  name="endereco"
                  type="text"
                  autocomplete="street-address"
                  required
                  placeholder="Rua, Número, Bairro"
                  class="w-full bg-white focus:bg-amma-green-lighter border-2 border-amma-green-light focus:border-amma-green rounded-btn py-3 px-4 text-grey-text outline-none transition-all text-sm font-medium"
                />
              </div>
            </div>

            <div class="flex flex-wrap justify-end gap-3 pt-4 mt-auto border-t border-amma-green-light">
              <button
                type="button"
                :disabled="isSaving"
                class="rounded-btn border-2 border-amma-green-light bg-white hover:bg-amma-green-lighter px-5 py-2.5 text-sm font-bold text-grey-text transition-colors cursor-pointer disabled:opacity-60 focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-amma-green"
                @click="loadProfile"
              >
                Descartar alterações
              </button>
              <button
                type="submit"
                :disabled="isSaving"
                class="rounded-btn bg-amma-green hover:bg-amma-green-dark px-6 py-2.5 text-sm font-bold text-white shadow-sm transition-colors efeito-zoom cursor-pointer disabled:cursor-wait disabled:opacity-60 focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-amma-green"
              >
                {{ isSaving ? 'Salvando...' : 'Salvar alterações' }}
              </button>
            </div>
          </form>
        </section>

        <!-- Seção 3: Segurança e Senha -->
        <section v-else aria-labelledby="seguranca-titulo" class="flex flex-col gap-6 h-full">
          <header class="border-b border-amma-green-light pb-4">
            <h2 id="seguranca-titulo" class="text-xl font-bold text-amma-green-dark flex items-center gap-2">
              <span class="material-symbols-outlined text-amma-green text-[22px]" aria-hidden="true">lock</span>
              Segurança e Senha
            </h2>
            <p class="text-sm text-grey-text mt-1">
              Proteja o acesso à sua conta mantendo sua senha atualizada.
            </p>
          </header>

          <div class="rounded-btn border border-amma-green-light bg-amma-green-lighter/50 p-6 flex flex-col sm:flex-row sm:items-center justify-between gap-4">
            <div class="flex items-start gap-3.5">
              <div class="w-11 h-11 rounded-btn bg-white border border-amma-green-light flex items-center justify-center text-amma-green shrink-0">
                <span class="material-symbols-outlined text-[22px]" aria-hidden="true">key</span>
              </div>
              <div>
                <h3 class="text-base font-bold text-amma-green-dark">Alterar senha de acesso</h3>
                <p class="text-sm text-grey-text mt-0.5">
                  Recomendamos utilizar uma senha forte e exclusiva para o portal Arborize.
                </p>
              </div>
            </div>

            <button
              type="button"
              class="rounded-btn bg-amma-green hover:bg-amma-green-dark px-5 py-2.5 text-sm font-bold text-white shadow-sm transition-colors efeito-zoom cursor-pointer shrink-0 inline-flex items-center justify-center gap-2 focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-amma-green"
              @click="isPasswordModalOpen = true"
            >
              <span class="material-symbols-outlined text-[18px]" aria-hidden="true">lock_reset</span>
              Alterar senha
            </button>
          </div>
        </section>
      </div>
    </div>

    <ModalAlterarSenha
      :is-open="isPasswordModalOpen"
      @close="isPasswordModalOpen = false"
      @forgot-password="handleForgotPassword"
    />
  </div>
</template>
