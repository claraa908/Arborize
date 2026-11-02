# Design — Gestão de Equipe (Gestor IFCE)

## Objetivo da experiência
Criar uma interface simples e direta onde o Gestor possa adicionar rapidamente os seus alunos/bolsistas ao sistema, sem se deparar com opções administrativas que não lhe dizem respeito (como escolhas de perfis da AMMA).

## Fluxo principal

1. Somente o Gestor do IFCE faz login e vê no menu a opção "Equipe do Laboratório". Administradores e demais perfis não visualizam nem acessam essa área.
2. Ele acede à página, que lista os bolsistas atuais e possui um botão "Novo Bolsista".
3. Ao clicar, abre-se um modal de cadastro. **Não há campo de seleção de Perfil (Role)**, pois o sistema já sabe que será um bolsista.
4. O Gestor preenche os dados pessoais e a senha, clica em Salvar, e a lista é atualizada.

## Estrutura da interface e Contratos da API

1. **Formulário de Cadastro (Modal/Página)**
- Inputs: Nome, CPF, E-mail, WhatsApp e Senha Inicial.
- Ausente: Qualquer campo de seleção de Nível/Role.
- *Endpoint Backend:* `POST /ifce/bolsistas`
- *Request:* `{"nome":"", "cpf":"", "email":"", "whatsapp":"", "senha":""}`
- *Response (201 Created):* Sucesso.

2. **Listagem da Equipe**
- *Endpoint:* `GET /ifce/bolsistas` (Header: `Authorization: Bearer <token>`).
- A listagem e o cadastro são autorizados exclusivamente para `GESTOR_IFCE`.

*Nota de Segurança API:* 
Optamos por criar a rota `/ifce/bolsistas` (em vez de usar a mesma rota do admin) justamente para que o backend assuma o controlo total sobre qual *role* será atribuída.
