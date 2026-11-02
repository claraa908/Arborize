# Padrões de Projeto do Arborize

Este documento registra os padrões observados no código atual do Arborize para orientar novas telas, componentes, serviços e endpoints. Ele descreve convenções realmente presentes e sinaliza onde o projeto ainda não tem uniformidade. Exemplos de código devem seguir a arquitetura mais consistente e tipada disponível, sem reproduzir limitações pontuais de módulos legados.

## 1. Identidade visual e UI — Frontend

### 1.1 Base visual e paleta

O frontend utiliza Tailwind CSS com configuração CSS-first: `frontend/src/assets/main.css` importa `tailwindcss` e declara os tokens no bloco `@theme`. Portanto, os nomes abaixo são classes utilitárias Tailwind geradas a partir de variáveis CSS do tema, não uma lista de cores mantida em `tailwind.config.js`.

| Classe | Cor | Uso observado/recomendado |
| --- | --- | --- |
| `amma-green` | `#6B8B3F` | Ações primárias, ícones, elementos ativos e destaque botânico. |
| `amma-green-dark` | `#4D6E1F` | Títulos, texto de destaque e hover mais escuro de ações primárias. |
| `amma-green-light` | `#E1E8D9` | Bordas, divisores e estados de foco suave. |
| `amma-green-lighter` | `#F5F8F2` | Fundos suaves, hover de campos e superfícies de estado positivo/neutro. |
| `amma-bg` | `#EEF2EA` | Fundo geral do sistema e áreas de apoio. |
| `grey-text` | `#6F6F6F` | Texto corrente, rótulos e conteúdo secundário. |
| `feedback-success` | `#5F8233` | Texto e ícones de sucesso. |
| `feedback-success-bg` | `#EEF4E8` | Fundo de feedback positivo. |
| `feedback-success-border` | `#D6E4C9` | Borda de feedback positivo. |
| `feedback-danger` | `#B85342` | Erros, ações destrutivas e texto de alerta. |
| `feedback-danger-bg` | `#FBEAE8` | Fundo de erro ou confirmação destrutiva. |
| `feedback-danger-border` | `#F3C7C0` | Borda de erro ou confirmação destrutiva. |
| `feedback-warning` | `#B37424` | Avisos e confirmações de atenção. |
| `feedback-warning-bg` | `#FDF4E7` | Fundo de aviso. |
| `feedback-warning-border` | `#F7DFC0` | Borda de aviso. |
| `feedback-info` | `#467185` | Informação neutra e orientação. |
| `feedback-info-bg` | `#EBF3F6` | Fundo informativo. |
| `feedback-info-border` | `#C8DFE7` | Borda informativa. |

Use a cor semântica acompanhada de texto ou ícone compreensível; não comunique estado somente por cor. Opacidades compostas como `text-grey-text/60` aparecem no código, mas não são tokens independentes.

### 1.2 Tipografia, hierarquia e espaçamento

- A fonte padrão do tema é **League Spartan**, configurada como `--font-sans`. O fallback é `sans-serif`.
- Títulos de página costumam usar `text-2xl font-bold text-amma-green-dark`; cabeçalhos de seção usam `text-lg font-bold text-amma-green-dark`.
- Texto de apoio costuma usar `text-sm text-grey-text`, frequentemente com margem superior curta (`mt-1`). Metadados e rótulos auxiliares usam `text-xs`, muitas vezes com `uppercase` e `tracking-wider`.
- Rótulos de formulário são normalmente `text-sm font-semibold text-grey-text`. Valores de tabela e conteúdo corrente costumam usar `text-sm font-medium text-grey-text`.
- O espaçamento é feito com a escala utilitária Tailwind. Padrões frequentes: páginas com `px-6 md:px-10`, áreas de conteúdo com `pt-6 pb-6`, grupos com `gap-4`, `gap-5` ou `gap-6`, e cartões/formulários com `p-5` ou `p-8` em telas maiores.
- A interface administrativa se organiza em colunas flexíveis e grades que passam de uma coluna para `md:grid-cols-2` ou mais em telas largas. Use `min-w-0` em regiões flexíveis e `overflow-x-auto` para tabelas largas.
- O tema define formatos próprios: `rounded-btn` (`10px 20px`), `rounded-btn-sidebar` (`10px 30px`), `rounded-box` (`15px 60px`), `rounded-btn-l-small` (`5px 10px`) e `rounded-btn-r-small` (`10px 5px`). Prefira esses tokens a arredondamentos arbitrários em componentes novos, exceto quando o formato circular tiver função visual própria.

