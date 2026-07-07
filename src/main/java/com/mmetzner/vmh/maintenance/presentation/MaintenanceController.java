package com.mmetzner.vmh.maintenance.presentation;

import com.mmetzner.vmh.maintenance.application.MaintenanceService;
import com.mmetzner.vmh.maintenance.application.dto.CreateMaintenanceRequest;
import com.mmetzner.vmh.maintenance.application.dto.MaintenanceResponse;
import com.mmetzner.vmh.maintenance.application.dto.UpdateMaintenanceRequest;
import com.mmetzner.vmh.shared.config.OpenApiConfig;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/vehicles/{vehicleId}/maintenances")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
@RequiredArgsConstructor
public class MaintenanceController {

    private final MaintenanceService maintenanceService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MaintenanceResponse registerMaintenance(
            @AuthenticationPrincipal UUID ownerId,
            @PathVariable UUID vehicleId,
            @Valid @RequestBody CreateMaintenanceRequest request
    ) {
        return maintenanceService.registerMaintenance(ownerId, vehicleId, request);
    }

    @GetMapping
    public List<MaintenanceResponse> listMaintenances(
            @AuthenticationPrincipal UUID ownerId,
            @PathVariable UUID vehicleId
    ) {
        return maintenanceService.listMaintenances(ownerId, vehicleId);
    }

    @GetMapping("/{maintenanceId}")
    public MaintenanceResponse findMaintenance(
            @AuthenticationPrincipal UUID ownerId,
            @PathVariable UUID vehicleId,
            @PathVariable UUID maintenanceId
    ) {
        return maintenanceService.findMaintenance(ownerId, vehicleId, maintenanceId);
    }

    @PutMapping("/{maintenanceId}")
    public MaintenanceResponse updateMaintenance(
            @AuthenticationPrincipal UUID ownerId,
            @PathVariable UUID vehicleId,
            @PathVariable UUID maintenanceId,
            @Valid @RequestBody UpdateMaintenanceRequest request
    ) {
        return maintenanceService.updateMaintenance(ownerId, vehicleId, maintenanceId, request);
    }

    @DeleteMapping("/{maintenanceId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMaintenance(
            @AuthenticationPrincipal UUID ownerId,
            @PathVariable UUID vehicleId,
            @PathVariable UUID maintenanceId
    ) {
        maintenanceService.deleteMaintenance(ownerId, vehicleId, maintenanceId);
    }
}