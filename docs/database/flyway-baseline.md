# Baseline Flyway do Arborize

`V1__baseline.sql` representa o esquema JPA existente. Bancos vazios recebem essa migration normalmente. A inicialização automática de baseline está desativada: um banco existente sem `flyway_schema_history` falha de propósito até que a equipe confirme sua estrutura.

## Preparar uma base existente

1. Faça backup verificável do banco e interrompa a implantação da aplicação durante a operação.
2. Confirme que as tabelas e relações existentes correspondem a `V1__baseline.sql`. A baseline registra apenas a versão; ela não executa nem altera os dados ou as tabelas atuais.
3. Exporte as configurações de conexão no ambiente da sessão e execute o goal `flyway:baseline` na versão 1. Assim, a senha não fica na linha de comando:

   ```powershell
   $env:FLYWAY_URL = $env:SPRING_DATASOURCE_URL
   $env:FLYWAY_USER = $env:SPRING_DATASOURCE_USERNAME
   $env:FLYWAY_PASSWORD = $env:SPRING_DATASOURCE_PASSWORD
   $env:FLYWAY_BASELINE_VERSION = "1"
   $env:FLYWAY_BASELINE_DESCRIPTION = "Esquema Arborize existente"
   mvn flyway:baseline
   ```

4. Confira a linha `1 / BASELINE` em `flyway_schema_history` e então inicie a aplicação. Flyway executará apenas migrations posteriores, começando pela migration de movimentações.
5. Valide os dados e os fluxos da aplicação antes de liberar o tráfego. Se a validação da estrutura falhar, pare a implantação e corrija a divergência; não use `baselineOnMigrate` nem restaure `ddl-auto=update` para contorná-la.

Para banco novo, crie o banco PostgreSQL vazio, configure `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME` e `SPRING_DATASOURCE_PASSWORD`, e inicie a aplicação. Flyway aplica V1 e as versões seguintes antes de Hibernate validar as entidades.
