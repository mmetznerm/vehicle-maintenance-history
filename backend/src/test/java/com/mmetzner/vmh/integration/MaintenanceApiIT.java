package com.mmetzner.vmh.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mmetzner.vmh.auth.application.dto.RegisterRequest;
import com.mmetzner.vmh.maintenance.application.dto.CreateMaintenanceRequest;
import com.mmetzner.vmh.maintenance.application.dto.UpdateMaintenanceRequest;
import com.mmetzner.vmh.vehicle.application.dto.CreateVehicleRequest;
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

import java.math.BigDecimal;
import java.time.LocalDate;
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
class MaintenanceApiIT {

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
    void shouldExecuteCompleteMaintenanceLifecycle() throws Exception {
        String accessToken = registerUserAndGetAccessToken();
        UUID vehicleId = createVehicleAndReturnId(accessToken);

        CreateMaintenanceRequest createRequest = new CreateMaintenanceRequest(
                LocalDate.of(2026, 7, 7),
                35_000,
                "Oil change",
                new BigDecimal("250.00")
        );

        String createResponse = mockMvc.perform(post("/v1/vehicles/{vehicleId}/maintenances", vehicleId)
                        .header(AUTHORIZATION, bearer(accessToken))
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.vehicleId").value(vehicleId.toString()))
                .andExpect(jsonPath("$.maintenanceDate").value("2026-07-07"))
                .andExpect(jsonPath("$.odometer").value(35_000))
                .andExpect(jsonPath("$.description").value("Oil change"))
                .andExpect(jsonPath("$.cost").value(250.00))
                .andReturn()
                .getResponse()
                .getContentAsString();

        UUID maintenanceId = UUID.fromString(
                objectMapper.readTree(createResponse).get("id").asText()
        );

        mockMvc.perform(get("/v1/vehicles/{vehicleId}/maintenances", vehicleId)
                        .header(AUTHORIZATION, bearer(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(maintenanceId.toString()))
                .andExpect(jsonPath("$[0].vehicleId").value(vehicleId.toString()))
                .andExpect(jsonPath("$[0].description").value("Oil change"));

        mockMvc.perform(get(
                        "/v1/vehicles/{vehicleId}/maintenances/{maintenanceId}",
                        vehicleId,
                        maintenanceId
                )
                        .header(AUTHORIZATION, bearer(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(maintenanceId.toString()))
                .andExpect(jsonPath("$.vehicleId").value(vehicleId.toString()));

        UpdateMaintenanceRequest updateRequest = new UpdateMaintenanceRequest(
                LocalDate.of(2026, 7, 8),
                36_000,
                "Oil and filter change",
                new BigDecimal("300.00")
        );

        mockMvc.perform(put(
                        "/v1/vehicles/{vehicleId}/maintenances/{maintenanceId}",
                        vehicleId,
                        maintenanceId
                )
                        .header(AUTHORIZATION, bearer(accessToken))
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(maintenanceId.toString()))
                .andExpect(jsonPath("$.maintenanceDate").value("2026-07-08"))
                .andExpect(jsonPath("$.odometer").value(36_000))
                .andExpect(jsonPath("$.description").value("Oil and filter change"))
                .andExpect(jsonPath("$.cost").value(300.00));

        mockMvc.perform(delete(
                        "/v1/vehicles/{vehicleId}/maintenances/{maintenanceId}",
                        vehicleId,
                        maintenanceId
                )
                        .header(AUTHORIZATION, bearer(accessToken)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get(
                        "/v1/vehicles/{vehicleId}/maintenances/{maintenanceId}",
                        vehicleId,
                        maintenanceId
                )
                        .header(AUTHORIZATION, bearer(accessToken)))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldRejectMaintenanceAccessWhenVehicleBelongsToAnotherUser() throws Exception {
        String ownerAccessToken = registerUserAndGetAccessToken();
        UUID ownerVehicleId = createVehicleAndReturnId(ownerAccessToken);

        String anotherUserAccessToken = registerUserAndGetAccessToken();

        CreateMaintenanceRequest request = new CreateMaintenanceRequest(
                LocalDate.of(2026, 7, 7),
                35_000,
                "Oil change",
                new BigDecimal("250.00")
        );

        mockMvc.perform(post("/v1/vehicles/{vehicleId}/maintenances", ownerVehicleId)
                        .header(AUTHORIZATION, bearer(anotherUserAccessToken))
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldRejectInvalidMaintenanceRequest() throws Exception {
        String accessToken = registerUserAndGetAccessToken();
        UUID vehicleId = createVehicleAndReturnId(accessToken);

        String invalidRequest = """
                {
                  "maintenanceDate": "2026-07-07",
                  "odometer": -1,
                  "description": "",
                  "cost": -10
                }
                """;

        mockMvc.perform(post("/v1/vehicles/{vehicleId}/maintenances", vehicleId)
                        .header(AUTHORIZATION, bearer(accessToken))
                        .contentType(APPLICATION_JSON)
                        .content(invalidRequest))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRequireAuthenticationToAccessMaintenances() throws Exception {
        UUID vehicleId = UUID.randomUUID();

        mockMvc.perform(get("/v1/vehicles/{vehicleId}/maintenances", vehicleId))
                .andExpect(status().isUnauthorized());
    }

    private UUID createVehicleAndReturnId(String accessToken) throws Exception {
        CreateVehicleRequest request = new CreateVehicleRequest(
                "abc-" + UUID.randomUUID().toString().substring(0, 4),
                "Honda",
                "Civic",
                2020,
                "Silver"
        );

        String response = mockMvc.perform(post("/v1/vehicles")
                        .header(AUTHORIZATION, bearer(accessToken))
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return UUID.fromString(objectMapper.readTree(response).get("id").asText());
    }

    private String registerUserAndGetAccessToken() throws Exception {
        String uniqueEmail = "user-" + UUID.randomUUID() + "@email.com";

        RegisterRequest registerRequest = new RegisterRequest(
                "Maycon Metzner",
                uniqueEmail,
                "StrongPassword123"
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
