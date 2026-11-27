# Proposal

## Why

O fluxo de Arborização permite iniciar e preencher o quiz sem uma conta autenticada. Exigir login ou cadastro antes de entrar no fluxo permite associar a experiência a uma sessão e impede que rotas diretas contornem a solicitação de autenticação.

## What Changes

- Permitir navegação pública em `/arborizacao`, no assistente de plantio (`/arborizacao/assistente`) e nas recomendações (`/arborizacao/recomendacoes`).
- Exigir autenticação ao clicar em "Fazer requerimento" (`/arborizacao/substituicao-nim`), ao selecionar uma muda recomendada (`/arborizacao/solicitar-muda`), na conversão (`/arborizacao/conversao`) e no acompanhamento; exibir modal com aviso contextual, efetuar login automático após o cadastro e retomar imediatamente o fluxo solicitado preservando os parâmetros (como a espécie escolhida).
- Exigir sessão válida no backend em `POST /api/substituicoes-botanicas`, `GET /api/solicitacoes-muda/conversao` e `POST /api/solicitacoes-muda`, mantendo `POST /api/assistente-plantio/recomendar` acessível publicamente para o quiz.
- Preservar o comportamento de destino pós-login para acessos sem uma rota protegida pendente e manter as permissões atuais das outras áreas públicas.

## Capabilities

### New Capabilities

- `arborizacao-authentication`: Define a autenticação necessária para acessar o fluxo público de Arborização e a retomada segura desse fluxo após login ou cadastro.

### Modified Capabilities

Nenhuma. Ainda não há especificações base publicadas no inventário do OpenSpec; as permissões atuais por perfil são documentadas em mudanças anteriores.

## Impact

- Frontend: metadados/guarda das rotas de Arborização, abertura e alternância dos modais de login e cadastro, retomada da rota solicitada e testes de navegação.
- Backend: autorização das APIs de recomendação, conversão e criação de solicitação para exigir uma sessão válida.
- Mantém os fluxos atuais de Viveiro, Biblioteca Digital e demais páginas públicas sem exigir login.
- Não prevê novos campos, tabelas ou dependências.
