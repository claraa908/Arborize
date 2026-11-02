package br.com.arborize.dtos.request;

import br.com.arborize.enums.ClassificacaoTerritorial;
import br.com.arborize.enums.CondicaoFitossanitaria;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Campos editáveis compartilhados pelos contratos de cadastro e PUT. */
public interface DadosArvoreRequest {
    Long especieId();
    BigDecimal latitude();
    BigDecimal longitude();
    ClassificacaoTerritorial classificacaoTerritorial();
    Boolean zonaRural();
    String localidadeRural();
    Long bairroId();
    String bairroNome();
    String cep();
    String referenciaLocalizacao();
    LocalDate dataPlantio();
    BigDecimal dapCm();
    BigDecimal alturaEstimadaM();
    CondicaoFitossanitaria condicaoFitossanitaria();
    String observacoes();
}
