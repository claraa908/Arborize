package br.com.arborize.dtos.response;

import br.com.arborize.enums.SubstituicaoBotanicaStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record SubstituicaoGestaoResponse(
        Long id,
        String protocolo,
        String nomeSolicitante,
        String cpf,
        String telefone,
        String logradouro,
        String numeroEndereco,
        String bairro,
        String complemento,
        String pontoReferencia,
        String descricaoArvore,
        String nomeFoto,
        SubstituicaoBotanicaStatus status,
        LocalDateTime criadaEm,
        LocalDateTime atualizadaEm,
        String parecer,
        String tipoJustificativa,
        String justificativa,
        String especieReposicao,
        LocalDate dataVistoria,
        String nomePdf
) {}
