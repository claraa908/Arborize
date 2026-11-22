package br.com.arborize.dtos.response;

import br.com.arborize.enums.CompensacaoStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record CompensacaoResponseDTO(Long id, String razaoSocial, String cnpj, String numeroTermo,
        LocalDate dataAssinatura, LocalDate prazoEntrega, int quantidadeAcordada, long quantidadeEntregue,
        CompensacaoStatus status, String arquivoTermo, LocalDateTime criadoEm,
        List<EntregaResponseDTO> entregas) {
    public record EntregaResponseDTO(Long id, int quantidade, String arquivoNome, String contentType,
            long tamanhoBytes, LocalDateTime registradaEm) { }
}
