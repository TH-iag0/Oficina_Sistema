package br.com.example.autogestao_backend.config;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import tools.jackson.databind.ObjectMapper;

@Configuration
@EnableMethodSecurity
public class SecurityConfiguration {
    private final ObjectMapper mapper;

    public SecurityConfiguration(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) {
        AuthenticationEntryPoint acessoNecessario = (request, response, erro) ->
            responderErro(response, 401, "Autenticação necessária.");
        http.cors(Customizer.withDefaults())
            .sessionManagement(session -> session.sessionCreationPolicy(
                org.springframework.security.config.http.SessionCreationPolicy.STATELESS))
            .csrf(Customizer.withDefaults())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.GET, "/api/csrf").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/ponto/marcacoes").permitAll()
                .requestMatchers("/api/acesso/**").denyAll()
                .requestMatchers("/api/**").hasRole("ADMIN")
                .anyRequest().authenticated())
            .addFilterBefore(new AcessoLocalFilter(), org.springframework.security.web.authentication.AnonymousAuthenticationFilter.class)
            .exceptionHandling(errors -> errors
                .authenticationEntryPoint(acessoNecessario)
                .accessDeniedHandler((request, response, erro) ->
                    responderErro(response, 403, "Acesso negado ou token CSRF inválido.")));
        return http.build();
    }

    @Bean
    public UrlBasedCorsConfigurationSource corsConfigurationSource(
        @Value("${autogestao.cors.allowed-origins:http://localhost:5173}") String origens) {
        CorsConfiguration cors = new CorsConfiguration();
        cors.setAllowedOrigins(Arrays.stream(origens.split(",")).map(String::strip).toList());
        cors.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        cors.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-CSRF-TOKEN"));
        cors.setAllowCredentials(true);
        cors.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", cors);
        return source;
    }

    private void responderErro(HttpServletResponse response, int status, String detalhe) throws IOException {
        response.setStatus(status);
        response.setContentType("application/problem+json");
        response.setCharacterEncoding("UTF-8");
        mapper.writeValue(response.getWriter(), ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(status), detalhe));
    }
}
