package com.mmetzner.vmh.shared.event;

import java.util.UUID;

public record HistorySharingEventPayload(
        boolean enabled,
        UUID publicId
) {
}
