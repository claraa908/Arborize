# Tarefas: Inventário Arbóreo Georreferenciado

**Spec:** `001-inventario-arboreo`  
**Requisito principal:** RF021  
**Sistema:** Arborize | AMMA de Quixadá  
**Auditoria de progresso:** 2026-11-01

## 1. Objetivo e convenções

Este checklist acompanha implementação e verificação do Inventário Arbóreo, cruzando os critérios de `requirements.md` e os contratos de `design.md` com o código existente.

- `[x]` indica atividade com código/evidência local correspondente.
- `[ ]` indica atividade ainda pendente, bloqueada por decisão, sem evidência suficiente ou dependente de validação integrada.
- Uma atividade implementada com mocks em testes não comprova disponibilidade operacional da API.
- As rotas do Inventário estão liberadas com `permitAll()` para testes locais; não há autenticação/autorização e o módulo não está liberado para uso administrativo em produção.
- As fontes principais desta auditoria foram os diretórios `frontend/src/` e `backend/arborize/src/main/java/`, testes sob `backend/arborize/src/test/java/`, especificações e artefatos de teste já existentes. O projeto real tem o backend em `backend/arborize/`, embora uma instrução antiga do `AGENTS.md` ainda indique `backend/src/`.

Antes de iniciar uma tarefa, ler integralmente `requirements.md`, `design.md` e este arquivo, além de `docs/padroes-de-projeto.md`; preservar alterações locais; executar somente a tarefa autorizada; não inventar dados institucionais nem marcar tarefas apenas porque o código foi iniciado.

## 2. Fase 0 — Levantamento e decisões

### T01: Confirmar os padrões existentes — concluída

- [x] Revisar as estruturas reais de frontend e backend.
- [x] Identificar componentes compartilhados, rotas, services, stores e padrões JPA reutilizáveis.
- [x] Confirmar o contrato do catálogo de espécies usado pelo Inventário.
- [x] Examinar a configuração e integração MinIO existentes.
- [x] Conferir MapLibre e fonte cartográfica disponível.
- [x] Registrar limitações que afetam o design e a integração.

**Evidência:** componentes, services e stores em `frontend/src/`; camadas em `backend/arborize/src/main/java/br/com/arborize/`; diagnóstico registrado nas especificações e no relatório de padrões.

### T02: Registrar dependências institucionais e de segurança — parcialmente concluída

- [x] Registrar a classificação fitossanitária como provisória até validação da AMMA.
- [x] Registrar a necessidade de fonte oficial e abrangência territorial dos bairros.
- [x] Registrar que os procedimentos de medição/estimativa precisam de confirmação.
- [ ] Confirmar com a AMMA quais perfis podem consultar, cadastrar, editar e gerenciar fotografias.
- [x] Identificar a falta de autenticação/autorização funcional no backend.
- [x] Identificar os critérios de aceite bloqueados pela falta de segurança e decisões institucionais.

**Bloqueios:** validação da AMMA e decisão institucional de perfis. A liberação temporária para testes locais não substitui autenticação ou autorização e deve ser revista antes da produção.

## 3. Fase 1 — Modelo e API do Inventário

### T03: Criar o domínio de indivíduos arbóreos — concluída

- [x] Criar entidade JPA `Arvore` com ID gerado.
- [x] Associar obrigatoriamente a espécie existente.
- [x] Armazenar latitude e longitude obrigatórias em tipo decimal.
- [x] Modelar data de plantio, DAP, altura e observações opcionais.
- [x] Definir condição inicial `NAO_AVALIADA` e metadados de criação/atualização.
- [x] Evitar cascata de exclusão de árvores por operações sobre espécies.
- [x] Verificar os invariantes do modelo em testes locais.

**Evidência:** `entities/Arvore.java`, `enums/CondicaoFitossanitaria.java`, testes de entidade e persistência.

### T04: Preparar a representação territorial — concluída estruturalmente

- [x] Criar entidade persistente `Bairro` sem inserir bairros fictícios.
- [x] Criar `ClassificacaoTerritorial` com os três estados requeridos, persistida como enum estável.
- [x] Permitir bairro opcional e evitar cascata de exclusão.
- [x] Validar coerência: `BAIRRO_IDENTIFICADO` exige referência; demais situações não recebem bairro.
- [x] Preparar associação e filtros por ID de bairro.
- [x] Registrar a fonte oficial de bairros como pendência da AMMA.

**Histórico:** entidade, vínculo opcional, estado territorial e filtro por ID foram implementados conforme a decisão anterior. A nova decisão mantém esse trabalho e programa a transição para texto opcional nas tarefas T29–T32; nenhum checkbox histórico é desmarcado.

### T05: Criar DTOs, mappers e validações — concluída

- [x] Criar DTOs de cadastro, atualização, ficha, resumo e paginação.
- [x] Criar mapeamentos sem expor entidades JPA na resposta HTTP.
- [x] Validar espécie, coordenadas, valores técnicos e enums.
- [x] Validar coerência da classificação territorial e do bairro.
- [x] Permitir nulos em campos técnicos opcionais e explicitar semântica do PUT.
- [x] Cobrir contratos e validações com testes.

**Evidência:** `dtos/request/`, `dtos/response/`, `mappers/ArvoreMapper.java`, `validation/` e testes respectivos.

### T06: Implementar cadastro e consulta individual — concluída em código

- [x] Criar `ArvoreRepository`.
- [x] Criar `ArvoreService` e regras de consulta/cadastro.
- [x] Criar `ArvoreController` com `POST /api/arvores` e `GET /api/arvores/{id}`.
- [x] Verificar espécie e gerar/devolver identificador.
- [x] Retornar `404` para recurso inexistente.
- [x] Preservar árvores classificadas como mortas.
- [x] Cobrir controller, serviço e persistência em testes disponíveis.

**Limitação operacional:** as rotas estão liberadas apenas para testes locais; não há autenticação/autorização funcional para uso administrativo.

### T07: Implementar listagem, pesquisa e filtros — concluída em código

- [x] Criar `GET /api/arvores` paginado e `GET /api/arvores/mapa`.
- [x] Pesquisar parcialmente por ID e nomes popular/científico.
- [x] Filtrar por bairro, espécie e condição fitossanitária.
- [x] Combinar pesquisa e filtros por interseção.
- [x] Compartilhar critérios entre tabela e mapa.
- [x] Validar página/tamanho e ordenar de forma determinística.
- [x] Cobrir specifications, serviços, controllers e consultas com testes.

