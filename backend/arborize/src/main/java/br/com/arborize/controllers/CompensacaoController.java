package br.com.arborize.controllers;

import br.com.arborize.dtos.request.CompensacaoStatusRequestDTO;
import br.com.arborize.dtos.request.TermoCompensacaoRequestDTO;
import br.com.arborize.dtos.response.CompensacaoResponseDTO;
import br.com.arborize.services.CompensacaoService;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/termos-compensacao")
@PreAuthorize("hasAnyRole('FUNCIONARIO_AMMA', 'ADMINISTRADOR_AMMA')")
public class CompensacaoController {
    private final CompensacaoService service;

    public CompensacaoController(CompensacaoService service) { this.service = service; }

    @GetMapping
    public List<CompensacaoResponseDTO> listar() { return service.listar(); }

    @GetMapping("/{id}")
    public CompensacaoResponseDTO consultar(@PathVariable long id) { return service.consultar(id); }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CompensacaoResponseDTO> criar(@RequestPart("dados") @Valid TermoCompensacaoRequestDTO dados,
            @RequestPart("arquivo") MultipartFile arquivo) {
        return ResponseEntity.status(201).body(service.criar(dados, arquivo));
    }

    @PutMapping("/{id}")
    public CompensacaoResponseDTO atualizar(@PathVariable long id, @Valid @RequestBody TermoCompensacaoRequestDTO dados) {
        return service.atualizar(id, dados);
    }

    @PatchMapping("/{id}/status")
    public CompensacaoResponseDTO atualizarStatus(@PathVariable long id, @Valid @RequestBody CompensacaoStatusRequestDTO dados) {
        return service.atualizarStatus(id, dados.status());
    }

    @PostMapping(value = "/{id}/entregas", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public CompensacaoResponseDTO adicionarEntrega(@PathVariable long id, @RequestParam Integer quantidade,
            @RequestPart("recibo") MultipartFile recibo) {
        return service.adicionarEntrega(id, quantidade, recibo);
    }

    @GetMapping("/{id}/documento")
    public ResponseEntity<byte[]> termo(@PathVariable long id) {
        var documento = service.recuperarTermo(id);
        return respostaArquivo(documento.conteudo(), documento.contentType(), documento.nome());
    }

    @GetMapping("/{id}/entregas/{entregaId}/documento")
    public ResponseEntity<byte[]> recibo(@PathVariable long id, @PathVariable long entregaId) {
        var documento = service.recuperarRecibo(id, entregaId);
        return respostaArquivo(documento.conteudo(), documento.contentType(), documento.nome());
    }

    private ResponseEntity<byte[]> respostaArquivo(byte[] bytes, String contentType, String nome) {
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(contentType)).contentLength(bytes.length)
                .cacheControl(CacheControl.noStore()).header("X-Content-Type-Options", "nosniff")
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(nome, StandardCharsets.UTF_8).build().toString()).body(bytes);
    }
}
