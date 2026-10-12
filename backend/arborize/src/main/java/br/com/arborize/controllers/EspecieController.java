package br.com.arborize.controllers;

import br.com.arborize.entities.Especie;
import br.com.arborize.services.EspecieService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

@RestController
@RequestMapping("/especies")
@Tag(name = "Espécies", description = "Endpoints do Catálogo de Espécies da Caatinga")
//@CrossOrigin(originPatterns = "*")
public class EspecieController {

    @Autowired
    private EspecieService especieService;

    @PostMapping
    public ResponseEntity<Especie> criarEspecie(@RequestBody Especie especie) {
        try {
            Especie novaEspecie = especieService.salvar(especie);
            return new ResponseEntity<>(novaEspecie, HttpStatus.CREATED);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping
    public ResponseEntity<List<Especie>> listarEspecies() {
        return ResponseEntity.ok(especieService.listarTodas());
    }
}