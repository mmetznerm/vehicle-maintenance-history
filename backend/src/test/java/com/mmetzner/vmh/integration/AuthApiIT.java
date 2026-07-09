package com.mmetzner.vmh.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mmetzner.vmh.auth.application.JwtService;
import com.mmetzner.vmh.auth.application.dto.AuthTokensResponse;
import com.mmetzner.vmh.auth.application.dto.LoginRequest;
import com.mmetzner.vmh.auth.application.dto.LogoutRequest;
import com.mmetzner.vmh.auth.application.dto.RefreshTokenRequest;
import com.mmetzner.vmh.auth.application.dto.RegisterRequest;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class AuthApiIT {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtService jwtService;

    @DynamicPropertySource
    static void configureDataSource(
            DynamicPropertyRegistry registry
    ) {
        registry.add(
                "spring.datasource.url",
                POSTGRES::getJdbcUrl
        );

        registry.add(
                "spring.datasource.username",
                POSTGRES::getUsername
        );

        registry.add(
                "spring.datasource.password",
                POSTGRES::getPassword
        );
    }

    @Test
    void executesCompleteAuthenticationLifecycle()
            throws Exception {
        String identifier = "auth-"
                + UUID.randomUUID()
                + "@example.com";

        RegisterRequest registerRequest = new RegisterRequest(
                "Authentication User",
                identifier,
                "password123"
        );

        MvcResult registerResult = mockMvc
                .perform(post("/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                registerRequest
                        )))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andReturn();

        AuthTokensResponse registerTokens = readTokens(
                registerResult
        );

        MvcResult loginResult = mockMvc
                .perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LoginRequest(
                                        identifier,
                                        "password123"
                                )
                        )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andReturn();

        AuthTokensResponse loginTokens = readTokens(loginResult);

        assertThat(jwtService.extractUserId(
                loginTokens.accessToken()
        )).isEqualTo(jwtService.extractUserId(
                registerTokens.accessToken()
        ));

        MvcResult refreshResult = mockMvc
                .perform(post("/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RefreshTokenRequest(
                                        loginTokens.refreshToken()
                                )
                        )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andReturn();

        AuthTokensResponse refreshedTokens = readTokens(
                refreshResult
        );

        assertThat(refreshedTokens.refreshToken())
                .isNotEqualTo(loginTokens.refreshToken());

        mockMvc.perform(post("/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RefreshTokenRequest(
                                        loginTokens.refreshToken()
                                )
                        )))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code")
                        .value("INVALID_REFRESH_TOKEN"));

        mockMvc.perform(post("/v1/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LogoutRequest(
                                        refreshedTokens.refreshToken()
                                )
                        )))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RefreshTokenRequest(
                                        refreshedTokens.refreshToken()
                                )
                        )))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code")
                        .value("INVALID_REFRESH_TOKEN"));
    }

    private AuthTokensResponse readTokens(
            MvcResult result
    ) throws Exception {
        return objectMapper.readValue(
                result.getResponse().getContentAsString(),
                AuthTokensResponse.class
        );
    }
}