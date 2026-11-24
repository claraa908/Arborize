# Design

## Context

A rota pública `home` já renderiza `HomeView.vue` dentro de `PublicLayout.vue`. A view hoje contém título, lorem ipsum e classes emerald. O layout público fornece a área vertical rolável e o padding geral. `ArborizacaoInicioView.vue` fornece a referência de largura, hierarquia, cartões e ações, pois esta tela não tem protótipo no Figma.

## Goals / Non-Goals

- Apresentar a jornada completa em uma página informativa, mobile first, com conteúdo de fácil manutenção e navegação acessível.
- Limitar a implementação a `HomeView.vue`, componentes em `frontend/src/components/home/`, conteúdo em `frontend/src/data/` e ao teste de componente no frontend.
- Manter fora do escopo TopBar, PublicLayout, roteador, outras telas, backend, tabelas, Docker Compose, lockfile e dependências novas.

## Decisions

### Composição da página

- Usar a largura `max-w-5xl` de Arborização e centralizar a composição no espaço do `PublicLayout`. Aplicar a fonte do tema, títulos em `text-amma-green-dark`, texto em `text-grey-text`, superfícies brancas ou verde-claro e `rounded-box` e `rounded-btn`.
- Compor a página com um único `h1` e seções de `h2`. A abertura contém texto institucional curto e centralizado, com o formato arredondado e a sombra das capas de Eventos e Biblioteca. O fundo usa um degradê verde com curvas suaves em CSS. A linha verde da jornada é decorativa. O texto e a ordem semântica comunicam a sequência. As dicas encerram a página.
- Usar os destinos confirmados em `router/index.ts`. A escolha da árvore e o pedido de muda com recicláveis começam por `arborizacao-assistente`, pois o formulário de muda depende da espécie selecionada. O link do último passo aponta para `#como-plantar`. Nas dicas pertinentes, a avaliação do Nim aponta para `arborizacao-substituicao-nim` e o acervo para `biblioteca-catalogo`. O fluxo de autenticação dos links protegidos continua sendo o do portal.

### Conteúdo local

