# Tasks — Cadastro e Perfil do Cidadão

## Implementação

### Backend (Spring Boot)
- [x] **T01 — Entidade e Repositório**
    - Mapear a classe `Usuario` (campos: `id`, `nome`, `cpf`, `email`, `whatsapp`, `endereco`, `senha`). Adicionar `UNIQUE` em `cpf` e `email`.
    - **Relacionada a:** HU01, RN01, RN03

- [x] **T02 — Endpoint de Cadastro**
    - Criar `POST /usuarios/cadastro` com BCrypt na senha e validação de duplicidade.
    - **Relacionada a:** HU01, RN02

- [x] **T03 — Endpoints de Perfil (Atualização de dados base)**
    - Criar `GET /usuarios/me` e `PUT /usuarios/me` para atualizar Nome, WhatsApp e Endereço (protegendo CPF e E-mail e usando o ID do token).
    - **Relacionada a:** HU02

- [x] **T04 — Endpoint de Alteração de Senha**
    - Criar `PUT /usuarios/me/senha` recebendo `AlterarSenhaRequestDTO` (senhaAtual, novaSenha). 
    - Validar se a `senhaAtual` bate com o BCrypt. Se sim, aplicar hash na `novaSenha` e salvar.
    - **Relacionada a:** HU03, RN04

### Frontend (Vue.js)
- [x] **T05 — Modal de Cadastro**
    - Conectar o botão "Cadastrar" à rota `POST`. Trocar o campo "Telefone" por "WhatsApp" com máscara.
    - **Relacionada a:** HU01

- [x] **T06 — Interface "Meu Perfil"**
    - Criar `PerfilView.vue` com campos de E-mail e CPF `disabled`. Conectar os botões de Salvar aos endpoints.
    - **Relacionada a:** HU02

- [x] **T07 — Componente `ModalAlterarSenha.vue`**
    - Criar o modal com os inputs (Senha Atual, Nova, Confirmação) e ícone de "olho". 
    - Implementar a validação computada (`computed`) que trava o botão se a nova senha e a confirmação não forem iguais.
    - **Relacionada a:** HU03

- [x] **T08 — Integração do Modal e Link de "Esqueci a Senha"**
    - Conectar o formulário do modal ao `PUT /usuarios/me/senha`. 
    - Adicionar o link "Esqueci a senha atual", que, ao ser clicado, fecha o modal e exibe um alerta temporário avisando sobre o e-mail de recuperação.
    - **Relacionada a:** HU03

## Verificação

- [ ] **T09 — Verificar: Cadastro**
    - Confirmar que o sistema grava o utilizador e a senha em hash. Validar bloqueio de duplicidade (CPF/E-mail).
- [ ] **T10 — Verificar: Edição de Perfil**
    - Confirmar atualização de WhatsApp e Endereço com sucesso, sem afetar credenciais.
- [ ] **T11 — Verificar: Alteração de Senha (Sucesso)**
    - Fornecer senha atual correta e senhas novas iguais -> Banco atualiza o hash.
- [ ] **T12 — Verificar: Alteração de Senha (Erros)**
    - Testar bloqueio visual no Front quando as senhas novas não batem. 
    - Testar resposta 400 do Back quando a "Senha atual" estiver errada.

> T09–T12 continuam pendentes de validação prática. A execução Maven não concluiu a compilação dos testes por incompatibilidades preexistentes em `EventosServiceTest.java`; a compilação principal do backend passou.

## Pronto quando
- [ ] Todas as três HUs (Cadastro, Edição e Senha) estão funcionais.
- [ ] O fluxo visual está de acordo com as imagens de protótipo aprovadas.
- [ ] A alteração de senha protege o sistema validando o Hash antigo antes de gravar o novo.
