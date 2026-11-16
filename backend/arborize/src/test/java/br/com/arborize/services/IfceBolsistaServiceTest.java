package br.com.arborize.services;

import br.com.arborize.dtos.request.IfceBolsistaRequestDTO;
import br.com.arborize.entities.Usuario;
import br.com.arborize.enums.UsuarioRole;
import br.com.arborize.mappers.UsuarioMapper;
import br.com.arborize.repositories.UsuarioRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
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
class IfceBolsistaServiceTest {

    @Mock
    private UsuarioRepository repository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private IfceBolsistaService service;

    @BeforeEach
    void setUp() {
        service = new IfceBolsistaService(repository, passwordEncoder, new UsuarioMapper());
    }

    @Test
    void ignoraRoleInjetadaNoJsonEForcaBolsistaIfce() throws Exception {
        String payload = """
                {"nome":"  Joana  ","cpf":"123.456.789-01","email":" Joana@Example.COM ",
                 "whatsapp":" 85999990000 ","senha":"senha-inicial","role":"ADMINISTRADOR_AMMA"}
                """;
        IfceBolsistaRequestDTO request = new ObjectMapper().readValue(payload, IfceBolsistaRequestDTO.class);
        when(repository.existsByCpf("12345678901")).thenReturn(false);
        when(repository.existsByEmailIgnoreCase("joana@example.com")).thenReturn(false);
        when(passwordEncoder.encode("senha-inicial")).thenReturn("hash-seguro");
        when(repository.saveAndFlush(any(Usuario.class))).thenAnswer(invocation -> {
            Usuario saved = invocation.getArgument(0);
            saved.setId(27L);
            return saved;
        });

        var result = service.cadastrarBolsista(request);

        ArgumentCaptor<Usuario> savedUser = ArgumentCaptor.forClass(Usuario.class);
        verify(repository).saveAndFlush(savedUser.capture());
        assertEquals(UsuarioRole.BOLSISTA_IFCE, savedUser.getValue().getRole());
        assertEquals("Joana", savedUser.getValue().getNome());
        assertEquals("12345678901", savedUser.getValue().getCpf());
        assertEquals("joana@example.com", savedUser.getValue().getEmail());
        assertEquals("hash-seguro", savedUser.getValue().getPassword());
        assertEquals("Joana", result.nome());
    }

    @Test
    void recusaCpfOuEmailDuplicadoSemSalvar() {
        var request = new IfceBolsistaRequestDTO(
                "Joana", "12345678901", "joana@example.com", "85999990000", "senha"
        );
        when(repository.existsByCpf("12345678901")).thenReturn(true);

        assertThrows(
                IfceBolsistaService.BolsistaDuplicadoException.class,
                () -> service.cadastrarBolsista(request)
        );

        verify(passwordEncoder, never()).encode(any());
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void traduzColisaoDeUnicidadeNaGravacaoComoBolsistaDuplicado() {
        var request = new IfceBolsistaRequestDTO(
                "Joana", "12345678901", "joana@example.com", "85999990000", "senha"
        );
        when(repository.existsByCpf("12345678901")).thenReturn(false);
        when(repository.existsByEmailIgnoreCase("joana@example.com")).thenReturn(false);
        when(passwordEncoder.encode("senha")).thenReturn("hash");
        when(repository.saveAndFlush(any(Usuario.class)))
                .thenThrow(new DataIntegrityViolationException("unique constraint"));

        assertThrows(
                IfceBolsistaService.BolsistaDuplicadoException.class,
                () -> service.cadastrarBolsista(request)
        );
    }

    @Test
    void listaOsAliasesDePapelBolsistaMapeadosParaRespostaPublica() {
        Usuario bolsista = new Usuario();
        bolsista.setId(12L);
        bolsista.setNome("Bolsista de teste");
        bolsista.setEmail("bolsista@example.com");
        bolsista.setWhatsapp("85999990000");
        bolsista.setRole(UsuarioRole.IFCE_BOLSISTA);
        when(repository.findByRoleInOrderByNomeAsc(List.of(
                UsuarioRole.BOLSISTA_IFCE, UsuarioRole.IFCE_BOLSISTA
        ))).thenReturn(List.of(bolsista));

        var result = service.listarBolsistas();

        assertEquals(1, result.size());
        assertEquals("Bolsista de teste", result.get(0).nome());
        assertEquals("bolsista@example.com", result.get(0).email());
        assertEquals("85999990000", result.get(0).whatsapp());
        verify(repository).findByRoleInOrderByNomeAsc(List.of(
                UsuarioRole.BOLSISTA_IFCE, UsuarioRole.IFCE_BOLSISTA
        ));
    }
}
