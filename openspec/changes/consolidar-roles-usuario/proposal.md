# Proposal

## Why

O sistema mantém três pares de papéis equivalentes (`AMMA`, `IFCE_GESTOR` e `IFCE_BOLSISTA` junto às formas já padronizadas), o que espalha verificações, amplia a chance de divergência entre interface e API e mantém valores antigos no banco. Consolidar o modelo nos cinco papéis definidos reduz essa duplicidade e deixa explícito o contrato de autorização.

## What Changes

- **BREAKING** Remover `AMMA`, `IFCE_GESTOR` e `IFCE_BOLSISTA` do modelo de papéis aceitos pela aplicação; os únicos papéis serão `CIDADAO`, `ADMINISTRADOR_AMMA`, `FUNCIONARIO_AMMA`, `GESTOR_IFCE` e `BOLSISTA_IFCE`.
- Migrar registros existentes para o papel canônico equivalente, preservando a identidade e o acesso funcional das contas.
- Atualizar autenticação, autorização, cadastro/listagem de usuários, tipos e apresentação no frontend, testes e documentação para usar somente os papéis canônicos.
- Invalidar sessões emitidas com papéis removidos e solicitar novo login; os próximos tokens usarão os valores canônicos.
- Reconciliar a proposta em andamento `restringir-acesso-gestor-ifce`, que ainda declara `IFCE_GESTOR` fora da restrição.

## Capabilities

### New Capabilities

- `role-based-access`: Define os papéis canônicos aceitos, a normalização dos registros legados e o uso consistente desses papéis na autorização.

### Modified Capabilities

Nenhuma. O repositório ainda não possui especificações publicadas em `openspec/specs/`.

## Impact

- Backend Spring: enum `UsuarioRole`, emissão e validação de JWT, filtros de segurança, regras de autorização e serviços que consultam usuários por papel.
- Banco PostgreSQL: nova migration Flyway para converter os valores existentes e restringir a coluna `usuarios.role` aos cinco papéis canônicos, sem reescrever migrations aplicadas.
- Frontend Vue: tipos de autenticação, leitura de sessão armazenada, guards, apresentação de perfis e serviços/telas administrativas.
- Testes de autenticação, autorização, gestão de usuários e navegação; diagrama de classes e artefatos da proposta do gestor IFCE ainda em andamento.
