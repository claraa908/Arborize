package br.com.arborize.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable()) // Desativa CSRF para permitir POST/PUT/DELETE
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll() // Libera requisições de pré-voo CORS
                        .requestMatchers(HttpMethod.POST, "/api/solicitacoes-muda").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/solicitacoes-muda/conversao").permitAll()
                        .requestMatchers("/api/assistente-plantio/**").permitAll()
                        .requestMatchers("/api/**", "/swagger-ui/**", "/v3/api-docs/**").permitAll() // Libera toda a API
                        .anyRequest().permitAll() // Temporariamente libera tudo para testes locais sem dor de cabeça
                );

        return http.build();
    }
}
