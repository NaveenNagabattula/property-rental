package com.propertyrental.api.service.impl;

import com.propertyrental.api.dto.response.AuthResponse;
import com.propertyrental.api.entity.RefreshToken;
import com.propertyrental.api.entity.User;
import com.propertyrental.api.exception.UnauthorizedException;
import com.propertyrental.api.repository.RefreshTokenRepository;
import com.propertyrental.api.security.JwtService;
import com.propertyrental.api.service.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;

    @Value("${app.jwt.refresh-expiration}")
    private long refreshExpiration;

    @Value("${app.jwt.expiration}")
    private long jwtExpiration;

    @Override
    @Transactional
    public RefreshToken createRefreshToken(User user) {
        log.info("Creating new refresh token for user: {}", user.getEmail());
        // Revoke any existing tokens for this user
        refreshTokenRepository.deleteByUser(user);

        RefreshToken refreshToken = RefreshToken.builder()
                .id(UUID.randomUUID())
                .token(UUID.randomUUID().toString())
                .user(user)
                .expiresAt(Instant.now().plusMillis(refreshExpiration))
                .revoked(false)
                .build();

        return refreshTokenRepository.save(refreshToken);
    }

    @Override
    public RefreshToken validateRefreshToken(String token) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> {
                    log.warn("Refresh token validation failed: token not found");
                    return new UnauthorizedException("Refresh token not found");
                });

        if (refreshToken.isRevoked()) {
            log.warn("Refresh token validation failed: token has been revoked for user: {}", refreshToken.getUser().getEmail());
            throw new UnauthorizedException("Refresh token has been revoked");
        }

        if (refreshToken.getExpiresAt().isBefore(Instant.now())) {
            log.warn("Refresh token validation failed: token has expired for user: {}", refreshToken.getUser().getEmail());
            throw new UnauthorizedException("Refresh token has expired");
        }

        log.info("Refresh token validated successfully for user: {}", refreshToken.getUser().getEmail());
        return refreshToken;
    }

    @Override
    @Transactional
    public AuthResponse rotateTokens(String token) {
        RefreshToken existingToken = validateRefreshToken(token);
        User user = existingToken.getUser();

        log.info("Rotating tokens for user: {}", user.getEmail());

        // Revoke old token
        existingToken.setRevoked(true);
        refreshTokenRepository.save(existingToken);

        // Issue new tokens
        String newAccessToken = jwtService.generateToken(user);
        RefreshToken newRefreshToken = createRefreshToken(user);

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken.getToken())
                .tokenType("Bearer")
                .expiresIn(jwtExpiration / 1000)
                .build();
    }

    @Override
    @Transactional
    public void revokeByUser(User user) {
        log.info("Revoking all active refresh tokens for user: {}", user.getEmail());
        refreshTokenRepository.deleteByUser(user);
    }
}
