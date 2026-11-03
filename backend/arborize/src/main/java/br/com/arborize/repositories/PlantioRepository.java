package br.com.arborize.repositories;

import br.com.arborize.entities.Plantio;
import br.com.arborize.repositories.projections.ZoneQuantityProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface PlantioRepository extends JpaRepository<Plantio, Long> {

    @Query("select coalesce(sum(p.quantity), 0) from Plantio p where p.confirmed = true")
    long sumConfirmedQuantity();

    @Query("""
            select coalesce(sum(p.quantity), 0)
            from Plantio p
            where p.confirmed = true and p.occurredAt between :start and :end
            """)
    long sumConfirmedQuantityBetween(@Param("start") LocalDate start, @Param("end") LocalDate end);

    @Query("""
            select year(p.occurredAt) as year, month(p.occurredAt) as month, sum(p.quantity) as quantity
            from Plantio p
            where p.confirmed = true and p.occurredAt between :start and :end
            group by year(p.occurredAt), month(p.occurredAt)
            """)
    List<br.com.arborize.repositories.projections.MonthlyQuantityProjection> findMonthlyConfirmedQuantities(
            @Param("start") LocalDate start,
            @Param("end") LocalDate end
    );

    @Query("""
            select p.zona as zona, p.bairro as bairro, sum(p.quantity) as quantity
            from Plantio p
            where p.confirmed = true and p.occurredAt between :start and :end
            group by p.zona, p.bairro
            """)
    List<ZoneQuantityProjection> sumConfirmedQuantitiesByZoneAndBairro(
            @Param("start") LocalDate start,
            @Param("end") LocalDate end
    );
}
