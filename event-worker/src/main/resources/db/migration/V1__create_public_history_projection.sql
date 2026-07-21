CREATE TABLE vehicle_histories (
    vehicle_id UUID PRIMARY KEY,
    brand VARCHAR(80) NOT NULL,
    model VARCHAR(80) NOT NULL,
    manufacture_year INTEGER NOT NULL,
    color VARCHAR(40),
    sharing_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    public_id UUID,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT uk_vehicle_histories_public_id UNIQUE (public_id),
    CONSTRAINT ck_vehicle_histories_sharing CHECK (
        (sharing_enabled = TRUE AND public_id IS NOT NULL)
        OR
        (sharing_enabled = FALSE AND public_id IS NULL)
    )
);

CREATE TABLE maintenance_histories (
    maintenance_id UUID PRIMARY KEY,
    vehicle_id UUID NOT NULL,
    maintenance_date DATE NOT NULL,
    odometer INTEGER NOT NULL,
    description VARCHAR(500) NOT NULL,
    cost NUMERIC(12, 2) NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT fk_maintenance_histories_vehicle
        FOREIGN KEY (vehicle_id)
            REFERENCES vehicle_histories (vehicle_id)
            ON DELETE CASCADE
);

CREATE INDEX idx_maintenance_histories_vehicle_date
    ON maintenance_histories (vehicle_id, maintenance_date, maintenance_id);

CREATE TABLE processed_events (
    event_id UUID NOT NULL,
    consumer_name VARCHAR(120) NOT NULL,
    processed_at TIMESTAMP WITH TIME ZONE NOT NULL,

    PRIMARY KEY (event_id, consumer_name)
);
