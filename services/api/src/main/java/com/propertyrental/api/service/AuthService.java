package com.propertyrental.api.service;

import com.propertyrental.api.dto.request.ForgotPasswordRequest;
import com.propertyrental.api.dto.request.LoginRequest;
import com.propertyrental.api.dto.request.RefreshRequest;
import com.propertyrental.api.dto.request.RegisterRequest;
import com.propertyrental.api.dto.request.ResetPasswordRequest;
import com.propertyrental.api.dto.response.AuthResponse;
import com.propertyrental.api.dto.response.UserResponse;

public interface AuthService {

    UserResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request, String clientIp);

    AuthResponse refreshToken(RefreshRequest request);

    void verifyEmail(String token);

    void forgotPassword(ForgotPasswordRequest request);

    void resetPassword(ResetPasswordRequest request);
}
