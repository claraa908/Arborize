# Auditoria de interface, usabilidade e fluxos — Inventário Arbóreo

**Data:** 1 de novembro de 2026  
**Escopo:** diagnóstico do frontend do Inventário Arbóreo, seus contratos e dependências de segurança no backend.  
**Natureza:** auditoria somente de leitura. Nenhum código, especificação ou checklist foi alterado.

## Resumo executivo

O módulo tem uma base funcional ampla: existem rotas administrativas, telas para consulta, cadastro, ficha e edição, store Pinia, serviços HTTP, mapa MapLibre e gestão de fotografias. A tipagem, compilação de produção e lint direcionado passaram. Testes de componentes e serviços usam respostas simuladas; eles não demonstram integração funcional com a API, o banco ou o MinIO.

Há um bloqueio crítico de segurança confirmado na configuração do backend: a API está liberada por `permitAll()`, inclusive as rotas do inventário, e não há autenticação/autorização efetiva nem guard de sessão no Vue Router. Isso diverge da regra RN11 da spec, que exige proteção também para consulta. Não se deve utilizar o inventário com dados reais enquanto essa condição persistir.

A verificação visual e interativa no navegador não pôde ser concluída: o ambiente recusou a abertura do servidor Vite no endereço local (`listen EPERM`). Assim, posicionamento, responsividade real, interação de mapa, chamadas do geocodificador e fluxos HTTP reais estão classificados como não verificados em runtime. Nenhuma operação de escrita foi executada, e não houve conexão a PostgreSQL ou MinIO.

## Escopo e método

Foram lidos `AGENTS.md`, `docs/padroes-de-projeto.md` e os três documentos de `specs/001-inventario-arboreo/`. Foram inspecionados os componentes, serviços, stores, tipos, rotas, testes frontend e configurações backend relacionados ao inventário. Foram executados type-check, build, lint direcionado e testes automatizados direcionados. A análise do navegador foi tentada com o servidor vinculado apenas a `127.0.0.1`; a tentativa falhou por restrição do ambiente.

As observações abaixo distinguem evidência estática no código, resultado automatizado e comportamento que não pôde ser verificado ao vivo. Os testes com mocks não são tratados como evidência de persistência, rollback, callbacks JPA ou integração com serviços externos.

## Fluxos e estado de verificação

| Fluxo | O que o código oferece | Verificação | Situação |
|---|---|---|---|
| Abrir a listagem do inventário | Rota administrativa, tela, consulta pela store e estados de carregamento/erro/vazio | Testes automatizados e inspeção estática; sem navegador/API real | Parcialmente verificado |
| Pesquisa, filtros e paginação | Store e serviços enviam pesquisa/filtros; resultados para mapa e tabela são mantidos separados | Testes mockados passam na maioria dos casos; sem API real | Parcialmente verificado |
| Mapa de ruas | MapLibre, tiles OpenStreetMap, marcadores provenientes das propriedades e seleção para abrir ficha | Configuração/código e testes; sem navegador | Não verificado visualmente |
| Mapa satélite | Estilo MapTiler condicionado a `VITE_MAPTILER_API_KEY`; alternativa de mapa normal disponível | Dependente de chave e configuração de origem; sem navegador | Condicional e não verificado |
| Seleção de ponto e busca de endereço | Clique reposiciona marcador; busca e reverse geocoding chamam MapTiler; os eventos atualizam formulário e bairro quando há correspondência no catálogo | Inspeção de código; sem navegador, rede ou resposta real do provedor | Não verificado em runtime |
| Cadastro | Formulário compartilhado, validação client-side e envio pelo serviço real | Testes de validação/camada; nenhuma gravação na API ou banco | Parcialmente verificado |
| Abrir ficha pela listagem | Consulta por ID, limpeza da ficha anterior, estados de erro e ação para editar | Testes isolados da ficha passam; sem API real/navegador | Parcialmente verificado |
| Edição | Carrega registro por ID e prepara payload completo do PUT, inclusive nulidade intencional dos campos opcionais | Testes têm uma asserção incompatível com o HTML atual; sem PUT real | Parcialmente verificado |
| Fotografias | Lista metadados, busca conteúdo pelo backend, valida tipo/tamanho/quantidade e exige confirmação para exclusão | Testes mockados; nenhuma operação no MinIO | Parcialmente verificado |
| Segurança administrativa | Backend atualmente aceita requisições sem autenticação; layout administrativo não é controle de acesso | Configuração e roteador inspecionados | Não conforme à RN11; risco crítico |
| Responsividade e acessibilidade | Há estrutura responsiva e alguns atributos de teclado/semântica nos componentes | Inspeção estática somente | Não validado em dispositivos/navegador |

## Achados

### S-01 — API do inventário sem autenticação ou autorização

**Prioridade: Crítica. Categoria: segurança e viabilidade dos fluxos.**

Em `backend/arborize/src/main/java/br/com/arborize/config/SecurityConfig.java`, as rotas `/api/arvores`, `/api/arvores/**` e `/api/bairros` usam `permitAll()`. A regra seguinte também permite `/api/**`, e a regra final permite qualquer outra requisição. Não há distinção efetiva entre consulta, cadastro, edição ou gestão de fotos. No frontend, as rotas são agrupadas sob `AuthenticatedLayout`, mas não há guard que comprove identidade ou permissão; esse layout é apenas apresentação visual.

Isso não satisfaz RN11, segundo a qual inclusive a consulta do inventário é administrativa. Um usuário pode contornar a interface e chamar a API diretamente. A política institucional de perfis ainda depende da AMMA; essa pendência não justifica liberar acesso público. Até a autenticação e a autorização estarem implementadas e verificadas, o módulo não deve ser considerado pronto para produção nem exposto a dados reais.

**Evidência:** `SecurityConfig.java`; configuração do Vue Router; `requirements.md` (RN11); `tasks.md` (T19 pendente). A auditoria não enviou requisições HTTP para tentar explorar as rotas.

### U-01 — Identidade e encerramento de sessão aparentam ser reais, mas são estáticos

**Prioridade: Alta. Categoria: confiança e segurança percebida.**

`frontend/src/components/layout/AppHeader.vue` apresenta o nome “Romênia Barros” e o cargo “Superintendente • AMMA” como identidade fixa. A ação de sair apenas pede confirmação, mostra mensagem de sessão encerrada e navega para a página inicial; não há sessão autenticada nem chamada de logout. Sem rotulagem inequívoca como demonstração, a interface comunica uma identidade e uma sessão que não foram verificadas.

**Impacto:** operadores podem supor que suas ações estão atribuídas a uma conta e a um perfil institucional. Isso também conflita com a orientação de não inventar usuários/permissões. O comportamento funcional foi confirmado por inspeção de código, sem execução no browser.

