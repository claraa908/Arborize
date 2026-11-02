# frontend

This template should help get you started developing with Vue 3 in Vite.

## Recommended IDE Setup

[VS Code](https://code.visualstudio.com/) + [Vue (Official)](https://marketplace.visualstudio.com/items?itemName=Vue.volar) (and disable Vetur).

## Recommended Browser Setup

- Chromium-based browsers (Chrome, Edge, Brave, etc.):
  - [Vue.js devtools](https://chromewebstore.google.com/detail/vuejs-devtools/nhdogjmejiglipccpnnnanhbledajbpd)
  - [Turn on Custom Object Formatter in Chrome DevTools](http://bit.ly/object-formatters)
- Firefox:
  - [Vue.js devtools](https://addons.mozilla.org/en-US/firefox/addon/vue-js-devtools/)
  - [Turn on Custom Object Formatter in Firefox DevTools](https://fxdx.dev/firefox-devtools-custom-object-formatters/)

## Type Support for `.vue` Imports in TS

TypeScript cannot handle type information for `.vue` imports by default, so we replace the `tsc` CLI with `vue-tsc` for type checking. In editors, we need [Volar](https://marketplace.visualstudio.com/items?itemName=Vue.volar) to make the TypeScript language service aware of `.vue` types.

## Customize configuration

See [Vite Configuration Reference](https://vite.dev/config/).

## Project Setup

```sh
npm install
```

### Compile and Hot-Reload for Development

```sh
npm run dev
```

### Type-Check, Compile and Minify for Production

```sh
npm run build
```

### Lint with [ESLint](https://eslint.org/)

```sh
npm run lint
```

## Mapa do Inventário Arbóreo (T12)

`InventarioMapa.vue` usa o MapLibre GL JS 6.13 já instalado. Recebe `arvores`
(`ArvoreResumoResponseDTO[]`) por propriedade, emite `selecionar` (registro ou `null`)
e `abrirFicha` (ID). Não importa service/store nem consulta o backend.
Na T13, a página principal passou a fornecer os resultados reais da store. Falhas de consulta
são apresentadas separadamente e não representam um inventário sem registros.

O enquadramento aproximado de Quixadá e o zoom inicial ficam em
`src/components/inventario/inventarioMapaConfig.ts`. Não há polígonos territoriais.
Somente marcadores com coordenadas finitas nos intervalos globais são considerados.
As quatro condições usam cor, símbolo e rótulo; o resumo é renderizado pelo Vue,
sem interpolar dados do registro em HTML do MapLibre.

A base padrão é raster: `https://tile.openstreetmap.org/{z}/{x}/{y}.png`.
A atribuição **© OpenStreetMap contributors** permanece visível no mapa, inclusive no celular,
com link para a [licença ODbL e copyright](https://www.openstreetmap.org/copyright).
O navegador usa seu cache e Referer normais. Não há download offline, prefetch ou proxy de tiles.
O serviço público tem disponibilidade best-effort, sem SLA; seu uso deve seguir a
[política de tiles da OSMF](https://operations.osmfoundation.org/policies/tiles/).
Antes de uma implantação de maior volume, avaliar um provedor com capacidade apropriada.

Para substituir o estilo, definir `VITE_INVENTARIO_MAP_STYLE_URL` com uma URL de estilo
compatível com MapLibre e suas atribuições corretas, e reiniciar o servidor/recompilar o Vite.
A licença e a política do novo provedor devem ser respeitadas. Nenhuma chave é necessária
para a base padrão. O worker ESM é empacotado pelo Vite via `?worker&url` e configurado
antes de criar o mapa, evitando procurar o worker na pasta de dependências pré-otimizadas.

A troca dos registros reconstrói somente os marcadores, preservando a câmera.
Ao sair da página são removidos listeners, marcadores e a instância do mapa;
o `remove()` do MapLibre libera também seu ResizeObserver e WebGL.
Falhas de rede/inicialização têm mensagem e nova tentativa explícita, sem impedir a tabela.
O mapa dos formulários e da ficha continua reservado às tarefas correspondentes.

Testes de dados/configuração e regressões da T11, sem DOM ou serviços externos:

```sh
npx --no-install vitest run src/__tests__/inventarioMapa.spec.ts src/__tests__/inventarioService.spec.ts src/__tests__/inventarioStore.spec.ts --environment node
```

## Inventário: pesquisa e mapa satélite (T13)

A tela consulta tabela e mapa pelo serviço do inventário, aplicando os mesmos filtros.
A paginação afeta somente a tabela. HTTP 401/403 apresenta acesso indisponível, sem
substituir a falha por uma lista vazia. O backend mantém o bloqueio administrativo;
a utilização com dados reais depende da autenticação e autorização futuras.

As opções de bairro e espécie são extraídas dos registros reais da consulta inicial
sem filtros ao mapa e mantidas durante a pesquisa. Não constituem uma lista oficial
de bairros nem incluem espécies sem indivíduos no inventário. Não há endpoint de
catálogo de bairros no backend atual. Uma falha nessa consulta impede carregar essas
opções, sem gerar valores presumidos.

### MapTiler Satellite

O mapa normal mantém OpenStreetMap. A opção Satélite usa o estilo `satellite-v4`
do MapTiler Cloud, imagens reais com os créditos fornecidos pelo estilo. O controle
de atribuição do MapLibre permanece visível e o logotipo vinculado do MapTiler é
exibido nessa base, inclusive para atender ao plano gratuito.

1. Crie uma chave de navegador dedicada no MapTiler Cloud, verificando as condições,
   cotas e disponibilidade de imagens do plano contratado.
2. Restrinja **Allowed HTTP origins** aos domínios autorizados (origem com protocolo,
   host e porta). Para desenvolvimento, use outra chave restrita à origem local.
3. Copie `.env.example` para `.env.local` e preencha `VITE_MAPTILER_API_KEY`.
4. Reinicie o servidor Vite ou gere novamente o build para aplicar a configuração.

Variáveis `VITE_*` são públicas no navegador e no bundle; não use credenciais secretas
ou uma chave administrativa. Não versione `.env.local`. Sem chave, Satélite fica
indisponível com explicação e o mapa normal continua funcionando. Uma chave inválida,
quota excedida ou falha do provedor gera erro visível; o usuário pode voltar a Mapa.
A troca de base mantém câmera, marcadores DOM e seleção, sem recriar o mapa.

Documentação oficial: [chaves e restrições de origem](https://docs.maptiler.com/guides/developer/credentials/api-key/),
[atribuição](https://docs.maptiler.com/guides/map-design/attribution/add-attribution/),
[API](https://docs.maptiler.com/cloud/api/reference/) e
[condições e créditos](https://www.maptiler.com/copyright/).

Verificações T13 sem DOM (mocks somente nos testes):

```sh
npx --no-install vitest run src/__tests__/inventarioConsulta.spec.ts src/__tests__/inventarioInterface.spec.ts src/__tests__/inventarioMapa.spec.ts src/__tests__/inventarioService.spec.ts src/__tests__/inventarioStore.spec.ts --environment node
```
