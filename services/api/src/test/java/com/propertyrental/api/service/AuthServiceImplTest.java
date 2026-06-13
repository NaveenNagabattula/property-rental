package com.propertyrental.api.service;

import com.propertyrental.api.dto.request.LoginRequest;
import com.propertyrental.api.dto.request.RegisterRequest;
import com.propertyrental.api.dto.response.AuthResponse;
import com.propertyrental.api.dto.response.UserResponse;
import com.propertyrental.api.entity.RefreshToken;
import com.propertyrental.api.entity.User;
import com.propertyrental.api.entity.enums.Role;
import com.propertyrental.api.exception.BusinessRuleException;
import com.propertyrental.api.exception.DuplicateEmailException;
import com.propertyrental.api.exception.UnauthorizedException;
import com.propertyrental.api.mapper.UserMapper;
import com.propertyrental.api.repository.UserRepository;
import com.propertyrental.api.security.JwtService;
import com.propertyrental.api.security.LoginAttemptService;
import com.propertyrental.api.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthServiceImpl Unit Tests")
class AuthServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private RefreshTokenService refreshTokenService;
    @Mock private EmailService emailService;
    @Mock private UserMapper userMapper;
    @Mock private LoginAttemptService loginAttemptService;

    @InjectMocks
    private AuthServiceImpl authService;

    private User testUser;
    private RegisterRequest registerRequest;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .email("guest@example.com")
                .passwordHash("$2a$12$encodedpassword")
                .firstName("John")
                .lastName("Doe")
                .role(Role.GUEST)
                .active(true)
                .emailVerified(true)
                .build();

        registerRequest = RegisterRequest.builder()
                .email("new@example.com")
                .password("Password123!")
                .firstName("Jane")
                .lastName("Smith")
                .role("GUEST")
                .build();
    }

    // ─── register ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("register — success: new user is saved and verification email is sent")
    void register_success() {
        given(userRepository.findByEmail(registerRequest.getEmail())).willReturn(Optional.empty());
        given(passwordEncoder.encode(anyString())).willReturn("$2a$12$encoded");
        given(userRepository.save(any(User.class))).willAnswer(inv -> inv.getArgument(0));
        given(userMapper.toDto(any(User.class))).willReturn(
                UserResponse.builder().email(registerRequest.getEmail()).build()
        );

        UserResponse result = authService.register(registerRequest);

        assertThat(result.getEmail()).isEqualTo(registerRequest.getEmail());
        verify(emailService).sendVerificationEmail(anyString(), anyString(), anyString());
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("register — throws DuplicateEmailException when email already exists")
    void register_duplicateEmail_throwsException() {
        given(userRepository.findByEmail(registerRequest.getEmail())).willReturn(Optional.of(testUser));

        assertThatThrownBy(() -> authService.register(registerRequest))
                .isInstanceOf(DuplicateEmailException.class)
                .hasMessageContaining("already registered");

        verify(userRepository, never()).save(any());
        verify(emailService, never()).sendVerificationEmail(anyString(), anyString(), anyString());
    }

    // ─── login ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("login — success: returns access and refresh tokens")
    void login_success() {
        LoginRequest loginRequest = LoginRequest.builder()
                .email(testUser.getEmail())
                .password("Password123!")
                .build();

        given(userRepository.findByEmail(testUser.getEmail())).willReturn(Optional.of(testUser));
        given(jwtService.generateToken(testUser)).willReturn("access_token_xyz");
        RefreshToken refreshToken = RefreshToken.builder()
                .token("refresh_token_xyz")
                .user(testUser)
                .expiresAt(Instant.now().plusSeconds(604800))
                .build();
        given(refreshTokenService.createRefreshToken(testUser)).willReturn(refreshToken);

        AuthResponse result = authService.login(loginRequest, "127.0.0.1");

        assertThat(result.getAccessToken()).isEqualTo("access_token_xyz");
        assertThat(result.getRefreshToken()).isEqualTo("refresh_token_xyz");
        assertThat(result.getTokenType()).isEqualTo("Bearer");
        verify(loginAttemptService).loginSucceeded("127.0.0.1");
    }

    @Test
    @DisplayName("login — throws UnauthorizedException for wrong password")
    void login_wrongPassword_throwsException() {
        LoginRequest loginRequest = LoginRequest.builder()
                .email("guest@example.com")
                .password("wrongpassword")
                .build();

        given(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .willThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> authService.login(loginRequest, "127.0.0.1"))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Invalid email or password");

        verify(loginAttemptService).loginFailed("127.0.0.1");
    }

    @Test
    @DisplayName("login — throws BusinessRuleException when email not verified")
    void login_emailNotVerified_throwsException() {
        testUser.setEmailVerified(false);
        LoginRequest loginRequest = LoginRequest.builder()
                .email(testUser.getEmail())
                .password("Password123!")
                .build();

        given(userRepository.findByEmail(testUser.getEmail())).willReturn(Optional.of(testUser));

        assertThatThrownBy(() -> authService.login(loginRequest, "127.0.0.1"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("verify your email");
    }

    // ─── verifyEmail ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("verifyEmail — success: marks account as verified")
    void verifyEmail_success() {
        String token = UUID.randomUUID().toString();
        testUser.setEmailVerified(false);
        testUser.setForgotPasswordToken(token);
        testUser.setForgotPasswordExpiry(Instant.now().plusSeconds(3600));

        given(userRepository.findByForgotPasswordToken(token)).willReturn(Optional.of(testUser));
        given(userRepository.save(any(User.class))).willAnswer(inv -> inv.getArgument(0));

        authService.verifyEmail(token);

        assertThat(testUser.isEmailVerified()).isTrue();
        assertThat(testUser.getForgotPasswordToken()).isNull();
        verify(userRepository).save(testUser);
    }

    @Test
    @DisplayName("verifyEmail — throws BusinessRuleException for invalid token")
    void verifyEmail_invalidToken_throwsException() {
        given(userRepository.findByForgotPasswordToken(anyString())).willReturn(Optional.empty());

        assertThatThrownBy(() -> authService.verifyEmail("bad-token"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Invalid or expired");
    }

    // ─── refreshToken ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("refreshToken — delegates to RefreshTokenService.rotateTokens")
    void refreshToken_delegatesToService() {
        com.propertyrental.api.dto.request.RefreshRequest request =
                com.propertyrental.api.dto.request.RefreshRequest.builder()
                        .refreshToken("old_refresh_token")
                        .build();

        AuthResponse expected = AuthResponse.builder()
                .accessToken("new_access")
                .refreshToken("new_refresh")
                .tokenType("Bearer")
                .build();

        given(refreshTokenService.rotateTokens("old_refresh_token")).willReturn(expected);

        AuthResponse result = authService.refreshToken(request);

        assertThat(result.getAccessToken()).isEqualTo("new_access");
        verify(refreshTokenService).rotateTokens("old_refresh_token");
    }
}

