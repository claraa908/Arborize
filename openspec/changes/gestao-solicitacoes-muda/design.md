# Design

## Context

`SolicitacoesView.vue` é um placeholder na rota autenticada. O backend já cria a solicitação com `PENDENTE_RECICLAVEIS`, persiste a contrapartida e monta o protocolo a partir do ano de criação e do ID. A consulta por protocolo e CPF já retorna o status persistido, mas `AcompanharSolicitacaoView.vue` usa passos fixos. O assistente guarda a resposta `local_plantio` no Pinia, hoje ausente do envio e da entidade. A configuração de segurança atual tem uma regra ampla que libera `/api/**`.

## Goals / Non-Goals

- Entregar a operação em três telas e reutilizar o contrato da solicitação persistida, os papéis e componentes existentes.
- Excluir desse trabalho a interface de entrega física e a baixa de estoque (SPEC-S3-01), laudos e substituição botânica (SPEC-S3-04), QR code e novas telas públicas.

## Decisions

### Painel e navegação

- Usar o `AuthenticatedLayout` e o invólucro de `InventarioView.vue` como referência para `min-w-0`, `px-6 md:px-10`, espaçamento e responsividade. Usar `AppBreadcrumb` nas três telas e `activeMenu: 'solicitacoes'` nas rotas filhas. Aplicar os tokens botânicos, League Spartan, `rounded-box`, `rounded-btn` e tokens semânticos definidos em `docs/padroes-de-projeto.md`.
- Manter o item de “Solicitações” no array `menuItems`. Em `visibleMenuItems`, aplicar `auth.canAccessManagement` somente a esse item. O cálculo atual só acrescenta “Equipe do Laboratório” para `canManageIfceTeam`. Reutilizar a exigência de acesso à gestão do router e usar os papéis AMMA para a ação de rejeitar. Assim, os aliases IFCE já reconhecidos por `canAccessManagement` não dependem do acesso à gestão de equipe.
- Tela 01 com busca por protocolo, ordenação “Mais recentes” e cartões horizontais empilháveis. Tela 02 em duas colunas com “Dados do pedido” e “Contrapartida”; a rejeição fica no detalhe e pede motivo. Tela 03 em duas colunas com “Conferência da entrega” e resumo; quantidade esperada e material são de leitura. Usar “Material” como rótulo do campo que contém “Garrafas PET”, corrigindo a duplicação do rótulo no protótipo. Em telas estreitas, empilhar as colunas e preservar a ordem dos controles.
- Usar `AppConfirmModal` para confirmar a decisão, inclusive a rejeição após validar o motivo. Mostrar sucesso via `AppToastContainer`, manter os valores em falha, bloquear envios simultâneos na UI e mostrar erro também junto ao campo ou à região afetada. Associar controles a rótulos, erros a `aria-describedby`, e preservar foco visível e selos textuais. O cartão próprio preserva a estrutura da tela 01 sem alterar `AppDataTable`.

### API de gestão

Reutilizar Controller → Service → Repository, Bean Validation, DTOs e `ProblemDetail` de `SolicitacaoMuda*`, seguindo o recorte `/gestao` e a organização de exceções da substituição botânica. Não alterar as classes de substituição.

| Método e rota | Contrato |
| --- | --- |
| `GET /api/solicitacoes-muda/gestao` | Página de cartões. Aceita `protocolo` exato e opcional, `page` e `size`. Ordena por criação decrescente e ID decrescente no empate. |
| `GET /api/solicitacoes-muda/gestao/{protocolo}` | Solicitação com destino, local opcional, contrapartida esperada e recebida quando disponível, além do motivo quando rejeitada. |
| `POST /api/solicitacoes-muda/gestao/{protocolo}/recebimento` | Recebe `quantidadeRecebida` e devolve o detalhe aprovado. |
| `POST /api/solicitacoes-muda/gestao/{protocolo}/rejeicao` | Recebe `motivo` e devolve o detalhe rejeitado. Exclusivo da AMMA. |
| `GET /api/solicitacoes-muda/gestao/{protocolo}/documento` | Emite o PDF após a aprovação, a partir da solicitação persistida. |

