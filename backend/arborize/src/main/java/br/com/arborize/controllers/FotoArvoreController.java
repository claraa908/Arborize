package br.com.arborize.controllers;

import br.com.arborize.dtos.response.FotoArvoreResponseDTO;
import br.com.arborize.services.FotoArvoreService;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import java.util.List;

@RestController
@RequestMapping("/api/arvores/{id}/fotos")
public class FotoArvoreController {
    private final FotoArvoreService service;

    public FotoArvoreController(FotoArvoreService service) {
        this.service = service;
    }

    /** Multipart com uma fotografia por requisição, no campo obrigatório {@code file}. */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<FotoArvoreResponseDTO> adicionar(@PathVariable("id") Long id,
            @RequestParam("file") MultipartFile file) {
        var foto = service.adicionar(id, file);
        var location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{fotoId}").buildAndExpand(foto.id()).toUri();
        return ResponseEntity.created(location).body(foto);
    }

    /** Consulta complementar para a galeria; retorna somente metadados administrativos. */
    @GetMapping
    public ResponseEntity<List<FotoArvoreResponseDTO>> listar(@PathVariable("id") Long id) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.listar(id));
    }

    @GetMapping("/{fotoId}")
    public ResponseEntity<byte[]> recuperar(@PathVariable("id") Long id,
            @PathVariable("fotoId") Long fotoId) {
        var foto = service.recuperar(id, fotoId);
        byte[] conteudo = foto.conteudo();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(foto.metadados().contentType()))
                .contentLength(conteudo.length)
                .cacheControl(CacheControl.noStore())
                .header("X-Content-Type-Options", "nosniff")
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline()
                        .filename("arvore-" + id + "-foto-" + fotoId).build().toString())
                .body(conteudo);
    }

    @DeleteMapping("/{fotoId}")
    public ResponseEntity<Void> remover(@PathVariable("id") Long id,
            @PathVariable("fotoId") Long fotoId) {
        service.remover(id, fotoId);
        return ResponseEntity.noContent().build();
    }
}
