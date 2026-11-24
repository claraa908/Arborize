# Tasks

## 1. Fechar as decisões de domínio e segurança

- [x] 1.1 Conferir RF017 e Épico 06 com base no trecho fornecido pelo usuário; registrar razão social, CNPJ, PDF oficial, quantitativos de mudas, acompanhamento baseado nos recibos e a decisão posterior de permitir ajuste manual do estado pela AMMA.
- [x] 1.2 Confirmar perfis `FUNCIONARIO_AMMA` e `ADMINISTRADOR_AMMA`, unidade de mudas e acúmulo de parcelas; registrar que novas parcelas recalculam o estado, com ajuste manual permitido para refletir outros insumos.
- [x] 1.3 Definir PDF do termo até 10 MB e recibos PDF/JPG/PNG até 2 MB; reutilizar a compensação transacional do armazenamento de fotos para remover objeto após rollback e validar limites/tipo no backend.

## 2. Interface visual Design First

- [x] 2.1 Implementar listagem/acompanhamento, cadastro, consulta/edição/entregas e controle manual do estado por usuário AMMA autorizado em Vue 3 (Composition API e `<script setup lang="ts">`), TypeScript e Tailwind, reutilizando componentes globais e dados fictícios; comparar com as referências e verificar responsividade, labels, foco e texto/ícone explícitos. Verificação: build Vite e revisão dos layouts desktop/mobile por código; navegador local não disponível para captura visual.
- [x] 2.2 Implementar estados visuais de carregamento, vazio, erro com nova tentativa, validação inline e sucesso sem API; verificar feedback via `AppToastContainer`, mensagens associadas e preservação dos campos. Verificação: estados isolados em componente testável e layouts responsivos revisados por código; captura em navegador indisponível.
- [x] 2.3 Adicionar testes dos status, atualização manual autorizada, estados de listagem e textos acessíveis; executar `vitest run --environment node src/__tests__/compensacaoInterface.spec.ts` e registrar eventuais lacunas. Verificação: 11 testes de interface e store passaram; suíte executada em Node porque `jsdom` está ausente neste node_modules.

## 3. Persistência e API protegida

- [ ] 3.1 Criar entidades JPA e migration Flyway aditiva apenas para termos de compensação, metadados/referências de arquivos e entregas/recibos; validar a migration isoladamente no PostgreSQL. Manter o `ddl-auto` atual sem baseline de outros módulos e registrar a divergência temporária frente ao `validate` global. Implementação criada e compilação Maven passou; pendente apenas validar/aplicar a migration contra PostgreSQL real quando o banco estiver disponível no ambiente/local.
- [x] 3.2 Implementar DTOs validados (`jakarta.validation`), `Repository` e `Service` para cadastro, consulta, entregas acumuladas e estado editável; testar acúmulo/recalculo após recibos, atualização manual válida/inválida e entradas/arquivos inválidos. Verificação: testes unitários focados passaram.
- [x] 3.3 Implementar `Controller` REST e `@ControllerAdvice` com `ProblemDetail` (RFC 7807) para validação, não autorizado, não encontrado e falhas operacionais; testar contratos HTTP e confirmar que stacktraces, detalhes internos ou dados sensíveis não são expostos. Verificação: MockMvc confirma 401/403 com ProblemDetail e perfis AMMA autorizados; testes de advice passaram.
- [x] 3.4 Implementar autorização no backend para o perfil interno autorizado da AMMA e armazenamento no MinIO para PDFs de termos e recibos (PDF ou imagem) com validações de tipo/tamanho; testar usuário permitido/negado, arquivo aceito/rejeitado e falha de armazenamento sem confirmação falsa ou registro inconsistente. Verificação: acesso permitido/negado testado via MockMvc, PDF real e limites validados, rollback/falha MinIO sem persistência verificado por teste unitário; endpoint MinIO real depende do ambiente de integração.
- [x] 3.5 Documentar os endpoints, DTOs, limites de upload e regras de autorização da capacidade; conferir coerência entre contratos, testes e especificação em `api.md`.

## 4. Integração frontend

- [x] 4.1 Criar tipos TypeScript, service Axios e store Pinia para listagem, detalhe, cadastro e registro acumulativo de entregas/recibos; executar checagem de tipos e testes da store para sucesso, carregamento e falhas sem mascarar erro como lista vazia. Verificação: testes passaram e não há erros de tipo novos na compensação; `npm run type-check` permanece com falhas preexistentes em `eslint.config.ts` e nas views da Biblioteca, sem alterações nessas áreas.
- [x] 4.2 Conectar as telas à API, exibir quantitativos/estado do backend, permitir atualização autorizada do estado e tratar validação/`ProblemDetail` preservando os dados preenchidos; validar cenários de sucesso e falha da spec. Verificação: build Vite e testes de store/estados passaram; fluxos estão ligados aos endpoints, sem ensaio E2E com serviços externos.
- [x] 4.3 Validar consulta/acesso aos documentos com usuário autorizado e negação para usuário sem permissão; garantir que a interface nunca exponha bucket, chave interna ou URL pública indevida do MinIO. Verificação: rotas de documento servem somente bytes via API protegida; teste de autorização HTTP cobre cidadão e perfis internos; não são expostas chaves/bucket.

## 5. Verificação de integração

- [x] 5.1 Executar build/tipagem do frontend e compilação/testes do backend, além de checar rotas e fluxos existentes; registrar resultados e corrigir regressões causadas por esta spec. Verificação: build Vite e testes focados frontend/backend passaram; a checagem total de tipos relata apenas falhas preexistentes de ESLint/Biblioteca, sem novas falhas de compensação.
- [ ] 5.2 Percorrer cadastro, consulta e registro acumulativo de entregas/recibos ponta a ponta com PostgreSQL e MinIO (incluindo falhas de rede/arquivo e estados vazio/carregando/erro/sucesso); conferir a transição automática do estado, responsividade e isolamento das demais specs da Sprint 3.