**Limitação operacional:** os contratos e consultas existem e estão acessíveis no ambiente local de teste; a ausência de autenticação/autorização impede o uso administrativo em produção.

### T08: Implementar edição de árvores — concluída em código

- [x] Implementar `PUT /api/arvores/{id}` com representação completa.
- [x] Preservar ID e metadados não editáveis.
- [x] Permitir correção das coordenadas e demais campos editáveis.
- [x] Limpar campos opcionais por `null` explícito.
- [x] Validar existência da espécie e do bairro quando aplicável.
- [x] Não remover fotografias pelo PUT geral.
- [x] Tratar validação e recurso inexistente.
- [x] Cobrir atualização em testes de controller, serviço e integração disponíveis.

**Limitação operacional histórica:** a rota está liberada somente para testes locais. Até T31 ser implementada, a versão atual do frontend ainda seleciona bairros do catálogo; após a transição, essa dependência será removida sem apagar a relação legada.

### T09: Padronizar respostas HTTP e erros — concluída para o Inventário

- [x] Retornar `201 Created` para cadastro, `200 OK` para consultas/PUT e códigos apropriados para falhas.
- [x] Retornar `ProblemDetail` com `type`, `title`, `status`, `detail`, `code` e erros de campo quando aplicável.
- [x] Cobrir entrada inválida, enums/JSON inválidos, IDs inválidos e recursos inexistentes.
- [x] Sanitizar falhas inesperadas sem expor detalhes internos.
- [x] Cobrir limites multipart e respostas `413` no fluxo de fotos.
- [x] Verificar com filtros reais a configuração temporária de acesso local e documentar que ela não fornece autorização.

**Evidência:** `controllers/GlobalExceptionHandler.java`, `FotoArvoreMultipartExceptionResolver.java` e testes de erro/segurança.

**Estado:** `ProblemDetail` agora é tratado globalmente. A migração de controllers legados que capturam exceções localmente permanece em T23.

## 4. Fase 2 — Estrutura e consulta no frontend

### T10: Preparar rotas e views administrativas — concluída

- [x] Criar/manter `/sistema/inventario` no layout administrativo.
- [x] Criar rotas de cadastro, ficha e edição.
- [x] Usar lazy loading, títulos e metadados de menu existentes.
- [x] Reutilizar breadcrumbs e componentes compartilhados.
- [x] Apresentar estados estruturais sem criar registros institucionais fictícios.

**Evidência:** `router/index.ts`, `InventarioView.vue`, `ArvoreDetalhesView.vue`, `ArvoreFormView.vue`, `AuthenticatedLayout.vue`.

### T11: Criar tipos, service e store — concluída

- [x] Definir tipos compatíveis com os DTOs Java.
- [x] Reutilizar instância Axios comum.
- [x] Implementar métodos de consulta, mapa, cadastro e PUT.
- [x] Preservar status HTTP e interpretar `ProblemDetail`.
- [x] Manter separados resultados paginados, mapa e ficha.
- [x] Organizar filtros, carregamentos, erros e operações Pinia.
- [x] Não produzir resultados de sucesso simulados na aplicação.

**Evidência:** `types/inventario.ts`, `services/inventarioService.ts`, `stores/inventarioStore.ts` e testes unitários correspondentes. Os métodos HTTP de fotografias foram adicionados na integração T18.

### T12: Implementar mapa interativo — parcialmente concluída

- [x] Integrar MapLibre GL e controles de navegação.
- [x] Configurar base OpenStreetMap com atribuição visível.
- [x] Configurar opcionalmente MapTiler Satellite por `VITE_MAPTILER_API_KEY`, sem incluir chave no repositório.
- [x] Preservar câmera, marcadores e seleção ao alternar estilo.
- [x] Exibir marcadores com estado textual/simbólico e seleção navegável.
- [x] Atualizar marcadores a partir de props e limpar recursos ao desmontar.
- [x] Exibir estado vazio e falha de mapa sem interromper a página.
- [ ] Verificar MapTiler Satellite com chave real, domínio autorizado e atribuição do provedor.

**Evidência:** `InventarioMapa.vue`, `inventarioMapaConfig.ts`, `inventarioMapa.spec.ts`, documentação de configuração no frontend. Há verificação local da base OSM; não há evidência de tiles MapTiler reais.

### T13: Implementar pesquisa, filtros e listagem — concluída em código; aceite live pendente

- [x] Pesquisar por identificador e nomes da espécie.
- [x] Filtrar por bairro, espécie e condição; combinar critérios.
- [x] Usar `AppDataTable` e paginação da API.
- [x] Navegar para a ficha e localizar árvore no mapa.
- [x] Sincronizar seleção/destaque da tabela com o mapa.
- [x] Diferenciar carregamento, vazio, erro e acesso negado.
- [x] Não tratar 401/403 como lista vazia.
- [ ] Verificar consulta real autorizada com backend integrado após T19.

**Evidência:** `InventarioView.vue`, `useInventarioConsulta.ts`, `inventarioStore.ts`, testes `inventarioConsulta.spec.ts` e `inventarioInterface.spec.ts`. O catálogo de bairros é consultado pelo frontend; a fonte oficial continua sem validação da AMMA.

## 5. Fase 3 — Cadastro, ficha e edição

### T14: Implementar cadastro de árvore — implementação frontend concluída; validação institucional pendente

- [x] Implementar formulário compartilhável no padrão administrativo.
- [x] Carregar opções reais de espécie pelo catálogo existente.
- [x] Implementar campos de localização e mapa auxiliar com sincronização manual.
- [x] Implementar situação territorial e validações de coordenadas/campos técnicos.
- [x] Não criar bairros nem espécies fictícios.
- [x] Exibir carregamento, validação, falha e preservar formulário após erro.
- [x] Submeter pelo service real e navegar após resposta de criação válida.
- [x] Consumir o catálogo `GET /api/bairros` no select via frontend.
- [ ] Validar com a AMMA a fonte oficial e a abrangência do catálogo.
- [ ] Verificar cadastro end-to-end após segurança T19.

**Evidência:** `ArvoreFormulario.vue`, `arvoreCadastroForm.ts`, `ArvoreLocalizacaoMapa.vue`, testes de formulário. A UI usa a API real; a configuração atual é permissiva apenas para testes locais e não simula sucesso.

### T15: Implementar ficha técnica — implementação concluída; aceite live pendente

