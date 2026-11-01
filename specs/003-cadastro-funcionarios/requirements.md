# Requisitos — Cadastro de Usuários Institucionais

## Objetivo
Permitir que o Administrador da AMMA gerencie o acesso da equipe interna e dos parceiros institucionais, criando contas para Funcionários da AMMA e Gestores do IFCE de forma segura e restrita.

## Histórias de usuário

### HU08 — Cadastrar Funcionário da AMMA
Como Administrador da AMMA logado,
quero poder cadastrar novos funcionários da AMMA no sistema,
para que eles possam auxiliar na gestão e moderação da plataforma.

### HU09 — Cadastrar Gestor do IFCE
Como Administrador da AMMA logado,
quero poder cadastrar novos Gestores do IFCE,
para que eles possam acessar as ferramentas de gestão do laboratório e gerenciar os bolsistas.

## Regras de negócio

- **RN10:** Apenas usuários autenticados com o nível de acesso `ADMINISTRADOR_AMMA` podem acessar a tela de gestão e enviar requisições de cadastro institucional.
- **RN11:** O formulário institucional deve permitir a escolha do tipo de conta (`FUNCIONARIO_AMMA` ou `GESTOR_IFCE`). O nível de Administrador não pode ser atribuído por esta tela (apenas via banco de dados, por segurança).
- **RN12:** O Administrador deve definir uma "Senha Inicial" durante o cadastro. O novo usuário utilizará essa senha no primeiro acesso e poderá alterá-la posteriormente no seu próprio "Meu Perfil".
- **RN13:** As regras de unicidade mantêm-se: não é possível cadastrar um funcionário ou gestor com um CPF ou E-mail que já exista no sistema.

## Critérios de aceitação

### Cenário 1 — Cadastro de Funcionário realizado com sucesso
**Dado que** o Administrador da AMMA está autenticado e acessa o "Painel de Gestão"  
**Quando** ele preenche os dados do novo funcionário, seleciona o perfil "Funcionário AMMA", define uma senha inicial e clica em "Salvar"  
**Então** o sistema deve criar a conta com a role `FUNCIONARIO_AMMA`, exibir a mensagem "Usuário cadastrado com sucesso" e listar o novo usuário na tabela de gestão.

### Cenário 2 — Tentativa de acesso indevido (Segurança)
**Dado que** um usuário comum (Cidadão) ou um Gestor do IFCE tenta acessar a URL da tela de gestão ou enviar uma requisição direta para a API de cadastro institucional  
**Quando** o sistema interceptar a requisição  
**Então** o frontend deve redirecionar o usuário para a página inicial (Home) e a API deve retornar o erro `403 Forbidden`, impedindo qualquer ação.

### Cenário 3 — Proteção contra duplicação
**Dado que** o Administrador tenta cadastrar um novo Gestor do IFCE  
**Quando** ele insere um e-mail ou CPF já utilizado por um cidadão ou outro funcionário  
**Então** o sistema deve bloquear o cadastro e exibir a mensagem "Este E-mail ou CPF já está em uso no sistema."