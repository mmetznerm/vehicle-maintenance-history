CREATE TABLE maintenances (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    vehicle_id UUID NOT NULL,
    maintenance_date DATE NOT NULL,
    odometer INTEGER NOT NULL,
    description VARCHAR(500) NOT NULL,
    cost NUMERIC(12, 2) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),

    CONSTRAINT fk_maintenances_vehicle
        FOREIGN KEY (vehicle_id)
            REFERENCES vehicles (id)
            ON DELETE CASCADE,

            CONSTRAINT ck_maintenances_odometer
                CHECK (odometer >= 0),

            CONSTRAINT ck_maintenances_cost
                CHECK (cost >= 0)
);

CREATE INDEX idx_maintenances_vehicle_id ON maintenances (vehicle_id);
CREATE INDEX idx_maintenances_vehicle_id_date ON maintenances (vehicle_id, maintenance_date DESC);