### U-02 — Possível sobrescrita de texto digitado durante reverse geocoding

**Prioridade: Média. Categoria: prevenção de perda de entrada.**

No fluxo de `frontend/src/components/inventario/ArvoreLocalizacaoMapa.vue` e `ArvoreFormulario.vue`, a escolha de coordenada inicia uma requisição assíncrona de reverse geocoding. Quando a resposta chega, o formulário atualiza os campos de referência/endereço. Como a edição manual continua possível durante a espera, o resultado tardio pode substituir texto que o operador digitou depois do clique.

**Reprodução prevista:** clicar no mapa, começar a digitar a referência antes de a resposta do provedor chegar e aguardar a resposta. A possibilidade é sustentada pela ordem de eventos no código; não foi reproduzida em navegador por indisponibilidade do ambiente. O componente cancela geocodificações reversas anteriores, o que reduz respostas antigas concorrentes, mas não evita a substituição da edição manual da resposta atual.

### U-03 — Feedback de falha pode aparecer duplicado

**Prioridade: Média. Categoria: feedback.**

`frontend/src/services/api.ts` envia erros HTTP ao feedback global, enquanto telas e componentes do inventário também exibem estados de erro locais. Em falhas de consulta, cadastro, edição ou fotos, há caminhos em que o mesmo evento pode produzir toast e mensagem inline simultaneamente. O tratamento global é útil para erros não tratados, mas a combinação pode repetir o texto e competir pela atenção.

**Evidência:** interceptor Axios e estados locais em `InventarioView.vue`, `ArvoreFormulario.vue`, `ArvoreDetalhesView.vue` e `ArvoreFotografias.vue`. A duplicação visual em execução não foi confirmada por navegador nem por resposta HTTP real.

### U-04 — Breadcrumbs navegáveis não têm semântica de controle

**Prioridade: Média. Categoria: acessibilidade.**

`frontend/src/components/layout/AppBreadcrumb.vue` usa elementos `<span>` com clique para itens que navegam. Esses elementos não são controles acionáveis semânticos e não oferecem a interação padrão por teclado como um `<a>` ou `<button>`. Isso afeta navegação por teclado e leitores de tela, contrariando a base de acessibilidade do projeto.

O retorno por `router.back()` também depende do histórico anterior. Ao abrir uma ficha ou formulário diretamente por URL, a ação “Voltar” pode levar o usuário para fora do fluxo esperado. Não foi possível conferir a apresentação visual do breadcrumb.

### U-05 — Especificação de fotos no formulário diverge do fluxo atual

**Prioridade: Média. Categoria: requisito/UX.**

O design descreve seleção/pré-visualização de até três imagens durante o cadastro da árvore (`design.md`, seção 5.4; e CA08 em `requirements.md` menciona anexar fotos no cadastro/edição). O fluxo implementado informa que as fotos podem ser adicionadas depois de cadastrar a árvore, na ficha/edição, pois o upload precisa de um ID persistido. O PUT geral não gerencia arquivos, conforme o desenho atual.

O fluxo implementado é coerente com endpoints de fotos separados, mas a documentação não deixa essa delimitação igualmente clara. É necessário decidir se “durante o cadastro” significa selecionar antes e enviar após criar, ou se o upload começa apenas depois da persistência; então alinhar critérios, fluxo e estados da interface. A auditoria não enviou fotos.

### U-06 — Texto técnico de configuração aparece para operadores

**Prioridade: Baixa a média. Categoria: linguagem da interface.**

O mapa exibe instruções sobre `VITE_MAPTILER_API_KEY`, arquivo `.env.local` e configuração de origens autorizadas quando o modo satélite não está disponível. A ficha e a área de fotos também mencionam códigos HTTP e a necessidade de disponibilizar autenticação/autorização. Esses textos descrevem configuração de desenvolvimento/operação do sistema, não ações que o técnico do inventário possa realizar.

**Evidência:** `InventarioMapa.vue`, `ArvoreDetalhesView.vue` e `ArvoreFotografias.vue`. O fato de o modo satélite depender de chave é uma dependência legítima; o ajuste recomendado é manter o motivo técnico em documentação/logs e apresentar ao operador uma mensagem acionável e não técnica.

### U-07 — Resultado do mapa pode combinar dois estados de falha

**Prioridade: Baixa. Categoria: clareza de estado.**

Quando a consulta do mapa falha, a tela principal apresenta erro e o componente do mapa recebe uma lista vazia, podendo também exibir o aviso de que os registros do mapa não estão disponíveis. Isso pode fazer uma falha de carregamento parecer também uma lista vazia. O código já distingue o erro geral da ausência de árvores, mas a apresentação simultânea merece validação visual e pode ser redundante.

**Evidência:** fluxo entre `InventarioView.vue`, `inventarioStore.ts` e `InventarioMapa.vue`. Não foi observado no navegador.

### P-01 — Peso dos artefatos do mapa

**Prioridade: Baixa. Categoria: desempenho/percepção de espera.**

O build reporta bundle de mapa (`inventarioMapaConfig`) de aproximadamente 1.055 kB antes de gzip e worker WebGL de aproximadamente 508 kB, além do aviso geral para chunks acima de 500 kB. O MapLibre pode elevar o tempo de carregamento inicial, especialmente em dispositivos móveis ou conexão limitada. O build é válido; este achado indica oportunidade de medição e carregamento sob demanda, não falha funcional comprovada.

### T-01 — Um teste de edição tem expectativa incompatível com o formulário

**Prioridade: Baixa. Categoria: confiabilidade da suíte.**

Na suíte `arvoreEdicaoView.spec.ts`, uma asserção espera que o ID da árvore apareça como `value="42"`. No formulário atual o identificador é exibido como texto e não é editável, enquanto o ID da espécie é outro campo. A execução isolada mantém essa falha. A expectativa do teste parece estar desatualizada ou apontando para o campo errado; ela deve ser revisada antes de ser usada como indicador de regressão funcional.

Outro teste de detalhes excedeu o timeout na execução agrupada e passou quando executado isoladamente. Isso sugere instabilidade/contenda de execução, não uma falha reproduzida da ficha.

## Estado das principais peças

### Backend observado

- Entidades e serviços do inventário e dos bairros existem no projeto em `backend/arborize/src/main/java/`.
- Endpoints de árvores cobrem consulta paginada, consulta para mapa, ficha, cadastro, atualização e fotos; o catálogo de bairros possui `GET /api/bairros`.
- O tratamento global usa `ProblemDetail` via advice global.
- A configuração de segurança está aberta por `permitAll()`; autenticação e autorização definitiva não estão implementadas.
- A auditoria não conectou ao PostgreSQL, não executou migrations/atualizações de esquema e não acessou o MinIO.

### Frontend observado

