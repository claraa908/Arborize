package br.com.arborize.repositories;

import br.com.arborize.entities.Bairro;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/** Consulta de referência territorial, sem operações de manutenção do catálogo. */
@Repository
public interface BairroRepository extends org.springframework.data.repository.Repository<Bairro, Long> {
    Optional<Bairro> findById(Long id);

    List<Bairro> findAllByAtivoTrueOrderByNomeAsc();
}
