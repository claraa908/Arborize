package br.com.arborize.dtos.request;

import br.com.arborize.validation.CpfValido;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record SolicitacaoMudaRequest(
        @NotNull @Positive Long especieId,
        @NotNull @Min(1) @Max(3) Integer quantidadeMudas,
        @NotBlank @Size(max = 180) String logradouro,
        @NotBlank @Size(max = 30) String numeroEndereco,
        @NotBlank @Size(max = 100) String bairro,
        @Size(max = 180) String complemento,
        @NotBlank @Size(max = 255) String pontoReferencia,
        @NotBlank @Size(max = 180)
        @Pattern(regexp = "^\\s*\\S+(?:\\s+\\S+)+\\s*$", message = "Informe seu nome completo.")
        String nomeSolicitante,
        @NotBlank @Pattern(regexp = "\\d{11}") @CpfValido String cpf,
        @NotBlank @Pattern(regexp = "\\d{10,11}", message = "Informe um telefone com DDD.") String telefone
) {
}
