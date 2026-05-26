package com.studyflow.service;

import com.studyflow.dto.AuthResponse;
import com.studyflow.dto.LoginRequest;
import com.studyflow.dto.RegisterRequest;
import com.studyflow.entity.User;
import com.studyflow.exception.DuplicateResourceException;
import com.studyflow.repository.UserRepository;
import com.studyflow.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtService jwtService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String normalizedEmail = normalizeEmail(request.email());
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new DuplicateResourceException("Email is already registered");
        }

        User savedUser = userRepository.save(new User(
                request.name().trim(),
                normalizedEmail,
                passwordEncoder.encode(request.password())
        ));

        String token = jwtService.generateToken(savedUser.getEmail());
        return AuthResponse.from(savedUser, token);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String normalizedEmail = normalizeEmail(request.email());
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(normalizedEmail, request.password())
        );

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new IllegalStateException("Authenticated user was not found"));

        String token = jwtService.generateToken(user.getEmail());
        return AuthResponse.from(user, token);
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }
}
