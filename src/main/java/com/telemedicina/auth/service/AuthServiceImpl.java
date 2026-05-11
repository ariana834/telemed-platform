package com.telemedicina.auth.service;

import com.telemedicina.auth.dto.AuthResponse;
import com.telemedicina.auth.dto.LoginRequest;
import com.telemedicina.auth.dto.RegisterRequest;
import com.telemedicina.auth.repository.AuthRepository;
import com.telemedicina.security.CustomUserDetails;
import com.telemedicina.security.JwtService;
import com.telemedicina.shared.config.JwtConfig;
import com.telemedicina.shared.exception.ApiException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthServiceImpl implements AuthService {

    private final AuthRepository authRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final JwtConfig jwtConfig;
    private final AuthenticationManager authenticationManager;

    @Autowired
    public AuthServiceImpl(AuthRepository authRepository,
                           PasswordEncoder passwordEncoder,
                           JwtService jwtService,
                           JwtConfig jwtConfig,
                           AuthenticationManager authenticationManager) {
        this.authRepository = authRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.jwtConfig = jwtConfig;
        this.authenticationManager = authenticationManager;
    }

    @Override
    public AuthResponse register(RegisterRequest request) {
        if (authRepository.existsByEmail(request.getEmail())) {
            throw new ApiException("Există deja un cont cu adresa " + request.getEmail(), HttpStatus.CONFLICT);
        }

        String hashedPassword = passwordEncoder.encode(request.getPassword());


        Long userId = authRepository.createUser(
                request.getEmail(), hashedPassword, "PATIENT"  // ignorăm rolul din request pentru securitate
        );

        //generam tokenul
        String token = jwtService.generateToken(userId, request.getEmail(), "PATIENT");

        return new AuthResponse(token, userId,
                request.getEmail(),
                "PATIENT",
                jwtConfig.getExpirationMs() / 1000  // convertim ms în secunde
        );
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

        String token = jwtService.generateToken(
                userDetails.getUserId(),
                userDetails.getUsername(),  // email
                userDetails.getAuthorities().iterator().next()
                        .getAuthority().replace("ROLE_", "")  // ex: PATIENT
        );
        return new AuthResponse(
                token,
                userDetails.getUserId(),
                userDetails.getUsername(),
                userDetails.getAuthorities().iterator().next().getAuthority().replace("ROLE_", ""),
                jwtConfig.getExpirationMs() / 1000
        );
    }
}