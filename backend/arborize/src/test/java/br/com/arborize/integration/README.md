# Integração de persistência do Inventário

Execução opt-in, pelo ciclo `test` do Maven:

```sh
python3 scripts/test-inventario-postgres.py
```

Requer Java 21, Python 3, acesso autorizado ao Docker e uma imagem local `postgres:15`.
O script não instala ferramentas e não baixa imagens. O primeiro ciclo Maven pode precisar
obter os plugins e componentes de teste herdados do Spring Boot. É possível selecionar
um Maven existente com `--maven /caminho/para/mvn` e repetir sem rede com `--offline`.

O script sempre cria um container novo e exclusivamente seu, em porta dinâmica restrita
a `127.0.0.1`, sem volumes persistentes, com dados em tmpfs e credencial aleatória descartável.
O container é removido ao terminar, inclusive após falha dos testes.

`InventarioPersistenciaIT` não é executado pelo padrão `*Test` do Surefire. O script o inclui
explicitamente junto aos testes do Inventário. Não executa o `ArborizeApplicationTests`,
pois esse teste legado carrega a aplicação completa e seu datasource normal.

O contexto JPA usa o perfil `inventario-persistencia-it`, não carrega `application*.properties`,
exige variáveis fornecidas pelo executor e recusa URLs fora do padrão isolado ou as portas
5432/5433. O schema é criado e descartado com `create-drop`, nunca `update`.
Não se deve fornecer manualmente URLs ou credenciais de bancos existentes.

Há PostgreSQL e Hibernate reais. Somente `FotoArvoreStorage` é simulado: não existe conexão
MinIO nem servidor HTTP neste contexto. A concorrência usa transações independentes, latches
e confirmação de espera via `pg_stat_activity`/`pg_blocking_pids` no banco exclusivo.

Resultados: `target/surefire-reports/`. SQL efetivamente enviado pelo Hibernate, com parâmetros
não expandidos: `target/inventario-it-sql.log`. Esses testes não comprovam operações reais MinIO,
recuperação após interrupção do processo ou reconciliação durável de objetos órfãos.

## T20-B: PostgreSQL e MinIO reais

Da pasta `backend/arborize`, com Docker autorizado e imagens locais `postgres:15` e
`arborize-minio-it:release-2025-09-07`:

```sh
python3 scripts/test-inventario-storage.py --offline --maven /caminho/para/mvn
```

Na ausência da imagem MinIO de teste, sua preparação explícita é:

```sh
python3 scripts/prepare-minio-test-image.py
```

A preparação requer rede e a imagem **já local** `debian:bookworm-slim`. Usa o binário
oficial Linux amd64 da release `RELEASE.2025-09-07T16-13-09Z`, verifica o checksum
oficial e o SHA-256 fixado no script; não instala binários no host. Nesta execução,
o registry oficial recusou o pull (401); não foi usado S3Mock nem uma distribuição alternativa.
O executor de testes não baixa imagens. `--minio-image` permite selecionar uma imagem
local equivalente, cuja versão efetiva é impressa no início.

Versões verificadas: PostgreSQL **15.19**, MinIO **RELEASE.2025-09-07T16-13-09Z**
(commit `07c3a429bfed433e49018cb0f78a52145d4bedeb`), Tomcat **11.0.24**, Java **21**.

Cada execução cria dois containers próprios, dados em tmpfs, portas dinâmicas somente
em `127.0.0.1`, banco/bucket exclusivos e credenciais aleatórias. Nenhuma configuração
da aplicação normal é carregada pelo contexto JPA/MinIO. O schema usa `create-drop`;
o processo filho de interrupção usa exclusivamente `validate`. O executor remove os
containers em `finally`, inclusive quando Maven falha. Como qualquer processo local,
um encerramento forçado do próprio executor pode impedir seu `finally`; os nomes começam
com `arborize-t20b-` e a label é `arborize.purpose=t20b-isolated-test` para identificação.
Não executar duas instâncias simultâneas na mesma pasta `target`.

### O que os testes comprovam

`InventarioStorageIT` usa repositories, transações, serviço, validador, storage e SDK MinIO
reais. JPG/PNG/WebP são gerados localmente; os bytes recuperados, MIME, chaves e metadados
são comparados com os objetos efetivos. São verificados: vazios, falsos, GIF, excesso de
2.097.152 bytes inclusive com tamanho declarado enganoso, quarta foto, árvore inexistente,
acesso cruzado entre árvores, exclusão preservando outra foto/árvore e leitura anônima 403.
DTOs e resposta HTTP de falha são verificados quanto a dados internos/segredos.