- [x] Buscar pelo ID da rota e descartar dados antigos ao mudar de identificador.
- [x] Exibir identificação botânica, localização, coordenadas, condição e dados técnicos.
- [x] Apresentar valores nulos como “Não informado”.
- [x] Exibir mapa da localização sem polígonos presumidos.
- [x] Implementar estados de carregamento, erro, 404, identificador inválido e acesso negado.
- [x] Expor navegação para edição sem simular autorização.
- [x] Exibir galeria real integrada aos endpoints de fotografias (T18).
- [ ] Verificar ficha com resposta autorizada do backend em fluxo integrado após T19.

**Evidência:** `ArvoreDetalhesView.vue`, `useArvoreFicha.ts`, `ArvoreFotografias.vue` e testes de ficha/fotografias.

### T16: Implementar edição de árvore — implementação concluída; aceite live pendente

- [x] Compartilhar `ArvoreFormulario.vue` entre cadastro e edição, identificando modo pela rota.
- [x] Carregar dados atuais pelo ID e evitar que resposta atrasada altere outra rota.
- [x] Preservar ID e campos não editáveis.
- [x] Enviar PUT completo e `null` para limpar campo opcional intencionalmente.
- [x] Permitir alteração de espécie, coordenadas, situação territorial e demais campos previstos.
- [x] Preservar valores preenchidos e apresentar falha sem confirmação falsa.
- [x] Navegar para a ficha somente após resposta confirmada.
- [x] Carregar opções de bairro de `GET /api/bairros`, preservando bairro atual quando indisponível para seleção.
- [ ] Verificar PUT end-to-end com autorização funcional após T19.

**Evidência:** `ArvoreFormView.vue`, `ArvoreFormulario.vue`, `arvoreCadastroForm.ts`, `useArvoreFicha.ts`, testes `arvoreEdicaoView.spec.ts` e `arvoreCadastroForm.spec.ts`.

## 6. Fase 4 — Fotografias

### T17: Implementar armazenamento de fotografias no backend — funcionalidade normal concluída; recuperação pós-crash pendente

- [x] Criar associação persistente entre foto e árvore.
- [x] Reutilizar MinIO com referência interna opaca; não expor URL/chave.
- [x] Criar upload multipart, listagem de metadados, recuperação e remoção.
- [x] Validar JPG, PNG e WebP, tamanho de 2 MB e máximo de três por árvore.
- [x] Preservar outras imagens ao remover uma fotografia.
- [x] Implementar compensação síncrona best-effort e bloquear concorrência no limite de fotos.
- [x] Testar PostgreSQL, MinIO e transporte multipart isolados com serviços reais de teste.
- [ ] Implementar reconciliação durável de órfãos/inconsistências após crash ou falha da compensação.

**Endpoints:** `POST /api/arvores/{id}/fotos`, `GET /api/arvores/{id}/fotos`, `GET /api/arvores/{id}/fotos/{fotoId}`, `DELETE /api/arvores/{id}/fotos/{fotoId}`.

**Limitação:** os testes reproduzem órfão/inconsistência quando a própria compensação falha ou o processo é encerrado; não há protocolo durável que os recupere. Essa limitação não deve ser descrita como atomicidade distribuída.

### T18: Integrar fotografias ao frontend — implementação concluída; validação operacional autorizada pendente

- [x] Listar metadados e recuperar conteúdo pelo backend.
- [x] Selecionar imagens JPG/PNG/WebP e validar MIME, tamanho e limite.
- [x] Exibir preview e liberar URLs Blob quando removidas/trocadas/desmontadas.
- [x] Enviar uma imagem por vez usando service multipart.
- [x] Exigir árvore persistida antes de associar fotografias.
- [x] Confirmar remoção de imagem existente e atualizar UI somente após confirmação do servidor.
- [x] Preservar imagens não alteradas e os dados de árvore no PUT.
- [x] Tratar 401/403, 404, 413, falha parcial e erro de rede sem falso sucesso.
- [ ] Verificar galeria/upload/remoção com backend administrativamente autorizado após T19.

**Evidência:** `ArvoreFotografias.vue`, `arvoreFotografias.ts`, métodos de fotos em `inventarioService.ts` e testes `arvoreFotografias.spec.ts`.

## 7. Fase 5 — Segurança e autorização

### T19: Implementar ou integrar autenticação e autorização — pendente e bloqueante para produção

- [ ] Confirmar estratégia compartilhada de autenticação para o Arborize.
- [ ] Validar com AMMA perfis autorizados para consulta, cadastro, edição e fotografias.
- [ ] Integrar identidade real do usuário no backend, sem criar sistema paralelo.
- [ ] Definir permissões separadas para consulta e operações de alteração.
- [ ] Proteger GET, POST, PUT e endpoints de fotos no backend.
- [ ] Integrar autenticação real ao frontend e às requisições Axios.
- [ ] Retornar e tratar corretamente 401 e 403.
- [ ] Testar autorização por regras com filtros de produção e identidade não fictícia.
- [ ] Substituir a liberação temporária `permitAll()` por proteção adequada antes de disponibilizar o módulo em produção.

**Estado verificado:** as rotas do Inventário estão liberadas por `permitAll()` para testes locais. Não há autenticação funcional nem perfis efetivos; os perfis e permissões continuam dependendo de validação com a AMMA.

## 8. Fase 6 — Testes, integração e aceite

### T20: Testes de backend — cobertura existente; regressão geral pendente

- [x] Testar cadastro válido/inválido, espécie inexistente e coordenadas.
- [x] Testar campos opcionais e valores técnicos inválidos.
- [x] Testar filtros combinados e paginação.
- [x] Testar PUT, limpeza de campos e preservação de ID/metadados.
- [x] Testar árvores mortas e recursos inexistentes.
- [x] Testar erros estruturados e dados sensíveis não expostos.
- [x] Testar uploads inválidos, 413, limite de quantidade e falhas de armazenamento.
- [x] Testar persistência PostgreSQL e integração MinIO isoladas.
- [x] Testar com filtros reais a configuração permissiva temporária de acesso local.
- [ ] Executar e registrar regressão de todos os módulos legados após harmonização de erros.

**Evidência disponível na auditoria:** `backend/arborize/target/surefire-reports/` contém 21 XML de suites, totalizando 217 testes, sem falhas, erros ou ignorados. README de integração delimita os testes reais PostgreSQL/MinIO e suas limitações. Esses arquivos são resultados preexistentes, não uma execução feita nesta auditoria.

### T21: Verificação frontend e integração fullstack — pendente