### 1.3 Botões e interações

**Ação primária:** fundo `bg-amma-green`, texto branco, `hover:bg-amma-green-dark`, peso `font-bold`, `rounded-btn`, padding próximo de `py-2.5 px-6`, e `efeito-zoom`. Exemplo consolidado em `ViveiroCadastroView.vue` e em detalhes de módulos administrativos.

**Ação secundária:** fundo branco, texto e borda verdes (`border-2 border-amma-green text-amma-green`), com hover para `bg-amma-green`/texto branco ou para `bg-amma-green-lighter`, conforme o destaque necessário. A ação de cabeçalho padrão do `AppDataTable` é desse tipo.

**Ação destrutiva:** use o grupo semântico de perigo. Os exemplos de linha de tabela usam ícone circular com `text-feedback-danger`, `hover:bg-feedback-danger-bg`, dimensões `w-8 h-8` e `rounded-full`. Botões destrutivos mais largos usam fundo `feedback-danger-bg`, texto `feedback-danger` e borda `feedback-danger-border`. A destruição deve pedir confirmação por `AppConfirmModal` com variante `danger`.

**Botões de ícone circulares:** são usados em ações compactas, especialmente em linhas de tabela e cabeçalhos. Devem ter dimensão e área de clique explícitas (`w-8 h-8`, ou maior em controles móveis), alinhamento central, `rounded-full`, rótulo acessível (`aria-label`) e estados de hover/foco visíveis.

**Animação:** a classe global `efeito-zoom`, definida em `main.css`, aplica `transition-all duration-300`, aumento no hover (`hover:scale-105`), sombra, redução ao pressionar (`active:scale-95`) e cursor de ponteiro. Use em ações clicáveis compatíveis com escala. Não a aplique indiscriminadamente a campos de formulário ou elementos que não sejam ações.

### 1.4 Formulários, validação e arquivos

O padrão visual mais frequente de campo é um contêiner vertical com rótulo, controle e texto auxiliar/erro. Inputs, selects e textareas usam largura total, fundo branco, borda verde-clara de 2 px, fundo verde muito claro no foco, borda verde no foco, `rounded-btn`, padding na escala de `py-3 px-4`, texto cinza e transição. Textareas normalmente definem `rows` e podem usar `resize-none` quando o layout exigir.

Exemplo da classe-base observada:

```text
w-full bg-white focus:bg-amma-green-lighter border-2 border-amma-green-light
focus:border-amma-green rounded-btn py-3 px-4 text-grey-text outline-none
transition-all text-sm font-medium
```

Rótulos indicam obrigatoriedade de forma textual. Vincule `label` e controle com `for`/`id`; exponha erros com `aria-invalid`, `aria-describedby` e texto de erro associado. A tarefa deve manter os valores digitados quando o envio falhar. Estados de submissão desabilitam o formulário ou o botão e mostram texto/indicador de carregamento.

Áreas de seleção de arquivo existentes usam `border-2 border-dashed border-amma-green-light`, hover de borda verde, fundo verde suave, ícone/texto instrutivo e preview dentro de um quadro com dimensões previsíveis (`overflow-hidden`). Para imagens locais, o frontend cria URLs com `URL.createObjectURL`; cada URL criada deve ser revogada quando substituída ou quando o componente for desmontado. Há telas legadas com ciclo de vida incompleto desse recurso; componentes novos devem cuidar explicitamente dessa limpeza.

O projeto tem formulários que compartilham a tela entre criação e edição. A decisão deriva do identificador/parâmetro da rota, por exemplo `docId` e `isEditing` em `BibliotecaFormView.vue`, `eventoId` em `EventosCadastroView.vue` e `route.name === 'inventario-editar'` em `ArvoreFormView.vue`. Quando os campos e regras forem os mesmos, prefira um formulário compartilhado: a view identifica o modo, carrega o recurso somente na edição, injeta os dados no componente e escolhe a ação create/update. Não associe a edição apenas à presença de um valor de formulário.

Uploads de documentos, banners e fotografias não são necessariamente parte do mesmo contrato JSON do recurso. A Biblioteca e Eventos usam `FormData` nas operações que recebem arquivos. No Inventário, o fluxo atual separa os endpoints de fotografias do cadastro/PUT da árvore. Novas especificações devem respeitar o contrato efetivo do módulo e não enviar/excluir arquivo incidentalmente no PUT geral.

