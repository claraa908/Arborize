# Transição territorial de árvores

O script `bairros-referencia-quixada.sql` também é configurado como carga idempotente do
perfil `local`: o Spring Boot o executa após a atualização do esquema JPA em cada inicialização
de desenvolvimento. Isso atende bancos novos e esquemas locais existentes sem aplicar a carga
em `prod`. A mesma carga continua podendo ser executada manualmente conforme o procedimento
abaixo, após conferir destino e autorização.

`territorio-arvore-bairro-cep.sql` permanece um procedimento manual; não é carregado pelo Spring
Boot. `ddl-auto=update` pode criar colunas da entidade, mas não executa backfill.

`territorio-arvore-bairro-cep.sql` adiciona `bairro_nome`, `cep` e `localidade_rural` como
colunas anuláveis. O backfill copia `bairros.nome` somente para árvores com `bairro_id` válido e
`bairro_nome IS NULL`. Não altera `bairro_id`, a tabela `bairros`, valores de CEP/localidade nem
nomes já preenchidos. O CEP e a localidade rural permanecem nulos onde não há informação real.

`bairros-referencia-quixada.sql` insere nomes ausentes da lista de 24 bairros publicada no Plano
Municipal da Primeira Infância de Quixadá (tabela com fonte indicada “IBGE 2023”, consultada em
2026-11-01). A lista é referência inicial e ainda depende de validação da AMMA. A referência
histórica da Lei Municipal nº 2.319/2007 apresenta 22 nomes: Carrascal não aparece dividido em
I/II, “Residencial” não consta e a página histórica transcreve “São Joãi”; o PMPI apresenta “São
João”. O script compara nomes sem distinção de acentuação, caixa ou espaços repetidos. Ele não
renomeia, reativa, remove ou troca IDs/associações preexistentes. Não cria polígonos ou
coordenadas e não transforma comunidades/distritos rurais em bairros.

Fontes consultadas em 2026-11-01:

- Prefeitura de Quixadá, Plano Municipal da Primeira Infância 2022–2031: <https://quixada.ce.gov.br/conselhoarquivo/1/302/planomunicipalpelaprimeirainfancia.pdf>.
- CREDE12/SEDUC-CE, referência histórica das Leis Municipais 1.863/1999 e 2.319/2007: <https://www.ce.gov.br/crede12-seduc/2016/02/25/quixada-historico/>.

## Execução manual controlada

Ao executar manualmente qualquer script, exigir autorização operacional explícita, backup
confirmado e revisão do banco e schema de destino. No perfil `local`, somente a carga de bairros
é aplicada automaticamente pelo Spring Boot. Ela pode ser reexecutada sem duplicar nomes
equivalentes, mas idempotência não substitui a validação institucional da lista nem a revisão de
bairros inativos e aliases. O backfill de `bairroNome` permanece manual e não deve ser repetido
depois que operadores puderem editar/limpar esse campo.

Antes da execução, conferir destino, contagens e duplicidades normalizadas:

```sql
SELECT current_database(), current_schema();

SELECT to_regclass('arvores') AS tabela_arvores,
       to_regclass('bairros') AS tabela_bairros;

SELECT COUNT(*) AS associacoes_com_nome
FROM arvores AS arvore
JOIN bairros AS bairro ON bairro.id = arvore.bairro_id
WHERE arvore.bairro_nome IS NULL;

SELECT COUNT(*) AS bairros_existentes,
       COUNT(DISTINCT translate(lower(regexp_replace(btrim(nome), '[[:space:]]+', ' ', 'g')),
           'áàâãäéèêëíìîïóòôõöúùûüç', 'aaaaaeeeeiiiiooooouuuuc')) AS nomes_normalizados
FROM bairros;
```

Com aprovação explícita para o banco local e confirmação da variável de conexão, executar
separadamente os scripts, interrompendo em erro:

```sh
psql --set=ON_ERROR_STOP=1 --dbname="$PGDATABASE" \
  --file=src/main/resources/db/manual/territorio-arvore-bairro-cep.sql

psql --set=ON_ERROR_STOP=1 --dbname="$PGDATABASE" \
  --file=src/main/resources/db/manual/bairros-referencia-quixada.sql
```

Após a execução, conferir o resultado antes de liberar a aplicação:

```sql
SELECT COUNT(*) AS associacoes_sem_nome
FROM arvores AS arvore
JOIN bairros AS bairro ON bairro.id = arvore.bairro_id
WHERE arvore.bairro_nome IS NULL;

SELECT COUNT(*) AS arvores_sem_bairro
FROM arvores
WHERE bairro_id IS NULL AND bairro_nome IS NULL;

SELECT COUNT(*) AS ceps_preenchidos
FROM arvores
WHERE cep IS NOT NULL;

SELECT COUNT(*) AS localidades_rurais_preenchidas
FROM arvores
WHERE localidade_rural IS NOT NULL;

SELECT translate(lower(regexp_replace(btrim(nome), '[[:space:]]+', ' ', 'g')),
       'áàâãäéèêëíìîïóòôõöúùûüç', 'aaaaaeeeeiiiiooooouuuuc') AS chave,
       COUNT(*) AS quantidade
FROM bairros
GROUP BY chave
HAVING COUNT(*) > 1;
```

`associacoes_sem_nome` deve ser zero após o backfill. A verificação de duplicidades deve ser
revisada antes da carga e retornar zero linhas depois, salvo colisões preexistentes que exigem
decisão manual; nunca apagar aliases ou bairros automaticamente. As demais contagens são dados
existentes, não devem ser ajustadas com valores presumidos. O script e seus testes não devem ser
executados em banco compartilhado ou de produção sem plano operacional aprovado.
