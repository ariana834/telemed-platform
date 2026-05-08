package com.telemedicina.auth.repository;

public interface AuthRepository {
    Long createUser(String email, String passwordHash, String role);
    boolean existsByEmail(String email);
}