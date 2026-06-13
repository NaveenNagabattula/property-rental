package com.propertyrental.api.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.propertyrental.api.dto.request.LoginRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("RateLimitFilter Integration Tests")
class RateLimitFilterTest {

    private LoginAttemptService loginAttemptService;
    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @RestController
    static class DummyAuthController {
        @PostMapping("/api/v1/auth/login")
        public void login(@RequestBody LoginRequest request) {
            // Dummy endpoint
        }
    }

    @BeforeEach
    void setUp() {
        loginAttemptService = new LoginAttemptService();
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        RateLimitFilter rateLimitFilter = new RateLimitFilter(loginAttemptService, objectMapper);

        mockMvc = MockMvcBuilders.standaloneSetup(new DummyAuthController())
                .addFilters(rateLimitFilter)
                .build();
    }

    @Test
    @DisplayName("Login rate limiting — blocks 6th login attempt after 5 failures")
    void whenFiveFailedLoginAttempts_thenSixthIsBlocked() throws Exception {
        String clientIp = "192.168.1.100";
        LoginRequest request = LoginRequest.builder()
                .email("test@example.com")
                .password("wrongpassword")
                .build();
        String json = objectMapper.writeValueAsString(request);

        // First 5 requests should go through to the endpoint (simulate failures)
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/v1/auth/login")
                            .with(req -> {
                                req.setRemoteAddr(clientIp);
                                return req;
                            })
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isOk());
            
            // Register failure
            loginAttemptService.loginFailed(clientIp);
        }

        // The 6th request should be blocked with 423 Locked
        mockMvc.perform(post("/api/v1/auth/login")
                        .with(req -> {
                            req.setRemoteAddr(clientIp);
                            return req;
                        })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isLocked())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Too many login attempts. Login is temporarily locked from this IP for 15 minutes."));
    }
}
