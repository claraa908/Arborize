# Tasks

## 1. Conteúdo e composição da página

- [x] 1.1 Criar conteúdo estático tipado em `frontend/src/data/`. Conferir os seis passos, 12 dicas iniciais, categorias, textos em português brasileiro e destinos em `router/index.ts`.
- [ ] 1.2 Refinar `HomeView.vue` e seus componentes conforme o design. Verificar ordem, `h1` único, cartão de abertura, alinhamento dos ícones, tutorial e carrossel em celular e desktop.
- [ ] 1.3 Conectar os links do quiz e do guia aos passos correspondentes e os links de Nim e Biblioteca às dicas. Verificar a autenticação herdada do quiz.

## 2. Carrossel acessível

- [ ] 2.1 Implementar scroll-snap, filtros, setas centralizadas e teclado com estado local. Verificar ausência do filtro “Todas” e do contador visual, filtro por categoria, reset de posição, limites, rótulos acessíveis, ausência de autoplay e respeito ao movimento reduzido.
- [x] 2.2 Adicionar teste de componente com Vitest, jsdom e Vue Test Utils já instalados. Cobrir ausência de “Todas”, categorias e filtro.

## 3. Verificação integrada

- [ ] 3.1 Abrir Início em celular, tablet e desktop. Conferir contraste, foco, texto dos links, rolagem horizontal restrita ao carrossel e a quantidade de cartões visíveis por largura. Abrir a página com a API indisponível e conferir que o conteúdo local continua acessível.
- [ ] 3.2 Rodar `npm run type-check`, lint e `npm run test:unit -- --run` no frontend. Para lint, usar os verificadores existentes sem `--fix` para evitar alterações fora do escopo. Confirmar que o diff de implementação se limita à home, aos seus componentes, ao conteúdo tipado e ao teste.

## Workflow follow-up

- A página, a âncora e o carrossel já foram implementados. As tarefas que incluem verificação em navegador ou testes permanecem abertas.
- Antes de integrar à `main`, registrar a revisão da AMMA sobre o conteúdo educativo e a confirmação da orientação sobre a entrega dos recicláveis.
