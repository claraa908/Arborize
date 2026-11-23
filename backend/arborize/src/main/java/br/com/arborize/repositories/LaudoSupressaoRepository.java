package br.com.arborize.repositories;

import br.com.arborize.entities.LaudoSupressao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LaudoSupressaoRepository extends JpaRepository<LaudoSupressao, Long> {
    Optional<LaudoSupressao> findBySolicitacaoId(Long solicitacaoId);
    boolean existsBySolicitacaoId(Long solicitacaoId);
}
