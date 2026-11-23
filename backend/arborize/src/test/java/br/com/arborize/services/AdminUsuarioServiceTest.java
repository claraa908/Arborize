package br.com.arborize.services;

import br.com.arborize.dtos.request.AdminUsuarioRequestDTO;
import br.com.arborize.entities.Usuario;
import br.com.arborize.enums.UsuarioRole;
import br.com.arborize.mappers.UsuarioMapper;
import br.com.arborize.repositories.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminUsuarioServiceTest {

    @Mock
    private UsuarioRepository repository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private AdminUsuarioService service;

    @BeforeEach
    void setUp() {
        service = new AdminUsuarioService(repository, passwordEncoder, new UsuarioMapper());
    }

    @Test
    void cadastraPerfilInstitucionalComDadosNormalizadosESenhaCriptografada() {
        var request = new AdminUsuarioRequestDTO(
                "  Ana Técnica  ", "123.456.789-01", " Ana@Example.COM ", " 85999990000 ",
                "senha-inicial", "FUNCIONARIO_AMMA"
        );
        when(repository.existsByCpf("12345678901")).thenReturn(false);
        when(repository.existsByEmailIgnoreCase("ana@example.com")).thenReturn(false);
        when(passwordEncoder.encode("senha-inicial")).thenReturn("hash-seguro");
        when(repository.saveAndFlush(any(Usuario.class))).thenAnswer(invocation -> {
            Usuario saved = invocation.getArgument(0);
            saved.setId(18L);
            return saved;
        });

        var result = service.cadastrarUsuarioInstitucional(request);

        ArgumentCaptor<Usuario> savedUser = ArgumentCaptor.forClass(Usuario.class);
        verify(repository).saveAndFlush(savedUser.capture());
        assertEquals("12345678901", savedUser.getValue().getCpf());
        assertEquals("ana@example.com", savedUser.getValue().getEmail());
        assertEquals("Ana Técnica", savedUser.getValue().getNome());
        assertEquals("hash-seguro", savedUser.getValue().getPassword());
        assertEquals(UsuarioRole.FUNCIONARIO_AMMA, savedUser.getValue().getRole());
        assertEquals(UsuarioRole.FUNCIONARIO_AMMA, result.role());
        verify(passwordEncoder).encode("senha-inicial");
    }

    @Test
    void rejeitaPapelForaDosDoisPerfisInstitucionaisPermitidos() {
        var request = new AdminUsuarioRequestDTO(
                "Ana", "12345678901", "ana@example.com", "85999990000", "senha", "ADMINISTRADOR_AMMA"
        );

        assertThrows(IllegalArgumentException.class, () -> service.cadastrarUsuarioInstitucional(request));

        verify(repository, never()).saveAndFlush(any());
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    void recusaCpfOuEmailJaCadastradoAntesDeCodificarASenha() {
        var request = new AdminUsuarioRequestDTO(
                "Ana", "12345678901", "ana@example.com", "85999990000", "senha", "GESTOR_IFCE"
        );
        when(repository.existsByCpf("12345678901")).thenReturn(false);
        when(repository.existsByEmailIgnoreCase("ana@example.com")).thenReturn(true);

        assertThrows(
                AdminUsuarioService.UsuarioInstitucionalDuplicadoException.class,
                () -> service.cadastrarUsuarioInstitucional(request)
        );

        verify(repository, never()).saveAndFlush(any());
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    void converteViolacaoDeUnicidadeNaPersistenciaEmDuplicidadeDeUsuario() {
        var request = new AdminUsuarioRequestDTO(
                "Ana", "12345678901", "ana@example.com", "85999990000", "senha", "GESTOR_IFCE"
        );
        when(repository.existsByCpf("12345678901")).thenReturn(false);
        when(repository.existsByEmailIgnoreCase("ana@example.com")).thenReturn(false);
        when(passwordEncoder.encode("senha")).thenReturn("hash");
        when(repository.saveAndFlush(any(Usuario.class)))
                .thenThrow(new DataIntegrityViolationException("unique constraint"));

        assertThrows(
                AdminUsuarioService.UsuarioInstitucionalDuplicadoException.class,
                () -> service.cadastrarUsuarioInstitucional(request)
        );

        verify(repository).saveAndFlush(any(Usuario.class));
    }

    @Test
    void listaSomenteOsPapeisInstitucionaisConfigurados() {
        Usuario usuario = new Usuario();
        usuario.setId(32L);
        usuario.setNome("Gestor de teste");
        usuario.setEmail("gestor@example.com");
        usuario.setRole(UsuarioRole.GESTOR_IFCE);
        when(repository.findByRoleInOrderByNomeAsc(List.of(
                UsuarioRole.FUNCIONARIO_AMMA, UsuarioRole.GESTOR_IFCE
        ))).thenReturn(List.of(usuario));

        var result = service.listarUsuariosInstitucionais();

        verify(repository).findByRoleInOrderByNomeAsc(List.of(
                UsuarioRole.FUNCIONARIO_AMMA, UsuarioRole.GESTOR_IFCE
        ));
        assertEquals(1, result.size());
        assertEquals("Gestor de teste", result.get(0).nome());
        assertEquals(UsuarioRole.GESTOR_IFCE, result.get(0).role());
    }
}
