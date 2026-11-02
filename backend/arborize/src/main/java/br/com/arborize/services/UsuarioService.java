package br.com.arborize.services;

import br.com.arborize.dtos.request.UsuarioCadastroRequestDTO;
import br.com.arborize.dtos.request.UsuarioPerfilRequestDTO;
import br.com.arborize.dtos.request.AlterarSenhaRequestDTO;
import br.com.arborize.dtos.response.UsuarioResponseDTO;
import br.com.arborize.mappers.UsuarioMapper;
import br.com.arborize.repositories.UsuarioRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;

@Service
public class UsuarioService {

    private static final String DUPLICATE_MESSAGE = "Já existe um usuário cadastrado com este CPF/E-mail";

    private final UsuarioRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final UsuarioMapper mapper;

    public UsuarioService(UsuarioRepository repository, PasswordEncoder passwordEncoder, UsuarioMapper mapper) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.mapper = mapper;
    }

    @Transactional
    public UsuarioResponseDTO cadastrar(UsuarioCadastroRequestDTO request) {
        String cpf = request.cpf().replaceAll("\\D", "");
        String email = request.email().trim().toLowerCase(Locale.ROOT);

        if (repository.existsByCpf(cpf) || repository.existsByEmailIgnoreCase(email)) {
            throw new UsuarioDuplicadoException(DUPLICATE_MESSAGE);
        }

        var usuario = mapper.toEntity(request, cpf, email, passwordEncoder.encode(request.senha()));

        try {
            return mapper.toResponseDTO(repository.saveAndFlush(usuario));
        } catch (DataIntegrityViolationException exception) {
            throw new UsuarioDuplicadoException(DUPLICATE_MESSAGE, exception);
        }
    }

    @Transactional(readOnly = true)
    public UsuarioResponseDTO buscarPerfil(Long usuarioId) {
        return mapper.toResponseDTO(repository.findById(usuarioId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado.")));
    }

    @Transactional
    public UsuarioResponseDTO atualizarPerfil(Long usuarioId, UsuarioPerfilRequestDTO request) {
        var usuario = repository.findById(usuarioId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado."));
        mapper.updateProfile(request, usuario);
        return mapper.toResponseDTO(repository.save(usuario));
    }

    @Transactional
    public void alterarSenha(Long usuarioId, AlterarSenhaRequestDTO request) {
        var usuario = repository.findById(usuarioId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado."));
        if (!passwordEncoder.matches(request.senhaAtual(), usuario.getSenha())) {
            throw new SenhaAtualIncorretaException();
        }
        usuario.setSenha(passwordEncoder.encode(request.novaSenha()));
        repository.save(usuario);
    }

    public static class UsuarioDuplicadoException extends RuntimeException {
        public UsuarioDuplicadoException(String message) { super(message); }
        public UsuarioDuplicadoException(String message, Throwable cause) { super(message, cause); }
    }

    public static class SenhaAtualIncorretaException extends RuntimeException {
    }
}
