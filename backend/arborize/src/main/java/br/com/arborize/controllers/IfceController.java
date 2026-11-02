package br.com.arborize.controllers;

import br.com.arborize.dtos.request.IfceBolsistaRequestDTO;
import br.com.arborize.dtos.response.IfceBolsistaResponseDTO;
import br.com.arborize.services.IfceBolsistaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/ifce/bolsistas")
@PreAuthorize("hasRole('GESTOR_IFCE')")
public class IfceController {

    private final IfceBolsistaService service;

    public IfceController(IfceBolsistaService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<IfceBolsistaResponseDTO>> listarBolsistas() {
        return ResponseEntity.ok(service.listarBolsistas());
    }

    @PostMapping
    public ResponseEntity<IfceBolsistaResponseDTO> cadastrarBolsista(
            @Valid @RequestBody IfceBolsistaRequestDTO request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.cadastrarBolsista(request));
    }

}
