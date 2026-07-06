CREATE TABLE vehicles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_id UUID NOT NULL,
    plate VARCHAR(10) NOT NULL,
    brand VARCHAR(80) NOT NULL,
    model VARCHAR(80) NOT NULL,
    manufacture_year INTEGER NOT NULL,
    color VARCHAR(40),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),

    CONSTRAINT fk_vehicles_owner
        FOREIGN KEY (owner_id)
            REFERENCES users (id)
            ON DELETE CASCADE,

    CONSTRAINT uk_vehicles_owner_plate
        UNIQUE (owner_id, plate),

    CONSTRAINT ck_vehicles_manufacture_year
        CHECK (manufacture_year >= 1886)
);

CREATE INDEX idx_vehicles_owner_id ON vehicles (owner_id);
