package br.com.arborize.dtos.request;

import org.springframework.web.multipart.MultipartFile;

public record EventosRequestDTO(
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
        String status,
        MultipartFile banner
) {
}