package br.com.arborize.services;

import br.com.arborize.dtos.request.LoginRequestDTO;
import br.com.arborize.entities.Usuario;
import br.com.arborize.enums.UsuarioRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AutenticacaoServiceTest {

    @Mock
    private UsuarioDetailsService usuarioDetailsService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private TokenService tokenService;

    private AutenticacaoService service;

    @BeforeEach
    void setUp() {
        service = new AutenticacaoService(usuarioDetailsService, passwordEncoder, tokenService);
    }

    @Test
    void loginRetornaRoleDoCidadao() {
        verificarRoleDeLogin(UsuarioRole.CIDADAO, "ROLE_CIDADAO");
    }

    @Test
    void loginRetornaRoleDoAdministradorDaAmma() {
        verificarRoleDeLogin(UsuarioRole.ADMINISTRADOR_AMMA, "ROLE_ADMINISTRADOR_AMMA");
    }

    @Test
    void loginRetornaRoleDoFuncionarioDaAmma() {
        verificarRoleDeLogin(UsuarioRole.FUNCIONARIO_AMMA, "ROLE_FUNCIONARIO_AMMA");
    }

    @Test
    void loginMantemRoleAmmaLegada() {
        verificarRoleDeLogin(UsuarioRole.AMMA, "ROLE_AMMA");
    }

    @Test
    void loginRejeitaSenhaIncorreta() {
        Usuario usuario = criarUsuario(UsuarioRole.CIDADAO);
        when(usuarioDetailsService.loadUserByUsername("cidadao@example.com")).thenReturn(usuario);
        when(passwordEncoder.matches("senha-incorreta", usuario.getPassword())).thenReturn(false);

        assertThrows(
                AutenticacaoService.CredenciaisInvalidasException.class,
                () -> service.login(new LoginRequestDTO("cidadao@example.com", "senha-incorreta"))
        );
        verify(tokenService, never()).gerarToken(usuario);
    }

    @Test
    void loginRejeitaUsuarioInexistente() {
        when(usuarioDetailsService.loadUserByUsername("desconhecido@example.com"))
                .thenThrow(new UsernameNotFoundException("Usuário não encontrado."));

        assertThrows(
                AutenticacaoService.CredenciaisInvalidasException.class,
                () -> service.login(new LoginRequestDTO("desconhecido@example.com", "qualquer"))
        );
    }

    private void verificarRoleDeLogin(UsuarioRole role, String authority) {
        Usuario usuario = criarUsuario(role);
        when(usuarioDetailsService.loadUserByUsername("usuario@example.com")).thenReturn(usuario);
        when(passwordEncoder.matches("senha-correta", usuario.getPassword())).thenReturn(true);
        when(tokenService.gerarToken(usuario)).thenReturn("jwt-assinado");

        var response = service.login(new LoginRequestDTO("usuario@example.com", "senha-correta"));

        assertEquals("jwt-assinado", response.token());
        assertEquals("Bearer", response.tipo());
        assertEquals("Usuário de teste", response.nome());
        assertEquals(authority, response.role());
    }

    private Usuario criarUsuario(UsuarioRole role) {
        Usuario usuario = new Usuario();
        usuario.setId(12L);
        usuario.setNome("Usuário de teste");
        usuario.setEmail("usuario@example.com");
        usuario.setSenha("hash-senha");
        usuario.setRole(role);
        return usuario;
    }
}
