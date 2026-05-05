package com.telemedicina.auth.controller;

import com.telemedicina.auth.dto.AuthResponse;
import com.telemedicina.auth.dto.LoginRequest;
import com.telemedicina.auth.dto.RegisterRequest;
import com.telemedicina.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller pentru autentificare.
 * Ambele endpoint-uri sunt PUBLICE (configurate în SecurityConfig).
 */
@RestController
@RequestMapping("/api/auth")
@Tag(name = "Autentificare", description = "Înregistrare și autentificare utilizatori")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * Înregistrează un pacient nou.
     *
     * POST /api/auth/register
     * Body: { "email": "...", "password": "..." }
     * Response 201: { "token": "...", "userId": 1, "email": "...", "role": "PATIENT" }
     */
    @PostMapping("/register")
    @Operation(summary = "Înregistrare pacient nou",
            description = "Creează un cont nou de pacient și returnează un JWT")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Autentifică un utilizator existent.
     *
     * POST /api/auth/login
     * Body: { "email": "...", "password": "..." }
     * Response 200: { "token": "...", "userId": 1, "email": "...", "role": "PATIENT" }
     */
    @PostMapping("/login")
    @Operation(summary = "Autentificare utilizator",
            description = "Returnează un JWT valid pentru utilizatorul autentificat")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }
}