- [ ] Executar typecheck, build e lint atuais do frontend e registrar resultados reproduzíveis.
- [ ] Verificar navegação entre lista, ficha, cadastro e edição em navegador.
- [ ] Verificar mapa OSM, marcadores, seleção e responsividade no navegador.
- [ ] Verificar MapTiler Satellite com chave real e origens permitidas.
- [ ] Verificar filtros e paginação com backend autorizado.
- [ ] Verificar cadastro, ficha e edição end-to-end.
- [ ] Verificar fotos: listagem, preview, upload, limite, remoção e falhas.
- [ ] Verificar estados de carregamento, vazio, 400, 401, 403, 404, 413 e 500.
- [ ] Verificar acessibilidade básica e resoluções móveis/desktop.
- [ ] Verificar CA22–CA24: código nas três visualizações, pesquisa, miniaturas, fallback sem foto e carregamento sem baixar fotos de todos os marcadores.
- [ ] Incluir CA25–CA26 na validação integrada após concluir T28: seleção/preview, preservação do formulário e tratamento de upload parcial sem duplicar a árvore.
- [ ] Revalidar módulos existentes após correções globais.

### T22: Revisar critérios de aceite e consistência documental — em andamento; aceite integral bloqueado

- [ ] Conferir CA01–CA41 contra evidência executável, sem inferir aceite pela existência de código.
- [ ] Registrar quais critérios estão implementados, bloqueados por autorização ou pendentes de validação institucional.
- [x] Atualizar `design.md`: registrar o gerenciamento de fotografias existente e o formulário compartilhado de cadastro/edição.
- [x] Atualizar `design.md` para refletir o uso de `isEditing`, o tratamento `ProblemDetail` e o endpoint de bairros.
- [x] Atualizar matriz de rastreabilidade e estado de continuidade das specs.
- [ ] Confirmar decisões pendentes com a AMMA e documentar resultados.
- [ ] Confirmar escopo e ausência de dados institucionais fictícios.
- [x] Registrar limitações de produção, incluindo `permitAll()` para testes locais, chave MapTiler e reconciliação MinIO.

## 9. Fase 7 — Adequações de arquitetura e gaps identificados

### T23: Harmonizar tratamento global de erros com `AGENTS.md` — parcialmente concluída

- [x] Implementar `GlobalExceptionHandler` com `@RestControllerAdvice` global e `ProblemDetail` padronizado.
- [x] Manter o contrato de erros já estabelecido para o Inventário.
- [ ] Migrar controllers legados que devolvem texto/capturam exceções localmente, sem expor mensagem interna.
- [ ] Adicionar testes de regressão HTTP para materiais, espécies e eventos.
- [ ] Não alterar status/contratos de módulos legados sem revisão de compatibilidade.
- [x] Ajustar o interceptor Axios ou o tratamento na store Pinia para ler `ProblemDetail.detail` e `errors` e exibir o feedback por `AppToastContainer`.

**Estado:** o handler global e o interceptor Axios para `ProblemDetail` estão implementados. Controllers que interceptam exceções localmente ainda podem responder fora do padrão; a revisão dos módulos legados permanece pendente.

### T24: Integrar catálogo legado de bairros — concluída conforme escopo anterior; validação institucional pendente

- [ ] Confirmar fonte, nomes, abrangência e política de inativação dos bairros com a AMMA.
- [x] Implementar `GET /api/bairros`, retornando bairros ativos em ordem alfabética, sem criar registros presumidos.
- [x] Consumir `GET /api/bairros` via Axios/Pinia para popular o select no `ArvoreFormulario.vue` e as opções de filtro.
- [x] Preservar bairro já associado quando estiver inativo ou ausente da lista de seleção.
- [x] Validar no formulário a coerência entre situação territorial e bairro selecionado.

**Dependência institucional:** o endpoint lista somente os bairros ativos existentes no banco. A lista, sua fonte e abrangência ainda não foram aprovadas pela AMMA; não inserir bairros fictícios nem considerar o catálogo oficial até essa validação.

**Evolução posterior:** a decisão de RN07 revisada remove a dependência desse catálogo nos novos cadastros e filtros. T24 e seus itens `[x]` registram o trabalho entregue conforme o requisito anterior; não representam conformidade do fluxo atual revisado. A validação institucional segue aberta apenas para eventual normalização/fonte oficial e não bloqueia a transição textual. A entidade, endpoint e vínculos serão preservados para compatibilidade.

### T25: Corrigir instruções de localização do backend — pendente

- [ ] Atualizar `AGENTS.md` para apontar o backend real em `backend/arborize/src/main/java/`.
- [ ] Revisar referências de caminho em instruções e documentos SDD para não direcionar futuras varreduras ao diretório inexistente `backend/src/`.

## 10. Fase 8 — Evolução de escopo: código institucional e miniaturas

Estas tarefas foram incluídas após a implementação inicial. Estão vinculadas a RN14–RN16
e CA22–CA26; não representam trabalho já concluído nem alteram os estados das tarefas
anteriores.

### T26: Apresentar e pesquisar pelo código institucional — implementação e testes concluídos; integração fullstack pendente

**Relacionada a:** HU01, HU03, HU05; RN01, RN08, RN14; CA22.

- [x] Derivar `ARV-QXD-` + ID decimal preenchido à esquerda até seis dígitos, sem truncar IDs maiores.
- [x] Não criar sequência, coluna persistida ou campo editável adicional; manter o ID numérico nas rotas e operações.
- [x] Incluir o código nos DTOs de resumo e ficha e apresentá-lo igual na listagem, ficha e popup.
- [x] Pesquisar por correspondência parcial do código sem diferenciar caixa e removendo espaços externos, preservando os demais critérios.
- [x] Cobrir IDs de um, seis e mais de seis dígitos, estabilidade na atualização e pesquisa em testes backend/frontend.

### T27: Exibir miniaturas protegidas na listagem e no mapa — implementação e testes concluídos; aceite integrado geral pendente

**Relacionada a:** HU01, HU03; RN10, RN11, RN12, RN15; CA23–CA24.

