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

public final class IfceGestorAccessFilter extends OncePerRequestFilter {

    private static final Set<String> ROLES_GESTOR = Set.of("ROLE_GESTOR_IFCE");

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (isGestor(authentication) && !isEndpointPermitido(request)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isGestor(Authentication authentication) {
        return authentication != null
                && authentication.isAuthenticated()
                && authentication.getAuthorities().stream()
                .anyMatch(authority -> ROLES_GESTOR.contains(authority.getAuthority()));
    }

    private boolean isEndpointPermitido(HttpServletRequest request) {
        if ("OPTIONS".equals(request.getMethod())) {
            return true;
        }

        String method = request.getMethod();
        String path = getRequestPath(request);
        if ("POST".equals(method) && "/auth/login".equals(path)) {
            return true;
        }

        if ("/especies".equals(path) || path.startsWith("/especies/")) {
            return true;
        }

        if ("GET".equals(method) && (
                "/eventos".equals(path) || path.startsWith("/eventos/")
                        || "/api/materials".equals(path)
                        || path.matches("/api/materials/\\d+(?:/(?:cover|download))?"))) {
            return true;
        }

        if (("GET".equals(method) && "/api/solicitacoes-muda/conversao".equals(path))
                || ("POST".equals(method) && (
                "/api/assistente-plantio/recomendar".equals(path)
                        || "/api/solicitacoes-muda".equals(path)))) {
            return true;
        }

        if (("GET".equals(method) && "/usuarios/me".equals(path))
                || ("PUT".equals(method)
                && ("/usuarios/me".equals(path) || "/usuarios/me/senha".equals(path)))) {
            return true;
        }

        if (("GET".equals(method) && (
                "/api/viveiro/movimentacoes".equals(path)
                        || "/api/viveiro/movimentacoes/resumo".equals(path)
                        || path.matches("/api/viveiro/movimentacoes/\\d+(?:/comprovante)?")))
                || ("POST".equals(method) && "/api/viveiro/movimentacoes".equals(path))
                || ("PUT".equals(method) && path.matches("/api/viveiro/movimentacoes/\\d+"))) {
            return true;
        }

        if ("POST".equals(method) && (
                "/api/substituicoes-botanicas".equals(path)
                        || "/api/substituicoes-botanicas/acompanhar".equals(path))) {
            return true;
        }

        return ("GET".equals(method) || "POST".equals(method))
                && "/ifce/bolsistas".equals(path);
    }

    private String getRequestPath(HttpServletRequest request) {
        String path = request.getRequestURI();
        String contextPath = request.getContextPath();
        return contextPath.isEmpty() ? path : path.substring(contextPath.length());
    }
}
