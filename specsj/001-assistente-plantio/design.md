# Design — Assistente de Plantio

## Referência visual

- O visual segue **apenas** `docs/padroes-de-projeto.md`: use as **classes de token** (`amma-green`, `amma-green-dark`, `amma-green-light`, `amma-green-lighter`, `amma-bg`, `grey-text`, `feedback-*`), a fonte do tema, os formatos de borda (`rounded-btn`, `rounded-box`), os botões, campos e componentes descritos lá. Não use valores hexadecimais.
- Não existe protótipo visual para esta funcionalidade. A estrutura das telas, os campos e os textos estão descritos abaixo; a composição visual deve ser feita com os padrões do projeto e com as telas públicas já existentes como referência de consistência.
- Se algo visual não estiver coberto pelos padrões, adote a opção mais simples e consistente com o restante do sistema e registre a decisão na entrega.

## Objetivo da experiência

A pessoa deve conseguir descrever o local de plantio respondendo perguntas rápidas e entender por que aquelas espécies foram indicadas, terminando com uma escolha clara.

## Histórias atendidas

- **HU01:** quiz com uma pergunta por vez, progresso visível e possibilidade de voltar.
- **HU02:** tela de resultados com cartões de espécies adequadas.
- **HU03:** botão "Refazer Quiz" na tela de resultados.
- **HU04:** botão "Selecionar Espécie" em cada cartão, levando ao formulário.

## Fluxo principal

1. A pessoa abre a aba **Arborização** e inicia o assistente.
2. O sistema mostra a pergunta 1 de 10 com as opções.
3. A pessoa escolhe uma opção e clica em "Próxima"; repete até a última pergunta, onde o botão passa a ser "Finalizar".
4. O sistema envia as respostas ao backend e mostra "Espécies recomendadas".
5. A pessoa clica em "Selecionar Espécie" e segue para o formulário (`002-solicitar-muda`), ou em "Refazer Quiz" para recomeçar.

## Estrutura da interface

Páginas públicas, dentro de `PublicLayout.vue` (menu **Início · Eventos · Arborização (ativo) · Sobre Nós**), com um cartão branco central (`rounded-box`) sobre o fundo `amma-bg`.

### Tela 1 — Assistente de Plantio

1. **Cabeçalho**
   - Texto pequeno "Serviço ao Cidadão" (`text-xs uppercase tracking-wider`).
   - Título "Assistente de Plantio" (`text-2xl font-bold text-amma-green-dark`).
   - Subtítulo: "Responda algumas perguntas rápidas sobre o local do plantio. Com base nas informações fornecidas, o Arborize indicará espécies adequadas para a sua solicitação." (`text-sm text-grey-text`).
2. **Barra de progresso:** 10 segmentos; concluídos e atual em `amma-green`, os demais em `amma-green-light`.
3. **Cartão da pergunta**
   - Rótulo "PERGUNTA N DE 10".
   - Enunciado em destaque e texto de apoio.
   - Opções: cartões selecionáveis com ilustração/ícone à esquerda, título e descrição curta. Normal: `border-2 border-amma-green-light`; selecionada: `border-amma-green bg-amma-green-lighter`.
4. **Rodapé de ações:** "Voltar" (secundário, oculto na pergunta 1) e "Próxima" (primário). Na última pergunta, "Próxima" vira "Finalizar".

Perguntas definidas para esta sprint:

| Nº | Enunciado | Opções |
|---|---|---|
| 1 | Onde a muda será plantada? (Escolha o ambiente principal de plantio.) | Calçada · Quintal |
| 2 | Quanto espaço há para a árvore crescer? (Considere o espaço livre ao redor do local de plantio.) | Pouco espaço · Espaço razoável · Bastante espaço · Não sei informar |
| 3 | Existe fiação elétrica sobre o local? | Sim · Não · Não tenho certeza |
| 4–9 | **A definir** (ver "Questões em aberto" em `requirements.md`) | — |
| 10 | Como é a terra no local onde você pretende plantar? | Meio arenosa e solta · Escura e fofa · Meio dura ou compactada · Não sei informar |

Textos de apoio e descrições curtas das opções: escrever em português simples e direto, no tom do restante do sistema, e deixar o grupo revisar.

### Tela 2 — Espécies recomendadas

1. **Cabeçalho:** "Resultado do assistente", título "Espécies recomendadas", subtítulo "Com base nas suas respostas, estas espécies são as mais adequadas para o local informado."
2. **Grade de até 3 cartões**, cada um com:
   - foto da espécie no topo;
   - nome popular e nome científico (em itálico);
   - selo "Adequada às condições informadas" com ícone de confirmação, nas cores `feedback-success`/`feedback-success-bg`/`feedback-success-border`;
   - etiqueta de porte (ex.: "Porte Médio");
   - botão secundário "Selecionar Espécie".
3. **Rodapé:** botão secundário central "Refazer Quiz" com ícone de reiniciar.

## Campos e controles

### Opção de resposta (cartão selecionável)

- Tipo: seleção única (radio estilizado como cartão).
- Obrigatório: sim, uma opção por pergunta.
- Validação: sem opção escolhida, "Próxima" não avança e mostra "Escolha uma opção para continuar." (cores `feedback-danger*`, com texto e ícone), associada às opções por `aria-describedby`.
- Comportamento: a seleção se mantém ao voltar e avançar.

