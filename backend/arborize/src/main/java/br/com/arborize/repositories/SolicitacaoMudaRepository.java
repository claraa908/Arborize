package br.com.arborize.repositories;

import br.com.arborize.entities.SolicitacaoMuda;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface SolicitacaoMudaRepository extends JpaRepository<SolicitacaoMuda, Long> {
    @Query("select coalesce(sum(s.totalRecicaveis), 0) from SolicitacaoMuda s")
    long somarTotalRecicaveis();
}
