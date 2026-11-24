# Spec Delta

## Purpose

Apresentar o Arborize à população de Quixadá e oferecer orientação pública sobre a escolha, a solicitação, o plantio e os cuidados com mudas na Caatinga.

## ADDED Requirements

### Requirement: Boas-vindas na página pública Início

A rota `home` SHALL apresentar conteúdo educativo em quatro seções ordenadas sob o layout público existente.

#### Scenario: Acesso inicial à página pública

- **WHEN** o visitante abre a página Início
- **THEN** vê nesta ordem Boas-vindas, Como funciona, Como plantar sua muda e Dicas da Caatinga
- **AND** a abertura apresenta o único `h1` da página, “Bem-vindo ao Arborize”, e uma frase de propósito em um cartão textual com fundo verde

#### Scenario: Uso dos atalhos de boas-vindas

- **WHEN** o visitante lê a seção de boas-vindas
- **THEN** os links do quiz e do guia aparecem junto das etapas em “Como funciona”

#### Scenario: Leitura e foco em celular e desktop

- **WHEN** o visitante usa a página em uma viewport pequena ou grande e navega com o teclado
- **THEN** os títulos seguem a hierarquia semântica, os links têm texto claro e os controles têm foco visível com contraste adequado
- **AND** o carrossel concentra a rolagem horizontal e a página mantém sua largura dentro da área disponível

### Requirement: Apresentação do Arborize e serviços

A página inicial SHALL apresentar o contexto da plataforma da AMMA no texto de boas-vindas, sem uma seção separada intitulada “O que é o Arborize”.

#### Scenario: Destinos dos cartões de serviço

- **WHEN** o visitante lê a página inicial
- **THEN** não encontra cartões separados de apresentação dos serviços nem a aba “O que é o Arborize”

### Requirement: Explicação do processo de solicitação

A seção “Como funciona” SHALL apresentar Descubra pelo quiz, Peça pelo formulário de muda, Entregue os recicláveis no Eco Ponto e Retire sua muda e plante. O texto SHALL informar que o visitante vê a conversão antes do envio e orientar a seguir a indicação de entrega do pedido.

#### Scenario: Leitura do processo em diferentes larguras

- **WHEN** o visitante lê “Como funciona” no celular ou no desktop
- **THEN** vê os quatro passos na mesma ordem, em lista vertical no celular e em quatro colunas no desktop, ligados por uma linha verde discreta

### Requirement: Tutorial de plantio

A seção “Como plantar sua muda” SHALL apresentar na âncora `#como-plantar` seis passos com ícone, título e texto curto. Uma dica rápida complementar é opcional.

#### Scenario: Leitura dos seis passos

- **WHEN** o visitante percorre o tutorial
- **THEN** encontra, nesta ordem, “Escolha o lugar certo”, “Prepare a cova”, “Plante com cuidado”, “Regue bem”, “Proteja” e “Acompanhe”
- **AND** os passos explicam espaço para a árvore adulta e distância de fios, muros e canos, cova e composto orgânico opcional, torrão intacto e colo ao nível do chão, rega inicial e cuidados de irrigação, tutor e garajau com cobertura afastada do tronco e acompanhamento nos primeiros 12 a 24 meses com orientação para avisar a AMMA em caso de dano ou murcha

### Requirement: Dicas da Caatinga com filtros

A seção “Dicas da Caatinga” SHALL oferecer cerca de 12 dicas estáticas adequadas a Quixadá. Cada cartão SHALL trazer o título da dica ao lado do ícone e uma ou duas frases abaixo. Os nomes das categorias SHALL aparecer nos filtros.

#### Scenario: Leitura e filtragem das dicas

- **WHEN** o visitante seleciona “Cuidados com a muda”, “Conheça seu solo”, “Clima e época” ou “Árvores nativas”
- **THEN** o carrossel exibe somente as dicas dessa categoria e reinicia no primeiro item
- **AND** o filtro “Todas” não é exibido

#### Scenario: Conteúdo regional

- **WHEN** o visitante lê as dicas das quatro categorias
- **THEN** encontra orientações de rega, cobertura, proteção e contato com a AMMA, cuidados nos solos arenoso, escuro e fofo ou compactado, planejamento conforme as chuvas e horários mais amenos, além de espécies nativas, porte sob fiação e substituição de Nim

### Requirement: Navegação acessível no carrossel

O carrossel SHALL operar sem autoplay, com setas Anterior e Próximo centralizadas abaixo dos cartões, rolagem por toque e navegação por teclado. SHALL respeitar a preferência por movimento reduzido.

#### Scenario: Limites e posição em rolagem manual

- **WHEN** o visitante navega pelas setas, por toque ou pelo teclado
- **THEN** a seta Anterior fica desabilitada no início e a seta Próximo fica desabilitada no último ponto alcançável
- **AND** as setas são os únicos controles de navegação visíveis abaixo dos cartões

#### Scenario: Quantidade visível e semântica

- **WHEN** o carrossel é exibido no celular, tablet ou desktop
- **THEN** a faixa tem capacidade para um cartão e meio, dois cartões ou três cartões, respectivamente, quando houver itens suficientes
- **AND** a região tem rótulo acessível e `aria-roledescription="carrossel"`, e cada cartão tem `role="group"`, `aria-roledescription="slide"` e rótulo “n de N” relativo à lista filtrada

### Requirement: Conteúdo inicial independente do backend

A página Início SHALL exibir seu conteúdo educativo a partir do frontend, sem depender de dados remotos para o tutorial e as dicas.

#### Scenario: Renderização com a API indisponível

- **WHEN** o visitante abre `home` com o backend indisponível
- **THEN** o conteúdo da página inicial e os controles locais do carrossel continuam disponíveis

### Requirement: Redação e revisão do conteúdo educativo

O conteúdo SHALL ser escrito em português brasileiro, com frases curtas, em tom acolhedor e direto. A redação SHALL se limitar às orientações fornecidas, com “12 a 24 meses” como único dado numérico educativo. A numeração estrutural das etapas do tutorial permanece aplicável.

#### Scenario: Questão em aberto para revisão da AMMA

- **WHEN** a mudança estiver pronta para integrar a `main`
- **THEN** a revisão do conteúdo educativo pela AMMA deve estar registrada e a orientação sobre o ponto de entrega dos recicláveis deve estar confirmada
