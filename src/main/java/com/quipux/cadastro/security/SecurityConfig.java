package com.quipux.cadastro.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Todas as APIs de negocio exigem um token JWT valido.
 *
 * <p>Ficam publicos apenas: o endpoint de login, a interface web estatica,
 * a documentacao OpenAPI e o console do H2.
 */
@Configuration
@EnableWebSecurity
@EnableConfigurationProperties({JwtProperties.class, AuthProperties.class})
public class SecurityConfig {

    private static final String[] ROTAS_PUBLICAS = {
            "/auth/login",
            "/", "/index.html", "/app.js", "/estilos.css", "/favicon.ico",
            "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html",
            "/h2-console/**"
    };

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JwtService jwtService,
            UserDetailsService userDetailsService, ObjectMapper objectMapper) throws Exception {

        // instanciado aqui (e nao como @Component) para que o Spring Boot nao
        // registre o filtro tambem fora da cadeia do Spring Security
        var jwtFilter = new JwtAuthenticationFilter(jwtService, userDetailsService);

        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sessao -> sessao.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(rotas -> rotas
                        .requestMatchers(ROTAS_PUBLICAS).permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(tratamento -> tratamento
                        .authenticationEntryPoint((request, response, ex) -> RespostaDeErroHttp.escrever(
                                objectMapper, request, response,
                                HttpServletResponse.SC_UNAUTHORIZED,
                                "Nao autenticado",
                                "Informe um token JWT valido no header Authorization"))
                        .accessDeniedHandler((request, response, ex) -> RespostaDeErroHttp.escrever(
                                objectMapper, request, response,
                                HttpServletResponse.SC_FORBIDDEN,
                                "Acesso negado",
                                "Voce nao tem permissao para acessar este recurso")))
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public UserDetailsService userDetailsService(AuthProperties propriedades, PasswordEncoder encoder) {
        return new InMemoryUserDetailsManager(
                User.withUsername(propriedades.usuario())
                        .password(encoder.encode(propriedades.senha()))
                        .roles("ADMIN")
                        .build());
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuracao)
            throws Exception {
        return configuracao.getAuthenticationManager();
    }
}
