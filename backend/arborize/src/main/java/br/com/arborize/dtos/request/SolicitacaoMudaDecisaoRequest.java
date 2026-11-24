package br.com.arborize.dtos.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public final class SolicitacaoMudaDecisaoRequest {
    private SolicitacaoMudaDecisaoRequest() {}

    public record Recebimento(
            @NotNull @DecimalMin("0") @DecimalMax("2147483647") @Digits(integer = 10, fraction = 0)
            BigDecimal quantidadeRecebida
    ) {}

    public record Rejeicao(@NotBlank @Size(max = 255) String motivo) {}
}