### 1.5 Componentes globais compartilhados

#### `AppDataTable`

Arquivo: `frontend/src/components/ui/AppDataTable.vue`.

Componente genérico (`T extends object`) que exibe coleção em tabela ou grade, busca opcional, resumo, paginação e uma ação no cabeçalho. As propriedades principais são `columns`, `items`, `pagination`, `loading`, `selectedId`, `itemKey`, `caption`, `search`, `searchPlaceholder`, `buttonLabel`, `buttonIcon`, `emptyMessage`, `showSearch`, `showSummary`, `allowViewToggle` e `defaultView`.

Eventos: `update:search`, `action-click`, `prev-page` e `next-page`. Slots nomeados: `header-actions` substitui a ação padrão; `card` substitui o conteúdo de cada cartão; um slot por chave de coluna recebe `{ item, value }` para formatar células/cartões. Para ações por linha, declare uma coluna (por exemplo `actions`) e forneça o slot com essa chave. A tabela não define um evento genérico de clique de linha; a navegação deve estar no conteúdo do slot ou num controle explícito com rótulo acessível.

`pagination.page` é indexado a partir de zero, mas o resumo visual apresenta a página ao usuário a partir de um. A busca embutida só emite o novo texto; não executa chamada HTTP por conta própria. A integração com filtro, consulta e estado de carregamento pertence à view/store.

#### `AppBreadcrumb`

Arquivo: `frontend/src/components/ui/AppBreadcrumb.vue`.

Recebe `items: BreadcrumbItem[]` e `showBack` (padrão `true`). Cada item pode informar `label`, `routeName`, `params` e `icon`. Item com `routeName` navega pelo Vue Router; item sem rota representa o contexto atual. O botão “Voltar” executa `router.back()`. Em fluxo onde o histórico não é confiável, a tela deve optar por `showBack=false` ou fornecer uma ação de retorno explícita em vez de assumir um destino.

#### `AppConfirmModal`

Arquivo: `frontend/src/components/feedback/AppConfirmModal.vue`; estado e contrato em `feedbackStore.ts` e `types/feedback.ts`.

É montado globalmente por `App.vue` e consome a store. Abra com `feedback.showConfirm({ title, message, confirmText, cancelText, variant })`, que retorna `Promise<boolean>`. Variantes existentes: `primary` (padrão botânico), `warning` e `danger`. Clique fora e tecla Escape cancelam. Use para exclusões e mudanças relevantes de estado, com mensagem clara sobre a consequência; não use como substituto de validação inline.

#### `AppToastContainer`

Arquivo: `frontend/src/components/feedback/AppToastContainer.vue`; montado por `AuthenticatedLayout.vue`. A store cria toasts por `showToast(message, type, duration, title?)`; tipos: `success`, `error`, `warning` e `info`. Os toasts têm tempo de vida quando `duration > 0` e podem ser dispensados. `AuthenticatedLayout` mantém o recipiente no contexto administrativo. Para erros ligados a campos, complemente o toast com mensagens junto aos controles.

## 2. Arquitetura e padrões do Frontend (Vue 3)

### 2.1 Estrutura e responsabilidade

O projeto usa Vue 3 com Composition API, TypeScript e componentes Single File Components. O ponto de entrada (`frontend/src/main.ts`) importa o tema, cria o app, instala Pinia e Vue Router e monta a aplicação.

| Diretório/arquivo | Responsabilidade observada |
| --- | --- |
| `frontend/src/views/` | Páginas associadas a rotas. Orquestram layout de página, carregamento inicial, navegação e composição de componentes. |
| `frontend/src/components/` | Componentes reutilizáveis por domínio, layout, UI e feedback. Componentes de apresentação recebem dados por props e emitem eventos quando possível. |
| `frontend/src/stores/` | Estado compartilhado e ações de domínio com Pinia. Stores de domínio consultam services e organizam carregamento/erro/dados; store de feedback centraliza toasts e confirmação. |
| `frontend/src/services/` | Acesso HTTP e transformação de contratos de transporte. Usa a instância Axios comum de `services/api.ts`. |
| `frontend/src/types/` | Interfaces e tipos de payload/response/estado usados por views, stores e services. |
| `frontend/src/components/<domínio>/` | Componentes e composables específicos de uma área, como `inventario/`. |
| `frontend/src/router/index.ts` | Rotas nomeadas, layouts pai, carregamento lazy das views e metadados de título/menu. |
| `frontend/src/assets/main.css` | Tokens Tailwind do tema e regras globais, incluindo `efeito-zoom` e scrollbar. |

