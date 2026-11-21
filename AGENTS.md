# Instruções do projeto — Arborize (Sprint 3)

## Objetivo
Construir e evoluir o **Arborize**, um sistema unificado para automatizar, rastrear e otimizar os processos ambientais da Autarquia Municipal de Meio Ambiente (AMMA) de Quixadá.

O sistema opera em duas frentes:
1. **Interface Pública:** Para os cidadãos solicitarem mudas (ex.: com conversão de materiais recicláveis), solicitarem substituição de espécies invasoras, registrarem plantios particulares, participarem de mutirões e acessarem o acervo de educação ambiental.

2. **Painel de Gestão (Backoffice):** Para a gestão e técnicos da AMMA e parceiros (como o laboratório do IFCE) administrarem solicitações de mudas, aprovações, baixas de estoque, inventário arbóreo georreferenciado, laudos técnicos de supressão e termos de compensação ambiental.

O desenvolvimento desta sprint segue os princípios de **Specification-Driven Development (SDD)**, utilizando o **OpenSpec** para organizar propostas de mudança, especificações, decisões de design e tarefas de implementação.

## Documentação Obrigatória (Regra Zero)
- **Nunca** inicie o design, a arquitetura ou a codificação de uma funcionalidade sem antes ler detalhadamente `docs/padroes-de-projeto.md` (paleta Tailwind, tipografia, componentes Vue globais, `ProblemDetail` e rotas Spring Boot) e os artefatos da especificação correspondente no OpenSpec.
- Ao notar ambiguidades, requisitos incompletos ou decisões não documentadas, registre a lacuna antes de prosseguir.

## Stack Obrigatória e Arquitetura Base
- **Frontend (SPA):** Vue 3 (exclusivamente via Composition API e `<script setup lang="ts">`), TypeScript, Vite, Tailwind CSS e Pinia para gerenciamento de estado global e reatividade.
- **Backend (API RESTful):** Java 17+ com Spring Boot. Estruturação obrigatória em camadas: `Controller` (exposição e recepção de DTOs), `Service` (regras de negócio) e `Repository` (Spring Data JPA), com migrations via Flyway (`ddl-auto=validate`).
- **Banco de Dados Relacional:** PostgreSQL para armazenamento estruturado, cadastros, metadados e georreferenciamento (coordenadas GPS).
- **Armazenamento de Objetos (Blob):** MinIO (compatível com API S3) estritamente para armazenamento e recuperação de arquivos binários, como fotografias comprobatórias e laudos técnicos em PDF.
- **Restrições de Stack:** É estritamente proibido introduzir novas linguagens, frameworks de interface, bancos de dados em memória ou bibliotecas de terceiros sem autorização explícita e justificativa técnica prévia.
- **Segurança:** Nunca comite ou versione senhas, tokens de API, chaves privadas ou dados sensíveis reais. Utilize variáveis de ambiente e respeite os perfis e permissões configurados.

## Princípio de Trabalho: SDD com OpenSpec
Todas as funcionalidades da sprint devem ser desenvolvidas a partir de especificações no OpenSpec. Antes de implementar qualquer funcionalidade:
1. Identifique a especificação e a proposta de mudança correspondente no OpenSpec.
2. Analise os requisitos funcionais, histórias de usuário e critérios de aceitação.
3. Consulte o documento de design e as decisões arquiteturais.
4. Verifique as tarefas previstas e sua ordem de execução.
5. Identifique dependências com funcionalidades e telas existentes.
6. Implemente somente o escopo definido na especificação, sem antecipar itens de baixa prioridade ou de próximas etapas sem autorização.
7. Valide a implementação com base nos critérios de aceitação antes de avançar.

## Princípio de Trabalho: Design First
Priorize o desenvolvimento visual antes da integração completa da lógica. A interface ajuda a validar os fluxos e contratos de dados:
1. Entenda o objetivo do usuário alvo (Cidadão, Técnico/Gestor da AMMA, Parceiro/Bolsista IFCE, Administrador), os papéis envolvidos e o contexto operacional.
2. Defina a estrutura visual, a navegação, a hierarquia das informações e os estados da interface com base em `docs/padroes-de-projeto.md`.
3. Crie ou atualize a interface Vue (`.vue`) com dados demonstrativos claramente fictícios (ex.: `"Muda de Ipê Amarelo - Teste"`) para validar a experiência visual.
4. Revise responsividade, acessibilidade e consistência visual em dispositivos móveis e desktops.
5. Conecte interações, crie DTOs, implemente endpoints no Spring Boot, configure stores no Pinia e integre a persistência no PostgreSQL/MinIO.
6. Ao modificar telas ou fluxos existentes, preserve os padrões visuais, de nomenclatura e de navegação já estabelecidos.

