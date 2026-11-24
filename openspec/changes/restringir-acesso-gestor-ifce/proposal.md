# Proposal

## Why

O perfil `ROLE_GESTOR_IFCE` atualmente herda acesso a telas e APIs administrativas além das necessárias à gestão do viveiro e da equipe de bolsistas. A restrição por rota e endpoint reduz esse acesso ao escopo operacional do gestor, inclusive em tentativas por URL direta ou chamadas à API.

## What Changes

- Limitar `ROLE_GESTOR_IFCE` a todo o site público (home, Arborização e Biblioteca Digital), às páginas do Viveiro, à tela Equipe do Laboratório IFCE e ao próprio perfil.
- Direcionar o gestor para Equipe do IFCE após o login e redirecionar para essa tela tentativas de abrir outras páginas não permitidas.
- Exibir no menu lateral somente Viveiro e Equipe do Laboratório; manter no cabeçalho os atalhos para a home, perfil próprio e saída.
- Restringir as APIs do gestor às operações do Viveiro, à consulta/cadastro de bolsistas e aos endpoints de leitura/edição do próprio perfil e senha.
- Aplicar a restrição ao papel canônico `ROLE_GESTOR_IFCE`, incluindo contas antigas `ROLE_IFCE_GESTOR` após sua conversão; preservar os demais papéis canônicos.

## Capabilities

### New Capabilities

- `role-based-access`: Define as rotas e APIs permitidas ao gestor IFCE, incluindo sua área de equipe, Viveiro, todo o site público e perfil próprio.

### Modified Capabilities

Nenhuma. O inventário principal de especificações ainda não contém capacidades publicadas.

## Impact

- Frontend: destino pós-login, guarda de rotas, menu lateral e cabeçalho do layout autenticado; reutiliza a tela existente de Equipe do IFCE e o perfil institucional.
- Backend: filtro de autorização após JWT para as rotas e APIs do gestor; mantém os contratos existentes de APIs públicas, `/ifce/bolsistas`, `/especies/**`, `/api/viveiro/movimentacoes/**` e `/usuarios/me`.
- Não adiciona tabelas, colunas, dependências ou novos contratos de negócio.
- Visitantes, bolsistas, funcionários da AMMA e administradores conservam suas permissões atuais; contas convertidas para gestor seguem a política de `ROLE_GESTOR_IFCE`.
