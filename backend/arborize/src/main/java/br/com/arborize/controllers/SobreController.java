package br.com.arborize.controllers;

import br.com.arborize.dtos.response.SobreIndicadoresResponseDTO;
import br.com.arborize.services.SobreIndicadoresService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sobre")
@Tag(name = "Sobre Nós", description = "Indicadores ambientais públicos exibidos na página institucional da AMMA")
public class SobreController {

    private final SobreIndicadoresService sobreIndicadoresService;

    public SobreController(SobreIndicadoresService sobreIndicadoresService) {
        this.sobreIndicadoresService = sobreIndicadoresService;
    }

    @GetMapping("/indicadores")
    @Operation(summary = "Obtém os indicadores ambientais públicos da página Sobre Nós")
    public ResponseEntity<SobreIndicadoresResponseDTO> obterIndicadores() {
        return ResponseEntity.ok(sobreIndicadoresService.obterIndicadores());
    }
}
