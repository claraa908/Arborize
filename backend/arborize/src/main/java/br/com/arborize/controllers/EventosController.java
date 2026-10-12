package br.com.arborize.controllers;

import br.com.arborize.entities.Eventos;
import br.com.arborize.services.EventosService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

@RestController
@RequestMapping("/eventos")
@Tag(name = "Eventos", description = "Endpoints de CRUD para Eventos")
//@CrossOrigin(originPatterns = "*")
public class EventosController {

    @Autowired
    private EventosService service;

    @PostMapping
    public ResponseEntity<Eventos> criarEvento(@RequestBody Eventos evento) {
        try {
            Eventos novoEvento = service.salvar(evento);
            return new ResponseEntity<>(novoEvento, HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping
    public ResponseEntity<List<Eventos>> listarEventos() {
        return ResponseEntity.ok(service.listarTodos());
    }
}