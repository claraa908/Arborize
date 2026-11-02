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

## Ambiente local reproduzível

O ambiente de desenvolvimento usa serviços isolados, portas vinculadas somente ao loopback e credenciais fornecidas localmente. A relação de 24 bairros é uma referência pública provisória, baseada no Plano Municipal da Primeira Infância de Quixadá; ainda depende de validação institucional da AMMA.

### Preparar os serviços

1. Copie o exemplo de variáveis e preencha as senhas locais. Gere senhas locais com `openssl rand -hex 24`; se definir `JWT_SECRET`, gere 32 bytes com `openssl rand -hex 32`. Não use credenciais institucionais nem reutilize segredos de outros ambientes.

   ```sh
   cp .env.example .env
   ```

   Configure ao menos `ARBORIZA_DB_PASSWORD`, `ARBORIZA_MINIO_ACCESS_KEY` e `ARBORIZA_MINIO_SECRET_KEY`; se usar o pgAdmin, defina também `ARBORIZA_PGADMIN_PASSWORD`. O arquivo `.env` é ignorado pelo Git.

2. Inicie apenas PostgreSQL e MinIO com um nome de projeto próprio. Isso cria volumes namespaced `arborize-dev_*`, separados de volumes existentes com outros nomes de projeto:

   ```sh
   docker compose -p arborize-dev up -d postgres minio
   ```

   O PostgreSQL 15 usa SCRAM para conexões TCP e publica a porta padrão `55433` somente em `127.0.0.1`. O MinIO publica `19000`/`19001` no loopback. Não use `down -v`: os volumes locais contêm os dados do ambiente.

3. Exporte as variáveis para o processo Spring e inicie o backend no perfil local:

   ```sh
   set -a
   . ./.env
   set +a
   APP_PROFILE=local bash backend/arborize/mvnw -f backend/arborize/pom.xml spring-boot:run
   ```

   Se o wrapper Maven não estiver disponível na cópia local, execute `mvn -f backend/arborize/pom.xml spring-boot:run`.

O perfil `local` aplica `bairros-referencia-quixada.sql` após a criação/atualização do esquema JPA, em toda inicialização do backend. O script insere somente nomes ainda ausentes após normalização; não renomeia, reativa nem remove bairros e não altera árvores ou associações. Assim, atende tanto um banco novo quanto um esquema de desenvolvimento já existente sem exigir execução manual recorrente. O script territorial de backfill continua manual e não é executado pelo bootstrap.

As variáveis `POSTGRES_*` do Compose e os argumentos de `initdb` só inicializam autenticação em um volume PostgreSQL novo. Eles não mudam senha nem `pg_hba.conf` de um volume já criado. Para usar intencionalmente outro banco local existente, configure `ARBORIZA_DB_URL`, `ARBORIZA_DB_USER` e `ARBORIZA_DB_PASSWORD` no ambiente local; confirme banco, schema e autenticação antes de iniciar, pois o perfil `local` executará a carga idempotente de bairros nesse destino. Não tente converter o volume automaticamente. O comando com `-p arborize-dev` usa um volume separado e não deve ser removido para “reaproveitar” dados de outro projeto.

### Administrador fictício de desenvolvimento

O bootstrap é desligado por padrão (`ARBORIZA_DEV_ADMIN_BOOTSTRAP_ENABLED=false`) e só é registrado no perfil `local`. Para criar uma conta, altere no `.env`:

```dotenv
ARBORIZA_DEV_ADMIN_BOOTSTRAP_ENABLED=true
ARBORIZA_DEV_ADMIN_EMAIL=admin.dev@example.invalid
ARBORIZA_DEV_ADMIN_PASSWORD=
```

Gere uma senha aleatória local, reinicie o backend e faça login em `/auth/login` com esses valores. O e-mail deve usar o domínio reservado `.invalid`. A senha é codificada com o `PasswordEncoder` BCrypt existente; somente o hash é persistido. Se o e-mail já estiver cadastrado, o bootstrap não altera nome, senha, CPF ou perfil. A rotina usa dados claramente fictícios para os demais campos obrigatórios e não pode iniciar quando o perfil `prod` estiver ativo. Depois da criação, pode desligar a flag; isso não remove a conta nem redefine sua senha.

Não coloque segredos no código, em SQL ou neste repositório. O bootstrap não altera regras de autenticação, permissões ou rotas.

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
docker compose -p arborize-dev ps
docker compose -p arborize-dev stop
docker compose -p arborize-dev down
```

`down` remove os containers e a rede do projeto, mas preserva os volumes nomeados. Não acrescente `-v` se quiser conservar os dados locais. O pgAdmin é opcional e fica restrito ao loopback:

```sh
docker compose -p arborize-dev --profile tools up -d pgadmin
```

A configuração do perfil `prod` desativa a carga SQL automática e o bootstrap administrativo. A lista de bairros continua sendo apenas uma referência pública provisória, não homologação da AMMA.