- [x] Estender `ArvoreResumoResponseDTO` com referência opcional `fotoCapa` (`id`, `contentType`) e retornar nulo sem foto.
- [x] Definir como capa a fotografia associada com menor ID e obter os metadados em lote/projeção, sem consultas N+1.
- [x] Reutilizar `GET /api/arvores/{id}/fotos/{fotoId}` para obter o conteúdo; não expor URL pública, chave MinIO ou criar endpoint público.
- [x] Carregar miniaturas da listagem somente para linhas visíveis e do mapa somente para o popup aberto.
- [x] Exibir ilustração acessível "Sem fotografia" quando não houver arquivo, sem imagens ou registros fictícios.
- [x] Tratar erro, 401/403 e conteúdo indisponível separadamente da ausência de foto; revogar URLs Blob temporárias.
- [x] Cobrir resposta com/sem foto, falha/acesso negado, carregamento tardio e ausência de requisições para os demais marcadores em testes.

**Evidência:** DTO de resumo, seleção determinística da capa, consulta em lote, carregamento tardio no frontend e testes correspondentes estão implementados. A autorização efetiva e o aceite fullstack continuam pendentes na T19/T21.

## 11. Fase 9 — Evolução de escopo: robustez da seleção de fotografias no cadastro

### T28: Investigar e corrigir seleção de fotografias no cadastro — pendente

**Relacionada a:** HU02; RN10, RN12, RN16; CA16, CA18, CA25–CA26.

**Prioridade:** investigar e corrigir este defeito antes de refinamentos estéticos menores.

**Relato do usuário:** ao selecionar uma fotografia em `/sistema/inventario/nova`, a área
principal fica quase toda branca e a sidebar permanece visível. O usuário informa que o
defeito foi reproduzido; a causa técnica continua desconhecida. Não considerar nenhuma
causa candidata como confirmada sem evidência.

- [ ] Reproduzir e registrar o comportamento no formulário real usando a seleção de arquivo do navegador; identificar a etapa em que a tela deixa de renderizar corretamente.
- [ ] Investigar evidências de exceções JavaScript, tratamento de `File` e URLs Blob, eventos/estado reativo do Vue, overlays, posicionamento CSS e rolagem; registrar achados que confirmem ou descartem cada hipótese relevante.
- [ ] Corrigir a causa demonstrada sem limpar ou remontar os campos, a instância do mapa ou a localização já marcada.
- [x] Apresentar prévia para cada arquivo válido selecionado antes da criação e permitir remover ou substituir a seleção.
- [x] Rejeitar formatos, tamanhos e quantidades inválidos com mensagens claras, mantendo a tela e os dados utilizáveis.
- [x] Enviar arquivos somente após a confirmação da criação, pelo endpoint existente de fotografias, sem incluí-los no cadastro da árvore.
- [x] Em falha parcial de upload, preservar o ID criado, indicar quais arquivos ficaram pendentes e permitir repetir somente esses uploads, sem repetir o cadastro nem reenviar imagens já confirmadas.
- [x] Criar testes de regressão que montem o `ArvoreFormulario.vue` real e exercitem seleção de arquivos (`File`/evento de input), prévia, remoção, substituição e preservação dos dados e localização; integrar `ArvoreLocalizacaoMapa.vue` quando necessário para comprovar a preservação do ponto; não validar apenas funções auxiliares.
- [x] Testar limites e erros de arquivo, falha parcial, nova tentativa apenas dos uploads e ausência de segunda chamada de criação da árvore.
- [ ] Verificar que a tela continua renderizada e utilizável após seleção válida e inválida; registrar separadamente evidência automatizada e validação manual no navegador.

**Evidência requerida para concluir:** teste de integração do componente real cobrindo os fluxos acima e registro da causa encontrada. Se a reprodução independente não for possível, manter a causa como não confirmada e documentar as evidências de navegador necessárias; não declarar o defeito resolvido.

## 12. Fase 10 — Evolução de escopo: localização territorial sem catálogo obrigatório

As tarefas seguintes implementam RN07 revisada e CA27–CA32. Elas não invalidam o histórico
de T04/T05/T06/T07/T08/T24, mas substituem suas decisões de catálogo e associação obrigatória
para o fluxo novo. A tabela `bairros`, `bairro_id` e os registros existentes devem ser
preservados. A interface cartográfica aprovada não deve ser redesenhada.

### T29: Adicionar persistência territorial textual e transição compatível — concluída

**Relacionada a:** HU02, HU04; RN03, RN07, RN09; CA27–CA30, CA32.

- [x] Criar migração aditiva para colunas nullable `bairro_nome` e `cep` em árvores, sem remover tabela, coluna, FK ou registros existentes.
- [x] Identificar e documentar o mecanismo de migração de dados compatível com o projeto; `ddl-auto=update` não substitui o backfill e não foi localizado Flyway/Liquibase na configuração examinada.
- [x] Copiar o nome do bairro associado para `bairro_nome` somente quando houver associação e o novo campo estiver vazio; preservar o ID e não inventar/reescrever dados.
- [x] Atualizar entidade e invariantes territoriais: `BAIRRO_IDENTIFICADO` exige nome confirmado; bairro não identificado não recebe nome; `FORA_DA_AREA_URBANA` permanece uma escolha explícita, nunca inferida de coordenadas/geocoder.
- [x] Manter o vínculo `Bairro` legado sem cascata e sem torná-lo pré-requisito para novos registros.
- [x] Testar migração/backfill com árvore vinculada, sem vínculo, nome já preenchido e múltiplos bairros existentes; comprovar preservação de IDs e dados.

### T30: Atualizar contratos, validações e pesquisa territorial do backend — concluída

**Relacionada a:** HU01, HU02, HU04, HU05; RN07–RN09; CA27–CA32.

- [x] Adicionar `bairroNome` e `cep` opcionais aos DTOs de cadastro, atualização e resposta, mantendo o objeto `bairro` legado durante a transição.
- [x] Aceitar cadastros sem `bairroId`; preservar compatibilidade de leitura e, se necessário, de escrita com clientes antigos até implantação coordenada.
- [x] Fazer `bairroNome` ser o dado canônico do novo fluxo e validar a classificação territorial sem resolver catálogo nem inferir área urbana.
- [x] Introduzir parâmetro textual `bairro` com correspondência parcial e sem distinção de caixa nos endpoints de listagem e mapa; considerar `bairro_nome` e fallback para `bairro.nome` legado.
- [x] Preservar paginação, filtros combinados e igualdade de resultados entre listagem e mapa.
- [x] Criar testes de controller/service/repository para dados novos, legados, bairro parcial, filtros combinados, ausência de CEP/bairro e estados territoriais distintos.

### T31: Atualizar formulário e filtro de bairro no frontend — concluída

**Relacionada a:** HU01, HU02, HU04, HU05; RN03, RN07–RN09; CA27–CA31.

