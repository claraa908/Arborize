BEGIN;

ALTER TABLE arvores
    ADD COLUMN IF NOT EXISTS bairro_nome TEXT;

ALTER TABLE arvores
    ADD COLUMN IF NOT EXISTS cep TEXT;

ALTER TABLE arvores
    ADD COLUMN IF NOT EXISTS localidade_rural TEXT;

UPDATE arvores AS arvore
SET bairro_nome = bairro.nome
FROM bairros AS bairro
WHERE arvore.bairro_id = bairro.id
  AND arvore.bairro_nome IS NULL;

COMMIT;