- Manter conteúdo estático tipado em `frontend/src/data/homeContent.ts`, com categorias e IDs estáveis para facilitar revisão. Organizar o tutorial como lista ordenada e o processo como lista vertical com quatro colunas em desktop. Os textos devem ser curtos, em português brasileiro, sem medidas, estatísticas ou prazos novos.
- Preparar 12 cartões iniciais. “Cuidados com a muda” tem rega profunda e espaçada, cobertura de folhas secas, proteção contra pisoteio e orientação para avisar a AMMA em caso de dano. “Conheça seu solo” tem uma dica para cada descrição dada no pedido. “Clima e época” combina água e início das chuvas em um cartão e horários mais amenos em outro. “Árvores nativas” tem nativas e fauna local, porte compatível com fiação e avaliação da substituição de Nim.
- Tratar os três exemplos de solo como complemento educativo ao assistente. O quiz atual pergunta sobre solo seco ou raso e disponibilidade de irrigação. O conteúdo da página pode relacionar solo e água à escolha, preservando os rótulos e valores reais do quiz.
- Usar o perfil municipal para contextualização de clima, solos e Caatinga e materiais técnicos para embasar os cuidados. Os textos da página usam apenas as informações solicitadas e serão revisados pela AMMA. [Perfil municipal de Quixadá — IPECE](https://www.ipece.ce.gov.br/wp-content/uploads/sites/45/2018/09/Quixada_2017.pdf) · [Plantio de mudas — Embrapa](https://www.atermaisdigital.cnptia.embrapa.br/web/restauracao-de-ecossistemas/plantio-de-mudas) · [Época e umidade do solo — Embrapa](https://www.webambiente.cnptia.embrapa.br/webambiente/wiki/doku.php?id=webambiente:plantio_mudas)

### Carrossel

- Implementar o carrossel em um componente Vue com `<script setup lang="ts">`, valores reativos locais, faixa `overflow-x-auto` e CSS scroll-snap. Usar largura calculada com os espaçamentos para um cartão e meio no celular, dois no tablet e três no desktop. Usar `min-w-0` no componente e manter a largura da rolagem contida nele.
- Usar chips com botões e `aria-pressed`, sem o filtro “Todas”. O filtro redefine o ponto de rolagem para o início e os rótulos acessíveis dos cartões passam a usar o total filtrado. Os nomes das categorias ficam nos filtros. Os cartões exibem o título da dica ao lado do ícone. Usar rolagem nativa por toque e trackpad.
- Centralizar as setas na base da faixa e sincronizar seus estados pelo deslocamento real, incluindo toque, filtro e redimensionamento. A numeração fica nos rótulos acessíveis dos cartões. Em larguras com vários cartões visíveis, a seta Próximo desabilita quando o último cartão já está totalmente visível. Usar tolerância de arredondamento ao detectar o fim. Quando todos os cartões cabem, as duas setas ficam desabilitadas.
- Navegar com as teclas direcionais quando a faixa tem foco. Preservar Tab e a ativação normal dos botões. Usar região rotulada com `aria-roledescription="carrossel"`, botões “Anterior” e “Próximo” com foco visível e slides com `role="group"`, `aria-roledescription="slide"` e rótulos posicionais. A preferência de movimento reduzido desliga a animação de rolagem.

### Redação inicial

| Passo do tutorial | Texto proposto |
| --- | --- |
| Escolha o lugar certo | Plante longe de fios, muros e canos. Pense no tamanho da árvore adulta e use o assistente para escolher. |
| Prepare a cova | Abra uma cova larga e funda e solte a terra do fundo e das laterais. Se tiver composto orgânico, misture-o à terra. |
| Plante com cuidado | Retire a embalagem sem desfazer o torrão. Deixe o colo da muda no nível do chão e pressione a terra de leve. |
| Regue bem | Faça uma primeira rega generosa. Depois, regue em profundidade e com intervalos, de preferência com gotejamento lento. |
| Proteja | Use tutor e garajau para proteger a muda de pisoteio e animais. Espalhe folhas secas ao redor, deixando espaço junto ao tronco. |
| Acompanhe | Nos primeiros 12 a 24 meses, observe a água, as ervas daninhas e as pragas. Se a muda murchar ou sofrer danos, avise a AMMA. |

| Categoria | Dica | Texto proposto |
| --- | --- | --- |
| Cuidados com a muda | Regue em profundidade | Faça regas profundas e espaçadas, conforme a necessidade da muda. |
| Cuidados com a muda | Cubra o solo | Folhas secas ajudam a manter a umidade. Deixe espaço junto ao tronco. |
| Cuidados com a muda | Evite o pisoteio | Proteja a muda com um garajau para reduzir danos de passos e animais. |
| Cuidados com a muda | Avise a AMMA em caso de dano | Se houver galhos quebrados ou outros danos, procure a AMMA antes de podar. |
| Conheça seu solo | Solo arenoso e solto | A água escoa rápido. Observe a umidade e, se necessário, encurte o intervalo das regas e cubra o solo. |
| Conheça seu solo | Solo escuro e fofo | A terra solta favorece as raízes. Regue com cuidado para evitar encharcamento. |
| Conheça seu solo | Solo duro ou compactado | Afrouxe a terra e abra uma cova mais larga. |
| Clima e época | Aproveite o início das chuvas | No semiárido, a água é essencial enquanto a muda se estabelece. O início do período de chuvas ajuda a muda a pegar. |
| Clima e época | Prefira horas mais frescas | Em dias de sol forte, cuide da muda cedo pela manhã ou no fim da tarde. |
| Árvores nativas | Dê preferência às nativas | Espécies da Caatinga combinam com o clima e a fauna locais. Veja o catálogo na Biblioteca Digital. |
| Árvores nativas | Observe a fiação | Se o local for adequado e tiver fiação, prefira uma espécie de pequeno porte indicada pelo assistente. |
| Árvores nativas | Solicite a substituição do Nim | O Nim Indiano é uma espécie exótica invasora. Você pode pedir uma avaliação para substituição. |

## Risks / Trade-offs

- Com múltiplos cartões visíveis, o último ponto de rolagem pode começar antes do último cartão → usar a posição real para desabilitar setas e manter rótulos individuais de todos os cartões.
- O tutorial recomenda evitar a fiação e uma dica aborda espaços sob fiação → explicitar que a escolha de porte pequeno pelo assistente se aplica quando o local é adequado e a proximidade com a rede precisa ser considerada.
- O texto solicitado cita Eco Ponto e as telas de conversão e acompanhamento atualmente indicam a AMMA → preservar a redação pedida na proposta e incluir orientação para seguir o local indicado no pedido. A AMMA deve confirmar a redação definitiva na revisão editorial.

## Open Questions

- A AMMA deve revisar e aprovar as orientações educativas antes da integração à `main`, incluindo a referência ao local de entrega dos recicláveis.
