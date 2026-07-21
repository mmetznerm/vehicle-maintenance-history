package com.mmetzner.vmh.inconsistency.infrastructure;

import java.io.Serializable;
import java.util.UUID;

public record ProcessedAlertEventId(UUID eventId, String consumerName) implements Serializable {
}
