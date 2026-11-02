package br.com.arborize.services;

import br.com.arborize.dtos.request.AdminUsuarioRequestDTO;
import br.com.arborize.dtos.response.AdminUsuarioResponseDTO;
import br.com.arborize.entities.Usuario;
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
public class AdminUsuarioService {

    private static final List<UsuarioRole> PERFIS_INSTITUCIONAIS = List.of(
            UsuarioRole.FUNCIONARIO_AMMA,
            UsuarioRole.GESTOR_IFCE,
            UsuarioRole.IFCE_GESTOR
    );

    private final UsuarioRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final UsuarioMapper mapper;

    public AdminUsuarioService(
            UsuarioRepository repository,
            PasswordEncoder passwordEncoder,
            UsuarioMapper mapper
    ) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<AdminUsuarioResponseDTO> listarUsuariosInstitucionais() {
        return repository.findByRoleInOrderByNomeAsc(PERFIS_INSTITUCIONAIS).stream()
                .map(mapper::toAdminResponseDTO)
                .toList();
    }

    @Transactional
    public AdminUsuarioResponseDTO cadastrarUsuarioInstitucional(AdminUsuarioRequestDTO request) {
        UsuarioRole role = UsuarioRole.valueOf(request.role());
        if (role != UsuarioRole.FUNCIONARIO_AMMA && role != UsuarioRole.GESTOR_IFCE) {
            throw new IllegalArgumentException("Selecione um perfil institucional válido.");
        }

        String cpf = request.cpf().replaceAll("\\D", "");
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (repository.existsByCpf(cpf) || repository.existsByEmailIgnoreCase(email)) {
            throw new UsuarioInstitucionalDuplicadoException();
        }

        Usuario usuario = mapper.toInstitutionalEntity(
                request,
                cpf,
                email,
                passwordEncoder.encode(request.senha()),
                role
        );

        try {
            return mapper.toAdminResponseDTO(repository.saveAndFlush(usuario));
        } catch (DataIntegrityViolationException exception) {
            throw new UsuarioInstitucionalDuplicadoException();
        }
    }

    public static class UsuarioInstitucionalDuplicadoException extends RuntimeException {
    }
}
