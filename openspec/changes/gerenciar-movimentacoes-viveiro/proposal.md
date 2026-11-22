# Proposal

## Why

O Viveiro agora mantém o estoque por espécie, mas não registra como esse saldo muda nem oferece histórico para consulta e correção. Esta mudança cria o fluxo operacional de entradas, saídas para plantio e perdas, atualizando o saldo e os indicadores mensais a partir dos registros.

## What Changes

- Criar e consultar movimentações dos tipos **Entrada / Germinação**, **Saída para Plantio** e **Baixa por Perda**, vinculadas diretamente a uma espécie; não haverá seleção ou cadastro de lote.
- Substituir a ação futura do painel por um fluxo ativo, com abas para espécies e movimentações, formulário seguindo `docs/interface/movimentacao-muda1.png` e `movimentacao-muda2.png`, tabela de histórico e edição de registros.
- Atualizar o estoque da espécie na mesma operação da movimentação: entradas somam; saídas e perdas subtraem. Impedir saldo negativo e preservar registro e saldo quando a operação falhar.
- Atualizar os cartões de saídas e perdas do mês a partir das movimentações do mês corrente. Manter “Em germinação” como `—`, pois o sistema não registra a conclusão dessa etapa nem acompanha lotes.
- Permitir comprovante opcional conforme a referência (PDF, PNG ou JPG, até 10 MB), com validação no cliente e servidor e armazenamento privado no MinIO.
- Restringir consulta e escrita às mesmas permissões de gestão já aplicadas ao Viveiro, incluindo os dois papéis de bolsista do IFCE; manter as demais autorizações existentes.
- Adotar Flyway e `ddl-auto=validate`, incluindo baseline do esquema atual antes da migration da nova tabela, conforme `AGENTS.md`.

## Capabilities

### New Capabilities

- `viveiro-movements`: registro, consulta e edição de movimentações, atualização consistente do estoque, indicadores mensais, comprovantes e autorização.

### Modified Capabilities

Nenhuma capability base foi encontrada em `openspec/specs/`. Esta mudança acrescenta um delta para `viveiro-movements`; a tela se integra ao comportamento de Viveiro planejado em `redesenhar-painel-viveiro`.

## Impact

- Frontend: painel, navegação interna, formulário, tabela/histórico, tipos, serviço e store do Viveiro.
- Backend: novo recurso REST, DTOs, entidade/repositório/serviço, validação transacional e bloqueio concorrente do saldo de espécie.
- Persistência: adoção de Flyway e baseline sem alteração de dados existentes; migration da movimentação e vínculo opcional com comprovante.
- Arquivos: validação de MIME/extensão/tamanho, associação e ciclo de vida no MinIO.
- Segurança: novas rotas autenticadas e allowlist explícita para bolsistas no filtro `IfceBolsistaAccessFilter`; rotas não devem herdar o `permitAll` amplo de `/api/**`.
- Integração com a proposta em andamento `redesenhar-painel-viveiro`: habilitar “Registrar Movimentação” quando o fluxo estiver disponível e alimentar as métricas que passam a ter fonte.
