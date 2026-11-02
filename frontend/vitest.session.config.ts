import { defineConfig } from 'vitest/config'
import clientConfig from './vitest.mapa.config.ts'

export default defineConfig({
  ...clientConfig,
  test: {
    environment: 'node',
    include: [
      'src/__tests__/sessionNavigation.spec.ts',
      'src/__tests__/meuPerfil.spec.ts',
      'src/__tests__/authStorage.spec.ts',
    ],
  },
})
