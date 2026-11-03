package br.com.arborize.repositories.projections;

public interface MonthlyQuantityProjection {
    Integer getYear();
    Integer getMonth();
    Long getQuantity();
}
