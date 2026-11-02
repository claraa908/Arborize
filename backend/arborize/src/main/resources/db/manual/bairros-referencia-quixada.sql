BEGIN;

SELECT pg_advisory_xact_lock(420172, 24);

WITH nomes(nome) AS (
    VALUES
        ('Alto da Boa Vista'),
        ('Alto São Francisco'),
        ('Baviera'),
        ('Bôto'),
        ('Campo Novo'),
        ('Campo Velho'),
        ('Carrascal I'),
        ('Carrascal II'),
        ('Cedro'),
        ('Centro'),
        ('Cohab'),
        ('Combate'),
        ('Curicaca'),
        ('Herval'),
        ('Irajá'),
        ('Jardim dos Monólitos'),
        ('Monte Alegre'),
        ('Nova Jerusalém'),
        ('Planalto Renascer'),
        ('Planalto Universitário'),
        ('Putiú'),
        ('Residencial'),
        ('São João'),
        ('Triângulo')
), normalizados AS (
    SELECT nome,
           translate(
               lower(regexp_replace(btrim(nome), '[[:space:]]+', ' ', 'g')),
               'áàâãäéèêëíìîïóòôõöúùûüç',
               'aaaaaeeeeiiiiooooouuuuc'
           ) AS chave
    FROM nomes
)
INSERT INTO bairros (nome, ativo)
SELECT referencia.nome, TRUE
FROM normalizados AS referencia
WHERE NOT EXISTS (
    SELECT 1
    FROM bairros AS existente
    WHERE translate(
              lower(regexp_replace(btrim(existente.nome), '[[:space:]]+', ' ', 'g')),
              'áàâãäéèêëíìîïóòôõöúùûüç',
              'aaaaaeeeeiiiiooooouuuuc'
          ) = referencia.chave
);

COMMIT;
