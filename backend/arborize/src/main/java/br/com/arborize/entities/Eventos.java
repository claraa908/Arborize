package br.com.arborize.entities;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "eventos")
public class Eventos {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false, length = 1000)
    private String descricao;

    // O banner geralmente é guardado como uma string (URL ou caminho do ficheiro) no banco de dados.
    // O upload do ficheiro real é feito separadamente para um serviço de armazenamento (como AWS S3 ou MinIO).
    //private String bannerUrl;

    @Column(nullable = false)
    private LocalDate dataInicio;

    @Column(nullable = false)
    private LocalDate dataTermino;

    @Column(nullable = false)
    private String localConcentracao;

    private String destinoFinal;

    @Column(nullable = false)
    private LocalTime horarioInicio;

    @Column(nullable = false)
    private LocalTime horarioTermino;

    private String descricaoPontoPartida;

    private String descricaoEncerramento;

    @OneToMany(mappedBy = "eventos", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DocumentEntities> documents = new ArrayList<>();


    // Construtor vazio
    public Eventos() {}

    // Getters e Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }

    //public String getBannerUrl() { return bannerUrl; }
    //public void setBannerUrl(String bannerUrl) { this.bannerUrl = bannerUrl; }

    public LocalDate getDataInicio() { return dataInicio; }
    public void setDataInicio(LocalDate dataInicio) { this.dataInicio = dataInicio; }

    public LocalDate getDataTermino() { return dataTermino; }
    public void setDataTermino(LocalDate dataTermino) { this.dataTermino = dataTermino; }

    public String getLocalConcentracao() { return localConcentracao; }
    public void setLocalConcentracao(String localConcentracao) { this.localConcentracao = localConcentracao; }

    public String getDestinoFinal() { return destinoFinal; }
    public void setDestinoFinal(String destinoFinal) { this.destinoFinal = destinoFinal; }

    public LocalTime getHorarioInicio() { return horarioInicio; }
    public void setHorarioInicio(LocalTime horarioInicio) { this.horarioInicio = horarioInicio; }

    public LocalTime getHorarioTermino() { return horarioTermino; }
    public void setHorarioTermino(LocalTime horarioTermino) { this.horarioTermino = horarioTermino; }

    public String getDescricaoPontoPartida() { return descricaoPontoPartida; }
    public void setDescricaoPontoPartida(String descricaoPontoPartida) { this.descricaoPontoPartida = descricaoPontoPartida; }

    public String getDescricaoEncerramento() { return descricaoEncerramento; }
    public void setDescricaoEncerramento(String descricaoEncerramento) { this.descricaoEncerramento = descricaoEncerramento; }

    public List<DocumentEntities> getDocuments() {
        return documents;
    }

    public void setDocuments(List<DocumentEntities> documents) {
        this.documents = documents;
    }
}