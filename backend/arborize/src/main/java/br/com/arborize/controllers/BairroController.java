package br.com.arborize.controllers;

import br.com.arborize.dtos.response.BairroResumoResponseDTO;
import br.com.arborize.services.BairroService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/bairros")
public class BairroController {
    private final BairroService service;

    public BairroController(BairroService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<BairroResumoResponseDTO>> listarDisponiveis() {
        return ResponseEntity.ok(service.listarDisponiveis());
    }
}
