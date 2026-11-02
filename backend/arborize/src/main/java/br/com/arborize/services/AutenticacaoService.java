package br.com.arborize.services;

import br.com.arborize.dtos.request.LoginRequestDTO;
import br.com.arborize.dtos.response.LoginResponseDTO;
import br.com.arborize.entities.Usuario;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AutenticacaoService {

    private final UsuarioDetailsService usuarioDetailsService;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    public AutenticacaoService(
            UsuarioDetailsService usuarioDetailsService,
            PasswordEncoder passwordEncoder,
            TokenService tokenService
    ) {
        this.usuarioDetailsService = usuarioDetailsService;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
    }

    public LoginResponseDTO login(LoginRequestDTO request) {
        Usuario usuario;
        try {
            usuario = usuarioDetailsService.loadUserByUsername(request.login());
        } catch (UsernameNotFoundException exception) {
            throw new CredenciaisInvalidasException();
        }

        if (!passwordEncoder.matches(request.senha(), usuario.getPassword())) {
            throw new CredenciaisInvalidasException();
        }

        return new LoginResponseDTO(
                tokenService.gerarToken(usuario),
                "Bearer",
                usuario.getNome(),
                usuario.getRole().getAuthority()
        );
    }

    public static class CredenciaisInvalidasException extends RuntimeException {
    }
}
