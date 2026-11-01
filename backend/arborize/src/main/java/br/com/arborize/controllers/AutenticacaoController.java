package br.com.arborize.controllers;

import br.com.arborize.dtos.request.LoginRequestDTO;
import br.com.arborize.dtos.response.LoginResponseDTO;
import br.com.arborize.services.AutenticacaoService;
import br.com.arborize.services.AutenticacaoService.CredenciaisInvalidasException;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AutenticacaoController {

    private final AutenticacaoService service;

    public AutenticacaoController(AutenticacaoService service) {
        this.service = service;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@Valid @RequestBody LoginRequestDTO request) {
        return ResponseEntity.ok(service.login(request));
    }

    @ExceptionHandler(CredenciaisInvalidasException.class)
    public ResponseEntity<String> handleCredenciaisInvalidas() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body("Credenciais inválidas. Tente novamente.");
    }
}
