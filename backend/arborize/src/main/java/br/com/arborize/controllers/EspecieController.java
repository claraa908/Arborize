package br.com.arborize.controllers;

import br.com.arborize.dtos.request.EspecieRequestDTO;
import br.com.arborize.dtos.response.EspecieResponseDTO;
import br.com.arborize.services.EspecieService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/especies")
public class EspecieController {

    @Autowired
    private EspecieService service;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> criarEspecie(@ModelAttribute EspecieRequestDTO dto) {
        try {
            EspecieResponseDTO novaEspecie = service.criarEspecie(dto);
            return ResponseEntity.status(HttpStatus.CREATED).body(novaEspecie);
        } catch (Exception e) {
            e.printStackTrace(); // Mantemos isto para debug fácil no terminal!
            return ResponseEntity.badRequest().body("Falha ao salvar espécie: " + e.getMessage());
        }
    }

    @GetMapping
    public ResponseEntity<List<EspecieResponseDTO>> listarEspecies() {
        return ResponseEntity.ok(service.listarTodos());
    }
}