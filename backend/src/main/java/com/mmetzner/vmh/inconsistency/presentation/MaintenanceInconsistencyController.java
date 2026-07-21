package com.mmetzner.vmh.inconsistency.presentation;

import com.mmetzner.vmh.inconsistency.application.MaintenanceInconsistencyService;
import com.mmetzner.vmh.inconsistency.application.dto.MaintenanceInconsistencyResponse;
import com.mmetzner.vmh.shared.config.OpenApiConfig;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/vehicles/{vehicleId}/inconsistencies")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
@RequiredArgsConstructor
public class MaintenanceInconsistencyController {

    private final MaintenanceInconsistencyService inconsistencyService;

    @GetMapping
    public List<MaintenanceInconsistencyResponse> list(
            @AuthenticationPrincipal UUID ownerId,
            @PathVariable UUID vehicleId,
            @RequestParam(defaultValue = "false") boolean includeResolved
    ) {
        return inconsistencyService.list(ownerId, vehicleId, includeResolved);
    }
}
