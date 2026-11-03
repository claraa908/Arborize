package br.com.arborize.repositories;

import br.com.arborize.entities.EcoPontoTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;

public interface EcoPontoTransactionRepository extends JpaRepository<EcoPontoTransaction, Long> {

    @Query("select coalesce(sum(t.bottlesCollected), 0) from EcoPontoTransaction t where t.confirmed = true")
    long sumConfirmedBottles();

    @Query("""
            select coalesce(sum(t.bottlesCollected), 0)
            from EcoPontoTransaction t
            where t.confirmed = true and t.transactionDate between :start and :end
            """)
    long sumConfirmedBottlesBetween(@Param("start") LocalDate start, @Param("end") LocalDate end);
}