- Há telas e rotas de inventário, cadastro, ficha e edição no Vue Router.
- `inventarioStore.ts` organiza consulta, filtros, paginação e seleção; `inventarioService.ts` usa a instância Axios e os contratos TypeScript.
- `InventarioMapa.vue` recebe dados por propriedades e não consulta diretamente a API; MapLibre é usado para mapa, marcadores e seleção.
- `ArvoreFormulario.vue` compartilha fluxo de criação/edição; a rota determina o modo de edição.
- A ficha consulta árvore por ID e incorpora `ArvoreFotografias.vue` para operações de arquivo por endpoints específicos.
- As rotas administrativas não têm verificação de identidade/perfil no router.

## Divergências documentais e governança

1. **Caminho do backend em `AGENTS.md`:** o texto menciona `backend/src/`, mas o código real está em `backend/arborize/src/`. Corrigir a instrução para evitar inspeções e alterações no diretório errado.
2. **`docs/padroes-de-projeto.md`:** a seção de segurança ainda descreve `ArvoreExceptionHandler` limitado ao inventário e `denyAll()`. O código atual contém `GlobalExceptionHandler` e `permitAll()`. A documentação arquitetural está desatualizada e não representa o risco atual.
3. **Catálogo de bairros:** o design ainda marca o consumo de `GET /api/bairros` como pendente em trechos, embora o endpoint exista e as tarefas T24 e integração frontend estejam assinaladas como concluídas. Harmonizar as seções do design com a implementação e deixar explícito que a fonte oficial/classificação institucional dos bairros ainda depende da AMMA, se essa pendência permanecer.
4. **Fotografias:** design e CA08 sugerem anexar durante cadastro/edição; o fluxo depende de a árvore já estar criada. Definir e documentar o momento de seleção e upload.
5. **Segurança:** a spec mantém perfis em validação institucional e T19 pendente, enquanto a configuração permissiva atual permite acesso público. Registrar a implementação atual como estado temporário inseguro, sem transformar `permitAll()` em regra funcional aprovada.
6. **Tarefas de validação:** manter T19 e a validação de perfis institucionais pendentes. A auditoria confirma que não há evidência para marcá-las como concluídas.

## Verificações executadas

| Verificação | Resultado |
|---|---|
| TypeScript (`npm run type-check`) | Passou |
| Build frontend (`npm run build-only`) | Passou; com avisos de chunks grandes do mapa/WebGL |
| ESLint direcionado aos arquivos do inventário e componentes compartilhados | Passou |
| Testes automatizados direcionados do inventário | 121 passaram; execução reportou também falha de dependência `jsdom`, uma asserção incompatível em teste de edição e timeout transitório de ficha |
| Teste de edição isolado | A mesma asserção de `value="42"` falhou; outros casos da suíte passaram |
| Teste de ficha isolado | 5 testes passaram |
| Inicialização local do Vite para browser | Bloqueada pelo ambiente com `listen EPERM` ao vincular `127.0.0.1:5173` |
| Testes de integração com PostgreSQL/MinIO e operações de escrita | Não executados; não seguros/necessários para esta auditoria de interface |

## Recomendações priorizadas

1. **Bloquear a exposição do módulo a dados reais até T19:** implementar autenticação/autorização no backend e guard no frontend; separar permissões conceituais de consulta, cadastro, edição e fotografias, sem presumir papéis institucionais. Validar perfis com a AMMA. Manter testes de segurança que comprovem 401/403 e que requisições negadas não alcancem o service.
2. **Remover aparência de autenticação fictícia:** substituir identidade fixa e logout simulado por dados reais da sessão quando houver autenticação; até lá, deixar a natureza não autenticada inequívoca sem afirmar sessão ativa.
3. **Resolver a divergência de fotografias:** decidir o fluxo de seleção e upload após persistência e alinhar requisitos, CA08, design e formulário.
4. **Evitar perda de digitação durante geocodificação:** não sobrescrever alterações manuais que ocorrerem depois da solicitação, ou só preencher campos ainda inalterados; validar com teste de interação quando o ambiente de browser estiver disponível.
5. **Consolidar feedback de erro:** definir quais erros devem aparecer inline e quais devem disparar toast, evitando duplicidade sem ocultar 401/403, problemas de validação ou indisponibilidade.
6. **Corrigir semântica e navegação do breadcrumb:** usar elementos interativos acessíveis e fallback previsível quando a ficha/formulário for aberto diretamente.
7. **Revisar mensagens técnicas e estados combinados do mapa:** traduzir configuração ausente em feedback voltado ao operador e garantir que falha de carregamento não se confunda com inventário vazio.
8. **Alinhar documentação com a árvore real:** corrigir o caminho do backend em `AGENTS.md`, atualizar `docs/padroes-de-projeto.md` e reconciliar as seções divergentes do design.
9. **Ajustar a suíte:** corrigir a expectativa do ID no teste de edição e garantir a dependência de ambiente DOM disponível no setup existente; investigar o timeout quando a suíte for executada sob CI estável.
10. **Medir carregamento do mapa em dispositivos de operação:** acompanhar impacto do chunk MapLibre e decidir se carregamento sob demanda é necessário.

## Limitações desta auditoria

- Não houve inspeção visual real em desktop ou celular porque o ambiente não permitiu iniciar o servidor local.
- Não foi possível observar console, foco por teclado, contraste em tela, cliques no mapa, respostas do MapTiler ou rede HTTP em execução.
- As chamadas de API nos testes são mockadas; não comprovam contratos em execução, persistência nem integração com MinIO/PostgreSQL.
- Não foram feitas requisições de escrita ou tentativas HTTP contra as rotas abertas.
- O estado/configuração da chave MapTiler não foi exposto nem confirmado nesta auditoria; o modo satélite permanece dependente da variável de ambiente e da configuração de origem no provedor.

## Integridade do repositório

Na auditoria inicial, nenhum arquivo de código, `requirements.md`, `design.md` ou `tasks.md` foi alterado; o relatório foi o único artefato criado naquela etapa. As alterações locais preexistentes foram preservadas.

## Acompanhamento das correções de interface — 1 de novembro de 2026

Esta atualização preserva os achados, descrições e evidências originais acima e registra o estado após a correção pontual de textos e navegação.