## Prioridade: UI/UX e Acessibilidade
- **Clareza Operacional:** Projete fluxos rápidos e intuitivos para cidadãos e equipes técnicas/parceiras, reduzindo atrito e dependência de suporte.
- **Visibilidade de Sistema:** Torne evidentes os estados das solicitações e acordos (ex.: pendente, aprovado, rejeitado, retirado, em aberto, parcialmente doado, concluído), ações primárias e rastreabilidade.
- **Educação Ambiental:** Integre, quando couber, orientações curtas ou modais informativos sobre plantio correto e manejo de espécies nativas da Caatinga.
- **Consistência Visual e Semântica:** Mantenha coesão em cores, tipografia, espaçamentos, bordas, botões, formulários, tabelas e badges. Nunca comunique status ou gravidade apenas pela cor — use sempre texto explícito e/ou ícones.
- **Estados da Interface:** Implemente obrigatoriamente estados de carregamento (spinners/skeletons), vazio (empty states), erro de requisição, validação de formulário e confirmação de sucesso usando `AppToastContainer` e `AppConfirmModal`.
- **Responsividade Híbrida:** Adote abordagem *Mobile First* para fluxos públicos/cidadão e priorize desktops/tablets para painéis de gestão da AMMA e operação do IFCE.
- **Acessibilidade Base:** Use HTML semântico, `labels` associados a campos, foco visível no teclado e contraste adequado.
- **Prevenção de Erros:** Evite telas congestionadas, CTAs ambíguos e confirmações desnecessárias em ações triviais ou reversíveis.

## Escopo da Sprint 3 (Prioridade Máxima — OpenSpec)
As funcionalidades de prioridade máxima desta sprint, alinhadas ao documento de especificação de requisitos e às histórias de usuário, são:

- **SPEC-S3-01 — Gestão de Mudas e Baixa de Estoque pelo IFCE (`RF002`, `RF009` | Épico 01 e Épico 03):** Tela para operadores/bolsistas do laboratório do IFCE consultarem/validarem a retirada de mudas, registrarem a entrega física e darem baixa no estoque do viveiro municipal.
- **SPEC-S3-02 — Gerenciamento de Solicitações de Mudas na Tela da AMMA (`RF007`, `RF008` | Épico 03):** Tela administrativa da AMMA para analisar, aprovar ou rejeitar solicitações de mudas enviadas por cidadãos e gerenciar a emissão/autorização de retirada exclusivamente no fluxo da AMMA (o usuário cidadão não gera o documento/laudo de retirada).
- **SPEC-S3-03 — Termos de Compensação Ambiental (`RF017` | Épico 06):** Tela com formulário para cadastro/geração de Termo de Compromisso de compensação ambiental de empreendimentos (incluindo upload de PDF e quantitativos acordados) integrada a uma tela para visualizar e acompanhar o estado dos acordos (ex.: *Em aberto*, *Parcialmente doado*, *Totalmente doado/Fechado*).
- **SPEC-S3-04 — Substituição Botânica e Gestão de Supressão (`RF019`, `RF021` | Épico 07):** Tela para o cidadão solicitar substituição de espécie exótica invasora (ex.: Nim Indiano) por espécie nativa, e tela para a AMMA gerenciar esses pedidos, emitir/anexar documentos de supressão e acompanhar o andamento da retirada/substituição.

### Diretrizes de Implementação da Sprint
- As especificações da sprint (`SPEC-S3-01` a `SPEC-S3-04`) podem ser executadas em paralelo por diferentes membros da equipe, cada uma com sua própria proposta de mudança no OpenSpec e em branches/commits focados no seu escopo.
- Reutilizar componentes, serviços e padrões já existentes no projeto sem quebrar fluxos anteriores nem alterar código de outras specs sem necessidade.
- Ao depender de fluxos ou recursos compartilhados (como solicitações de muda, comprovantes ou estoque), combinar previamente os contratos/status para evitar conflitos de integração.
- Não antecipar funcionalidades de baixa prioridade (como plantio particular, adoção/certificado de árvore, alertas automáticos de vistorias, doações voluntárias de cidadãos ou cronograma de manutenção) sem autorização explícita.
- Validar cada especificação concluída com base nos seus critérios de aceitação.

