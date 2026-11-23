package br.com.arborize.controllers;

import br.com.arborize.dtos.request.AcompanharSubstituicaoRequest;
import br.com.arborize.dtos.request.DecisaoSubstituicaoRequest;
import br.com.arborize.dtos.request.LaudoSubstituicaoRequest;
import br.com.arborize.dtos.request.ProgressoSubstituicaoRequest;
import br.com.arborize.dtos.request.SolicitacaoSubstituicaoRequest;
import br.com.arborize.dtos.response.SubstituicaoGestaoResponse;
import br.com.arborize.dtos.response.SubstituicaoRastreioResponse;
import br.com.arborize.services.SubstituicaoBotanicaService;
import jakarta.validation.Valid;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/substituicoes-botanicas")
@Validated
public class SubstituicaoBotanicaController {
    private final SubstituicaoBotanicaService service;

    public SubstituicaoBotanicaController(SubstituicaoBotanicaService service) {
        this.service = service;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<SubstituicaoRastreioResponse> criar(@Valid @ModelAttribute SolicitacaoSubstituicaoRequest request) {
        SubstituicaoRastreioResponse response = service.criar(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping(path = "/acompanhar", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<SubstituicaoRastreioResponse> acompanhar(@Valid @RequestBody AcompanharSubstituicaoRequest request) {
        return ResponseEntity.ok(service.acompanhar(request));
    }

    @GetMapping("/gestao")
    public ResponseEntity<List<SubstituicaoGestaoResponse>> listar() {
        return ResponseEntity.ok(service.listar());
    }

    @GetMapping("/gestao/{id}")
    public ResponseEntity<SubstituicaoGestaoResponse> buscar(@PathVariable Long id) {
        return ResponseEntity.ok(service.buscar(id));
    }

    @PutMapping("/gestao/{id}/analise")
    public ResponseEntity<SubstituicaoGestaoResponse> iniciarAnalise(
            @PathVariable Long id, @AuthenticationPrincipal Long usuarioId) {
        return ResponseEntity.ok(service.iniciarAnalise(id, usuarioId));
    }

    @PutMapping("/gestao/{id}/decisao")
    public ResponseEntity<SubstituicaoGestaoResponse> decidir(
            @PathVariable Long id,
            @Valid @RequestBody DecisaoSubstituicaoRequest request,
            @AuthenticationPrincipal Long usuarioId) {
        return ResponseEntity.ok(service.decidir(id, request, usuarioId));
    }

    @PostMapping("/gestao/{id}/eventos")
    public ResponseEntity<SubstituicaoGestaoResponse> registrarProgresso(
            @PathVariable Long id,
            @Valid @RequestBody ProgressoSubstituicaoRequest request,
            @AuthenticationPrincipal Long usuarioId) {
        return ResponseEntity.ok(service.registrarProgresso(id, request, usuarioId));
    }

    @PostMapping(path = "/gestao/{id}/laudo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<SubstituicaoGestaoResponse> emitirLaudo(
            @PathVariable Long id,
            @Valid @ModelAttribute LaudoSubstituicaoRequest request,
            @AuthenticationPrincipal Long usuarioId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.emitirLaudo(id, request, usuarioId));
    }

    @GetMapping("/gestao/{id}/laudo/arquivo")
    public ResponseEntity<byte[]> recuperarLaudo(@PathVariable Long id) {
        SubstituicaoGestaoResponse detalhe = service.buscar(id);
        byte[] bytes = service.recuperarLaudo(id);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentLength(bytes.length);
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename(detalhe.nomePdf(), StandardCharsets.UTF_8).build());
        headers.set("X-Content-Type-Options", "nosniff");
        return ResponseEntity.ok().headers(headers).body(bytes);
    }

    @GetMapping("/gestao/{id}/foto")
    public ResponseEntity<byte[]> recuperarFoto(@PathVariable Long id) {
        SubstituicaoBotanicaService.FotoResponse foto = service.recuperarFoto(id);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(foto.contentType()));
        headers.setContentLength(foto.bytes().length);
        headers.set("X-Content-Type-Options", "nosniff");
        headers.set("Cache-Control", "no-store");
        return ResponseEntity.ok().headers(headers).body(foto.bytes());
    }
}
