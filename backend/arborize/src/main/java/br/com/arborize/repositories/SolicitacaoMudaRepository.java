package br.com.arborize.repositories;

import br.com.arborize.entities.SolicitacaoMuda;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface SolicitacaoMudaRepository extends JpaRepository<SolicitacaoMuda, Long> {
    @Override
    @EntityGraph(attributePaths = "especie")
    Page<SolicitacaoMuda> findAll(Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from SolicitacaoMuda s where s.id = :id")
    Optional<SolicitacaoMuda> buscarParaDecisao(@Param("id") long id);
}
