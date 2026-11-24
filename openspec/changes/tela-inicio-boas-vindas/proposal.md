# Proposal

## Why

A página pública Início hoje traz apenas um título e texto de preenchimento. Ela deve apresentar o Arborize, plataforma da AMMA de Quixadá, e orientar a população desde a escolha de uma árvore até os primeiros cuidados com a muda.

## What Changes

- Apresentar o Arborize e seus serviços para descobrir a árvore certa, solicitar mudas, pedir avaliação para substituição de Nim Indiano e aprender sobre arborização.
- Organizar o conteúdo em torno de “A jornada da muda”, com as etapas Descubra, Peça, Plante e Cuide ligadas por uma linha verde discreta.
- Abrir com “Bem-vindo ao Arborize”, uma frase de propósito e o contexto da plataforma da AMMA em um cartão inspirado nas capas de Eventos e Biblioteca.
- Centralizar o conteúdo principal da página e alinhar os elementos visuais, sem o card cinza da jornada no cabeçalho.
- Mostrar “Como funciona” depois da apresentação, em quatro passos: Descubra pelo quiz, Peça pelo formulário, Entregue os recicláveis no Eco Ponto e Retire sua muda e plante. Explicar que a conversão aparece antes do envio do pedido. Usar lista vertical no celular e quatro colunas no desktop, com links para o quiz e o guia nos passos correspondentes.
- Apresentar “Como plantar sua muda” em seguida, com a âncora `#como-plantar`. O tutorial tem seis passos numerados, com ícone, título e texto curto em cada um. Uma dica rápida pode complementar o tutorial.
- Incluir o tutorial de plantio solicitado pela cliente e um carrossel de dicas sobre o clima, o solo e a Caatinga em Quixadá.
- Encerrar a página com as dicas da Caatinga, sem uma seção adicional de fechamento. A redação educativa fica sujeita à revisão da AMMA antes da integração à `main`.
- Usar o layout público e os padrões visuais existentes, com `ArborizacaoInicioView.vue` como referência de estilo.

## Capabilities

### New Capabilities

- `inicio-publico` — página pública de boas-vindas, apresentação institucional e educação sobre plantio e cuidados em Quixadá.

### Modified Capabilities

## Impact

- Frontend da rota pública `home` e conteúdo local de `HomeView.vue`. Eventuais links usam destinos já disponíveis no portal.
- O escopo é informativo e inclui conteúdo estático e interação local do carrossel. Novas rotas, backend, tabelas e mudanças nos fluxos dos serviços ficam fora da proposta.
