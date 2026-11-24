package br.com.arborize.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import java.io.IOException;

@Component
public class CompensacaoSecurityProblemHandler {
    public AuthenticationEntryPoint authenticationEntryPoint() {
        return (request, response, exception) -> write(response, HttpStatus.UNAUTHORIZED,
                "Autenticação necessária", "Entre com um perfil autorizado da AMMA para acessar esta operação.", "nao-autenticado");
    }

    public AccessDeniedHandler accessDeniedHandler() {
        return (request, response, exception) -> write(response, HttpStatus.FORBIDDEN,
                "Acesso não autorizado", "Esta operação está disponível somente para perfis autorizados da AMMA.", "sem-permissao");
    }

    private void write(HttpServletResponse response, HttpStatus status, String title, String detail, String code) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding(java.nio.charset.StandardCharsets.UTF_8.name());
        response.getWriter().write("{\"type\":\"urn:arborize:compensacao:" + code + "\",\"title\":\"" + title
                + "\",\"status\":" + status.value() + ",\"detail\":\"" + detail + "\",\"code\":\"" + code + "\"}");
    }
}