Use `<script setup lang="ts">` em componentes novos, nomes de rotas estáveis e carregamento lazy para páginas de domínio como no router atual. Mantenha separação clara: view não deve conter detalhes de Axios; service não deve gerir interface; componente visual não deve assumir catálogo/API sem uma razão explícita.

### 2.2 HTTP, stores e estados

`frontend/src/services/api.ts` exporta uma instância Axios com `baseURL` em `VITE_API_URL` e fallback local `http://localhost:8080`. Serviços por domínio usam essa instância. Os módulos antigos frequentemente retornam `response.data` diretamente; o Inventário implementa tratamento tipado e traduz falhas para erro de domínio que conserva status HTTP e `ProblemDetail`. Para fluxos novos, preserve status, código e causa da falha; não converta 401/403, falha de rede ou erro do servidor em coleção vazia ou sucesso aparente.

Store Pinia é apropriada quando os dados/ações atravessam componentes ou rotas e há necessidade de coordenar consultas. A store do Inventário é um exemplo mais completo: separa página da tabela, registros do mapa, ficha selecionada, filtros, opções e estados de carregamento/erro por operação. O estado `null` distingue “ainda não carregado/indisponível” de resultado vazio real. Stores legadas são mais simples e algumas apresentam falhas via toast genérico; não copie essa limitação quando a tela precisa distinguir estados.

Padrão de fluxo recomendado a partir do código mais recente:

1. A view recolhe rota e interação do usuário.
2. A view chama uma ação explícita da store; evitar chamadas automáticas se o recurso ou a autorização não estiverem prontos.
3. A store ativa carregamento, limpa o erro anterior e chama o service.
4. O service envia os parâmetros/payload com tipos e traduz erros sem ocultar status.
5. A store atualiza dados somente após resposta válida e sempre encerra carregamento em `finally`.
6. A UI diferencia carregando, sucesso, vazio, erro, não encontrado e acesso negado conforme o contrato.

Formate valores na camada de apresentação ou em utilitários de domínio sem alterar o DTO armazenado. Datas, unidades, enums e valores nulos devem ser exibidos explicitamente; nos dados do Inventário, por exemplo, ausências são apresentadas como “Não informado”. Para chamadas concorrentes por IDs de rota, invalide/limpe o dado antigo e ignore respostas cujo identificador não corresponda à rota atual.

### 2.3 Layout e navegação

`AuthenticatedLayout.vue` compõe `AppSidebar`, `AppHeader`, `AppToastContainer` e `RouterView` num painel administrativo; `PublicLayout.vue` atende páginas públicas. A área principal é rolável e a moldura usa `amma-bg`, superfície branca e `rounded-box`. Breadcrumb e cabeçalho da página mantêm contexto. A presença de uma rota filha no layout autenticado não comprova que exista autenticação funcional; consulte a configuração de segurança e o fluxo de identidade real antes de assumir autorização.

## 3. Arquitetura e padrões do Backend (Spring Boot)

### 3.1 Organização por camadas

O backend está em `backend/arborize/src/main/java/br/com/arborize/`:

- `controllers/`: adaptadores REST, parâmetros HTTP e códigos de resposta.
- `dtos/request/` e `dtos/response/`: contratos externos de entrada e saída.
- `services/`: regras de aplicação, transações e coordenação de repositórios/armazenamento.
- `repositories/`: Spring Data JPA, consultas e specifications.
- `entities/`: modelo relacional JPA.
- `enums/`: valores de domínio serializados no contrato.
- `mappers/`: conversão entre entidades e DTOs, sem persistência.
- `config/`: configurações Spring, CORS, segurança e clientes externos.
- `storage/`: integração de objetos com MinIO.
- `exceptions/` e `validation/`: falhas de domínio e validações declarativas/customizadas.

Controller deve permanecer fino: validar formato HTTP e delegar ao service. Service aplica regras, controla transação e carrega relações necessárias. Repository encapsula persistência. Mapper centraliza conversão e não deve alterar persistência por conta própria. Evite serializar entidades JPA diretamente: isso acopla API ao esquema e expõe relações/metadados inadvertidamente.

### 3.2 Rotas REST, verbos e versionamento

