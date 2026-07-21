package com.mmetzner.vmh.vehicle.presentation;

import com.mmetzner.vmh.shared.config.OpenApiConfig;
import com.mmetzner.vmh.vehicle.application.VehicleService;
import com.mmetzner.vmh.vehicle.application.dto.CreateVehicleRequest;
import com.mmetzner.vmh.vehicle.application.dto.UpdateVehicleRequest;
import com.mmetzner.vmh.vehicle.application.dto.VehicleResponse;
import com.mmetzner.vmh.vehicle.application.dto.VehicleSummaryResponse;
import com.mmetzner.vmh.vehicle.application.dto.VehicleHistorySharingResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/vehicles")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
@RequiredArgsConstructor
public class VehicleController {

    private final VehicleService vehicleService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public VehicleResponse registerVehicle(
            @AuthenticationPrincipal UUID ownerId,
            @Valid @RequestBody CreateVehicleRequest request
    ) {
        return vehicleService.registerVehicle(ownerId, request);
    }

    @GetMapping
    public List<VehicleSummaryResponse> listVehicles(@AuthenticationPrincipal UUID ownerId) {
        return vehicleService.listVehicles(ownerId);
    }

    @GetMapping("/{vehicleId}")
    public VehicleResponse findVehicle(
            @AuthenticationPrincipal UUID ownerId,
            @PathVariable UUID vehicleId
    ) {
        return vehicleService.findVehicle(ownerId, vehicleId);
    }

    @PutMapping("/{vehicleId}")
    public VehicleResponse updateVehicle(
            @AuthenticationPrincipal UUID ownerId,
            @PathVariable UUID vehicleId,
            @Valid @RequestBody UpdateVehicleRequest request
    ) {
        return vehicleService.updateVehicle(ownerId, vehicleId, request);
    }

    @DeleteMapping("/{vehicleId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteVehicle(
            @AuthenticationPrincipal UUID ownerId,
            @PathVariable UUID vehicleId
    ) {
        vehicleService.deleteVehicle(ownerId, vehicleId);
    }

    @GetMapping("/{vehicleId}/history-sharing")
    public VehicleHistorySharingResponse getHistorySharing(
            @AuthenticationPrincipal UUID ownerId,
            @PathVariable UUID vehicleId
    ) {
        return vehicleService.getHistorySharing(ownerId, vehicleId);
    }

    @PostMapping("/{vehicleId}/history-sharing")
    public VehicleHistorySharingResponse enableHistorySharing(
            @AuthenticationPrincipal UUID ownerId,
            @PathVariable UUID vehicleId
    ) {
        return vehicleService.enableHistorySharing(ownerId, vehicleId);
    }

    @DeleteMapping("/{vehicleId}/history-sharing")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void disableHistorySharing(
            @AuthenticationPrincipal UUID ownerId,
            @PathVariable UUID vehicleId
    ) {
        vehicleService.disableHistorySharing(ownerId, vehicleId);
    }
}
