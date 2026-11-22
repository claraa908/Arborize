# Proposal

## Why

Atualmente, os perfis de bolsista do IFCE herdam acesso às telas administrativas e a diversas APIs do Arborize, embora sua operação deva se limitar ao Viveiro. A mudança alinha a navegação e a autorização no servidor a esse escopo, inclusive quando alguém tenta abrir uma URL ou endpoint diretamente.

## What Changes

- Restringir os papéis `ROLE_BOLSISTA_IFCE` e `ROLE_IFCE_BOLSISTA` ao fluxo do Viveiro, mantendo ambos como aliases do perfil de bolsista já reconhecidos pelo sistema.
- Após o login, direcionar para a tela do Viveiro. Em uma sessão autenticada, permitir a página pública inicial e os fluxos públicos de Arborização e Biblioteca Digital; as demais rotas fora do Viveiro continuam redirecionando para o Viveiro. Manter acessíveis as subpáginas necessárias da aba, como cadastro, detalhes e edição de espécies.
- Exibir somente a opção Viveiro no menu lateral do bolsista, manter o atalho de cabeçalho para a página pública inicial e ocultar os demais atalhos. Manter disponível o encerramento da sessão.
- Negar no backend, com resposta de acesso proibido, chamadas autenticadas do bolsista a endpoints fora do Viveiro e dos endpoints públicos explicitamente necessários à Arborização e à Biblioteca Digital.
- Preservar a navegação e as permissões atuais dos demais perfis e o acesso público de visitantes sem sessão.

## Capabilities

### New Capabilities

- `role-based-access`: Define o acesso por papel às páginas e APIs, incluindo a restrição do perfil de bolsista do IFCE ao Viveiro.

### Modified Capabilities

Nenhuma. O diretório de especificações não contém capacidades existentes.

## Impact

- Frontend: `authStore`, guarda e destino pós-login do Vue Router, menu lateral e atalhos do cabeçalho.
- Backend: filtro de autorização para que uma sessão de bolsista alcance `/especies/**` e somente os endpoints públicos necessários à Arborização e à Biblioteca Digital; login e preflight continuam necessários ao protocolo.
- A mudança não cria rotas de negócio, tabelas, colunas ou dependências. Endpoints de Viveiro adicionados por mudanças futuras deverão ser autorizados explicitamente conforme seus contratos.
- Visitantes sem sessão e perfis diferentes de bolsista continuam sujeitos às regras atuais.