| Achado original | Estado nesta atualização | Evidência/limite |
|---|---|---|
| S-01 — API sem autenticação/autorização | **Pendente; risco crítico permanece.** | Não foi alterada a configuração backend nem implementada autenticação. A interface não deve ser considerada segura para dados reais. |
| U-01 — identidade e encerramento de sessão aparentes | **Corrigido na apresentação.** | O cabeçalho não exibe mais nome/cargo fixos nem confirma encerramento de sessão. O controle agora leva à página inicial e tem rótulo acessível. Nenhuma sessão foi implementada. |
| U-02 — texto manual pode ser substituído por reverse geocoding | **Pendente.** | Fora do escopo desta execução; a interação não foi verificada no navegador. |
| U-03 — feedback global e inline duplicado | **Pendente.** | O fluxo de toasts e erros não foi alterado. |
| U-04 — breadcrumbs sem semântica e retorno dependente do histórico | **Corrigido.** | `AppBreadcrumb` usa navegação semântica, links do Vue Router, identifica a página atual e direciona “Voltar” para o ancestral navegável mais próximo da trilha. Funciona também quando a rota de ficha/edição é aberta diretamente. |
| U-05 — divergência sobre o momento de anexar fotografias | **Pendente.** | Requisitos e design não foram alterados; o fluxo funcional de fotografias não foi modificado. |
| U-06 — mensagens técnicas expostas ao operador | **Corrigido nos textos de interface alcançados por esta etapa.** | Mensagens do mapa deixaram de citar variável de ambiente, arquivo, Vite ou configuração de origem; mensagens de acesso negado na lista, formulário, ficha e fotos deixaram de exibir códigos HTTP ou instruções sobre autenticação. A mudança em `ArvoreFotografias.vue` foi somente textual, sem alterar operações, validações ou estados. A documentação de configuração técnica permanece fora da interface. |
| U-07 — estados de erro e vazio do mapa podem se sobrepor | **Pendente.** | Não foi alterado o fluxo de consulta/estado do mapa. |
| P-01 — tamanho do bundle MapLibre | **Pendente.** | O build segue emitindo avisos de chunks maiores que 500 kB; não houve otimização de carregamento nesta etapa. |
| T-01 — asserção de edição incompatível com o formulário | **Pendente, preexistente.** | O teste `arvoreEdicaoView.spec.ts` espera `value="42"` para um identificador que é apresentado como texto. Não foi alterado nesta etapa, pois não é falha de texto/breadcrumb/navegação causada pelas correções solicitadas. |

### Correspondência com os requisitos e o design

As correções de linguagem mantêm os estados de acesso indisponível e falha exigidos por CA10–CA12 e pelas seções de estados da interface do `design.md`; apenas removem informação técnica que não ajuda o operador. As mudanças no breadcrumb concretizam a navegação listada nas seções 6 e 8 do design e a orientação de acessibilidade de links/controles descrita na seção 9 e em `docs/padroes-de-projeto.md`. Nenhuma correção exige mudança de comportamento funcional especificado; `requirements.md` e `design.md` permaneceram intactos.

### Verificações desta atualização

- `npm run type-check`: passou.
- `npm run build-only`: passou, com avisos já observados de chunks grandes do mapa/WebGL.
- ESLint nos componentes e testes tocados: passou.
- Testes SSR pertinentes (`appBreadcrumb`, ficha, fotografias e estados da listagem), executados com ambiente Node para não depender do `jsdom` ausente: 4 arquivos e 22 testes passaram.
- A execução padrão do Vitest continua sem `jsdom` no ambiente; nenhuma dependência foi instalada.
- O teste `arvoreEdicaoView.spec.ts` não fez parte desta execução final e mantém a falha preexistente da expectativa `value="42"` documentada acima.
- Não foi possível inspecionar em navegador: a inicialização local do Vite em `127.0.0.1:5173` retornou `listen EPERM`. Portanto, não se declara validação visual, responsiva ou de foco por teclado concluída.
- Nenhuma chamada de escrita foi feita; backend, banco, segurança, requisitos, design e checklist não foram alterados.

Arquivos alterados nesta etapa: `frontend/src/components/ui/AppBreadcrumb.vue`, `frontend/src/components/layout/AppHeader.vue`, `frontend/src/components/inventario/InventarioMapa.vue`, `frontend/src/components/inventario/ArvoreFormulario.vue`, `frontend/src/components/inventario/ArvoreFotografias.vue`, `frontend/src/views/auth/InventarioView.vue`, `frontend/src/views/auth/inventario/ArvoreDetalhesView.vue`, `frontend/src/__tests__/appBreadcrumb.spec.ts`, `frontend/src/__tests__/arvoreDetalhesView.spec.ts`, `frontend/src/__tests__/inventarioInterface.spec.ts` e este relatório. Nenhuma alteração preexistente fora desses arquivos foi descartada.

## Revalidação de U-04 e U-06 — 1 de novembro de 2026

Na revalidação focada, confirmei que U-04 continua corrigido em `AppBreadcrumb.vue`: os ancestrais usam `RouterLink`, o item atual é exposto com `aria-current="page"`, há foco visível e o retorno aponta para a rota ancestral navegável mais próxima, sem depender do histórico. Atualizei a descrição correspondente em `docs/padroes-de-projeto.md` para refletir o comportamento atual.

U-06 continua corrigido nas telas do Inventário. A mensagem sem chave do mapa agora informa, em linguagem de operador, que a visualização por satélite precisa ser configurada e que o mapa de ruas permanece disponível, preservando o sentido de CA21. As mensagens da ficha e das fotografias não expõem mais status HTTP nem detalhes de autenticação. A orientação de configuração MapTiler permanece na documentação técnica `frontend/README.md` e na spec; nenhuma operação de fotografia, geocodificação ou tratamento global de erros foi alterada nesta revalidação.

Verificações desta revalidação: TypeScript passou; build passou com avisos de chunks grandes já documentados; ESLint direcionado passou; 42 testes em 5 arquivos passaram (`appBreadcrumb`, ficha, fotografias, estados da listagem e configuração do mapa). A inspeção por navegador continua indisponível: a tentativa local de iniciar o Vite em `127.0.0.1:5173` retorna `listen EPERM`; não se declara validação visual ou de teclado em browser.

`requirements.md`, `design.md` e `tasks.md` não foram modificados. Nenhum critério foi marcado como concluído nesta revalidação. Permanecem os demais achados da auditoria, sem avanço de escopo.

## Correções focadas de U-02 e U-03 — 1 de novembro de 2026

Esta atualização mantém os achados e evidências originais acima e registra somente as correções solicitadas para geocodificação e duplicidade de feedback.

