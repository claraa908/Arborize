import { fileURLToPath, URL } from 'node:url'
import { defineConfig } from 'vitest/config'
import { compileScript, parse } from '@vue/compiler-sfc'
import ts from 'typescript'

export default defineConfig({
  plugins: [{
    name: 'vue-client-mapa-integration',
    transform(source, id) {
      if (!id.endsWith('.vue')) return
      const { descriptor } = parse(source, { filename: id })
      const script = compileScript(descriptor, {
        id,
        inlineTemplate: true,
        templateOptions: { compilerOptions: { hoistStatic: false } },
      })
      return ts.transpileModule(script.content, {
        compilerOptions: { target: ts.ScriptTarget.ESNext, module: ts.ModuleKind.ESNext },
      }).outputText
    },
  }],
  resolve: { alias: { '@': fileURLToPath(new URL('./src', import.meta.url)) } },
  test: {
    environment: 'node',
    include: [
      'src/__tests__/arvoreMapaIntegracao.spec.ts',
      'src/__tests__/arvoreFotoMiniatura.spec.ts',
      'src/__tests__/arvoreCadastroForm.spec.ts',
      'src/__tests__/arvoreGeocodificacao.spec.ts',
    ],
  },
})
