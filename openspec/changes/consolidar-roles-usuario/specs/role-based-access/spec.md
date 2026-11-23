# Spec Delta

## Purpose

Define o conjunto canônico de papéis de usuário do Arborize e assegura que autenticação, autorização e sessões usem esses valores de forma consistente após a consolidação dos papéis legados.

## ADDED Requirements

### Requirement: O sistema reconhece somente os papéis canônicos
O sistema MUST reconhecer e emitir somente `CIDADAO`, `ADMINISTRADOR_AMMA`, `FUNCIONARIO_AMMA`, `GESTOR_IFCE` e `BOLSISTA_IFCE` como papéis de usuário.

#### Scenario: Usuário realiza login com um papel canônico
- **WHEN** um usuário autenticado possui um dos cinco papéis canônicos
- **THEN** a resposta de login e a autoridade do token contêm somente o papel canônico correspondente

#### Scenario: Token contém um papel removido
- **WHEN** uma requisição apresenta um token com `ROLE_AMMA`, `ROLE_IFCE_GESTOR` ou `ROLE_IFCE_BOLSISTA`
- **THEN** o sistema não autentica a requisição com esse papel e exige novo login para acessar recursos protegidos

### Requirement: Registros com papéis legados são convertidos sem perder a conta
A atualização dos dados MUST converter os papéis `AMMA`, `IFCE_GESTOR` e `IFCE_BOLSISTA` para seus equivalentes canônicos, preservando os demais dados e a identidade de cada usuário.

#### Scenario: Papéis antigos são migrados
- **WHEN** a migração encontra um usuário com papel `AMMA`, `IFCE_GESTOR` ou `IFCE_BOLSISTA`
- **THEN** define, respectivamente, `FUNCIONARIO_AMMA`, `GESTOR_IFCE` ou `BOLSISTA_IFCE` e mantém os demais dados da conta

### Requirement: O papel canônico mantém as permissões do perfil equivalente
Após a conversão, cada usuário MUST receber as permissões do papel canônico correspondente; a consolidação MUST NOT ampliar nem reduzir o acesso dos demais papéis.

#### Scenario: Usuário migrado acessa sua área permitida
- **WHEN** um usuário convertido autentica com o papel canônico resultante
- **THEN** as regras de rota e API aplicáveis ao papel canônico são usadas, preservando as permissões dos outros papéis

#### Scenario: Bolsista migrado usa o Viveiro
- **WHEN** uma conta `IFCE_BOLSISTA` convertida autentica como `BOLSISTA_IFCE`
- **THEN** ela mantém o acesso de bolsista às páginas e operações autorizadas do Viveiro e ao próprio perfil

### Requirement: Sessões antigas direcionam o usuário a autenticar novamente
O frontend MUST descartar uma sessão local cujo papel deixou de ser reconhecido e direcionar o usuário ao fluxo de login.

#### Scenario: Navegador mantém papel removido no armazenamento local
- **WHEN** a aplicação inicia com uma sessão armazenada cujo papel é `ROLE_AMMA`, `ROLE_IFCE_GESTOR` ou `ROLE_IFCE_BOLSISTA`
- **THEN** a sessão local é limpa e o usuário é tratado como não autenticado
