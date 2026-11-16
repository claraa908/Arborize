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

## Executando os Testes de Integração do Back-End

Alguns testes do backend (como o fluxo de solicitação de mudas) são testes de integração reais e exigem que o banco de dados PostgreSQL esteja ativo localmente.

### 1. Inicie a infraestrutura
Certifique-se de que os containers do projeto (PostgreSQL e MinIO) estão rodando. Na pasta onde está o arquivo `docker-compose.yml`, execute:

```bash
docker compose up -d
```

### 2. Configure a variável de conexão
O Spring Boot utiliza a variável de ambiente `ARBORIZE_IT_JDBC_URL` para conectar o contexto de testes ao banco local. Exporte essa variável no terminal que você vai usar para rodar os testes.

**No Windows (PowerShell):**
```powershell
$env:ARBORIZE_IT_JDBC_URL="jdbc:postgresql://localhost:5433/arborize_db"
```

**No Windows (CMD):**
```cmd
set ARBORIZE_IT_JDBC_URL=jdbc:postgresql://localhost:5433/arborize_db
```

**No Linux, macOS ou Git Bash:**
```bash
export ARBORIZE_IT_JDBC_URL="jdbc:postgresql://localhost:5433/arborize_db"
```
*(Nota: Se o seu PostgreSQL estiver rodando na porta padrão, altere `5433` para `5432` na URL acima).*

### 3. Rode os testes
No mesmo terminal onde a variável foi configurada, navegue até a pasta raiz do backend (`backend/arborize` onde fica o `pom.xml`) e execute:

```bash
mvn test
```

## Executando os Testes do Frontend

O frontend do projeto utiliza o **Vitest** e o **Vue Test Utils** para a execução dos testes unitários e de integração de componentes, rodando em um ambiente simulado de navegador (JSDOM).

### 1. Instalação das dependências
Antes de rodar os testes pela primeira vez, certifique-se de que todas as dependências foram instaladas corretamente. Navegue até a pasta do frontend e execute:

```bash
npm install
```

### 2. Rodando todos os testes
Para executar toda a suíte de testes do frontend, utilize o comando padrão configurado no projeto na pasta frontend:

```bash
npm run test:unit
```

> **Dica:** O Vitest inicia no modo de observação (*watch mode*) por padrão. Ele ficará ativo no terminal e executará os testes novamente de forma automática sempre que você salvar alguma alteração no código. Para encerrar o executor, pressione a tecla `q` ou `Ctrl + C`.

### 3. Rodando um teste específico
Se você estiver trabalhando em uma funcionalidade isolada e quiser rodar apenas o arquivo de teste dela para ganhar tempo, passe o nome do arquivo após o comando com `--`:

```bash
npm run test:unit -- NomeDoArquivo.spec.ts
```

*(Você pode digitar apenas parte do nome do arquivo, e o Vitest filtrará todos os testes correspondentes).*
