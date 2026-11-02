package br.com.arborize.controllers;

import br.com.arborize.dtos.request.AdminUsuarioRequestDTO;
import br.com.arborize.dtos.response.AdminUsuarioResponseDTO;
import br.com.arborize.services.AdminUsuarioService;
import br.com.arborize.services.AdminUsuarioService.UsuarioInstitucionalDuplicadoException;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/admin/usuarios")
@PreAuthorize("hasRole('ADMINISTRADOR_AMMA')")
public class AdminController {

    private final AdminUsuarioService service;

    public AdminController(AdminUsuarioService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<AdminUsuarioResponseDTO>> listarUsuarios() {
        return ResponseEntity.ok(service.listarUsuariosInstitucionais());
    }

    @PostMapping
    public ResponseEntity<AdminUsuarioResponseDTO> cadastrarUsuario(
            @Valid @RequestBody AdminUsuarioRequestDTO request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.cadastrarUsuarioInstitucional(request));
    }

    @ExceptionHandler(UsuarioInstitucionalDuplicadoException.class)
    public ResponseEntity<String> handleUsuarioDuplicado() {
        return ResponseEntity.badRequest().body("Este E-mail ou CPF já está em uso no sistema.");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<String> handleValidacao(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .findFirst()
                .orElse("Verifique os dados informados.");
        return ResponseEntity.badRequest().body(message);
    }
}
