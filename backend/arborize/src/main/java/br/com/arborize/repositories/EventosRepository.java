package br.com.arborize.repositories;

import br.com.arborize.entities.Eventos;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventosRepository extends JpaRepository<Eventos, Long> {
}