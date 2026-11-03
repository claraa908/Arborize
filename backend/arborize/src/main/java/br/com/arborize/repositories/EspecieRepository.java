package br.com.arborize.repositories;

import br.com.arborize.entities.Especie;
import br.com.arborize.repositories.projections.SpeciesStockProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EspecieRepository extends JpaRepository<Especie, Long> {

    @Query("select coalesce(sum(e.mudasDisponiveis), 0) from Especie e")
    long sumMudasDisponiveis();

    @Query("""
            select e.nomePopular as species, sum(e.mudasDisponiveis) as quantity
            from Especie e
            where e.mudasDisponiveis > 0
            group by e.nomePopular
            order by sum(e.mudasDisponiveis) desc
            """)
    List<SpeciesStockProjection> findStockBySpecies();
}