package br.com.arborize.repositories;

import br.com.arborize.entities.FotoArvore;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface FotoArvoreRepository extends org.springframework.data.repository.Repository<FotoArvore, Long> {
    interface FotoCapaProjecao {
        Long getArvoreId();
        Long getId();
        String getContentType();
    }

    @Query("select f.arvore.id as arvoreId, f.id as id, f.contentType as contentType " +
            "from FotoArvore f where f.arvore.id in :arvoreIds and f.id = " +
            "(select min(f2.id) from FotoArvore f2 where f2.arvore.id = f.arvore.id)")
    List<FotoCapaProjecao> findCapasByArvoreIds(@Param("arvoreIds") Collection<Long> arvoreIds);

    long countByArvoreId(Long arvoreId);
    List<FotoArvore> findAllByArvoreIdOrderByIdAsc(Long arvoreId);
    Optional<FotoArvore> findByIdAndArvoreId(Long id, Long arvoreId);
    FotoArvore saveAndFlush(FotoArvore foto);
    void delete(FotoArvore foto);
    void flush();
}
