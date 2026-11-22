package br.com.arborize.repositories;

import br.com.arborize.entities.SolicitacaoSubstituicaoEvento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SolicitacaoSubstituicaoEventoRepository extends JpaRepository<SolicitacaoSubstituicaoEvento, Long> {
    List<SolicitacaoSubstituicaoEvento> findBySolicitacaoIdAndPublicoTrueOrderByCriadoEmAsc(Long solicitacaoId);
    List<SolicitacaoSubstituicaoEvento> findBySolicitacaoIdOrderByCriadoEmAsc(Long solicitacaoId);
}
