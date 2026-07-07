package com.mmetzner.vmh.maintenance.presentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mmetzner.vmh.maintenance.application.MaintenanceService;
import com.mmetzner.vmh.maintenance.application.dto.CreateMaintenanceRequest;
import com.mmetzner.vmh.maintenance.application.dto.MaintenanceResponse;
import com.mmetzner.vmh.shared.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class MaintenanceControllerTests {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    private MaintenanceService maintenanceService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        maintenanceService = mock(MaintenanceService.class);

        mockMvc = standaloneSetup(new MaintenanceController(maintenanceService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldRegisterMaintenance() throws Exception {
        UUID ownerId = UUID.randomUUID();
        UUID vehicleId = UUID.randomUUID();
        UUID maintenanceId = UUID.randomUUID();

        authenticateAs(ownerId);

        CreateMaintenanceRequest request = new CreateMaintenanceRequest(
                LocalDate.of(2026, 7, 7),
                35_000,
                "Troca de óleo",
                new BigDecimal("250.00")
        );

        when(maintenanceService.registerMaintenance(ownerId, vehicleId, request))
                .thenReturn(response(maintenanceId, vehicleId));

        mockMvc.perform(post("/v1/vehicles/{vehicleId}/maintenances", vehicleId)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(maintenanceId.toString()))
                .andExpect(jsonPath("$.vehicleId").value(vehicleId.toString()))
                .andExpect(jsonPath("$.description").value("Troca de óleo"));
    }

    @Test
    void shouldListMaintenances() throws Exception {
        UUID ownerId = UUID.randomUUID();
        UUID vehicleId = UUID.randomUUID();
        UUID maintenanceId = UUID.randomUUID();

        authenticateAs(ownerId);

        when(maintenanceService.listMaintenances(ownerId, vehicleId))
                .thenReturn(List.of(response(maintenanceId, vehicleId)));

        mockMvc.perform(get("/v1/vehicles/{vehicleId}/maintenances", vehicleId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(maintenanceId.toString()))
                .andExpect(jsonPath("$[0].vehicleId").value(vehicleId.toString()));
    }

    @Test
    void shouldFindMaintenance() throws Exception {
        UUID ownerId = UUID.randomUUID();
        UUID vehicleId = UUID.randomUUID();
        UUID maintenanceId = UUID.randomUUID();

        authenticateAs(ownerId);

        when(maintenanceService.findMaintenance(ownerId, vehicleId, maintenanceId))
                .thenReturn(response(maintenanceId, vehicleId));

        mockMvc.perform(get(
                        "/v1/vehicles/{vehicleId}/maintenances/{maintenanceId}",
                        vehicleId,
                        maintenanceId
                ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(maintenanceId.toString()))
                .andExpect(jsonPath("$.vehicleId").value(vehicleId.toString()));
    }

    @Test
    void shouldDeleteMaintenance() throws Exception {
        UUID ownerId = UUID.randomUUID();
        UUID vehicleId = UUID.randomUUID();
        UUID maintenanceId = UUID.randomUUID();

        authenticateAs(ownerId);

        doNothing().when(maintenanceService)
                .deleteMaintenance(ownerId, vehicleId, maintenanceId);

        mockMvc.perform(delete(
                        "/v1/vehicles/{vehicleId}/maintenances/{maintenanceId}",
                        vehicleId,
                        maintenanceId
                ))
                .andExpect(status().isNoContent());
    }

    @Test
    void shouldRejectInvalidMaintenanceRequest() throws Exception {
        UUID ownerId = UUID.randomUUID();
        UUID vehicleId = UUID.randomUUID();

        authenticateAs(ownerId);

        String invalidRequest = """
                {
                  "maintenanceDate": "2026-07-07",
                  "odometer": -1,
                  "description": "",
                  "cost": -10
                }
                """;

        mockMvc.perform(post("/v1/vehicles/{vehicleId}/maintenances", vehicleId)
                        .contentType(APPLICATION_JSON)
                        .content(invalidRequest))
                .andExpect(status().isBadRequest());
    }

    private MaintenanceResponse response(UUID maintenanceId, UUID vehicleId) {
        return new MaintenanceResponse(
                maintenanceId,
                vehicleId,
                LocalDate.of(2026, 7, 7),
                35_000,
                "Troca de óleo",
                new BigDecimal("250.00"),
                OffsetDateTime.now(),
                OffsetDateTime.now()
        );
    }

    private void authenticateAs(UUID ownerId) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(ownerId, null, List.of())
        );
    }
}