Falhas SQL são provocadas por triggers **somente no banco temporário**. Um proxy TCP local
encaminha as requisições assinadas ao MinIO real e permite negar PUT/DELETE ou reter o ACK
depois de uma operação efetivamente concluída. Não substitui o storage por um mock.
Negação pelo proxy simula uma falha de transporte/S3, não uma política IAM do MinIO.

| Cenário | Metadados ao final | Objetos ao final | Resultado observado |
|---|---:|---:|---|
| A: MinIO rejeita upload (bucket ausente) | 0 | 0 | Nenhuma confirmação falsa |
| B: INSERT falha após PUT real | 0 | 0 | Compensação remove o objeto |
| C: DELETE do objeto negado | 1 | 1 | Banco e conteúdo preservados |
| D: DELETE SQL falha depois da remoção real | 1 | 1 | Backup restaurado com bytes/MIME corretos |
| E: compensação de upload também falha | 0 | 1 | Objeto órfão; log de compensação pendente |
| E: restauração também falha | 1 | 0 | Metadado sem objeto; log de compensação pendente |
| F: JVM filha encerrada após PUT e antes do ACK | 0 | 1 | Órfão; callback em memória não executa |
| F: JVM filha encerrada após DELETE e antes do ACK | 1 | 0 | Metadado sem objeto; callback não executa |
| Duas inclusões concorrentes partindo de duas fotos | 3 | 3 | Uma confirma, outra rejeita, sem excedente |
| Primeira inclusão concorrente sofre rollback, sucessora confirma | 3 | 3 | Objeto da primeira removido, sem metadado duplicado |

A concorrência aguarda confirmação de bloqueio no PostgreSQL (`pg_blocking_pids`) e usa
latches no proxy; não depende apenas de atrasos arbitrários. As interrupções encerram
somente a JVM filha criada pelo teste, depois de confirmar a operação real no MinIO.
Esses resultados **não garantem atomicidade distribuída nem recuperação após crash**.

`FotoArvoreMultipartIT` usa Tomcat/Servlet e HTTP reais, com Service simulado exclusivamente
para isolar o transporte: 2 MiB mais overhead chegam ao Controller; corpo imediatamente
acima de 10 MiB retorna ProblemDetail 413 sem chamar o Service. Os testes de segurança
separados usam `SecurityConfig` e filtros reais, preservando `denyAll()`.

Defeitos reproduzidos e corrigidos: teto padrão de 1 MiB fora do perfil local; resposta
HTML por falha multipart anterior à escolha do Controller. O teto de transporte de 10 MiB
do perfil local foi explicitado na configuração comum; o limite do inventário continua
2 MiB no validador. Um resolver de exceção atende somente POST na rota de fotos e mantém
o contrato ProblemDetail. O descarte Tomcat foi limitado a 11 MiB para permitir responder
ao excesso próximo ao teto sem descarte ilimitado. Corpos muito maiores, clientes que
interrompem o envio e proxies externos ainda podem encerrar a conexão; não há garantia
de resposta JSON para tráfego arbitrariamente grande nem validação de proxy de produção.
Outros módulos não recebem o novo tratamento de erro; a regressão global deles permanece pendente.

Evidências geradas: `target/surefire-reports/`, `target/minio-it-evidence.txt` e logs das JVMs
filhas `target/minio-it-crash-*.log`. Não contêm credenciais configuradas pelos testes.

Resultado T20-B: compilação e **217 testes aprovados**, sem falhas, erros ou ignorados:
19 PostgreSQL/MinIO reais, 16 de persistência PostgreSQL, 2 de transporte HTTP real e
180 testes unitários/contratos/segurança (incluindo 9 com filtros reais). Isso não equivale
a executar toda a aplicação ou verificar todos os módulos legados.

### Encaminhamento para T17-C (não implementado)

Definir um protocolo durável de operações, com intenção persistida antes de tocar o objeto,
estados recuperáveis, repetição idempotente e retenção suficiente para restauração de exclusões.
Avaliar outbox/worker e reconciliação periódica entre metadados e objetos, com tolerância a
operações em andamento, alertas e tratamento operacional de resultados ambíguos. Repetir os
testes E/F após reinício e exigir convergência antes de concluir prevenção de órfãos na T17.
