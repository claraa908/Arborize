package br.com.arborize.repositories;

import br.com.arborize.entities.Arvore;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;

/** Contrato restrito a cadastro e leituras; consultas delegadas ao SimpleJpaRepository. */
@Repository
public interface ArvoreRepository extends org.springframework.data.repository.Repository<Arvore, Long> {
    Arvore save(Arvore arvore);
    void flush();
    Optional<Arvore> findById(Long id);

    /** Serializa uploads e remoções da mesma árvore até o fim da transação. */
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select a from Arvore a where a.id = :id")
    Optional<Arvore> findByIdParaFotografias(@org.springframework.data.repository.query.Param("id") Long id);

    @EntityGraph(attributePaths = {"especie", "bairro"})
    Page<Arvore> findAll(Specification<Arvore> specification, Pageable pageable);

    @EntityGraph(attributePaths = {"especie", "bairro"})
    List<Arvore> findAll(Specification<Arvore> specification, Sort sort);
}
