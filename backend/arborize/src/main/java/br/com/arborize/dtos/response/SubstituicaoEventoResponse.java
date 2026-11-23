package br.com.arborize.dtos.response;

import br.com.arborize.enums.SubstituicaoBotanicaStatus;

import java.time.LocalDateTime;

public record SubstituicaoEventoResponse(
        SubstituicaoBotanicaStatus status,
        String descricao,
        LocalDateTime criadoEm
) {}
