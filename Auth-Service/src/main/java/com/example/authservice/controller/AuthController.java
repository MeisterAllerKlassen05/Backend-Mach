package com.example.authservice.controller;

import com.example.authservice.auth.JwtTokenProvider;
import com.example.authservice.auth.dto.JwtResponse;
import com.example.authservice.auth.dto.LoginRequest;
import com.example.authservice.model.entity.RefreshToken;
import com.example.authservice.model.entity.User;
import com.example.authservice.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final com.example.authservice.service.RefreshTokenService refreshTokenService;

    public AuthController(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtTokenProvider tokenProvider,
            com.example.authservice.service.RefreshTokenService refreshTokenService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.tokenProvider = tokenProvider;
        this.refreshTokenService = refreshTokenService;
    }


    // Öffentlicher Endpunkt: für Registrierung + Pfrüfung von Username und Passwort
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "statusCode", 400,
                    "error", "Username bereits vergeben!"));
        }

        if (request.getPassword() == null || request.getPassword().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "statusCode", 400,
                    "error", "Passwort darf nicht leer sein!"
            ));
        }

        if (request.getPassword().length() < 8) {
            return ResponseEntity.badRequest().body(Map.of(
                    "statusCode", 400,
                    "error", "Passwort muss mindestens 8 Zeichen lang sein!"
            ));
        }


        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole("USER");  // Immer USER

        userRepository.save(user);
        return ResponseEntity.ok(Map.of(
                "statusCode", 201,
                "message", "User erfolgreich registriert!"));
    }


    // Öffentlicher Endpunkt: für Login, gibt jetzt AccessToken + RefreshToken zurück
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
            );
        } catch (AuthenticationException e) {
            return ResponseEntity.status(401).body(Map.of(
                    "statusCode", 401,
                    "error", "Ungültiger Benutzername oder Passwort!"));
        }

        User user = userRepository.findByUsername(request.getUsername()).orElseThrow();
        String accessToken = tokenProvider.generateAccessToken(user.getUsername(), user.getRole());
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

        return ResponseEntity.ok(new JwtResponse(
                accessToken,
                refreshToken.getToken(),
                user.getUsername(),
                user.getRole(),
                user.isActive()
        ));
    }


    //Geschützer Endpoint: Token refresh ohne Login
    @PostMapping("/refresh-token")
    public ResponseEntity<?> refreshToken(@RequestHeader("Authorization") String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(401).body(Map.of(
                    "statusCode", 401,
                    "error", "Fehlender oder ungültiger Authorization-Header"));
        }

        String token = authHeader.substring(7);

        try {
            RefreshToken refreshToken = refreshTokenService.validateRefreshToken(token);
            User user = refreshToken.getUser();

            String newAccessToken = tokenProvider.generateAccessToken(user.getUsername(), user.getRole());

            return ResponseEntity.ok(Map.of(
                    "accessToken", newAccessToken,
                    "refreshToken", refreshToken.getToken()
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.status(401).body(Map.of(
                    "statusCode", 401,
                    "error", e.getMessage()));
        }
    }


    //Geschützter Endpoint: Logout, widerruft den Refresh-Token
    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestHeader("Authorization") String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(401).body(Map.of(
                    "statusCode", 401,
                    "error", "Fehlender oder ungültiger Authorization-Header"));
        }

        String token = authHeader.substring(7);
        refreshTokenService.revokeRefreshToken(token);

        return ResponseEntity.ok(Map.of(
                "statusCode", 200,
                "message", "Erfolgreich ausgeloggt!"));
    }

    public static class RegisterRequest {
        private String username;
        private String password;
        private String role;

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
    }
}
