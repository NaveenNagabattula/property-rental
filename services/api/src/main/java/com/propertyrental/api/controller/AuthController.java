package com.propertyrental.api.controller;

import com.propertyrental.api.dto.request.ForgotPasswordRequest;
import com.propertyrental.api.dto.request.LoginRequest;
import com.propertyrental.api.dto.request.RefreshRequest;
import com.propertyrental.api.dto.request.RegisterRequest;
import com.propertyrental.api.dto.request.ResetPasswordRequest;
import com.propertyrental.api.dto.response.ApiResponse;
import com.propertyrental.api.dto.response.AuthResponse;
import com.propertyrental.api.dto.response.UserResponse;
import com.propertyrental.api.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "Authentication and account management endpoints")
public class AuthController {

    private final AuthService authService;
    private final com.propertyrental.api.mapper.UserMapper userMapper;

    @GetMapping("/me")
    @Operation(summary = "Get current authenticated user profile", security = @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser(
            @org.springframework.security.core.annotation.AuthenticationPrincipal com.propertyrental.api.entity.User currentUser
    ) {
        if (currentUser == null) {
            throw new com.propertyrental.api.exception.UnauthorizedException("Not authenticated");
        }
        UserResponse response = userMapper.toDto(currentUser);
        return ResponseEntity.ok(
                ApiResponse.<UserResponse>builder()
                        .success(true)
                        .message("Current user profile retrieved successfully")
                        .data(response)
                        .build()
        );
    }

    @PostMapping("/register")
    @Operation(summary = "Register a new user", description = "Creates a new HOST or GUEST account and sends a verification email")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "User registered. Email verification sent."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation error or duplicate email")
    })
    public ResponseEntity<ApiResponse<UserResponse>> register(@Valid @RequestBody RegisterRequest request) {
        log.info("POST /api/v1/auth/register received for email={} role={}", request.getEmail(), request.getRole());
        UserResponse user = authService.register(request);
        log.info("Registration successful for email={}", request.getEmail());
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.<UserResponse>builder()
                        .success(true)
                        .message("Registration successful. Please check your email to verify your account.")
                        .data(user)
                        .build()
        );
    }

    @PostMapping("/login")
    @Operation(summary = "Login with email and password", description = "Authenticates a user and returns JWT access and refresh tokens")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Login successful. Returns access + refresh tokens."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Invalid credentials"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "423", description = "Account locked due to too many failed attempts")
    })
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest
    ) {
        String clientIp = getClientIp(httpRequest);
        log.info("POST /api/v1/auth/login received for email={} from ip={}", request.getEmail(), clientIp);
        AuthResponse authResponse = authService.login(request, clientIp);
        log.info("POST /api/v1/auth/login successful for email={} from ip={}", request.getEmail(), clientIp);
        return ResponseEntity.ok(
                ApiResponse.<AuthResponse>builder()
                        .success(true)
                        .message("Login successful")
                        .data(authResponse)
                        .build()
        );
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token", description = "Issues a new access token using a valid refresh token")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "New access token issued"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Invalid or expired refresh token")
    })
    public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(@Valid @RequestBody RefreshRequest request) {
        log.info("POST /api/v1/auth/refresh received");
        AuthResponse authResponse = authService.refreshToken(request);
        log.info("Token refresh successful");
        return ResponseEntity.ok(
                ApiResponse.<AuthResponse>builder()
                        .success(true)
                        .message("Token refreshed successfully")
                        .data(authResponse)
                        .build()
        );
    }

    @GetMapping("/verify-email")
    @Operation(summary = "Verify user email address", description = "Marks the user account as email-verified using the token from the verification email")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Email verified successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid or expired token")
    })
    public ResponseEntity<ApiResponse<Void>> verifyEmail(@RequestParam String token) {
        log.info("GET /api/v1/auth/verify-email received");
        authService.verifyEmail(token);
        log.info("Email verification successful");
        return ResponseEntity.ok(
                ApiResponse.<Void>builder()
                        .success(true)
                        .message("Email verified successfully. You can now log in.")
                        .build()
        );
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Trigger password reset email", description = "Sends a password reset link to the given email if it exists in the system")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Password reset email sent if account exists")
    })
    public ResponseEntity<ApiResponse<Void>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        log.info("POST /api/v1/auth/forgot-password received for email={}", request.getEmail());
        authService.forgotPassword(request);
        log.info("Forgot password email triggered for email={}", request.getEmail());
        return ResponseEntity.ok(
                ApiResponse.<Void>builder()
                        .success(true)
                        .message("If this email exists, a reset link has been sent.")
                        .build()
        );
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Reset password with token", description = "Resets the user password using the token from the reset email")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Password reset successful"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid or expired token")
    })
    public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        log.info("POST /api/v1/auth/reset-password received");
        authService.resetPassword(request);
        log.info("Password reset successful");
        return ResponseEntity.ok(
                ApiResponse.<Void>builder()
                        .success(true)
                        .message("Password reset successful. Please log in with your new password.")
                        .build()
        );
    }

    private String getClientIp(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0].trim();
    }
}
