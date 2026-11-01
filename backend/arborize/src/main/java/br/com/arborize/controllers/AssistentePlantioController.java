package br.com.arborize.controllers;

import br.com.arborize.dtos.request.AssistentePlantioRequest;
import br.com.arborize.dtos.response.EspecieRecomendadaResponse;
import br.com.arborize.services.AssistentePlantioService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/assistente-plantio")
public class AssistentePlantioController {
    private final AssistentePlantioService service;

    public AssistentePlantioController(AssistentePlantioService service) {
        this.service = service;
    }

    @PostMapping("/recomendar")
    public ResponseEntity<List<EspecieRecomendadaResponse>> recomendar(
            @Valid @RequestBody AssistentePlantioRequest request
    ) {
        return ResponseEntity.ok(service.recomendar(request));
    }
}
