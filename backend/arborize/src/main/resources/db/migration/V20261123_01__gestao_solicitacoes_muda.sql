ALTER TABLE solicitacoes_muda
    ADD COLUMN local_plantio varchar(20),
    ADD COLUMN quantidade_recebida integer,
    ADD COLUMN motivo_rejeicao varchar(255),
    DROP CONSTRAINT IF EXISTS solicitacoes_muda_status_check,
    ADD CONSTRAINT solicitacoes_muda_local_plantio_check
        CHECK (local_plantio IN ('CALCADA', 'QUINTAL')),
    ADD CONSTRAINT solicitacoes_muda_status_check
        CHECK (status IN ('PENDENTE_RECICLAVEIS', 'APROVADA', 'RETIRADA_CONCLUIDA', 'REJEITADA'));