### Botões

- **Próxima / Finalizar** (primário: `bg-amma-green text-white hover:bg-amma-green-dark font-bold rounded-btn py-2.5 px-6 efeito-zoom`): valida a resposta atual e avança; em "Finalizar" envia as respostas e mostra carregamento.
- **Voltar, Selecionar Espécie, Refazer Quiz** (secundário: `bg-white border-2 border-amma-green text-amma-green hover:bg-amma-green-lighter rounded-btn font-bold efeito-zoom`).
- Estado desabilitado visível durante carregamento.

## Estados da interface

- **Inicial:** pergunta 1 sem opção selecionada, "Voltar" oculto.
- **Carregando:** após "Finalizar", spinner ou esqueleto na área de resultados, botões desabilitados.
- **Sucesso:** cartões de espécies recomendadas.
- **Erro:** mensagem **inline** (`role="alert"`) em `feedback-danger*`: "Não foi possível buscar as recomendações agora. Suas respostas foram mantidas." com botão "Tentar novamente". Não depender de toast: o `AppToastContainer` só é montado no layout autenticado.
- **Vazio:** "Não encontramos espécies adequadas para essas condições." com botão "Refazer Quiz".
- **Validação:** mensagem inline ao tentar avançar sem resposta.

## Arquitetura do frontend

Seguir `docs/padroes-de-projeto.md`, seção 2. Nomes sugeridos (conferir a convenção de pastas das views públicas existentes):

- **Rotas** nomeadas e carregadas com lazy loading em `router/index.ts`, sob o layout público: `arborizacao-assistente` e `arborizacao-recomendacoes` (caminhos `/arborizacao/...`).
- **Views:** uma por tela, apenas orquestrando.
- **Componentes** de apresentação em uma pasta de domínio (ex.: `components/arborizacao/`): pergunta, barra de progresso, cartão de espécie.
- **Perguntas** em arquivo tipado (lista com id, enunciado, texto de apoio e opções).
- **Store Pinia** (ex.: `quizStore`): respostas, passo atual, recomendações, espécie selecionada, estados de carregamento e erro. `null` significa "ainda não carregado" e lista vazia significa "sem resultados". Carregamento sempre encerrado em `finally`.
- **Service** (ex.: `recomendacaoService`) usa a instância Axios de `services/api.ts`, preserva o status HTTP e o `ProblemDetail` ao falhar, e **não** transforma erro em lista vazia.
- **Tipos** em `types/`.
- `<script setup lang="ts">` em todos os componentes novos.

## Contrato da API (proposta)

`POST /api/recomendacoes-especies` (consulta que não altera estado; `POST` apenas para enviar as respostas no corpo)

- **Entrada** (record com Bean Validation, `@Valid @RequestBody`): lista de respostas, cada uma com o identificador da pergunta e o valor escolhido.
- **Saída `200`** (record): lista de até 3 espécies com `id`, `nomePopular`, `nomeCientifico`, `porte` e referência da foto conforme o mecanismo atual do catálogo.
- **Erros** (`ProblemDetail`, `application/problem+json`, com `type`, `title`, `status`, `detail`, `code` e `errors` com campo e mensagem): `400` para respostas inválidas; `500` com detalhe sanitizado.
- O `@RestControllerAdvice` de `ArvoreExceptionHandler` não é global. Criar um advice próprio, limitado ao novo controller (`assignableTypes`).
- Ajustar nomes ao padrão dos DTOs (`dtos/request`, `dtos/response`) e do pacote `br.com.arborize`.

## Responsividade

- **Desktop:** cartão central largo; 2 colunas de opções quando houver 4 opções; resultados em 3 colunas.
- **Mobile (prioridade, tela pública):** opções em uma coluna, botões com área de toque confortável, resultados em 1 coluna.

## Acessibilidade

- Cada pergunta é um grupo (`fieldset` com `legend`) de botões de rádio com rótulos associados.
- Navegação por teclado completa e foco visível (campos e botões com anel de foco).
- Ao trocar de pergunta, o foco vai para o enunciado e o progresso é anunciado ("Pergunta 2 de 10").
- Mensagens de erro e validação em região `aria-live` ou `role="alert"`, ligadas ao controle por `aria-describedby`.
- Texto alternativo nas fotos; selo de adequação com texto, não só ícone ou cor.

## Critérios de aceitação

- [ ] As telas seguem a estrutura deste design, usando os tokens e componentes do `docs/padroes-de-projeto.md`.
- [ ] O quiz percorre todas as perguntas configuradas, com progresso correto.
- [ ] Não é possível avançar sem resposta; "Voltar" preserva as respostas.
- [ ] A tela de resultados mostra até 3 espécies nativas adequadas, vindas da API.
- [ ] Estados de carregamento, erro e vazio funcionam, com erro inline.
- [ ] "Refazer Quiz" limpa tudo; "Selecionar Espécie" leva ao formulário com a espécie.
- [ ] A API valida a entrada no servidor e responde erros em `ProblemDetail`.
- [ ] Funciona em desktop e celular e é utilizável pelo teclado.
