# Design — Solicitação de Muda

## Referência visual

- O visual segue **apenas** `docs/padroes-de-projeto.md`: use as **classes de token** (`amma-green`, `amma-green-dark`, `amma-green-light`, `amma-green-lighter`, `amma-bg`, `grey-text`, `feedback-*`), a fonte do tema, os formatos de borda (`rounded-btn`, `rounded-box`), os botões, campos e componentes descritos lá. Não use valores hexadecimais.
- Não existe protótipo visual para esta funcionalidade. A estrutura das telas, os campos e os textos estão descritos abaixo; a composição visual deve ser feita com os padrões do projeto e com as telas públicas já existentes como referência de consistência.
- Se algo visual não estiver coberto pelos padrões, adote a opção mais simples e consistente com o restante do sistema e registre a decisão na entrega.

## Objetivo da experiência

A pessoa deve conseguir pedir mudas de forma simples, entender o custo em recicláveis antes de confirmar e saber qual é o próximo passo depois do envio.

## Histórias atendidas

- **HU01:** formulário "Solicitar muda".
- **HU02:** tela "Conversão Eco Ponto".
- **HU03:** botão "Enviar Solicitação" e número do pedido.
- **HU04:** tela "Acompanhar solicitação" logo após o envio.

## Fluxo principal

1. A pessoa chega do assistente com a espécie selecionada (ou é redirecionada ao assistente).
2. O sistema mostra o formulário com a espécie já preenchida e quantidade 1.
3. A pessoa ajusta a quantidade, preenche endereço e identificação e clica em "Continuar".
4. O sistema valida, busca a conversão no backend e mostra "Conversão Eco Ponto".
5. A pessoa confere e clica em "Enviar Solicitação" (ou em "Voltar" para corrigir).
6. O sistema cria a solicitação e mostra "Acompanhar solicitação" com o número e o status.

## Estrutura da interface

Páginas públicas em `PublicLayout.vue` (menu com **Arborização** ativo), com um cartão branco central (`rounded-box`) sobre `amma-bg`.

### Tela 1 — Solicitar muda

1. **Cabeçalho:** texto pequeno "Solicitação", título "Solicitar muda", subtítulo "Confirme a espécie, informe a quantidade e o endereço de destino e preencha seus dados de identificação."
2. **Cartão "Solicitação"** (esquerda)
   - Cartão da espécie: miniatura, nome popular e nome científico.
   - Seletor de quantidade com "−", valor e "+" e rótulo "Quantidade de mudas".
   - Campos: Rua, Número, Bairro, Complemento.
3. **Cartão "Identificação"** (direita): Nome Completo, CPF, Telefone.
4. **Rodapé:** botão primário "Continuar" à direita.

### Tela 2 — Conversão Eco Ponto

1. **Cabeçalho:** "ECO Ponto", título "Conversão Eco Ponto", subtítulo "Confira a contrapartida de recicláveis antes de enviar o pedido."
2. **Painel verde (`bg-amma-green`, esquerda):** "Para N mudas de [espécie]", número grande de garrafas, "garrafas PET" e ilustração discreta de reciclagem.
3. **Resumo (direita):** linhas "Quantidade de mudas", "Espécie" e "Recicláveis necessários".
4. **Rodapé:** "Voltar" (secundário) e "Enviar Solicitação" (primário).

### Tela 3 — Acompanhar solicitação

1. **Cabeçalho:** "Solicitação de Muda", título "Acompanhar solicitação", subtítulo "Veja o andamento do seu pedido e as próximas etapas."
2. **Cartão de andamento:** "Solicitação #000123", selo de status ("Aguardando recicláveis") e linha do tempo com 4 etapas:
   - Solicitação Enviada: "Pedido registrado no Arborize."
   - Aguardando recicláveis: "Entregue os materiais no Eco Ponto."
   - Aprovação da AMMA: "Será liberada após a conferência."
   - Retirada da muda: "Apresente o comprovante no ponto de retirada."
   - Concluída: ícone de confirmação em `feedback-success`; em andamento: ícone de ampulheta em `feedback-warning`; bloqueada: cadeado em cinza. Cada etapa tem também o **status em texto**.
3. **Resumo do Pedido:** solicitante, espécie, quantidade e recicláveis.
4. Aprovação pela AMMA e comprovante de retirada ficam fora desta spec. Não exibir "Ver Comprovante".

## Campos e controles

Classe-base dos campos (do padrão do projeto): `w-full bg-white focus:bg-amma-green-lighter border-2 border-amma-green-light focus:border-amma-green rounded-btn py-3 px-4 text-grey-text outline-none transition-all text-sm font-medium`. Rótulos: `text-sm font-semibold text-grey-text`, com `label for`/`id`. Erros: texto em `feedback-danger` junto ao campo, `aria-invalid` e `aria-describedby`.

### Quantidade de mudas

- Tipo: seletor numérico com botões circulares "−" e "+" (com `aria-label` "Diminuir quantidade" e "Aumentar quantidade").
- Obrigatório: sim (padrão 1). Inteiro entre 1 e o máximo configurável.
- Comportamento: "−" não passa de 1; ao chegar no máximo, "+" fica desabilitado e um texto informa o limite.

### Rua, Número, Bairro, Complemento

- Texto. Obrigatório: Rua, Número e Bairro; Complemento não.
- Validação: "Preencha este campo."; remover espaços das pontas.

### Nome Completo

- Texto, obrigatório; pelo menos nome e sobrenome. Mensagem: "Informe seu nome completo."

