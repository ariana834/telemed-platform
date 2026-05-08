package com.telemedicina.auth.service;

import com.telemedicina.auth.dto.AuthResponse;
import com.telemedicina.auth.dto.LoginRequest;
import com.telemedicina.auth.dto.RegisterRequest;

public interface AuthService {
    AuthResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
}