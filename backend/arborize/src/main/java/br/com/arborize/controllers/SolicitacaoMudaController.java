package br.com.arborize.controllers;

import br.com.arborize.dtos.request.SolicitacaoMudaRequest;
import br.com.arborize.dtos.response.ConversaoReciclagemResponse;
import br.com.arborize.dtos.response.SolicitacaoMudaResponse;
import br.com.arborize.services.SolicitacaoMudaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/api/solicitacoes-muda")
public class SolicitacaoMudaController {
    private final SolicitacaoMudaService service;

    public SolicitacaoMudaController(SolicitacaoMudaService service) {
        this.service = service;
    }

    @GetMapping("/conversao")
    public ResponseEntity<ConversaoReciclagemResponse> calcularConversao(@RequestParam int quantidade) {
        return ResponseEntity.ok(service.calcularConversao(quantidade));
    }

    @PostMapping
    public ResponseEntity<SolicitacaoMudaResponse> criar(@Valid @RequestBody SolicitacaoMudaRequest request) {
        SolicitacaoMudaResponse response = service.criar(request);
        return ResponseEntity.created(URI.create("/api/solicitacoes-muda/" + response.id()))
                .header(HttpHeaders.CONTENT_TYPE, "application/json")
                .body(response);
    }
}
