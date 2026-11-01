# Instruções do projeto — Arborize

## Objetivo
Construir o **Arborize**, um sistema unificado para automatizar, rastrear e otimizar os processos ambientais da Autarquia Municipal de Meio Ambiente (AMMA) de Quixadá. 
O sistema opera em duas frentes:
1. **Interface Pública:** Para os cidadãos solicitarem mudas (ex: via Eco Ponto com troca de garrafas PET), registrarem plantios particulares, solicitarem substituição de espécies invasoras, participarem de mutirões e acessarem o acervo de educação ambiental.
2. **Painel de Gestão (Backoffice):** Para a Defesa Civil e técnicos da AMMA administrarem o inventário arbóreo georreferenciado, controlarem o estoque do viveiro municipal, emitirem e gerenciarem laudos agronômicos e monitorarem compensações ambientais.

## Documentação Obrigatória (Regra Zero)
**Nunca** inicie o design, a arquitetura ou a codificação de uma funcionalidade sem antes ler detalhadamente o arquivo `docs/padroes-de-projeto.md`. Este documento atua como a única fonte da verdade e substitui protótipos visuais externos, ditando as regras exatas para o uso da paleta de cores (Tailwind), tipografia, componentes Vue globais, tratamento de erros via `ProblemDetail` e estruturação de rotas no Spring Boot.

## Stack Obrigatória e Arquitetura Base
- **Frontend (SPA):** Vue 3 (exclusivamente via Composition API e `<script setup lang="ts">`), TypeScript, Vite, Tailwind CSS e Pinia para gerenciamento de estado global e reatividade.
- **Backend (API RESTful):** Java 17+ com Spring Boot. Estruturação obrigatória em camadas: `Controller` (exposição e recepção de DTOs), `Service` (regras de negócio) e `Repository` (Spring Data JPA).
- **Banco de Dados Relacional:** PostgreSQL para todo o armazenamento estruturado, cadastros, metadados e georreferenciamento (coordenadas GPS).
- **Armazenamento de Objetos (Blob):** MinIO (compatível com API S3) estritamente para armazenamento e recuperação de arquivos binários, como fotografias comprobatórias de plantio e laudos técnicos em PDF.
- **Restrições de Stack:** É estritamente proibido introduzir novas linguagens, frameworks de interface, bancos de dados em memória (Redis) ou bibliotecas de terceiros sem autorização explícita e justificativa técnica prévia.
- **Segurança:** Nunca comite ou versione senhas, tokens de API, chaves privadas ou dados sensíveis reais. Utilize variáveis de ambiente.

## Princípio de Trabalho: Design First
Trabalhe de forma **design first**. A interface dita os contratos de dados, e não o contrário. Siga a ordem exata:
1. Entenda profundamente o objetivo do usuário alvo (Cidadão, Técnico da AMMA, Parceiro IFCE, Administrador), os papéis envolvidos e o contexto de uso municipal.
2. Defina a estrutura visual, a hierarquia da informação, os fluxos de navegação e os estados da interface, baseando-se nas regras de `docs/padroes-de-projeto.md`.
3. Crie ou atualize a interface Vue (.vue) utilizando dados demonstrativos e estáticos (mockados) no setup do script, priorizando a entrega de uma experiência visual completa e coerente.
4. Revise exaustivamente a interface em dispositivos móveis e desktops, ajustando problemas de clareza, alinhamento, consistência e acessibilidade.
5. Somente após a aprovação da interface estática, conecte interações, crie os DTOs correspondentes, implemente os endpoints no Spring Boot, configure as stores do Pinia e estabeleça a persistência no PostgreSQL/MinIO.
6. Em manutenções de telas existentes, preserve rigorosamente os padrões visuais, de nomenclatura e de navegação já estabelecidos.

## Prioridade: UI/UX e Acessibilidade
- **Clareza Operacional:** Projete fluxos rápidos e intuitivos para cidadãos leigos e equipes técnicas, garantindo que atividades burocráticas sejam resolvidas sem necessidade de suporte em TI.
- **Visibilidade de Sistema:** Torne evidentes o status das solicitações (ex: pendente, em análise, aprovado, recusado), as ações primárias da tela e a localização precisa em mapas interativos.
- **Educação Ambiental:** Integre, sempre que couber, textos curtos ou modais informativos com um passo a passo educativo para a população sobre o plantio correto e o manejo de mudas urbanas.
- **Consistência Visual:** Mantenha coesão estrita nas cores, famílias tipográficas, espaçamentos, raios de borda, botões, formulários, tabelas, filtros e badges.
- **Semântica de Cores:** Use cores de status sempre acompanhadas de texto explícito ou ícones de apoio. Nunca comunique gravidade, sucesso ou erro exclusivamente pela cor (foco em daltonismo e acessibilidade).
- **Estados da Interface:** Projete e codifique obrigatoriamente estados visuais para: carregamento em andamento (spinners/skeletons), ausência de dados (empty states criativos), erros de requisição, confirmação de sucesso, validação de formulários e conteúdo indisponível. Utilize `AppToastContainer` e `AppConfirmModal`.
- **Responsividade Híbrida:** Interfaces voltadas ao público geral devem priorizar o uso mobile (Mobile First). Telas voltadas à operação técnica (painel gerencial, laudos, aprovações) devem priorizar desktops e tablets.
- **Acessibilidade Base:** Utilize marcação HTML semântica, rótulos (labels) explicitamente associados aos campos de formulário, contornos de foco (focus ring) visíveis na navegação por teclado e contraste adequado de leitura.
- **Prevenção de Erros:** Evite telas congestionadas, chamadas para ação (CTAs) ambíguas, repetição desnecessária de informações e modais de confirmação para ações reversíveis ou triviais.
- **Mock de Dados:** Ao utilizar dados demonstrativos na fase de UI, certifique-se de que sejam claramente fictícios (ex: "Muda de Ipê Amarelo - Teste").