| Achado | Estado após esta execução | Evidência e limite |
|---|---|---|
| U-02 — reverse geocoding pode sobrescrever digitação posterior | **Corrigido e reproduzido em teste unitário assíncrono.** | O formulário agora registra a revisão da referência, da situação territorial e do bairro no início da identificação. Respostas posteriores só atualizam cada campo se ele não foi alterado manualmente desde aquela seleção. O teste com resposta geográfica adiada confirmou que a referência digitada durante a espera permanece; outro teste confirmou que o preenchimento automático legítimo continua funcionando quando o campo não foi alterado. Não houve execução visual em navegador. |
| U-03 — toast global e erro local podem duplicar feedback | **Corrigido para consulta, catálogo de bairros, cadastro e edição; fotos preservadas sem alteração.** | Teste com adapter Axios local reproduziu uma resposta 403: requisição marcada para tratamento local propagou o status e não criou toast; requisição sem essa marca continuou criando toast global. O service do Inventário marca apenas suas operações não relacionadas a fotografias, cujas telas já exibem estado local. Os métodos e a interface de fotografias permaneceram intocados; portanto, eventual duplicação nesse fluxo ainda precisa de tratamento em tarefa que autorize mudanças em fotos. |

### Arquivos desta correção

- `frontend/src/components/inventario/ArvoreFormulario.vue`
- `frontend/src/components/inventario/ArvoreLocalizacaoMapa.vue`
- `frontend/src/components/inventario/arvoreCadastroForm.ts`
- `frontend/src/services/api.ts`
- `frontend/src/services/inventarioService.ts`
- `frontend/src/__tests__/arvoreGeocodificacao.spec.ts`
- `frontend/src/__tests__/apiFeedback.spec.ts`
- `frontend/src/__tests__/inventarioService.spec.ts`
- `frontend/src/__tests__/inventarioConsulta.spec.ts`
- Este relatório

### Verificações desta correção

- `npm run build`: passou, incluindo TypeScript e build Vite.
- ESLint direcionado aos componentes, services e testes modificados: passou.
- Testes direcionados sem DOM, usando ambiente Node: 92 testes em 8 arquivos passaram, incluindo respostas de reverse geocoding adiadas, erros HTTP 401/403/5xx, contrato de supressão de toast nas operações do Inventário e regressões de store, consultas, ficha, cadastro e fotos.
- Os testes de fotos foram somente executados para regressão; nenhum comportamento nem método desse fluxo foi alterado.
- A inspeção por navegador não foi executada nesta correção. A limitação ambiental previamente registrada para iniciar o Vite permanece; portanto, não se declara validação visual.
- `requirements.md`, `design.md` e `tasks.md`, backend, segurança, autenticação e operações de fotografias não foram alterados.

## Ajustes da tela de cadastro e seleção no mapa — 1 de novembro de 2026

Esta atualização registra somente a melhoria da tela `/sistema/inventario/nova` e da seleção de
coordenadas solicitadas nesta etapa. Os achados anteriores e suas evidências foram preservados.

| Área | Resultado | Evidência e limite |
|---|---|---|
| Clique e coordenadas | Fluxo reforçado e coberto por teste unitário; causa de execução real ainda não reproduzida. | A leitura do componente confirmou que o listener MapLibre já estava registrado e que o marcador e as coordenadas já eram atualizados antes do reverse geocoding; portanto, geocodificação não era uma dependência para preencher latitude/longitude. Foram identificadas duas fontes concretas de interferência: o pino permanecia como alvo de ponteiro e a busca usava um `<form>` aninhado ao formulário de cadastro. O tratamento de coordenadas foi centralizado e testado; o pino agora ignora eventos de ponteiro. Sem navegador, não foi possível provar que esses pontos causavam a falha percebida em todas as posições. |
| Formulário de busca | Corrigido o markup e o acionamento. | O campo de endereço estava em um `<form>` interno ao formulário de cadastro. Foi substituído por uma região `role="search"` e o botão de busca passou a ser explícito (`type="button"`); Enter continua buscando e não submete o cadastro. |
| Edição e troca de base | Regressões cobertas parcialmente em unidade. | O teste de seleção cobre a ordem síncrona das atualizações. A suíte preexistente de configuração do mapa cobre a preservação da câmera ao trocar estilo. O watcher de coordenadas da edição foi inspecionado e permanece responsável por sincronizar o marcador a partir das propriedades. A renderização real do MapLibre não pôde ser exercitada em navegador nesta sessão. |
| Usabilidade do cadastro | Ajustada sem alterar campos ou regras funcionais. | O mapa ocupa proporção maior no desktop, o painel de dados mantém rolagem própria, o formulário vem antes do mapa em telas pequenas, e os blocos de instrução, identificador e coordenadas foram compactados. Busca e bases cartográficas permanecem juntas ao mapa; ações e marcação de campo obrigatório seguem visíveis. |

### Documentação e arquivos

O `design.md` recebeu uma seção pontual que registra a composição refinada, sem alterar requisitos,
validações, payloads ou regras de negócio. Foram modificados `frontend/src/components/inventario/ArvoreLocalizacaoMapa.vue`,
`frontend/src/components/inventario/ArvoreFormulario.vue` e `frontend/src/views/auth/inventario/ArvoreFormView.vue`;
foram criados `frontend/src/components/inventario/arvoreLocalizacaoSelecao.ts` e o teste
`frontend/src/__tests__/arvoreLocalizacaoMapa.spec.ts`.

### Verificações e limitações

Os testes direcionados passaram: 36 testes em 4 arquivos, incluindo conversão sem inversão dos eixos,
ordem síncrona de marcador/coordenadas, configuração de estilos e proteção contra regressão de
geocodificação. TypeScript e build passaram; o build manteve avisos de chunks grandes do mapa. ESLint
nos arquivos tocados passou. A inspeção por navegador não está disponível no ambiente: Vite retorna
`listen EPERM` ao iniciar em `127.0.0.1:5173`, e não há uma aba de navegador ativa nesta sessão. Por
isso, não se declara confirmação visual do clique, responsividade ou renderização do MapLibre real.
Não houve alterações no backend, segurança, autenticação, fotografias ou outras áreas da auditoria,
e nenhum critério da spec foi marcado como concluído.

## Investigação reproduzida: clique e busca de endereço — 1 de novembro de 2026

Esta investigação complementa o registro anterior: os ajustes de ponteiro e do formulário de busca
não demonstraram a causa e não resolveram os sintomas relatados. Foi reproduzida agora uma falha
comum aos dois fluxos, usando os componentes reais e a implementação instalada de `Marker`.

### Causa demonstrada e correção mínima

Em `ArvoreLocalizacaoMapa.vue`, a criação era `new Marker(...).addTo(mapa)` e somente depois
executava `setLngLat`. No MapLibre instalado, `addTo` insere o elemento no canvas container,
registra listeners e chama `_update` imediatamente. `_update` projeta `_lngLat`, que ainda está
indefinido nessa ordem. A implementação está em `node_modules/maplibre-gl/src/ui/marker.ts`,
nos métodos `addTo`, `_update` e `setLngLat`.

O teste integrado reproduziu o erro de argumento `LngLatLike` dentro de `Marker.addTo`:

