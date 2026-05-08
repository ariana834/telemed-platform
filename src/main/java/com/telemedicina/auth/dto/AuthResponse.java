package com.telemedicina.auth.dto;

public class AuthResponse {

    private String token;
    private Long userId;
    private String email;
    private String role;
    private long expiresInSeconds;

    public AuthResponse(String token, Long userId, String email,
                        String role, long expiresInSeconds) {
        this.token = token;
        this.userId = userId;
        this.email = email;
        this.role = role;
        this.expiresInSeconds = expiresInSeconds;
    }

    public String getToken() { return token; }
    public Long getUserId() { return userId; }
    public String getEmail() { return email; }
    public String getRole() { return role; }
    public long getExpiresInSeconds() { return expiresInSeconds; }
}