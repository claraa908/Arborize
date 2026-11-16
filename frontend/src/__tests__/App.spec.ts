import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import { createPinia } from 'pinia'
import { createRouter, createWebHistory } from 'vue-router'
import App from '../App.vue'

// Cria um roteador falso (mock) para os testes
const router = createRouter({
  history: createWebHistory(),
  routes: [{ path: '/', component: { template: '<div>Home</div>' } }],
})

describe('App', () => {
  it('mounts and renders properly', () => {
    const pinia = createPinia() // Inicia a Pinia

    const wrapper = mount(App, {
      global: {
        plugins: [pinia, router], // Injeta a Pinia e o Router no componente
      },
    })

    expect(wrapper.exists()).toBe(true)
  })
})
