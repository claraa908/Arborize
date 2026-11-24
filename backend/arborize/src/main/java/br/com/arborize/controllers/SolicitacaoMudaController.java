package br.com.arborize.controllers;

import br.com.arborize.dtos.request.SolicitacaoMudaRequest;
import br.com.arborize.dtos.request.AcompanharSolicitacaoMudaRequest;
import br.com.arborize.dtos.response.ConversaoReciclagemResponse;
import br.com.arborize.dtos.response.SolicitacaoMudaResponse;
import br.com.arborize.dtos.response.SolicitacaoMudaGestaoResponse;
import br.com.arborize.dtos.request.SolicitacaoMudaDecisaoRequest;
import br.com.arborize.services.SolicitacaoMudaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/api/solicitacoes-muda")
public class SolicitacaoMudaController {
    private static final String AMMA = "hasAnyRole('ADMINISTRADOR_AMMA','FUNCIONARIO_AMMA')";
    private static final String GESTAO = AMMA + " or hasAnyRole('BOLSISTA_IFCE','GESTOR_IFCE')";
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

    @PostMapping("/acompanhar")
    public ResponseEntity<SolicitacaoMudaResponse> acompanhar(@Valid @RequestBody AcompanharSolicitacaoMudaRequest request) {
        return ResponseEntity.ok(service.acompanhar(request));
    }

    @GetMapping("/gestao")
    @PreAuthorize(GESTAO)
    public SolicitacaoMudaGestaoResponse.Pagina listar(
            @RequestParam(required = false) String protocolo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.listar(protocolo, page, size);
    }

    @GetMapping("/gestao/{protocolo}")
    @PreAuthorize(GESTAO)
    public SolicitacaoMudaGestaoResponse detalhar(@PathVariable String protocolo) {
        return service.detalhar(protocolo);
    }

    @PostMapping("/gestao/{protocolo}/recebimento")
    @PreAuthorize(GESTAO)
    public SolicitacaoMudaGestaoResponse receber(@PathVariable String protocolo,
            @Valid @RequestBody SolicitacaoMudaDecisaoRequest.Recebimento request) {
        return service.registrarRecebimento(protocolo, request.quantidadeRecebida().intValueExact());
    }

    @GetMapping("/gestao/{protocolo}/documento")
    @PreAuthorize(GESTAO)
    public ResponseEntity<byte[]> documento(@PathVariable String protocolo) {
        var retirada = service.emitirDocumento(protocolo);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .cacheControl(CacheControl.noStore())
                .headers(headers -> headers.setContentDisposition(ContentDisposition.attachment()
                        .filename(retirada.protocolo() + ".pdf").build()))
                .body(retirada.pdf());
    }

    @PostMapping("/gestao/{protocolo}/rejeicao")
    @PreAuthorize(AMMA)
    public SolicitacaoMudaGestaoResponse rejeitar(@PathVariable String protocolo,
            @Valid @RequestBody SolicitacaoMudaDecisaoRequest.Rejeicao request) {
        return service.rejeitar(protocolo, request.motivo());
    }
}
