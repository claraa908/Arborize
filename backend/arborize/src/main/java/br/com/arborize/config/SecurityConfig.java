package br.com.arborize.config;

import br.com.arborize.security.JwtAuthenticationFilter;
import br.com.arborize.security.CompensacaoSecurityProblemHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import jakarta.servlet.http.HttpServletRequest;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CompensacaoSecurityProblemHandler compensacaoSecurityProblemHandler;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                          CompensacaoSecurityProblemHandler compensacaoSecurityProblemHandler) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.compensacaoSecurityProblemHandler = compensacaoSecurityProblemHandler;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, exception) -> {
                            if (isCompensacaoRoute(request)) {
                                compensacaoSecurityProblemHandler.authenticationEntryPoint().commence(request, response, exception);
                            } else response.sendError(401);
                        })
                        .accessDeniedHandler((request, response, exception) -> {
                            if (isCompensacaoRoute(request)) {
                                compensacaoSecurityProblemHandler.accessDeniedHandler().handle(request, response, exception);
                            } else response.sendError(403);
                        })
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/solicitacoes-muda").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/solicitacoes-muda/conversao").permitAll()
                        .requestMatchers("/api/assistente-plantio/**").permitAll()
                        .requestMatchers("/api/materials/admin", "/api/materials/admin/**")
                        .hasAnyRole("AMMA", "ADMINISTRADOR_AMMA", "FUNCIONARIO_AMMA", "GESTOR_IFCE", "IFCE_GESTOR", "BOLSISTA_IFCE", "IFCE_BOLSISTA")
                        .requestMatchers("/api/termos-compensacao", "/api/termos-compensacao/**")
                        .hasAnyRole("FUNCIONARIO_AMMA", "ADMINISTRADOR_AMMA")
                        .requestMatchers("/api/**", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/auth/login", "/usuarios/cadastro").permitAll()
                        .requestMatchers("/admin/usuarios", "/admin/usuarios/**").hasRole("ADMINISTRADOR_AMMA")
                        .requestMatchers("/ifce/bolsistas", "/ifce/bolsistas/**").hasRole("GESTOR_IFCE")
                        .requestMatchers("/usuarios/me", "/usuarios/me/**").authenticated()
                        .requestMatchers(
                                HttpMethod.POST,
                                "/eventos", "/eventos/**",
                                "/especies", "/especies/**",
                                "/api/materials", "/api/materials/**"
                        ).hasAnyRole("AMMA", "ADMINISTRADOR_AMMA", "FUNCIONARIO_AMMA", "GESTOR_IFCE", "IFCE_GESTOR", "BOLSISTA_IFCE", "IFCE_BOLSISTA")
                        .requestMatchers(
                                HttpMethod.PUT,
                                "/eventos", "/eventos/**",
                                "/especies", "/especies/**",
                                "/api/materials", "/api/materials/**"
                        ).hasAnyRole("AMMA", "ADMINISTRADOR_AMMA", "FUNCIONARIO_AMMA", "GESTOR_IFCE", "IFCE_GESTOR", "BOLSISTA_IFCE", "IFCE_BOLSISTA")
                        .requestMatchers(
                                HttpMethod.DELETE,
                                "/eventos", "/eventos/**",
                                "/especies", "/especies/**",
                                "/api/materials", "/api/materials/**"
                        ).hasAnyRole("AMMA", "ADMINISTRADOR_AMMA", "FUNCIONARIO_AMMA", "GESTOR_IFCE", "IFCE_GESTOR", "BOLSISTA_IFCE", "IFCE_BOLSISTA")
                        .requestMatchers(
                                HttpMethod.GET,
                                "/especies", "/especies/**",
                                "/eventos", "/eventos/**",
                                "/api/materials", "/api/materials/*",
                                "/api/materials/*/cover", "/api/materials/*/download"
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    private boolean isCompensacaoRoute(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        return path.equals("/api/termos-compensacao") || path.startsWith("/api/termos-compensacao/");
    }
}