## Domínio e Termos de Negócio
- **Solicitação de Mudas / Retirada:** Pedido do cidadão para recebimento de mudas, analisado/aprovado pela AMMA (com autorização/comprovante gerido no fluxo da AMMA, sem geração pelo usuário) e concluído com validação e baixa de estoque no laboratório do IFCE.
- **Compensação Ambiental:** Acordo de empreendimentos para compensação vegetal (com termo em PDF e controle de quantitativos exigidos vs. entregues/abatidos por status).
- **Substituição Botânica / Supressão:** Solicitação para remoção/substituição de espécie exótica invasora (como o *Nim Indiano*) por espécie nativa da Caatinga, vinculada a vistoria, documento de supressão e acompanhamento de retirada pela AMMA.
- **Inventário Arbóreo e Garajau:** Cadastro técnico e georreferenciado (latitude, longitude, DAP, altura, fitossanidade) das árvores públicas, incluindo estruturas físicas de proteção (*garajaus*) para mudas recém-plantadas.
- **Limitação de Escopo:** Não invente regras operacionais, perfis de acesso fora do escopo (`Administrador`, `Técnico/Funcionário`, `Parceiro/Bolsista`, `Cidadão`) ou políticas não especificadas. Ao encontrar lacunas, sinalize e adote a opção mais simples e restritiva.

## Organização e Qualidade Técnica
- **Validação de Fronteira:** Valide e normalize entradas no back-end (Spring Boot) com `jakarta.validation` além das validações de formulário no front-end.
- **Consultas Seguras:** Use consultas parametrizadas no JPA/Hibernate; nunca concatene strings para montar queries.
- **Uploads Restritos:** Valide extensão, MIME type e tamanho máximo de arquivos (PDFs de termos/laudos, fotos) na `Controller` antes de enviar ao MinIO.
- **Tratamento de Exceções Global:** Use `@ControllerAdvice` retornando `ProblemDetail` (RFC 7807), sem capturar `Exception` genérica com `printStackTrace` ou vazar mensagens internas brutas ao cliente. O front-end deve interceptar e exibir erros de forma amigável.
- **Logs Seguros:** Nunca registre senhas, tokens JWT, chaves de API, e-mails ou CPFs em logs.
- **Commits e Escopo:** Mantenha mudanças focadas na especificação/tarefa atual e em commits granulares, evitando refatorações não solicitadas em outros módulos.
- **Código Limpo:** Escreva código expressivo sem comentários óbvios; use comentários apenas para regras de negócio não óbvias ou workarounds necessários.

## Verificação Antes de Concluir
Antes de finalizar uma tarefa ou especificação:
1. Confira os requisitos, histórias de usuário e critérios de aceitação da spec no OpenSpec.
2. Verifique o comportamento em telas mobile e desktop.
3. Teste os estados de carregamento, vazio, erro, validação e sucesso.
4. Valide a integração com a API e a persistência no PostgreSQL e/ou MinIO (caminhos de sucesso e falha).
5. Confira tipagem TypeScript e compilação do back-end sem erros.
6. Garanta que funcionalidades e rotas existentes continuem funcionando.
7. Na resposta final, informe objetivamente o que foi criado ou alterado, o que foi validado e eventuais pendências ou limitações.

## Interface do sistema

- Utilizar como referência visual o arquivo `docs/interfaces/image.png`, caso esteja disponível.
- Preservar a identidade visual definida para o projeto.
- Priorizar componentes modernos, organizados e responsivos.
- Utilizar a abordagem Design First antes de conectar dados reais.
- Não modificar a identidade visual sem solicitação explícita.

## Regra Principal
**Especificar antes de implementar. Projetar antes de programar. Validar antes de concluir.**

Toda implementação deve seguir o OpenSpec e os padrões do projeto, mantendo a qualidade visual, a segurança e a clareza técnica do Arborize.

## Como trabalhar neste repositório (Spec-Driven Development)
1. Antes de iniciar qualquer codificação, consulte as especificações, propostas e tarefas dentro da pasta `openspec/`, além de referências visuais disponíveis em `docs/interface/<funcionalidade>` e as diretrizes deste `AGENTS.md`.
2. Analise os artefatos da especificação correspondente em `openspec/` para entender o escopo, os requisitos, os critérios de aceitação, as decisões de design e as tarefas previstas antes de alterar o código.
3. É expressamente proibido implementar rotas, criar tabelas ou colunas no banco de dados, ou construir fluxos de interface que não constem na especificação da funcionalidade sem autorização explícita.
4. Execute as tarefas na ordem prevista dentro da especificação escolhida em `openspec/`, respeitando os agrupamentos (ex.: frontend vs. backend) e evitando bloqueios desnecessários entre integrantes que estejam trabalhando em especificações distintas em paralelo.
5. Respeite a organização física do repositório: o código do front-end fica em `frontend/src/` (ou `src/` conforme a estrutura do projeto) e o código do back-end Java/Spring Boot fica em `backend/arborize/src/main/java/`, mantendo `pom.xml`, `package.json` e `.gitignore` limpos.
6. Marque as tarefas como concluídas apenas quando os respectivos critérios de aceitação puderem ser validados na prática (incluindo estados da interface, integração com a API/persistência quando aplicável e ausência de erros de compilação/tipagem).