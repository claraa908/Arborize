package br.com.arborize.config;

import br.com.arborize.security.JwtAuthenticationFilter;
import br.com.arborize.security.IfceBolsistaAccessFilter;
import br.com.arborize.security.IfceGestorAccessFilter;
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

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

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
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/solicitacoes-muda").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/solicitacoes-muda/conversao").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/assistente-plantio/recomendar").authenticated()
                        .requestMatchers("/api/materials/admin", "/api/materials/admin/**")
                        .hasAnyRole("ADMINISTRADOR_AMMA", "FUNCIONARIO_AMMA", "GESTOR_IFCE", "BOLSISTA_IFCE")
                        .requestMatchers("/api/viveiro/movimentacoes", "/api/viveiro/movimentacoes/**")
                        .hasAnyRole("ADMINISTRADOR_AMMA", "FUNCIONARIO_AMMA", "GESTOR_IFCE", "BOLSISTA_IFCE")
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
                        ).hasAnyRole("ADMINISTRADOR_AMMA", "FUNCIONARIO_AMMA", "GESTOR_IFCE", "BOLSISTA_IFCE")
                        .requestMatchers(
                                HttpMethod.PUT,
                                "/eventos", "/eventos/**",
                                "/especies", "/especies/**",
                                "/api/materials", "/api/materials/**"
                        ).hasAnyRole("ADMINISTRADOR_AMMA", "FUNCIONARIO_AMMA", "GESTOR_IFCE", "BOLSISTA_IFCE")
                        .requestMatchers(
                                HttpMethod.DELETE,
                                "/eventos", "/eventos/**",
                                "/especies", "/especies/**",
                                "/api/materials", "/api/materials/**"
                        ).hasAnyRole("ADMINISTRADOR_AMMA", "FUNCIONARIO_AMMA", "GESTOR_IFCE", "BOLSISTA_IFCE")
                        .requestMatchers(
                                HttpMethod.GET,
                                "/especies", "/especies/**",
                                "/eventos", "/eventos/**",
                                "/api/materials", "/api/materials/*",
                                "/api/materials/*/cover", "/api/materials/*/download"
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(new IfceBolsistaAccessFilter(), JwtAuthenticationFilter.class)
                .addFilterAfter(new IfceGestorAccessFilter(), IfceBolsistaAccessFilter.class);

        return http.build();
    }
}
