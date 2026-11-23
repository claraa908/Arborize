# Tasks

## 1. Normalização dos dados

- [x] 1.1 Criar migration Flyway posterior à V2 que converte `AMMA`, `IFCE_GESTOR` e `IFCE_BOLSISTA` para os papéis canônicos e substitui a restrição `CHECK`; verificar em PostgreSQL que os dados da conta permanecem e que valores legados deixam de ser aceitos.

## 2. Backend e autorização

- [x] 2.1 Remover os aliases de `UsuarioRole`, `SecurityConfig`, filtros e serviços de consulta; atualizar testes backend para confirmar que os cinco papéis canônicos mantêm suas permissões e que os serviços listam contas migradas pelo novo papel.
- [x] 2.2 Garantir que tokens com authorities removidas não autentiquem recursos protegidos e que novos logins emitam a authority canônica; verificar com testes de token e filtro de autenticação.

## 3. Frontend e sessões

- [x] 3.1 Reduzir `USER_ROLES`, tipos de perfis institucionais e mapas de apresentação aos cinco papéis; atualizar testes de navegação e componentes para verificar rótulos e acessos dos perfis canônicos.
- [x] 3.2 Limpar token, nome e papel quando o papel salvo não for reconhecido e encaminhar a sessão ao login; verificar cenários de armazenamento legado e papéis canônicos nos testes de autenticação do frontend.

## 4. Reconciliar autorização e documentação

- [x] 4.1 Atualizar os artefatos ainda em andamento de `restringir-acesso-gestor-ifce` para remover a exceção `IFCE_GESTOR` e aplicar a política de `GESTOR_IFCE` às contas convertidas; validar os cenários da proposta e as permissões dos demais perfis.
- [x] 4.2 Atualizar o diagrama de classes para exibir somente os cinco papéis atuais e revisar os artefatos ativos para garantir que referências a aliases estejam limitadas à migração e à transição de sessões; preservar migrations e mudanças concluídas como histórico.
