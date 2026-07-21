package com.mmetzner.vmh.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mmetzner.vmh.auth.application.dto.RegisterRequest;
import com.mmetzner.vmh.vehicle.application.dto.CreateVehicleRequest;
import com.mmetzner.vmh.vehicle.application.dto.UpdateVehicleRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class VehicleApiIT {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("vehicle-maintenance-history")
            .withUsername("vehicle-maintenance-history")
            .withPassword("vehicle-maintenance-history");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @DynamicPropertySource
    static void configureDatasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Test
    void shouldExecuteCompleteVehicleLifecycle() throws Exception {
        String accessToken = registerUserAndGetAccessToken();

        CreateVehicleRequest createVehicleRequest = new CreateVehicleRequest(
                "abc-1234",
                "Honda",
                "Civic",
                2020,
                "Silver"
        );

        String createVehicleResponse = mockMvc.perform(post("/v1/vehicles")
                        .header(AUTHORIZATION, bearer(accessToken))
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createVehicleRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.plate").value("ABC1234"))
                .andExpect(jsonPath("$.brand").value("Honda"))
                .andExpect(jsonPath("$.model").value("Civic"))
                .andExpect(jsonPath("$.manufactureYear").value(2020))
                .andExpect(jsonPath("$.color").value("Silver"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        UUID vehicleId = UUID.fromString(
                objectMapper.readTree(createVehicleResponse).get("id").asText()
        );

        mockMvc.perform(get("/v1/vehicles")
                        .header(AUTHORIZATION, bearer(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(vehicleId.toString()))
                .andExpect(jsonPath("$[0].plate").value("ABC1234"))
                .andExpect(jsonPath("$[0].brand").value("Honda"));

        mockMvc.perform(get("/v1/vehicles/{vehicleId}", vehicleId)
                        .header(AUTHORIZATION, bearer(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(vehicleId.toString()))
                .andExpect(jsonPath("$.plate").value("ABC1234"));

        UpdateVehicleRequest updateVehicleRequest = new UpdateVehicleRequest(
                "xyz-9876",
                "Toyota",
                "Corolla",
                2024,
                "Black"
        );

        mockMvc.perform(put("/v1/vehicles/{vehicleId}", vehicleId)
                        .header(AUTHORIZATION, bearer(accessToken))
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateVehicleRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(vehicleId.toString()))
                .andExpect(jsonPath("$.plate").value("XYZ9876"))
                .andExpect(jsonPath("$.brand").value("Toyota"))
                .andExpect(jsonPath("$.model").value("Corolla"))
                .andExpect(jsonPath("$.manufactureYear").value(2024))
                .andExpect(jsonPath("$.color").value("Black"));

        mockMvc.perform(delete("/v1/vehicles/{vehicleId}", vehicleId)
                        .header(AUTHORIZATION, bearer(accessToken)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/v1/vehicles/{vehicleId}", vehicleId)
                        .header(AUTHORIZATION, bearer(accessToken)))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldRejectDuplicatedPlateForSameUser() throws Exception {
        String accessToken = registerUserAndGetAccessToken();

        CreateVehicleRequest request = new CreateVehicleRequest(
                "abc-1234",
                "Honda",
                "Civic",
                2020,
                "Silver"
        );

        mockMvc.perform(post("/v1/vehicles")
                        .header(AUTHORIZATION, bearer(accessToken))
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/v1/vehicles")
                        .header(AUTHORIZATION, bearer(accessToken))
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void shouldRequireAuthenticationToAccessVehicles() throws Exception {
        mockMvc.perform(get("/v1/vehicles"))
                .andExpect(status().isUnauthorized());
    }

    private String registerUserAndGetAccessToken() throws Exception {
        String uniqueEmail = "user-" + UUID.randomUUID() + "@email.com";

        RegisterRequest registerRequest = new RegisterRequest(
                "Maycon Metzner",
                uniqueEmail,
                "password123"
        );

        String response = mockMvc.perform(post("/v1/auth/register")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").exists())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode json = objectMapper.readTree(response);
        String accessToken = json.get("accessToken").asText();

        assertThat(accessToken).isNotBlank();

        return accessToken;
    }

    private String bearer(String accessToken) {
        return "Bearer " + accessToken;
    }
}
