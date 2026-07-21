package com.mmetzner.vmh.consistency.projection;

import java.io.Serializable;
import java.util.UUID;

public record ProcessedEventId(UUID eventId, String consumerName) implements Serializable {
}
