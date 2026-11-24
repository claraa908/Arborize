package br.com.arborize.repositories;

import br.com.arborize.entities.SolicitacaoSubstituicao;
import br.com.arborize.enums.SubstituicaoBotanicaStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SolicitacaoSubstituicaoRepository extends JpaRepository<SolicitacaoSubstituicao, Long> {
    Optional<SolicitacaoSubstituicao> findByProtocoloAndCpf(String protocolo, String cpf);

    List<SolicitacaoSubstituicao> findAllByOrderByCriadaEmDesc();

    List<SolicitacaoSubstituicao> findByStatusOrderByCriadaEmDesc(SubstituicaoBotanicaStatus status);

    List<SolicitacaoSubstituicao> findByCpfOrderByCriadaEmDesc(String cpf);

    long countByStatus(SubstituicaoBotanicaStatus status);
}
