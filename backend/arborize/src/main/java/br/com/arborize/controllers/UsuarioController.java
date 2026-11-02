package br.com.arborize.controllers;

import br.com.arborize.dtos.request.UsuarioCadastroRequestDTO;
import br.com.arborize.dtos.request.UsuarioPerfilRequestDTO;
import br.com.arborize.dtos.request.AlterarSenhaRequestDTO;
import br.com.arborize.dtos.response.UsuarioResponseDTO;
import br.com.arborize.services.UsuarioService;
import br.com.arborize.services.UsuarioService.UsuarioDuplicadoException;
import br.com.arborize.services.UsuarioService.SenhaAtualIncorretaException;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/usuarios")
public class UsuarioController {

    private final UsuarioService service;

    public UsuarioController(UsuarioService service) {
        this.service = service;
    }

    @PostMapping("/cadastro")
    public ResponseEntity<UsuarioResponseDTO> cadastrar(@Valid @RequestBody UsuarioCadastroRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.cadastrar(request));
    }

    @GetMapping("/me")
    public ResponseEntity<UsuarioResponseDTO> buscarMeuPerfil(@AuthenticationPrincipal Long usuarioId) {
        return ResponseEntity.ok(service.buscarPerfil(usuarioId));
    }

    @PutMapping("/me")
    public ResponseEntity<UsuarioResponseDTO> atualizarMeuPerfil(
            @AuthenticationPrincipal Long usuarioId,
            @Valid @RequestBody UsuarioPerfilRequestDTO request
    ) {
        return ResponseEntity.ok(service.atualizarPerfil(usuarioId, request));
    }

    @PutMapping("/me/senha")
    public ResponseEntity<Void> alterarMinhaSenha(
            @AuthenticationPrincipal Long usuarioId,
            @Valid @RequestBody AlterarSenhaRequestDTO request
    ) {
        service.alterarSenha(usuarioId, request);
        return ResponseEntity.ok().build();
    }

    @ExceptionHandler(UsuarioDuplicadoException.class)
    public ResponseEntity<String> handleUsuarioDuplicado(UsuarioDuplicadoException exception) {
        return ResponseEntity.badRequest().body(exception.getMessage());
    }

    @ExceptionHandler(SenhaAtualIncorretaException.class)
    public ResponseEntity<String> handleSenhaAtualIncorreta() {
        return ResponseEntity.badRequest().body("A senha atual está incorreta. Tente novamente.");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<String> handleValidacao(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .findFirst()
                .orElse("Por favor, preencha todos os campos obrigatórios");
        return ResponseEntity.badRequest().body(message);
    }
}
