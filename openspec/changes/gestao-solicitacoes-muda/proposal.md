# Proposal

## Why

A SPEC-S3-02 do Épico 03 cobre RF007 e RF008. Hoje a solicitação pública já registra o pedido e calcula a contrapartida, mas o painel ainda exibe um placeholder e o acompanhamento usa uma linha do tempo fixa.

## What Changes

- Implementar as telas “Solicitações de mudas”, “Detalhes da solicitação” e “Registrar recebimento”, seguindo as referências 01 a 03. As imagens 04 e 05 dão contexto ao acompanhamento já existente.
- A AMMA recebe as garrafas PET, aprova o pedido e emite o documento de retirada para entregar ao cidadão. O IFCE consulta a solicitação e recebe o documento apresentado pelo cidadão. A baixa de estoque pertence à SPEC-S3-01. As restrições de acesso do bolsista pertencem à frente responsável pelas permissões.
- Usar o protocolo existente, como `SOL-2026-0011`, como identificador de retirada. O número `#000123` é ilustrativo. QR code, segundo identificador e tabela de comprovantes ficam fora do escopo. A AMMA pode baixar o PDF com esse protocolo após a aprovação. O cidadão consulta o status e apresenta o documento emitido pela AMMA no IFCE.
- Compartilhar os estados `PENDENTE_RECICLAVEIS`, `APROVADA`, `RETIRADA_CONCLUIDA` e `REJEITADA` com a SPEC-S3-01. A tela de validação e entrega física e a baixa de estoque pertencem à SPEC-S3-01.
- Gravar o local de plantio informado no assistente, mantendo compatibilidade com pedidos antigos e com o formulário existente. Atualizar a linha do tempo conforme o status consultado por protocolo e CPF.
- Adotar como proposta inicial a aprovação somente quando o total recebido atingir o esperado e o pedido estiver pendente. Rejeição exige motivo. A confirmação da regra sobre quantidade está em aberto.

## Capabilities

### New Capabilities

- `solicitacoes-muda` — gestão de pedidos, conferência de PET, autorização de retirada por protocolo e acompanhamento do estado real.

### Modified Capabilities

## Impact

- Painel autenticado, rotas de solicitações e item de menu existente. Formulário e acompanhamento públicos recebem ajustes pontuais.
- Contratos administrativos, validações, enum de status e campos adicionais na entidade JPA já existente. O schema PostgreSQL será gerenciado por migrações versionadas do Flyway, com JPA em `ddl-auto=validate`, conforme o AGENTS.md.
- O painel omite CPF e telefone. Os dados públicos continuam limitados à consulta existente. Laudos, substituição botânica e telas da SPEC-S3-01 ficam fora desta mudança.
