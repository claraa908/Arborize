# Design

## Context

Ver `proposal.md` para a motivação e `specs/compensacao-ambiental/spec.md` para o contrato de comportamento. `docs/padroes-de-projeto.md` (quando presente) e `AGENTS.md` descrevem a estrutura Vue/Spring, padrões visuais, componentes globais, segurança, armazenamento e tratamento de erros do projeto. As referências visuais disponíveis são `openspec/docs/interfaces/spec-03/tela1_inicial_compensacao_ambiental.png`, `tela2_cadastro_compensacao_ambiental.png` e `tela3_editar_consultar_termo.png`. Não há implementação nem spec anterior de compensação ambiental.

## Goals / Non-Goals

**Goals:**

- Manter separação clara de responsabilidades entre interface (Vue), store (Pinia), service HTTP (Axios) e camadas REST/serviço/persistência (Spring Boot).
- Tratar arquivos (PDF do termo e recibos em PDF ou imagem) com validação rigorosa e referência/metadados associados ao termo ou à entrega.
- Permitir o cadastro de termos, o registro acumulativo de entregas/recibos e a gestão do estado do acordo (*Em aberto*, *Parcialmente doado*, *Totalmente doado/Fechado*) por usuário interno autorizado da AMMA, com validação no servidor.
- Tornar a interface utilizável em desktop, tablet e viewports estreitas, sem expor a funcionalidade na área pública do cidadão.

**Non-Goals:**

- Integrar diretamente ao fluxo de doação/retirada de cidadãos no IFCE ou alterar o escopo de outras specs paralelas da Sprint 3.
- Antecipar itens de baixa prioridade (como doações voluntárias de cidadãos, registro de plantio particular, adoção/certificados, alertas automáticos ou cronograma de manutenção).
- Alterar a autenticação transversal existente ou refatorar módulos fora deste domínio sem necessidade.

## Decisions

### Frontend e composição visual

- Criar páginas Vue com Composition API e `<script setup lang="ts">`, componentes de domínio para listagem/acompanhamento, formulário de termo e registro/histórico de entregas e recibos, com tipagem TypeScript explícita. A view coordena rota e interações, a store Pinia gerencia estado/termos/operações e o service concentra chamadas HTTP via Axios.
- Reutilizar layouts, componentes globais (`AppToastContainer`, `AppConfirmModal`, tabela/estrutura do projeto quando compatíveis) e tokens Tailwind do Arborize. Em telas estreitas, garantir usabilidade sem rolagem horizontal forçada nas ações principais.
- Na listagem/acompanhamento, seguir o layout de referência: cabeçalho com ação de novo termo, indicadores resumidos quando sustentados pelos dados carregados e tabela/lista com empreendimento/empresa, identificação do termo, quantitativo acordado, quantitativo entregue, estado do acordo e ações de consulta/edição/entrega. Nunca usar números demonstrativos como dados reais.
- No cadastro e consulta/edição, priorizar os dados confirmados por RF017 (razão social, CNPJ, quantitativo de mudas e PDF assinado) e manter número do termo, data de assinatura e prazo como campos da tela de referência até que a regra de obrigatoriedade seja confirmada.
- Tratar recibos de parcela como registros acumulativos. Ao cadastrar e após cada nova entrega, derivar o estado no backend a partir da soma: zero = Em aberto; abaixo do acordado e maior que zero = Parcialmente doado; igual ou acima = Totalmente doado/Fechado. Persistir também o estado para permitir ajuste explícito por funcionário/admin AMMA quando o acordo considerar insumos além de mudas; o próximo recibo de mudas recalcula o estado com base no total comprovado.
- Implementar estados de carregamento, vazio, erro com nova tentativa, validação inline de campos/arquivos e feedback de sucesso/erro.

### Backend e contratos

- Organizar em `Controller` → `Service` → `Repository`, com DTOs explícitos de request/response, entidades JPA privadas à camada de persistência e migrations aditivas via Flyway. Nesta mudança o `ddl-auto` existente é mantido; a divergência temporária para o `validate` global está descrita na seção Flyway abaixo.
- Persistir no PostgreSQL os dados do termo, metadados/referências de arquivos e histórico de entregas/recibos. Cada nova entrega soma/acumula ao total entregue do termo, preservando entregas anteriores.
- Persistir o estado do acordo como enum controlado. Expor uma operação de atualização restrita ao perfil `FUNCIONARIO_AMMA` ou `ADMINISTRADOR_AMMA`; validar que o valor pertence aos estados previstos e apresentar texto/ícone. Ao registrar uma nova parcela de mudas, recalcular e persistir o estado pelos quantitativos acumulados, conforme RF017.
- Validar entradas com `jakarta.validation` no `Controller` e regras de domínio no `Service` (incluindo quantidade positiva, campos obrigatórios e validação de permissões/estado).
- Expor endpoints REST documentados em `api.md`: listagem/consulta/criação/edição de termos, alteração do estado, registro acumulativo de entregas e recuperação privada de documentos. Padronizar erros com `ProblemDetail` (RFC 7807), inclusive 401/403 específicos desta rota, sem vazar stacktraces ou dados sensíveis em logs.

