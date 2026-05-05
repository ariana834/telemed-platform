package com.telemedicina.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filtru JWT care rulează O SINGURĂ DATĂ per request (OncePerRequestFilter).
 *
 * Fluxul:
 * 1. Extrage header-ul Authorization: Bearer <token>
 * 2. Validează token-ul cu JwtService
 * 3. Dacă e valid, autentifică utilizatorul în SecurityContext
 * 4. Continuă lanțul de filtre
 *
 * Dacă token-ul lipsește sau e invalid, request-ul continuă neautentificat
 * — SecurityConfig va bloca accesul la endpoint-urile protejate.
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsServiceImpl userDetailsService;

    public JwtAuthFilter(JwtService jwtService, UserDetailsServiceImpl userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");

        // Dacă nu există header sau nu începe cu "Bearer ", trecem mai departe
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // Extragem token-ul (după "Bearer ")
        final String token = authHeader.substring(7);

        // Dacă token-ul nu e valid, nu autentificăm — request-ul merge mai departe
        if (!jwtService.isTokenValid(token)) {
            filterChain.doFilter(request, response);
            return;
        }

        final String email = jwtService.extractEmail(token);

        // Autentificăm doar dacă nu există deja o autentificare activă
        if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = userDetailsService.loadUserByUsername(email);

            // Construim obiectul de autentificare Spring Security
            UsernamePasswordAuthenticationToken authToken =
                    new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,                        // credentials null — deja autentificat prin JWT
                            userDetails.getAuthorities()
                    );
            authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            // Setăm autentificarea în context — Spring Security îl va vedea
            SecurityContextHolder.getContext().setAuthentication(authToken);
        }

        filterChain.doFilter(request, response);
    }
}