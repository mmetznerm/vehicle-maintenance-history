package com.mmetzner.vmh.shared.event;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties("app.kafka.outbox")
public record OutboxRelayProperties(
        int batchSize,
        int maxAttempts,
        Duration publishTimeout
) {
    public OutboxRelayProperties {
        if (batchSize <= 0) {
            batchSize = 50;
        }
        if (maxAttempts <= 0) {
            maxAttempts = 10;
        }
        if (publishTimeout == null) {
            publishTimeout = Duration.ofSeconds(10);
        }
    }
}
