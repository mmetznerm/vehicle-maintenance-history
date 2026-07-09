package com.mmetzner.vmh.auth.presentation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mmetzner.vmh.auth.application.AuthService;
import com.mmetzner.vmh.auth.application.dto.AuthTokensResponse;
import com.mmetzner.vmh.auth.application.dto.LoginRequest;
import com.mmetzner.vmh.auth.application.dto.RegisterRequest;
import com.mmetzner.vmh.shared.exception.GlobalExceptionHandler;

@ExtendWith(MockitoExtension.class)
class AuthControllerTests {

    @Mock
    private AuthService authService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();

        mockMvc = MockMvcBuilders
                .standaloneSetup(new AuthController(authService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void registersUserAndReturnsCreated() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "Maycon Metzner",
                "maycon@example.com",
                "password123"
        );

        when(authService.register(any(RegisterRequest.class)))
                .thenReturn(new AuthTokensResponse(
                        "access-token",
                        "refresh-token"
                ));

        mockMvc.perform(post("/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken")
                        .value("access-token"))
                .andExpect(jsonPath("$.refreshToken")
                        .value("refresh-token"));
    }

    @Test
    void logsInUserAndReturnsTokens() throws Exception {
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
                        .value("access-token"))
                .andExpect(jsonPath("$.refreshToken")
                        .value("refresh-token"));
    }

    @Test
    void rejectsInvalidRegistrationRequest() throws Exception {
        String request = """
                {
                  "fullName": "Maycon Metzner",
                  "emailOrPhone": "",
                  "password": "password123"
                }
                """;

        mockMvc.perform(post("/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code")
                        .value("REQUEST_VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors[0].field")
                        .value("emailOrPhone"));

        verifyNoInteractions(authService);
    }
}