- [x] Remover a exigência de carregar `GET /api/bairros` para cadastrar ou editar árvore; manter o endpoint fora do caminho crítico.
- [x] Exibir bairro, CEP e referência como valores opcionais editáveis; apresentar sugestões do geocoder sem tratá-las como oficiais e permitir confirmação/correção.
- [x] Garantir que coordenadas sejam atualizadas imediatamente ao selecionar o ponto, sem aguardar reverse geocoding; falha ou ausência de resultado não bloqueia cadastro.
- [x] Permitir escolher explicitamente as três situações territoriais; nunca inferir `FORA_DA_AREA_URBANA` por coordenadas, geocoder ou limite presumido.
- [x] Substituir filtro de seleção pelo filtro textual de bairro registrado e enviar o mesmo critério à tabela e ao mapa.
- [x] Criar testes de componente/store para geocoder sem bairro/CEP, edição manual, preservação dos campos, estados territoriais e filtros, sem registros fictícios.

### T32: Verificar transição integrada e compatibilidade territorial — concluída

**Relacionada a:** HU01–HU05; RN03, RN07–RN09; CA27–CA32.

- [x] Validar em integração que árvores existentes continuam exibindo o bairro derivado da associação preservada após backfill.
- [x] Validar cadastro e edição novos sem `bairroId`, com bairro textual, sem bairro e sem CEP.
- [x] Validar busca textual de bairro em listagem e mapa, combinada com pesquisa, espécie e condição.
- [x] Confirmar ausência de classificação automática fora da área urbana e preservar a distinção dos estados territoriais.
- [x] Documentar compatibilidade e sequência de implantação/rollback sem apagar associações ou conteúdo territorial.
- [x] Confirmar evidência de migração, testes e comportamento integrado antes de registrar as tarefas T29–T32 como concluídas.

**Evidência:** persistência e backfill foram exercitados em PostgreSQL descartável; contratos e filtros foram testados com PostgreSQL descartável; os testes frontend cobrem sugestão, edição manual, estados territoriais e sincronização mapa/lista. O aceite de uso real continua sujeito à T19/T21 e às validações institucionais indicadas abaixo.

## 13. Matriz de rastreabilidade resumida

| Requisito/critério | Código já existente | Pendência principal |
| --- | --- | --- |
| RN01–RN04; CA01–CA03, CA19 | Entidade, DTOs, validações, API, formulário e testes | Uso real bloqueado por T19; persistência de CEP integra a transição T29–T32. |
| RN05–RN06; CA07 | Enum provisório, persistência, ficha e testes | Validar classificação com AMMA. |
| RN07/RN17; CA05, CA13, CA27–CA41 | Catálogo local ativo por ID no urbano, localidade rural, classificação interna compatível e seletor nativo único no formulário e filtro textual por nome | Validação institucional da lista pela AMMA; teste de teclado no navegador não registrado; autorização e aceite integrado permanecem pendentes (T19/T21/T22/T36). |
| RN08/RN17; CA04–CA05, CA09, CA13, CA17, CA41 | API de consulta, mapa, tabela e mesmo filtro territorial de nome nos dois resultados | Consulta live autorizada e regressão fullstack permanecem pendentes (T19/T21); aceitação visual T37 foi confirmada pelo usuário, sem equivaler a teste integrado completo. |
| RN09; CA06, CA14–CA15, CA18, CA29, CA32, CA39 | PUT, formulário de edição e transição legada verificados | Consulta/salvamento live autorizado permanece pendente (T19/T21). |
| RN10/RN12/RN16; CA08, CA10, CA16, CA18, CA25–CA26 | Endpoints e interface de fotografias; prévia, uploads e sucesso parcial têm cobertura automatizada | Defeito visual observado na seleção durante cadastro permanece sem causa/correção confirmada (T28); aceite live após T19/T21. |
| RN11; CA12 | Rotas liberadas temporariamente com `permitAll()` para testes locais | Autenticação, política de perfis e autorização; não está pronto para produção (T19). |
| RN13; CA20–CA21 | Mapa OSM e opção MapTiler implementados | Chave, origem e tiles reais (T12/T21). |
| RN14; CA22 | ID numérico permanente já existe | Código derivado, pesquisa e apresentação nas três visualizações (T26). |
| RN15; CA23–CA24 | Endpoints protegidos de conteúdo de foto já existem | Metadado de capa no resumo, leitura em lote e miniaturas tardias (T27); segurança efetiva depende T19. |

## 14. Ordem recomendada para encerrar

1. A evolução urbano/rural foi implementada em T33–T36; a carga dos 24 bairros já foi aplicada ao PostgreSQL local após autorização operacional. Não repetir a carga sem nova necessidade e autorização; não confundir a operação local com homologação institucional.
2. Obter validação da AMMA para a lista de referência e para classificação fitossanitária, procedimentos de medição e perfis autorizados. A validação institucional da lista não altera o fato de que sua fonte pública foi verificada.
3. Validar perfis com a AMMA e integrar a autenticação compartilhada; substituir `permitAll()` antes de produção (T19).
4. Verificar MapTiler com chave de ambiente restrita e completar T12/T21.
5. Executar integração frontend/backend autorizada e regressão fullstack em T21.
6. Completar a migração dos handlers legados e o consumo de `ProblemDetail` no frontend (T23).
7. Corrigir referências de estrutura do repositório em T25 e reconciliar `design.md`/matriz em T22.
8. Manter T26–T27 como concluídas segundo as evidências técnicas e automatizadas registradas; validar sua operação protegida dentro da T21 após T19.
9. Executar T28 para investigar e corrigir a falha de seleção de fotografias no cadastro antes de refinamentos estéticos menores; validar CA16, CA18 e CA25–CA26 sem presumir a causa.
10. Reavaliar CA01–CA41 e registrar aceite final em T22 somente após T19, T21, T28 e validações institucionais aplicáveis. A confirmação visual reportada para T37 não substitui esses critérios.

## 15. Definição de pronto

Uma tarefa só está concluída quando o comportamento correspondente estiver implementado, os testes/verificações previstos tiverem evidência, os erros forem tratados conforme o contrato, as limitações estiverem explícitas e não houver dependência bloqueante omitida.

O módulo só pode ser considerado pronto para uso administrativo real quando:

