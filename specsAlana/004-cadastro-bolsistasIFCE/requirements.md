# Requisitos — Cadastro de Bolsistas (Gestor IFCE)

## Objetivo
Permitir que os usuários com o perfil de Gestor do IFCE possam gerenciar a sua própria equipe de laboratório, cadastrando exclusivamente Bolsistas do IFCE de forma segura.

## Histórias de usuário

### HU10 — Cadastrar Bolsista do IFCE
Como Gestor do IFCE logado,
quero poder cadastrar novos bolsistas no sistema,
para que eles tenham acesso às ferramentas de gestão do laboratório do IFCE.

## Regras de negócio

- **RN14:** Apenas usuários autenticados com o nível de acesso `GESTOR_IFCE` podem visualizar a equipe e acessar a funcionalidade de criar bolsistas. Nenhum outro perfil, inclusive `ADMINISTRADOR_AMMA`, pode acessar essa funcionalidade.
- **RN15:** Um Gestor do IFCE **só pode criar usuários do tipo `BOLSISTA_IFCE`**. O sistema deve ignorar qualquer tentativa de criar outro tipo de perfil (como Administrador, Cidadão ou Funcionário AMMA).
- **RN16:** O Gestor define uma "Senha Inicial" no momento do cadastro. O bolsista utilizará essa senha no primeiro acesso e deverá alterá-la posteriormente no seu "Meu Perfil".
- **RN17:** As regras padrão de unicidade de CPF e E-mail continuam a ser aplicadas rigorosamente.

## Critérios de aceitação

### Cenário 1 — Cadastro de Bolsista com sucesso
**Dado que** o Gestor do IFCE está autenticado e acessa a página de "Gestão de Equipe (IFCE)"  
**Quando** ele preenche os dados do bolsista (nome, cpf, email, whatsapp, senha) e submete o formulário  
**Então** o sistema deve criar o usuário forçando a role `BOLSISTA_IFCE`, e exibir a mensagem "Bolsista cadastrado com sucesso!".

### Cenário 2 — Tentativa de elevação de privilégios (Backend Security)
**Dado que** um Gestor do IFCE mal-intencionado interceta a requisição da API  
**Quando** ele injeta no JSON o campo `"role": "ADMINISTRADOR_AMMA"` e envia para o servidor  
**Então** o backend deve ignorar esse campo e salvar o usuário estritamente como `BOLSISTA_IFCE`, ou devolver erro `400 Bad Request`.

### Cenário 3 — Proteção de acesso indevido
**Dado que** um usuário com qualquer perfil diferente de `GESTOR_IFCE` tenta visualizar a equipe ou enviar uma requisição para criar um bolsista  
**Quando** a requisição atinge a API  
**Então** o sistema deve retornar `403 Forbidden`, pois esse perfil não possui a role `GESTOR_IFCE`.