## Domínio e Termos de Negócio
- **Ocorrência / Solicitação:** Registro dinâmico que pode representar um pedido de doação de muda via Eco Ponto, um registro civil de plantio particular, ou um chamado técnico para substituição de árvore com risco de queda/espécie invasora.
- **Inventário Arbóreo:** Banco de dados técnico e georreferenciado (coordenadas de latitude/longitude) do patrimônio verde do município. Cada registro contém atributos vitais como espécie botânica, DAP (Diâmetro à Altura do Peito em cm), altura estimada em metros e estado fitossanitário.
- **Nim Indiano (Azadirachta indica):** Espécie exótica invasora severa que compromete a biodiversidade local e destrói infraestruturas subterrâneas. É alvo prioritário de protocolos de supressão e substituição por espécies nativas do bioma Caatinga.
- **Garajau:** Estrutura física de madeira ou metal (grade) utilizada para a proteção de mudas recém-plantadas no passeio público contra depredação, pisoteio e trânsito animal.
- **Compensação Ambiental / Laudos:** Módulo documental onde o sistema hospeda Planos de Flora, Inventários Florestais e Termos de Compromisso firmados com empreendedores. Exige transparência total no acompanhamento de quantitativos exigidos vs. executados.
- **Limitação de Escopo:** Não invente regras operacionais, categorias taxonômicas, permissões de usuários (o sistema possui estritamente: Administrador, Técnico, Parceiro e Cidadão) ou políticas de retenção que não estejam formalmente definidas nas especificações. Ao notar lacunas, sinalize no prompt e adote a abordagem mais simples e restritiva.

## Organização e Qualidade Técnica
- **Validação de Fronteira:** Valide e normalize todas as entradas diretamente no servidor (Spring Boot) usando `jakarta.validation`. Não confie exclusivamente na validação do Vue.js.
- **Consultas Seguras:** Mantenha consultas estritamente parametrizadas no JPA/Hibernate. Não construa queries concatenando strings.
- **Uploads Restritos:** Arquivos anexos (fotos de plantio e laudos PDF) devem ter sua extensão, MIME type e tamanho máximo rigorosamente validados na Controller antes de serem despachados para o MinIO.
- **Tratamento de Exceções Global:** Utilize um `@ControllerAdvice` retornando `ProblemDetail` (RFC 7807) padronizado. O frontend (Axios) deve interceptar esses retornos e alimentar a UI de forma elegante (sem quebrar a tela ou expor stacktraces).
- **Dados Residuais em Logs:** É estritamente proibido imprimir (logger/system.out) senhas, tokens JWT, chaves de API, e-mails ou CPFs de usuários em logs da aplicação.
- **Escopo Atômico de PRs/Commits:** Faça alterações focadas apenas no que foi solicitado. Não refatore, reestruture ou formate códigos de módulos que não pertencem à tarefa atual.
- **Documentação de Código:** Não polua o código-fonte com comentários explicando lógica básica (ex: `// salva o usuário no banco`). O código deve ser expressivo. Use comentários apenas para documentar workarounds complexos ou regras de negócio contra-intuitivas.

## Verificação Antes de Concluir Tarefas
- **Cross-browser / Cross-device:** Certifique-se de que a interface se ajusta e não quebra layouts em resoluções clássicas de desktop e smartphones.
- **Cenários de Exceção:** Simule ou valide mentalmente se a interface reage bem quando a API retorna erro 500, erro 400 (Bad Request com validação), ou quando não há internet.
- **Testes de Integração Backend:** Valide os caminhos felizes (Sucesso) e caminhos tristes (Falha) na persistência de dados no PostgreSQL e no sucesso do upload no MinIO.
- **Transparência:** Em sua resposta de entrega, liste objetivamente o que foi criado, modificado e quais pendências técnicas ou bloqueios ainda precisam ser resolvidos em iterações futuras.

## Como trabalhar neste repositório (Spec-Driven Development)
1. Antes de iniciar qualquer codificação, abra e leia o documento `docs/padroes-de-projeto.md` para entender as fundações arquiteturais.
2. Leia todos os arquivos de especificação localizados em `specs/<funcionalidade>/` (`requirements.md`, `design.md` e `tasks.md`).
3. É expressamente proibido implementar rotas, criar tabelas no banco de dados ou construir fluxos de interface que não constem na especificação.
4. Execute as tarefas estritamente na ordem numérica e respeite os agrupamentos (Frontend vs Backend) estabelecidos no `tasks.md`.
5. Estruturação física: O código Java da API REST encontra-se dentro de `backend/src/main/java/` e a SPA Vue encontra-se dentro de `frontend/src/`. Mantenha as dependências nos respectivos arquivos `pom.xml` e `package.json` rigorosamente limpas.
6. Marque as tarefas como concluídas apenas quando todos os Critérios de Aceitação BDD (`requirements.md`) puderem ser validados na prática.