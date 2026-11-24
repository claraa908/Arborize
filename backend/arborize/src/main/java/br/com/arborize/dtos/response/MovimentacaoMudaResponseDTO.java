package br.com.arborize.dtos.response;

import br.com.arborize.enums.TipoMovimentacaoMuda;

import java.time.LocalDate;

public record MovimentacaoMudaResponseDTO(
        Long id,
        TipoMovimentacaoMuda tipo,
        Long especieId,
        String especieNomePopular,
        String especieNomeCientifico,
        Integer quantidade,
        LocalDate data,
        String destino,
        String responsavel,
        String observacoes,
        String comprovanteNome
) { }
