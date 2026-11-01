package br.com.arborize.repositories;

import br.com.arborize.entities.DocumentEntities;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DocumentRepository extends JpaRepository<DocumentEntities, Long> {
}
