package br.com.arborize.repositories;

import br.com.arborize.entities.MovimentacaoMuda;
import br.com.arborize.enums.TipoMovimentacaoMuda;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;

public interface MovimentacaoMudaRepository extends JpaRepository<MovimentacaoMuda, Long>, JpaSpecificationExecutor<MovimentacaoMuda> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from MovimentacaoMuda m where m.id = :id")
    Optional<MovimentacaoMuda> findByIdForUpdate(@Param("id") Long id);

    @Query("select coalesce(sum(m.quantidade), 0) from MovimentacaoMuda m where m.tipo = :tipo and m.data between :inicio and :fim")
    long somarQuantidadeNoPeriodo(
            @Param("tipo") TipoMovimentacaoMuda tipo,
            @Param("inicio") LocalDate inicio,
            @Param("fim") LocalDate fim);

    @Query("select coalesce(sum(m.quantidade), 0) from MovimentacaoMuda m where m.tipo = :tipo")
    long somarQuantidadePorTipo(@Param("tipo") TipoMovimentacaoMuda tipo);

    @Override
    Page<MovimentacaoMuda> findAll(Specification<MovimentacaoMuda> specification, Pageable pageable);
}
