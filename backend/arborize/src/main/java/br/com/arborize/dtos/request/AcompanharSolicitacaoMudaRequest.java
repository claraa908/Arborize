package br.com.arborize.dtos.request;

import br.com.arborize.validation.CpfValido;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record AcompanharSolicitacaoMudaRequest(
        @NotBlank String protocolo,
        @NotBlank @Pattern(regexp = "\\d{11}") @CpfValido String cpf
) {
}
