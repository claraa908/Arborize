# Requisitos — Autenticação, Sessão e Edição Segura

## Objetivo
Permitir que usuários cadastrados iniciem uma sessão segura no sistema para acessar funcionalidades restritas (como editar o próprio perfil), garantindo que ninguém possa alterar os dados de outra pessoa.

## Histórias de usuário

### HU04 — Fazer Login
Como cidadão cadastrado,
quero entrar no sistema usando meu E-mail/CPF e Senha,
para iniciar uma sessão segura e acessar meu perfil.

### HU05 — Manter Sessão
Como cidadão logado,
quero que o sistema lembre de mim enquanto navego pelas páginas,
para não ter que fazer login a cada clique.

### HU06 — Logout
Como cidadão logado,
quero poder sair da minha conta,
para encerrar minha sessão com segurança em dispositivos compartilhados.

### HU07 — Edição de Perfil Restrita (Segurança)
Como cidadão logado,
quero garantir que apenas eu possa editar os meus dados,
para proteger minhas informações de acessos indevidos.

## Regras de negócio

- **RN05:** O login só deve ser bem-sucedido se as credenciais (e-mail ou CPF + senha) baterem exatamente com os registros do banco.
- **RN06:** Uma vez autenticado, o backend deve gerar um Token de Sessão (JWT) válido por um tempo determinado (ex: 24 horas).
- **RN07:** Rotas sensíveis (como carregar e editar o perfil) devem exigir o Token. Sem ele, o backend deve barrar a requisição (Erro 401 - Unauthorized).
- **RN08:** O usuário só pode editar o seu próprio perfil. O backend deve inferir quem está fazendo a requisição lendo o Token, e não confiando no ID enviado pelo frontend.

## Critérios de aceitação

### Cenário 1 — Login com sucesso
**Dado que** o cidadão está no modal de Login  
**Quando** ele insere as credenciais corretas e clica em "Entrar"  
**Então** o sistema deve gerar o Token, o frontend deve salvar a sessão e a Navbar deve mudar para o estado "Logado", fechando o modal.

### Cenário 2 — Login com falha
**Dado que** o cidadão está no modal de Login  
**Quando** ele insere uma senha incorreta ou usuário inexistente  
**Então** o sistema deve exibir a mensagem "Credenciais inválidas. Tente novamente." e impedir o acesso.

### Cenário 3 — Proteção de Rota (Edição Segura)
**Dado que** um usuário mal-intencionado (ou deslogado) tenta enviar uma requisição direta para editar um perfil  
**Quando** a requisição chega no backend sem um Token de Sessão válido  
**Então** o sistema deve devolver o erro `401 Unauthorized` ou `403 Forbidden` e nenhuma alteração deve ser feita no banco.