- No primeiro clique, a exceção ocorre antes de `update:latitude` e `update:longitude` chegarem
  ao formulário. Como a expressão de atribuição não termina, a referência do marcador permanece
  nula, apesar de seu elemento já ter sido inserido no DOM.
- Na busca por botão e por Enter, uma resposta simulada HTTP 200 com `features[0].center` válido
  faz `flyTo` funcionar; a criação do marcador lança a mesma exceção, a atualização de coordenadas
  não acontece e o `catch` apresenta a mensagem genérica de falha na pesquisa.
- O elemento inserido antes da projeção fica sem o posicionamento calculado pelo MapLibre. Isso
  explica tecnicamente um pino residual junto à origem do container e é compatível com o relato do
  canto superior esquerdo; não houve inspeção da captura ou de sua renderização real nesta sessão.

A correção consiste em criar o marcador com a coordenada antes de adicioná-lo:
`new Marker(...).setLngLat([longitude, latitude]).addTo(mapa)`.
As seleções seguintes continuam movendo a mesma instância. Coordenadas são emitidas imediatamente;
reverse geocoding não é requisito para selecionar o ponto. A proteção anterior contra respostas
geográficas atrasadas e a sincronização por propriedades foram preservadas.

### Evidências de integração

`frontend/src/__tests__/arvoreMapaIntegracao.spec.ts` monta `ArvoreFormulario.vue` e
`ArvoreLocalizacaoMapa.vue` juntos, com Pinia real, renderização cliente via renderer Vue em Node
e `Marker` real do MapLibre. Apenas o elemento HTML do marcador e o mapa/WebGL são adaptados ao
host de teste; serviços de catálogo e respostas geográficas são simulados exclusivamente nos testes.
Isso exercita listeners do componente, botão/Enter, requisição, parsing, marcador, emits, handlers
do formulário, propriedades reativas, outputs e descarte. Não equivale a renderização WebGL ou
consulta real ao provedor.

Antes da correção, os três casos de primeiro clique, busca por botão e busca por Enter falharam.
Depois da correção, esses casos passaram. A cobertura foi ampliada para edição, movimento/zoom,
troca de base, ausência de chave, reverse geocoding pendente, respostas 401/403/429/500 do geocodificador,
ausência de resultados, descarte dos recursos e diagnóstico seguro. O erro HTTP de geocodificação
não é confundido com autorização do backend administrativo.

### Estado da configuração e limites da conclusão

Foi verificada somente a presença de uma declaração não vazia de `VITE_MAPTILER_API_KEY` em
`frontend/.env.local`, sem exibir o valor. Isso não comprova que o servidor Vite em uso a carregou,
que a chave é válida, que a origem do navegador está autorizada ou que existe cota no provedor.
A busca atual codifica o texto e acrescenta Quixadá, Ceará e Brasil; interpreta o primeiro resultado
e valida seu centro antes de aplicá-lo. Não foram alterados o ranking nem os parâmetros geográficos
do provedor por hipótese. Resultados reais diferentes do esperado exigem a coleta descrita abaixo.

O Vite voltou a falhar com `listen EPERM` em `127.0.0.1:5173`. O inventário de ferramentas de
navegador não apresentou abas ativas. Portanto, a causa de código foi reproduzida e corrigida nos
testes integrados, mas os dois fluxos ainda precisam ser confirmados no navegador do operador,
com o mapa, a origem e o MapTiler reais.

### Diagnóstico temporário no navegador de desenvolvimento

O módulo `inventarioMapaDiagnostico.ts` funciona somente com `import.meta.env.DEV` e ativação
explícita na sessão. Não registra chave, URL, endereço, coordenadas, identificadores de árvores,
respostas completas ou mensagens de exceção. Registra nomes de etapas, booleanos, quantidade de
resultados, fase da falha e status HTTP numérico. Nenhum diagnóstico é exibido na interface.

1. Reiniciar o Vite se houve alteração de ambiente e abrir `/sistema/inventario/nova`.
2. No Console, ativar e recarregar:

   ```js
   sessionStorage.setItem('arborize:diagnostico-mapa', '1')
   location.reload()
   ```

3. Filtrar o Console por `[Arborize:mapa]`. No início, conferir `mapa.inicializado`,
   `chaveConfigurada` e `mapa.listeners` com `cliqueRegistrado: true`.
4. Clicar numa área livre do mapa. Esperar `dom.clique` com `alvoCanvas: true`, `mapa.clique`,
   `marcador.inicio`, `marcador.posicionado`, `formulario.latitude`, `formulario.longitude`,
   `coordenadas.emitidas` e `props.sincronizadas`. Conferir visualmente o pino e os dois outputs.
   Repetir depois de zoom, deslocamento, troca de base e na edição.
5. Pesquisar por botão e por Enter. Esperar `busca.acionada`, `busca.requisicao`,
   `busca.resposta`, `busca.resultado`, `busca.centralizada`, atualização de marcador/formulário
   e `busca.concluida`. Se falhar, coletar `status` e `fase`; zero resultados é registrado como
   `resultados: 0`. A interface mantém a mensagem operacional de erro/ausência de resultado.
6. Coletar somente as linhas desse diagnóstico e o comportamento observado. Não compartilhar URLs
   de requisições, arquivos HAR ou capturas que exponham a chave. Se `dom.clique` registrar
   `alvoCanvas: false` em área livre, investigar elemento sobreposto com o inspetor; cliques em
   controles do próprio mapa podem legitimamente ter esse valor.
7. Desativar ao encerrar:

   ```js
   sessionStorage.removeItem('arborize:diagnostico-mapa')
   location.reload()
   ```

Se a pesquisa falhar antes de `busca.resposta`, verificar conexão, bloqueio de rede/CORS e conteúdo
misto no navegador; se retornar 401/403, verificar a chave/origens no MapTiler; se retornar 429,
verificar cota. Esses são ramos de investigação, não falhas confirmadas nesta execução.

### Arquivos e verificações desta investigação

- Alterados: `ArvoreLocalizacaoMapa.vue` (ordem de inicialização e etapas de diagnóstico),
  `ArvoreFormulario.vue` (somente diagnóstico da recepção das coordenadas) e este relatório.
- Criados: `inventarioMapaDiagnostico.ts`, `arvoreMapaIntegracao.spec.ts`, `mapaTestHost.ts` e
  `frontend/vitest.mapa.config.ts`. A configuração compila os SFCs como cliente para executar
  seu ciclo de vida sem instalar dependência DOM.
- A execução integrada usa `npx vitest run --config vitest.mapa.config.ts`.
- Resultado final: 16 testes integrados e 36 testes de regressão existentes passaram; TypeScript,
  build Vite e ESLint direcionado passaram. A inspeção do bundle de produção não encontrou o
  prefixo nem a chave de ativação dos diagnósticos. Testes também comprovaram ausência de logs
  sem ativação e em produção, e ausência de chave/endereço/coordenadas nos logs de desenvolvimento.
