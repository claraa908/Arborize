package br.com.arborize.dtos.request;

import br.com.arborize.enums.TipoMovimentacaoMuda;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;

public class MovimentacaoMudaRequestDTO {
    @NotNull
    private TipoMovimentacaoMuda tipo;

    @NotNull
    @Positive
    private Long especieId;

    @NotNull
    @Positive
    private Integer quantidade;

    @NotNull
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate data;

    @Size(max = 180)
    private String destino;

    @Size(max = 160)
    private String responsavel;

    @Size(max = 1000)
    private String observacoes;

    private MultipartFile comprovante;

    public TipoMovimentacaoMuda getTipo() { return tipo; }
    public void setTipo(TipoMovimentacaoMuda tipo) { this.tipo = tipo; }
    public Long getEspecieId() { return especieId; }
    public void setEspecieId(Long especieId) { this.especieId = especieId; }
    public Integer getQuantidade() { return quantidade; }
    public void setQuantidade(Integer quantidade) { this.quantidade = quantidade; }
    public LocalDate getData() { return data; }
    public void setData(LocalDate data) { this.data = data; }
    public String getDestino() { return destino; }
    public void setDestino(String destino) { this.destino = destino; }
    public String getResponsavel() { return responsavel; }
    public void setResponsavel(String responsavel) { this.responsavel = responsavel; }
    public String getObservacoes() { return observacoes; }
    public void setObservacoes(String observacoes) { this.observacoes = observacoes; }
    public MultipartFile getComprovante() { return comprovante; }
    public void setComprovante(MultipartFile comprovante) { this.comprovante = comprovante; }
}
