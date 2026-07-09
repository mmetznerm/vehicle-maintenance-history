package com.mmetzner.vmh.vehicle.presentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mmetzner.vmh.shared.exception.GlobalExceptionHandler;
import com.mmetzner.vmh.vehicle.application.VehicleService;
import com.mmetzner.vmh.vehicle.application.dto.CreateVehicleRequest;
import com.mmetzner.vmh.vehicle.application.dto.VehicleResponse;
import com.mmetzner.vmh.vehicle.application.dto.VehicleSummaryResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.junit.jupiter.api.AfterEach;
import org.springframework.security.core.context.SecurityContextHolder;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doNothing;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class VehicleControllerTests {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    private VehicleService vehicleService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        vehicleService = mock(VehicleService.class);

        mockMvc = standaloneSetup(new VehicleController(vehicleService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldRegisterVehicle() throws Exception {
        UUID ownerId = UUID.randomUUID();
        UUID vehicleId = UUID.randomUUID();

        CreateVehicleRequest request = new CreateVehicleRequest(
                "abc-1234",
                "Honda",
                "Civic",
                2020,
                "Prata"
        );

        VehicleResponse response = new VehicleResponse(
                vehicleId,
                "ABC1234",
                "Honda",
                "Civic",
                2020,
                "Prata",
                OffsetDateTime.now(),
                OffsetDateTime.now()
        );

        when(vehicleService.registerVehicle(ownerId, request)).thenReturn(response);

        authenticateAs(ownerId);

        mockMvc.perform(post("/v1/vehicles")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(vehicleId.toString()))
                .andExpect(jsonPath("$.plate").value("ABC1234"))
                .andExpect(jsonPath("$.brand").value("Honda"))
                .andExpect(jsonPath("$.model").value("Civic"));
    }

    @Test
    void shouldListVehicles() throws Exception {
        UUID ownerId = UUID.randomUUID();
        UUID vehicleId = UUID.randomUUID();

        when(vehicleService.listVehicles(ownerId))
                .thenReturn(List.of(new VehicleSummaryResponse(
                        vehicleId,
                        "ABC1234",
                        "Honda",
                        "Civic",
                        2020,
                        "Prata"
                )));

        authenticateAs(ownerId);

        mockMvc.perform(get("/v1/vehicles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(vehicleId.toString()))
                .andExpect(jsonPath("$[0].plate").value("ABC1234"));
    }

    @Test
    void shouldFindVehicle() throws Exception {
        UUID ownerId = UUID.randomUUID();
        UUID vehicleId = UUID.randomUUID();

        when(vehicleService.findVehicle(ownerId, vehicleId))
                .thenReturn(new VehicleResponse(
                        vehicleId,
                        "ABC1234",
                        "Honda",
                        "Civic",
                        2020,
                        "Prata",
                        OffsetDateTime.now(),
                        OffsetDateTime.now()
                ));

        authenticateAs(ownerId);

        mockMvc.perform(get("/v1/vehicles/{vehicleId}", vehicleId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(vehicleId.toString()))
                .andExpect(jsonPath("$.plate").value("ABC1234"));
    }

    @Test
    void shouldDeleteVehicle() throws Exception {
        UUID ownerId = UUID.randomUUID();
        UUID vehicleId = UUID.randomUUID();

        doNothing().when(vehicleService).deleteVehicle(ownerId, vehicleId);

        authenticateAs(ownerId);

        mockMvc.perform(delete("/v1/vehicles/{vehicleId}", vehicleId))
                .andExpect(status().isNoContent());
    }

    private void authenticateAs(UUID ownerId) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(ownerId, null, List.of())
        );
    }
}