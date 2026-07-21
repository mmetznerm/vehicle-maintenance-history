CREATE TABLE maintenance_inconsistencies (
    alert_id UUID PRIMARY KEY,
    vehicle_id UUID NOT NULL,
    rule_code VARCHAR(80) NOT NULL,
    severity VARCHAR(20) NOT NULL,
    maintenance_ids JSONB NOT NULL,
    summary VARCHAR(200) NOT NULL,
    details VARCHAR(1000) NOT NULL,
    status VARCHAR(20) NOT NULL,
    detected_at TIMESTAMP WITH TIME ZONE NOT NULL,
    resolved_at TIMESTAMP WITH TIME ZONE,
    last_event_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT ck_maintenance_inconsistency_status CHECK (status IN ('ACTIVE', 'RESOLVED'))
);

CREATE INDEX idx_maintenance_inconsistencies_vehicle
    ON maintenance_inconsistencies (vehicle_id, status, detected_at DESC);

CREATE TABLE processed_alert_events (
    event_id UUID NOT NULL,
    consumer_name VARCHAR(120) NOT NULL,
    processed_at TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (event_id, consumer_name)
);
