package br.com.arborize.repositories;

import br.com.arborize.entities.MetaZoneamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MetaZoneamentoRepository extends JpaRepository<MetaZoneamento, Long> {

    @Query("""
            select m from MetaZoneamento m
            where m.ano = :year and m.trimestre = :quarter
              and (:zone is null or lower(m.zona) = lower(cast(:zone as text)))
            order by m.zona, m.bairro
            """)
    List<MetaZoneamento> findForDashboard(
            @Param("zone") String zone,
            @Param("year") int year,
            @Param("quarter") int quarter
    );

    @Query("select distinct m.zona from MetaZoneamento m order by m.zona")
    List<String> findDistinctZones();

    @Query("select distinct m.ano from MetaZoneamento m order by m.ano")
    List<Integer> findDistinctYears();
}