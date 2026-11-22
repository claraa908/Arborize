package br.com.arborize.entities;

import br.com.arborize.enums.SubstituicaoBotanicaStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "solicitacoes_substituicao", indexes = {
        @Index(name = "idx_solicitacao_substituicao_status_data", columnList = "status, criada_em")
}, uniqueConstraints = @UniqueConstraint(name = "uk_solicitacao_substituicao_protocolo", columnNames = "protocolo"))
@Getter
@Setter
@NoArgsConstructor
public class SolicitacaoSubstituicao {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 32)
    private String protocolo;

    @Column(name = "nome_solicitante", nullable = false, length = 180)
    private String nomeSolicitante;

    @Column(nullable = false, length = 11)
    private String cpf;

    @Column(nullable = false, length = 11)
    private String telefone;

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

    @Column(name = "descricao_arvore", nullable = false, columnDefinition = "text")
    private String descricaoArvore;

    @Column(nullable = false, length = 40)
    @Enumerated(EnumType.STRING)
    private SubstituicaoBotanicaStatus status;

    @Column(name = "foto_objeto", length = 512)
    private String fotoObjeto;

    @Column(name = "foto_nome_original", length = 255)
    private String fotoNomeOriginal;

    @Column(name = "foto_content_type", length = 100)
    private String fotoContentType;

    @Column(name = "foto_tamanho_bytes")
    private Long fotoTamanhoBytes;

    @Column(name = "criada_em", nullable = false)
    private LocalDateTime criadaEm;

    @Column(name = "atualizada_em", nullable = false)
    private LocalDateTime atualizadaEm;
}
