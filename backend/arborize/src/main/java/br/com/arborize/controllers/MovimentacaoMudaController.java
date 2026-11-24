package br.com.arborize.controllers;

import br.com.arborize.dtos.request.MovimentacaoMudaRequestDTO;
import br.com.arborize.dtos.response.MovimentacaoMudaResumoResponseDTO;
import br.com.arborize.dtos.response.MovimentacaoMudaResponseDTO;
import br.com.arborize.dtos.response.MovimentacaoMudaPaginaResponseDTO;
import br.com.arborize.enums.TipoMovimentacaoMuda;
import br.com.arborize.exceptions.MovimentacaoMudaInvalidaException;
import br.com.arborize.services.MovimentacaoMudaService;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/viveiro/movimentacoes")
public class MovimentacaoMudaController {
    private static final int TAMANHO_MAXIMO_PAGINA = 100;
    private final MovimentacaoMudaService service;

    public MovimentacaoMudaController(MovimentacaoMudaService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<MovimentacaoMudaPaginaResponseDTO> listar(
            @RequestParam(required = false) TipoMovimentacaoMuda tipo,
            @RequestParam(required = false) Long especieId,
            @RequestParam(required = false) LocalDate dataInicio,
            @RequestParam(required = false) LocalDate dataFim,
            @RequestParam(required = false) String busca,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        if (page < 0 || size < 1 || size > TAMANHO_MAXIMO_PAGINA) {
            throw new MovimentacaoMudaInvalidaException("Informe uma página válida e um tamanho entre 1 e 100.");
        }
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Order.desc("data"), Sort.Order.desc("id")));
        var resultado = service.listar(tipo, especieId, dataInicio, dataFim, busca, pageable);
        return ResponseEntity.ok(new MovimentacaoMudaPaginaResponseDTO(
                resultado.getContent(),
                resultado.getNumber(),
                resultado.getSize(),
                resultado.getTotalElements(),
                resultado.getTotalPages()));
    }

    @GetMapping("/resumo")
    public ResponseEntity<MovimentacaoMudaResumoResponseDTO> resumo() {
        return ResponseEntity.ok(service.resumo());
    }

    @GetMapping("/{id}")
    public ResponseEntity<MovimentacaoMudaResponseDTO> buscar(@PathVariable Long id) {
        return ResponseEntity.ok(service.buscar(id));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MovimentacaoMudaResponseDTO> criar(@Valid @ModelAttribute MovimentacaoMudaRequestDTO request) {
        MovimentacaoMudaResponseDTO criada = service.criar(request);
        var location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(criada.id()).toUri();
        return ResponseEntity.created(location).body(criada);
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MovimentacaoMudaResponseDTO> atualizar(
            @PathVariable Long id,
            @Valid @ModelAttribute MovimentacaoMudaRequestDTO request) {
        return ResponseEntity.ok(service.atualizar(id, request));
    }

    @GetMapping("/{id}/comprovante")
    public ResponseEntity<byte[]> comprovante(@PathVariable Long id) {
        var comprovante = service.buscarComprovante(id);
        MediaType tipo = MediaType.APPLICATION_OCTET_STREAM;
        if (comprovante.contentType() != null) tipo = MediaType.parseMediaType(comprovante.contentType());
        return ResponseEntity.ok()
                .contentType(tipo)
                .contentLength(comprovante.conteudo().length)
                .cacheControl(CacheControl.noStore())
                .header("X-Content-Type-Options", "nosniff")
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline()
                        .filename(comprovante.nomeArquivo(), StandardCharsets.UTF_8).build().toString())
                .body(comprovante.conteudo());
    }
}
