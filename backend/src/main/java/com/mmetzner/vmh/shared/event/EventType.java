package com.mmetzner.vmh.shared.event;

public enum EventType {
    VEHICLE_CREATED("VehicleCreated", "Vehicle"),
    VEHICLE_UPDATED("VehicleUpdated", "Vehicle"),
    VEHICLE_DELETED("VehicleDeleted", "Vehicle"),
    VEHICLE_HISTORY_SHARING_CHANGED("VehicleHistorySharingChanged", "Vehicle"),
    MAINTENANCE_CREATED("MaintenanceCreated", "Maintenance"),
    MAINTENANCE_UPDATED("MaintenanceUpdated", "Maintenance"),
    MAINTENANCE_DELETED("MaintenanceDeleted", "Maintenance");

    private final String externalName;
    private final String aggregateType;

    EventType(String externalName, String aggregateType) {
        this.externalName = externalName;
        this.aggregateType = aggregateType;
    }

    public String externalName() {
        return externalName;
    }

    public String aggregateType() {
        return aggregateType;
    }
}