Os módulos mais recentes usam recursos no plural e `ResponseEntity`, com prefixo `/api`, como `/api/arvores` e `/api/materials`. No Inventário, `GET /api/arvores` lista de forma paginada, `GET /api/arvores/{id}` busca ficha, `POST /api/arvores` cria, `PUT /api/arvores/{id}` atualiza e sub-recursos de fotos ficam em `/api/arvores/{id}/fotos`. O catálogo de espécies usa `/especies`, sem `/api`, e os eventos usam `/eventos`; há, portanto, inconsistência histórica de prefixo.

Não foi identificado versionamento de URL como `/api/v1`. Não introduza uma versão como se já fosse padrão existente; qualquer adoção de versionamento deve ser decisão arquitetural transversal.

Convenções observadas/recomendadas para novos endpoints:

- `GET` consulta e não deve modificar estado.
- `POST` cria recurso ou executa ação de criação; retorno comum é `201 Created`, de preferência com `Location` quando houver URL do recurso.
- `PUT` substitui/atualiza a representação conforme contrato do módulo; responda `200 OK` com a representação atualizada quando o contrato prevê corpo.
- `DELETE` pode responder `204 No Content` quando concluído sem corpo.
- Use recursos no plural, IDs no caminho, filtros/paginação como parâmetros de consulta e nomes de campos que correspondam aos DTOs. Especifique limites e índice da paginação; a listagem do Inventário começa na página zero.
- Não confunda a convenção REST atual com uniformidade: controladores legados ainda divergem em prefixo, injeção, tratamento e formato de erro.

### 3.3 DTOs, validação e mapeamento

Os DTOs recentes do Inventário são `record`s Java com contratos tipados e campos em camelCase. Requests usam Jakarta Bean Validation (`@NotNull`, `@Positive`, `@DecimalMin`, `@DecimalMax`, entre outras) e são recebidos com `@Valid @RequestBody` em JSON. Regras entre campos usam validação customizada de classe, como `@TerritorioConsistente`. Responses também são records e podem conter DTOs resumidos de relações, mantendo a representação da API independente da entidade.

O padrão de maior consistência para novos contratos é:

1. Criar Request DTO apropriado para a operação (cadastro, atualização ou filtro quando necessário).
2. Marcar obrigatoriedade e limites no DTO; validações que dependem do banco ou de regra entre campos também devem ser verificadas no service/validator.
3. Validar no limite HTTP com `@Valid` e validar também no service quando a chamada interna possa contornar o controller.
4. Usar Response DTO explícito, sem retornar entidade JPA.
5. Mapear de forma centralizada, com relações carregadas dentro da transação.
6. Definir a semântica de campos opcionais e `null`, especialmente em PUT, de forma explícita no contrato.

O padrão não está aplicado em todos os módulos. Há DTOs legados com `MultipartFile` e campos numéricos/booleanos recebidos como `String`, e controllers que usam `@ModelAttribute` sem validação Bean Validation explícita. Evite reproduzir isso em novos endpoints JSON tipados sem necessidade de compatibilidade.

### 3.4 Upload, armazenamento e recuperação de arquivos

O projeto configura `MinioClient` em `config/MinioConfig.java`, com endpoint, credenciais e bucket fornecidos por propriedades externas. Credenciais não devem ser gravadas em arquivos rastreados. A camada de armazenamento do Inventário (`storage/FotoArvoreStorage.java`) encapsula operações MinIO e não publica URL nem chave interna do objeto.

Há dois estilos de upload existentes:

- Recursos antigos, como Materiais e Eventos, recebem `multipart/form-data` por `@ModelAttribute` com campos `MultipartFile` no DTO.
- Fotografias do Inventário usam rota dedicada, uma imagem por requisição, campo `file`, `@RequestParam("file") MultipartFile`, validação dedicada de conteúdo/tamanho e sub-recurso `/api/arvores/{id}/fotos`.

Para arquivos associados a entidades, mantenha o arquivo em armazenamento de objetos e os metadados/referência opaca no banco. Não exponha bucket, chave interna ou URL pública no DTO. Faça validação de tipo/tamanho no servidor, trate falha de armazenamento explicitamente e defina a política de consistência entre banco e MinIO.

**Recuperação/download existente:** os endpoints de PDF/capa da Biblioteca e de fotografia de árvore retornam `ResponseEntity<byte[]>`, com `Content-Type` e `Content-Disposition` (attachment para download e inline para exibição). As fotografias do Inventário também aplicam `Cache-Control: no-store` e `X-Content-Type-Options: nosniff`. Apesar dos nomes `getFileStream`/`getCoverStream` no service, o controller materializa e retorna `byte[]`; não há aqui um padrão geral de streaming HTTP via `StreamingResponseBody`. Para arquivos grandes, avaliar stream real/`Resource` separadamente em vez de chamar o padrão atual de streaming.

