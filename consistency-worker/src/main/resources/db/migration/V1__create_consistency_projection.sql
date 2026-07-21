CREATE TABLE vehicle_snapshots (
    vehicle_id UUID PRIMARY KEY,
    manufacture_year INTEGER,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE maintenance_snapshots (
    maintenance_id UUID PRIMARY KEY,
    vehicle_id UUID NOT NULL,
    maintenance_date DATE NOT NULL,
    odometer INTEGER NOT NULL,
    description VARCHAR(500) NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_maintenance_snapshot_vehicle FOREIGN KEY (vehicle_id)
        REFERENCES vehicle_snapshots (vehicle_id) ON DELETE CASCADE
);

CREATE INDEX idx_maintenance_snapshot_vehicle_date
    ON maintenance_snapshots (vehicle_id, maintenance_date, maintenance_id);

CREATE TABLE active_inconsistencies (
    alert_id UUID PRIMARY KEY,
    vehicle_id UUID NOT NULL,
    rule_code VARCHAR(80) NOT NULL,
    severity VARCHAR(20) NOT NULL,
    maintenance_ids TEXT NOT NULL,
    summary VARCHAR(200) NOT NULL,
    details VARCHAR(1000) NOT NULL,
    detected_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_active_inconsistencies_vehicle ON active_inconsistencies (vehicle_id);

CREATE TABLE processed_events (
    event_id UUID NOT NULL,
    consumer_name VARCHAR(120) NOT NULL,
    processed_at TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (event_id, consumer_name)
);

CREATE TABLE alert_outbox_events (
    sequence_id BIGSERIAL PRIMARY KEY,
    event_id UUID NOT NULL UNIQUE,
    vehicle_id UUID NOT NULL,
    event_type VARCHAR(80) NOT NULL,
    payload JSONB NOT NULL,
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL,
    published_at TIMESTAMP WITH TIME ZONE,
    attempts INTEGER NOT NULL DEFAULT 0,
    last_error VARCHAR(1000)
);

CREATE INDEX idx_alert_outbox_pending ON alert_outbox_events (sequence_id)
    WHERE published_at IS NULL;
