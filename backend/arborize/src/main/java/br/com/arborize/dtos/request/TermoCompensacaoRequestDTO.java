package br.com.arborize.dtos.request;

import jakarta.validation.constraints.*;
import java.time.LocalDate;

public record TermoCompensacaoRequestDTO(
        @NotBlank @Size(max = 180) String razaoSocial,
        @NotBlank @Pattern(regexp = "[0-9]{14}") String cnpj,
        @Size(max = 60) String numeroTermo,
        LocalDate dataAssinatura,
        LocalDate prazoEntrega,
        @NotNull @Positive Integer quantidadeAcordada
) { }
