package br.com.arborize.services;

import br.com.arborize.dtos.request.LoginRequestDTO;
import br.com.arborize.dtos.response.LoginResponseDTO;
import br.com.arborize.entities.Usuario;
import br.com.arborize.repositories.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class AutenticacaoService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    public AutenticacaoService(
            UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder,
            TokenService tokenService
    ) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
    }

    public LoginResponseDTO login(LoginRequestDTO request) {
        String identificador = request.login().trim();
        Usuario usuario = identificador.contains("@")
                ? usuarioRepository.findByEmailIgnoreCase(identificador.toLowerCase(Locale.ROOT)).orElse(null)
                : usuarioRepository.findByCpf(identificador.replaceAll("\\D", "")).orElse(null);

        if (usuario == null || !passwordEncoder.matches(request.senha(), usuario.getSenha())) {
            throw new CredenciaisInvalidasException();
        }

        return new LoginResponseDTO(tokenService.gerarToken(usuario), "Bearer", usuario.getNome());
    }

    public static class CredenciaisInvalidasException extends RuntimeException {
    }
}