- A autenticação e autorização estiverem funcionais no backend, cobrindo consultas e alterações.
- As regras institucionais aplicáveis estiverem aprovadas ou formalmente delimitadas.
- O fluxo urbano exigir seleção de bairro ativo e o rural localidade textual; a fonte municipal inicial estiver documentada, sua validação pela AMMA permanecer explícita e os vínculos anteriores forem preservados.
- O código institucional for derivado do ID permanente e permanecer consistente nas visualizações, sem uma sequência paralela.
- As miniaturas usarem o acesso protegido existente e forem carregadas sob demanda, sem downloads de todas as fotos do mapa.
- A seleção de fotografias no cadastro preservar formulário, mapa e localização, apresentar prévias e tratar falha parcial sem duplicar a árvore.
- Os critérios BDD obrigatórios tiverem sido verificados em fluxo integrado.
- A API não transformar falhas em estados vazios ou falsos sucessos.
- Os limites conhecidos de MapTiler e armazenamento MinIO estiverem tratados e documentados.
- O checklist e o design corresponderem ao código auditado.

## 16. Fase 11 — Evolução de escopo: classificação urbana/rural e catálogo inicial de referência

Esta evolução substitui para novos cadastros a entrada textual territorial livre definida em
T29–T32, preservando seus registros e histórico. Não altera o mapa aprovado, fotografias,
segurança, códigos institucionais ou miniaturas. Os bairros listados são referência municipal
publicada, não homologação da AMMA. Relacionada a RN07 revisada e CA33–CA39.

### T33: Preparar carga inicial idempotente dos bairros de referência

**Relacionada a:** HU02, HU04; RN07; CA36–CA37.

- [x] Confirmar no PMPI publicado pela Prefeitura a lista dos 24 nomes e documentar a referência, data de consulta e divergência histórica da Lei 2.319/2007.
- [x] Criar SQL manual seguro que insira somente equivalentes ausentes, compare acentuação/caixa/espaços, não reative/renomeie/exclua registros e preserve IDs/associações.
- [x] Incluir verificações pré/pós-execução e instruções de aplicação local que exijam autorização explícita, sem execução automática no boot.
- [x] Testar execução repetida, nomes já existentes, registro inativo preexistente e preservação de ID/associação exclusivamente em PostgreSQL descartável.

**Evidência dos testes:** fonte municipal verificada em 2026-11-01; SQL de carga executado duas vezes no PostgreSQL 15 efêmero; resultados confirmam nomes equivalentes únicos e preservação dos IDs, nomes e estados preexistentes. Esses testes não alteraram banco de aplicação. A AMMA ainda deve validar a lista.

**Atualização operacional posterior:** após autorização explícita do usuário, a carga dos 24 bairros foi aplicada ao PostgreSQL local de desenvolvimento. O usuário confirmou a presença do catálogo ativo e o funcionamento de `GET /api/bairros`. Não foi executado backfill territorial; esta operação não representa homologação institucional nem autoriza carga em outro ambiente.

### T34: Adaptar persistência e contratos backend ao modo urbano/rural

**Relacionada a:** HU02, HU04; RN03, RN07, RN09; CA33–CA37, CA39.

- [x] Adicionar `localidade_rural` nullable sem remover `bairro_id`, entidade `Bairro`, `bairroNome`, `cep` ou classificação legada; atualizar somente o SQL manual aditivo.
- [x] Adicionar `zonaRural` e `localidadeRural` aos requests/responses sem quebrar construtores ou clientes legados; conservar campos territoriais antigos para leitura/compatibilidade.
- [x] Derivar a classificação interna exclusivamente de `zonaRural` nos requests novos; exigir bairro ativo por ID no modo urbano e localidade rural não vazia no modo rural.
- [x] Preservar IDs, metadados, associações legadas e registros `BAIRRO_NAO_IDENTIFICADO`; não associar árvores antigas automaticamente por semelhança de nomes.
- [x] Manter o filtro textual de bairro e igualdade entre consultas da tabela/mapa, incluindo fallback legado; excluir localidade rural do filtro de bairro.
- [x] Cobrir DTO/validator/service/controller/mapper e persistência no PostgreSQL descartável para modos válidos/inválidos, atualizações, inatividade, legados e filtros.

**Evidência:** compilação e suíte de integração PostgreSQL descartável; inclui cadastro urbano/rural, edição explícita de registro legado, metadados/associação, filtro combinado igual entre tabela e mapa e backfill manual.

### T35: Integrar escolha territorial e catálogo pesquisável no frontend

**Relacionada a:** HU01, HU02, HU04, HU05; RN07–RN09; CA33–CA35, CA38–CA39.

- [x] Usar checkbox “A árvore está localizada em zona rural”, desmarcado por padrão; alternar os campos sem descartar valores locais silenciosamente.
- [x] Carregar `GET /api/bairros` pela store/service existentes e oferecer busca e seletor nativo acessível por teclado, usando apenas bairros ativos e payload com ID.
- [x] Exigir bairro ativo no modo urbano e localidade rural textual no modo rural; manter CEP/referência opcionais e coordenadas imediatas.
- [x] Expandir os dados de reverse geocoding sem mudança visual do mapa; sugerir bairro apenas com correspondência única normalizada e sugerir localidade rural como texto editável.
- [x] Impedir que geocoder classifique zona ou que respostas atrasadas substituam modo/campos editados manualmente; tratar ausência/ambiguidade sem inventar dados.
- [x] Carregar e editar registros novos e legados sem apagar associações/metadados; manter filtro de bairro textual atual e a apresentação territorial sem chamar localidade rural de bairro.
- [ ] Cobrir formulário real, mudança de modo, pesquisa/seleção por teclado, correspondência com e sem acento, ambiguidade, alterações manuais concorrentes, validação/foco e payload create/update em navegador real; componentes e lógica estão cobertos por testes de host, mas teclado nativo do seletor não foi exercitado em navegador.

**Evidência automatizada:** TypeScript e build aprovados; 58 testes frontend, incluindo formulário real com host Vue/MapLibre, alternância, sugestões/ambiguidade, proteção contra respostas tardias, payload e mapa preservado. O teste visual/teclado de navegador está pendente por `listen EPERM` no servidor Vite.

### T36: Validar a evolução territorial integrada

**Relacionada a:** HU01–HU05; RN03, RN07–RN09; CA33–CA39.

