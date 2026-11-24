package br.com.arborize.repositories;

import br.com.arborize.entities.EntregaCompensacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EntregaCompensacaoRepository extends JpaRepository<EntregaCompensacao, Long> {
    List<EntregaCompensacao> findAllByTermoIdOrderByCriadoEmAscIdAsc(Long termoId);

    Optional<EntregaCompensacao> findByIdAndTermoId(Long id, Long termoId);

    @Query("select coalesce(sum(e.quantidade), 0) from EntregaCompensacao e where e.termo.id = :termoId")
    long somarQuantidadesPorTermo(@Param("termoId") Long termoId);
}
