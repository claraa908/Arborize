package br.com.arborize.entities;

import br.com.arborize.enums.SolicitacaoMudaStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "solicitacoes_muda")
@Getter
@Setter
@NoArgsConstructor
public class SolicitacaoMuda {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "especie_id", nullable = false)
    private Especie especie;

    @Column(name = "quantidade_mudas", nullable = false)
    private Integer quantidadeMudas;

    @Column(name = "total_recicaveis", nullable = false)
    private Integer totalRecicaveis;

    @Column(nullable = false, length = 180)
    private String logradouro;

    @Column(name = "numero_endereco", nullable = false, length = 30)
    private String numeroEndereco;

    @Column(nullable = false, length = 100)
    private String bairro;

    @Column(length = 180)
    private String complemento;

    @Column(name = "ponto_referencia", nullable = false, length = 255)
    private String pontoReferencia;

    @Column(name = "nome_solicitante", nullable = false, length = 180)
    private String nomeSolicitante;

    @Column(nullable = false, length = 11)
    private String cpf;

    @Column(nullable = false, length = 11)
    private String telefone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private SolicitacaoMudaStatus status;

    @Column(name = "criada_em", nullable = false)
    private LocalDateTime criadaEm;
}