- Preservar as permissões existentes da gestão de solicitações. A rota de PDF reutiliza a autorização de gestão. A revisão dos acessos por papel pertence à frente responsável pelas permissões.
- Usar DTOs administrativos próprios, sem CPF e telefone. Respostas públicas preservam o formato e passam a retornar os valores previstos do enum; o motivo de rejeição é de gestão. Destino é composto dos campos de endereço já persistidos. Nenhum dado pessoal deve ser escrito em logs.
- Extrair do mapeamento a formatação única do protocolo e reaproveitar a resolução por ID com validação do protocolo completo para consulta e busca exata. Usar consultas parametrizadas e paginação JPA. Protocolos inválidos ou desconhecidos devolvem vazio na busca e 404 no detalhe.

### Persistência e transições

- Acrescentar ao enum `APROVADA`, `RETIRADA_CONCLUIDA` e `REJEITADA`. Acrescentar na entidade já existente os campos opcionais `localPlantio`, `quantidadeRecebida` e `motivoRejeicao`. O local é um enum gravado como texto com `CALCADA` e `QUINTAL`; legado nulo aparece como “Não informado”. O motivo é aparado, obrigatório na rejeição e limitado a 255 caracteres. A contagem recebida é um inteiro não negativo e conferido contra a contrapartida já persistida.
- Executar aprovação e rejeição sob transação, com leitura do pedido para escrita protegida por bloqueio pessimista no Repository e rechecagem do status. A operação que chegar depois recebe 409. O recebimento e a aprovação são uma única gravação. A SPEC-S3-01 deve usar o mesmo protocolo e proteger a transição `APROVADA` → `RETIRADA_CONCLUIDA` de entregas duplicadas.

### Integrações públicas

- A solicitação já nasce de `RecomendacoesView` → formulário. Ler `respostas.local_plantio` da store do assistente no envio de `SolicitarMudaView`, passar somente `CALCADA` ou `QUINTAL` no payload e aceitar ausência em acessos diretos sem contexto do quiz. A opção evita transportar a resposta por novas rotas e alterar o assistente. Validar no DTO quando presente.
- Derivar a linha do tempo e o selo existentes do status retornado pelo backend. Para `APROVADA`, orientar o cidadão a apresentar no IFCE o documento emitido pela AMMA. As telas públicas 04 e 05 orientam o significado do estado. A AMMA baixa o PDF por uma rota administrativa, com geração em memória via PDFBox a partir dos dados persistidos.

## Risks / Trade-offs

As edições em arquivos existentes estão delimitadas no inventário ao fim do [delta da spec](specs/solicitacoes-muda/spec.md).

- Aprovações concorrentes ou repetidas → proteger a atualização no banco e devolver conflito com orientação para recarregar.
- Registros antigos sem local → manter colunas opcionais e exibir ausência explicitamente.
- Tela e API podem divergir nas permissões → testar cada papel e alias, acesso direto e rejeição sem permissão.
- Acesso direto ao formulário sem a store do assistente → manter o envio disponível com local ausente.

## Migration Plan

1. Reutilizar a configuração e a migração inicial compartilhadas previstas em `fluxo-substituicao-botanica`. Se ainda não estiverem disponíveis na integração, coordenar uma única migração inicial que represente o schema vigente e um procedimento explícito de baseline para bases existentes, após conferir o schema real. Validar o caminho de uma base vazia e o de uma base já inicializada, preservando os dados.
2. Adicionar uma migração Flyway versionada para as colunas opcionais de `solicitacoes_muda` e ajustar eventual restrição de status aos valores do enum. Depois de verificar a migração nos dois caminhos, usar `ddl-auto=validate` nos perfis suportados e conferir a inicialização do backend. Entregar backend e frontend juntos.
3. Validar a leitura de um pedido legado, o status atualizado via consulta pública e a rejeição concorrente. Depois de gravar os novos estados, uma versão anterior do backend não consegue lê-los. Em caso de rollback, coordenar a compatibilidade do enum com os registros já persistidos.

Esta mudança usa a baseline compartilhada `V1__baseline.sql` da integração e acrescenta a migração `V20261123_01`. Para bases existentes, seguir o procedimento de conferência do [guia de baseline do projeto](../../../docs/database/flyway-baseline.md). Um banco cujo histórico registra uma V2 diferente da V2 compartilhada precisa de reconciliação antes da migração.

## Open Questions

- Confirmar a política de quantidade após revisão operacional. O plano recusa entregas abaixo do esperado e registra o total efetivamente recebido quando igual ou superior. O PDF não fixa o tratamento de divergências de quantidade.
