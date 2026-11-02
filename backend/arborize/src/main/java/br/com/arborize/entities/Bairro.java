package br.com.arborize.entities;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Catálogo territorial cuja fonte e conteúdo dependem de validação pela AMMA. */
@Entity
@Table(name = "bairros")
@Getter
@Setter
@NoArgsConstructor
public class Bairro {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter(AccessLevel.NONE)
    private Long id;

    @Column(nullable = false)
    private String nome;

    // A indisponibilidade preserva referências históricas sem excluir o bairro.
    @Column(nullable = false)
    private Boolean ativo = true;
}
