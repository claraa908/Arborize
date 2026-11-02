package br.com.arborize.services;

import br.com.arborize.dtos.request.IfceBolsistaRequestDTO;
import br.com.arborize.dtos.response.IfceBolsistaResponseDTO;
import br.com.arborize.enums.UsuarioRole;
import br.com.arborize.mappers.UsuarioMapper;
import br.com.arborize.repositories.UsuarioRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
public class IfceBolsistaService {

    private static final List<UsuarioRole> ROLES_BOLSISTAS = List.of(
            UsuarioRole.BOLSISTA_IFCE,
            UsuarioRole.IFCE_BOLSISTA
    );

    private final UsuarioRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final UsuarioMapper mapper;

    public IfceBolsistaService(
            UsuarioRepository repository,
            PasswordEncoder passwordEncoder,
            UsuarioMapper mapper
    ) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<IfceBolsistaResponseDTO> listarBolsistas() {
        return repository.findByRoleInOrderByNomeAsc(ROLES_BOLSISTAS).stream()
                .map(mapper::toIfceBolsistaResponseDTO)
                .toList();
    }

    @Transactional
    public IfceBolsistaResponseDTO cadastrarBolsista(IfceBolsistaRequestDTO request) {
        String cpf = request.cpf().replaceAll("\\D", "");
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (repository.existsByCpf(cpf) || repository.existsByEmailIgnoreCase(email)) {
            throw new BolsistaDuplicadoException();
        }

        var bolsista = mapper.toIfceBolsistaEntity(
                request,
                cpf,
                email,
                passwordEncoder.encode(request.senha())
        );

        try {
            return mapper.toIfceBolsistaResponseDTO(repository.saveAndFlush(bolsista));
        } catch (DataIntegrityViolationException exception) {
            throw new BolsistaDuplicadoException();
        }
    }

    public static class BolsistaDuplicadoException extends RuntimeException {
    }
}
