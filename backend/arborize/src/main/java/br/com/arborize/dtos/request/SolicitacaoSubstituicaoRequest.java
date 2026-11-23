package br.com.arborize.dtos.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
public class SolicitacaoSubstituicaoRequest {
    @NotBlank
    @Size(max = 180)
    private String nomeSolicitante;

    @NotBlank
    private String cpf;

    @NotBlank
    private String telefone;

    @NotBlank
    @Size(max = 180)
    private String logradouro;

    @NotBlank
    @Size(max = 30)
    private String numeroEndereco;

    @NotBlank
    @Size(max = 100)
    private String bairro;

    @Size(max = 180)
    private String complemento;

    @NotBlank
    @Size(max = 255)
    private String pontoReferencia;

    @NotBlank
    @Size(max = 2000)
    private String descricaoArvore;

    private MultipartFile foto;
}
