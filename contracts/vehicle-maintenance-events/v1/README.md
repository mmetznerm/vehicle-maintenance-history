# Vehicle Maintenance Events v1

All events are published to `vehicle-maintenance-events.v1` with `vehicleId` as
the Kafka record key. This keeps events for one vehicle in the same partition.

The envelope is defined by `vehicle-maintenance-event.schema.json`. Payloads use
camelCase and contain only the data required to build downstream projections.
Owner identifiers, license plates, authentication data, and other personal data
must not be included.

## Compatibility

- Additive, optional payload fields are backward compatible within version 1.
- Consumers must ignore unknown payload fields and unknown event types.
- Removing or changing the meaning or type of a field requires a new event version.
- A breaking envelope change requires a new topic version.
- `eventId` identifies one immutable event and is the consumer idempotency key.
- Delivery is at least once; consumers must accept duplicate records.

`examples.json` contains one representative envelope for every v1 event type.
