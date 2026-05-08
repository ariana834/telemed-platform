package com.telemedicina.security;

import com.telemedicina.shared.config.JwtConfig;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;

@Service
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);
    private final JwtConfig jwtConfig;

    public JwtService(JwtConfig jwtConfig) {
        this.jwtConfig = jwtConfig;
    }

    public String generateToken(Long userId, String email, String role) {
        return Jwts.builder()
                .subject(email)
                .claim("userId", userId)
                .claim("role", role)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + jwtConfig.getExpirationMs()))
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
        byte[] keyBytes = jwtConfig.getSecret().getBytes();
        return Keys.hmacShaKeyFor(keyBytes);
    }
}