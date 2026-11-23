package br.com.arborize.dtos.response;

import br.com.arborize.enums.SubstituicaoBotanicaStatus;

import java.time.LocalDateTime;
import java.util.List;

public record SubstituicaoRastreioResponse(
        String protocolo,
        SubstituicaoBotanicaStatus status,
        LocalDateTime criadaEm,
        String bairro,
        String descricaoArvore,
        String especieReposicao,
        List<SubstituicaoEventoResponse> eventos
) {}
