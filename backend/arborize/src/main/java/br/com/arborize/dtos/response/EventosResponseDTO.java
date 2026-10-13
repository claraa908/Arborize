package br.com.arborize.dtos.response;

import java.util.List;

public record EventosResponseDTO(
        Long id,
        String nome,
        String descricao,
        String dataInicio,
        String dataTermino,
        String localConcentracao,
        String destinoFinal,
        String horarioInicio,
        String horarioTermino,
        String descricaoPontoPartida,
        String descricaoEncerramento,
        List<DocumentResponseDTO> documentos
) {
}