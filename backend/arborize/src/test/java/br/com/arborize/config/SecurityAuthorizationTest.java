package br.com.arborize.config;

import br.com.arborize.services.TokenService;
import br.com.arborize.security.CompensacaoSecurityProblemHandler;
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

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = SecurityAuthorizationTest.SecurityProbeController.class)
@AutoConfigureMockMvc
@Import({SecurityConfig.class, CorsGlobalConfig.class, SecurityAuthorizationTest.SecurityProbeController.class})
class SecurityAuthorizationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TokenService tokenService;

    @MockitoBean
    private CompensacaoSecurityProblemHandler compensacaoSecurityProblemHandler;

    @Autowired
    private SecurityProbeController securityProbeController;

    @Test
    void APIsDeArborizacaoExigemAutenticacaoEContinuamDisponiveisAosPerfisAutenticados() throws Exception {
        mockMvc.perform(post("/api/assistente-plantio/recomendar"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/solicitacoes-muda/conversao"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/solicitacoes-muda"))
                .andExpect(status().isUnauthorized());

        for (var usuario : List.<String[]>of(
                new String[]{"jwt-cidadao", "ROLE_CIDADAO"},
                new String[]{"jwt-admin", "ROLE_ADMINISTRADOR_AMMA"},
                new String[]{"jwt-funcionario", "ROLE_FUNCIONARIO_AMMA"},
                new String[]{"jwt-gestor", "ROLE_GESTOR_IFCE"},
                new String[]{"jwt-bolsista", "ROLE_BOLSISTA_IFCE"})) {
            requestAs(HttpMethod.POST, usuario[0], usuario[1], "/api/assistente-plantio/recomendar")
                    .andExpect(status().isOk());
            requestAs(HttpMethod.GET, usuario[0], usuario[1], "/api/solicitacoes-muda/conversao")
                    .andExpect(status().isOk());
            requestAs(HttpMethod.POST, usuario[0], usuario[1], "/api/solicitacoes-muda")
                    .andExpect(status().isOk());
            requestAs(usuario[0], usuario[1], "/eventos")
                    .andExpect(status().isOk());
            requestAs(HttpMethod.POST, usuario[0], usuario[1], "/api/substituicoes-botanicas")
                    .andExpect(status().isOk());
        }
    }

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

    @Test
    void authoritiesLegadasNaoAutenticamRecursosProtegidos() throws Exception {
        for (String authority : List.of("ROLE_AMMA", "ROLE_IFCE_GESTOR", "ROLE_IFCE_BOLSISTA")) {
            String jwt = "jwt-legado-" + authority;
            when(tokenService.validarEObterUsuarioId(jwt)).thenReturn(9L);
            when(tokenService.validarEObterRole(jwt))
                    .thenThrow(new IllegalArgumentException("Role invÃ¡lida no token."));

            mockMvc.perform(get("/usuarios/me").header("Authorization", "Bearer " + jwt))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Test
    void bolsistasAcessamSomenteApisDoViveiro() throws Exception {
        for (var bolsista : List.<String[]>of(new String[]{"jwt-bolsista", "ROLE_BOLSISTA_IFCE"})) {
            requestAs(HttpMethod.GET, bolsista[0], bolsista[1], "/especies")
                    .andExpect(status().isOk())
                    .andExpect(content().string("viveiro"));
            requestAs(HttpMethod.GET, bolsista[0], bolsista[1], "/especies/12/imagem")
                    .andExpect(status().isOk())
                    .andExpect(content().string("imagem"));
            requestAs(HttpMethod.POST, bolsista[0], bolsista[1], "/especies")
                    .andExpect(status().isOk());
            requestAs(HttpMethod.PUT, bolsista[0], bolsista[1], "/especies/editar/12")
                    .andExpect(status().isOk());
            requestAs(HttpMethod.DELETE, bolsista[0], bolsista[1], "/especies/12")
                    .andExpect(status().isOk());
            requestAs(HttpMethod.GET, bolsista[0], bolsista[1], "/api/materials")
                    .andExpect(status().isOk())
                    .andExpect(content().string("catalogo-publico"));
            requestAs(HttpMethod.GET, bolsista[0], bolsista[1], "/api/materials/12")
                    .andExpect(status().isOk())
                    .andExpect(content().string("material-publico"));
            requestAs(HttpMethod.GET, bolsista[0], bolsista[1], "/api/materials/12/cover")
                    .andExpect(status().isOk());
            requestAs(HttpMethod.GET, bolsista[0], bolsista[1], "/api/materials/12/download")
                    .andExpect(status().isOk());
            requestAs(bolsista[0], bolsista[1], "/eventos")
                    .andExpect(status().isOk())
                    .andExpect(content().string("eventos"));
            requestAs(bolsista[0], bolsista[1], "/eventos/12")
                    .andExpect(status().isOk())
                    .andExpect(content().string("evento"));
            requestAs(bolsista[0], bolsista[1], "/eventos/12/banner")
                    .andExpect(status().isOk());
            requestAs(HttpMethod.POST, bolsista[0], bolsista[1], "/api/substituicoes-botanicas")
                    .andExpect(status().isOk());
            requestAs(HttpMethod.POST, bolsista[0], bolsista[1], "/api/substituicoes-botanicas/acompanhar")
                    .andExpect(status().isOk());
            requestAs(HttpMethod.POST, bolsista[0], bolsista[1], "/api/assistente-plantio/recomendar")
                    .andExpect(status().isOk());
            requestAs(HttpMethod.GET, bolsista[0], bolsista[1], "/api/solicitacoes-muda/conversao")
                    .andExpect(status().isOk());
            requestAs(HttpMethod.POST, bolsista[0], bolsista[1], "/api/solicitacoes-muda")
                    .andExpect(status().isOk());
            requestAs(bolsista[0], bolsista[1], "/api/viveiro/movimentacoes")
                    .andExpect(status().isOk())
                    .andExpect(content().string("movimentacoes"));
            requestAs(bolsista[0], bolsista[1], "/api/viveiro/movimentacoes/12")
                    .andExpect(status().isOk());
            requestAs(bolsista[0], bolsista[1], "/api/viveiro/movimentacoes/resumo")
                    .andExpect(status().isOk());
            requestAs(bolsista[0], bolsista[1], "/api/viveiro/movimentacoes/12/comprovante")
                    .andExpect(status().isOk());
            requestAs(HttpMethod.POST, bolsista[0], bolsista[1], "/api/viveiro/movimentacoes")
                    .andExpect(status().isOk());
            requestAs(HttpMethod.PUT, bolsista[0], bolsista[1], "/api/viveiro/movimentacoes/12")
                    .andExpect(status().isOk());
            requestAs(bolsista[0], bolsista[1], "/usuarios/me")
                    .andExpect(status().isOk())
                    .andExpect(content().string("perfil"));
            requestAs(HttpMethod.PUT, bolsista[0], bolsista[1], "/usuarios/me")
                    .andExpect(status().isOk());
            requestAs(HttpMethod.PUT, bolsista[0], bolsista[1], "/usuarios/me/senha")
                    .andExpect(status().isOk());

            securityProbeController.publicProbeCalls.set(0);
            requestAs(bolsista[0], bolsista[1], "/api/publico/probe")
                    .andExpect(status().isForbidden());
            org.junit.jupiter.api.Assertions.assertEquals(0, securityProbeController.publicProbeCalls.get());
            requestAs(bolsista[0], bolsista[1], "/api/materials/admin")
                    .andExpect(status().isForbidden());
            securityProbeController.substitutionManagementCalls.set(0);
            requestAs(bolsista[0], bolsista[1], "/api/substituicoes-botanicas/gestao")
                    .andExpect(status().isForbidden());
            requestAs(HttpMethod.POST, bolsista[0], bolsista[1], "/api/substituicoes-botanicas/gestao/12/eventos")
                    .andExpect(status().isForbidden());
            org.junit.jupiter.api.Assertions.assertEquals(0, securityProbeController.substitutionManagementCalls.get());
            requestAs(HttpMethod.POST, bolsista[0], bolsista[1], "/api/materials")
                    .andExpect(status().isForbidden());
            requestAs(HttpMethod.PUT, bolsista[0], bolsista[1], "/api/materials/12")
                    .andExpect(status().isForbidden());
            requestAs(HttpMethod.DELETE, bolsista[0], bolsista[1], "/api/materials/12")
                    .andExpect(status().isForbidden());
            requestAs(HttpMethod.PATCH, bolsista[0], bolsista[1], "/api/materials/12/status")
                    .andExpect(status().isForbidden());
            requestAs(HttpMethod.DELETE, bolsista[0], bolsista[1], "/api/viveiro/movimentacoes/12")
                    .andExpect(status().isForbidden());
            requestAs(HttpMethod.DELETE, bolsista[0], bolsista[1], "/usuarios/me")
                    .andExpect(status().isForbidden());
            requestAs(bolsista[0], bolsista[1], "/usuarios/me/senha")
                    .andExpect(status().isForbidden());
            requestAs(HttpMethod.POST, bolsista[0], bolsista[1], "/auth/login")
                    .andExpect(status().isNotFound());
        }
    }

    @Test
    void gestorIfceAcessaSomenteViveiroEquipeEPerfilProprio() throws Exception {
        String jwt = "jwt-gestor-allowlist";
        String role = "ROLE_GESTOR_IFCE";

        requestAs(HttpMethod.GET, jwt, role, "/especies")
                .andExpect(status().isOk())
                .andExpect(content().string("viveiro"));
        requestAs(HttpMethod.POST, jwt, role, "/especies").andExpect(status().isOk());
        requestAs(HttpMethod.PUT, jwt, role, "/especies/editar/12").andExpect(status().isOk());
        requestAs(HttpMethod.DELETE, jwt, role, "/especies/12").andExpect(status().isOk());

        requestAs(jwt, role, "/api/viveiro/movimentacoes")
                .andExpect(status().isOk())
                .andExpect(content().string("movimentacoes"));
        requestAs(jwt, role, "/api/viveiro/movimentacoes/resumo").andExpect(status().isOk());
        requestAs(jwt, role, "/api/viveiro/movimentacoes/12").andExpect(status().isOk());
        requestAs(jwt, role, "/api/viveiro/movimentacoes/12/comprovante").andExpect(status().isOk());
        requestAs(HttpMethod.POST, jwt, role, "/api/viveiro/movimentacoes").andExpect(status().isOk());
        requestAs(HttpMethod.PUT, jwt, role, "/api/viveiro/movimentacoes/12").andExpect(status().isOk());

        requestAs(jwt, role, "/ifce/bolsistas").andExpect(status().isOk());
        requestAs(HttpMethod.POST, jwt, role, "/ifce/bolsistas").andExpect(status().isOk());
        requestAs(jwt, role, "/eventos").andExpect(status().isOk());
        requestAs(jwt, role, "/eventos/12").andExpect(status().isOk());
        requestAs(jwt, role, "/eventos/12/banner").andExpect(status().isOk());
        requestAs(HttpMethod.POST, jwt, role, "/api/substituicoes-botanicas").andExpect(status().isOk());
        requestAs(HttpMethod.POST, jwt, role, "/api/substituicoes-botanicas/acompanhar").andExpect(status().isOk());
        requestAs(jwt, role, "/api/materials")
                .andExpect(status().isOk())
                .andExpect(content().string("catalogo-publico"));
        requestAs(jwt, role, "/api/materials/12")
                .andExpect(status().isOk())
                .andExpect(content().string("material-publico"));
        requestAs(jwt, role, "/api/materials/12/cover").andExpect(status().isOk());
        requestAs(jwt, role, "/api/materials/12/download").andExpect(status().isOk());
        requestAs(jwt, role, "/api/solicitacoes-muda/conversao").andExpect(status().isOk());
        requestAs(HttpMethod.POST, jwt, role, "/api/solicitacoes-muda").andExpect(status().isOk());
        requestAs(HttpMethod.POST, jwt, role, "/api/assistente-plantio/recomendar").andExpect(status().isOk());
        requestAs(jwt, role, "/usuarios/me")
                .andExpect(status().isOk())
                .andExpect(content().string("perfil"));
        requestAs(HttpMethod.PUT, jwt, role, "/usuarios/me").andExpect(status().isOk());
        requestAs(HttpMethod.PUT, jwt, role, "/usuarios/me/senha").andExpect(status().isOk());

        when(tokenService.validarEObterUsuarioId(jwt)).thenReturn(9L);
        when(tokenService.validarEObterRole(jwt)).thenReturn(role);
        mockMvc.perform(request(HttpMethod.OPTIONS, "/api/publico/probe")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "GET")
                        .header("Access-Control-Request-Headers", "authorization")
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk());
        requestAs(HttpMethod.POST, jwt, role, "/auth/login").andExpect(status().isNotFound());

        securityProbeController.publicProbeCalls.set(0);
        requestAs(jwt, role, "/api/publico/probe").andExpect(status().isForbidden());
        org.junit.jupiter.api.Assertions.assertEquals(0, securityProbeController.publicProbeCalls.get());
        requestAs(jwt, role, "/api/arvores").andExpect(status().isForbidden());
        requestAs(jwt, role, "/api/bairros").andExpect(status().isForbidden());
        requestAs(jwt, role, "/api/dashboard/overview").andExpect(status().isForbidden());
        requestAs(jwt, role, "/api/materials/admin").andExpect(status().isForbidden());
        securityProbeController.substitutionManagementCalls.set(0);
        requestAs(jwt, role, "/api/substituicoes-botanicas/gestao").andExpect(status().isForbidden());
        requestAs(HttpMethod.POST, jwt, role, "/api/substituicoes-botanicas/gestao/12/eventos")
                .andExpect(status().isForbidden());
        org.junit.jupiter.api.Assertions.assertEquals(0, securityProbeController.substitutionManagementCalls.get());
        requestAs(jwt, role, "/admin/usuarios").andExpect(status().isForbidden());
        requestAs(HttpMethod.PUT, jwt, role, "/ifce/bolsistas").andExpect(status().isForbidden());
        requestAs(jwt, role, "/ifce/bolsistas/12").andExpect(status().isForbidden());
        requestAs(HttpMethod.DELETE, jwt, role, "/api/viveiro/movimentacoes/12")
                .andExpect(status().isForbidden());
        requestAs(HttpMethod.DELETE, jwt, role, "/usuarios/me").andExpect(status().isForbidden());
        requestAs(jwt, role, "/usuarios/12").andExpect(status().isForbidden());
    }

    @Test
    void endpointPublicoContinuaDisponivelParaVisitantesEOutrosPerfisEPreflightFunciona() throws Exception {
        securityProbeController.publicProbeCalls.set(0);
        mockMvc.perform(get("/api/publico/probe"))
                .andExpect(status().isOk())
                .andExpect(content().string("publico"));
        mockMvc.perform(get("/eventos"))
                .andExpect(status().isOk())
                .andExpect(content().string("eventos"));
        requestAs("jwt-cidadao", "ROLE_CIDADAO", "/api/publico/probe")
                .andExpect(status().isOk());
        requestAs("jwt-funcionario", "ROLE_FUNCIONARIO_AMMA", "/api/publico/probe")
                .andExpect(status().isOk());
        org.junit.jupiter.api.Assertions.assertEquals(3, securityProbeController.publicProbeCalls.get());

        mockMvc.perform(request(HttpMethod.OPTIONS, "/api/publico/probe")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "GET")
                        .header("Access-Control-Request-Headers", "authorization"))
                .andExpect(status().isOk());
    }

    @Test
    void movimentacoesExigemPerfilDeEscritaDoViveiroEContinuamPrivadas() throws Exception {
        mockMvc.perform(get("/api/viveiro/movimentacoes"))
                .andExpect(status().isUnauthorized());
        requestAs("jwt-cidadao", "ROLE_CIDADAO", "/api/viveiro/movimentacoes")
                .andExpect(status().isForbidden());
        for (String role : List.of(
                "ROLE_ADMINISTRADOR_AMMA",
                "ROLE_FUNCIONARIO_AMMA",
                "ROLE_GESTOR_IFCE",
                "ROLE_BOLSISTA_IFCE")) {
            requestAs("jwt-" + role.toLowerCase(), role, "/api/viveiro/movimentacoes")
                    .andExpect(status().isOk());
        }
    }

    private org.springframework.test.web.servlet.ResultActions requestAs(String jwt, String role, String path)
            throws Exception {
        return requestAs(HttpMethod.GET, jwt, role, path);
    }

    private org.springframework.test.web.servlet.ResultActions requestAs(HttpMethod method, String jwt, String role, String path)
            throws Exception {
        when(tokenService.validarEObterUsuarioId(jwt)).thenReturn(9L);
        when(tokenService.validarEObterRole(jwt)).thenReturn(role);
        var builder = request(method, path);
        return mockMvc.perform(builder.header("Authorization", "Bearer " + jwt)
                .accept(MediaType.TEXT_PLAIN));
    }

    @RestController
    public static class SecurityProbeController {
        private final AtomicInteger publicProbeCalls = new AtomicInteger();
        private final AtomicInteger substitutionManagementCalls = new AtomicInteger();

        @GetMapping("/api/publico/probe")
        public String publicProbe() {
            publicProbeCalls.incrementAndGet();
            return "publico";
        }

        @GetMapping("/api/materials/admin")
        public String materialsAdmin() { return "ok"; }

        @GetMapping("/api/materials")
        public String publicMaterials() { return "catalogo-publico"; }

        @GetMapping("/api/materials/{id}")
        public String publicMaterial() { return "material-publico"; }

        @GetMapping("/api/materials/{id}/cover")
        public String publicMaterialCover() { return "capa"; }

        @GetMapping("/api/materials/{id}/download")
        public String publicMaterialDownload() { return "download"; }

        @GetMapping("/eventos")
        public String publicEvents() { return "eventos"; }

        @GetMapping("/eventos/{id}")
        public String publicEvent() { return "evento"; }

        @GetMapping("/eventos/{id}/banner")
        public String publicEventBanner() { return "banner"; }

        @PostMapping("/api/substituicoes-botanicas")
        public String createSubstitutionRequest() { return "requerimento"; }

        @PostMapping("/api/substituicoes-botanicas/acompanhar")
        public String trackSubstitutionRequest() { return "acompanhamento"; }

        @GetMapping("/api/substituicoes-botanicas/gestao")
        public String manageSubstitutionRequests() {
            substitutionManagementCalls.incrementAndGet();
            return "gestao";
        }

        @PostMapping("/api/substituicoes-botanicas/gestao/12/eventos")
        public String updateSubstitutionProgress() {
            substitutionManagementCalls.incrementAndGet();
            return "progresso";
        }

        @GetMapping("/api/solicitacoes-muda/conversao")
        public String calculateConversion() { return "conversao"; }

        @PostMapping("/api/assistente-plantio/recomendar")
        public String recommendSpecies() { return "recomendacoes"; }

        @PostMapping("/api/solicitacoes-muda")
        public String createMudaRequest() { return "solicitacao"; }

        @PostMapping("/api/materials")
        public String createMaterial() { return "criado"; }

        @org.springframework.web.bind.annotation.PutMapping("/api/materials/{id}")
        public String updateMaterial() { return "atualizado"; }

        @org.springframework.web.bind.annotation.DeleteMapping("/api/materials/{id}")
        public String deleteMaterial() { return "excluido"; }

        @org.springframework.web.bind.annotation.PatchMapping("/api/materials/{id}/status")
        public String updateMaterialStatus() { return "status"; }

        @PostMapping("/api/materials/admin")
        public String createAdminMaterial() { return "ok"; }

        @GetMapping("/api/viveiro/movimentacoes")
        public String movements() { return "movimentacoes"; }

        @GetMapping("/api/viveiro/movimentacoes/resumo")
        public String movementSummary() { return "resumo"; }

        @GetMapping("/api/viveiro/movimentacoes/{id}")
        public String movement() { return "movimento"; }

        @GetMapping("/api/viveiro/movimentacoes/{id}/comprovante")
        public String movementProof() { return "comprovante"; }

        @PostMapping("/api/viveiro/movimentacoes")
        public String createMovement() { return "criado"; }

        @org.springframework.web.bind.annotation.PutMapping("/api/viveiro/movimentacoes/{id}")
        public String updateMovement() { return "atualizado"; }

        @org.springframework.web.bind.annotation.DeleteMapping("/api/viveiro/movimentacoes/{id}")
        public String deleteMovement() { return "excluido"; }

        @GetMapping("/usuarios/me")
        public String ownProfile() { return "perfil"; }

        @org.springframework.web.bind.annotation.PutMapping("/usuarios/me")
        public String updateOwnProfile() { return "perfil-atualizado"; }

        @org.springframework.web.bind.annotation.PutMapping("/usuarios/me/senha")
        public String updateOwnPassword() { return "senha-atualizada"; }

        @org.springframework.web.bind.annotation.DeleteMapping("/usuarios/me")
        public String deleteOwnProfile() { return "excluido"; }

        @GetMapping("/admin/usuarios")
        public String adminUsers() { return "ok"; }

        @GetMapping("/ifce/bolsistas")
        public String ifceTeam() { return "ok"; }

        @PostMapping("/ifce/bolsistas")
        public String createIfceScholar() { return "criado"; }

        @GetMapping("/especies")
        public String species() { return "viveiro"; }

        @GetMapping("/especies/{id}/imagem")
        public String speciesImage() { return "imagem"; }

        @PostMapping("/especies")
        public String createSpecies() { return "criada"; }

        @org.springframework.web.bind.annotation.PutMapping("/especies/editar/{id}")
        public String updateSpecies() { return "atualizada"; }

        @org.springframework.web.bind.annotation.DeleteMapping("/especies/{id}")
        public String deleteSpecies() { return "excluida"; }
    }
}
