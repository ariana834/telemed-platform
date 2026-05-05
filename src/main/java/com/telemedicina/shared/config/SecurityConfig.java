package com.telemedicina.shared.config;

import com.telemedicina.security.JwtAuthFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Configurația centrală Spring Security.
 *
 * Principii:
 * - STATELESS: nu folosim sesiuni HTTP (REST API pur)
 * - JWT: autentificarea se face prin token în header
 * - CSRF dezactivat: nu e necesar pentru API-uri REST stateless
 * - BCrypt: hash-ul parolelor cu cost factor 12
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity   // permite @PreAuthorize pe metode individuale
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final UserDetailsService userDetailsService;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter, UserDetailsService userDetailsService) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.userDetailsService = userDetailsService;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // Dezactivăm CSRF — nu e necesar pentru API REST cu JWT
                .csrf(AbstractHttpConfigurer::disable)

                // Fără sesiuni — fiecare request e independent
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // Configurăm regulile de acces per endpoint
                .authorizeHttpRequests(auth -> auth

                        // ===== ENDPOINT-URI PUBLICE (fără autentificare) =====
                        .requestMatchers(HttpMethod.POST, "/api/auth/register").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()

                        // Swagger UI și documentația API — accesibile în dev
                        .requestMatchers(
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/api-docs/**",
                                "/v3/api-docs/**"
                        ).permitAll()

                        // ===== ENDPOINT-URI ADMIN =====
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")

                        // ===== ENDPOINT-URI DOCTOR =====
                        .requestMatchers("/api/doctors/*/schedule").hasAnyRole("DOCTOR", "ADMIN")

                        // ===== TOATE CELELALTE necesită autentificare =====
                        .anyRequest().authenticated()
                )

                // Adăugăm filtrul JWT înainte de filtrul standard de username/password
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)

                // Provider-ul care verifică credențialele la login
                .authenticationProvider(authenticationProvider());

        return http.build();
    }

    /**
     * Provider-ul de autentificare:
     * - folosește UserDetailsService pentru a încărca utilizatorul din DB
     * - folosește BCryptPasswordEncoder pentru a verifica parola
     */
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    /**
     * AuthenticationManager e necesar în AuthService pentru login manual.
     * Spring Boot 3 nu îl expune automat ca Bean — trebuie declarat explicit.
     */
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * BCrypt cu cost factor 12 — echilibru bun între securitate și performanță.
     * Cost 10 = ~100ms, Cost 12 = ~400ms per hash (acceptabil pentru login).
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}