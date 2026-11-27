import { describe, expect, it, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import AppDataTable from '@/components/ui/AppDataTable.vue'
import LoginModal from '@/components/modals/LoginModal.vue'
import RegisterModal from '@/components/modals/RegisterModal.vue'

vi.mock('@/stores/authStore', () => ({
  useAuthStore: () => ({
    login: vi.fn(),
    isAuthenticated: false,
  }),
}))

describe('padronização visual e paginação', () => {
  it('pagina automaticamente em 10 itens com cabeçalho fixo e controles Anterior/Próxima', async () => {
    const items = Array.from({ length: 12 }, (_, index) => ({
      id: index + 1,
      nome: `Registro ${index + 1}`,
    }))

    const wrapper = mount(AppDataTable, {
      props: {
        columns: [{ key: 'nome', label: 'Nome' }],
        items,
        defaultView: 'table',
      },
    })

    expect(wrapper.find('thead').classes()).toContain('sticky')
    expect(wrapper.find('thead').classes()).toContain('top-0')
    expect(wrapper.findAll('tbody tr')).toHaveLength(10)
    expect(wrapper.text()).toContain('Página 1 de 2')
    expect(wrapper.text()).toContain('Registro 10')
    expect(wrapper.text()).not.toContain('Registro 11')

    await wrapper.get('[aria-label="Próxima página"]').trigger('click')
    expect(wrapper.findAll('tbody tr')).toHaveLength(2)
    expect(wrapper.text()).toContain('Página 2 de 2')
    expect(wrapper.text()).toContain('Registro 11')
    expect(wrapper.text()).toContain('Registro 12')
  })

  it('usa título institucional geral no LoginModal e evita rolagem horizontal no RegisterModal', () => {
    setActivePinia(createPinia())
    const loginWrapper = mount(LoginModal, { props: { isOpen: true } })
    expect(loginWrapper.text()).toContain('Acesso ao Sistema - AMMA Quixadá')
    expect(loginWrapper.text()).not.toContain('Portal do Cidadão - AMMA Quixadá')

    const registerWrapper = mount(RegisterModal, { props: { isOpen: true } })
    const scrollArea = registerWrapper.get('div.overflow-y-auto')
    expect(scrollArea.classes()).toContain('overflow-x-hidden')
    const submitBtn = registerWrapper.get('button[type="submit"]')
    expect(submitBtn.classes()).not.toContain('efeito-zoom')
  })
})
