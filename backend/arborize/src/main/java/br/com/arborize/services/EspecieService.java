package br.com.arborize.services;

import br.com.arborize.entities.Especie;
import br.com.arborize.repositories.EspecieRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EspecieService {

    @Autowired
    private EspecieRepository repository;

    public Especie salvar(Especie especie) {
        return repository.save(especie);
    }

    public List<Especie> listarTodas() {
        return repository.findAll();
    }
}