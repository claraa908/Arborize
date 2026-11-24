package br.com.arborize.repositories;

import br.com.arborize.entities.TermoCompensacao;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TermoCompensacaoRepository extends JpaRepository<TermoCompensacao, Long> {
    List<TermoCompensacao> findAllByOrderByCriadoEmDescIdDesc();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from TermoCompensacao t where t.id = :id")
    Optional<TermoCompensacao> findByIdForUpdate(@Param("id") Long id);
}