### Autorização e armazenamento

- Proteger rotas de interface e endpoints REST para exigir autenticação e perfil interno autorizado da AMMA, negando acesso por padrão a usuários não autorizados ou anônimos.
- Armazenar o PDF do termo e os arquivos de recibo (PDF ou imagem) no MinIO, gravando no PostgreSQL apenas metadados e referência necessária, sem expor credenciais, chaves internas sensíveis ou URLs públicas indevidas.
- Validar conteúdo real, extensão, MIME type e tamanho máximo antes do MinIO: PDF do termo até 10 MB, recibos PDF/JPG/PNG até 2 MB. O limite de recibos é uma escolha conservadora coerente com o limite atual de fotografia. Validar PDF por assinatura e leitura estrutural com PDFBox já disponível; validar imagens por decodificação real, sem confiar apenas no nome/MIME enviado pelo cliente.
- Garantir atomicidade/compensação entre banco e MinIO: se o upload falhar ou a persistência no banco falhar após upload, não confirmar sucesso nem deixar registro inconsistente/órfão.

### Flyway e configuração JPA

- Adicionar o starter Flyway e módulo PostgreSQL necessários para executar migrations. A migration desta mudança cria somente as tabelas de compensação e suas constraints/índices; não baselinar nem criar estruturas dos demais domínios.
- Manter os valores de `spring.jpa.hibernate.ddl-auto` existentes em todos os perfis. O projeto ainda não tem baseline Flyway para as entidades legadas, portanto `ddl-auto=validate` global fica temporariamente pendente. Não afirmar validação global concluída; validar a migration isolada em PostgreSQL e registrar essa divergência até uma mudança de infraestrutura separada.

### Referência visual e Figma

As telas exportadas (`tela1_inicial_compensacao_ambiental.png`, `tela2_cadastro_compensacao_ambiental.png` e `tela3_editar_consultar_termo.png`) foram utilizadas como referência visual inicial. Caso o acesso direto ao Figma seja disponibilizado ou necessário, confirmar detalhes visuais, espaçamentos, estados e limites exatos de campos/arquivos com o arquivo original e a equipe.

## Risks / Trade-offs

- [Status selecionável no mockup diverge do RF017] → Permitir ajuste manual pelo perfil AMMA autorizado porque o acordo pode incluir insumos além de mudas; o recebimento de uma nova parcela de mudas recalcula o estado pela soma registrada.
- [Acúmulo de entregas e múltiplos recibos] → O histórico de entregas deve somar ao total entregue em vez de sobrescrever; definir claramente o contrato de envio de cada nova entrega para evitar duplicidade acidental.
- [Limite de 2 MB para recibos não vem do RF017] → É uma decisão restritiva de implementação, ancorada no limite de fotografia existente e aplicada igualmente a recibos PDF/JPG/PNG; documentar no formulário e validar no backend.
- [Falha distribuída entre PostgreSQL e MinIO] → Tratar falhas de armazenamento com rollback/remoção de objeto órfão e retorno de `ProblemDetail` sem sucesso falso.
- [Projeto ainda não tem baseline Flyway para tabelas existentes] → Executar a migration somente das tabelas desta capacidade e preservar `ddl-auto` atual; planejar baseline global numa mudança dedicada sem afetar esta sprint.
- [Banco legado não tem histórico Flyway] → A configuração padrão do Flyway não aplica automaticamente a migration em um schema PostgreSQL já preenchido e sem `flyway_schema_history`. Como baseline de tabelas externas não está autorizada, o primeiro deploy precisa de uma estratégia operacional de bootstrap isolada/ambiente limpo antes da ativação; não criar essa baseline nesta mudança.
- [Status pode refletir itens além de mudas] → Permitir ajuste manual por perfil interno autorizado; recalcular automaticamente após novo recibo de mudas e manter as entregas históricas.
- [Autorização e integração paralela na Sprint 3] → Respeitar os perfis/rotas reais do projeto e evitar alterações fora do escopo da especificação.

## Migration Plan

1. Confirmar detalhes pendentes do `RF017`, limites de upload e perfis/rotas reais do projeto; ajustar a spec se necessário antes de codificar.
2. Criar migration Flyway aditiva somente para compensação e validar sua aplicação no PostgreSQL. Manter temporariamente `ddl-auto` como está configurado porque uma baseline para outras tabelas está fora do escopo autorizado. A validação PostgreSQL ainda depende de um serviço PostgreSQL acessível no ambiente de execução.
3. Desenvolver a interface e componentes em Vue 3 + TypeScript + Tailwind com dados demonstrativos fictícios e validar layout, estados e acessibilidade (Design First).
4. Implementar DTOs, `Service`, `Controller`, `ProblemDetail`, integração com MinIO e store Pinia, testando cenários de sucesso, validação e falha.
5. Para rollback, reverter o deploy/branch da aplicação mantendo migrations aditivas compatíveis; qualquer limpeza de dados/objetos deve seguir procedimento operacional explícito sem exclusão automática indevida.
