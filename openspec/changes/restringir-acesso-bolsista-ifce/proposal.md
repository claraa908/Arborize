# Proposal

## Why

Atualmente, os perfis de bolsista do IFCE herdam acesso às telas administrativas e a diversas APIs do Arborize, embora sua operação deva se limitar ao Viveiro e ao próprio perfil. A mudança alinha a navegação e a autorização no servidor a esse escopo, inclusive quando alguém tenta abrir uma URL ou endpoint diretamente.

## What Changes

- Restringir o papel canônico `ROLE_BOLSISTA_IFCE` ao fluxo do Viveiro e ao perfil próprio.
- Após o login, direcionar para a tela do Viveiro. Em uma sessão autenticada, permitir a página pública inicial, os fluxos públicos de Arborização e Biblioteca Digital e a tela de perfil próprio; as demais rotas continuam redirecionando para o Viveiro. Manter acessíveis as subpáginas necessárias do Viveiro, como cadastro, detalhes e edição de espécies.
- Exibir somente a opção Viveiro no menu lateral, manter no cabeçalho o atalho para a página pública, o acesso ao perfil próprio e o encerramento da sessão.
- Negar no backend, com resposta de acesso proibido, chamadas autenticadas do bolsista a endpoints fora do Viveiro, do perfil próprio e dos endpoints públicos explicitamente necessários à Arborização e à Biblioteca Digital.
- Preservar a navegação e as permissões atuais dos demais perfis e o acesso público de visitantes sem sessão.

## Capabilities

### New Capabilities

- `role-based-access`: Define o acesso por papel às páginas e APIs, incluindo a restrição do bolsista do IFCE ao Viveiro e ao próprio perfil.

### Modified Capabilities

Nenhuma. O diretório de especificações não contém capacidades existentes.

## Impact

- Frontend: `authStore`, guarda e destino pós-login do Vue Router, menu lateral e atalhos do cabeçalho, incluindo acesso ao perfil institucional próprio.
- Backend: filtro de autorização para que uma sessão de bolsista alcance `/especies/**`, os endpoints do próprio perfil e somente os endpoints públicos necessários à Arborização e à Biblioteca Digital; login e preflight continuam necessários ao protocolo.
- A mudança não cria rotas de negócio, tabelas, colunas ou dependências. Endpoints de Viveiro adicionados por mudanças futuras deverão ser autorizados explicitamente conforme seus contratos.
- Visitantes sem sessão e perfis diferentes de bolsista continuam sujeitos às regras atuais.
