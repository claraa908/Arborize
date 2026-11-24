package br.com.arborize.dtos.response;

import java.time.LocalDateTime;
import java.util.List;

public record CidadaoAcompanhamentosResponseDTO(
        List<SolicitacaoMudaResumo> solicitacoesMuda,
        List<SubstituicaoResumo> substituicoesBotanicas
) {
    public record SolicitacaoMudaResumo(
            Long id,
            String protocolo,
            String especieNome,
            Integer quantidadeMudas,
            Integer totalRecicaveis,
            Integer quantidadeRecebida,
            String status,
            String motivoRejeicao,
            String logradouro,
            String numeroEndereco,
            String bairro,
            String complemento,
            String pontoReferencia,
            String localPlantio,
            LocalDateTime criadaEm
    ) {
    }

    public record SubstituicaoEventoResumo(
            String status,
            String descricao,
            LocalDateTime criadoEm
    ) {
    }

    public record SubstituicaoResumo(
            Long id,
            String protocolo,
            String status,
            String bairro,
            String logradouro,
            String numeroEndereco,
            String complemento,
            String pontoReferencia,
            String descricaoArvore,
            LocalDateTime criadaEm,
            List<SubstituicaoEventoResumo> historico
    ) {
    }
}
