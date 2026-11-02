package br.com.arborize.services;

import br.com.arborize.dtos.request.AlterarSenhaRequestDTO;
import br.com.arborize.dtos.request.UsuarioCadastroRequestDTO;
import br.com.arborize.dtos.request.UsuarioPerfilRequestDTO;
import br.com.arborize.entities.Usuario;
import br.com.arborize.mappers.UsuarioMapper;
import br.com.arborize.repositories.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository repository;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private UsuarioService service;

    @BeforeEach
    void setUp() {
        service = new UsuarioService(repository, passwordEncoder, new UsuarioMapper());
    }

    @Test
    void cadastrarNormalizaDadosEGeraHashDaSenha() {
        var request = new UsuarioCadastroRequestDTO(
                " Ana Silva ",
                "12345678900",
                "ANA@EXAMPLE.COM",
                " (88) 99999-1234 ",
                " Rua A ",
                "senha-segura"
        );
        when(repository.existsByCpf("12345678900")).thenReturn(false);
        when(repository.existsByEmailIgnoreCase("ana@example.com")).thenReturn(false);
        when(repository.saveAndFlush(any(Usuario.class))).thenAnswer(invocation -> {
            Usuario usuario = invocation.getArgument(0);
            usuario.setId(42L);
            return usuario;
        });

        var response = service.cadastrar(request);

        ArgumentCaptor<Usuario> usuarioCaptor = ArgumentCaptor.forClass(Usuario.class);
        verify(repository).saveAndFlush(usuarioCaptor.capture());
        Usuario salvo = usuarioCaptor.getValue();
        assertEquals(42L, response.id());
        assertEquals("12345678900", response.cpf());
        assertEquals("ana@example.com", response.email());
        assertEquals("Ana Silva", response.nome());
        assertFalse(salvo.getSenha().equals(request.senha()));
        assertTrue(passwordEncoder.matches(request.senha(), salvo.getSenha()));
    }

    @Test
    void cadastrarRejeitaCpfDuplicado() {
        var request = new UsuarioCadastroRequestDTO(
                "Ana Silva", "12345678900", "ana@example.com", "88999991234", "Rua A", "senha-segura"
        );
        when(repository.existsByCpf("12345678900")).thenReturn(true);

        var exception = assertThrows(UsuarioService.UsuarioDuplicadoException.class, () -> service.cadastrar(request));

        assertEquals("Já existe um usuário cadastrado com este CPF/E-mail", exception.getMessage());
        verify(repository, never()).saveAndFlush(any(Usuario.class));
    }

    @Test
    void cadastrarRejeitaEmailDuplicado() {
        var request = new UsuarioCadastroRequestDTO(
                "Ana Silva", "12345678900", "ana@example.com", "88999991234", "Rua A", "senha-segura"
        );
        when(repository.existsByCpf("12345678900")).thenReturn(false);
        when(repository.existsByEmailIgnoreCase("ana@example.com")).thenReturn(true);

        assertThrows(UsuarioService.UsuarioDuplicadoException.class, () -> service.cadastrar(request));
        verify(repository, never()).saveAndFlush(any(Usuario.class));
    }

    @Test
    void atualizarPerfilPreservaCpfEmailESenha() {
        Usuario usuario = criarUsuario();
        String senhaAntes = usuario.getSenha();
        when(repository.findById(7L)).thenReturn(Optional.of(usuario));
        when(repository.save(usuario)).thenReturn(usuario);

        var response = service.atualizarPerfil(7L, new UsuarioPerfilRequestDTO("Novo Nome", "88988887777", "Rua B"));

        assertEquals("Novo Nome", response.nome());
        assertEquals("12345678900", response.cpf());
        assertEquals("ana@example.com", response.email());
        assertEquals("88988887777", response.whatsapp());
        assertEquals("Rua B", response.endereco());
        assertEquals(senhaAntes, usuario.getSenha());
    }

    @Test
    void alterarSenhaGeraNovoHashQuandoSenhaAtualConfere() {
        Usuario usuario = criarUsuario();
        usuario.setSenha(passwordEncoder.encode("senha-atual"));
        when(repository.findById(7L)).thenReturn(Optional.of(usuario));
        when(repository.save(usuario)).thenReturn(usuario);

        service.alterarSenha(7L, new AlterarSenhaRequestDTO("senha-atual", "nova-senha-segura"));

        assertTrue(passwordEncoder.matches("nova-senha-segura", usuario.getSenha()));
        assertFalse(passwordEncoder.matches("senha-atual", usuario.getSenha()));
        verify(repository).save(usuario);
    }

    @Test
    void alterarSenhaRejeitaSenhaAtualIncorreta() {
        Usuario usuario = criarUsuario();
        usuario.setSenha(passwordEncoder.encode("senha-correta"));
        when(repository.findById(7L)).thenReturn(Optional.of(usuario));

        assertThrows(
                UsuarioService.SenhaAtualIncorretaException.class,
                () -> service.alterarSenha(7L, new AlterarSenhaRequestDTO("senha-incorreta", "nova-senha-segura"))
        );
        verify(repository, never()).save(any(Usuario.class));
    }

    private Usuario criarUsuario() {
        Usuario usuario = new Usuario();
        usuario.setId(7L);
        usuario.setNome("Ana Silva");
        usuario.setCpf("12345678900");
        usuario.setEmail("ana@example.com");
        usuario.setWhatsapp("(88) 99999-1234");
        usuario.setEndereco("Rua A");
        usuario.setSenha(passwordEncoder.encode("senha-original"));
        return usuario;
    }
}
