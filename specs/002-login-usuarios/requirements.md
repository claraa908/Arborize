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

### HU08 — Navegar conforme a sessão
Como usuário autenticado,
quero acessar meu dashboard e meu perfil no layout correspondente à minha conta,
para navegar pelo Arborize mantendo minha sessão e reconhecendo as ações disponíveis.

## Regras de negócio

- **RN05:** O login só deve ser bem-sucedido se as credenciais (e-mail ou CPF + senha) baterem exatamente com os registros do banco de dados, independentemente do tipo de usuário.
- **RN06:** O sistema deve identificar o nível de acesso (Role/Perfil) da conta associada ao e-mail ou CPF informado e incluir essa informação na resposta do login.
- **RN07:** O backend deve gerar um Token de Sessão (JWT) válido por um tempo determinado (ex: 24 horas), contendo as permissões (roles) do usuário dentro do token.
- **RN08:** Rotas sensíveis (como carregar e editar o perfil) devem exigir o Token. Sem ele, o backend deve barrar a requisição (Erro 401 - Unauthorized).
- **RN09:** O usuário só pode editar o seu próprio perfil. O backend deve inferir quem está fazendo a requisição lendo o Token, e não confiando no ID enviado pelo frontend.

- **RN10:** Visitantes utilizam o layout público com entrada e cadastro. Cidadãos autenticados mantêm esse layout, com Dashboard, Meu perfil e Sair. Usuários institucionais utilizam o painel administrativo existente; ao visitar o site público, veem Voltar ao painel e Sair, sem controles de visitante ou de cidadão.
- **RN11:** Após login sem destino autorizado explícito, o cidadão segue para seu dashboard público e os perfis institucionais reconhecidos seguem para o dashboard administrativo existente. A sessão é restaurada antes da avaliação de menus e rotas. URLs administrativas são bloqueadas para cidadãos, inclusive no acesso direto; logout limpa a sessão e retorna à página pública. Redirecionamentos externos, desconhecidos ou incompatíveis com o papel não são aceitos.
- **RN12:** Meu perfil existe nos dois layouts e reutiliza os mesmos serviços e formulário. Apenas nome, WhatsApp e endereço são editáveis pelo contrato atual; CPF, e-mail e papel não podem ser alterados nesse formulário. A alteração de senha mantém o fluxo existente. O dashboard do cidadão não apresenta métricas, histórico ou solicitações fictícias.
- **RN13:** Os controles específicos de gestão de usuários e equipe IFCE preservam as restrições atuais de papel. Reconhecer um usuário institucional não concede novos privilégios. Guards e menus do frontend não substituem autorização do backend; a pendência 001/T19 permanece aberta.

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

### Cenário 4 — Visitante e cidadão nas páginas públicas
**Dado que** uma página pública está aberta,  
**Quando** não existe sessão,  
**Então** aparecem Entrar e Cadastrar-se, sem exigir login para recursos públicos.  
**Dado que** há uma sessão de cidadão,  
**Quando** ele navega nessas páginas,  
**Então** a sessão permanece e os controles são Dashboard, Meu perfil e Sair, sem gestão institucional.

### Cenário 5 — Destino do login e acesso direto
**Dado que** o login teve sucesso sem destino autorizado explícito,  
**Quando** o frontend avalia o papel retornado,  
**Então** o cidadão acessa `/dashboard` no layout público e o institucional acessa `/sistema` no layout administrativo.  
**Dado que** um cidadão abre uma URL administrativa diretamente,  
**Quando** o guard avalia a sessão,  
**Então** o acesso é impedido sem renderizar o painel administrativo nem produzir loop.

### Cenário 6 — Perfil pessoal nos dois layouts
**Dado que** há sessão válida,  
**Quando** o usuário acessa Meu perfil,  
**Então** os dados de `/usuarios/me` aparecem no layout público para cidadão e administrativo para institucional, com os mesmos campos editáveis, estados de carregamento, erro e acesso negado; nenhum controle permite alterar papel, CPF ou e-mail.

### Cenário 7 — Visita institucional ao site público
**Dado que** um institucional está no painel,  
**Quando** escolhe Visitar página pública,  
**Então** mantém a sessão e encontra Voltar ao painel, que retorna ao dashboard administrativo, sem Dashboard ou Meu perfil do cidadão e sem login/cadastro de visitante.

### Cenário 8 — Recarregar e sair
**Dado que** cidadão ou institucional está autenticado,  
**Quando** recarrega a página ou navega entre seções públicas permitidas,  
**Então** a sessão e os menus correspondentes permanecem.  
**Quando** confirma ou executa Sair,  
**Então** a sessão é removida e retorna ao estado de visitante na página inicial.

### Cenário 9 — Restrições de gestão preservadas
**Dado que** o usuário não é administrador da AMMA ou não é `GESTOR_IFCE`, respectivamente,  
**Quando** tenta acessar a gestão de usuários ou a equipe IFCE,  
**Então** o frontend mantém os bloqueios existentes, sem ampliar as permissões do backend.

### Cenário 10 — Dashboard do cidadão sem dados inventados
**Dado que** há sessão de cidadão,  
**Quando** seu dashboard é exibido,  
**Então** há saudação com o nome da sessão e links para Meu perfil e seções públicas existentes, sem indicadores ou históricos simulados.
