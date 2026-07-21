# Event-Driven Public Vehicle History

## Purpose

The authenticated backend remains the system of record. It publishes vehicle,
maintenance and sharing changes so an independent worker can build a public,
read-optimized history. The public API never exposes the owner identifier,
license plate or maintenance cost.

```text
Browser -> Backend API -> PostgreSQL
                         | same transaction
                         v
                    outbox_events
                         |
                         v
                       Kafka
                         |
                         v
                Public History Worker -> Projection PostgreSQL -> Public API
```

Both applications live in this repository because they form one portfolio use
case and share a deliberately versioned contract. They still have separate
builds, processes, databases and deployment units, so the service boundary is
visible without making local setup unnecessarily fragmented.

## Event contract

All v1 records use topic `vehicle-maintenance-events.v1` and `vehicleId` as the
Kafka key. Per-vehicle ordering is therefore preserved inside a partition. The
JSON Schema, compatibility rules and examples live in
`contracts/vehicle-maintenance-events/v1`.

Supported event types:

- `VehicleCreated`, `VehicleUpdated`, `VehicleDeleted`
- `MaintenanceCreated`, `MaintenanceUpdated`, `MaintenanceDeleted`
- `VehicleHistorySharingChanged`

## Delivery and consistency

The domain change and its outbox record are committed in the same database
transaction. A scheduled relay publishes pending rows and marks them only after
Kafka acknowledges the record. A crash between those operations can publish a
record more than once, so delivery is intentionally **at least once**.

The worker stores `(event_id, consumer_name)` in `processed_events` in the same
transaction that updates the projection. Duplicate deliveries are ignored.
The public view is eventually consistent and can briefly lag behind a successful
write to the backend.

## Sharing and privacy

Enabling sharing creates an opaque UUID. Disabling it clears that UUID and makes
the corresponding public URL unavailable after the event is consumed. Enabling
again generates a different UUID, so a revoked link cannot become valid later.

The public response includes only vehicle characteristics plus maintenance date,
odometer and description. Cost stays in the worker projection for possible
private analytics but is not part of the public API contract.

## Local demo

Start the full stack:

```bash
docker compose --profile app up --build
```

Then:

1. Register and create a vehicle at `http://localhost:8080`.
2. Add one or more maintenance records.
3. Open the vehicle details and enable public history sharing.
4. Open or copy the generated `/history/{publicId}` URL.
5. Update a maintenance and refresh the public page after the event is consumed.
6. Revoke sharing and verify that the URL no longer returns the history.

Useful inspection commands:

```bash
docker compose exec kafka /opt/kafka/bin/kafka-topics.sh --bootstrap-server kafka:9092 --describe --topic vehicle-maintenance-events.v1
docker compose exec postgres psql -U vehicle-maintenance-history -d vehicle-maintenance-history -c "select event_type, attempts, published_at from outbox_events order by sequence_id desc;"
docker compose exec history-postgres psql -U vehicle-history -d vehicle-history -c "select event_id, consumer_name, processed_at from processed_events order by processed_at desc;"
```

## Rebuilding the projection

Kafka retention determines how far the projection can be rebuilt. For a local
demonstration, stop the worker, clear its owned tables, reset its consumer group
to the beginning, and start it again:

```bash
docker compose stop event-worker
docker compose exec history-postgres psql -U vehicle-history -d vehicle-history -c "truncate processed_events, maintenance_histories, vehicle_histories cascade;"
docker compose exec kafka /opt/kafka/bin/kafka-consumer-groups.sh --bootstrap-server kafka:9092 --group vehicle-public-history-v1 --topic vehicle-maintenance-events.v1 --reset-offsets --to-earliest --execute
docker compose --profile app up -d event-worker
```

Existing records created before event publication was introduced are not
automatically backfilled. A production rollout would publish a controlled
snapshot or run a dedicated backfill before relying on replay alone.
