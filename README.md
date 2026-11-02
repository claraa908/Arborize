## 🌳 Sistema de Gestão e Monitoramento de Arborização Urbana

> Sistema para mapeamento, acompanhamento e gestão de áreas de plantio urbano, desenvolvido como projeto prático acadêmico em parceria com a **AMMA (Autarquia Municipal do Meio Ambiente)**.

### 👥 Equipe
- **Alana Maria** - *backend* - [GitHub](https://github.com/alana102)
- **Clara Cruz** - *backend* - [GitHub](https://github.com/claraa908)
- **Francisco Edinaldo** - *frontend* - [GitHub](https://github.com/edinaldoGit)
- **Jennifer Marques** - *frontend* - [GitHub](https://github.com/jennifermaqs)

### 🪴 Sobre o projeto
O sistema tem o propósito de auxiliar a AMMA na divulgação de seus projetos e eventos, além de conscientizar a população sobre o trabalho realizado em relação à arborização do município de Quixadá. Ao mesmo tempo, o sistema também atua como uma ferramenta de gestão interna que fornece recursos para organização de documentos de licitação, gerenciamento de denúncias e monitoramento de áreas arborizadas.

### 💻 Tecnologias Utilizadas
| Camada | Tecnologia | Finalidade |
| :--- | :--- | :--- |
| **Backend** | <img src="https://img.shields.io/badge/java-%23ED8B00.svg?style=flat&logo=openjdk&logoColor=white" alt="Java"/> <img src="https://img.shields.io/badge/spring_boot-%236DB33F.svg?style=flat&logo=spring&logoColor=white" alt="Spring Boot"/> | API REST, regras de negócio e persistência |
| **Banco de Dados** | <img src="https://img.shields.io/badge/postgresql-%23316192.svg?style=flat&logo=postgresql&logoColor=white" alt="PostgreSQL"/> | Banco relacional |
| **Storage de Arquivos** | <img src="https://img.shields.io/badge/MinIO-C7202C.svg?style=flat&logo=minio&logoColor=white" alt="MinIO"/> | Armazenamento de imagens em Object Storage |
| **Frontend** | <img src="https://img.shields.io/badge/vue.js-%2335495e.svg?style=flat&logo=vuedotjs&logoColor=%234FC08D" alt="Vue.js"/> <img src="https://img.shields.io/badge/typescript-%23007ACC.svg?style=flat&logo=typescript&logoColor=white" alt="TypeScript"/> <img src="https://img.shields.io/badge/tailwindcss-%2338B2AC.svg?style=flat&logo=tailwind-css&logoColor=white" alt="Tailwind CSS"/> | Interface interativa e consumo da API |
| **Mapeamento Web** | <img src="https://img.shields.io/badge/MapLibre_GL_JS-FF5722.svg?style=flat&logo=maestro&logoColor=white" alt="MapLibre"/> | Renderização georreferenciada de mapas e áreas de plantio |

### Link do Figma:
https://www.figma.com/design/CpatSsboKRabdzBzyetI3b/Arborize?node-id=0-1&p=f&t=nnnVTwkizXyJFbRU-0

### Solicitação de mudas

- `ARBORIZA_SOLICITACAO_MAX_MUDAS`: limite configurável por pedido (padrão `3`, máximo permitido nesta versão: `3`).
- `ARBORIZA_GARRAFAS_POR_MUDA`: garrafas PET necessárias por muda (padrão `5`).

O endpoint de conversão e a API de solicitações usam esses parâmetros no backend. CPF e telefone são persistidos somente com dígitos e não são retornados pela API nem escritos nos logs da funcionalidade.

## Ambiente local de desenvolvimento

O projeto utiliza o `docker-compose.yml` original da equipe: PostgreSQL 15 em `5433`,
pgAdmin em `5050` e MinIO em `9000` (API) e `9001` (console). Os nomes fixos são
`arborize-postgres`, `arborize-pgadmin` e `arborize-minio`. Não é necessário criar nem
preencher um `.env` na raiz para iniciar esses serviços ou o backend local.

**Somente desenvolvimento:** esse Compose contém credenciais fixas e configura
`POSTGRES_HOST_AUTH_METHOD=trust`. As portas não têm restrição explícita ao loopback.
Essa configuração não é adequada para produção nem para exposição em redes não confiáveis.

### Preparar os serviços

1. Confira nomes e portas antes de iniciar:

   ```sh
   docker ps -a
   ```

   Se algum nome ou porta já pertencer a outro ambiente, não pare nem recrie seus serviços.
   Resolva o conflito com o responsável antes de prosseguir. Em especial, um PostgreSQL
   existente na porta `5433` pode conter dados de outro worktree; não inicie o backend
   contra esse destino sem confirmar banco, volume e autorização.

2. Em uma máquina sem conflitos, na raiz do projeto, inicie os serviços:

   ```sh
   docker compose up -d
   docker compose ps
   ```

   Aguarde PostgreSQL e MinIO aceitarem conexões. O Compose original não possui healthcheck
   do PostgreSQL. Os volumes lógicos `postgres_data` e `minio_data` preservam banco e arquivos;
   os nomes físicos dependem do nome do projeto Compose. Preserve o nome do projeto usado
   anteriormente para não criar volumes paralelos sem os dados esperados.

3. Inicie o backend com Java 17 ou superior no perfil local:

   ```sh
   APP_PROFILE=local bash backend/arborize/mvnw -f backend/arborize/pom.xml spring-boot:run
   ```

   Se o wrapper Maven não estiver disponível, use `mvn -f backend/arborize/pom.xml spring-boot:run`.
   A API utiliza `http://localhost:8080`. Os padrões locais conectam a
   `jdbc:postgresql://localhost:5433/arborize_db` e ao MinIO em `http://localhost:9000`,
   com as credenciais de desenvolvimento do Compose original.

As variáveis `ARBORIZA_DB_URL`, `ARBORIZA_DB_PORT`, `ARBORIZA_DB_NAME`, `ARBORIZA_DB_USER`,
`ARBORIZA_DB_PASSWORD`, `ARBORIZA_MINIO_URL`, `ARBORIZA_MINIO_API_PORT`,
`ARBORIZA_MINIO_ACCESS_KEY` e `ARBORIZA_MINIO_SECRET_KEY` continuam disponíveis como
sobrescritas opcionais para ambientes já configurados. Valores exportados anteriormente
prevalecem sobre os padrões; confira o destino efetivo antes de iniciar o backend.

Restaurar o Compose não altera autenticação, usuários ou senhas de um volume PostgreSQL
já inicializado. Não recrie volumes nem altere `pg_hba.conf` para tentar corrigir uma falha
sem antes verificar a configuração efetiva. O mesmo cuidado vale para dados e credenciais
já existentes no MinIO. Não use `docker compose down -v`.

O perfil `local` mantém `ddl-auto=update` e aplica `bairros-referencia-quixada.sql` após a
atualização do esquema JPA em cada inicialização. O script insere somente nomes ausentes
após normalização, sem renomear, reativar, remover bairros ou alterar árvores e associações.
A lista de 24 bairros é uma referência pública provisória, baseada no Plano Municipal da
Primeira Infância de Quixadá, ainda sujeita à validação da AMMA. O backfill territorial
continua manual e não é executado pela inicialização. Confirme o banco antes de iniciar,
pois a inicialização normal do perfil local pode atualizar o esquema e inserir bairros.

### Administrador fictício de desenvolvimento

O bootstrap administrativo continua opcional, desligado por padrão e exclusivo do perfil
`local`. Ele não altera contas existentes. Para utilizá-lo, copie opcionalmente
`.env.example` para `.env`, habilite `ARBORIZA_DEV_ADMIN_BOOTSTRAP_ENABLED=true` e configure
`ARBORIZA_DEV_ADMIN_EMAIL` com domínio reservado `.invalid` e uma senha aleatória local em
`ARBORIZA_DEV_ADMIN_PASSWORD`. Exporte essas variáveis antes de iniciar o backend:

```sh
set -a
. ./.env
set +a
APP_PROFILE=local bash backend/arborize/mvnw -f backend/arborize/pom.xml spring-boot:run
```

O Spring Boot não lê o `.env` automaticamente. Sem bootstrap, esse arquivo e essa etapa
não são necessários. A senha é codificada pelo PasswordEncoder BCrypt existente; somente
o hash é persistido. A rotina não altera nome, senha, CPF ou papel de uma conta existente,
não pode iniciar com `prod` ativo e não modifica as regras de autenticação. Após criar a
conta fictícia, desligue a flag. O `.env` deve permanecer privado e ignorado pelo Git.

`JWT_SECRET` também é opcional no desenvolvimento: sem valor, o backend gera uma chave
por processo. Para manter sessões após reinicializações, forneça localmente um segredo
gerado com `openssl rand -hex 32`. Nunca publique senhas de contas ou segredos JWT.

### Frontend e MapTiler

1. Copie `frontend/.env.example` para `frontend/.env.local` e mantenha `VITE_API_URL=http://localhost:8080` para o backend local.
2. O arquivo versionado `frontend/.env.development` fornece a chave pública MapTiler usada na demonstração acadêmica local. Chaves `VITE_*` são entregues ao navegador; a chave pública está sujeita às cotas do plano e deve ter as origens locais autorizadas no MapTiler.
3. Se precisar substituir a chave apenas nesta máquina, use `frontend/.env.development.local`, ignorado pelo Git. No Vite, arquivos específicos do modo (`.env.development`) têm precedência sobre `.env.local`; variáveis exportadas no ambiente do processo têm precedência sobre os arquivos.
4. Inicie o frontend:

   ```sh
   npm --prefix frontend run dev
   ```

   O arquivo `.env.local` é ignorado pelo Git. Chaves `VITE_*` ficam visíveis no navegador; use somente uma chave pública restrita por origem, nunca uma credencial secreta.

A base cartográfica normal funciona sem chave. A chave MapTiler é necessária somente para a base que usa o provedor. Não altere o estilo, as atribuições ou os controles do mapa para configurar a chave.

### Encerrar e consultar serviços

```sh
docker compose ps
docker compose stop
```

Os comandos devem ser executados com o mesmo nome de projeto usado na inicialização e
somente sobre o ambiente confirmado. Não remova volumes para reiniciar o projeto. O pgAdmin
faz parte da inicialização padrão do Compose original e fica disponível em `http://localhost:5050`;
o console MinIO fica em `http://localhost:9001`.

O perfil `prod` continua desativando a carga SQL automática e o bootstrap administrativo.
A restauração do ambiente local não equivale à preparação para produção nem à homologação
dos bairros pela AMMA.
