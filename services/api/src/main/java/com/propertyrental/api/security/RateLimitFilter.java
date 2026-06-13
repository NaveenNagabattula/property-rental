package com.propertyrental.api.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.propertyrental.api.dto.response.ApiResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import org.springframework.lang.NonNull;

import java.io.IOException;
import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private final LoginAttemptService loginAttemptService;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        if ("POST".equalsIgnoreCase(request.getMethod()) && request.getRequestURI().endsWith("/api/v1/auth/login")) {
            String ip = getClientIP(request);
            if (loginAttemptService.isBlocked(ip)) {
                log.warn("Login request blocked for ip={}: too many failed attempts", ip);
                response.setStatus(HttpStatus.LOCKED.value());
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                
                ApiResponse<Void> apiResponse = ApiResponse.<Void>builder()
                        .success(false)
                        .message("Too many login attempts. Login is temporarily locked from this IP for 15 minutes.")
                        .timestamp(Instant.now())
                        .build();

                response.getWriter().write(objectMapper.writeValueAsString(apiResponse));
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private String getClientIP(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0];
    }
}
