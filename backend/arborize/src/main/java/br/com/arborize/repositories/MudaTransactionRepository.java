package br.com.arborize.repositories;

import br.com.arborize.entities.MudaTransaction;
import br.com.arborize.repositories.projections.MonthlyQuantityProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface MudaTransactionRepository extends JpaRepository<MudaTransaction, Long> {

    @Query("select coalesce(sum(t.quantity), 0) from MudaTransaction t where t.confirmed = true")
    long sumConfirmedQuantity();

    @Query("""
            select coalesce(sum(t.quantity), 0)
            from MudaTransaction t
            where t.confirmed = true and t.transactionDate between :start and :end
            """)
    long sumConfirmedQuantityBetween(@Param("start") LocalDate start, @Param("end") LocalDate end);

    @Query("""
            select year(t.transactionDate) as year, month(t.transactionDate) as month, sum(t.quantity) as quantity
            from MudaTransaction t
            where t.confirmed = true and t.transactionDate between :start and :end
            group by year(t.transactionDate), month(t.transactionDate)
            """)
    List<MonthlyQuantityProjection> findMonthlyConfirmedQuantities(
            @Param("start") LocalDate start,
            @Param("end") LocalDate end
    );
}