- Nenhuma alteração no backend, segurança, autenticação, outras funcionalidades, requisitos,
  design ou checklist. Alterações locais preexistentes foram preservadas; nenhum commit foi feito.

## Execução da T28 — seleção de fotografias no cadastro (2026-11-01)

### Relato e diagnóstico

O usuário relata que a falha foi reproduzida no navegador: ao selecionar uma fotografia em
`/sistema/inventario/nova`, a área principal fica praticamente branca enquanto a sidebar
permanece visível. A causa técnica permanece **não identificada**; não há evidência para
atribuir o branco a exceção JavaScript, `File`/Blob URL, reatividade, overlay, CSS ou rolagem.

Na revisão do checkout atual foi confirmado um descompasso importante: o ramo de cadastro
em `ArvoreFormulario.vue` não tinha seletor de arquivos e apenas informava que fotos podiam
ser adicionadas depois. O seletor real existente estava em `ArvoreFotografias.vue`, exigia
um `arvoreId` e era montado na edição/ficha. Portanto, o fluxo relatado não pôde ser
reproduzido no código atual antes da implementação. Isso comprova a lacuna de cadastro, mas
não explica o sintoma observado no navegador do usuário.

O teste de regressão integrado monta o `ArvoreFormulario.vue` real, o mapa auxiliar real e
o novo componente de seleção. A seleção válida e inválida manteve a árvore do formulário e a
mesma instância de mapa montadas. Também foi injetada uma exceção de `URL.createObjectURL`:
o componente comunicou a falha e manteve o restante da tela funcional. Essa evidência cobre
o caminho implementado, mas não reproduz nem identifica a causa original da tela branca.

### Correção do fluxo conforme RN16 e CA25–CA26

- Foi criado `ArvoreFotografiasCadastro.vue` para seleção prévia no cadastro, pré-visualização,
  remoção/substituição, validação dos formatos/tamanho/quantidade e limpeza de URLs Blob.
- A lista de arquivos usa estado raso (`shallowRef`) para conservar objetos `File` nativos;
  falha ao criar uma URL de prévia é capturada e não interrompe a renderização.
- `ArvoreFormulario.vue` agora mantém a seleção junto dos dados e do mapa. Primeiro aguarda
  a criação da árvore; somente após receber o ID envia os arquivos ao endpoint existente.
- Em falha de upload, a árvore criada e seu ID são preservados, arquivos já confirmados são
  retirados da fila e somente os restantes podem ser reenviados. O formulário bloqueia uma
  segunda criação e mantém as informações preenchidas visíveis. Sem fotos selecionadas, o
  usuário continua podendo adicioná-las depois pela ficha técnica.

### Evidências e pendências

- O teste integrado real foi ampliado para seleção, prévia, remoção, substituição,
  preservação de campos/coordenadas/instância do mapa, formatos/tamanho/limite, erro de
  Blob URL, criação anterior ao upload e retentativa parcial sem duplicar a árvore.
- `npx vitest run --config vitest.mapa.config.ts`: 40 testes em 2 arquivos passaram.
- `npm run build`: type-check e build Vite passaram.
- ESLint, Oxlint e Prettier direcionados aos arquivos da T28 passaram.
- A inspeção no navegador não foi possível. A política da ferramenta bloqueou a observação
  da aba local; adicionalmente, o servidor Vite não iniciou por `listen EPERM` em
  `127.0.0.1:5174`. Não há validação visual manual nesta execução.
- A causa do branco e sua correção continuam pendentes de reprodução no navegador do usuário.
  A T28 permanece incompleta: manter pendentes os itens de reprodução/causa, correção da
  causa demonstrada e verificação manual; os itens de implementação e testes integrados
  comprovados foram marcados em `tasks.md`.

Arquivos desta execução: `frontend/src/components/inventario/ArvoreFormulario.vue`,
`frontend/src/components/inventario/ArvoreFotografiasCadastro.vue`,
`frontend/src/__tests__/arvoreMapaIntegracao.spec.ts`, `specs/001-inventario-arboreo/tasks.md`
e este relatório. Backend, segurança e autenticação não foram alterados.

## Complemento T28 — rolagem após selecionar fotografias (2026-11-01)

### Achado estrutural e correção aplicada

A revisão dos ancestrais confirmou que `AuthenticatedLayout.vue` usa um `main` com
`overflow-y-auto` dentro de contêineres limitados à altura da viewport. Em telas abaixo de
`xl`, o formulário usa uma grade de uma coluna: o mapa tem altura definida, enquanto o painel
de dados é um flex column sem altura limitada. O filho que contém todos os campos e as prévias
estava configurado como `min-h-0 flex-1`, embora nesse breakpoint não tivesse rolagem própria.
Assim, o conteúdo podia exceder a contribuição de altura do painel/linha da grade e não
estender corretamente o contêiner que permite rolar a página. A inserção da prévia aumenta a
altura desse conteúdo, tornando a inconsistência relevante ao selecionar arquivos.

A correção mínima em `ArvoreFormulario.vue` troca o comportamento base para `flex-none`, de
modo que o conteúdo determine a altura natural do painel em telas menores. O desktop conserva
o comportamento anterior por meio de `xl:flex-1 xl:overflow-y-auto`, já que nesse breakpoint o
formulário tem altura delimitada e rolagem interna intencional. Nenhum código ou classe de
`ArvoreLocalizacaoMapa.vue` foi modificado; dimensões do mapa, controles, localização e estado
da instância permanecem sob o comportamento já aprovado.

O teste integrado de `ArvoreFormulario.vue` real agora confere as classes responsivas do
painel após selecionar uma imagem válida e verifica prévia, campos e coordenadas preservados
e identidade da instância do mapa. A inspeção do código identifica a falha estrutural e a
regressão automatizada confirma o contrato CSS e a preservação do estado, mas não houve
medição de `scrollTop`, `scrollHeight`, `clientHeight` ou retângulos calculados no navegador.
Portanto, a correção ainda requer validação visual no ambiente do usuário antes de declarar
encerrado o defeito observado.

### Verificações e pendência de navegador

- `npx vitest run --config vitest.mapa.config.ts`: 40 testes em 2 arquivos passaram.
- `npm run type-check` e `npm run build`: passaram.
- ESLint, Oxlint e Prettier direcionados aos arquivos modificados: passaram.
- A inspeção em navegador continua indisponível nesta sessão: a ferramenta bloqueou a aba
  local e a inicialização anterior do Vite falhou com `listen EPERM` em `127.0.0.1:5174`.
  Não se declara validação visual concluída.

