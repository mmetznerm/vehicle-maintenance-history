ALTER TABLE vehicles
    ADD COLUMN history_sharing_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN history_public_id UUID;

ALTER TABLE vehicles
    ADD CONSTRAINT ck_vehicles_history_sharing
        CHECK (
            (history_sharing_enabled = TRUE AND history_public_id IS NOT NULL)
            OR
            (history_sharing_enabled = FALSE AND history_public_id IS NULL)
        );

CREATE UNIQUE INDEX uk_vehicles_history_public_id
    ON vehicles (history_public_id)
    WHERE history_public_id IS NOT NULL;
