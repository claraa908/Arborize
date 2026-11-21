package br.com.arborize.config;

import br.com.arborize.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private static final String[] STAFF_AND_LAB_ROLES = {
            "AMMA", "ADMINISTRADOR_AMMA", "FUNCIONARIO_AMMA",
            "GESTOR_IFCE", "IFCE_GESTOR", "BOLSISTA_IFCE", "IFCE_BOLSISTA"
    };

    private static final String[] AMMA_ADMIN_ROLES = {
            "AMMA", "ADMINISTRADOR_AMMA", "FUNCIONARIO_AMMA"
    };

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
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
                        .authenticationEntryPoint((request, response, exception) -> response.sendError(401))
                )
                .authorizeHttpRequests(auth -> auth
                        // 1. Preflight CORS
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // 2. Autenticação e cadastro público de usuário
                        .requestMatchers("/auth", "/auth/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/usuarios/cadastro").permitAll()

                        // 3. Rotas restritas específicas (SEMPRE antes de regras genéricas)
                        .requestMatchers("/api/dashboard", "/api/dashboard/**").hasAnyRole(AMMA_ADMIN_ROLES)
                        .requestMatchers("/api/materials/admin", "/api/materials/admin/**").hasAnyRole(STAFF_AND_LAB_ROLES)
                        .requestMatchers("/admin/usuarios", "/admin/usuarios/**").hasRole("ADMINISTRADOR_AMMA")
                        .requestMatchers("/ifce/bolsistas", "/ifce/bolsistas/**").hasAnyRole("GESTOR_IFCE", "IFCE_GESTOR")
                        .requestMatchers("/usuarios/me", "/usuarios/me/**").authenticated()

                        // Árvores e fotos: leitura pública, escrita/exclusão protegida
                        .requestMatchers(HttpMethod.GET, "/api/arvores", "/api/arvores/**", "/api/arvores/*/fotos", "/api/arvores/*/fotos/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/arvores", "/api/arvores/**", "/api/arvores/*/fotos", "/api/arvores/*/fotos/**").hasAnyRole(STAFF_AND_LAB_ROLES)
                        .requestMatchers(HttpMethod.PUT, "/api/arvores", "/api/arvores/**", "/api/arvores/*/fotos", "/api/arvores/*/fotos/**").hasAnyRole(STAFF_AND_LAB_ROLES)
                        .requestMatchers(HttpMethod.PATCH, "/api/arvores", "/api/arvores/**", "/api/arvores/*/fotos", "/api/arvores/*/fotos/**").hasAnyRole(STAFF_AND_LAB_ROLES)
                        .requestMatchers(HttpMethod.DELETE, "/api/arvores", "/api/arvores/**", "/api/arvores/*/fotos", "/api/arvores/*/fotos/**").hasAnyRole(STAFF_AND_LAB_ROLES)

                        // Eventos, espécies e materiais: leitura pública, escrita/exclusão protegida
                        .requestMatchers(HttpMethod.GET,
                                "/especies", "/especies/**",
                                "/eventos", "/eventos/**",
                                "/api/materials", "/api/materials/*",
                                "/api/materials/*/cover", "/api/materials/*/download"
                        ).permitAll()
                        .requestMatchers(HttpMethod.POST,
                                "/eventos", "/eventos/**",
                                "/especies", "/especies/**",
                                "/api/materials", "/api/materials/**"
                        ).hasAnyRole(STAFF_AND_LAB_ROLES)
                        .requestMatchers(HttpMethod.PUT,
                                "/eventos", "/eventos/**",
                                "/especies", "/especies/**",
                                "/api/materials", "/api/materials/**"
                        ).hasAnyRole(STAFF_AND_LAB_ROLES)
                        .requestMatchers(HttpMethod.PATCH,
                                "/eventos", "/eventos/**",
                                "/especies", "/especies/**",
                                "/api/materials", "/api/materials/**"
                        ).hasAnyRole(STAFF_AND_LAB_ROLES)
                        .requestMatchers(HttpMethod.DELETE,
                                "/eventos", "/eventos/**",
                                "/especies", "/especies/**",
                                "/api/materials", "/api/materials/**"
                        ).hasAnyRole(STAFF_AND_LAB_ROLES)

                        // 4. Rotas públicas de apoio / fluxo cidadão
                        .requestMatchers(HttpMethod.GET, "/api/solicitacoes-muda/conversao").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/solicitacoes-muda").permitAll()
                        .requestMatchers("/api/assistente-plantio", "/api/assistente-plantio/**").permitAll()
                        .requestMatchers("/api/bairros", "/api/bairros/**").permitAll()
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()

                        // 5. Fallback seguro
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}