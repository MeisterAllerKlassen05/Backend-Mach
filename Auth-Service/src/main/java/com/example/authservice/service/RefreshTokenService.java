package com.example.authservice.service;

import com.example.authservice.model.entity.RefreshToken;
import com.example.authservice.model.entity.User;
import com.example.authservice.repository.RefreshTokenRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class RefreshTokenService {

    @Value("${app.jwt.refresh-token-expiration-ms:604800000}")
    private long refreshExpiryMs;

    private final RefreshTokenRepository refreshTokenRepository;

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
    }

    //Erstellt einen neuen Refresh-Token für den given User.
    @Transactional
    public RefreshToken createRefreshToken(User user) {
        revokeRefreshTokensByUser(user);

        String token = UUID.randomUUID().toString();
        Instant expiryDate = Instant.now().plusMillis(refreshExpiryMs);

        RefreshToken refreshToken = new RefreshToken(token, expiryDate, user);
        return refreshTokenRepository.save(refreshToken);
    }


     //Validiert und gibt den Refresh-Token zurück. Wirft Exception wenn ungültig/abgelaufen/nicht gefunden.
    public RefreshToken validateRefreshToken(String token) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Ungültiger Refresh-Token"));

        if (refreshToken.isExpired()) {
            deleteRefreshToken(refreshToken);
            throw new RuntimeException("Refresh-Token ist abgelaufen");
        }

        return refreshToken;
    }

     //Widerruft (löscht) den Refresh-Token.
    @Transactional
    public void revokeRefreshToken(String token) {
        refreshTokenRepository.findByToken(token).ifPresent(this::deleteRefreshToken);
    }
    //Löscht Refreshtoken aus der DB.
    @Transactional
    public void deleteRefreshToken(RefreshToken refreshToken) {
        refreshTokenRepository.delete(refreshToken);
    }

    //Widerruft alle Refresh-Tokens eines Users (wichtig für Logout).
    private void revokeRefreshTokensByUser(User user) {
        refreshTokenRepository.findAll().stream()
                .filter(rt -> rt.getUser().getId().equals(user.getId()))
                .forEach(this::deleteRefreshToken);
    }
}
