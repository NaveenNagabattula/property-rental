package com.propertyrental.api.service;

import com.propertyrental.api.dto.response.AuthResponse;
import com.propertyrental.api.entity.RefreshToken;
import com.propertyrental.api.entity.User;

public interface RefreshTokenService {

    RefreshToken createRefreshToken(User user);

    RefreshToken validateRefreshToken(String token);

    AuthResponse rotateTokens(String refreshToken);

    void revokeByUser(User user);
}
