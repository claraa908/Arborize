package br.com.arborize.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

public final class IfceBolsistaAccessFilter extends OncePerRequestFilter {

    private static final Set<String> ROLES_BOLSISTA = Set.of("ROLE_BOLSISTA_IFCE");

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (isBolsista(authentication) && !isEndpointPermitido(request)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isBolsista(Authentication authentication) {
        return authentication != null
                && authentication.isAuthenticated()
                && authentication.getAuthorities().stream()
                .anyMatch(authority -> ROLES_BOLSISTA.contains(authority.getAuthority()));
    }

    private boolean isEndpointPermitido(HttpServletRequest request) {
        if ("OPTIONS".equals(request.getMethod())) {
            return true;
        }

        String path = getRequestPath(request);
        if ("POST".equals(request.getMethod()) && "/auth/login".equals(path)) {
            return true;
        }

        if ("/especies".equals(path) || path.startsWith("/especies/")) {
            return true;
        }

        String method = request.getMethod();
        if (("GET".equals(method) && "/usuarios/me".equals(path))
                || ("PUT".equals(method)
                && ("/usuarios/me".equals(path) || "/usuarios/me/senha".equals(path)))) {
            return true;
        }
        if ("GET".equals(method) && (
                "/api/viveiro/movimentacoes".equals(path)
                        || "/api/viveiro/movimentacoes/resumo".equals(path)
                        || path.matches("/api/viveiro/movimentacoes/\\d+(?:/comprovante)?"))) {
            return true;
        }
        if ("POST".equals(method) && "/api/viveiro/movimentacoes".equals(path)) {
            return true;
        }
        if ("PUT".equals(method) && path.matches("/api/viveiro/movimentacoes/\\d+")) {
            return true;
        }

        if ("GET".equals(method)) {
            return "/api/materials".equals(path)
                    || path.matches("/api/materials/\\d+(?:/(?:cover|download))?")
                    || "/api/solicitacoes-muda/conversao".equals(path);
        }

        return ("POST".equals(method) && (
                "/api/assistente-plantio/recomendar".equals(path)
                        || "/api/solicitacoes-muda".equals(path)
        ));
    }

    private String getRequestPath(HttpServletRequest request) {
        String path = request.getRequestURI();
        String contextPath = request.getContextPath();
        return contextPath.isEmpty() ? path : path.substring(contextPath.length());
    }
}
