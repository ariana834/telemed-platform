package com.telemedicina.auth.service;

import com.telemedicina.auth.dto.AuthResponse;
import com.telemedicina.auth.dto.LoginRequest;
import com.telemedicina.auth.dto.RegisterRequest;
import com.telemedicina.auth.repository.AuthRepository;
import com.telemedicina.security.CustomUserDetails;
import com.telemedicina.security.JwtService;
import com.telemedicina.shared.config.JwtConfig;
import com.telemedicina.shared.exception.ApiException;
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
        // Verificăm că email-ul nu e deja înregistrat
        if (authRepository.existsByEmail(request.getEmail())) {
            throw new ApiException("Există deja un cont cu adresa " + request.getEmail(),
                    HttpStatus.CONFLICT);
        }

        // Securizăm parola cu BCrypt înainte de salvare
        // NICIODATĂ nu salvăm parola în clar în baza de date
        String hashedPassword = passwordEncoder.encode(request.getPassword());

        // Înregistrăm doar pacienți prin endpoint-ul public
        // Doctorii și adminii sunt creați separat de admin
        Long userId = authRepository.createUser(
                request.getEmail(),
                hashedPassword,
                "PATIENT"  // ignorăm rolul din request pentru securitate
        );

        // Generăm token-ul imediat — utilizatorul e autentificat automat după register
        String token = jwtService.generateToken(userId, request.getEmail(), "PATIENT");

        return new AuthResponse(
                token,
                userId,
                request.getEmail(),
                "PATIENT",
                jwtConfig.getExpirationMs() / 1000  // convertim ms în secunde
        );
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        // AuthenticationManager verifică email + parolă față de DB
        // Dacă sunt greșite, aruncă BadCredentialsException (prins în GlobalExceptionHandler)
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        // Extragem detaliile utilizatorului autentificat
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

        // Generăm un token JWT nou
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
                userDetails.getAuthorities().iterator().next()
                        .getAuthority().replace("ROLE_", ""),
                jwtConfig.getExpirationMs() / 1000
        );
    }
}