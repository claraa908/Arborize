package br.com.arborize.services;

import br.com.arborize.entities.Eventos;
import br.com.arborize.repositories.EventosRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EventosService {

    @Autowired
    private EventosRepository repository;

    public Eventos salvar(Eventos evento) {
        return repository.save(evento);
    }

    public List<Eventos> listarTodos() {
        return repository.findAll();
    }
}