### CPF

- Texto com máscara `000.000.000-00` (função utilitária própria), obrigatório.
- Validação: formato e dígitos verificadores: "Informe um CPF válido." Não registrar em log.

### Telefone

- Texto com máscara `(00) 00000-0000`, obrigatório. Validação: "Informe um telefone com DDD."

### Botões

- **Continuar** (primário): valida tudo, mantém os valores e vai para a conversão.
- **Voltar** (secundário): retorna ao formulário preservando os dados.
- **Enviar Solicitação** (primário): envia ao backend; desabilitado e com indicador de carregamento enquanto envia.

## Estados da interface

- **Inicial:** espécie preenchida, quantidade 1, demais campos vazios.
- **Carregando:** ao buscar a conversão e ao enviar, indicador de carregamento e botões desabilitados.
- **Sucesso:** tela de acompanhamento com o número da solicitação.
- **Erro:** mensagem **inline** em destaque (`feedback-danger*`, com ícone e texto, `role="alert"`): "Não foi possível enviar sua solicitação agora. Seus dados foram mantidos." com "Tentar novamente". Erros de campo ficam junto a cada campo. O `AppToastContainer` só existe no layout autenticado, então não depender dele nas páginas públicas.
- **Sem espécie:** redirecionamento ao assistente com a mensagem da RN01.

## Arquitetura do frontend

Seguir `docs/padroes-de-projeto.md`, seção 2. Nomes sugeridos (conferir a convenção das views públicas existentes):

- **Rotas** nomeadas e lazy no layout público: `arborizacao-solicitar`, `arborizacao-conversao`, `arborizacao-acompanhar`.
- **Views** orquestram; **componentes** em pasta de domínio (espécie, seletor de quantidade, linha do tempo, resumo) recebem props e emitem eventos.
- **Store Pinia** (ex.: `solicitacaoMudaStore`): dados do formulário, conversão, resultado do envio, estados de carregamento e erro por operação, usando a espécie da store do assistente.
- **Service** (ex.: `solicitacaoMudaService`) via `services/api.ts`; preserva o status HTTP e o `ProblemDetail` e não esconde falha como sucesso.
- **Utilitários** de máscara e validação de CPF/telefone separados dos componentes. **Tipos** em `types/`.
- `<script setup lang="ts">` nos componentes novos.

## Contrato da API (proposta)

- `GET /api/solicitacoes-muda/conversao?quantidade=3`: devolve `quantidade`, `garrafasPorMuda` e `totalGarrafas`. Quantidade fora do limite: `400`.
- `POST /api/solicitacoes-muda`: corpo JSON com `especieId`, `quantidadeMudas`, `rua`, `numeroEndereco`, `bairro`, `complemento`, `nomeSolicitante`, `cpf` e `telefone`. Resposta `201 Created` com `Location` e corpo contendo `id`, `numero` (formatado), `status`, `especie` (id, nome popular), `quantidadeMudas`, `totalRecicaveis`, `nomeSolicitante` e `criadaEm`. **Não devolve CPF, telefone nem endereço.**
- Entrada em record com Bean Validation e `@Valid @RequestBody`; regras repetidas no Service (quantidade máxima, espécie existente). Validador de CPF em `validation/`.
- Erros em `ProblemDetail` (`type`, `title`, `status`, `detail`, `code`, `errors` por campo): `400` entrada inválida, `404` espécie inexistente, `500` sanitizado. Advice próprio limitado ao novo controller.
- Parâmetros configuráveis: `arborize.solicitacao.garrafas-por-muda` (10) e `arborize.solicitacao.max-mudas` (5, provisório).
- Não existe `GET` público de solicitação por número (RN12).
- Ajustar nomes ao padrão do `backend/arborize` (`controllers`, `dtos/request`, `dtos/response`, `services`, `repositories`, `entities`, `enums`, `mappers`).

## Responsividade

- **Desktop:** duas colunas (Solicitação e Identificação) na tela 1; painel verde e resumo lado a lado na tela 2; andamento e resumo lado a lado na tela 3.
- **Mobile (prioridade, tela pública):** colunas empilhadas; campos em largura total; botões grandes e fáceis de tocar; painel verde acima do resumo.

## Acessibilidade

- Todos os campos com `label` associado e indicação textual de obrigatório; erros ligados ao campo e anunciados.
- Ao falhar a validação, o foco vai para o primeiro campo com erro.
- Botões "−" e "+" com rótulo acessível e valor anunciado.
- Linha do tempo como lista ordenada, com o estado de cada etapa em texto ("concluída", "em andamento", "bloqueada"), não só ícone ou cor.
- As máscaras de CPF e telefone não podem atrapalhar leitores de tela nem o preenchimento por teclado.

## Critérios de aceitação

- [ ] As três telas seguem a estrutura deste design, com tokens e componentes do `docs/padroes-de-projeto.md`.
- [ ] A espécie do assistente chega preenchida no formulário.
- [ ] Validações de obrigatórios, CPF e telefone funcionam no navegador e no servidor.
- [ ] A conversão exibida vem do backend e muda com a quantidade.
- [ ] "Voltar" preserva os dados; o botão de envio fica desabilitado durante o envio.
- [ ] A solicitação é salva com número, status "Aguardando recicláveis" e total de recicláveis.
- [ ] Erros claros e inline; CPF e telefone fora de logs e de respostas da API.
- [ ] Funciona em desktop e celular e é utilizável pelo teclado.
