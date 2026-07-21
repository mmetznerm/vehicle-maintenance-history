# Maintenance Inconsistency Alerts v1

The consistency worker publishes alert lifecycle events to
`maintenance-inconsistency-alerts.v1`, keyed by `vehicleId`. The backend uses a
different consumer group to build the authenticated alert view.

## Event types

- `MaintenanceInconsistencyDetected` creates or reactivates an alert.
- `MaintenanceInconsistencyResolved` closes an alert after the source data is corrected.

## Rules

- `ODOMETER_ROLLBACK`: a later maintenance has a lower odometer reading.
- `POSSIBLE_DUPLICATE`: records share date, odometer and normalized description.
- `DATE_BEFORE_MANUFACTURE`: maintenance predates the vehicle manufacture year.

`alertId` is deterministic for the rule and implicated records. Reprocessing the
same domain history therefore produces the same logical alert. `eventId` remains
the idempotency key for each lifecycle transition.

Delivery is at least once. Consumers must ignore unknown optional payload fields
and deduplicate by `eventId`. Owner data, license plate, cost and authentication
data must not be included.
