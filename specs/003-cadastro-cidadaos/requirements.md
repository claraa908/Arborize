# Requisitos — Cadastro e Perfil do Cidadão

## Objetivo

Permitir que um cidadão possa se cadastrar no sistema como usuário padrão, além de gerenciar os seus dados pessoais e credenciais de acesso com segurança.

## Histórias de usuário

### HU01 — Cadastrar cidadão
Como cidadão,
quero me cadastrar no sistema,
para usufruir das informações disponibilizadas no sistema e realizar doações.

### HU02 — Editar cidadão
Como cidadão,
quero editar minhas informações de cadastro,
para manter meu perfil com minhas informações pessoais atualizado.

### HU03 — Alterar senha de acesso
Como cidadão logado,
quero alterar a minha senha através da tela de perfil,
para garantir e manter a segurança da minha conta.

## Regras de negócio

- **RN01:** cada cidadão deve possuir um identificador único.
- **RN02:** campos como nome, cpf, email, whatsapp, endereço e senha são obrigatórios e não devem ficar vazios.
- **RN03:** não devem existir cidadãos com o mesmo cpf ou email cadastrados no sistema.
- **RN04:** para alterar a senha, o usuário deve obrigatoriamente fornecer a senha atual correta. A nova senha deve ter no mínimo 8 caracteres e a confirmação deve ser idêntica.

## Critérios de aceitação

### Cenário 1 — Cadastro realizado com sucesso
**Dado que** o cidadão abre o modal de cadastro e não possui conta no sistema  
**Quando** ele preenche todos os campos obrigatórios (nome, cpf, email, whatsapp, endereço, senha) corretamente e clica em "Cadastrar"  
**Então** o sistema deve criar o registro gerando um identificador único, fechar o modal de cadastro, abrir o modal de login e exibir uma notificação de sucesso "Cadastro realizado com sucesso".

### Cenário 2 — Tentativa de cadastro com CPF ou E-mail duplicado
**Dado que** o cidadão está no modal de cadastro preenchendo o formulário  
**Quando** ele insere um CPF ou E-mail que já está associado a outro usuário no banco de dados e tenta salvar  
**Então** o sistema deve bloquear o cadastro e exibir a mensagem de erro "Já existe um usuário cadastrado com este CPF/E-mail" dentro do próprio modal.

### Cenário 3 — Validação de campos obrigatórios vazios
**Dado que** o cidadão está no modal de cadastro ou alteração de senha  
**Quando** ele tenta submeter o formulário deixando um ou mais campos obrigatórios vazios  
**Então** o sistema deve impedir o envio, destacar os campos faltantes e exibir a mensagem "Por favor, preencha todos os campos obrigatórios".

### Cenário 4 — Edição de perfil bem-sucedida
**Dado que** o cidadão está autenticado no sistema e acessa a área de edição do seu perfil  
**Quando** ele altera as suas informações (como whatsapp ou endereço) e clica em "Salvar alterações"  
**Então** o sistema deve atualizar os dados no banco e exibir a mensagem "Perfil atualizado com sucesso".

### Cenário 5 — Alteração de senha bem-sucedida
**Dado que** o cidadão está no modal de alterar senha  
**Quando** ele insere a senha atual corretamente e preenche a nova senha (com confirmação idêntica)  
**Então** o sistema deve atualizar o hash da senha no banco de dados, fechar o modal e exibir "Senha atualizada com segurança!".

### Cenário 6 — Erro ao alterar senha (Senha atual incorreta)
**Dado que** o cidadão tenta alterar a sua senha  
**Quando** ele informa uma "Senha atual" que não corresponde ao hash guardado no banco  
**Então** o sistema deve bloquear a ação e exibir a mensagem "A senha atual está incorreta. Tente novamente."

## Restrições

- **Segurança da Senha:** A senha do usuário deve ser armazenada utilizando criptografia unidirecional (hash BCrypt), nunca em texto limpo.
- **Formatação de Dados:** O CPF deve ser salvo no banco apenas com números (11 dígitos), cabendo ao frontend (Vue.js) aplicar a máscara visual. O Whatsapp também deve conter máscara no front.
- **Validação Dupla:** As regras de obrigatoriedade e unicidade devem ser validadas tanto no frontend (Vue) quanto no backend (Spring Validation).