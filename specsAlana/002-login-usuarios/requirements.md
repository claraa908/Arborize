# Requisitos — Autenticação Unificada, Sessão e Edição Segura

## Objetivo
Permitir que qualquer usuário cadastrado (Cidadão, Funcionário da AMMA, Gestor ou Bolsista do IFCE) inicie uma sessão segura no sistema através de um login único, garantindo que o sistema identifique automaticamente o seu nível de permissão com base na credencial fornecida.

## Histórias de usuário

### HU04 — Fazer Login Unificado
Como usuário cadastrado,
quero entrar no sistema usando meu E-mail ou CPF e Senha,
para iniciar uma sessão e acessar as funcionalidades correspondentes ao meu perfil de acesso.

### HU05 — Manter Sessão
Como usuário logado,
quero que o sistema lembre de mim enquanto navego pelas páginas,
para não ter que fazer login a cada clique.

### HU06 — Logout
Como usuário logado,
quero poder sair da minha conta,
para encerrar minha sessão com segurança em dispositivos compartilhados.

### HU07 — Edição de Perfil Restrita (Segurança)
Como usuário logado,
quero garantir que apenas eu possa editar os meus dados pessoais,
para proteger minhas informações de acessos indevidos.

## Regras de negócio

- **RN05:** O login só deve ser bem-sucedido se as credenciais (e-mail ou CPF + senha) baterem exatamente com os registros do banco de dados, independentemente do tipo de usuário.
- **RN06:** O sistema deve identificar o nível de acesso (Role/Perfil) da conta associada ao e-mail ou CPF informado e incluir essa informação na resposta do login.
- **RN07:** O backend deve gerar um Token de Sessão (JWT) válido por um tempo determinado (ex: 24 horas), contendo as permissões (roles) do usuário dentro do token.
- **RN08:** Rotas sensíveis (como carregar e editar o perfil) devem exigir o Token. Sem ele, o backend deve barrar a requisição (Erro 401 - Unauthorized).
- **RN09:** O usuário só pode editar o seu próprio perfil. O backend deve inferir quem está fazendo a requisição lendo o Token, e não confiando no ID enviado pelo frontend.

## Critérios de aceitação

### Cenário 1 — Login unificado com sucesso
**Dado que** o usuário (de qualquer perfil) está no modal de Login  
**Quando** ele insere as credenciais corretas e clica em "Entrar"  
**Então** o sistema deve gerar o Token, retornar qual é o perfil daquele usuário (ex: `ROLE_AMMA`, `ROLE_CIDADAO`), e o frontend deve redirecionar ou adaptar a interface com base nesse nível de acesso.

### Cenário 2 — Login com falha
**Dado que** o usuário está no modal de Login  
**Quando** ele insere uma senha incorreta ou usuário inexistente  
**Então** o sistema deve exibir a mensagem "Credenciais inválidas. Tente novamente." e impedir o acesso.

### Cenário 3 — Proteção de Rota Baseada em Role e Sessão
**Dado que** um usuário tenta enviar uma requisição direta para uma rota restrita (ex: edição de perfil)  
**Quando** a requisição chega no backend sem um Token de Sessão válido  
**Então** o sistema deve devolver o erro `401 Unauthorized` ou `403 Forbidden` e nenhuma alteração deve ser feita no banco.