- [x] Executar os testes de T33 e contratos/persistência T34 em PostgreSQL exclusivamente descartável; registrar separadamente qualquer operação autorizada no banco local de desenvolvimento.
- [x] Verificar criação/edição urbana e rural, falhas de validação, dados opcionais, classificação interna, preservação legada e payload do frontend.
- [x] Verificar igualdade do filtro de bairro entre listagem e mapa, inclusive nomes legados e filtros combinados.
- [x] Executar TypeScript, build, lint e testes frontend/backend pertinentes e registrar resultados separados por nível (mock, unitário, integração PostgreSQL).
- [ ] Verificar os dois fluxos no navegador real se disponível; registrar MapTiler/live API e AMMA como pendências caso não haja evidência.
- [x] Atualizar a matriz de rastreabilidade sem marcar autenticação T19, validação institucional ou aceite global do módulo.

**Evidência:** T33–T34 foram exercitadas em PostgreSQL 15 descartável; TypeScript, build, lint e testes automatizados foram executados. A verificação de navegador prevista especificamente em T36 permaneceu pendente no ambiente daquela execução (`listen EPERM`). A confirmação visual posterior da T37 é evidência somente das alterações daquela tarefa e não substitui a validação integrada de T36. T19 e validação institucional seguem pendentes.

## 17. Fase 12 — Evolução de escopo: simplificação dos seletores de bairro

Esta evolução atualiza a experiência introduzida em T35 sem alterar o catálogo, backend,
mapa ou regras urbano/rural. T35 e T36 permanecem como histórico e não têm seus checkboxes
reabertos ou reescritos. Relacionada a RN07, RN08, RN17 e CA40–CA41.

### T37: Simplificar o seletor do formulário e integrar o filtro do inventário — implementação e aceitação visual informada; verificação manual de teclado pendente

**Relacionada a:** HU01, HU02, HU04, HU05; RN07, RN08, RN17; CA33–CA35, CA38–CA41.

- [x] Remover do cadastro/edição a busca separada de bairro; manter um seletor nativo com opção inicial, bairros ativos em ordem alfabética e seleção/payload pelo ID.
- [x] Corrigir a sincronização de validação para limpar erro somente após selecionar ID ativo; rejeitar ID ausente/inativo e manter seleção de edição, sugestão geográfica e escolha manual concorrente.
- [x] Preservar o fluxo rural, em que o seletor urbano fica oculto e a localidade rural continua obrigatória.
- [x] Substituir o campo textual de bairro da página principal por seletor alimentado pelo catálogo ativo; enviar o nome no parâmetro textual `bairro`, omitir o critério para “Todos os bairros” e aplicar o mesmo filtro à tabela e ao mapa.
- [x] Preservar pesquisa, espécie, condição, tamanho de página e compatibilidade do filtro textual com nomes legados suportados pelo backend.
- [x] Criar testes automatizados do formulário Vue real, store, apresentação da listagem e consulta; cobrir catálogo ativo/ordenado, validação, edição, rural, filtro “Todos” e sincronização tabela/mapa.
- [x] Registrar a confirmação posterior do usuário no navegador de que as alterações visuais da T37 ficaram corretas; esta evidência cobre a aceitação visual informada, não a integração fullstack.
- [ ] Registrar verificação específica de navegação por teclado no seletor nativo; a implementação nativa e os testes automatizados não substituem essa evidência manual.
- [x] Atualizar RN07/RN08, design e rastreabilidade sem alterar tarefas históricas nem os critérios de mapa aprovado.

**Evidência:** TypeScript, build e lint passaram; 123 testes direcionados passaram, incluindo o formulário Vue real sob host de teste, store, filtro textual e igualdade dos critérios/resultados da tabela e do mapa. O usuário confirmou posteriormente no próprio navegador que as modificações da T37 ficaram corretas. Essa confirmação é aceita como evidência visual da T37, mas não prova todos os fluxos integrados, navegação por teclado ou o aceite geral da spec.

## 18. Estado formal de encerramento

**Estado da etapa: Implementação concluída; pronta para integração; aceite final pendente.** O escopo funcional está congelado para a integração geral. Não iniciar novas funcionalidades do Inventário durante essa fase, exceto correções comprovadamente necessárias.

Isso encerra a etapa atual de implementação, não declara todos os critérios de aceitação cumpridos nem altera os checkboxes pendentes. A confirmação visual do usuário para T37 é preservada no alcance indicado; a spec permanece sem aceite integral até as verificações abaixo:

- **T19 / CA12:** autenticação e autorização da aplicação não estão implementadas; as rotas continuam liberadas com `permitAll()` apenas para testes locais.
- **T21:** falta a verificação fullstack autorizada dos fluxos de consulta, cadastro, edição e fotografias, incluindo estados HTTP e responsividade.
- **T28 / CA25–CA26:** permanece o defeito visual reportado ao selecionar fotografias no cadastro; a causa e a correção não foram confirmadas, ainda que prévias, validações e upload parcial tenham testes automatizados.
- **T22:** a revisão final dos critérios de aceitação contra evidência executável não pode ser concluída enquanto os bloqueios acima permanecerem.
- **Validações institucionais:** classificação fitossanitária, procedimentos de medição/estimativa, fonte e abrangência do catálogo e perfis autorizados continuam sem homologação formal da AMMA.

### Retomada na integração, estabilização ou homologação

As pendências permanecem nas tarefas existentes; esta seção apenas identifica seus pontos de retomada, sem criar tarefas duplicadas:

| Pendente | Ponto de retomada |
| --- | --- |
| Autenticação/autorização | T19, antes do aceite operacional e da disponibilização administrativa. |
| Verificação fullstack | T21, após T19 e com backend/serviços de desenvolvimento autorizados. |
| Aceite final dos critérios | T22, depois das verificações T19, T21, T28 e decisões institucionais aplicáveis. |
| Defeito visual na seleção de fotografias | T28, durante estabilização; investigar a causa e validar no navegador antes de declarar correção. |
| Validações da AMMA | T02/T24 e decisões registradas em `requirements.md`; homologação durante a fase institucional apropriada. |
| MapTiler e acessibilidade | T12/T21 para tiles reais; T35/T36/T37 para acessibilidade e navegação por teclado, sem confundir a aceitação visual T37 com essa verificação. |

A implementação pode seguir para integração com esses itens explicitamente abertos. Nenhuma pendência foi reclassificada como melhoria futura apenas para fechar a spec, e nenhum requisito obrigatório foi descartado.

A carga local dos 24 bairros não equivale a validação institucional, e a aceitação visual da T37 não equivale a teste fullstack nem ao aceite integral do módulo. Não declarar o Inventário pronto para uso administrativo real ou produção até que os critérios de encerramento da seção 15 e as pendências acima sejam resolvidos.
