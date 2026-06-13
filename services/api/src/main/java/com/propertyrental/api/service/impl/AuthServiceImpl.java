package com.propertyrental.api.service.impl;

import com.propertyrental.api.dto.request.ForgotPasswordRequest;
import com.propertyrental.api.dto.request.LoginRequest;
import com.propertyrental.api.dto.request.RefreshRequest;
import com.propertyrental.api.dto.request.RegisterRequest;
import com.propertyrental.api.dto.request.ResetPasswordRequest;
import com.propertyrental.api.dto.response.AuthResponse;
import com.propertyrental.api.dto.response.UserResponse;
import com.propertyrental.api.entity.User;
import com.propertyrental.api.entity.enums.Role;
import com.propertyrental.api.exception.BusinessRuleException;
import com.propertyrental.api.exception.DuplicateEmailException;
import com.propertyrental.api.exception.UnauthorizedException;
import com.propertyrental.api.mapper.UserMapper;
import com.propertyrental.api.repository.UserRepository;
import com.propertyrental.api.security.JwtService;
import com.propertyrental.api.security.LoginAttemptService;
import com.propertyrental.api.service.AuthService;
import com.propertyrental.api.service.EmailService;
import com.propertyrental.api.service.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final RefreshTokenService refreshTokenService;
    private final EmailService emailService;
    private final UserMapper userMapper;
    private final LoginAttemptService loginAttemptService;

    @Value("${app.jwt.expiration}")
    private long jwtExpiration;

    @Override
    @Transactional
    public UserResponse register(RegisterRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new DuplicateEmailException("Email already registered: " + request.getEmail());
        }

        String verificationToken = UUID.randomUUID().toString();

        User user = User.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .role(Role.valueOf(request.getRole()))
                .active(true)
                .emailVerified(false)
                .forgotPasswordToken(verificationToken)
                .forgotPasswordExpiry(Instant.now().plusSeconds(86400)) // 24 hrs
                .build();

        User saved = userRepository.save(user);

        emailService.sendVerificationEmail(saved.getEmail(), saved.getFirstName(), verificationToken);
        log.info("New user registered: {}", saved.getEmail());

        return userMapper.toDto(saved);
    }

    @Override
    public AuthResponse login(LoginRequest request, String clientIp) {
        log.info("Login attempt for email={} from ip={}", request.getEmail(), clientIp);

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );
        } catch (BadCredentialsException ex) {
            loginAttemptService.loginFailed(clientIp);
            log.warn("Login failed for email={} from ip={}: invalid credentials", request.getEmail(), clientIp);
            throw new UnauthorizedException("Invalid email or password");
        }

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> {
                    log.warn("Login failed for email={} from ip={}: user not found after authentication",
                            request.getEmail(), clientIp);
                    return new UnauthorizedException("Invalid email or password");
                });

        if (!user.isActive()) {
            log.warn("Login failed for email={} from ip={}: account inactive", request.getEmail(), clientIp);
            throw new UnauthorizedException("Invalid email or password");
        }

        if (!user.isEmailVerified()) {
            log.warn("Login failed for email={} from ip={}: email not verified", request.getEmail(), clientIp);
            throw new BusinessRuleException("Please verify your email before logging in");
        }

        loginAttemptService.loginSucceeded(clientIp);

        if (hasAdminPortalAccess(user.getRole())) {
            log.info("Admin portal login successful for email={} role={} from ip={}",
                    user.getEmail(), user.getRole(), clientIp);
        } else {
            log.info("Login successful for email={} role={} from ip={} (no admin portal access)",
                    user.getEmail(), user.getRole(), clientIp);
        }

        String accessToken = jwtService.generateToken(user);
        var refreshToken = refreshTokenService.createRefreshToken(user);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken.getToken())
                .tokenType("Bearer")
                .expiresIn(jwtExpiration / 1000)
                .build();
    }

    private boolean hasAdminPortalAccess(Role role) {
        return role == Role.SUPER_ADMIN || role == Role.PROPERTY_MANAGER;
    }

    @Override
    public AuthResponse refreshToken(RefreshRequest request) {
        return refreshTokenService.rotateTokens(request.getRefreshToken());
    }

    @Override
    @Transactional
    public void verifyEmail(String token) {
        User user = userRepository.findByForgotPasswordToken(token)
                .orElseThrow(() -> new BusinessRuleException("Invalid or expired verification token"));

        if (user.getForgotPasswordExpiry() == null || Instant.now().isAfter(user.getForgotPasswordExpiry())) {
            throw new BusinessRuleException("Verification token has expired");
        }

        user.setEmailVerified(true);
        user.setForgotPasswordToken(null);
        user.setForgotPasswordExpiry(null);
        userRepository.save(user);
        log.info("Email verified for user: {}", user.getEmail());
    }

    @Override
    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        // Always return success to prevent email enumeration
        userRepository.findByEmail(request.getEmail()).ifPresent(user -> {
            String resetToken = UUID.randomUUID().toString();
            user.setForgotPasswordToken(resetToken);
            user.setForgotPasswordExpiry(Instant.now().plusSeconds(3600)); // 1 hour
            userRepository.save(user);
            emailService.sendPasswordResetEmail(user.getEmail(), user.getFirstName(), resetToken);
        });
        log.info("Forgot password request processed for: {}", request.getEmail());
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        User user = userRepository.findByForgotPasswordToken(request.getToken())
                .orElseThrow(() -> new BusinessRuleException("Invalid or expired reset token"));

        if (user.getForgotPasswordExpiry() == null || Instant.now().isAfter(user.getForgotPasswordExpiry())) {
            throw new BusinessRuleException("Password reset token has expired");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setForgotPasswordToken(null);
        user.setForgotPasswordExpiry(null);
        userRepository.save(user);

        // Revoke all active refresh tokens for security
        refreshTokenService.revokeByUser(user);
        log.info("Password reset successful for: {}", user.getEmail());
    }
}
