package com.mmetzner.vmh.auth.presentation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mmetzner.vmh.auth.application.AuthService;
import com.mmetzner.vmh.auth.application.JwtService;
import com.mmetzner.vmh.auth.application.dto.AuthTokensResponse;
import com.mmetzner.vmh.auth.application.dto.LoginRequest;
import com.mmetzner.vmh.auth.infrastructure.security.JwtAuthenticationFilter;
import com.mmetzner.vmh.auth.infrastructure.security.SecurityConfig;
import com.mmetzner.vmh.shared.exception.ApiErrorCode;

@WebMvcTest(
        controllers = AuthController.class,
        excludeAutoConfiguration =
                UserDetailsServiceAutoConfiguration.class
)
@Import({
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        ApiSecurityHttpTests.ProtectedTestController.class
})
@TestPropertySource(properties =
        "app.security.cors.allowed-origins=https://app.example.com"
)
class ApiSecurityHttpTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private JwtService jwtService;

    @Test
    void allowsLoginWithoutBearerToken() throws Exception {
        LoginRequest request = new LoginRequest(
                "maycon@example.com",
                "password123"
        );

        when(authService.login(any(LoginRequest.class)))
                .thenReturn(new AuthTokensResponse(
                        "access-token",
                        "refresh-token"
                ));

        mockMvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken")
                        .value("access-token"));
    }

    @Test
    void rejectsProtectedEndpointWithoutToken() throws Exception {
        mockMvc.perform(get("/v1/protected"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code")
                        .value(ApiErrorCode.UNAUTHENTICATED.name()))
                .andExpect(jsonPath("$.path")
                        .value("/v1/protected"));
    }

    @Test
    void allowsProtectedEndpointWithValidToken() throws Exception {
        UUID userId = UUID.randomUUID();

        when(jwtService.extractUserId("valid-token"))
                .thenReturn(userId);

        mockMvc.perform(get("/v1/protected")
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                "Bearer valid-token"
                        ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId")
                        .value(userId.toString()));
    }

    @RestController
    static class ProtectedTestController {

        @GetMapping("/v1/protected")
        Map<String, String> protectedEndpoint(
                Authentication authentication
        ) {
            return Map.of(
                    "userId",
                    authentication.getPrincipal().toString()
            );
        }
    }
}