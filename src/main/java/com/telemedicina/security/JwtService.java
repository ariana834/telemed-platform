package com.telemedicina.security;

import com.telemedicina.shared.config.JwtConfig;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;

/**
 * Serviciu responsabil de tot ce ține de JWT:
 * - generarea token-ului după login
 * - validarea token-ului la fiecare request
 * - extragerea email-ului din token
 */
@Service
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);

    private final JwtConfig jwtConfig;

    public JwtService(JwtConfig jwtConfig) {
        this.jwtConfig = jwtConfig;
    }

    public String generateToken(Long userId, String email, String role) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + jwtConfig.getExpirationMs());

        return Jwts.builder()
                .subject(email)
                .claim("userId", userId)
                .claim("role", role)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(getSigningKey())
                .compact();
    }

    public String extractEmail(String token) {
        return parseClaims(token).getSubject();
    }

    public Long extractUserId(String token) {
        return ((Number) parseClaims(token).get("userId")).longValue();
    }

    public String extractRole(String token) {
        return (String) parseClaims(token).get("role");
    }

    /**
     * Validează token-ul complet: semnătură + expirare + format.
     * Returnează true dacă e valid, false altfel (nu aruncă excepție).
     */
    public boolean isTokenValid(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.warn("JWT expirat: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.warn("JWT nesuportat: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            log.warn("JWT malformat: {}", e.getMessage());
        } catch (SecurityException e) {
            log.warn("Semnătură JWT invalidă: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.warn("JWT gol sau null: {}", e.getMessage());
        }
        return false;
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSigningKey() {
        // Dacă secretul nu e base64, îl convertim direct din bytes
        byte[] keyBytes = jwtConfig.getSecret().getBytes();
        return Keys.hmacShaKeyFor(keyBytes);
    }
}