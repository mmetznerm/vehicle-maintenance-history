package com.mmetzner.vmh.history.api;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/v1/public/vehicle-histories")
@RequiredArgsConstructor
public class PublicVehicleHistoryController {

    private final PublicVehicleHistoryService service;

    @GetMapping("/{publicId}")
    public PublicVehicleHistoryResponse findByPublicId(@PathVariable UUID publicId) {
        return service.findByPublicId(publicId);
    }
}