### 3.5 Status HTTP, erros e segurança

O Inventário tem contrato de erro mais estruturado: `ArvoreExceptionHandler` é um `@RestControllerAdvice` limitado aos controllers de árvores e fotografias e retorna `ProblemDetail` (`application/problem+json`) com `type`, `title`, `status`, `detail`, `code` e, em erros de entrada, lista `errors` com campo e mensagem. Exemplos: `400` para entrada inválida, `404` para recurso inexistente, `413` para arquivo acima do limite e `500` com detalhe sanitizado para falha inesperada. Não transforme conflitos de domínio em `409` sem regra/contrato que o exija.

Esse advice **não é global** para os outros módulos. `EventosController` e `EspecieController`, por exemplo, capturam exceções localmente e devolvem `400` com texto; outros fluxos não compartilham o formato ProblemDetail. Ao criar uma API nova, use contrato de erro estruturado consistente, documente códigos/status e evite expor mensagem interna ou stack trace ao cliente. Log técnico deve ficar no servidor com contexto suficiente e sem segredos.

Segurança também varia por rota e permanece pendente de consolidação: a configuração atual contém `denyAll()` explícito para `/api/arvores` e `/api/arvores/**`, antes de uma regra mais ampla permissiva para `/api/**`. Isso bloqueia temporariamente o Inventário; não remova o bloqueio nem interprete rotas no layout administrativo como autenticação. Qualquer API administrativa exige autenticação e autorização no backend antes de ser disponibilizada.

## 4. Guia curto para novas especificações e implementações

1. Defina ator, regra de negócio e estados da interface antes do código; documente campos, unidade, nulabilidade, limites, permissões e comportamento de erro.
2. Reutilize tokens, layout e componentes existentes; desenhe carregando, sucesso, vazio, erro, não encontrado e acesso negado.
3. Defina tipos/DTOs e semântica do contrato antes de ligar formulário, store e persistência.
4. Mantenha componentes visuais desacoplados da API; concentre HTTP no service e coordenação no store quando estado compartilhado justificar.
5. No backend, siga Controller → Service → Repository, com DTOs explícitos, validação, transação e mapeamento.
6. Para upload, estabeleça endpoint e ciclo de vida próprios, limites, metadados, falhas parciais e política de remoção; não embuta uma alteração de arquivo numa atualização JSON sem contrato claro.
7. Verifique a configuração real de segurança antes de chamar uma funcionalidade de pronta para produção.

## 5. Referências de código consultadas

- UI e tokens: `frontend/src/assets/main.css`, `frontend/src/components/ui/AppDataTable.vue`, `frontend/src/components/ui/AppBreadcrumb.vue`, `frontend/src/components/feedback/AppConfirmModal.vue`, `frontend/src/components/feedback/AppToastContainer.vue`.
- Navegação e layout: `frontend/src/router/index.ts`, `frontend/src/layouts/AuthenticatedLayout.vue`, `frontend/src/App.vue`, `frontend/src/main.ts`.
- Formulários e uploads: `frontend/src/views/auth/biblioteca/BibliotecaFormView.vue`, `frontend/src/views/auth/eventos/EventosCadastroView.vue`, `frontend/src/views/auth/inventario/ArvoreFormView.vue`, `frontend/src/components/inventario/ArvoreFormulario.vue`.
- HTTP e estado: `frontend/src/services/api.ts`, `frontend/src/services/inventarioService.ts`, `frontend/src/stores/inventarioStore.ts`, `frontend/src/stores/feedbackStore.ts`.
- REST, DTOs, regras e imagens: `backend/arborize/src/main/java/br/com/arborize/controllers/ArvoreController.java`, `FotoArvoreController.java`, `ArvoreExceptionHandler.java`, `EducationalMaterialController.java`, `backend/arborize/src/main/java/br/com/arborize/dtos/`, `services/`, `mappers/`, `storage/FotoArvoreStorage.java`.
- Segurança, CORS e MinIO: `backend/arborize/src/main/java/br/com/arborize/config/SecurityConfig.java`, `CorsGlobalConfig.java`, `CorsConfig.java`, `MinioConfig.java`.
