package br.com.arborize.services;

import br.com.arborize.entities.Usuario;
import br.com.arborize.repositories.UsuarioRepository;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class UsuarioDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    public UsuarioDetailsService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public Usuario loadUserByUsername(String login) {
        if (login == null || login.isBlank()) {
            throw new UsernameNotFoundException("Usuário não encontrado.");
        }

        String identificador = login.trim();
        return identificador.contains("@")
                ? usuarioRepository.findByEmailIgnoreCase(identificador.toLowerCase(Locale.ROOT))
                    .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado."))
                : usuarioRepository.findByCpf(identificador.replaceAll("\\D", ""))
                    .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado."));
    }
}
