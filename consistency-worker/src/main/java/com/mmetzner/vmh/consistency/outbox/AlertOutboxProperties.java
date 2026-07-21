package com.mmetzner.vmh.consistency.outbox;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties("app.kafka.outbox")
public record AlertOutboxProperties(
        int batchSize,
        int maxAttempts,
        Duration publishTimeout
) {
    public AlertOutboxProperties {
        if (batchSize <= 0) batchSize = 50;
        if (maxAttempts <= 0) maxAttempts = 10;
        if (publishTimeout == null) publishTimeout = Duration.ofSeconds(10);
    }
}
