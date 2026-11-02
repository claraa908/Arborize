# Design — Cadastro e Perfil do Cidadão

## Objetivo da experiência

Permitir que um cidadão consiga criar a sua conta de forma rápida através do modal já existente e, posteriormente, acesse uma tela dedicada e intuitiva ("Meu Perfil") para visualizar e atualizar as suas informações pessoais e gerir a sua segurança (Alterar Senha).

## Histórias atendidas

- **HU01:** Cadastrar cidadão.
- **HU02:** Editar cidadão.
- **HU03:** Alterar senha de acesso.

## Fluxo principal

**Fluxo A: Cadastro (Via Modal)**
1. A pessoa clica em "Criar conta" e abre o Modal de Cadastro.
2. A pessoa preenche os dados e clica em "Cadastrar".
3. O sistema envia a requisição (`POST /usuarios/cadastro`), exibe alerta de sucesso e abre o Login.

**Fluxo B: Edição (Nova Tela "Meu Perfil")**
1. A pessoa acessa o menu e clica em "Meu Perfil".
2. O sistema carrega a página com os dados (`GET /usuarios/me`), usando o token da sessão.
3. A pessoa edita (ex: whatsapp, endereço) e clica em "Salvar alterações".
4. O sistema atualiza (`PUT /usuarios/me`) e exibe notificação de sucesso.

**Fluxo C: Alterar Senha (Modal interno do Perfil)**
1. Na tela "Meu Perfil", a pessoa clica em "Alterar senha".
2. Abre-se um Modal solicitando Senha Atual, Nova Senha e Confirmação.
3. A pessoa preenche e salva. O sistema valida (`PUT /usuarios/me/senha`), fecha o modal e confirma sucesso.

## Estrutura da interface e Contratos da API

1. **Modal de Cadastro**
- *Endpoint Backend:* `POST /usuarios/cadastro`
- *Request:* `{"nome":"", "cpf":"", "email":"", "whatsapp":"", "endereco":"", "senha":""}`

2. **Tela "Meu Perfil"**
- Avatar com iniciais, CPF e E-mail bloqueados (`disabled`). Campos de Nome, WhatsApp e Endereço editáveis. 
- *Endpoint Backend:* `PUT /usuarios/me` (protegido pelo token; o ID vem da sessão)
- *Request:* `{"nome":"", "whatsapp":"", "endereco":""}`

3. **Modal de Alterar Senha**
- Sobreposição na tela de perfil com 3 inputs de senha e link de recuperação.
- *Endpoint Backend:* `PUT /usuarios/me/senha` (protegido pelo token; o ID vem da sessão)
- *Request:* `{"senhaAtual": "...", "novaSenha": "..."}`
- *Response:* `200 OK` (Sucesso) ou `400 Bad Request` (Senha atual incorreta).

## Campos e controles

### WhatsApp e Endereço
- Tipo: Input de texto.
- Obrigatório: Sim.
- Placeholder: "(00) 00000-0000" e "Rua, Número, Bairro".

### Modal de Senhas (Senha atual, Nova e Confirmar)
- Tipo: Password (com ícone 'Olho' para revelar).
- Validação no Front: As senhas de "Nova" e "Confirmar" devem ser exatamente iguais.
- Validação no Back: A "Senha atual" deve bater com o hash do banco. A "Nova senha" ganha um novo hash BCrypt.
- **Link Secundário:** "Esqueci a senha atual" (Abaixo do primeiro input, verde, que dispara fluxo de recuperação futuro).

## Estados da interface

- **Sucesso:** Componente *Toast* verde no canto superior com mensagens claras ("Perfil atualizado" ou "Senha alterada").
- **Erro de Senha:** Se a "Senha atual" estiver errada, exibe-se uma caixa vermelha de erro no topo do modal. Se a "Nova" e "Confirmar" não baterem, o botão de submissão fica desativado (`disabled`) e um texto vermelho surge abaixo do input.

## Responsividade e Acessibilidade

- Desktop: Tela de Perfil centralizada (max-width 600px). Modais sobrepostos com fundo escurecido.
- Mobile: Elementos ocupam 100% da largura.
- Acessibilidade: Navegação correta via `Tab` entre os inputs de senha.
