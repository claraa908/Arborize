package br.com.arborize.repositories;

import br.com.arborize.entities.DocumentEntities;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentRepository extends JpaRepository<DocumentEntities, Long> { }
