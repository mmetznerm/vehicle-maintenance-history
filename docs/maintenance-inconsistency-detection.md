# Maintenance Inconsistency Detection

## Goal

The consistency worker demonstrates an asynchronous domain analysis without
slowing down or coupling the transactional API. It consumes the existing
vehicle stream, keeps a private maintenance snapshot, evaluates deterministic
rules and publishes alert lifecycle events.

## Rules

| Rule | Severity | Detection |
|---|---|---|
| `ODOMETER_ROLLBACK` | Critical | A record on a later date has a lower odometer than the highest earlier reading. Records on the same day are not compared. |
| `POSSIBLE_DUPLICATE` | Warning | Two or more records share date, odometer and a normalized description. Case, accents and punctuation are ignored. |
| `DATE_BEFORE_MANUFACTURE` | Critical | A maintenance year is earlier than the vehicle manufacture year. |

These rules produce signals for review, not automatic changes to maintenance
data. A user corrects or removes the source record through the normal API.

## Alert lifecycle

Alert IDs are deterministic from the vehicle, rule and sorted maintenance IDs.
Reprocessing the same history therefore does not create a different alert.

1. A source event updates the worker's snapshot.
2. The complete history for that vehicle is evaluated.
3. Newly detected alerts are stored and emitted as
   `MaintenanceInconsistencyDetected`.
4. Alerts no longer produced by the rules are removed from the active set and
   emitted as `MaintenanceInconsistencyResolved`.
5. The backend projects both transitions and exposes active alerts by default.

The detector database and backend projection are deliberately separate. The
alert event contains no owner, plate or maintenance cost, preserving the privacy
boundary established by the source event contract.

## Portfolio demo

Start all services:

```bash
docker compose --profile app up --build
```

Create a vehicle and add these records in order:

| Date | Odometer | Description |
|---|---:|---|
| 2025-01-10 | 50,000 km | Oil change |
| 2025-02-10 | 40,000 km | Brake service |

The details page eventually shows a critical odometer rollback alert. Change
the second reading to `60,000 km`; after the correction event is processed, the
active alert disappears. Select **Show resolved** to confirm the transition was
retained by the backend projection.

To demonstrate fan-out and service ownership, inspect the two consumer groups
and the consistency database:

```bash
docker compose exec kafka /opt/kafka/bin/kafka-consumer-groups.sh --bootstrap-server kafka:9092 --list
docker compose exec consistency-postgres psql -U maintenance-consistency -d maintenance-consistency -c "select rule_code, severity, maintenance_ids from active_inconsistencies;"
docker compose exec postgres psql -U vehicle-maintenance-history -d vehicle-maintenance-history -c "select rule_code, status, detected_at, resolved_at from maintenance_inconsistencies order by detected_at desc;"
```

## Operational characteristics

- Source and alert delivery are at least once; consumers are idempotent.
- Kafka records are keyed by `vehicleId`, preserving per-vehicle ordering.
- Input and output topics have independent versioned contracts.
- The worker owns its PostgreSQL schema and can rebuild its snapshot by replaying
  the source topic.
- Rule evaluation currently scans one vehicle's history after each relevant
  event. For very large histories, incremental indexes or stream processing
  state stores would be the next evolution.
