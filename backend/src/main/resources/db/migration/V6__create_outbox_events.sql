CREATE TABLE outbox_events (
    sequence_id BIGSERIAL PRIMARY KEY,
    event_id UUID NOT NULL,
    aggregate_type VARCHAR(80) NOT NULL,
    aggregate_id UUID NOT NULL,
    vehicle_id UUID NOT NULL,
    event_type VARCHAR(80) NOT NULL,
    event_version INTEGER NOT NULL,
    payload JSONB NOT NULL,
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL,
    published_at TIMESTAMP WITH TIME ZONE,
    attempts INTEGER NOT NULL DEFAULT 0,
    last_error VARCHAR(1000),

    CONSTRAINT uk_outbox_events_event_id UNIQUE (event_id),
    CONSTRAINT ck_outbox_events_event_version CHECK (event_version > 0),
    CONSTRAINT ck_outbox_events_attempts CHECK (attempts >= 0)
);

CREATE INDEX idx_outbox_events_pending
    ON outbox_events (sequence_id)
    WHERE published_at IS NULL;
