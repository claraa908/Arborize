package br.com.arborize.config;

import br.com.arborize.services.TokenService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.http.HttpMethod;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = SecurityAuthorizationTest.SecurityProbeController.class)
@AutoConfigureMockMvc
@Import({SecurityConfig.class, SecurityAuthorizationTest.SecurityProbeController.class})
class SecurityAuthorizationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TokenService tokenService;

    @Test
    void materiaisAdministrativosNaoFicamPublicosPelaRegraGenericaApi() throws Exception {
        mockMvc.perform(get("/api/materials/admin"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void materiaisAdministrativosRejeitamCidadaoEAceitamPerfilInstitucional() throws Exception {
        requestAs("jwt-cidadao", "ROLE_CIDADAO", "/api/materials/admin")
                .andExpect(status().isForbidden());
        requestAs("jwt-funcionario", "ROLE_FUNCIONARIO_AMMA", "/api/materials/admin")
                .andExpect(status().isOk())
                .andExpect(content().string("ok"));
    }

    @Test
    void materiaisAdministrativosTambemProtegemOperacoesDeEscrita() throws Exception {
        mockMvc.perform(post("/api/materials/admin"))
                .andExpect(status().isUnauthorized());
        requestAs(HttpMethod.POST, "jwt-cidadao", "ROLE_CIDADAO", "/api/materials/admin")
                .andExpect(status().isForbidden());
        requestAs(HttpMethod.POST, "jwt-funcionario", "ROLE_FUNCIONARIO_AMMA", "/api/materials/admin")
                .andExpect(status().isOk());
    }

    @Test
    void gestaoDeUsuariosExigePapelDeAdministrador() throws Exception {
        mockMvc.perform(get("/admin/usuarios"))
                .andExpect(status().isUnauthorized());
        requestAs("jwt-cidadao", "ROLE_CIDADAO", "/admin/usuarios")
                .andExpect(status().isForbidden());
        requestAs("jwt-admin", "ROLE_ADMINISTRADOR_AMMA", "/admin/usuarios")
                .andExpect(status().isOk());
    }

    @Test
    void equipeIfceExigeGestorMesmoQuandoAdministradorEstaAutenticado() throws Exception {
        requestAs("jwt-admin", "ROLE_ADMINISTRADOR_AMMA", "/ifce/bolsistas")
                .andExpect(status().isForbidden());
        requestAs("jwt-gestor", "ROLE_GESTOR_IFCE", "/ifce/bolsistas")
                .andExpect(status().isOk());
    }

    private org.springframework.test.web.servlet.ResultActions requestAs(String jwt, String role, String path)
            throws Exception {
        return requestAs(HttpMethod.GET, jwt, role, path);
    }

    private org.springframework.test.web.servlet.ResultActions requestAs(HttpMethod method, String jwt, String role, String path)
            throws Exception {
        when(tokenService.validarEObterUsuarioId(jwt)).thenReturn(9L);
        when(tokenService.validarEObterRole(jwt)).thenReturn(role);
        var builder = method == HttpMethod.POST ? post(path) : get(path);
        return mockMvc.perform(builder.header("Authorization", "Bearer " + jwt)
                .accept(MediaType.TEXT_PLAIN));
    }

    @RestController
    public static class SecurityProbeController {
        @GetMapping("/api/materials/admin")
        public String materialsAdmin() { return "ok"; }

        @PostMapping("/api/materials/admin")
        public String createAdminMaterial() { return "ok"; }

        @GetMapping("/admin/usuarios")
        public String adminUsers() { return "ok"; }

        @GetMapping("/ifce/bolsistas")
        public String ifceTeam() { return "ok"; }
    }
}
