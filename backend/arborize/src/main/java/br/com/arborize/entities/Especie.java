package br.com.arborize.entities;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "especies")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Especie {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nomePopular;

    @Column(nullable = false)
    private String nomeCientifico;

    private String familiaBotanica;

    @Column(nullable = false)
    private String categoria;

    @Column(nullable = false)
    private String porteArvore;

    @Column(nullable = false)
    private Boolean recomendadaCalcadas;

    private Integer tempoGerminacaoDias;

    @Column(columnDefinition = "TEXT")
    private String descricaoCultivo;

    // Guardará o link público gerado pelo MinIO
    //private String imagemUrl;
}