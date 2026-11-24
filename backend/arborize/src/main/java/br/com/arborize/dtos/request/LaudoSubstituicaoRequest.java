package br.com.arborize.dtos.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;

@Getter
@Setter
public class LaudoSubstituicaoRequest {
    @NotNull
    @Positive
    private Long especieReposicaoId;

    @NotBlank
    @Size(max = 80)
    private String parecer;

    @NotBlank
    @Size(max = 80)
    private String tipoJustificativa;

    @NotBlank
    @Size(max = 5000)
    private String justificativa;

    @NotNull
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dataVistoria;

    @NotNull
    private MultipartFile arquivo;
}
