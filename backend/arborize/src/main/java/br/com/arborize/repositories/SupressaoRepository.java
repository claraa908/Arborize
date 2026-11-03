package br.com.arborize.repositories;

import br.com.arborize.entities.Supressao;
import br.com.arborize.repositories.projections.ZoneQuantityProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface SupressaoRepository extends JpaRepository<Supressao, Long> {

    @Query("""
            select coalesce(sum(s.quantity), 0)
            from Supressao s, Especie e
            where s.authorized = true
              and lower(s.removedSpecies) = lower(:species)
              and s.replacementSpecies is not null
              and s.replacementSpecies <> ''
              and lower(e.nomeCientifico) = lower(s.replacementSpecies)
              and lower(e.categoria) = lower('Nativa da Caatinga')
            """)
    long sumAuthorizedReplacementsForSpecies(@Param("species") String species);

    @Query("""
            select s.zona as zona, s.bairro as bairro, sum(s.quantity) as quantity
            from Supressao s
            where s.authorized = true and s.occurredAt between :start and :end
            group by s.zona, s.bairro
            """)
    List<ZoneQuantityProjection> sumAuthorizedQuantitiesByZoneAndBairro(
            @Param("start") LocalDate start,
            @Param("end") LocalDate end
    );
}
