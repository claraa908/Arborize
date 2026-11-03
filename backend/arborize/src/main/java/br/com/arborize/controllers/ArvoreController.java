package br.com.arborize.controllers;

import br.com.arborize.dtos.request.ArvoreCadastroRequestDTO;
import br.com.arborize.dtos.request.ArvoreAtualizacaoRequestDTO;
import br.com.arborize.dtos.response.ArvoreResponseDTO;
import br.com.arborize.dtos.response.ArvorePaginaResponseDTO;
import br.com.arborize.dtos.response.ArvoreResumoResponseDTO;
import br.com.arborize.enums.CondicaoFitossanitaria;
import br.com.arborize.exceptions.EntradaInventarioInvalidaException;
import org.springframework.data.domain.PageRequest;
import java.util.List;
import br.com.arborize.services.ArvoreService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/arvores")
public class ArvoreController {
    // Limites técnicos da tabela; não restringem o universo filtrado do mapa.
    private static final int MAX_PAGE_SIZE = 100;
    private final ArvoreService service;

    public ArvoreController(ArvoreService service) {
        this.service = service;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ArvoreResponseDTO> cadastrar(
            @Valid @RequestBody ArvoreCadastroRequestDTO dto) {
        ArvoreResponseDTO criada = service.criarArvore(dto);
        var location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(criada.id()).toUri();
        return ResponseEntity.created(location).body(criada);
    }

    @GetMapping
    public ResponseEntity<ArvorePaginaResponseDTO> listar(
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "bairro", required = false) String bairro,
            @RequestParam(name = "bairroId", required = false) Long bairroId,
            @RequestParam(name = "especieId", required = false) Long especieId,
            @RequestParam(name = "condicao", required = false) CondicaoFitossanitaria condicao,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        if (page < 0) {
            throw new EntradaInventarioInvalidaException("page", "A página deve ser maior ou igual a zero.");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new EntradaInventarioInvalidaException("size", "O tamanho da página deve estar entre 1 e 100.");
        }
        var resultado = service.listar(search, bairro, bairroId, especieId, condicao, PageRequest.of(page, size));
        return ResponseEntity.ok(new ArvorePaginaResponseDTO(resultado.getContent(), resultado.getNumber(),
                resultado.getSize(), resultado.getTotalElements(), resultado.getTotalPages()));
    }

    @GetMapping("/mapa")
    public ResponseEntity<List<ArvoreResumoResponseDTO>> consultarMapa(
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "bairro", required = false) String bairro,
            @RequestParam(name = "bairroId", required = false) Long bairroId,
            @RequestParam(name = "especieId", required = false) Long especieId,
            @RequestParam(name = "condicao", required = false) CondicaoFitossanitaria condicao) {
        return ResponseEntity.ok(service.consultarMapa(search, bairro, bairroId, especieId, condicao));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ArvoreResponseDTO> atualizar(@PathVariable("id") Long id,
            @Valid @RequestBody ArvoreAtualizacaoRequestDTO dto) {
        return ResponseEntity.ok(service.atualizarArvore(id, dto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ArvoreResponseDTO> buscarPorId(@PathVariable("id") Long id) {
        return ResponseEntity.ok(service.buscarPorId(id));
    }
}
