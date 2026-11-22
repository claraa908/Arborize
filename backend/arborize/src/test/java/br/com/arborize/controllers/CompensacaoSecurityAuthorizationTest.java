package br.com.arborize.controllers;

import br.com.arborize.config.SecurityConfig;
import br.com.arborize.services.CompensacaoService;
import br.com.arborize.services.TokenService;
import br.com.arborize.security.CompensacaoSecurityProblemHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = CompensacaoController.class)
@AutoConfigureMockMvc
@Import({SecurityConfig.class, CompensacaoSecurityProblemHandler.class})
class CompensacaoSecurityAuthorizationTest {
    @Autowired MockMvc mvc;
    @MockitoBean TokenService tokenService;
    @MockitoBean CompensacaoService service;

    @Test void rotasExigemAutenticacao() throws Exception {
        mvc.perform(get("/api/termos-compensacao"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.code").value("nao-autenticado"));
    }

    @Test void cidadaoRecebe403EPerfisInternosPodemConsultar() throws Exception {
        requisicaoComo("cidadao", "ROLE_CIDADAO").andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.code").value("sem-permissao"));
        when(service.listar()).thenReturn(java.util.List.of());
        requisicaoComo("funcionario", "ROLE_FUNCIONARIO_AMMA").andExpect(status().isOk());
        requisicaoComo("administrador", "ROLE_ADMINISTRADOR_AMMA").andExpect(status().isOk());
    }

    private org.springframework.test.web.servlet.ResultActions requisicaoComo(String token, String role) throws Exception {
        when(tokenService.validarEObterUsuarioId(token)).thenReturn(12L);
        when(tokenService.validarEObterRole(token)).thenReturn(role);
        return mvc.perform(get("/api/termos-compensacao")
                .header("Authorization", "Bearer " + token).accept(MediaType.APPLICATION_JSON));
    }
}
