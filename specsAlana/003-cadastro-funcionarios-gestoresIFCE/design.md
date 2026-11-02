# Design — Painel de Gestão e Cadastro Institucional

## Objetivo da experiência
Fornecer um ambiente administrativo ("Painel de Gestão") exclusivo para o Administrador da AMMA, onde ele tem total controle sobre a criação de contas com privilégios elevados, sem misturar esse fluxo com o cadastro público de cidadãos.

## Fluxo principal

1. **Acesso:** O Administrador faz login. O menu superior exibe o botão exclusivo "Painel de Gestão".
2. **Visualização:** O Administrador clica e é direcionado para a tela de gestão, que exibe uma tabela com os usuários institucionais já cadastrados e um botão primário "Novo Usuário".
3. **Ação:** Ao clicar em "Novo Usuário", abre-se um Modal (ou uma nova página) com o formulário de cadastro institucional.
4. **Submissão:** Ele preenche os dados, seleciona a função (Role) num campo *Dropdown* (Select), insere a senha inicial e salva.

## Estrutura da interface e Contratos da API

1. **Tela "Painel de Gestão"**
- Tabela de listagem: Nome, E-mail, Perfil (Funcionário ou Gestor), Data de Cadastro e Status.
- *Endpoint de Leitura (opcional para popular a tabela):* `GET /admin/usuarios` (Header: `Authorization: Bearer <token>`).

2. **Formulário/Modal de Cadastro Institucional**
- Inputs: Nome, CPF, E-mail, WhatsApp.
- Select: Nível de Acesso (Opções: `Funcionário AMMA`, `Gestor IFCE`).
- Input de Senha: Senha inicial para o usuário.
- *Endpoint Backend:* `POST /admin/usuarios`
- *Request:* `{"nome":"", "cpf":"", "email":"", "whatsapp":"", "senha":"", "role": "FUNCIONARIO_AMMA"}`
- *Response (201 Created):* Sucesso.
- *Response (403 Forbidden):* Token não pertence a um Administrador.

## Estados da interface e Acessibilidade

- **Proteção de Rota (Vue Router):** A rota `/gestao` deve ser protegida por um `beforeEach` guard que verifica se o estado do Pinia/localStorage contém a role `ADMINISTRADOR_AMMA`.
- **Feedback Visual:** Ao cadastrar, a tabela deve ser atualizada reativamente sem necessidade de recarregar a página por completo, e um *Toast* verde de sucesso deve aparecer no topo.
- **Select Acessível:** O menu de seleção do nível de acesso deve ser navegável por teclado.