Procedimento manual: abrir `/sistema/inventario/nova` em desktop e celular; preencher campos,
marcar uma localização, rolar até Fotografias e selecionar uma imagem válida, uma inválida,
substituir uma prévia e removê-la. Confirmar que campos, mapa, marcador e coordenadas não
mudam de posição indevidamente, que o formulário continua alcançável pela rolagem e que a
mensagem de validação permanece visível. No DevTools, comparar `scrollTop`, `scrollHeight` e
`clientHeight` do `main` e os `getBoundingClientRect()` do formulário, mapa e seção de fotos
antes e depois da seleção.

## T28 — investigação da abertura do seletor e foco nativo (2026-11-01)

### Retificação do diagnóstico anterior

O usuário confirmou que o ajuste anterior não resolveu o defeito. O deslocamento ocorre
imediatamente ao clicar em **Selecionar imagens**, antes de escolher qualquer arquivo; a
sidebar permanece normal e o painel fica inacessível por rolagem. A atribuição anterior à
altura intrínseca do flex/grid **não foi demonstrada como causa**. O teste de classes CSS
não media geometria, rolagem nem o comportamento de foco nativo. Os itens de investigação e
correção da causa foram reabertos na T28. O registro anterior foi preservado como histórico,
sem representar comprovação de solução.

### Evidências de código e hipótese pendente

- `ArvoreFotografiasCadastro.vue` contém um `input type="file"` com `class="sr-only"`,
  dentro do próprio `label` associado pelo atributo `for`. O input não usa `hidden` nem
  `display: none`; ocultação visual não implica remoção da navegação/foco nativos.
- O componente não tem manipuladores de abertura, `.click()`, `focus` ou `focusin`. A
  ativação do seletor é a ação padrão do `label` no navegador. A posição renderizada do
  input e o possível deslocamento ao receber foco precisam ser medidos, sem inferi-los
  apenas pelo markup.
- `selecionarArquivos` só é chamado em `change`; processamento das imagens, geração de
  URLs Blob e inserção de prévias não executam ao simples clique de abertura. Portanto,
  não são necessários para o gatilho antecipado descrito pelo usuário. Isso não exclui
  falhas independentes nesses caminhos.
- Não existe tratamento de `cancel` no componente atual. No formulário, foco/rolagem
  explícitos estão ligados à validação ao salvar; o composable de navegação rola o `main`
  na montagem e em mudanças de rota. Nenhum desses caminhos é chamado pelo seletor no
  código inspecionado.
- `AuthenticatedLayout.vue` contém ancestrais com `overflow-hidden`, um ancestral
  posicionado com `relative` e um `main` com `overflow-y-auto`. O painel do formulário
  também tem rolagem interna em `xl`. Esses elementos devem ser medidos junto do input
  para distinguir foco fora da área visível, rolagem de um ancestral oculto e deslocamento
  real de geometria. Nenhum novo ajuste estrutural foi aplicado.

A hipótese de foco/rolagem provocado pela ativação do input oculto permanece **não
confirmada**. A ferramenta de navegador só apresentou a aba local com o título
“Não é possível aceder a este site”, sem página utilizável para coletar as medidas. As
restrições anteriores de inspeção local continuam registradas acima; não houve reprodução
independente nem alteração do mecanismo de abertura nesta execução.

### Diagnóstico para o Console do navegador do usuário

Arquivo pronto para copiar:
`docs/auditorias/diagnosticos/t28-seletor-fotografias.js`.

O script apenas lê geometria/estilos e registra eventos com listeners passivos. Não chama
`focus`, `.click()`, `scrollTo`, `preventDefault`, não altera DOM/CSS nem acessa APIs, storage,
valores dos campos, nomes/conteúdo das fotos, coordenadas ou chaves. Os registros ficam
somente no Console e em `window.arborizeT28.dados`. Há limite de 80 registros e desligamento
automático após dois minutos; reinstalar o script inicia uma coleta nova.

1. Abrir `/sistema/inventario/nova` e o Console do DevTools. Manter o mesmo tamanho de janela
   e a mesma posição do DevTools durante a coleta. Rolar até Fotografias, sem enviar o formulário.
2. Copiar todo o conteúdo do arquivo de diagnóstico e executá-lo no Console. Esperar a
   mensagem `[Arborize T28] Pronto`.
3. Clicar uma vez em **Selecionar imagens** e **cancelar** a janela nativa. Tentar rolar
   sobre o painel de dados. Esse primeiro teste não deve selecionar nenhum arquivo.
4. Voltar ao Console, executar `arborizeT28.capturar('depois-de-cancelar')` e copiar os
   resultados com `copy(JSON.stringify(arborizeT28.dados, null, 2))` nos navegadores que
   oferecem `copy` no DevTools. Alternativamente, avaliar `JSON.stringify(arborizeT28.dados,
   null, 2)` e copiar a string retornada. Encerrar com `arborizeT28.parar()`.
5. Recarregar a página e repetir a instalação do script em uma coleta separada, dessa vez
   selecionando **uma** fotografia válida. Capturar com o rótulo `depois-de-selecionar`,
   exportar os dados e encerrar. Não confirmar o cadastro nem fazer upload nesta investigação.
6. Enviar as duas coletas, informando em qual delas houve deslocamento/travamento e o
   navegador/sistema operacional utilizados. Nenhuma URL de requisição ou arquivo HAR é necessário.

O registro inicial inclui todos os elementos medidos. Os seguintes incluem somente aqueles
cujas medidas ou estilos mudaram, além da etapa, alvo, elemento com foco, número de arquivos,
viewport e quantidade de inputs com o mesmo ID. As etapas distinguem `pointerdown`, cliques
no label/input, `focusin`/`focusout`, `change`, `cancel`, retorno de foco, tentativa de rolagem
(`wheel`) e rolagem efetiva (`scroll`). As leituras imediatas, no próximo quadro e após
250 ms permitem localizar quando o foco e o deslocamento mudam. `scrollTop`, `scrollHeight`,
`clientHeight`, retângulos, `position`, `clip` e `offsetParent` são incluídos nos elementos
alterados.

### Verificações e estado

- Validação de sintaxe do diagnóstico com `node --check`: passou.
- Quatro testes integrados existentes de seleção/prévia, limites, envio após criação e
  falha parcial passaram; 27 casos não relacionados foram ignorados pelo filtro da execução.
  O renderer Node não implementa foco nativo, layout CSS ou a ação padrão do `label`;
  esses testes não comprovam nem descartam a hipótese atual.
- Nenhum componente, estilo, código de mapa, backend, autenticação ou segurança foi alterado.
  Não houve nova correção funcional, commit ou declaração de solução.
- T28 continua pendente de evidência do navegador. A implementação fica interrompida até
  obter as coletas; somente então decidir entre corrigir a abertura/foco do seletor ou
  investigar o ancestral que efetivamente mudou.
