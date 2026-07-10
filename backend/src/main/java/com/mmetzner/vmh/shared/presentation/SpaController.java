package com.mmetzner.vmh.shared.presentation;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SpaController {

    @GetMapping({
            "/",
            "/login",
            "/register",
            "/vehicles",
            "/vehicles/new",
            "/vehicles/{vehicleId}",
            "/vehicles/{vehicleId}/edit",
            "/vehicles/{vehicleId}/maintenances/new",
            "/vehicles/{vehicleId}/maintenances/{maintenanceId}/edit"
    })
    public String forwardToIndex() {
        return "forward:/index.html";
    }
}
